package ec.com.antenasur.dto;

import java.io.Serializable;

import lombok.Data;

/** Estado calculado del acta para la lista y proceso seleccionados. */
@Data
public class EstadoActaInscripcionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private int totalCargos;
    private int totalCandidatos;
    private int totalAutoridades;
    private boolean listaCompleta;
    private boolean autoridadesDisponibles;
    private boolean puedeGenerar;
    private String contextoHash;
    private String detalle;
    private Integer actaGeneradaId;
    private Integer actaFirmadaId;
}
