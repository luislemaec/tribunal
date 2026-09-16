package ec.com.antenasur.security.qr;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Reglas comunes para canje y revalidacion; el contexto procede de BD. */
public final class ReglasAccesoQr {
	private ReglasAccesoQr() {
	}

	/**
	 * Resultado genérico que se devuelve al llamador y se audita. El detalle
	 * técnico se obtiene con {@link #diagnosticar}.
	 */
	public static ResultadoAccesoQr evaluar(EstadoAccesoQr estado, boolean sesion, Instant ahora, Instant desde,
			Instant hasta, Instant finSufragio, boolean documentoVersion, boolean documento, boolean proceso,
			boolean mesa, boolean presidente, boolean usuario, boolean junta) {
		return diagnosticar(estado, sesion, ahora, desde, hasta, finSufragio, documentoVersion, documento, proceso,
				mesa, presidente, usuario, junta).resultado();
	}

	/**
	 * La vigencia del token es la ventana almacenada en {@code acceso_qr_acta},
	 * que debe corresponder exactamente al cierre del SUFRAGIO y su ventana
	 * posterior. No se exige que {@code ahora} esté dentro del sufragio: el acta
	 * se canjea justamente después del cierre. La comprobación de
	 * correspondencia evita que un cambio de cronograma —o una fila emitida con
	 * una regla anterior— otorgue una vigencia distinta de la vigente.
	 */
	public static CausaRechazoQr diagnosticar(EstadoAccesoQr estado, boolean sesion, Instant ahora, Instant desde,
			Instant hasta, Instant finSufragio, boolean documentoVersion, boolean documento, boolean proceso,
			boolean mesa, boolean presidente, boolean usuario, boolean junta) {
		if (estado == EstadoAccesoQr.REVOCADO)
			return CausaRechazoQr.REVOCADO;
		if (estado == null)
			return CausaRechazoQr.ESTADO_DESCONOCIDO;
		if (sesion && estado != EstadoAccesoQr.CANJEADO)
			return CausaRechazoQr.SESION_NO_CANJEADA;
		if (!sesion && estado == EstadoAccesoQr.CANJEADO)
			return CausaRechazoQr.YA_CANJEADO;
		if (finSufragio == null)
			return CausaRechazoQr.SUFRAGIO_NO_CONFIGURADO;
		if (desde == null || hasta == null || ahora == null || !desde.isBefore(hasta))
			return CausaRechazoQr.VENTANA_INVALIDA;
		if (!ventanaCorrespondeAlCierre(desde, hasta, finSufragio))
			return CausaRechazoQr.VENTANA_NO_CORRESPONDE_A_CIERRE_SUFRAGIO;
		if (ahora.isBefore(desde))
			return CausaRechazoQr.ANTES_DE_VIGENCIA;
		if (!ahora.isBefore(hasta))
			return CausaRechazoQr.FUERA_DE_VIGENCIA;
		if (!documentoVersion)
			return CausaRechazoQr.DOCUMENTO_VERSION_INVALIDA;
		if (!documento)
			return CausaRechazoQr.DOCUMENTO_NO_VIGENTE;
		if (!proceso)
			return CausaRechazoQr.PROCESO_NO_VIGENTE;
		if (!mesa)
			return CausaRechazoQr.MESA_NO_VIGENTE;
		if (!presidente)
			return CausaRechazoQr.PRESIDENTE_NO_VIGENTE;
		if (!usuario)
			return CausaRechazoQr.USUARIO_NO_AUTORIZADO;
		if (!junta)
			return CausaRechazoQr.JRV_INCOMPLETA;
		return CausaRechazoQr.VALIDO;
	}

	/**
	 * La ventana almacenada debe ser exactamente [cierre, cierre + vigencia].
	 * Se compara al segundo porque la fecha viaja entre {@code TIMESTAMP} y
	 * {@code TIMESTAMP WITH TIME ZONE}; el cronograma se define al minuto.
	 */
	public static boolean ventanaCorrespondeAlCierre(Instant desde, Instant hasta, Instant finSufragio) {
		if (desde == null || hasta == null || finSufragio == null)
			return false;
		return coincide(desde, finSufragio)
				&& coincide(hasta, finSufragio.plus(VentanaAccesoQr.VIGENCIA_POSTERIOR));
	}

	private static boolean coincide(Instant uno, Instant otro) {
		return uno.truncatedTo(ChronoUnit.SECONDS).equals(otro.truncatedTo(ChronoUnit.SECONDS));
	}

	public static boolean enVentana(Instant ahora, Instant desde, Instant hasta) {
		return ahora != null && desde != null && hasta != null && desde.isBefore(hasta) && !ahora.isBefore(desde)
				&& ahora.isBefore(hasta);
	}
}
