package ec.com.antenasur.security.qr;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Limitador acotado para un nodo; el proxy debe limitar tambien las
 * solicitudes.
 */
@ApplicationScoped
public class LimiteIntentosQr {
	private final Map<String, Ventana> ventanas = new HashMap<>();
	private final Clock reloj;

	public LimiteIntentosQr() {
		this(Clock.systemUTC());
	}

	LimiteIntentosQr(Clock reloj) {
		this.reloj = reloj;
	}

	public synchronized boolean permitir(String ip, boolean canje) {
		long ahora = reloj.millis();
		ventanas.entrySet().removeIf(e -> e.getValue().hasta <= ahora);
		String clave = (canje ? "POST:" : "GET:") + ip;
		Ventana ventana = ventanas.get(clave);
		if (ventana == null) {
			if (ventanas.size() >= 10000)
				return false;
			ventana = new Ventana(ahora + 60000);
			ventanas.put(clave, ventana);
		}
		return ++ventana.intentos <= (canje ? 10 : 60);
	}

	private static final class Ventana {
		final long hasta;
		int intentos;

		Ventana(long hasta) {
			this.hasta = hasta;
		}
	}
}
