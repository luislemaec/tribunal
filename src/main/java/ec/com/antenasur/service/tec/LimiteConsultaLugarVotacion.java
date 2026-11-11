package ec.com.antenasur.service.tec;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Límite de consultas públicas de lugar de votación por IP, en ventanas de un minuto (mismo
 * esquema que LimiteIntentosApi y LimiteIntentosQr). Frena la consulta masiva de cédulas;
 * acotado a un nodo, el proxy debe limitar también las solicitudes.
 */
@ApplicationScoped
public class LimiteConsultaLugarVotacion {

    static final int MAX_POR_IP = 15;
    private static final long VENTANA_MS = 60_000L;
    private static final int MAX_CLAVES = 10_000;

    private final Map<String, Ventana> ventanas = new HashMap<>();
    private final Clock reloj;

    public LimiteConsultaLugarVotacion() {
        this(Clock.systemUTC());
    }

    LimiteConsultaLugarVotacion(Clock reloj) {
        this.reloj = reloj;
    }

    /**
     * Registra una consulta de la IP y devuelve cuántas lleva en la ventana vigente. Se permite
     * mientras sea {@code <= MAX_POR_IP}; si la tabla está llena, se rechaza por precaución.
     */
    public synchronized int registrar(String ip) {
        long ahora = reloj.millis();
        ventanas.entrySet().removeIf(e -> e.getValue().hasta <= ahora);
        String clave = ip == null || ip.isBlank() ? "<sin-ip>" : ip.trim();
        Ventana ventana = ventanas.get(clave);
        if (ventana == null) {
            if (ventanas.size() >= MAX_CLAVES) {
                return Integer.MAX_VALUE;
            }
            ventana = new Ventana(ahora + VENTANA_MS);
            ventanas.put(clave, ventana);
        }
        return ++ventana.consultas;
    }

    public static boolean permitida(int consultas) {
        return consultas <= MAX_POR_IP;
    }

    /** Solo la primera consulta rechazada de la ventana deja rastro en la bitácora. */
    public static boolean primerRechazo(int consultas) {
        return consultas == MAX_POR_IP + 1;
    }

    private static final class Ventana {
        final long hasta;
        int consultas;

        Ventana(long hasta) {
            this.hasta = hasta;
        }
    }
}
