package ec.com.antenasur.service.tec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;
import ec.com.antenasur.enums.TipoDocumentoMesa;
import ec.com.antenasur.dto.MesaDocumentosDTO;
import ec.com.antenasur.dto.RevisionActaFinalDTO;
import ec.com.antenasur.exception.NegocioException;

/** Verifica que una ruta alternativa no alcance repositorios antes de validar. */
class ComandosDocumentoMesaTest {
    private DisponibilidadDocumentoMesaService bloqueo() {
        return new DisponibilidadDocumentoMesaService() {
            @Override public MesaDocumentosDTO validar(TipoDocumentoMesa tipo, Integer proceso, Integer recinto, Integer mesa, boolean generar) {
                throw new NegocioException("denegado");
            }
            @Override public void validarFinal(Integer proceso, Integer mesa) { throw new NegocioException("denegado"); }
        };
    }
    @ParameterizedTest
    @EnumSource(value = TipoDocumentoMesa.class, names = {"PADRON_MESA", "ACTA_PARCIAL", "CERTIFICADOS_VOTACION"})
    void reutilizarDocumentoNoOmitePrecondiciones(TipoDocumentoMesa tipo) throws Exception {
        var s = new ReporteMesaService(); AccesoDocumentoMesaTest.inyectar(s, "disponibilidad", bloqueo());
        var e = assertThrows(NegocioException.class, () -> s.generarDocumentoMesa(tipo, false, 1,2,3,999,false));
        assertEquals("denegado", e.getMessage());
        assertThrows(NegocioException.class, () -> s.generarDocumentoMesa(tipo, true, 1,2,3,999,false));
    }
    @ParameterizedTest
    @EnumSource(value = TipoDocumentoMesa.class, names = {"DESIGNACION_MJRV"}, mode = EnumSource.Mode.EXCLUDE)
    void visualizarExigePrecondicionesAntesDeBuscarArchivo(TipoDocumentoMesa tipo) throws Exception {
        var s = new ReporteMesaService(); AccesoDocumentoMesaTest.inyectar(s, "disponibilidad", bloqueo());
        assertEquals("denegado", assertThrows(NegocioException.class,
                () -> s.obtenerDocumentoActivo(tipo,1,2,3,999,false)).getMessage());
    }
    @Test void servicioEscrutinioNoPermiteEludirRevisionFisica() throws Exception {
        var s = new EscrutinioService(); AccesoDocumentoMesaTest.inyectar(s, "disponibilidadDocumental", bloqueo());
        assertEquals("denegado", assertThrows(NegocioException.class,
                () -> s.validarResultadosFinalesDTO(1,2,3,new RevisionActaFinalDTO())).getMessage());
    }
    @Test void revisarNoPuedeMarcarValidadaSinCuadre() throws Exception {
        var s = new ActaFisicaEscrutinioService();
        AccesoDocumentoMesaTest.inyectar(s, "accesoDocumental", new AccesoDocumentoMesaService() {
            @Override public boolean esRevisor() { return true; }
        });
        assertThrows(NegocioException.class, () -> s.revisar(1, "VALIDADA", null, "usuario-enviado"));
    }
}
