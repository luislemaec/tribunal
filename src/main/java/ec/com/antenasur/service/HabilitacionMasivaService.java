package ec.com.antenasur.service;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.ResultadoHabilitacionMasivaDTO;
import ec.com.antenasur.dto.ResultadoHabilitacionMasivaDTO.Rechazo;
import ec.com.antenasur.exception.IglesiaPersonaException;
import ec.com.antenasur.facade.IglesiaFacade;
import ec.com.antenasur.facade.IglesiaPersonaFacade;
import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.model.tec.Proceso;
import ec.com.antenasur.service.tec.CronogramaService;
import ec.com.antenasur.service.tec.ProcesoService;
import ec.com.antenasur.util.Constantes;
import lombok.extern.slf4j.Slf4j;

/**
 * Habilitación masiva de los miembros pendientes de una iglesia que actualizó como máximo
 * el porcentaje configurado de su listado ({@link #esApta}). Los ya revisados no se tocan.
 * Solo Administrador y Tribunal. Cada miembro se procesa con
 * {@link IglesiaPersonaService#habilitarPendiente} en su propia transacción y con las
 * mismas reglas que la edición individual: los válidos quedan revisados y habilitados, y
 * un miembro inválido no revierte a los demás; queda pendiente con su motivo.
 *
 * <p>La transacción de este método solo sostiene el bloqueo por iglesia (evita dos
 * ejecuciones simultáneas) y la entrada de bitácora con el resumen.</p>
 */
@Slf4j
@Stateless
@DeclareRoles({ "SITEC-Administrador", "SITEC-Tribunal" })
@RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal" })
public class HabilitacionMasivaService {

    static final String ACTIVIDAD = "HABILITACION_MASIVA";
    private static final String ROL_TRIBUNAL = "SITEC-" + Constantes.getRolTribunal();
    private static final String ROL_ADMINISTRADOR = "SITEC-" + Constantes.getRolAdministrador();
    private static final int LARGO_ACTIVIDAD = 255;

    /** Proxy del EJB: así se respeta el REQUIRES_NEW de cada miembro. */
    @Inject
    private IglesiaPersonaService iglesiaPersonaService;

    @Inject
    private IglesiaPersonaFacade iglesiaPersonaFacade;

    @Inject
    private IglesiaFacade iglesiaFacade;

    @Inject
    private CronogramaService cronogramaService;

    @Inject
    private ProcesoService procesoService;

    @Resource
    private SessionContext sessionContext;

    /**
     * Regla única de la habilitación masiva, usada por el botón y por {@link #habilitar}:
     * la iglesia tiene miembros, le quedan pendientes y sus actualizados (la cuenta del
     * indicador «Miembros actualizados» de Personas) no superan el porcentaje máximo.
     * Comparación exacta, sin el redondeo del porcentaje que muestra la pantalla.
     */
    public static boolean esApta(int total, int actualizados, int porcentajeMaximo) {
        return total > 0 && actualizados < total && (long) actualizados * 100 <= (long) total * porcentajeMaximo;
    }

    public static boolean esApta(int total, int actualizados) {
        return esApta(total, actualizados, Constantes.getPorcentajeMaximoHabilitacionMasiva());
    }

