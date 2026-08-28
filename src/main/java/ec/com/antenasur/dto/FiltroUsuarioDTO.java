package ec.com.antenasur.dto;

import java.io.Serializable;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Criterios opcionales para la búsqueda paginada de usuarios activos. */
@Data
@NoArgsConstructor
public class FiltroUsuarioDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer rolId;
    private String username;
    private String nombres;
    private String iglesiaNombre;
    /** Cuando es true, la consulta muestra únicamente usuarios dados de baja. */
    private Boolean soloDadosBaja = false;
}
