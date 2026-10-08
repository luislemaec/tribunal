package ec.com.antenasur.security.api;

import java.time.Clock;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Límite de intentos de inicio de sesión de la API móvil, por IP y por usuario, en ventanas
 * de un minuto (mismo esquema que LimiteIntentosQr). Acotado a un nodo: el proxy debe limitar
 * también las solicitudes.
 */
@ApplicationScoped
public class LimiteIntentosApi {

    static final int MAX_POR_IP = 10;
    static final int MAX_POR_USUARIO = 5;
    private static final long VENTANA_MS = 60_000L;
    private static final int MAX_CLAVES = 10_000;

    private final Map<String, Ventana> ventanas = new HashMap<>();
    private final Clock reloj;

    public LimiteIntentosApi() {
        this(Clock.systemUTC());
    }

    LimiteIntentosApi(Clock reloj) {
        this.reloj = reloj;
    }

    /** Registra un intento y devuelve {@code false} si la IP o el usuario superaron su límite. */
    public synchronized boolean permitir(String ip, String usuario) {
        long ahora = reloj.millis();
        ventanas.entrySet().removeIf(e -> e.getValue().hasta <= ahora);
        boolean ipPermitida = sumar("IP:" + ip, ahora) <= MAX_POR_IP;
        boolean usuarioPermitido = usuario == null
                || sumar("USR:" + usuario.trim().toLowerCase(Locale.ROOT), ahora) <= MAX_POR_USUARIO;
        return ipPermitida && usuarioPermitido;
    }

    private int sumar(String clave, long ahora) {
        Ventana ventana = ventanas.get(clave);
        if (ventana == null) {
            if (ventanas.size() >= MAX_CLAVES) {
                return Integer.MAX_VALUE;
            }
            ventana = new Ventana(ahora + VENTANA_MS);
            ventanas.put(clave, ventana);
        }
        return ++ventana.intentos;
    }

    private static final class Ventana {
        final long hasta;
        int intentos;

        Ventana(long hasta) {
            this.hasta = hasta;
        }
    }
}
