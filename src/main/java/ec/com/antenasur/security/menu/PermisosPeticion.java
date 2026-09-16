package ec.com.antenasur.security.menu;

import ec.com.antenasur.util.MenuVO;
import jakarta.enterprise.context.RequestScoped;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Snapshot solo de esta peticion; la siguiente vuelve a consultar permisos. */
@RequestScoped
public class PermisosPeticion {
    private final Map<String, List<MenuVO>> menus = new HashMap<>();
    public List<MenuVO> obtener(String principal) { return menus.get(principal); }
    public void guardar(String principal, List<MenuVO> valor) { menus.put(principal, valor); }
}
