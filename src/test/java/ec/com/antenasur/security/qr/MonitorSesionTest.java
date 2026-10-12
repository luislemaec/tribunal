package ec.com.antenasur.security.qr;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.security.sesion.PingSesionServlet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Inactividad de la sesión web (tec-sesion.js + /sesion/ping, que reemplazaron a p:idleMonitor):
 * mismo plazo que el servidor, aviso y cierre antes de que el servidor destruya la sesión,
 * renovación mientras hay actividad y sin ping en las sesiones QR.
 */
class MonitorSesionTest {

    private static final String PF = "http://primefaces.org/ui";
    private static final String H = "jakarta.faces.html";
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void laVistaIniciaElMonitorSoloConSesionYConElTimeoutDelServidor() throws Exception {
        Document doc = xml(WEBAPP.resolve("WEB-INF/navegacion.xhtml"));
        Element form = (Element) doc.getElementsByTagNameNS(H, "form").item(0);
        assertNotNull(form, "navegacion.xhtml debe declarar el formulario del monitor de sesión");
        assertTrue(form.getAttribute("rendered").contains("loginBean.loggedIn"));
        assertTrue(form.getAttribute("rendered").contains("tiempoSession gt 0"));

        Map<String, Element> comandos = new HashMap<>();
        var lista = form.getElementsByTagNameNS(PF, "remoteCommand");
        for (int i = 0; i < lista.getLength(); i++) {
            Element c = (Element) lista.item(i);
            comandos.put(c.getAttribute("name"), c);
        }
        Element cierre = comandos.get("tecCerrarSesionExpirada");
        assertNotNull(cierre, "el cierre por inactividad pasa por LoginBean (bitácora y login.jsf?expirado=1)");
        assertEquals("#{loginBean.cerrarSessionExpirada()}", cierre.getAttribute("action"));
        assertEquals("false", cierre.getAttribute("global"), "no debe contar como actividad del usuario");
        Element mantener = comandos.get("tecMantenerSesion");
        assertNotNull(mantener, "las sesiones QR se renuevan con una petición JSF, sin /sesion/ping");
        assertTrue(mantener.getAttribute("oncomplete").contains("TecSesion.contactoRenovado"));

        String script = form.getElementsByTagNameNS(H, "outputScript").item(0).getTextContent();
        assertTrue(script.contains("TecSesion.iniciar("));
        // HttpSession expresa el tiempo en segundos; tec-sesion.js lo convierte a milisegundos.
        assertTrue(script.contains("timeoutSegundos: #{loginBean.tiempoSession}"));
        assertTrue(script.contains("'#{request.contextPath}/sesion/ping'"));
        assertTrue(script.contains("tec.contextoQr") && script.contains("TEC-QR"),
                "sin ping en las sesiones QR: su filtro invalidaría la sesión");
        assertTrue(script.contains("login.jsf?expirado=1"));
    }

    @Test
    void laPlantillaCargaElScriptYGlobalsTieneElAviso() throws Exception {
        String plantilla = Files.readString(WEBAPP.resolve("WEB-INF/template.xhtml"), StandardCharsets.UTF_8);
        assertTrue(plantilla.contains("name=\"js/tec-sesion.js\""));
        assertTrue(plantilla.contains("/WEB-INF/navegacion.xhtml"));

        Document globals = xml(WEBAPP.resolve("WEB-INF/globals.xhtml"));
        var dialogos = globals.getElementsByTagNameNS(PF, "dialog");
        Element aviso = null;
        for (int i = 0; i < dialogos.getLength(); i++) {
            Element d = (Element) dialogos.item(i);
            if ("dlgIdle".equals(d.getAttribute("widgetVar"))) {
                aviso = d;
            }
        }
        assertNotNull(aviso, "tec-sesion.js abre el diálogo dlgIdle antes del cierre");
        assertEquals("false", aviso.getAttribute("closable"), "solo «Continuar» extiende la sesión");
        assertTrue(aviso.getTextContent().contains("tecSesionSegundos") || aviso.getElementsByTagName("span").getLength() > 0);
    }

