package ec.com.antenasur.security.api;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Límite de intentos de login de la API móvil: 10 por IP y 5 por usuario por minuto. */
class LimiteIntentosApiTest {

    private Instant ahora = Instant.parse("2026-10-08T12:00:00Z");

    private LimiteIntentosApi limite() {
        return new LimiteIntentosApi(new Clock() {
            @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zona) { return this; }
            @Override public Instant instant() { return ahora; }
        });
    }

    @Test
    void cincoIntentosPorUsuarioSinImportarMayusculasNiEspacios() {
        LimiteIntentosApi l = limite();
        for (int i = 0; i < LimiteIntentosApi.MAX_POR_USUARIO; i++) {
            assertTrue(l.permitir("10.0.0." + i, i % 2 == 0 ? "llema" : " LLEMA "));
        }
        assertFalse(l.permitir("10.0.0.99", "llema"));
        assertTrue(l.permitir("10.0.0.99", "otro"));
    }

    @Test
    void diezIntentosPorIpAunqueCambieElUsuario() {
        LimiteIntentosApi l = limite();
        for (int i = 0; i < LimiteIntentosApi.MAX_POR_IP; i++) {
            assertTrue(l.permitir("10.0.0.1", "usuario" + i));
        }
        assertFalse(l.permitir("10.0.0.1", "nuevo"));
        assertTrue(l.permitir("10.0.0.2", "nuevo"));
    }

    @Test
    void laVentanaSeLiberaAlMinuto() {
        LimiteIntentosApi l = limite();
        for (int i = 0; i <= LimiteIntentosApi.MAX_POR_USUARIO; i++) {
            l.permitir("10.0.0.1", "llema");
        }
        assertFalse(l.permitir("10.0.0.1", "llema"));
        ahora = ahora.plusSeconds(60);
        assertTrue(l.permitir("10.0.0.1", "llema"));
    }
}
