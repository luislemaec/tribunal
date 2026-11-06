package ec.com.antenasur.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Contrato de la entrada EXPORTAR_REPORTE_EXCEL (specs/001-auditoria-exportaciones-excel/contracts). */
class ExportacionExcelRegistrosTest {

    private static String actividad(String detalle) {
        return ExportacionExcelRegistros.ACCION_EXPORTAR + " | " + detalle;
    }

    @Test
    void formatoExactoDelContrato() {
        assertEquals("MÓDULO: PADRÓN; RESULTADO: EXITOSO; DETALLE: Padrón – Mesa; Filtro: Recinto: ESCUELA CENTRAL; Mesa: 1;"
                + " Registros: 154; Archivo: empadronados_ESCUELA_CENTRAL_1.xlsx",
                ExportacionExcelRegistros.componerDetalle("PADRÓN", "Padrón – Mesa; Filtro: Recinto: ESCUELA CENTRAL; Mesa: 1",
                        154, "empadronados_ESCUELA_CENTRAL_1.xlsx"));
    }

    @Test
    void descripcionLargaSeAcortaSinPerderCantidadNiArchivo() {
        String filtros = "Personas – Miembros; Filtro: " + "Parroquia MUY LARGA ".repeat(30);
        String detalle = ExportacionExcelRegistros.componerDetalle("PERSONAS E IGLESIAS", filtros, 1234,
                "personas_20261106_1530.xlsx");
        assertEquals(ExportacionExcelRegistros.LARGO_ACTIVIDAD, actividad(detalle).length());
        assertTrue(detalle.startsWith("MÓDULO: PERSONAS E IGLESIAS; RESULTADO: EXITOSO; DETALLE: Personas – Miembros"));
        assertTrue(detalle.endsWith("; Registros: 1234; Archivo: personas_20261106_1530.xlsx"));
    }

    @Test
    void descripcionCortaNoSeModifica() {
        String detalle = ExportacionExcelRegistros.componerDetalle("PERSONAS E IGLESIAS", "Iglesias – Listado; Filtro: Todos",
                12, "iglesias.xlsx");
        assertTrue(actividad(detalle).length() <= ExportacionExcelRegistros.LARGO_ACTIVIDAD);
        assertTrue(detalle.contains("DETALLE: Iglesias – Listado; Filtro: Todos; Registros: 12; Archivo: iglesias.xlsx"));
    }

    @Test
    void descripcionVaciaOSaltosDeLinea() {
        assertTrue(ExportacionExcelRegistros.componerDetalle("PADRÓN", null, 1, "a.xlsx")
                .contains("DETALLE: ; Registros: 1; Archivo: a.xlsx"));
        assertTrue(ExportacionExcelRegistros.componerDetalle("PADRÓN", "Linea 1\nLinea 2", 1, "a.xlsx")
                .contains("DETALLE: Linea 1 Linea 2;"));
    }
}
