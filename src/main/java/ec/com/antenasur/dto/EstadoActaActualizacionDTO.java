package ec.com.antenasur.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Estado de disponibilidad del acta de actualizacion de miembros. */
@Getter
@AllArgsConstructor
public class EstadoActaActualizacionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int totalMiembros;
    private final int miembrosActualizados;
    private final boolean puedeGenerar;
    private final Integer documentoId;
}
