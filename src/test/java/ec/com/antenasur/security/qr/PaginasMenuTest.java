package ec.com.antenasur.security.qr;

import ec.com.antenasur.security.menu.PaginasMenu;
import ec.com.antenasur.util.MenuVO;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaginasMenuTest {
    @Test void aliasRepresentanLaMismaPaginaSinAceptarRutasAjenas() {
        for (String ruta : List.of("usuarios", "/usuarios.xhtml", "usuarios.jsf", "/faces/usuarios.faces"))
            assertEquals("usuarios.jsf", PaginasMenu.normalizar(ruta));
        for (String ruta : List.of("S/N", "../usuarios.jsf", "/otra/usuarios.jsf", "//otro/usuarios.jsf",
                "https://otro/usuarios.jsf", "usuarios.jsf;hack", "usuarios%2ejsf", "#{bean.accion}"))
            assertNull(PaginasMenu.normalizar(ruta), ruta);
    }

    @Test void extraeSoloDestinosFinalesSinDuplicadosYConOutcomeLiteral() {
        var paginas = PaginasMenu.extraer(List.of(menu(2, 1, "roles.jsf", null, false),
                menu(3, 2, "usuarios.xhtml", null, true), menu(4, 2, "usuarios.jsf", null, true),
                menu(5, 2, "S/N", "/permisos", true)));
        assertEquals(List.of("usuarios.jsf", "permisos.jsf", "inicio.jsf", "cambioClave.jsf"), paginas);
        assertFalse(PaginasMenu.permite(paginas, "roles.jsf"));
        assertFalse(PaginasMenu.permite(null, "usuarios.jsf"));
    }

    @Test void noAutorizaHijosOcultosPorPadreAusenteNiCiclos() {
        var menus = List.of(menu(2, 1, null, null, false), menu(3, 2, "usuarios.jsf", null, true),
                menu(4, 99, "roles.jsf", null, true), menu(5, 6, null, null, false), menu(6, 5, null, null, false));
        assertEquals(List.of(menus.get(0), menus.get(1)), PaginasMenu.conectados(menus, 1));
    }

    static MenuVO menu(int id, int padre, String url, String accion, boolean finalizado) {
        return new MenuVO(id, "opcion", accion, url, padre, 8, finalizado, id, null);
    }
}
