package ec.com.antenasur.util;

import java.util.List;
import java.util.Map;

import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;

/**
 * Base de las tablas paginadas en el servidor de TEC. Corrige el total de páginas cuando
 * la búsqueda cambia fuera de la tabla (formularios de filtros, buscadores, combos).
 *
 * <p>PrimeFaces 15 solo vuelve a contar los registros al cargar la primera página si su
 * total guardado es 0 ({@link LazyDataModel#calculateRowCount}). Como estas tablas filtran
 * con criterios del bean que PrimeFaces no conoce, tras buscar mostraba las filas nuevas
 * con el número de páginas anterior, y solo se corregía al navegar a otra página (allí sí
 * llama a {@code count}). Aquí cada carga de la primera página recuenta: si la página no
 * se llena, el total sale de las filas cargadas sin otra consulta; si se llena, se ejecuta
 * el mismo {@code count} de siempre.
 */
public abstract class ModeloLazy<T> extends LazyDataModel<T> {

    private static final long serialVersionUID = 1L;

    @Override
    public int calculateRowCount(List<?> datos, Map<String, FilterMeta> filtros, int primero, int filas) {
        if (primero == 0) {
            setRowCount(0);
        }
        return super.calculateRowCount(datos, filtros, primero, filas);
    }
}
