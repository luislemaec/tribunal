package ec.com.antenasur.service.tec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import ec.com.antenasur.dto.IglesiaDTO;
import ec.com.antenasur.dto.RecintoDTO;
import ec.com.antenasur.dto.RecintoHistorialDTO;

/** Interpretación del historial Envers de un recinto y marca «Nuevo». */
class RecintoHistorialTest {

    /** rev, revtype, fecha, nombre, parroquia, estado, usuario, cantón, provincia. */
    private static Object[] fila(int rev, int tipo, String nombre, String parroquia, Boolean estado) {
        return new Object[] { rev, (short) tipo, new Date(), nombre, parroquia, estado, "admin", "GUAMOTE",
                "CHIMBORAZO" };
    }

    @Test
    void altaCambiosBajaYReactivacionDelMasRecienteAlMasAntiguo() {
        List<RecintoHistorialDTO> h = RecintoService.construirHistorial(Arrays.asList(
                fila(1, 0, "ESCUELA A", "CEBADAS", true),
                fila(2, 1, "ESCUELA B", "CEBADAS", true),
                fila(3, 1, "ESCUELA B", "PALMIRA", true),
                fila(4, 1, "ESCUELA B", "PALMIRA", false),
                fila(5, 1, "ESCUELA B", "PALMIRA", true)));
        assertEquals(5, h.size());
        assertEquals(RecintoHistorialDTO.ACCION_REACTIVA, h.get(0).getAccion());
        assertEquals(RecintoHistorialDTO.ACCION_DESACTIVA, h.get(1).getAccion());
        assertEquals(RecintoHistorialDTO.ACCION_ACTUALIZA, h.get(2).getAccion());
        assertTrue(h.get(2).cambio(RecintoHistorialDTO.CAMPO_PARROQUIA));
        assertEquals("CEBADAS", h.get(2).getParroquiaAnterior());
        assertFalse(h.get(2).cambio(RecintoHistorialDTO.CAMPO_NOMBRE));
        assertTrue(h.get(3).cambio(RecintoHistorialDTO.CAMPO_NOMBRE));
        assertEquals("ESCUELA A", h.get(3).getNombreAnterior());
        assertEquals(RecintoHistorialDTO.ACCION_CREA, h.get(4).getAccion());
        assertTrue(h.get(4).getCambiados().isEmpty());
        assertEquals("CHIMBORAZO", h.get(4).getProvincia());
    }

    @Test
    void recintoDeMigracionSinAltaAuditadaEsRegistroBase() {
        List<RecintoHistorialDTO> h = RecintoService.construirHistorial(
                Arrays.<Object[]>asList(fila(7, 1, "ESCUELA C", "TIXAN", true)));
        assertEquals(RecintoHistorialDTO.ACCION_BASE, h.get(0).getAccion());
        assertTrue(h.get(0).getCambiados().isEmpty());
    }

    @Test
    void eliminacionYListaVacia() {
        assertEquals(RecintoHistorialDTO.ACCION_ELIMINA, RecintoService.construirHistorial(
                Arrays.<Object[]>asList(fila(1, 0, "X", "P", true), fila(2, 2, null, null, null))).get(0).getAccion());
        assertTrue(RecintoService.construirHistorial(null).isEmpty());
    }

    @Test
    void nuevoConElMismoCriterioQueIglesias() {
        RecintoDTO recinto = new RecintoDTO();
        assertFalse(recinto.isNuevo(), "sin fecha de registro");
        recinto.setFechaCrea(new Date());
        assertTrue(recinto.isNuevo(), "registrado hoy");
        recinto.setFechaCrea(fecha(IglesiaDTO.INICIO_IDENTIFICADOR_NUEVA.minusDays(1)));
        assertFalse(recinto.isNuevo(), "antes del inicio del identificador");
        recinto.setFechaCrea(fecha(LocalDate.now().minusMonths(IglesiaDTO.MESES_VIGENCIA_NUEVA).minusDays(1)));
        assertFalse(recinto.isNuevo(), "vencida la vigencia");
    }

    private static Date fecha(LocalDate dia) {
        return Date.from(dia.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
