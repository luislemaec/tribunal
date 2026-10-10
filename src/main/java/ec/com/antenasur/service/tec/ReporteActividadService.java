package ec.com.antenasur.service.tec;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.audit.CatalogoActividades;
import ec.com.antenasur.dto.ActividadAuditoriaDTO;
import ec.com.antenasur.dto.FiltroActividadAuditoriaDTO;
import ec.com.antenasur.dto.UsuarioAuditoriaDTO;
import ec.com.antenasur.dto.UsuarioEnLineaDTO;
import ec.com.antenasur.facade.tec.ProcesoFacade;
import ec.com.antenasur.facade.tec.ReporteActividadFacade;
import ec.com.antenasur.security.sesion.SesionesWebEnLinea;

/**
 * Pestaña Actividad de Rep. Registros: resumen de transacciones de la bitácora y
 * usuarios en línea. Mismo alcance que Actividades (procesos.xhtml): Administrador ve
 * todo y Tribunal todo salvo lo de los usuarios Administrador.
 */
@Stateless
@DeclareRoles({ "SITEC-Administrador", "SITEC-Tribunal" })
@RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal" })
public class ReporteActividadService {

    /** Una sesión de la App cuenta como en línea si se usó en este plazo (igual que el timeout web). */
    private static final long MINUTOS_APP_EN_LINEA = 15;

    private static final String ROL_ADMINISTRADOR = "SITEC-Administrador";

    @Resource
    private SessionContext sessionContext;

    @Inject
    private ProcesoFacade procesoFacade;

    @Inject
    private ProcesoService procesoService;

    @Inject
    private ReporteActividadFacade reporteActividadFacade;

    @Inject
    private SesionesWebEnLinea sesionesWebEnLinea;

    private boolean excluirAdministradores() {
        return !sessionContext.isCallerInRole(ROL_ADMINISTRADOR);
    }

    /** Transacciones por día, con los días sin actividad en 0 para no deformar la serie. */
    public Map<LocalDate, Long> transaccionesPorDia(int dias) {
        LocalDate desde = LocalDate.now().minusDays(dias - 1L);
        Map<LocalDate, Long> serie = new LinkedHashMap<>();
        for (LocalDate dia = desde; !dia.isAfter(LocalDate.now()); dia = dia.plusDays(1)) {
            serie.put(dia, 0L);
        }
        for (Object[] fila : procesoFacade.contarPorDia(desde, excluirAdministradores())) {
            if (fila[0] instanceof LocalDate dia && serie.containsKey(dia)) {
                serie.put(dia, ((Number) fila[1]).longValue());
            }
        }
        return serie;
    }

    /** Transacciones por módulo del catálogo, de mayor a menor y sin los módulos vacíos. */
    public Map<String, Long> transaccionesPorModulo(int dias) {
        List<String> modulos = CatalogoActividades.modulos();
        long[] totales = procesoFacade.contarPorModulo(LocalDate.now().minusDays(dias - 1L), modulos,
                excluirAdministradores());
        Map<String, Long> resultado = new LinkedHashMap<>();
        List<Integer> orden = new ArrayList<>();
        for (int i = 0; i < modulos.size(); i++) {
            orden.add(i);
        }
        orden.sort(Comparator.comparingLong((Integer i) -> totales[i]).reversed());
        for (int i : orden) {
            if (totales[i] > 0) {
                resultado.put(modulos.get(i), totales[i]);
            }
        }
        return resultado;
    }

    /** Últimos eventos, con el alcance que ProcesoService aplica en Actividades. */
    public List<ActividadAuditoriaDTO> ultimosEventos(int cantidad) {
        return procesoService.buscarAuditoria(new FiltroActividadAuditoriaDTO(), 0, cantidad);
    }

    /** Usuarios con sesión web vigente o sesión de la App usada recientemente. */
    public List<UsuarioEnLineaDTO> usuariosEnLinea() {
        Map<String, UsuarioEnLineaDTO> porUsuario = new LinkedHashMap<>();
        for (SesionesWebEnLinea.SesionWeb sesion : sesionesWebEnLinea.vigentes()) {
            porUsuario.computeIfAbsent(sesion.usuario(), UsuarioEnLineaDTO::new)
                    .agregar(UsuarioEnLineaDTO.WEB, sesion.desde());
        }
        Instant usadaDesde = Instant.now().minus(MINUTOS_APP_EN_LINEA, ChronoUnit.MINUTES);
        for (Object[] fila : reporteActividadFacade.usuariosAppEnLinea(usadaDesde)) {
            porUsuario.computeIfAbsent((String) fila[0], UsuarioEnLineaDTO::new)
                    .agregar(UsuarioEnLineaDTO.APP, (Instant) fila[1]);
        }
        Map<String, UsuarioAuditoriaDTO> identidades = procesoFacade.buscarIdentidades(porUsuario.keySet());
        boolean excluir = excluirAdministradores();
        List<UsuarioEnLineaDTO> resultado = new ArrayList<>();
        for (UsuarioEnLineaDTO enLinea : porUsuario.values()) {
            UsuarioAuditoriaDTO identidad = identidades.get(enLinea.getUsuario());
            String roles = identidad != null ? identidad.getRolesTexto() : null;
            // Mismo criterio que Actividades: Tribunal no ve a quienes tienen el rol Administrador.
            // UsuarioAuditoriaDTO muestra los roles sin el prefijo SITEC-.
            if (excluir && roles != null && List.of(roles.split(", ")).contains("Administrador")) {
                continue;
            }
            enLinea.setNombre(identidad != null ? identidad.getNombre() : null);
            enLinea.setRoles(roles);
            resultado.add(enLinea);
        }
        resultado.sort(Comparator.comparing(UsuarioEnLineaDTO::getDesde,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return resultado;
    }
}
