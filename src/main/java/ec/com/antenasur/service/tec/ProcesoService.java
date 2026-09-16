package ec.com.antenasur.service.tec;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.PermitAll;
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
@DeclareRoles({"SITEC-Administrador", "SITEC-Tribunal",
    "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa"})
@RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal",
    "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa"})
public class ProcesoService extends AbstractService<Proceso, Integer, ProcesoFacade> {

    private static final java.util.regex.Pattern USUARIO_AUDITABLE =
            java.util.regex.Pattern.compile("[A-Za-z0-9._@-]{1,120}");
    private static final java.util.regex.Pattern IP_AUDITABLE =
            java.util.regex.Pattern.compile("[0-9A-Fa-f:.]{1,64}");

    @Inject
    private ProcesoFacade procesoFacade;

    @Resource
    private SessionContext sessionContext;

    @Override
    protected ProcesoFacade getFacade() {
        return procesoFacade;
    }

    /** Operaciones normales de bitácora: requieren una identidad Elytron. */
    @Override
    @RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal",
        "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa"})
    public Proceso create(Proceso entity) {
        return super.create(entity);
    }

    /**
     * Excepción mínima para el único momento previo a la autenticación. El
     * método no permite elegir acción, módulo, resultado ni detalle y sólo
     * persiste datos normalizados del intento fallido.
     */
    @PermitAll
    public void registrarLoginFallidoPreautenticacion(String usuarioIntentado, String ip) {
        String usuario = normalizarUsuarioIntentado(usuarioIntentado);
        String ipNormalizada = normalizarIp(ip);
        procesoFacade.registrarLoginFallidoPreautenticacion(usuario, ipNormalizada);
    }

    /**
     * Canal público y acotado para la recuperación de clave. La actividad es
     * fija y el usuario proviene de una solicitud previamente validada por la
     * capa de usuarios; nunca recibe correo, token ni texto libre.
     */
    @PermitAll
    public void registrarSolicitudRecuperacionClavePreautenticacion(String usuarioValidado, String ip) {
        String usuario = usuarioValidado == null ? "<anonimo>" : normalizarUsuarioIntentado(usuarioValidado);
        procesoFacade.registrarSolicitudRecuperacionClavePreautenticacion(usuario, normalizarIp(ip));
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
    @RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal",
        "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa"})
    public List<ActividadAuditoriaDTO> buscarAuditoria(FiltroActividadAuditoriaDTO filtro,
            int first, int pageSize) {
        String usuarioAlcance = resolverUsuarioAlcance();
        List<Proceso> registros = procesoFacade.buscarAuditoria(filtro, usuarioAlcance, first, pageSize);
        List<ActividadAuditoriaDTO> resultado = new ArrayList<>();
        for (Proceso registro : registros) resultado.add(ActividadAuditoriaDTO.fromEntity(registro));
        return resultado;
    }

    @RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal",
        "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa"})
    public int contarAuditoria(FiltroActividadAuditoriaDTO filtro) {
        return procesoFacade.contarAuditoria(filtro, resolverUsuarioAlcance());
    }

    /** Solo Administrador recibe el catálogo global de usuarios para el filtro. */
    @RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal",
        "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa"})
    public List<String> listarUsuariosAuditoria() {
        if (!sessionContext.isCallerInRole("SITEC-Administrador")) return List.of();
        return procesoFacade.listarUsuariosConActividad();
    }

    @RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal",
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

    private String normalizarUsuarioIntentado(String usuario) {
        if (usuario == null) return "<desconocido>";
        String valor = usuario.trim();
        return USUARIO_AUDITABLE.matcher(valor).matches() ? valor : "<desconocido>";
    }

    private String normalizarIp(String ip) {
        if (ip == null) return "";
        String valor = ip.trim();
        return IP_AUDITABLE.matcher(valor).matches() ? valor : "";
    }
}
