package ec.com.antenasur.service.tec;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import ec.com.antenasur.dto.DependenciasMesaDTO;
import ec.com.antenasur.enums.TipoDocumentoMesa;
import ec.com.antenasur.util.Constantes;

class ReglasDocumentoMesaTest {
    private DependenciasMesaDTO completa() {
        var d = new DependenciasMesaDTO();
        d.setEmpadronados(20); d.setHabilitados(20); d.setPersonasCertificado(20); d.setJuntaCompleta(true);
        return d;
    }
    private String motivo(TipoDocumentoMesa tipo, DependenciasMesaDTO d) {
        return DisponibilidadDocumentoMesaService.motivo(tipo, d, true, true, true);
    }
    @Test void padronVacioBloquea() {
        assertEquals(Constantes.getMensaje("reportesMesa.error.sin.padron"), motivo(TipoDocumentoMesa.PADRON_MESA, new DependenciasMesaDTO()));
    }
    @Test void padronNoExigeJuntaNiCierre() {
        var d = completa(); d.setJuntaCompleta(false);
        assertNull(motivo(TipoDocumentoMesa.PADRON_MESA, d));
    }
    @Test void certificadosExigenHabilitacion() {
        var d = completa(); d.setHabilitados(0);
        assertNotNull(motivo(TipoDocumentoMesa.CERTIFICADOS_VOTACION, d));
    }
    @Test void certificadosDuplicadosBloquean() {
        var d = completa(); d.setPersonasCertificado(19);
        assertNotNull(motivo(TipoDocumentoMesa.CERTIFICADOS_VOTACION, d));
    }
    @Test void certificadosExigenRol() {
        assertNotNull(DisponibilidadDocumentoMesaService.motivo(TipoDocumentoMesa.CERTIFICADOS_VOTACION, completa(), false, true, true));
    }
    @Test void actaConUnIntegranteNoSeHabilitaAunqueExistaArchivo() {
        var d = completa(); d.setJuntaCompleta(false); d.getCargosFaltantes().add("SECRETARIO DE MESA");
        String causa = motivo(TipoDocumentoMesa.ACTA_PARCIAL, d);
        assertTrue(causa.contains("SECRETARIO DE MESA"));
        var estado = DisponibilidadDocumentoMesaService.estado(true, true, causa, false);
        assertTrue(estado.isBloqueado()); assertFalse(estado.isPuedeVisualizar()); assertFalse(estado.isPuedeRegenerar());
    }
    @Test void formularioNoExigeConteoNiCierre() { assertNull(motivo(TipoDocumentoMesa.ACTA_PARCIAL, completa())); }
    @Test void formularioExigeFechaYCategorias() {
        assertNotNull(DisponibilidadDocumentoMesaService.motivo(TipoDocumentoMesa.ACTA_PARCIAL, completa(), true, false, true));
        assertNotNull(DisponibilidadDocumentoMesaService.motivo(TipoDocumentoMesa.ACTA_PARCIAL, completa(), true, true, false));
    }
    @Test void fotografiaNoHabilitaMesaAbierta() {
        var d = completa(); d.getDocumentos().put(TipoDocumentoMesa.ACTA_FISICA_ESCRUTINIO.getNombreTipo(), 1);
        assertEquals(Constantes.getMensaje("reportesMesa.regla.actaFisica.cierre"), motivo(TipoDocumentoMesa.ACTA_FISICA_ESCRUTINIO, d));
    }
    @Test void cerradaSinFotoEsPendiente() {
        var d = completa(); d.setCerrada(true);
        var e = DisponibilidadDocumentoMesaService.estado(false, false, motivo(TipoDocumentoMesa.ACTA_FISICA_ESCRUTINIO, d), true);
        assertEquals("PENDIENTE", e.getEstado()); assertFalse(e.isPuedeGenerar()); assertFalse(e.isPuedeVisualizar());
    }
    @Test void cerradaConFotoPermiteVerSinGenerar() {
        var d = completa(); d.setCerrada(true); d.getDocumentos().put(TipoDocumentoMesa.ACTA_FISICA_ESCRUTINIO.getNombreTipo(), 1);
        var e = DisponibilidadDocumentoMesaService.estado(false, true, motivo(TipoDocumentoMesa.ACTA_FISICA_ESCRUTINIO, d), false);
        assertTrue(e.isPuedeVisualizar()); assertFalse(e.isPuedeGenerar()); assertFalse(e.isPuedeRegenerar());
    }
    @Test void generadoPermiteVisualizarYRegenerar() {
        var e = DisponibilidadDocumentoMesaService.estado(true, true, null, false);
        assertTrue(e.isPuedeVisualizar()); assertTrue(e.isPuedeRegenerar()); assertFalse(e.isPuedeGenerar());
    }
    @Test void vicepresidenteNoEsPresidente() {
        assertEquals("PRESIDENTE", MiembroJRVService.normalizarCargo(" Presidente de Mesa "));
        assertNotEquals("PRESIDENTE", MiembroJRVService.normalizarCargo("VICEPRESIDENTE DE MESA"));
    }
}
