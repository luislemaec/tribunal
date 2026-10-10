package ec.com.antenasur.service.tec;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.AdministradorIglesiaDTO;
import ec.com.antenasur.dto.CategoriaGeograficaDTO;
import ec.com.antenasur.dto.ConteoAdministradoresDTO;
import ec.com.antenasur.facade.tec.ProcesoFacade;
import ec.com.antenasur.facade.tec.ReporteAdministradoresFacade;

/**
 * Pestaña Administradores de Rep. Registros: iglesias con y sin administrador,
 * administradores activados o pendientes, último acceso y avance de registro con y
 * sin administrador. Mismos roles que la página.
 *
 * <p>Tres consultas por carga (iglesias, administradores, últimos accesos), ninguna por
 * iglesia. La agrupación por cantón o parroquia se hace sobre las filas por iglesia, así
 * que cambiar el filtro o los días de inactividad no vuelve a consultar.</p>
 */
@Stateless
@DeclareRoles({ "SITEC-Administrador", "SITEC-Tribunal" })
@RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal" })
public class ReporteAdministradoresService {

    @Inject
    private ReporteAdministradoresFacade reporteAdministradoresFacade;

    @Inject
    private ProcesoFacade procesoFacade;

    /** Iglesias activas con su administrador (si lo tiene) y su último acceso. */
    public List<AdministradorIglesiaDTO> iglesias() {
        List<AdministradorIglesiaDTO> iglesias = reporteAdministradoresFacade.listarIglesias();
        Map<Integer, AdministradorIglesiaDTO> porId = new HashMap<>();
        for (AdministradorIglesiaDTO iglesia : iglesias) {
            porId.put(iglesia.getIglesiaId(), iglesia);
        }
        // Ordenados por id de usuario: si una iglesia tuviera dos, prevalece el primero.
        for (Object[] admin : reporteAdministradoresFacade.listarAdministradores()) {
            AdministradorIglesiaDTO iglesia = porId.get((Integer) admin[0]);
            if (iglesia != null && !iglesia.isTieneAdministrador()) {
                iglesia.setAdministradorNombre((String) admin[1]);
                iglesia.setAdministradorUsuario((String) admin[2]);
                iglesia.setAdministradorCorreo((String) admin[3]);
                iglesia.setAdministradorPermanente((Boolean) admin[4]);
            }
        }
        List<String> usuarios = iglesias.stream().filter(AdministradorIglesiaDTO::isTieneAdministrador)
                .map(AdministradorIglesiaDTO::getAdministradorUsuario).toList();
        Map<String, Date> accesos = procesoFacade.buscarUltimoInicioSesion(usuarios);
        for (AdministradorIglesiaDTO iglesia : iglesias) {
            if (iglesia.isTieneAdministrador()) {
                iglesia.setUltimoAcceso(accesos.get(iglesia.getAdministradorUsuario()));
            }
        }
        return iglesias;
    }

    /** Sin acceso reciente: nunca inició sesión o su último acceso es anterior a {@code dias}. */
    public void marcarInactividad(List<AdministradorIglesiaDTO> iglesias, int dias) {
        Date limite = Date.from(Instant.now().minus(dias, ChronoUnit.DAYS));
        for (AdministradorIglesiaDTO iglesia : iglesias) {
            iglesia.setSinAccesoReciente(iglesia.isTieneAdministrador()
                    && (iglesia.getUltimoAcceso() == null || iglesia.getUltimoAcceso().before(limite)));
        }
    }

    public List<ConteoAdministradoresDTO> porCanton(List<AdministradorIglesiaDTO> iglesias) {
        List<ConteoAdministradoresDTO> cantones = agrupar(iglesias, AdministradorIglesiaDTO::getCantonId,
                i -> new ConteoAdministradoresDTO(i.getCantonId(), i.getCanton(), i.getProvincia()));
        CategoriaGeograficaDTO.desambiguar(cantones);
        return cantones;
    }

    public List<ConteoAdministradoresDTO> porParroquia(List<AdministradorIglesiaDTO> iglesias) {
        return agrupar(iglesias, AdministradorIglesiaDTO::getParroquiaId,
                i -> new ConteoAdministradoresDTO(i.getParroquiaId(), i.getParroquia(), i.getProvincia()));
    }

    public ConteoAdministradoresDTO totales(List<AdministradorIglesiaDTO> iglesias) {
        ConteoAdministradoresDTO total = new ConteoAdministradoresDTO(null, null, null);
        iglesias.forEach(total::acumular);
        return total;
    }

    /**
     * Avance de registro en iglesias con y sin administrador: {% habilitados, % revisados}
     * sobre los registrados de cada grupo (sumados por iglesia).
     */
    public int[][] avanceConYSinAdministrador(List<AdministradorIglesiaDTO> iglesias) {
        long[] registrados = new long[2];
        long[] habilitados = new long[2];
        long[] revisados = new long[2];
        for (AdministradorIglesiaDTO iglesia : iglesias) {
            int grupo = iglesia.isTieneAdministrador() ? 0 : 1;
            registrados[grupo] += iglesia.getRegistrados();
            habilitados[grupo] += iglesia.getHabilitados();
            revisados[grupo] += Math.max(0L, iglesia.getRegistrados() - iglesia.getPendientesRevision());
        }
        return new int[][] { { porcentaje(habilitados[0], registrados[0]), porcentaje(revisados[0], registrados[0]) },
                { porcentaje(habilitados[1], registrados[1]), porcentaje(revisados[1], registrados[1]) } };
    }

    /**
     * Lista de acción: sin administrador, pendientes de activar, sin acceso reciente y al día.
     * Mutable: p:dataTable con sortBy la ordena en el propio objeto (SortFeature).
     */
    public List<AdministradorIglesiaDTO> ordenarParaAtencion(List<AdministradorIglesiaDTO> iglesias) {
        return iglesias.stream().sorted(Comparator.comparingInt(AdministradorIglesiaDTO::getPrioridad)
                .thenComparing(AdministradorIglesiaDTO::getCanton)
                .thenComparing(AdministradorIglesiaDTO::getIglesia)).collect(Collectors.toCollection(ArrayList::new));
    }

    private static List<ConteoAdministradoresDTO> agrupar(List<AdministradorIglesiaDTO> iglesias,
            Function<AdministradorIglesiaDTO, Integer> clave,
            Function<AdministradorIglesiaDTO, ConteoAdministradoresDTO> nueva) {
        Map<Integer, ConteoAdministradoresDTO> grupos = new LinkedHashMap<>();
        for (AdministradorIglesiaDTO iglesia : iglesias) {
            grupos.computeIfAbsent(clave.apply(iglesia), k -> nueva.apply(iglesia)).acumular(iglesia);
        }
        List<ConteoAdministradoresDTO> resultado = new ArrayList<>(grupos.values());
        resultado.sort(Comparator.comparingLong(ConteoAdministradoresDTO::getIglesias).reversed()
                .thenComparing(ConteoAdministradoresDTO::getNombre));
        return resultado;
    }

    private static int porcentaje(long parte, long total) {
        return total == 0 ? 0 : (int) Math.round(parte * 100.0 / total);
    }
}
