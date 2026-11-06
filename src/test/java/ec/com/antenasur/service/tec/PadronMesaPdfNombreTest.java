package ec.com.antenasur.service.tec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import ec.com.antenasur.controller.ReporteMesaController;
import ec.com.antenasur.dto.DocumentoDTO;
import ec.com.antenasur.dto.MesaDTO;
import ec.com.antenasur.dto.ReporteMesaDTO;
import ec.com.antenasur.enums.TipoDocumentoMesa;

/** Nombre de descarga y bitácora del padrón de mesa en PDF. */
class PadronMesaPdfNombreTest {

    @Test
    void nombreConMesaYVersion() {
        assertEquals("Padron_Mesa_MESA-1_V2", ReporteMesaService.nombrePadron(reporte("MESA 1", 7), 2));
    }

    @Test
    void nombreSinTildesNiCaracteresEspeciales() {
        assertEquals("Padron_Mesa_MESA-N-3-Anejo-Nunez_V1",
                ReporteMesaService.nombrePadron(reporte("  MESA Nº 3 / Anéjo  Núñez ", 7), 1));
    }

    @Test
    void nombreSinNombreDeMesaUsaElId() {
        assertEquals("Padron_Mesa_7_V3", ReporteMesaService.nombrePadron(reporte(null, 7), 3));
    }

    @Test
    void bitacoraDelPadronIdentificaFormatoYArchivo() {
        DocumentoDTO documento = documento("PADRON-1-M7-20261106-200000-abcd1234", "Padron_Mesa_MESA-1_V2", ".pdf");
        assertEquals("PADRON-1-M7-20261106-200000-abcd1234; Formato: PDF; Archivo: Padron_Mesa_MESA-1_V2.pdf",
                ReporteMesaController.datosBitacora(TipoDocumentoMesa.PADRON_MESA, documento));
    }

    @Test
    void bitacoraDeOtrosDocumentosSinCambios() {
        DocumentoDTO documento = documento("ACTA-PARCIAL-1", "ACTA-PARCIAL-1", ".pdf");
        assertEquals("ACTA-PARCIAL-1",
                ReporteMesaController.datosBitacora(TipoDocumentoMesa.ACTA_PARCIAL, documento));
    }

    private static ReporteMesaDTO reporte(String nombreMesa, int idMesa) {
        MesaDTO mesa = new MesaDTO();
        mesa.setId(idMesa);
        mesa.setNombre(nombreMesa);
        ReporteMesaDTO reporte = new ReporteMesaDTO();
        reporte.setMesa(mesa);
        return reporte;
    }

    private static DocumentoDTO documento(String codigo, String nombre, String extension) {
        DocumentoDTO documento = new DocumentoDTO();
        documento.setCodigo(codigo);
        documento.setNombre(nombre);
        documento.setExtension(extension);
        return documento;
    }
}