    public ResultadoHabilitacionMasivaDTO habilitar(Integer iglesiaId, String ip) {
        String rol = rolCaller();
        if (rol == null) {
            throw new IglesiaPersonaException("form.personas.masiva.error.permiso");
        }
        if (!cronogramaService.permiteEdicionPadron()) {
            throw new IglesiaPersonaException("form.personas.error.cronograma");
        }
        Iglesia iglesia = iglesiaId != null ? iglesiaFacade.find(iglesiaId) : null;
        if (iglesia == null || !Boolean.TRUE.equals(iglesia.getEstado())) {
            throw new IglesiaPersonaException("form.personas.error.iglesia.inactiva");
        }
        if (!iglesiaPersonaFacade.bloquearHabilitacionMasiva(iglesiaId)) {
            throw new IglesiaPersonaException("form.personas.masiva.error.en.curso");
        }
        // Se vuelve a evaluar ya con el bloqueo: la vista pudo quedar desactualizada.
        int[] progreso = iglesiaPersonaService.calcularProgresoActualizacion(iglesiaId);
        int porcentajeMaximo = Constantes.getPorcentajeMaximoHabilitacionMasiva();
        if (!esApta(progreso[0], progreso[1], porcentajeMaximo)) {
            throw progreso[0] > 0 && progreso[1] >= progreso[0]
                    ? new IglesiaPersonaException("form.personas.masiva.error.sin.pendientes")
                    : new IglesiaPersonaException("form.personas.masiva.error.no.disponible",
                            porcentajeMaximo, progreso[1], progreso[0]);
        }
        int revisadosPrevios = progreso[1];

        List<Object[]> pendientes = iglesiaPersonaFacade.listarPendientesRevisionPorIglesia(iglesiaId);
        List<Rechazo> rechazados = new ArrayList<>();
        int habilitados = 0;
        for (Object[] fila : pendientes) {
            Integer vinculoId = (Integer) fila[0];
            try {
                iglesiaPersonaService.habilitarPendiente(vinculoId, iglesiaId);
                habilitados++;
            } catch (IglesiaPersonaException e) {
                rechazados.add(rechazo(fila, Constantes.getMensaje(e.getMessageKey(), e.getArguments())));
            } catch (RuntimeException e) {
                // Solo esta transacción se revirtió; el resto continúa.
                boolean relacionActiva = esViolacionRelacionActiva(e);
                if (!relacionActiva) {
                    log.error("Error al habilitar el miembro {} en la habilitación masiva", vinculoId, e);
                }
                rechazados.add(rechazo(fila, Constantes.getMensaje(relacionActiva
                        ? "form.personas.error.relacion.activa" : "form.personas.error.guardar")));
            }
        }
        registrarBitacora(iglesia.getNombre(), rol, revisadosPrevios, porcentajeMaximo, pendientes.size(),
                habilitados, rechazados.size(), ip);
        return new ResultadoHabilitacionMasivaDTO(iglesia.getNombre(), revisadosPrevios, pendientes.size(),
                habilitados, rechazados);
    }

    private static Rechazo rechazo(Object[] fila, String motivo) {
        return new Rechazo((Integer) fila[0], (String) fila[1], (String) fila[2], motivo);
    }

    private String rolCaller() {
        if (sessionContext == null) {
            return null;
        }
        if (sessionContext.isCallerInRole(ROL_TRIBUNAL)) {
            return ROL_TRIBUNAL;
        }
        return sessionContext.isCallerInRole(ROL_ADMINISTRADOR) ? ROL_ADMINISTRADOR : null;
    }

    /**
     * Una entrada con el resumen (usuario y fecha los pone la bitácora); cada miembro
     * habilitado deja además su entrada ACTUALIZA y su revisión de Envers. Si no cabe en
     * la columna, se acorta el nombre de la iglesia y nunca los totales.
     */
    private void registrarBitacora(String iglesia, String rol, int revisadosPrevios, int porcentajeMaximo,
            int procesados, int habilitados, int rechazados, String ip) {
        String prefijo = ACTIVIDAD + " | MÓDULO: PERSONAS E IGLESIAS; RESULTADO: EXITOSO; DETALLE: Iglesia: ";
        String sufijo = "; Rol: " + rol + "; Revisados previos: " + revisadosPrevios + "; Umbral: "
                + porcentajeMaximo + " %; Procesados: " + procesados + "; Habilitados: " + habilitados
                + "; Rechazados: " + rechazados;
        String nombre = iglesia == null ? "" : iglesia.replaceAll("[\\r\\n\\t]", " ");
        int disponible = Math.max(0, LARGO_ACTIVIDAD - prefijo.length() - sufijo.length());
        Proceso actividad = new Proceso(ip);
        actividad.setActividad(prefijo + nombre.substring(0, Math.min(nombre.length(), disponible)) + sufijo);
        procesoService.create(actividad);
    }

    /** Violación 23505 del trigger fn_validar_iglesia_activa_persona en la cadena de causas. */
    private static boolean esViolacionRelacionActiva(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof java.sql.SQLException sql && "23505".equals(sql.getSQLState())) {
                return true;
            }
            if (causa.getCause() == causa) {
                break;
            }
        }
        return false;
    }
}
