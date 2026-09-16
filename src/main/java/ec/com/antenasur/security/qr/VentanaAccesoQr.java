package ec.com.antenasur.security.qr;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;

/**
 * Ventana de canje del QR del Acta Parcial: se abre al cierre del SUFRAGIO y
 * dura {@link #VIGENCIA_POSTERIOR}. El PDF puede generarse antes, pero el token
 * solo se canjea dentro de esa ventana.
 *
 * <p>{@code tec.cronograma_fase.cref_fecha_fin} es {@code TIMESTAMP} sin zona
 * (hora de pared) y {@code tec.acceso_qr_acta.vigente_*} es
 * {@code TIMESTAMP WITH TIME ZONE} (instante absoluto). La conversión se hace
 * aquí de forma explícita contra {@link #ZONA}: si se dejara al driver JDBC, el
 * instante dependería de {@code user.timezone} del servidor y la ventana se
 * correría varias horas en un servidor configurado en UTC.
 */
public final class VentanaAccesoQr {

	/** Zona legal del proceso electoral. */
	public static final ZoneId ZONA = ZoneId.of("America/Guayaquil");

	/** Duración de la ventana de canje posterior al cierre del sufragio. */
	public static final Duration VIGENCIA_POSTERIOR = Duration.ofHours(6);

	private VentanaAccesoQr() {
	}

	/**
	 * Reinterpreta una fecha leída de una columna {@code TIMESTAMP} sin zona
	 * como hora de pared de {@link #ZONA}. El driver construye la fecha usando
	 * la zona por defecto de la JVM; aquí se recupera la hora de pared original
	 * y se ancla a la zona electoral, de modo que el resultado no depende de la
	 * configuración del servidor.
	 */
	public static Instant instante(Date fechaLocal) {
		if (fechaLocal == null)
			return null;
		return fechaLocal.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().atZone(ZONA).toInstant();
	}

	/** Inicio de vigencia del token: el cierre del sufragio. */
	public static Instant desde(Date finSufragio) {
		return instante(finSufragio);
	}

	/** Fin de vigencia del token: cierre del sufragio más {@link #VIGENCIA_POSTERIOR}. */
	public static Instant hasta(Date finSufragio) {
		Instant cierre = instante(finSufragio);
		return cierre == null ? null : cierre.plus(VIGENCIA_POSTERIOR);
	}
}
