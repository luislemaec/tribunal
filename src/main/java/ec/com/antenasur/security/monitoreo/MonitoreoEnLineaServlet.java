package ec.com.antenasur.security.monitoreo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import ec.com.antenasur.facade.tec.ReporteActividadFacade;
import ec.com.antenasur.security.api.LimiteIntentosApi;
import ec.com.antenasur.security.sesion.SesionesWebEnLinea;
import ec.com.antenasur.service.tec.ReporteActividadService;
import lombok.extern.slf4j.Slf4j;

/**
 * Monitoreo externo de usuarios en línea, sin iniciar sesión en TEC:
 * {@code GET /monitoreo/en-linea} con la cabecera {@code X-Monitoreo-Clave}. Responde solo
 * totales por canal, sin nombres ni datos personales:
 * <pre>{"web": 5, "app": 3, "usuarios": 7, "actualizado": "2026-10-15T18:30:00Z"}</pre>
 * {@code usuarios} cuenta personas distintas (quien usa web y App a la vez cuenta una vez).
 * Mismas reglas que la pestaña Actividad: web = sesiones autenticadas vigentes de este nodo
 * ({@link SesionesWebEnLinea}); App = sesiones vigentes usadas en los últimos
 * {@value ReporteActividadService#MINUTOS_APP_EN_LINEA} minutos.
 *
 * <p>Seguridad:</p>
 * <ul>
 * <li>Desactivado por defecto: sin la propiedad {@value #PROPIEDAD} (mínimo
 * {@value #LARGO_MINIMO} caracteres) responde 404, como si no existiera.</li>
 * <li>La clave va solo en cabecera (nunca en la URL, que queda en los logs) y se compara en
 * tiempo constante. Solo HTTPS. Máximo 10 peticiones por minuto por IP.</li>
 * <li>No crea sesión HTTP ni usa la identidad Elytron: no da acceso a nada más. LoginFilter
 * excluye esta ruta exacta.</li>
 * </ul>
 */
@Slf4j
@WebServlet("/monitoreo/en-linea")
public class MonitoreoEnLineaServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** Propiedad del sistema con la clave de monitoreo (p. ej. system-property de WildFly). */
    public static final String PROPIEDAD = "tec.monitoreo.clave";

    public static final String CABECERA = "X-Monitoreo-Clave";

    static final int LARGO_MINIMO = 32;

    @Inject
    private SesionesWebEnLinea sesionesWeb;

    @Inject
    private ReporteActividadFacade reporteActividadFacade;

    /** Ventana propia (no comparte contadores con el login de la API): 10 por minuto por IP. */
    private final transient LimiteIntentosApi limite = new LimiteIntentosApi();

    @Override
    protected void doGet(HttpServletRequest peticion, HttpServletResponse respuesta) throws IOException {
        respuesta.setHeader("Cache-Control", "no-store");
        respuesta.setHeader("Pragma", "no-cache");
        String clave = System.getProperty(PROPIEDAD);
        if (clave == null || clave.strip().length() < LARGO_MINIMO) {
            respuesta.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!peticion.isSecure()) {
            error(respuesta, HttpServletResponse.SC_FORBIDDEN, "HTTPS_REQUERIDO");
            return;
        }
        if (!limite.permitir(peticion.getRemoteAddr(), null)) {
            error(respuesta, 429, "DEMASIADOS_INTENTOS");
            return;
        }
        if (!coincide(peticion.getHeader(CABECERA), clave.strip())) {
            log.warn("Monitoreo en línea: clave rechazada desde {}", peticion.getRemoteAddr());
            error(respuesta, HttpServletResponse.SC_UNAUTHORIZED, "CLAVE_INVALIDA");
            return;
        }
        Set<String> web = new HashSet<>();
        sesionesWeb.vigentes().forEach(s -> web.add(s.usuario().toLowerCase(java.util.Locale.ROOT)));
        Set<String> app = new HashSet<>();
        Instant usadaDesde = Instant.now().minus(ReporteActividadService.MINUTOS_APP_EN_LINEA, ChronoUnit.MINUTES);
        for (Object[] fila : reporteActividadFacade.usuariosAppEnLinea(usadaDesde)) {
            app.add(String.valueOf(fila[0]).toLowerCase(java.util.Locale.ROOT));
        }
        Set<String> usuarios = new HashSet<>(web);
        usuarios.addAll(app);
        respuesta.setContentType("application/json");
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        respuesta.getWriter().write("{\"web\":" + web.size() + ",\"app\":" + app.size() + ",\"usuarios\":"
                + usuarios.size() + ",\"actualizado\":\"" + Instant.now().truncatedTo(ChronoUnit.SECONDS) + "\"}");
    }

    /** Comparación en tiempo constante (sobre el hash: no revela el largo de la clave). */
    static boolean coincide(String recibida, String esperada) {
        if (recibida == null || recibida.isBlank()) {
            return false;
        }
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] a = sha.digest(recibida.strip().getBytes(StandardCharsets.UTF_8));
            byte[] b = sha.digest(esperada.getBytes(StandardCharsets.UTF_8));
            return MessageDigest.isEqual(a, b);
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }

    private static void error(HttpServletResponse respuesta, int estado, String codigo) throws IOException {
        respuesta.setStatus(estado);
        respuesta.setContentType("application/json");
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        respuesta.getWriter().write("{\"codigo\":\"" + codigo + "\"}");
    }
}
