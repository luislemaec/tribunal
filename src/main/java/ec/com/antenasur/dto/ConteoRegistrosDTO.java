package ec.com.antenasur.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

/**
 * Fila del reporte Rep. Registros: conteos de personas distintas de una
 * categoría geográfica (cantón o parroquia), o el total cuando {@code id} es
 * nulo. Solo las personas con vínculo, persona e iglesia activos cuentan como
 * registradas.
 */
@Getter
public class ConteoRegistrosDTO implements Serializable, CategoriaGeograficaDTO {

    private static final long serialVersionUID = 1L;

    private final Integer id;
    private final String nombre;
    private final String provincia;
    private final long registrados;
    private final long habilitados;
    private final long pendientesRevision;

    /** Nombre mostrado; incluye la provincia cuando otro cantón se llama igual. */
    @Setter
    private String etiqueta;

    public ConteoRegistrosDTO(Integer id, String nombre, String provincia, Long registrados, Long habilitados,
            Long pendientesRevision) {
        this.id = id;
        this.nombre = nombre == null ? "" : nombre;
        this.provincia = provincia;
        this.registrados = registrados == null ? 0L : registrados;
        this.habilitados = habilitados == null ? 0L : habilitados;
        this.pendientesRevision = pendientesRevision == null ? 0L : pendientesRevision;
        this.etiqueta = this.nombre;
    }

    public long getNoHabilitados() {
        return Math.max(0L, registrados - habilitados);
    }

    public long getRevisados() {
        return Math.max(0L, registrados - pendientesRevision);
    }

    /** Porcentaje de habilitados sobre los registrados de la misma categoría. */
    public int getPorcentajeHabilitados() {
        return registrados == 0 ? 0 : (int) Math.round(habilitados * 100.0 / registrados);
    }
}
