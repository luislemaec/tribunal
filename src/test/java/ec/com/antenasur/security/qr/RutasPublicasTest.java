package ec.com.antenasur.security.qr;

import ec.com.antenasur.util.LoginFilterExcluder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RutasPublicasTest {
    @Test void paginasSonExactasYSusAliasFacesNoGeneranBucles() {
        var filtro = LoginFilterExcluder.getInstance("/tec");
        for (String ruta : new String[]{"/login.jsf", "/login.xhtml", "/login.faces", "/faces/login.xhtml",
                "/restablecerClave.jsf", "/olvidoClave.xhtml", "/public/resultados.html", "/errors/permisos.jsf"})
            assertTrue(filtro.isExcludeUrl("/tec" + ruta), ruta);
        for (String ruta : new String[]{"/login.jsf/usuarios.jsf", "/login.jsfOtro", "/public/../usuarios.jsf",
                "/public/%2e%2e/usuarios.jsf", "/login.jsf;otro", "/usuarios.xhtml", "/faces/usuarios.xhtml"})
            assertFalse(filtro.isExcludeUrl("/tec" + ruta), ruta);
    }
    @Test void contextosNoCompartenLaPrimeraInstancia() {
        var raiz = LoginFilterExcluder.getInstance("");
        var tec = LoginFilterExcluder.getInstance("/tec");
        assertTrue(raiz.isExcludeUrl("/login.jsf"));
        assertFalse(tec.isExcludeUrl("/login.jsf"));
        assertTrue(tec.isExcludeUrl("/tec/login.jsf"));
        assertFalse(tec.isExcludeUrl("/tecOtro/login.jsf"));
    }
}
