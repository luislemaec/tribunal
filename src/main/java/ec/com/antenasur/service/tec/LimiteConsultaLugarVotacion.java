package ec.com.antenasur.service.tec;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Límites de la consulta pública de lugar de votación, en dos capas:
 * <ul>
 * <li>por IP, en ventanas de un minuto (mismo esquema que LimiteIntentosApi y LimiteIntentosQr).
 * Holgado porque muchas personas comparten IP (red móvil, router de una comunidad) y los robots
 * ya los frena el desafío anti-robot: propiedad {@code tec.consulta.limite.ip}, 30 por minuto;</li>
 * <li>global por segundo, para proteger al servidor y a la base ante picos o ataques
 * distribuidos: propiedad {@code tec.consulta.limite.global}, 20 por segundo.</li>
 * </ul>
 * Acotado a un nodo; Nginx limita además la página (docs/deployment/tribunal.nginx.conf).
 */
@ApplicationScoped
public class LimiteConsultaLugarVotacion {

    static final int MAX_POR_IP = Integer.getInteger("tec.consulta.limite.ip", 30);
    static final int MAX_GLOBAL_POR_SEGUNDO = Integer.getInteger("tec.consulta.limite.global", 20);
    private static final long VENTANA_MS = 60_000L;
    private static final int MAX_CLAVES = 10_000;

    private final Map<String, Ventana> ventanas = new HashMap<>();
    private final Clock reloj;
    private long segundoGlobal = -1;
    private int consultasGlobales;

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

    /** Tope global: true si la consulta cabe en el cupo del segundo actual. */
    public synchronized boolean permitirGlobal() {
        long segundo = reloj.millis() / 1000;
        if (segundo != segundoGlobal) {
            segundoGlobal = segundo;
            consultasGlobales = 0;
        }
        return ++consultasGlobales <= MAX_GLOBAL_POR_SEGUNDO;
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
