package ec.com.antenasur.dto;

import java.io.Serializable;

import ec.com.antenasur.model.Rol;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Vista de la entidad {@link Rol}: rol del sistema RBAC. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String nombre;
    private String descripcion;

    public static RolDTO fromEntity(Rol r) {
        if (r == null) {
            return null;
        }
        return new RolDTO(r.getId(), r.getNombre(), r.getDescripcion());
    }

    public Rol toEntity() {
        Rol r = new Rol();
        r.setId(this.id);
        r.setNombre(this.nombre);
        r.setDescripcion(this.descripcion);
        return r;
    }
}
