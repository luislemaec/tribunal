package ec.com.antenasur.service.tec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

/** Reglas de privacidad y de abuso de la consulta pública de lugar de votación. */
class ConsultaLugarVotacionTest {

    @Test
    void cedulaExactaDeDiezDigitos() {
        assertEquals("0602720963", ConsultaLugarVotacionService.normalizarCedula(" 060272096-3 "));
        assertNull(ConsultaLugarVotacionService.normalizarCedula("060272096"));
        assertNull(ConsultaLugarVotacionService.normalizarCedula("06027209631"));
        assertNull(ConsultaLugarVotacionService.normalizarCedula("JUAN PEREZ"));
        assertNull(ConsultaLugarVotacionService.normalizarCedula("06027%0963"));
        assertNull(ConsultaLugarVotacionService.normalizarCedula(null));
    }

    @Test
    void nombreEnmascaradoConIniciales() {
        assertEquals("J*** C***** N**** P*****",
                ConsultaLugarVotacionService.enmascararNombre("Juan Carlos", "  Naula  Paucar"));
        assertEquals("Ñ** A*", ConsultaLugarVotacionService.enmascararNombre("ÑAN", "A"));
        assertEquals("", ConsultaLugarVotacionService.enmascararNombre(null, null));
    }

    @Test
    void cedulaEnmascaradaParaElLog() {
        assertEquals("*******963", ConsultaLugarVotacionService.enmascararCedula("0602720963"));
        assertEquals("***", ConsultaLugarVotacionService.enmascararCedula(null));
    }

    @Test
    void limitePorIpYUnSoloRegistroDeBloqueoPorVentana() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-11T10:00:00Z"), ZoneOffset.UTC);
        LimiteConsultaLugarVotacion limite = new LimiteConsultaLugarVotacion(reloj);
        int consultas = 0;
        for (int i = 0; i < LimiteConsultaLugarVotacion.MAX_POR_IP; i++) {
            consultas = limite.registrar("10.0.0.1");
            assertTrue(LimiteConsultaLugarVotacion.permitida(consultas));
        }
        int rechazo = limite.registrar("10.0.0.1");
        assertFalse(LimiteConsultaLugarVotacion.permitida(rechazo));
        assertTrue(LimiteConsultaLugarVotacion.primerRechazo(rechazo));
        assertFalse(LimiteConsultaLugarVotacion.primerRechazo(limite.registrar("10.0.0.1")));
        // Otra IP no se ve afectada.
        assertTrue(LimiteConsultaLugarVotacion.permitida(limite.registrar("10.0.0.2")));
    }
}
