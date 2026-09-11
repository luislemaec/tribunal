package ec.com.antenasur.security.qr;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DiagnosticoQrTest {
    @Test void distingueTransporteSinRelajarSeguridad() {
        String enabled = System.getProperty("tec.qr.enabled");
        String origen = System.getProperty("tec.qr.public.base-url");
        try {
            System.setProperty("tec.qr.enabled", "false");
            assertEquals("QR_DESHABILITADO", causa(true, "tribunal.conpociiech.org", 443));
            System.setProperty("tec.qr.enabled", "true");
            assertEquals("HTTPS_NO_RECONOCIDO", causa(false, "tribunal.conpociiech.org", 443));
            System.clearProperty("tec.qr.public.base-url");
            assertEquals("ORIGEN_CONFIGURADO_INVALIDO", causa(true, "tribunal.conpociiech.org", 443));
            System.setProperty("tec.qr.public.base-url", "https://tribunal.conpociiech.org");
            assertTrue(AccesoActaServlet.origenPermitido("https://tribunal.conpociiech.org"));
            assertFalse(AccesoActaServlet.origenPermitido(null));
            assertFalse(AccesoActaServlet.origenPermitido("null"));
            assertFalse(AccesoActaServlet.origenPermitido("https://otro.example"));
            assertFalse(AccesoActaServlet.origenPermitido("http://tribunal.conpociiech.org"));
            assertEquals("HOST_PUERTO_NO_AUTORIZADO", causa(true, "localhost", 443));
            assertEquals("HOST_PUERTO_NO_AUTORIZADO", causa(true, "tribunal.conpociiech.org", 8080));
            assertNull(causa(true, "tribunal.conpociiech.org", 443));
        } finally {
            restaurar("tec.qr.enabled", enabled);
            restaurar("tec.qr.public.base-url", origen);
        }
    }

    @Test void diagnosticoNoIncluyeMensajesSensibles() {
        var error = new IllegalStateException("token secreto", new SecurityException("credencial secreta"));
        assertEquals("java.lang.IllegalStateException > java.lang.SecurityException", DiagnosticoQr.tipoExcepcion(error));
    }

    @Test void formularioUsaSameOriginSinCambiarPoliticaDeErrores() throws Exception {
        var pagina = AccesoActaServlet.class.getDeclaredMethod("pagina",
                jakarta.servlet.http.HttpServletResponse.class, String.class, String.class, String.class, int.class);
        pagina.setAccessible(true);
        var headers = new java.util.HashMap<String, String>();
        var writer = new java.io.PrintWriter(new java.io.StringWriter());
        var response = (jakarta.servlet.http.HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{jakarta.servlet.http.HttpServletResponse.class}, (proxy, method, args) -> {
                    if (method.getName().equals("setHeader")) headers.put((String) args[0], (String) args[1]);
                    if (method.getName().equals("getWriter")) return writer;
                    return null;
                });
        headers.put("Referrer-Policy", "no-referrer");
        pagina.invoke(null, response, "", TokenActaQr.generar(), "actaQr.confirmacion", 200);
        assertEquals("same-origin", headers.get("Referrer-Policy"));
        headers.put("Referrer-Policy", "no-referrer");
        pagina.invoke(null, response, "", null, "actaQr.error.acceso", 403);
        assertEquals("no-referrer", headers.get("Referrer-Policy"));
    }

    private static String causa(boolean seguro, String host, int puerto) {
        var request = (HttpServletRequest) Proxy.newProxyInstance(DiagnosticoQrTest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isSecure" -> seguro;
                    case "getServerName" -> host;
                    case "getServerPort" -> puerto;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return AccesoActaServlet.causaTransporte(request);
    }

    private static void restaurar(String nombre, String valor) {
        if (valor == null) System.clearProperty(nombre); else System.setProperty(nombre, valor);
    }
}
