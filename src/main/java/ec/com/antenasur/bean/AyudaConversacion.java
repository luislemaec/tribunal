package ec.com.antenasur.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.SessionScoped;

import ec.com.antenasur.dto.MensajeAyudaDTO;
import lombok.Getter;
import lombok.Setter;

/**
 * Conversación del chatbot de ayuda durante la sesión: se conserva al cambiar de pantalla
 * y se descarta al cerrar sesión. Solo en memoria; nunca se guarda en la base de datos.
 */
@SessionScoped
public class AyudaConversacion implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Tope de mensajes: los más antiguos se descartan. */
    private static final int MAX_MENSAJES = 40;

    @Getter
    private final List<MensajeAyudaDTO> mensajes = new ArrayList<>();

    /** Última pantalla para la que se ofrecieron sugerencias. */
    @Getter
    @Setter
    private String ultimaPaginaSugerida;

    public synchronized void agregar(MensajeAyudaDTO mensaje) {
        mensajes.forEach(m -> m.setNuevo(false));
        mensajes.add(mensaje);
        while (mensajes.size() > MAX_MENSAJES) {
            mensajes.remove(0);
        }
    }

    /** Tras mostrarlos: no se vuelven a animar al redibujar la conversación. */
    public synchronized void marcarVistos() {
        mensajes.forEach(m -> m.setNuevo(false));
    }

    public synchronized void reiniciar() {
        mensajes.clear();
        ultimaPaginaSugerida = null;
    }

    public boolean isVacia() {
        return mensajes.isEmpty();
    }
}
