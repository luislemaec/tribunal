package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Resultado de la consulta pública de lugar de votación. Solo lo necesario para que el elector
 * acuda a votar: nombre enmascarado (para confirmar que es él), proceso, fecha del sufragio si
 * está configurada, recinto, mesa, parroquia y cantón. No incluye la cédula completa ni la
 * iglesia. Clase con getters (no record) porque la vista la lee por EL.
 */
@Getter
@AllArgsConstructor
public class LugarVotacionPublicoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String nombreEnmascarado;
    private final String proceso;
    private final Date fechaSufragio;
    private final String recinto;
    private final String mesa;
    private final String parroquia;
    private final String canton;
}
