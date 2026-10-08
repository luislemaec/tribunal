package ec.com.antenasur.security.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.wildfly.security.auth.server.SecurityIdentity;

import ec.com.antenasur.service.tec.SesionMovilService;
import ec.com.antenasur.service.tec.SesionMovilService.SesionValida;

/**
 * Puerta de la API móvil (/api/*). Solo HTTPS; nunca crea HttpSession ni cookies.
 *
 * <ul>
 * <li>{@code /auth/login} y {@code /auth/refresh} son públicos (tienen su propio control).</li>
 * <li>El resto exige {@code Authorization: Bearer <token>} válido (SesionMovilService).</li>
 * <li>Una sesión de cambio de clave obligatorio solo puede cambiar la clave, consultar su
 * perfil o cerrar sesión.</li>
 * <li>La petición se ejecuta con la identidad Elytron del usuario ({@link IdentidadTec}): los
 * EJB autorizan con sus propios {@code @RolesAllowed}, igual que en la web.</li>
 * </ul>
 */
@WebFilter(urlPatterns = "/api/*")
public class FiltroApiMovil implements Filter {

    /** Atributo de la petición con la {@link SesionValida} del token. */
    public static final String ATRIBUTO_SESION = FiltroApiMovil.class.getName() + ".sesion";

    static final String PREFIJO = "/api/v1";
    static final Set<String> RUTAS_PUBLICAS = Set.of(PREFIJO + "/auth/login", PREFIJO + "/auth/refresh");
    static final Set<String> RUTAS_CAMBIO_CLAVE = Set.of(PREFIJO + "/auth/cambiar-clave", PREFIJO + "/auth/yo",
            PREFIJO + "/auth/logout");

    @Inject
    private SesionMovilService sesiones;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest peticion = (HttpServletRequest) req;
        HttpServletResponse respuesta = (HttpServletResponse) res;
        respuesta.setHeader("Cache-Control", "no-store");
        respuesta.setHeader("Pragma", "no-cache");

        if (!peticion.isSecure()) {
            error(respuesta, HttpServletResponse.SC_FORBIDDEN, "HTTPS_REQUERIDO", "La API solo acepta conexiones HTTPS.");
            return;
        }
        String ruta = peticion.getRequestURI().substring(peticion.getContextPath().length());
        if (RUTAS_PUBLICAS.contains(ruta)) {
            chain.doFilter(req, res);
            return;
        }

        SesionValida sesion = sesiones.validarAcceso(tokenBearer(peticion));
        if (sesion == null) {
            respuesta.setHeader("WWW-Authenticate", "Bearer");
            error(respuesta, HttpServletResponse.SC_UNAUTHORIZED, "SESION_INVALIDA",
                    "La sesión no es válida o expiró. Inicie sesión nuevamente.");
            return;
        }
        if (sesion.soloCambioClave() && !RUTAS_CAMBIO_CLAVE.contains(ruta)) {
            error(respuesta, HttpServletResponse.SC_FORBIDDEN, "CAMBIO_CLAVE_OBLIGATORIO",
                    "Debe cambiar su contraseña antes de continuar.");
            return;
        }
        SecurityIdentity identidad;
        try {
            identidad = IdentidadTec.identidadDe(sesion.usuario());
        } catch (Exception e) {
            throw new ServletException("No se pudo establecer la identidad del usuario.", e);
        }
        if (identidad == null) {
            respuesta.setHeader("WWW-Authenticate", "Bearer");
            error(respuesta, HttpServletResponse.SC_UNAUTHORIZED, "SESION_INVALIDA",
                    "La sesión no es válida o expiró. Inicie sesión nuevamente.");
            return;
        }
        peticion.setAttribute(ATRIBUTO_SESION, sesion);
        try {
            identidad.runAs((PrivilegedExceptionAction<Void>) () -> {
                chain.doFilter(req, res);
                return null;
            });
        } catch (PrivilegedActionException e) {
            Throwable causa = e.getCause();
            if (causa instanceof IOException io) throw io;
            if (causa instanceof ServletException se) throw se;
            if (causa instanceof RuntimeException re) throw re;
            throw new ServletException(causa);
        }
    }

    /** Token de la cabecera {@code Authorization: Bearer <token>}; {@code null} si falta. */
    static String tokenBearer(HttpServletRequest peticion) {
        String cabecera = peticion.getHeader("Authorization");
        if (cabecera == null || !cabecera.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = cabecera.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    static void error(HttpServletResponse respuesta, int estado, String codigo, String mensaje) throws IOException {
        respuesta.setStatus(estado);
        respuesta.setContentType("application/json");
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        respuesta.getWriter().write("{\"codigo\":\"" + codigo + "\",\"mensaje\":\"" + mensaje + "\"}");
    }
}
