package ec.com.antenasur.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Total de páginas de las tablas lazy tras cambiar la búsqueda fuera de la tabla
 * (bitácora, usuarios, candidatos, padrón, personas). Reproduce la secuencia de
 * PrimeFaces 15: load de la primera página y luego calculateRowCount.
 */
class ModeloLazyTest {

    /** Datos simulados: el bean decide cuántos registros cumplen el filtro vigente. */
    private int totalFiltrado;
    private int conteos;

    private List<Integer> pagina(int primero, int filas) {
        List<Integer> r = new ArrayList<>();
        for (int i = primero; i < Math.min(primero + filas, totalFiltrado); i++) {
            r.add(i);
        }
        return r;
    }

    private int cargarPrimeraPagina(LazyDataModel<Integer> modelo, int filas) {
        List<Integer> datos = modelo.load(0, filas, Collections.emptyMap(), Collections.emptyMap());
        return modelo.calculateRowCount(datos, Collections.emptyMap(), 0, filas);
    }

    private LazyDataModel<Integer> original() {
        return new LazyDataModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public int count(Map<String, FilterMeta> f) {
                conteos++;
                return totalFiltrado;
            }

            @Override
            public List<Integer> load(int primero, int filas, Map<String, SortMeta> o, Map<String, FilterMeta> f) {
                return pagina(primero, filas);
            }
        };
    }

    private LazyDataModel<Integer> corregido() {
        return new ModeloLazy<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public int count(Map<String, FilterMeta> f) {
                conteos++;
                return totalFiltrado;
            }

            @Override
            public List<Integer> load(int primero, int filas, Map<String, SortMeta> o, Map<String, FilterMeta> f) {
                return pagina(primero, filas);
            }
        };
    }

    @Test
    void sinLaCorreccionElTotalQuedaDesactualizadoTrasBuscar() {
        LazyDataModel<Integer> modelo = original();
        totalFiltrado = 200;
        assertEquals(200, cargarPrimeraPagina(modelo, 25), "8 páginas");
        totalFiltrado = 40; // nueva búsqueda desde el formulario de filtros
        assertEquals(200, cargarPrimeraPagina(modelo, 25), "el defecto: sigue mostrando 8 páginas");
    }

    @Test
    void conLaCorreccionSeRecuentaAlBuscar() {
        LazyDataModel<Integer> modelo = corregido();
        totalFiltrado = 200;
        assertEquals(200, cargarPrimeraPagina(modelo, 25));
        totalFiltrado = 40;
        assertEquals(40, cargarPrimeraPagina(modelo, 25), "2 páginas tras buscar");
    }

    @Test
    void siLaPrimeraPaginaNoSeLlenaNoHaceOtraConsulta() {
        LazyDataModel<Integer> modelo = corregido();
        totalFiltrado = 200;
        cargarPrimeraPagina(modelo, 25);
        conteos = 0;
        totalFiltrado = 7;
        assertEquals(7, cargarPrimeraPagina(modelo, 25));
        assertEquals(0, conteos, "el total sale de las filas cargadas");
        totalFiltrado = 0;
        assertEquals(0, cargarPrimeraPagina(modelo, 25), "búsqueda sin resultados");
    }

    @Test
    void enOtrasPaginasNoCambiaElComportamiento() {
        LazyDataModel<Integer> modelo = corregido();
        totalFiltrado = 200;
        cargarPrimeraPagina(modelo, 25);
        List<Integer> datos = modelo.load(50, 25, Collections.emptyMap(), Collections.emptyMap());
        assertEquals(200, modelo.calculateRowCount(datos, Collections.emptyMap(), 50, 25));
    }
}
