package ec.com.antenasur.dto;

import java.io.Serializable;

import ec.com.antenasur.model.Usuario;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Resultado de asegurar una cuenta y un rol sin duplicar relaciones. */
@Getter
@AllArgsConstructor
public class ResultadoProvisionUsuarioDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Usuario usuario;
    private final boolean reutilizado;
    private final boolean reactivado;
}
