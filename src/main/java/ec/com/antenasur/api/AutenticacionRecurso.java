package ec.com.antenasur.api;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.wildfly.security.auth.server.SecurityIdentity;

import ec.com.antenasur.api.ContratosApi.ErrorApi;
import ec.com.antenasur.api.ContratosApi.PerfilUsuario;
import ec.com.antenasur.api.ContratosApi.RespuestaSesion;
import ec.com.antenasur.api.ContratosApi.SolicitudCambioClave;
import ec.com.antenasur.api.ContratosApi.SolicitudLogin;
import ec.com.antenasur.api.ContratosApi.SolicitudRefresh;
import ec.com.antenasur.bean.PlantillaCorreoBean;
import ec.com.antenasur.dto.AuthDataDTO;
import ec.com.antenasur.dto.UsuarioDTO;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.model.tec.Proceso;
import ec.com.antenasur.security.api.FiltroApiMovil;
import ec.com.antenasur.security.api.IdentidadTec;
import ec.com.antenasur.security.api.LimiteIntentosApi;
import ec.com.antenasur.security.qr.ConfiguracionQr;
import ec.com.antenasur.service.UsuarioService;
import ec.com.antenasur.service.tec.CorreoService;
import ec.com.antenasur.service.tec.ProcesoService;
import ec.com.antenasur.service.tec.SesionMovilService;
import ec.com.antenasur.service.tec.SesionMovilService.SesionValida;
import ec.com.antenasur.service.tec.SesionMovilService.TokensEmitidos;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.JsfUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Autenticación de la App móvil (docs/api-movil.md). Reutiliza las mismas reglas del login
 * web: Elytron verifica la clave (BCrypt), {@code UsuarioService.cargarContextoUsuarioAutenticado}
 * exige usuario activo con rol TEC y un usuario no permanente debe cambiar su clave primero.
 * Nunca registra claves ni tokens.
 */
@Path("/auth")
@RequestScoped
@Produces(ApiMovilAplicacion.JSON)
@Slf4j
public class AutenticacionRecurso {

    private static final String TIPO_TOKEN = "Bearer";
    private static final int MAX_USUARIO = 100;
    private static final int MAX_CLAVE = 256;

    // Mismo formato estructurado que el login web: la bitácora los muestra como
    // «Inició sesión», «Cerró sesión» y «Cambió contraseña» (CatalogoActividades).
    private static final String AUDITORIA_LOGIN =
            "LOGIN | MÓDULO: ACCESO; RESULTADO: EXITOSO; DETALLE: Inicio de sesión desde la App móvil";
    private static final String AUDITORIA_LOGOUT =
            "LOGOUT | MÓDULO: ACCESO; RESULTADO: EXITOSO; DETALLE: Cierre de sesión desde la App móvil";
    private static final String AUDITORIA_CAMBIO_CLAVE =
            "LOGOUT | MÓDULO: ACCESO; RESULTADO: EXITOSO; DETALLE: Cierre tras cambio de clave desde la App móvil";

    @Inject
    private SesionMovilService sesiones;

    @Inject
    private UsuarioService usuarioService;

    @Inject
    private ProcesoService procesoService;

    @Inject
    private LimiteIntentosApi limite;

    @Inject
    private CorreoService correoService;

    @Inject
    private PlantillaCorreoBean plantillaCorreoBean;

    @Context
    private HttpServletRequest peticion;

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(SolicitudLogin solicitud) {
        String usuario = solicitud == null || solicitud.getUsuario() == null ? "" : solicitud.getUsuario().trim();
        String clave = solicitud == null ? null : solicitud.getClave();
        if (usuario.isEmpty() || usuario.length() > MAX_USUARIO || clave == null || clave.isEmpty()
                || clave.length() > MAX_CLAVE) {
            return error(Response.Status.BAD_REQUEST, "SOLICITUD_INVALIDA", "Ingrese su usuario y contraseña.");
        }
        String ip = peticion.getRemoteAddr();
        if (!limite.permitir(ip, usuario)) {
            return error(Response.Status.TOO_MANY_REQUESTS, "DEMASIADOS_INTENTOS",
                    "Demasiados intentos. Espere un minuto e inténtelo de nuevo.");
        }
        // Las identidades efímeras del QR (TECQR:) no pueden iniciar sesión por la API.
        if (usuario.startsWith(ConfiguracionQr.PREFIJO)) {
            return rechazarCredenciales(usuario, ip);
        }

        SecurityIdentity identidad;
        try {
            identidad = IdentidadTec.autenticar(usuario, clave.toCharArray());
        } catch (Exception e) {
            log.error("No se pudo verificar las credenciales en el realm de TEC", e);
            return errorInterno();
        }
        if (identidad == null) {
            return rechazarCredenciales(usuario, ip);
        }
        AuthDataDTO contexto = contexto(identidad, usuario);
        if (contexto == null || !contexto.isResolved()) {
            return rechazarCredenciales(usuario, ip);
        }
        boolean soloCambioClave = !Boolean.TRUE.equals(contexto.getUsuario().getPermanente());
        TokensEmitidos tokens = sesiones.emitir(contexto.getUsuario().getId(), soloCambioClave, ip,
                solicitud.getDispositivo());
        auditar(identidad, ip, AUDITORIA_LOGIN);
        return Response.ok(respuesta(tokens, contexto)).build();
    }

