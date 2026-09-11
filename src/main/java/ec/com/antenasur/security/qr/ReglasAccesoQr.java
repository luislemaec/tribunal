package ec.com.antenasur.security.qr;

import java.time.Instant;

/** Reglas comunes para canje y revalidacion; el contexto procede de BD. */
public final class ReglasAccesoQr {
	private ReglasAccesoQr() {
	}

	public static ResultadoAccesoQr evaluar(EstadoAccesoQr estado, boolean sesion, Instant ahora, Instant desde,
			Instant hasta, Instant sufragioInicio, Instant sufragioFin, boolean documento, boolean proceso,
			boolean mesa, boolean presidente, boolean usuario, boolean junta) {
		if (estado == EstadoAccesoQr.REVOCADO)
			return ResultadoAccesoQr.REVOCADO;
		if (estado == null || (sesion && estado != EstadoAccesoQr.CANJEADO))
			return ResultadoAccesoQr.TOKEN_INVALIDO;
		if (!sesion && estado == EstadoAccesoQr.CANJEADO)
			return ResultadoAccesoQr.YA_UTILIZADO;
		if (!enVentana(ahora, desde, hasta) || !enVentana(ahora, sufragioInicio, sufragioFin))
			return ResultadoAccesoQr.FUERA_DE_VIGENCIA;
		if (!documento)
			return ResultadoAccesoQr.DOCUMENTO_NO_VIGENTE;
		if (!proceso)
			return ResultadoAccesoQr.PROCESO_NO_VIGENTE;
		if (!mesa)
			return ResultadoAccesoQr.MESA_NO_VIGENTE;
		if (!presidente)
			return ResultadoAccesoQr.PRESIDENTE_NO_VIGENTE;
		if (!usuario)
			return ResultadoAccesoQr.USUARIO_NO_AUTORIZADO;
		if (!junta)
			return ResultadoAccesoQr.JRV_INCOMPLETA;
		return ResultadoAccesoQr.VALIDO;
	}

	public static boolean enVentana(Instant ahora, Instant desde, Instant hasta) {
		return ahora != null && desde != null && hasta != null && desde.isBefore(hasta) && !ahora.isBefore(desde)
				&& ahora.isBefore(hasta);
	}
}
