package ec.com.antenasur.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

/**
 * Fila de la pestaña Iglesias: iglesias activas de un cantón o parroquia y
 * cuántas tienen administrador, lista de miembros cargada y al menos una persona
 * habilitada. El total (id nulo) usa la misma consulta sin agrupar.
 */
@Getter
public class ConteoIglesiasDTO implements Serializable, CategoriaGeograficaDTO {

    private static final long serialVersionUID = 1L;

    private final Integer id;
    private final String nombre;
    private final String provincia;
    private final long iglesias;
    private final long conAdministrador;
    private final long conListaMiembros;
    private final long conHabilitados;

    @Setter
    private String etiqueta;

    public ConteoIglesiasDTO(Integer id, String nombre, String provincia, Long iglesias, Long conAdministrador,
            Long conListaMiembros, Long conHabilitados) {
        this.id = id;
        this.nombre = nombre == null ? "" : nombre;
        this.provincia = provincia;
        this.iglesias = iglesias == null ? 0L : iglesias;
        this.conAdministrador = conAdministrador == null ? 0L : conAdministrador;
        this.conListaMiembros = conListaMiembros == null ? 0L : conListaMiembros;
        this.conHabilitados = conHabilitados == null ? 0L : conHabilitados;
        this.etiqueta = this.nombre;
    }

    public long getSinAdministrador() {
        return Math.max(0L, iglesias - conAdministrador);
    }

    public long getSinListaMiembros() {
        return Math.max(0L, iglesias - conListaMiembros);
    }

    public long getSinHabilitados() {
        return Math.max(0L, iglesias - conHabilitados);
    }
}
