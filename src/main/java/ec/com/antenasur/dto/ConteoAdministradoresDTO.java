package ec.com.antenasur.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

/**
 * Fila agregada de la pestaña Administradores: iglesias de un cantón o parroquia
 * (o el total, id nulo) y el estado de sus administradores. Se acumula a partir de
 * las filas por iglesia; cada iglesia cuenta una sola vez.
 */
@Getter
public class ConteoAdministradoresDTO implements Serializable, CategoriaGeograficaDTO {

    private static final long serialVersionUID = 1L;

    private final Integer id;
    private final String nombre;
    private final String provincia;
    private long iglesias;
    private long conAdministrador;
    private long activados;
    private long sinAccesoReciente;

    @Setter
    private String etiqueta;

    public ConteoAdministradoresDTO(Integer id, String nombre, String provincia) {
        this.id = id;
        this.nombre = nombre == null ? "" : nombre;
        this.provincia = provincia;
        this.etiqueta = this.nombre;
    }

    public void acumular(AdministradorIglesiaDTO fila) {
        iglesias++;
        if (fila.isTieneAdministrador()) {
            conAdministrador++;
            if (fila.isAdministradorActivado()) {
                activados++;
            }
            if (fila.isSinAccesoReciente()) {
                sinAccesoReciente++;
            }
        }
    }

    public long getSinAdministrador() {
        return iglesias - conAdministrador;
    }

    public long getPendientesActivar() {
        return conAdministrador - activados;
    }

    public int getPorcentajeConAdministrador() {
        return iglesias == 0 ? 0 : (int) Math.round(conAdministrador * 100.0 / iglesias);
    }
}
