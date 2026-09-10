package ec.com.antenasur.service.tec;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import ec.com.antenasur.dto.EscrutinioDTO;
import ec.com.antenasur.dto.RevisionActaFinalDTO;

class RevisionActaFinalTest {
    private EscrutinioDTO votos(int id, String nombre, String tipo, Integer votos) {
        var r = new EscrutinioDTO(); r.setCategoriaId(id); r.setCategoriaNombre(nombre); r.setCategoriaTipo(tipo); r.setTotalVotos(votos); return r;
    }
    private RevisionActaFinalDTO revision() {
        var r = new RevisionActaFinalDTO();
        r.getResultados().add(votos(1, "LISTA 1", "LISTA", 10));
        r.getResultados().add(votos(2, "LISTA 2", "LISTA", 20));
        r.getResultados().add(votos(3, "BLANCOS", "ESPECIAL", 2));
        r.getResultados().add(votos(4, "NULOS", "ESPECIAL", 3));
        r.getResultados().add(votos(5, "PAPELETAS RESTANTES", "ESPECIAL", 80));
        r.setValidosDeclarados(30); r.setTotalDeclarado(35); return r;
    }
    @Test void sumaListasYExcluyePapeletasRestantes() {
        var r = revision(); assertTrue(r.isCuadrada()); assertEquals(35, r.getTotal()); assertFalse(r.isRevisada());
    }
    @Test void diferenciaDeValidosImpideConfirmar() {
        var r = revision(); r.setValidosDeclarados(31); assertEquals(1L, r.getDiferenciaValidos()); assertFalse(r.isCuadrada());
        assertEquals(10, r.getResultados().get(0).getTotalVotos());
    }
    @Test void diferenciaTotalImpideConfirmar() {
        var r = revision(); r.setTotalDeclarado(34); assertEquals(-1L, r.getDiferenciaTotal()); assertFalse(r.isCuadrada());
    }
    @Test void noUsaSumasComoDeclaracionAutomatica() {
        var r = revision(); r.setValidosDeclarados(null); r.setTotalDeclarado(null); assertFalse(r.isCuadrada());
    }
    @Test void categoriaDuplicadaImpideConfirmar() {
        var r = revision(); r.getResultados().add(r.getResultados().get(0)); assertFalse(r.isCuadrada());
    }
    @Test void rechazaNegativosNulosYDesconocidos() {
        var r = revision(); r.getResultados().get(0).setTotalVotos(-1); assertFalse(r.isCuadrada());
        r.getResultados().get(0).setTotalVotos(null); assertFalse(r.isCuadrada());
        r = revision(); r.getResultados().add(votos(6, "OTROS", "ESPECIAL", 0)); assertFalse(r.isCuadrada());
    }
    @Test void noDesbordaAlSumarEnteros() {
        var r = revision(); r.getResultados().get(0).setTotalVotos(Integer.MAX_VALUE);
        r.getResultados().get(1).setTotalVotos(Integer.MAX_VALUE);
        assertEquals(4294967294L, r.getValidos()); assertFalse(r.isCuadrada());
    }
    @Test void requiereCategoriasDeBlancosYNulosAunqueSuValorSeaCero() {
        var r = revision(); r.getResultados().remove(3); r.setTotalDeclarado(32); assertFalse(r.isCuadrada());
    }
}
