package ec.com.antenasur.dto;

import java.io.Serializable;

import ec.com.antenasur.model.RolUsuario;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista compuesta de {@link RolUsuario}: relación rol↔usuario. Embebe los
 * DTOs de Rol y Usuario para que la vista exponga el nombre del rol y el
 * username sin acceder a entidades.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolUsuarioDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private RolDTO rol;
    private UsuarioDTO usuario;

    public static RolUsuarioDTO fromEntity(RolUsuario ru) {
        if (ru == null) {
            return null;
        }
        RolUsuarioDTO dto = new RolUsuarioDTO();
        dto.setId(ru.getId());
        dto.setRol(RolDTO.fromEntity(ru.getRol()));
        dto.setUsuario(UsuarioDTO.fromEntity(ru.getUsuario()));
        return dto;
    }
}
