package ec.com.antenasur.security.qr;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReglasAccesoQrTest {
    /** Cierre del SUFRAGIO: apertura de la ventana de canje. */
    private static final Instant CIERRE = Instant.parse("2026-11-30T22:00:00Z");
    private static final Instant DESDE = CIERRE;
    private static final Instant HASTA = CIERRE.plus(VentanaAccesoQr.VIGENCIA_POSTERIOR);

    private CausaRechazoQr diagnosticar(EstadoAccesoQr estado, boolean sesion, Instant ahora, boolean... hechos) {
        return ReglasAccesoQr.diagnosticar(estado, sesion, ahora, DESDE, HASTA, CIERRE,
                hechos[0], hechos[1], hechos[2], hechos[3], hechos[4], hechos[5], hechos[6]);
    }

    private ResultadoAccesoQr evaluar(EstadoAccesoQr estado, boolean sesion, Instant ahora, boolean... hechos) {
        return diagnosticar(estado, sesion, ahora, hechos).resultado();
    }

    private static boolean[] todoValido() {
        return new boolean[]{true, true, true, true, true, true, true};
    }

    @Test void antesDelCierreDeSufragioSeRechaza() {
        assertEquals(CausaRechazoQr.ANTES_DE_VIGENCIA,
                diagnosticar(EstadoAccesoQr.EMITIDO, false, CIERRE.minusSeconds(1), todoValido()));
        assertEquals(CausaRechazoQr.ANTES_DE_VIGENCIA,
                diagnosticar(EstadoAccesoQr.EMITIDO, false, CIERRE.minusSeconds(3600), todoValido()));
    }

    @Test void exactamenteEnElCierreSePermite() {
        assertEquals(CausaRechazoQr.VALIDO, diagnosticar(EstadoAccesoQr.EMITIDO, false, CIERRE, todoValido()));
    }

    @Test void dentroDeLasSeisHorasPosterioresSePermite() {
        assertEquals(CausaRechazoQr.VALIDO,
                diagnosticar(EstadoAccesoQr.EMITIDO, false, CIERRE.plusSeconds(1), todoValido()));
        assertEquals(CausaRechazoQr.VALIDO,
                diagnosticar(EstadoAccesoQr.EMITIDO, false, CIERRE.plusSeconds(3 * 3600), todoValido()));
        assertEquals(CausaRechazoQr.VALIDO,
                diagnosticar(EstadoAccesoQr.EMITIDO, false, HASTA.minusSeconds(1), todoValido()));
    }

    @Test void despuesDeLasSeisHorasSeRechaza() {
        assertEquals(CausaRechazoQr.FUERA_DE_VIGENCIA,
                diagnosticar(EstadoAccesoQr.EMITIDO, false, HASTA, todoValido()));
        assertEquals(CausaRechazoQr.FUERA_DE_VIGENCIA,
                diagnosticar(EstadoAccesoQr.EMITIDO, false, HASTA.plusSeconds(1), todoValido()));
    }

    @Test void revocadoOYaCanjeadoSeRechazanDentroDeLaVentana() {
        Instant dentro = CIERRE.plusSeconds(60);
        assertEquals(CausaRechazoQr.YA_CANJEADO,
                diagnosticar(EstadoAccesoQr.CANJEADO, false, dentro, todoValido()));
        for (boolean sesion : new boolean[]{false, true}) {
            assertEquals(CausaRechazoQr.REVOCADO,
                    diagnosticar(EstadoAccesoQr.REVOCADO, sesion, dentro, todoValido()));
        }
    }

    @Test void laSesionSoloRevalidaUnTokenYaCanjeado() {
        Instant dentro = CIERRE.plusSeconds(60);
        assertEquals(CausaRechazoQr.VALIDO, diagnosticar(EstadoAccesoQr.CANJEADO, true, dentro, todoValido()));
        assertEquals(CausaRechazoQr.SESION_NO_CANJEADA,
                diagnosticar(EstadoAccesoQr.EMITIDO, true, dentro, todoValido()));
    }

    @Test void ventanaAlmacenadaDebeCorresponderAlCierreDelSufragio() {
        Instant dentro = CIERRE.plusSeconds(60);
        // Regla anterior (inicio..fin de SUFRAGIO): no se acepta en silencio.
        assertEquals(CausaRechazoQr.VENTANA_NO_CORRESPONDE_A_CIERRE_SUFRAGIO,
                ReglasAccesoQr.diagnosticar(EstadoAccesoQr.EMITIDO, false, dentro,
                        CIERRE.minusSeconds(8 * 3600), CIERRE, CIERRE,
                        true, true, true, true, true, true, true));
        // Cronograma desplazado tras la emisión: tampoco amplía ni traslada la vigencia.
        assertEquals(CausaRechazoQr.VENTANA_NO_CORRESPONDE_A_CIERRE_SUFRAGIO,
                ReglasAccesoQr.diagnosticar(EstadoAccesoQr.EMITIDO, false, dentro, DESDE, HASTA,
                        CIERRE.plusSeconds(3600),
                        true, true, true, true, true, true, true));
        // Ventana manipulada para durar más de lo permitido.
        assertEquals(CausaRechazoQr.VENTANA_NO_CORRESPONDE_A_CIERRE_SUFRAGIO,
                ReglasAccesoQr.diagnosticar(EstadoAccesoQr.EMITIDO, false, dentro, DESDE,
                        HASTA.plusSeconds(3600), CIERRE,
                        true, true, true, true, true, true, true));
    }

    @Test void sinFaseDeSufragioConfiguradaNoHayCanje() {
        assertEquals(CausaRechazoQr.SUFRAGIO_NO_CONFIGURADO,
                ReglasAccesoQr.diagnosticar(EstadoAccesoQr.EMITIDO, false, CIERRE, DESDE, HASTA, null,
                        true, true, true, true, true, true, true));
    }

    @Test void limitesDeVigenciaSonInicioInclusivoYFinExclusivo() {
        assertFalse(ReglasAccesoQr.enVentana(DESDE.minusNanos(1), DESDE, HASTA));
        assertTrue(ReglasAccesoQr.enVentana(DESDE, DESDE, HASTA));
        assertTrue(ReglasAccesoQr.enVentana(HASTA.minusNanos(1), DESDE, HASTA));
        assertFalse(ReglasAccesoQr.enVentana(HASTA, DESDE, HASTA));
        assertFalse(ReglasAccesoQr.enVentana(DESDE, HASTA, DESDE));
        assertFalse(ReglasAccesoQr.enVentana(DESDE, null, HASTA));
    }

    @Test void versionDocumentalSeDistingueDeOtrosRechazosDocumentales() {
        boolean[] hechos = todoValido();
        hechos[0] = false;
        assertEquals(CausaRechazoQr.DOCUMENTO_VERSION_INVALIDA,
                diagnosticar(EstadoAccesoQr.EMITIDO, false, CIERRE, hechos));
        assertEquals(ResultadoAccesoQr.DOCUMENTO_NO_VIGENTE,
                evaluar(EstadoAccesoQr.EMITIDO, false, CIERRE, hechos));
    }

    @Test void cadaCondicionElectoralEsObligatoriaTantoEnCanjeComoEnSesion() {
        CausaRechazoQr[] errores = {CausaRechazoQr.DOCUMENTO_VERSION_INVALIDA, CausaRechazoQr.DOCUMENTO_NO_VIGENTE,
                CausaRechazoQr.PROCESO_NO_VIGENTE, CausaRechazoQr.MESA_NO_VIGENTE,
                CausaRechazoQr.PRESIDENTE_NO_VIGENTE, CausaRechazoQr.USUARIO_NO_AUTORIZADO,
                CausaRechazoQr.JRV_INCOMPLETA};
        for (int i = 0; i < errores.length; i++) {
            boolean[] hechos = todoValido();
            hechos[i] = false;
            assertEquals(errores[i], diagnosticar(EstadoAccesoQr.EMITIDO, false, CIERRE, hechos));
            assertEquals(errores[i], diagnosticar(EstadoAccesoQr.CANJEADO, true, CIERRE, hechos));
        }
    }
}
