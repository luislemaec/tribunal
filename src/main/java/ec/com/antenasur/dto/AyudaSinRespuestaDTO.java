package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Consulta del chatbot que no encontró respuesta (anónima y enmascarada). */
@Getter
@AllArgsConstructor
public class AyudaSinRespuestaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Integer id;
    private final String texto;
    private final String pagina;
    private final int veces;
    private final Date ultimaFecha;
}
