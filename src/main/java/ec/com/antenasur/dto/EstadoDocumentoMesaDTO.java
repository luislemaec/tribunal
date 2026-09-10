package ec.com.antenasur.dto;

import java.io.Serializable;
import lombok.Value;

/** Resultado de las reglas del servicio; la vista no consulta persistencia. */
@Value
public class EstadoDocumentoMesaDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    boolean puedeGenerar;
    boolean puedeRegenerar;
    boolean puedeVisualizar;
    boolean bloqueado;
    String motivoBloqueo;
    String estado;
}
