package ec.com.antenasur.security.menu;

import ec.com.antenasur.util.MenuVO;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Identidad de pagina compartida por menu, filtro y autorizacion EJB. */
public final class PaginasMenu {
    private PaginasMenu() { }

    public static String normalizar(String valor) {
        if (valor == null || valor.isBlank() || valor.contains("#{") || valor.contains("${")
                || valor.contains("%") || valor.contains("\\") || valor.contains(";")
                || valor.contains("..")) return null;
        try {
            URI uri = URI.create(valor.trim());
            if (uri.isAbsolute() || uri.getRawAuthority() != null || uri.getFragment() != null) return null;
            String pagina = uri.getPath();
            if (pagina == null) return null;
            if (pagina.startsWith("/")) pagina = pagina.substring(1);
            if (pagina.startsWith("faces/")) pagina = pagina.substring(6);
            if (pagina.isBlank() || pagina.contains("/")) return null;
            int punto = pagina.lastIndexOf('.');
            if (punto >= 0) {
                if (!Set.of("jsf", "xhtml", "faces").contains(pagina.substring(punto + 1))) return null;
                pagina = pagina.substring(0, punto);
            }
            return pagina.matches("[A-Za-z][A-Za-z0-9_-]*") ? pagina + ".jsf" : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static List<String> extraer(Collection<MenuVO> menus) {
        Set<String> resultado = new LinkedHashSet<>();
        if (menus != null) for (MenuVO menu : menus) {
            if (!Boolean.TRUE.equals(menu.getEndNode())) continue;
            String pagina = normalizar(menu.getUrlMenu());
            if (pagina == null) pagina = normalizar(menu.getActionMenu());
            if (pagina != null) resultado.add(pagina);
        }
        // Compatibilidad: paginas auxiliares ya disponibles en MenuService.
        resultado.add("inicio.jsf");
        resultado.add("cambioClave.jsf");
        return new java.util.ArrayList<>(resultado);
    }

    public static List<MenuVO> conectados(List<MenuVO> menus, Integer raiz) {
        Set<Integer> alcanzables = new java.util.HashSet<>();
        alcanzables.add(raiz);
        boolean cambio;
        do {
            cambio = false;
            for (MenuVO menu : menus) {
                if (menu.getIdMenuParent() != null && alcanzables.contains(menu.getIdMenuParent()))
                    cambio |= alcanzables.add(menu.getIdMenu());
            }
        } while (cambio);
        return new java.util.ArrayList<>(menus.stream()
                .filter(m -> alcanzables.contains(m.getIdMenu()) && !java.util.Objects.equals(raiz, m.getIdMenu())).toList());
    }

    public static boolean permite(Collection<String> paginas, String solicitada) {
        String pagina = normalizar(solicitada);
        return pagina != null && paginas != null
                && paginas.stream().map(PaginasMenu::normalizar).anyMatch(pagina::equals);
    }
}