    @POST
    @Path("/refresh")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response refresh(SolicitudRefresh solicitud) {
        TokensEmitidos tokens = sesiones.renovar(solicitud == null ? null : solicitud.getRefreshToken());
        if (tokens == null) {
            return sesionInvalida();
        }
        SecurityIdentity identidad;
        try {
            identidad = IdentidadTec.identidadDe(tokens.usuario());
        } catch (Exception e) {
            log.error("No se pudo establecer la identidad al renovar la sesión móvil", e);
            return errorInterno();
        }
        AuthDataDTO contexto = identidad == null ? null : contexto(identidad, tokens.usuario());
        if (contexto == null || !contexto.isResolved()) {
            // Usuario sin roles TEC vigentes: la sesión deja de ser válida.
            sesiones.revocar(tokens.sesionId(), SesionMovilService.MOTIVO_SIN_ROL);
            return sesionInvalida();
        }
        return Response.ok(respuesta(tokens, contexto)).build();
    }

    /** Perfil del usuario autenticado (se ejecuta con su identidad: FiltroApiMovil). */
    @GET
    @Path("/yo")
    public Response yo() {
        SesionValida sesion = sesionActual();
        AuthDataDTO contexto = usuarioService.cargarContextoUsuarioAutenticado(sesion.usuario(), prefijoRoles());
        if (contexto == null || !contexto.isResolved()) {
            sesiones.revocar(sesion.sesionId(), SesionMovilService.MOTIVO_SIN_ROL);
            return sesionInvalida();
        }
        return Response.ok(perfil(contexto, sesion.soloCambioClave())).build();
    }

    /**
     * Cambio de clave con la sesión vigente (también la restringida). Usa la misma validación
     * de la web; al terminar revoca todas las sesiones móviles del usuario y entrega una
     * sesión completa nueva.
     */
    @POST
    @Path("/cambiar-clave")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response cambiarClave(SolicitudCambioClave solicitud) {
        SesionValida sesion = sesionActual();
        String actual = solicitud == null ? null : solicitud.getClaveActual();
        String nueva = solicitud == null ? null : solicitud.getClaveNueva();
        if (actual == null || actual.isEmpty() || nueva == null || nueva.isBlank() || nueva.length() > MAX_CLAVE) {
            return error(Response.Status.BAD_REQUEST, "SOLICITUD_INVALIDA", "Ingrese la contraseña actual y la nueva.");
        }
        // Misma política que la web (8 a 16, sin espacios, mayúscula, minúscula y número).
        if (!JsfUtil.cumplePoliticaContrasenia(nueva)) {
            return error(Response.Status.BAD_REQUEST, "CLAVE_NO_CUMPLE_POLITICA",
                    Constantes.getMensaje("msg.password.politic"));
        }
        UsuarioDTO actualizado;
        try {
            actualizado = usuarioService.cambiarContraseniaAutenticada(sesion.usuarioId(), sesion.usuario(), actual,
                    nueva);
        } catch (NegocioException e) {
            return error(Response.Status.BAD_REQUEST, "CLAVE_REUTILIZADA", e.getMessage());
        }
        if (actualizado == null) {
            return error(Response.Status.BAD_REQUEST, "CLAVE_INCORRECTA", "La contraseña actual no es correcta.");
        }
        // UsuarioService ya revocó todas las sesiones móviles del usuario (incluida esta).
        String ip = peticion.getRemoteAddr();
        TokensEmitidos tokens = sesiones.emitir(sesion.usuarioId(), false, ip, null);
        registrarActividad(ip, AUDITORIA_CAMBIO_CLAVE);
        notificarCambioClave(actualizado);
        AuthDataDTO contexto = usuarioService.cargarContextoUsuarioAutenticado(sesion.usuario(), prefijoRoles());
        return Response.ok(respuesta(tokens, contexto)).build();
    }

    @POST
    @Path("/logout")
    public Response logout() {
        SesionValida sesion = sesionActual();
        sesiones.revocar(sesion.sesionId(), SesionMovilService.MOTIVO_LOGOUT);
        registrarActividad(peticion.getRemoteAddr(), AUDITORIA_LOGOUT);
        return Response.noContent().build();
    }

