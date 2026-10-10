package ec.com.antenasur.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import lombok.Getter;
import lombok.Setter;

/** Usuario con al menos una sesión vigente (web o App) en la pestaña Actividad. */
@Getter
public class UsuarioEnLineaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String WEB = "Web";
    public static final String APP = "App";

    private final String usuario;
    private final Set<String> canales = new TreeSet<>();
    private Instant desde;

    @Setter
    private String nombre;
    @Setter
    private String roles;

    public UsuarioEnLineaDTO(String usuario) {
        this.usuario = usuario;
    }

    /** Suma un canal; conserva el inicio más antiguo entre sus sesiones. */
    public void agregar(String canal, Instant inicio) {
        canales.add(canal);
        if (inicio != null && (desde == null || inicio.isBefore(desde))) {
            desde = inicio;
        }
    }

    public String getCanalesTexto() {
        return String.join(" + ", canales);
    }

    /** Para f:convertDateTime, que trabaja con java.util.Date. */
    public Date getDesdeFecha() {
        return desde == null ? null : Date.from(desde);
    }
}
