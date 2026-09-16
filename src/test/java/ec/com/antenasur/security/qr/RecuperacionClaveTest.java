package ec.com.antenasur.security.qr;

import ec.com.antenasur.facade.UsuarioFacade;
import ec.com.antenasur.model.Usuario;
import ec.com.antenasur.model.Persona;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.lang.reflect.Proxy;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecuperacionClaveTest {
    @Test void consumoRevalidaConBloqueoYSoloPermiteUnaEscritura() {
        var usuario = usuario();
        var facade = facade(usuario, false);
        assertTrue(facade.consumirTokenRecuperacion(10, "hash", "bcrypt"));
        assertEquals("bcrypt", usuario.getContrasenia());
        assertNull(usuario.getLink());
        assertFalse(facade.consumirTokenRecuperacion(10, "hash", "otro"));
    }
    @Test void tokenRevocadoDuranteLaEsperaDelBloqueoNoCambiaClave() {
        var usuario = usuario();
        assertFalse(facade(usuario, true).consumirTokenRecuperacion(10, "hash", "bcrypt"));
        assertNull(usuario.getContrasenia());
    }
    @Test void tokenExpiradoYCuentaInactivaNoSeConsumen() {
        var usuario = usuario();
        usuario.setUsuarioFechaExpira(Timestamp.from(Instant.now().minusSeconds(1)));
        assertFalse(facade(usuario, false).consumirTokenRecuperacion(10, "hash", "bcrypt"));
        usuario = usuario();
        usuario.setEstado(false);
        assertFalse(facade(usuario, false).consumirTokenRecuperacion(10, "hash", "bcrypt"));
    }
    private Usuario usuario() {
        var usuario = new Usuario(); usuario.setId(10); usuario.setEstado(true); usuario.setLink("hash");
        usuario.setUsuarioFechaExpira(Timestamp.from(Instant.now().plusSeconds(60)));
        var persona = new Persona(); persona.setEstado(true); usuario.setPersonsa(persona);
        return usuario;
    }
    private UsuarioFacade facade(Usuario usuario, boolean revocarAlRefrescar) {
        var em = (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EntityManager.class},
                (p,m,a) -> {
                    if (m.getName().equals("find")) {
                        assertEquals(LockModeType.PESSIMISTIC_WRITE, a[2]); return usuario;
                    }
                    if (m.getName().equals("refresh")) {
                        assertEquals(LockModeType.PESSIMISTIC_WRITE, a[1]);
                        if (revocarAlRefrescar) usuario.setLink(null);
                        return null;
                    }
                    throw new AssertionError(m.getName());
                });
        return new UsuarioFacade() {
            @Override protected EntityManager getEntityManager() { return em; }
            @Override public Usuario edit(Usuario u) { return u; }
        };
    }
}
