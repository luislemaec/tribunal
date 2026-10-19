package ec.com.antenasur.security.api;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ec.com.antenasur.facade.tec.ReporteActividadFacade;
import ec.com.antenasur.security.monitoreo.MonitoreoEnLineaServlet;
import ec.com.antenasur.security.sesion.SesionesWebEnLinea;
import ec.com.antenasur.util.LoginFilterExcluder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Monitoreo de usuarios en línea sin sesión: desactivado sin clave, solo HTTPS, clave en
 * cabecera, límite por IP y respuesta con totales por canal sin nombres.
 */
class MonitoreoEnLineaTest {

    private static final String CLAVE = "clave-de-monitoreo-de-prueba-0123456789";

    private final List<String> app = new ArrayList<>();
    private final List<SesionesWebEnLinea.SesionWeb> web = new ArrayList<>();
    private MonitoreoEnLineaServlet servlet;

    @BeforeEach
    void preparar() throws Exception {
        System.setProperty(MonitoreoEnLineaServlet.PROPIEDAD, CLAVE);
        servlet = new MonitoreoEnLineaServlet();
        inyectar("sesionesWeb", new SesionesWebEnLinea() {
            @Override
            public List<SesionWeb> vigentes() {
                return web;
            }
        });
        inyectar("reporteActividadFacade", new ReporteActividadFacade() {
            @Override
            public List<Object[]> usuariosAppEnLinea(Instant usadaDesde) {
                List<Object[]> filas = new ArrayList<>();
                app.forEach(u -> filas.add(new Object[] { u, Instant.now() }));
                return filas;
            }
        });
    }

    @AfterEach
    void limpiar() {
        System.clearProperty(MonitoreoEnLineaServlet.PROPIEDAD);
    }

    @Test
    void devuelveTotalesPorCanalSinNombres() throws Exception {
        web.add(new SesionesWebEnLinea.SesionWeb("tribunal1", Instant.now()));
        web.add(new SesionesWebEnLinea.SesionWeb("presidente7", Instant.now()));
        app.add("presidente7"); // web y App a la vez: una sola persona
        app.add("iglesia3");
        Respuesta r = llamar(CLAVE, true, "10.0.0.1");
        assertEquals(200, r.estado);
        assertTrue(r.cuerpo.startsWith("{\"web\":2,\"app\":2,\"usuarios\":3,\"actualizado\":\""), r.cuerpo);
        assertFalse(r.cuerpo.contains("tribunal1") || r.cuerpo.contains("presidente7"), "sin nombres");
        assertEquals("no-store", r.cabeceras.get("Cache-Control"));
    }

    @Test
    void sinClaveConfiguradaElEndpointNoExiste() throws Exception {
        System.clearProperty(MonitoreoEnLineaServlet.PROPIEDAD);
        assertEquals(404, llamar(CLAVE, true, "10.0.0.1").estado);
        System.setProperty(MonitoreoEnLineaServlet.PROPIEDAD, "corta");
        assertEquals(404, llamar("corta", true, "10.0.0.1").estado, "una clave débil no lo activa");
    }

    @Test
    void exigeHttpsYLaClaveCorrecta() throws Exception {
        assertEquals(403, llamar(CLAVE, false, "10.0.0.2").estado);
        assertEquals(401, llamar(null, true, "10.0.0.2").estado);
        assertEquals(401, llamar(CLAVE + "x", true, "10.0.0.2").estado);
        assertEquals(200, llamar(CLAVE, true, "10.0.0.2").estado);
    }

    @Test
    void limitaLasPeticionesPorIp() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertEquals(401, llamar("incorrecta", true, "10.0.0.3").estado);
        }
        assertEquals(429, llamar(CLAVE, true, "10.0.0.3").estado, "ni con la clave correcta tras el límite");
        assertEquals(200, llamar(CLAVE, true, "10.0.0.4").estado, "otra IP no se ve afectada");
    }

    @Test
    void laRutaExactaNoPasaPorElLoginWeb() {
        LoginFilterExcluder excluder = LoginFilterExcluder.getInstance("");
        assertTrue(excluder.isExcludeUrl("/monitoreo/en-linea"));
        assertFalse(excluder.isExcludeUrl("/monitoreo/otra"), "solo la ruta exacta");
    }

    // ------------------------------------------------------------

    private static final class Respuesta {
        int estado = 200;
        String cuerpo = "";
        final Map<String, String> cabeceras = new HashMap<>();
    }

    private Respuesta llamar(String clave, boolean segura, String ip) throws Exception {
        Respuesta r = new Respuesta();
        StringWriter cuerpo = new StringWriter();
        PrintWriter escritor = new PrintWriter(cuerpo);
        HttpServletResponse respuesta = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { HttpServletResponse.class }, (p, m, a) -> switch (m.getName()) {
                    case "setHeader" -> r.cabeceras.put((String) a[0], (String) a[1]);
                    case "setStatus" -> { r.estado = (Integer) a[0]; yield null; }
                    case "sendError" -> { r.estado = (Integer) a[0]; yield null; }
                    case "getWriter" -> escritor;
                    case "setContentType", "setCharacterEncoding" -> null;
                    default -> throw new UnsupportedOperationException(m.getName());
                });
        HttpServletRequest peticion = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { HttpServletRequest.class }, (p, m, a) -> switch (m.getName()) {
                    case "getMethod" -> "GET";
                    case "isSecure" -> segura;
                    case "getRemoteAddr" -> ip;
                    case "getHeader" -> MonitoreoEnLineaServlet.CABECERA.equals(a[0]) ? clave : null;
                    case "getProtocol" -> "HTTP/1.1";
                    default -> throw new UnsupportedOperationException(m.getName());
                });
        servlet.service(peticion, respuesta);
        escritor.flush();
        r.cuerpo = cuerpo.toString();
        return r;
    }

    private void inyectar(String nombre, Object valor) throws Exception {
        var campo = MonitoreoEnLineaServlet.class.getDeclaredField(nombre);
        campo.setAccessible(true);
        campo.set(servlet, valor);
    }
}
