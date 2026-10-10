package ec.com.antenasur.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Pregunta y respuesta que ve el usuario en el chatbot. {@code enlacePagina} solo llega
 * cuando el usuario puede abrir esa pantalla.
 */
@Getter
@AllArgsConstructor
public class AyudaRespuestaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Integer id;
    private final String pregunta;
    private final String respuesta;
    private final String enlacePagina;
}
