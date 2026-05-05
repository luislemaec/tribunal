package ec.com.antenasur.dto;

import java.io.Serializable;

import ec.com.antenasur.model.tec.Cargo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Vista de la entidad {@link Cargo}: cargo institucional. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CargoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String nombre;

    public static CargoDTO fromEntity(Cargo c) {
        if (c == null) {
            return null;
        }
        return new CargoDTO(c.getId(), c.getNombre());
    }

    public Cargo toEntity() {
        Cargo c = new Cargo();
        c.setId(this.id);
        c.setNombre(this.nombre);
        return c;
    }
}
