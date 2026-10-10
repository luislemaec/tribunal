package ec.com.antenasur.service.tec;

import java.util.Date;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
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
import ec.com.antenasur.dto.UsuarioAuditoriaDTO;
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
     * Devuelve actividades bajo el alcance del principal EJB: Administrador consulta
     * todo; Tribunal todo salvo las actividades de usuarios Administrador; los demás
     * roles solo sus propios registros.
     */
    @RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal",
        "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa"})
    public List<ActividadAuditoriaDTO> buscarAuditoria(FiltroActividadAuditoriaDTO filtro,
            int first, int pageSize) {
        Alcance alcance = resolverAlcance();
        return conIdentidad(procesoFacade.buscarAuditoria(filtro, alcance.usuario(), alcance.sinAdministradores(),
                first, pageSize));
    }

    /**
     * Convierte las actividades a DTO y completa nombre y roles de sus usuarios con una
     * sola consulta para todos los usuarios distintos (sin N+1).
     */
    private List<ActividadAuditoriaDTO> conIdentidad(List<Proceso> registros) {
        List<ActividadAuditoriaDTO> resultado = new ArrayList<>();
        Set<String> usuarios = new LinkedHashSet<>();
        for (Proceso registro : registros) {
            ActividadAuditoriaDTO actividad = ActividadAuditoriaDTO.fromEntity(registro);
            resultado.add(actividad);
            if (actividad.getUsuario() != null) usuarios.add(actividad.getUsuario());
        }
        Map<String, UsuarioAuditoriaDTO> identidades = procesoFacade.buscarIdentidades(usuarios);
        for (ActividadAuditoriaDTO actividad : resultado) actividad.aplicarIdentidad(identidades.get(actividad.getUsuario()));
        return resultado;
    }

    @RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal",
        "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa"})
    public int contarAuditoria(FiltroActividadAuditoriaDTO filtro) {
        Alcance alcance = resolverAlcance();
        return procesoFacade.contarAuditoria(filtro, alcance.usuario(), alcance.sinAdministradores());
    }

    /**
     * Catálogo de usuarios para el filtro: completo para Administrador, sin
     * Administradores para Tribunal; los demás roles no lo reciben.
     */
    @RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal",
        "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa"})
    public List<UsuarioAuditoriaDTO> listarUsuariosAuditoria() {
        Alcance alcance = resolverAlcance();
        if (alcance.usuario() != null) return List.of();
        List<String> usuarios = procesoFacade.listarUsuariosConActividad(alcance.sinAdministradores());
        Map<String, UsuarioAuditoriaDTO> identidades = procesoFacade.buscarIdentidades(usuarios);
        List<UsuarioAuditoriaDTO> opciones = new ArrayList<>();
        for (String usuario : usuarios) {
            UsuarioAuditoriaDTO identidad = identidades.get(usuario);
            opciones.add(identidad != null ? identidad : new UsuarioAuditoriaDTO(usuario, null));
        }
        return opciones;
    }

    /** Todas las actividades del filtro (no solo la página visible), con nombre y roles. */
    @RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal",
        "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa"})
    public List<ActividadAuditoriaDTO> listarAuditoriaParaExportar(FiltroActividadAuditoriaDTO filtro) {
        int total = contarAuditoria(filtro);
        Alcance alcance = resolverAlcance();
        return total == 0 ? List.of()
                : conIdentidad(procesoFacade.buscarAuditoria(filtro, alcance.usuario(), alcance.sinAdministradores(),
                        0, total));
    }

    /**
     * Alcance de la bitácora del principal EJB. {@code usuario} no nulo limita a sus
     * propios registros. Si tiene ambos roles, prevalece Administrador.
     */
    private record Alcance(String usuario, boolean sinAdministradores) { }

    private Alcance resolverAlcance() {
        if (sessionContext.getCallerPrincipal() == null
                || sessionContext.getCallerPrincipal().getName() == null
                || sessionContext.getCallerPrincipal().getName().isBlank()) {
            throw new NegocioException("No existe un usuario autenticado para consultar la auditoría.");
        }
        if (sessionContext.isCallerInRole("SITEC-Administrador")) return new Alcance(null, false);
        if (sessionContext.isCallerInRole("SITEC-Tribunal")) return new Alcance(null, true);
        return new Alcance(sessionContext.getCallerPrincipal().getName(), false);
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
