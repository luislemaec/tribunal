package ec.com.antenasur.security.qr;

import ec.com.antenasur.util.LoginFilter;
import jakarta.servlet.http.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RedireccionSesionTest {
    @Test void postSinSesionRedirigeSinForwardNiCrearSesion() throws Exception {
        var caso = new Caso("/actaE.jsf", "POST", false, false);
        filtrar(caso);
        assertEquals(303, caso.status);
        assertEquals("/tec/login.jsf", caso.headers.get("Location"));
        assertFalse(caso.continua);
    }

    @Test void ajaxSinSesionNavegaConRespuestaParcialSinViewState() throws Exception {
        var caso = new Caso("/actaE.jsf", "POST", true, false);
        filtrar(caso);
        assertEquals(200, caso.status);
        var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var doc = factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(caso.body.toString())));
        assertEquals("/tec/login.jsf", ((org.w3c.dom.Element) doc.getElementsByTagName("redirect").item(0)).getAttribute("url"));
        assertFalse(caso.body.toString().contains("ViewState"));
        assertFalse(caso.continua);
    }

    @Test void postViejoDeLoginRedirigePeroGetDeLoginContinua() throws Exception {
        var viejo = new Caso("/login.jsf", "POST", false, false);
        filtrar(viejo);
        assertEquals(303, viejo.status);
        assertFalse(viejo.continua);
        var nuevo = new Caso("/login.jsf", "GET", false, false);
        filtrar(nuevo);
        assertTrue(nuevo.continua);
    }

    @Test void rutaQrProhibidaDestruyeYRedirigeSinEjecutarCadena() throws Exception {
        var caso = new Caso("/usuarios.jsf", "POST", false, true);
        assertTrue(new ControlHttpSesionQr().procesar(caso.request(), caso.response(), (r,s) -> caso.continua = true));
        assertTrue(caso.logout);
        assertTrue(caso.invalidada);
        assertEquals(303, caso.status);
        assertEquals("/tec/login.jsf", caso.headers.get("Location"));
        assertFalse(caso.continua);
    }

    private static void filtrar(Caso caso) throws Exception {
        var filtro = new LoginFilter();
        var campo = LoginFilter.class.getDeclaredField("controlQr");
        campo.setAccessible(true);
        campo.set(filtro, new ControlHttpSesionQr());
        var permisos = LoginFilter.class.getDeclaredField("autorizacionMenu");
        permisos.setAccessible(true);
        permisos.set(filtro, new ec.com.antenasur.security.menu.AutorizacionMenuService() {
            @Override public java.util.List<String> paginasActuales() {
                return caso.permisosVigentes;
            }
        });
        filtro.doFilter(caso.request(), caso.response(), (r,s) -> caso.continua = true);
    }

    @Test void menuAutorizaPermisosYAsignacionSinVetoPorRol() throws Exception {
        for (String pagina : new String[]{"permisos", "asignacionUsuarios"}) {
            var caso = autenticado(pagina, false);
            filtrar(caso);
            assertTrue(caso.continua, pagina);
        }
        var iglesias = autenticado("iglesias", false);
        filtrar(iglesias);
        assertTrue(iglesias.continua);
    }

    @Test void usuariosYRolesRequierenMenuAutorizadoInclusoParaAdministrador() throws Exception {
        for (boolean administrador : new boolean[]{false, true}) {
            for (String pagina : new String[]{"usuarios", "roles"}) {
                var permitido = autenticado(pagina, administrador);
                filtrar(permitido);
                assertTrue(permitido.continua, pagina);
                var denegado = autenticado(pagina, administrador);
                denegado.atributos.put("listaPermisos", java.util.List.of("dashboard.jsf"));
                denegado.permisosVigentes = java.util.List.of("dashboard.jsf");
                filtrar(denegado);
                assertFalse(denegado.continua, pagina);
                assertEquals("/tec/errors/permisos.jsf", denegado.headers.get("Location"));
            }
        }
    }

    @Test void administradorConPermisoConservaPantallasAdministrativas() throws Exception {
        for (String pagina : new String[]{"usuarios", "roles", "permisos", "asignacionUsuarios"}) {
            var caso = autenticado(pagina, true);
            filtrar(caso);
            assertTrue(caso.continua, pagina);
        }
    }

    private static Caso autenticado(String pagina, boolean administrador) {
        var caso = new Caso("/" + pagina + ".jsf", "GET", false, false);
        caso.sesionNormal = true;
        caso.principal = "usuario";
        caso.administrador = administrador;
        var usuario = new ec.com.antenasur.dto.UsuarioDTO();
        usuario.setId(8);
        usuario.setPermanente(true);
        var login = new ec.com.antenasur.bean.LoginBean();
        login.setUsuario(usuario);
        caso.atributos.put("loginBean", login);
        caso.atributos.put("listaPermisos", java.util.List.of(pagina + ".jsf"));
        caso.permisosVigentes = java.util.List.of(pagina + ".jsf");
        return caso;
    }

    @Test void sesionNormalSinPrincipalSeInvalidaSinEjecutarCadena() throws Exception {
        var caso = new Caso("/dashboard.jsf", "POST", false, false);
        caso.sesionNormal = true;
        filtrar(caso);
        assertTrue(caso.invalidada);
        assertEquals(303, caso.status);
        assertEquals("/tec/login.jsf", caso.headers.get("Location"));
        assertFalse(caso.continua);
    }

    private static final class Caso {
        final String ruta, metodo;
        final boolean ajax, qr;
        boolean continua, logout, invalidada, sesionNormal, administrador;
        String principal;
        java.util.List<String> permisosVigentes = java.util.List.of();
        final HashMap<String,Object> atributos = new HashMap<>();
        int status;
        final HashMap<String,String> headers = new HashMap<>();
        final StringWriter body = new StringWriter();
        Caso(String ruta, String metodo, boolean ajax, boolean qr) {
            this.ruta=ruta; this.metodo=metodo; this.ajax=ajax; this.qr=qr;
        }
        HttpSession sesion() {
            return (HttpSession) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{HttpSession.class},
                    (p,m,a) -> {
                        if (m.getName().equals("getAttribute")) return atributos.get(a[0]);
                        if (m.getName().equals("setAttribute")) { atributos.put((String)a[0], a[1]); return null; }
                        if (m.getName().equals("invalidate")) { invalidada=true; return null; }
                        throw new AssertionError(m.getName());
                    });
        }
        HttpServletRequest request() {
            return (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{HttpServletRequest.class},
                    (p,m,a) -> switch(m.getName()) {
                        case "getCharacterEncoding" -> "UTF-8";
                        case "getSession" -> {
                            assertNotNull(a);
                            assertEquals(false, a[0], "El filtro no debe crear una sesion");
                            yield qr || sesionNormal ? sesion() : null;
                        }
                        case "getContextPath" -> "/tec";
                        case "getRequestURI" -> "/tec" + ruta;
                        case "getRequestURL" -> new StringBuffer("https://tribunal.local/tec" + ruta);
                        case "getServletPath" -> ruta;
                        case "getDispatcherType" -> jakarta.servlet.DispatcherType.REQUEST;
                        case "getMethod" -> metodo;
                        case "getContentType" -> null;
                        case "isSecure" -> true;
                        case "getUserPrincipal" -> principal == null ? null : (java.security.Principal) () -> principal;
                        case "isUserInRole" -> qr || administrador && "SITEC-Administrador".equals(a[0]);
                        case "getHeader" -> ajax && "Faces-Request".equals(a[0]) ? "partial/ajax" : null;
                        case "logout" -> { logout=true; yield null; }
                        default -> throw new AssertionError("Operacion inesperada: " + m.getName());
                    });
        }
        HttpServletResponse response() {
            return (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{HttpServletResponse.class},
                    (p,m,a) -> {
                        switch (m.getName()) {
                            case "setStatus" -> status=(Integer)a[0];
                            case "setHeader" -> headers.put((String)a[0], (String)a[1]);
                            case "setContentType" -> { }
                            case "getWriter" -> { return new PrintWriter(body); }
                            default -> throw new AssertionError(m.getName());
                        }
                        return null;
                    });
        }
    }
}
