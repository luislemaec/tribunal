package ec.com.antenasur.util;

import java.util.Set;

public final class LoginFilterExcluder {
    private static final Set<String> PAGINAS = Set.of("login", "olvidoClave",
            "recuperaClaveCorrecto", "restablecerClave", "claveActualizada", "consultar", "resultados");
    private final String contextPath;

    private LoginFilterExcluder(String contextPath) {
        this.contextPath = contextPath == null ? "" : contextPath;
    }

    public boolean isExcludeUrl(String url) {
        String ruta = rutaLocal(url);
        if (ruta == null) return false;
        return ruta.startsWith("/resources/") || ruta.startsWith("/jakarta.faces.resource/")
                || ruta.startsWith("/public/") || ruta.startsWith("/errors/")
                || "/index.html".equals(ruta) || PAGINAS.contains(paginaFaces(ruta));
    }

    public boolean esLogin(String url) {
        return "login".equals(paginaFaces(rutaLocal(url)));
    }

    private String rutaLocal(String url) {
        if (url == null || !url.startsWith(contextPath + "/")) return null;
        String ruta = url.substring(contextPath.length());
        if (ruta.contains("..") || ruta.contains("%") || ruta.contains(";") || ruta.contains("\\")) return null;
        return ruta;
    }

    private String paginaFaces(String ruta) {
        if (ruta == null) return "";
        String pagina = ruta.startsWith("/faces/") ? ruta.substring(7) : ruta.substring(1);
        if (pagina.contains("/")) return "";
        int punto = pagina.lastIndexOf('.');
        if (punto < 0 || !Set.of("jsf", "faces", "xhtml").contains(pagina.substring(punto + 1))) return "";
        return pagina.substring(0, punto);
    }

    public static LoginFilterExcluder getInstance(String contextPath) {
        return new LoginFilterExcluder(contextPath);
    }

}
