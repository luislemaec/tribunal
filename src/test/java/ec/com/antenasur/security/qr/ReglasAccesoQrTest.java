package ec.com.antenasur.security.qr;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReglasAccesoQrTest {
    private static final Instant INICIO = Instant.parse("2026-11-30T12:00:00Z");
    private static final Instant FIN = INICIO.plusSeconds(3600);

    private ResultadoAccesoQr evaluar(EstadoAccesoQr estado, boolean sesion, Instant ahora, boolean... hechos) {
        return ReglasAccesoQr.evaluar(estado, sesion, ahora, INICIO, FIN, INICIO, FIN,
                hechos[0], hechos[1], hechos[2], hechos[3], hechos[4], hechos[5]);
    }

    @Test void permiteCanjeDentroDelCronograma() {
        assertEquals(ResultadoAccesoQr.VALIDO, evaluar(EstadoAccesoQr.EMITIDO, false, INICIO,
                true, true, true, true, true, true));
    }

    @Test void impideReplayPeroPermiteRevalidarSesionDelCanje() {
        assertEquals(ResultadoAccesoQr.YA_UTILIZADO, evaluar(EstadoAccesoQr.CANJEADO, false, INICIO,
                true, true, true, true, true, true));
        assertEquals(ResultadoAccesoQr.VALIDO, evaluar(EstadoAccesoQr.CANJEADO, true, INICIO,
                true, true, true, true, true, true));
        assertEquals(ResultadoAccesoQr.TOKEN_INVALIDO, evaluar(EstadoAccesoQr.EMITIDO, true, INICIO,
                true, true, true, true, true, true));
    }

    @Test void revocacionBloqueaTambienLaSesion() {
        for (boolean sesion : new boolean[]{false, true}) {
            assertEquals(ResultadoAccesoQr.REVOCADO, evaluar(EstadoAccesoQr.REVOCADO, sesion, INICIO,
                    true, true, true, true, true, true));
        }
    }

    @Test void limitesDeVigenciaSonInicioInclusivoYFinExclusivo() {
        assertFalse(ReglasAccesoQr.enVentana(INICIO.minusNanos(1), INICIO, FIN));
        assertTrue(ReglasAccesoQr.enVentana(INICIO, INICIO, FIN));
        assertTrue(ReglasAccesoQr.enVentana(FIN.minusNanos(1), INICIO, FIN));
        assertFalse(ReglasAccesoQr.enVentana(FIN, INICIO, FIN));
        assertFalse(ReglasAccesoQr.enVentana(INICIO, FIN, INICIO));
        assertFalse(ReglasAccesoQr.enVentana(INICIO, null, FIN));
    }

    @Test void cronogramaModificadoNoAmpliaVigenciaDelToken() {
        assertEquals(ResultadoAccesoQr.FUERA_DE_VIGENCIA,
                ReglasAccesoQr.evaluar(EstadoAccesoQr.CANJEADO, true, INICIO.plusSeconds(20),
                        INICIO, FIN, INICIO.plusSeconds(30), FIN, true, true, true, true, true, true));
        assertEquals(ResultadoAccesoQr.FUERA_DE_VIGENCIA,
                ReglasAccesoQr.evaluar(EstadoAccesoQr.EMITIDO, false, FIN,
                        INICIO, FIN, INICIO, FIN.plusSeconds(3600), true, true, true, true, true, true));
    }

    @Test void cadaCondicionElectoralEsObligatoriaTantoEnCanjeComoEnSesion() {
        ResultadoAccesoQr[] errores = {ResultadoAccesoQr.DOCUMENTO_NO_VIGENTE,
                ResultadoAccesoQr.PROCESO_NO_VIGENTE, ResultadoAccesoQr.MESA_NO_VIGENTE,
                ResultadoAccesoQr.PRESIDENTE_NO_VIGENTE, ResultadoAccesoQr.USUARIO_NO_AUTORIZADO,
                ResultadoAccesoQr.JRV_INCOMPLETA};
        for (int i = 0; i < errores.length; i++) {
            boolean[] hechos = {true, true, true, true, true, true};
            hechos[i] = false;
            assertEquals(errores[i], evaluar(EstadoAccesoQr.EMITIDO, false, INICIO, hechos));
            assertEquals(errores[i], evaluar(EstadoAccesoQr.CANJEADO, true, INICIO, hechos));
        }
    }
}
