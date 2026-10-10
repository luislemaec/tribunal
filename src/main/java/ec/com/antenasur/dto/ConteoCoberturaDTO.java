package ec.com.antenasur.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

/**
 * Fila de la pestaña Cobertura del padrón: personas habilitadas y, de ellas, las
 * que ya están en el padrón del proceso activo. Personas distintas; el total
 * (id nulo) no es la suma de las categorías.
 */
@Getter
public class ConteoCoberturaDTO implements Serializable, CategoriaGeograficaDTO {

    private static final long serialVersionUID = 1L;

    private final Integer id;
    private final String nombre;
    private final String provincia;
    private final long habilitados;
    private final long empadronados;

    @Setter
    private String etiqueta;

    public ConteoCoberturaDTO(Integer id, String nombre, String provincia, Long habilitados, Long empadronados) {
        this.id = id;
        this.nombre = nombre == null ? "" : nombre;
        this.provincia = provincia;
        this.habilitados = habilitados == null ? 0L : habilitados;
        this.empadronados = empadronados == null ? 0L : empadronados;
        this.etiqueta = this.nombre;
    }

    /** Habilitados que todavía no tienen mesa en el proceso activo. */
    public long getSinPadron() {
        return Math.max(0L, habilitados - empadronados);
    }

    public int getPorcentajeCobertura() {
        return habilitados == 0 ? 0 : (int) Math.round(empadronados * 100.0 / habilitados);
    }
}
