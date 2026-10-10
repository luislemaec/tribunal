package ec.com.antenasur.security.sesion;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.servlet.http.HttpSession;

/**
 * Sesiones web autenticadas vigentes, en memoria. Se alta tras un login correcto
 * (LoginController) y se da de baja cuando la sesión HTTP se destruye por cierre o
 * por inactividad ({@link SesionesWebListener}).
 *
 * <p>La clave es un token guardado como atributo de la sesión y no el id de sesión,
 * que el contenedor puede cambiar tras el login. Solo cuenta las sesiones de este
 * nodo: con WildFly en clúster cada nodo vería únicamente las suyas. Un reinicio la
 * deja vacía, igual que a las sesiones.</p>
 */
@ApplicationScoped
public class SesionesWebEnLinea {

    static final String ATRIBUTO = SesionesWebEnLinea.class.getName() + ".token";

    /** Usuario y momento del login de una sesión web vigente. */
    public record SesionWeb(String usuario, Instant desde) { }

    private final Map<String, SesionWeb> sesiones = new ConcurrentHashMap<>();

    public void registrar(HttpSession sesion, String usuario) {
        if (sesion == null || usuario == null || usuario.isBlank()) {
            return;
        }
        Object anterior = sesion.getAttribute(ATRIBUTO);
        String token = anterior instanceof String t ? t : UUID.randomUUID().toString();
        sesion.setAttribute(ATRIBUTO, token);
        sesiones.put(token, new SesionWeb(usuario, Instant.now()));
    }

    void quitar(HttpSession sesion) {
        Object token = sesion == null ? null : sesion.getAttribute(ATRIBUTO);
        if (token instanceof String t) {
            sesiones.remove(t);
        }
    }

    public List<SesionWeb> vigentes() {
        return List.copyOf(sesiones.values());
    }
}
