package ec.com.antenasur.service.tec;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Caché breve de la consulta pública por cédula: las consultas repetidas (la misma persona que
 * vuelve a consultar, o varias que consultan por un familiar) no llegan a la base. Guarda solo el
 * resultado ya enmascarado. Vigencia configurable con {@code tec.consulta.cache.segundos} (60);
 * 0 la desactiva. Como máximo {@value #MAX_ENTRADAS} cédulas.
 */
@ApplicationScoped
public class CacheConsultaLugarVotacion {

    static final int MAX_ENTRADAS = 5_000;

    private record Entrada(ConsultaLugarVotacionService.Resultado resultado, long vence) {
    }

    private final Map<String, Entrada> entradas = new ConcurrentHashMap<>();
    private final Clock reloj;
    private final long vigenciaMs;

    public CacheConsultaLugarVotacion() {
        this(Clock.systemUTC(), Long.getLong("tec.consulta.cache.segundos", 60L) * 1000L);
    }

    CacheConsultaLugarVotacion(Clock reloj, long vigenciaMs) {
        this.reloj = reloj;
        this.vigenciaMs = Math.max(0L, vigenciaMs);
    }

    public ConsultaLugarVotacionService.Resultado obtener(String cedula) {
        if (vigenciaMs == 0) {
            return null;
        }
        Entrada entrada = entradas.get(cedula);
        if (entrada == null) {
            return null;
        }
        if (entrada.vence() < reloj.millis()) {
            entradas.remove(cedula, entrada);
            return null;
        }
        return entrada.resultado();
    }

    public void guardar(String cedula, ConsultaLugarVotacionService.Resultado resultado) {
        if (vigenciaMs == 0) {
            return;
        }
        long ahora = reloj.millis();
        if (entradas.size() >= MAX_ENTRADAS) {
            entradas.values().removeIf(e -> e.vence() < ahora);
            if (entradas.size() >= MAX_ENTRADAS) {
                return;
            }
        }
        entradas.put(cedula, new Entrada(resultado, ahora + vigenciaMs));
    }
}
