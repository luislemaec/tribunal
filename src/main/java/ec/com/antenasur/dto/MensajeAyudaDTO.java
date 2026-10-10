package ec.com.antenasur.dto;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * Mensaje de la conversación del chatbot de ayuda. Los del asistente pueden llevar una
 * respuesta aprobada (pregunta, texto, enlace), opciones para elegir y los canales de
 * contacto. Todo es texto plano: la vista lo escapa.
 */
@Getter
public class MensajeAyudaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Identificador estable para acciones sobre el mensaje (no depende de su posición). */
    private final String id = java.util.UUID.randomUUID().toString();
    private final boolean delUsuario;
    private final String texto;
    private final LocalTime hora = LocalTime.now().withNano(0);

    /** Respuesta aprobada que contesta este mensaje del asistente (null si no hay). */
    @Setter
    private AyudaRespuestaDTO respuesta;

    /** Preguntas para elegir con un clic (sugerencias o «también puede interesarle»). */
    private final List<AyudaRespuestaDTO> opciones = new ArrayList<>();

    /** Mostrar los canales oficiales de contacto bajo el mensaje. */
    @Setter
    private boolean conContacto;

    /** Ya valorado («¿Le sirvió?») en esta conversación. */
    @Setter
    private boolean valorado;

    /** Recién agregado: la vista lo revela de forma progresiva una sola vez. */
    @Setter
    private boolean nuevo = true;

    private MensajeAyudaDTO(boolean delUsuario, String texto) {
        this.delUsuario = delUsuario;
        this.texto = texto;
    }

    public static MensajeAyudaDTO delUsuario(String texto) {
        return new MensajeAyudaDTO(true, texto);
    }

    public static MensajeAyudaDTO delAsistente(String texto) {
        return new MensajeAyudaDTO(false, texto);
    }

    public String getHoraTexto() {
        return String.format("%02d:%02d", hora.getHour(), hora.getMinute());
    }
}