    @Test
    void elScriptAvisaYCierraAntesQueElServidor() throws Exception {
        String js = Files.readString(WEBAPP.resolve("resources/demo/js/tec-sesion.js"), StandardCharsets.UTF_8);
        assertTrue(js.contains("opciones.timeoutSegundos * 1000"), "segundos del servidor a milisegundos");
        long margenCierre = constante(js, "MARGEN_CIERRE_MS");
        long anticipoAviso = constante(js, "ANTICIPO_AVISO_MS");
        long intervaloPing = constante(js, "INTERVALO_PING_MS");

        Document web = xml(WEBAPP.resolve("WEB-INF/web.xml"));
        long timeoutMs = Long.parseLong(web.getElementsByTagNameNS("*", "session-timeout").item(0).getTextContent().trim())
                * 60_000L;
        assertEquals(900_000L, timeoutMs, "15 minutos, igual que la App móvil");
        assertTrue(margenCierre > 0, "el navegador cierra antes de que el servidor destruya la sesión");
        assertTrue(timeoutMs - margenCierre > anticipoAviso, "el aviso aparece antes del cierre");
        assertTrue(intervaloPing < timeoutMs - margenCierre - anticipoAviso,
                "con actividad, la sesión se renueva antes de llegar al aviso");
        assertTrue(js.contains("if (avisoVisible || cerrando)"),
                "con el aviso abierto, mover el ratón no extiende la sesión");
        assertTrue(js.contains("dlgIdle"));
    }

    @Test
    void elPingRenuevaSoloSesionesIniciadasYNoCreaSesiones() throws Exception {
        LoginBean iniciado = new LoginBean();
        var campo = LoginBean.class.getDeclaredField("loggedIn");
        campo.setAccessible(true);
        campo.set(iniciado, true);

        assertEquals(204, ping(sesion(iniciado), true));
        assertEquals(401, ping(null, true), "sin sesión");
        assertEquals(401, ping(sesion(new LoginBean()), true), "sesión sin iniciar");
        assertEquals(401, ping(sesion(iniciado), false), "sin principal autenticado");
    }

    // ------------------------------------------------------------

    private int ping(HttpSession sesion, boolean conPrincipal) throws Exception {
        int[] estado = { 0 };
        Map<String, String> cabeceras = new HashMap<>();
        HttpServletRequest peticion = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { HttpServletRequest.class }, (p, m, a) -> switch (m.getName()) {
                    case "getSession" -> {
                        assertEquals(false, a[0], "el ping nunca crea una sesión");
                        yield sesion;
                    }
                    case "getUserPrincipal" -> conPrincipal ? (Principal) () -> "usuario" : null;
                    default -> throw new UnsupportedOperationException(m.getName());
                });
        HttpServletResponse respuesta = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { HttpServletResponse.class }, (p, m, a) -> switch (m.getName()) {
                    case "setHeader" -> cabeceras.put((String) a[0], (String) a[1]);
                    case "setStatus" -> {
                        estado[0] = (Integer) a[0];
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(m.getName());
                });
        var doGet = PingSesionServlet.class.getDeclaredMethod("doGet", HttpServletRequest.class, HttpServletResponse.class);
        doGet.setAccessible(true);
        doGet.invoke(new PingSesionServlet(), peticion, respuesta);
        assertEquals("no-store", cabeceras.get("Cache-Control"));
        return estado[0];
    }

    private HttpSession sesion(LoginBean bean) {
        return (HttpSession) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { HttpSession.class },
                (p, m, a) -> {
                    assertEquals("getAttribute", m.getName());
                    return "loginBean".equals(a[0]) ? bean : null;
                });
    }

    private static long constante(String js, String nombre) {
        Matcher m = Pattern.compile("var " + nombre + "\\s*=\\s*(\\d+);").matcher(js);
        assertTrue(m.find(), nombre + " no encontrado en tec-sesion.js");
        return Long.parseLong(m.group(1));
    }

    private static Document xml(Path archivo) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(archivo.toFile());
    }
}
