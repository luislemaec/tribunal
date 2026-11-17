package ec.com.antenasur.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Persona encontrada por cédula para designarla en la JRV desde otra mesa (mjrv.xhtml).
 * Clase con getters (no record): la lee EL en el diálogo.
 */
@Getter
@AllArgsConstructor
public class CandidatoJrvDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Integer iglesiaPersonaId;
    private final String cedula;
    private final String nombres;
    private final String iglesia;
    /** Mesa y recinto donde está empadronada (y donde sigue votando). */
    private final String mesaVotacion;
    private final String recintoVotacion;
    /** Ya pertenece al padrón de la mesa seleccionada: se designa sin advertencia. */
    private final boolean mismaMesa;
}
