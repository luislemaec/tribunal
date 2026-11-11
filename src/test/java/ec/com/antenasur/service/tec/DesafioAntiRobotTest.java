package ec.com.antenasur.service.tec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

/** Desafío anti-robot (ALTCHA propio) y caché de la consulta pública de lugar de votación. */
class DesafioAntiRobotTest {

    /** Reloj que se puede adelantar. */
    static final class Reloj extends Clock {
        Instant ahora = Instant.parse("2026-11-11T10:00:00Z");

        @Override
        public Instant instant() {
            return ahora;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    private static String resolver(DesafioAntiRobot.Desafio d) {
        for (long n = 0; n <= d.maximo(); n++) {
            if (DesafioAntiRobot.sha256(d.salt() + n).equals(d.challenge())) {
                return String.valueOf(n);
            }
        }
        return null;
    }

    @Test
    void personaQueResuelveYEsperaPasaUnaSolaVez() {
        Reloj reloj = new Reloj();
        DesafioAntiRobot desafio = new DesafioAntiRobot(reloj, null, 1_000);
        DesafioAntiRobot.Desafio d = desafio.crear();
        String numero = resolver(d);
        reloj.ahora = reloj.ahora.plus(Duration.ofSeconds(3));
        assertTrue(desafio.verificar(d.challenge(), d.salt(), d.firma(), numero));
        // Reutilizar la misma respuesta (robot que repite la solicitud) se rechaza.
        assertFalse(desafio.verificar(d.challenge(), d.salt(), d.firma(), numero));
    }

    @Test
    void rechazaRespuestaIncorrectaFirmaAlteradaEnvioInmediatoYVencido() {
        Reloj reloj = new Reloj();
        DesafioAntiRobot desafio = new DesafioAntiRobot(reloj, null, 1_000);
        DesafioAntiRobot.Desafio d = desafio.crear();
        String numero = resolver(d);
        // Envío antes del tiempo mínimo: típico de un robot.
        assertFalse(desafio.verificar(d.challenge(), d.salt(), d.firma(), numero));
        reloj.ahora = reloj.ahora.plus(Duration.ofSeconds(3));
        assertFalse(desafio.verificar(d.challenge(), d.salt(), d.firma(), String.valueOf(Long.parseLong(numero) + 1)));
        assertFalse(desafio.verificar(d.challenge(), d.salt(), "00" + d.firma().substring(2), numero));
        assertFalse(desafio.verificar(d.challenge(), d.salt(), d.firma(), "abc"));
        assertFalse(desafio.verificar(null, d.salt(), d.firma(), numero));
        reloj.ahora = reloj.ahora.plus(Duration.ofMinutes(11));
        assertFalse(desafio.verificar(d.challenge(), d.salt(), d.firma(), numero), "vencido");
    }

    @Test
    void otraInstanciaConOtraClaveNoAceptaElDesafio() {
        Reloj reloj = new Reloj();
        DesafioAntiRobot.Desafio d = new DesafioAntiRobot(reloj, null, 1_000).crear();
        String numero = resolver(d);
        reloj.ahora = reloj.ahora.plus(Duration.ofSeconds(3));
        assertFalse(new DesafioAntiRobot(reloj, null, 1_000).verificar(d.challenge(), d.salt(), d.firma(), numero));
    }

    @Test
    void cacheVenceYPuedeDesactivarse() {
        Reloj reloj = new Reloj();
        CacheConsultaLugarVotacion cache = new CacheConsultaLugarVotacion(reloj, 60_000);
        ConsultaLugarVotacionService.Resultado r = new ConsultaLugarVotacionService.Resultado(
                ConsultaLugarVotacionService.Estado.NO_ENCONTRADO, null);
        cache.guardar("0602720963", r);
        assertEquals(r, cache.obtener("0602720963"));
        reloj.ahora = reloj.ahora.plus(Duration.ofSeconds(61));
        assertNull(cache.obtener("0602720963"));
        CacheConsultaLugarVotacion apagada = new CacheConsultaLugarVotacion(reloj, 0);
        apagada.guardar("0602720963", r);
        assertNull(apagada.obtener("0602720963"));
    }

    @Test
    void topeGlobalPorSegundo() {
        Clock fijo = Clock.fixed(Instant.parse("2026-11-11T10:00:00Z"), ZoneOffset.UTC);
        LimiteConsultaLugarVotacion limite = new LimiteConsultaLugarVotacion(fijo);
        for (int i = 0; i < LimiteConsultaLugarVotacion.MAX_GLOBAL_POR_SEGUNDO; i++) {
            assertTrue(limite.permitirGlobal());
        }
        assertFalse(limite.permitirGlobal());
    }
}
