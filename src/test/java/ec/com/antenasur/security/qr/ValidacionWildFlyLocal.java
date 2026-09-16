package ec.com.antenasur.security.qr;

import ec.com.antenasur.service.PasswordService;
import java.io.StringReader;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import javax.net.ssl.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.ParserDelegator;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;

/** Ejecucion manual y local. Secretos solo en memoria; elimina exclusivamente sus fixtures. */
public final class ValidacionWildFlyLocal {
    private final Connection db;
    private final URI origen;
    private final SSLContext tls;
    private final String marca = "tec_audit_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    private final List<Integer> personas = new ArrayList<>(), usuarios = new ArrayList<>(), iglesias = new ArrayList<>();
    private final Map<HttpClient,String> sesiones = new LinkedHashMap<>();

    private ValidacionWildFlyLocal(Connection db, URI origen, SSLContext tls) {
        this.db = db; this.origen = origen; this.tls = tls;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4 || !"--crear-fixtures-temporales".equals(args[3]))
            throw new IllegalArgumentException("standalone.xml certificado.pem https://tribunal.local --crear-fixtures-temporales");
        URI origen = URI.create(args[2]);
        if (!"https".equals(origen.getScheme()) || !"tribunal.local".equals(origen.getHost()))
            throw new IllegalArgumentException("Solo se permite tribunal.local mediante HTTPS");
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var config = factory.newDocumentBuilder().parse(Path.of(args[0]).toFile());
        Element ds = null;
        var fuentes = config.getElementsByTagName("datasource");
        for (int i = 0; i < fuentes.getLength(); i++) {
            var candidato = (Element) fuentes.item(i);
            if ("java:jboss/datasources/TribunalDS".equals(candidato.getAttribute("jndi-name"))) ds = candidato;
        }
        if (ds == null) throw new IllegalStateException("TribunalDS no encontrado");
        String url = texto(ds, "connection-url");
        if (!url.startsWith("jdbc:postgresql://localhost:") && !url.startsWith("jdbc:postgresql://127.0.0.1:"))
            throw new IllegalStateException("La prueba solo admite PostgreSQL local");
        var certificados = CertificateFactory.getInstance("X.509");
        var almacen = KeyStore.getInstance(KeyStore.getDefaultType());
        almacen.load(null, null);
        try (var input = Files.newInputStream(Path.of(args[1]))) {
            int n = 0;
            for (var certificado : certificados.generateCertificates(input)) almacen.setCertificateEntry("local" + n++, certificado);
        }
        var confianza = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        confianza.init(almacen);
        var tls = SSLContext.getInstance("TLS"); tls.init(null, confianza.getTrustManagers(), null);
        Class.forName("org.postgresql.Driver");
        try (var db = DriverManager.getConnection(url, texto(ds, "user-name"), texto(ds, "password"))) {
            var prueba = new ValidacionWildFlyLocal(db, origen, tls);
            try { prueba.ejecutar(); }
            finally { prueba.limpiar(); }
        }
    }

    private static String texto(Element elemento, String nombre) {
        var lista = elemento.getElementsByTagName(nombre);
        if (lista.getLength() == 0 && ("user-name".equals(nombre) || "password".equals(nombre))) {
            var seguridad = elemento.getElementsByTagName("security");
            if (seguridad.getLength() == 1 && ((Element) seguridad.item(0)).hasAttribute(nombre))
                return ((Element) seguridad.item(0)).getAttribute(nombre);
        }
        if (lista.getLength() != 1) throw new IllegalStateException("Configuracion local no soportada: " + nombre);
        return lista.item(0).getTextContent().trim();
    }

    private void ejecutar() throws Exception {
        int iglesia = insertar("INSERT INTO public.tb_iglesia (igl_nombre,igl_documento,estado,u_crea) VALUES (?,?,true,?) RETURNING igl_id",
                marca, marca, marca);
        iglesias.add(iglesia);
        for (String rol : List.of("SITEC-Administrador", "SITEC-Tribunal", "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa")) {
            int numero = personas.size();
            String nombre = marca + "_" + numero;
            int persona = insertar("INSERT INTO public.tb_persona (pers_nombre,pers_documento,estado,u_crea) VALUES (?,?,true,?) RETURNING pers_id",
                    "PRUEBA TEMPORAL " + rol, "SN-" + nombre, marca);
            personas.add(persona);
            String clave = "Aa9" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            int usuario = insertar("INSERT INTO public.tb_usuario (usu_nombre,usu_correo,usu_clave,usu_permanente,pers_id,igl_id,estado,u_crea) VALUES (?,?,?,true,?,?,true,?) RETURNING usu_id",
                    nombre, nombre + "@example.invalid", new PasswordService().hashBcrypt(clave), persona,
                    rol.equals("SITEC-IglesiaAdmin") ? iglesia : null, marca);
            usuarios.add(usuario);
            int asignados = modificar("INSERT INTO public.tb_role_user (usu_id,rol_id,estado,u_crea) SELECT ?,rol_id,true,? FROM public.tb_rol WHERE rol_nombre=? AND estado=true",
                    usuario, marca, rol);
            if (asignados != 1) throw new IllegalStateException("Rol activo no disponible: " + rol);
            var cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
            var cliente = HttpClient.newBuilder().sslContext(tls).cookieHandler(cookies)
                    .connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NEVER).build();
            var login = get(cliente, "/login.jsf");
            var formulario = new FormularioLogin();
            new ParserDelegator().parse(new StringReader(login.body()), formulario, true);
            if (formulario.boton == null || !formulario.campos.containsKey("jakarta.faces.ViewState"))
                throw new IllegalStateException("No se reconocio el formulario de login");
            formulario.campos.put("frmLogin", "frmLogin");
            formulario.campos.put("frmLogin:usuario", nombre);
            formulario.campos.put("frmLogin:password", clave);
            formulario.campos.put(formulario.boton, formulario.boton);
            String body = formulario.campos.entrySet().stream().map(e -> codificar(e.getKey()) + "=" + codificar(e.getValue()))
                    .collect(java.util.stream.Collectors.joining("&"));
            var respuesta = cliente.send(HttpRequest.newBuilder(origen.resolve("/login.jsf"))
                    .timeout(Duration.ofSeconds(60)).header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
            String destino = respuesta.headers().firstValue("Location").orElse("");
            if (respuesta.statusCode() / 100 != 3 || destino.contains("login"))
                throw new IllegalStateException("Login FORM fallido para " + rol + "; HTTP=" + respuesta.statusCode());
            System.out.println("PASS login FORM " + rol);
            sesiones.put(cliente, destino);
            var principal = get(cliente, destino);
            exigirPagina(principal, rol + " destino de login");
            for (String pagina : List.of("usuarios", "roles", "permisos", "asignacionUsuarios")) {
                var resultado = get(cliente, "/" + pagina + ".jsf");
                if (rol.equals("SITEC-Administrador")) {
                    if (pagina.equals("asignacionUsuarios") && resultado.headers().firstValue("Location").orElse("").contains("/errors/permisos"))
                        System.out.println("INFO pantalla antigua no incluida en menu activo de Administrador");
                    else exigirPagina(resultado, pagina);
                }
                else if (!resultado.headers().firstValue("Location").orElse("").contains("/errors/permisos"))
                    throw new IllegalStateException("Falta rechazo de " + pagina + " para " + rol);
            }
            System.out.println("PASS fronteras administrativas " + rol);
            if (rol.equals("SITEC-Tribunal")) {
                for (String pagina : List.of("iglesias", "autoridades", "mjrv", "reportesMesa")) exigirPagina(get(cliente, "/" + pagina + ".jsf"), pagina);
                System.out.println("PASS pantallas electorales Tribunal");
            }
            if (rol.equals("SITEC-IglesiaAdmin")) {
                var panel = get(cliente, "/dashboard.jsf");
                exigirPagina(panel, "dashboard iglesia");
                if (!panel.body().contains(marca)) throw new IllegalStateException("No se visualiza la iglesia de prueba asignada");
                exigirPagina(get(cliente, "/iglesias.jsf"), "iglesias propias");
                exigirPagina(get(cliente, "/personas.jsf"), "personas propias");
                System.out.println("PASS contexto iglesia asignada");
            }
        }
    }

    private HttpResponse<String> get(HttpClient cliente, String ruta) throws Exception {
        URI destino = origen.resolve(ruta);
        if (!Objects.equals(destino.getHost(), origen.getHost())) throw new IllegalStateException("Redireccion fuera del entorno local");
        return cliente.send(HttpRequest.newBuilder(destino).timeout(Duration.ofSeconds(60)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private static void exigirPagina(HttpResponse<String> respuesta, String nombre) {
        if (respuesta.statusCode() != 200 || respuesta.body().contains("ComponentNotFoundException")
                || respuesta.body().contains("EJBAccessException") || respuesta.body().contains("WFLYEJB0364"))
            throw new IllegalStateException("Pagina no disponible: " + nombre + "; HTTP=" + respuesta.statusCode());
    }
    private int insertar(String sql, Object... valores) throws SQLException {
        try (var st = db.prepareStatement(sql)) {
            for (int i=0;i<valores.length;i++) st.setObject(i+1,valores[i]);
            try (var resultado=st.executeQuery()) { if (!resultado.next()) throw new SQLException("Fixture no insertado"); return resultado.getInt(1); }
        }
    }
    private int modificar(String sql, Object... valores) throws SQLException {
        try (var st = db.prepareStatement(sql)) {
            for (int i=0;i<valores.length;i++) st.setObject(i+1,valores[i]);
            return st.executeUpdate();
        }
    }
    private void limpiar() throws SQLException {
        for (var sesion : sesiones.entrySet()) {
            try { cerrarSesion(sesion.getKey(), sesion.getValue()); }
            catch (Exception e) { System.out.println("WARN cierre de sesion de prueba no confirmado: " + e.getClass().getSimpleName()); }
        }
        for (int id : usuarios) {
            modificar("DELETE FROM public.tb_role_user WHERE usu_id=? AND u_crea=?", id, marca);
            modificar("DELETE FROM public.tb_usuario WHERE usu_id=? AND u_crea=?", id, marca);
        }
        for (int id : personas) modificar("DELETE FROM public.tb_persona WHERE pers_id=? AND u_crea=?", id, marca);
        for (int id : iglesias) modificar("DELETE FROM public.tb_iglesia WHERE igl_id=? AND u_crea=?", id, marca);
        System.out.println("Fixtures eliminados; auditoria de accesos conservada.");
    }
    private void cerrarSesion(HttpClient cliente, String pagina) throws Exception {
        var html = get(cliente, pagina);
        var campos = new LinkedHashMap<String,String>();
        String[] enlace = {null};
        new ParserDelegator().parse(new StringReader(html.body()), new HTMLEditorKit.ParserCallback() {
            boolean dentro;
            @Override public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
                if ("form".equals(t.toString())) dentro = "formLogoutBar".equals(a.getAttribute(HTML.Attribute.ID));
                capturar(t,a);
            }
            @Override public void handleEndTag(HTML.Tag t, int pos) { if ("form".equals(t.toString())) dentro = false; }
            @Override public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) { capturar(t,a); }
            private void capturar(HTML.Tag t, MutableAttributeSet a) {
                if (!dentro) return;
                if ("a".equals(t.toString())) enlace[0] = Objects.toString(a.getAttribute(HTML.Attribute.ID), null);
                if ("input".equals(t.toString()) && a.getAttribute(HTML.Attribute.NAME) != null)
                    campos.put(a.getAttribute(HTML.Attribute.NAME).toString(), Objects.toString(a.getAttribute(HTML.Attribute.VALUE), ""));
            }
        }, true);
        if (enlace[0] == null) throw new IllegalStateException("No se encontro logout");
        campos.put("formLogoutBar", "formLogoutBar");
        campos.put("jakarta.faces.partial.ajax", "true");
        campos.put("jakarta.faces.source", enlace[0]);
        campos.put("jakarta.faces.partial.execute", enlace[0]);
        campos.put(enlace[0], enlace[0]);
        String body = campos.entrySet().stream().map(e -> codificar(e.getKey()) + "=" + codificar(e.getValue()))
                .collect(java.util.stream.Collectors.joining("&"));
        cliente.send(HttpRequest.newBuilder(origen.resolve(pagina)).timeout(Duration.ofSeconds(30))
                .header("Faces-Request", "partial/ajax").header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.discarding());
        var comprobacion = get(cliente, pagina);
        if (!comprobacion.headers().firstValue("Location").orElse("").contains("/login"))
            throw new IllegalStateException("Logout no confirmado");
        System.out.println("PASS logout de cuenta temporal");
    }
    private static String codificar(String valor) { return URLEncoder.encode(valor, StandardCharsets.UTF_8); }
    private static final class FormularioLogin extends HTMLEditorKit.ParserCallback {
        final Map<String,String> campos = new LinkedHashMap<>();
        String boton;
        @Override public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) { capturar(t,a); }
        @Override public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) { capturar(t,a); }
        private void capturar(HTML.Tag t, MutableAttributeSet a) {
            Object nombre = a.getAttribute(HTML.Attribute.NAME);
            if (nombre == null) return;
            if ("input".equals(t.toString()) && "hidden".equals(a.getAttribute(HTML.Attribute.TYPE)))
                campos.put(nombre.toString(), Objects.toString(a.getAttribute(HTML.Attribute.VALUE), ""));
            if ("button".equals(t.toString()) && nombre.toString().startsWith("frmLogin:")) boton = nombre.toString();
        }
    }
}
