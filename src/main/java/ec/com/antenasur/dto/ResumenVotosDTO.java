package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import ec.com.antenasur.model.tec.Mesa;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agregado de mesas + escrutadas + cuadre, calculado por
 * {@code MesaService.calcularResumenVotos(...)}. La vista lo usa para mostrar
 * el indicador "X% de mesas escrutadas" y el padrón total de votantes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumenVotosDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Mesa> mesas = Collections.emptyList();
    private List<Mesa> mesasEscrutadas = Collections.emptyList();
    private int totalVotantes;
    private float porcentajeMesasEscrutadas;
}
