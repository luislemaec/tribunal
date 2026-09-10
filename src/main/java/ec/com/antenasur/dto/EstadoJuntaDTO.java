package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Value;

/** Resultado de consultar la conformacion de una junta en una mesa y proceso. */
@Value
public class EstadoJuntaDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    boolean completa;
    int dignidadesAsignadas;
    List<String> cargosFaltantes;
}
