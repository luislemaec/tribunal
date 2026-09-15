package ec.com.antenasur.service.tec;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.SessionContext;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.ActividadAuditoriaDTO;
import ec.com.antenasur.dto.FiltroActividadAuditoriaDTO;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.facade.tec.ProcesoFacade;
import ec.com.antenasur.model.tec.Proceso;
import ec.com.antenasur.service.AbstractService;

@Stateless
@DeclareRoles({"SITEC-Administrador", "SITEC-Tecnico", "SITEC-Analista", "SITEC-Tribunal",
    "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa"})
public class ProcesoService extends AbstractService<Proceso, Integer, ProcesoFacade> {

    @Inject
    private ProcesoFacade procesoFacade;

    @Resource
    private SessionContext sessionContext;

    @Override
    protected ProcesoFacade getFacade() {
        return procesoFacade;
    }

    public List<Proceso> getProcesoPorUsuario(String usuario) {
        return procesoFacade.getProcesoPorUsuario(usuario);
    }

    /**
     * Acepta {@link java.util.Date} (lo que JSF/PrimeFaces y otros consumidores
     * usan habitualmente) y convierte internamente a {@link java.sql.Date} que
     * es lo que requiere el facade JPA.
     */
    public List<Proceso> getProcesoPorUsuario(Date fechaInicio, Date fechaFin, String usuario) {
        java.sql.Date sqlInicio = (fechaInicio != null) ? new java.sql.Date(fechaInicio.getTime()) : null;
        java.sql.Date sqlFin = (fechaFin != null) ? new java.sql.Date(fechaFin.getTime()) : null;
        return procesoFacade.getProcesoPorUsuario(sqlInicio, sqlFin, usuario);
    }

    /**
     * Devuelve actividades bajo el alcance del principal EJB. Administrador
     * puede consultar todo; los demás roles solo sus propios registros.
     */
    @RolesAllowed({"SITEC-Administrador", "SITEC-Tecnico", "SITEC-Analista", "SITEC-Tribunal",
        "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa"})
    public List<ActividadAuditoriaDTO> buscarAuditoria(FiltroActividadAuditoriaDTO filtro,
            int first, int pageSize) {
        String usuarioAlcance = resolverUsuarioAlcance();
        List<Proceso> registros = procesoFacade.buscarAuditoria(filtro, usuarioAlcance, first, pageSize);
        List<ActividadAuditoriaDTO> resultado = new ArrayList<>();
        for (Proceso registro : registros) resultado.add(ActividadAuditoriaDTO.fromEntity(registro));
        return resultado;
    }

    @RolesAllowed({"SITEC-Administrador", "SITEC-Tecnico", "SITEC-Analista", "SITEC-Tribunal",
        "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa"})
    public int contarAuditoria(FiltroActividadAuditoriaDTO filtro) {
        return procesoFacade.contarAuditoria(filtro, resolverUsuarioAlcance());
    }

    /** Solo Administrador recibe el catálogo global de usuarios para el filtro. */
    @RolesAllowed({"SITEC-Administrador", "SITEC-Tecnico", "SITEC-Analista", "SITEC-Tribunal",
        "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa"})
    public List<String> listarUsuariosAuditoria() {
        if (!sessionContext.isCallerInRole("SITEC-Administrador")) return List.of();
        return procesoFacade.listarUsuariosConActividad();
    }

    @RolesAllowed({"SITEC-Administrador", "SITEC-Tecnico", "SITEC-Analista", "SITEC-Tribunal",
        "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa"})
    public List<Proceso> listarAuditoriaParaReporte(FiltroActividadAuditoriaDTO filtro) {
        int total = contarAuditoria(filtro);
        return total == 0 ? List.of() : procesoFacade.buscarAuditoria(filtro, resolverUsuarioAlcance(), 0, total);
    }

    private String resolverUsuarioAlcance() {
        if (sessionContext.getCallerPrincipal() == null
                || sessionContext.getCallerPrincipal().getName() == null
                || sessionContext.getCallerPrincipal().getName().isBlank()) {
            throw new NegocioException("No existe un usuario autenticado para consultar la auditoría.");
        }
        return sessionContext.isCallerInRole("SITEC-Administrador")
                ? null : sessionContext.getCallerPrincipal().getName();
    }
}