    // ------------------------------------------------------------------

    private SesionValida sesionActual() {
        Object sesion = peticion.getAttribute(FiltroApiMovil.ATRIBUTO_SESION);
        if (!(sesion instanceof SesionValida valida)) {
            // Solo ocurre si el filtro no se aplicó: se trata como no autenticado.
            throw new IllegalStateException("Petición sin sesión validada por FiltroApiMovil.");
        }
        return valida;
    }

    /** Contexto TEC del usuario (mismas reglas del login web), ejecutado con su identidad. */
    private AuthDataDTO contexto(SecurityIdentity identidad, String usuario) {
        return conIdentidad(identidad,
                () -> usuarioService.cargarContextoUsuarioAutenticado(usuario, prefijoRoles()));
    }

    private void auditar(SecurityIdentity identidad, String ip, String actividad) {
        conIdentidad(identidad, () -> {
            registrarActividad(ip, actividad);
            return null;
        });
    }

    /** Registra en tec.procesos con la identidad vigente (el autor sale del principal EJB). */
    private void registrarActividad(String ip, String actividad) {
        try {
            Proceso proceso = new Proceso(ip);
            proceso.setActividad(actividad);
            procesoService.create(proceso);
        } catch (Exception e) {
            log.error("No se pudo registrar la actividad de la App móvil", e);
        }
    }

    /** Mismo aviso por correo que el cambio de clave web (CambioClaveController). */
    private void notificarCambioClave(UsuarioDTO usuario) {
        try {
            if (usuario.getCorreo() == null || usuario.getCorreo().isBlank()) {
                return;
            }
            HashMap<String, String> parametros = correoService.construirParametrosBase();
            parametros.put("nombreApellido", usuario.getPersonaNombres());
            parametros.put("nombreUsuario", usuario.getUsername());
            correoService.enviarNotificacion(List.of(usuario.getCorreo()), plantillaCorreoBean.obtieneCorreoCambioClave(),
                    parametros, usuario.getId(), Constantes.getPathLogo());
        } catch (Exception e) {
            log.error("No se pudo enviar el aviso de cambio de clave", e);
        }
    }

    private Response rechazarCredenciales(String usuario, String ip) {
        try {
            procesoService.registrarLoginFallidoPreautenticacion(usuario, ip);
        } catch (Exception e) {
            log.error("No se pudo registrar el intento de inicio de sesión fallido", e);
        }
        return error(Response.Status.UNAUTHORIZED, "CREDENCIALES_INVALIDAS", "Usuario o contraseña incorrectos.");
    }

    private static <T> T conIdentidad(SecurityIdentity identidad, PrivilegedExceptionAction<T> accion) {
        try {
            return identidad.runAs(accion);
        } catch (PrivilegedActionException e) {
            Throwable causa = e.getCause();
            if (causa instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException(causa);
        }
    }

    private static String prefijoRoles() {
        return Constantes.getMensaje("roles.sitec");
    }

    private static RespuestaSesion respuesta(TokensEmitidos tokens, AuthDataDTO contexto) {
        return new RespuestaSesion(tokens.accessToken(), tokens.refreshToken(), TIPO_TOKEN, tokens.expiraEnSegundos(),
                tokens.soloCambioClave(), perfil(contexto, tokens.soloCambioClave()));
    }

    private static PerfilUsuario perfil(AuthDataDTO contexto, boolean soloCambioClave) {
        UsuarioDTO usuario = contexto.getUsuario();
        String nombre = ((usuario.getPersonaApellidos() == null ? "" : usuario.getPersonaApellidos().trim()) + " "
                + (usuario.getPersonaNombres() == null ? "" : usuario.getPersonaNombres().trim())).trim();
        List<String> roles = contexto.getNombresRoles() == null ? List.of() : List.copyOf(contexto.getNombresRoles());
        return new PerfilUsuario(usuario.getUsername(), nombre.isEmpty() ? usuario.getUsername() : nombre, roles,
                usuario.getIglesiaId(), soloCambioClave);
    }

    private static Response sesionInvalida() {
        return Response.status(Response.Status.UNAUTHORIZED).header("WWW-Authenticate", TIPO_TOKEN)
                .entity(new ErrorApi("SESION_INVALIDA", "La sesión no es válida o expiró. Inicie sesión nuevamente."))
                .build();
    }

    private static Response errorInterno() {
        return error(Response.Status.INTERNAL_SERVER_ERROR, "ERROR_INTERNO",
                "No se pudo completar la solicitud. Inténtelo más tarde.");
    }

    static Response error(Response.Status estado, String codigo, String mensaje) {
        return Response.status(estado).entity(new ErrorApi(codigo, mensaje)).build();
    }
}
