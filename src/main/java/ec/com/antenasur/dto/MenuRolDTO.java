package ec.com.antenasur.dto;

import java.io.Serializable;

import ec.com.antenasur.model.MenuRol;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista de {@link MenuRol}: relación menu↔rol. Embebe los DTOs de Menu y
 * Rol. Mantiene {@code estado} (no es soft-delete: marca si el permiso está
 * concedido o no).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuRolDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private MenuDTO menu;
    private RolDTO rol;
    private Boolean estado;

    public static MenuRolDTO fromEntity(MenuRol mr) {
        if (mr == null) {
            return null;
        }
        MenuRolDTO dto = new MenuRolDTO();
        dto.setId(mr.getId());
        dto.setMenu(MenuDTO.fromEntity(mr.getMenu()));
        dto.setRol(RolDTO.fromEntity(mr.getRol()));
        dto.setEstado(mr.getEstado());
        return dto;
    }
}
