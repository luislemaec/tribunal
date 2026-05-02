package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resultado de la fase de autenticación contra el dominio: usuario, persona
 * asociada y roles. Todos los campos son DTOs — el controller no recibe
 * entidades.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthDataDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private UsuarioDTO usuario;
    private PersonaDTO persona;
    private List<RolUsuarioDTO> rolesUsuario = new ArrayList<>();
    private List<String> nombresRoles = new ArrayList<>();

    public boolean isResolved() {
        return usuario != null && rolesUsuario != null && !rolesUsuario.isEmpty();
    }
}
