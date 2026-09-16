package ec.com.antenasur.security.qr;

/**
 * Causa técnica concreta del rechazo, exclusivamente para bitácora. Varias
 * causas comparten el mismo {@link ResultadoAccesoQr} porque la respuesta HTTP
 * y la auditoría deben seguir siendo genéricas: el detalle queda en el log del
 * servidor y no se entrega a quien escanea.
 */
public enum CausaRechazoQr {

	VALIDO(ResultadoAccesoQr.VALIDO),

	TOKEN_FORMATO_INVALIDO(ResultadoAccesoQr.TOKEN_INVALIDO),
	TOKEN_NO_ENCONTRADO(ResultadoAccesoQr.TOKEN_INVALIDO),
	ESTADO_DESCONOCIDO(ResultadoAccesoQr.TOKEN_INVALIDO),
	SESION_NO_CANJEADA(ResultadoAccesoQr.TOKEN_INVALIDO),

	REVOCADO(ResultadoAccesoQr.REVOCADO),
	YA_CANJEADO(ResultadoAccesoQr.YA_UTILIZADO),

	SUFRAGIO_NO_CONFIGURADO(ResultadoAccesoQr.FUERA_DE_VIGENCIA),
	VENTANA_NO_CORRESPONDE_A_CIERRE_SUFRAGIO(ResultadoAccesoQr.FUERA_DE_VIGENCIA),
	VENTANA_INVALIDA(ResultadoAccesoQr.FUERA_DE_VIGENCIA),
	ANTES_DE_VIGENCIA(ResultadoAccesoQr.FUERA_DE_VIGENCIA),
	FUERA_DE_VIGENCIA(ResultadoAccesoQr.FUERA_DE_VIGENCIA),

	DOCUMENTO_VERSION_INVALIDA(ResultadoAccesoQr.DOCUMENTO_NO_VIGENTE),
	DOCUMENTO_NO_VIGENTE(ResultadoAccesoQr.DOCUMENTO_NO_VIGENTE),
	PROCESO_NO_VIGENTE(ResultadoAccesoQr.PROCESO_NO_VIGENTE),
	MESA_NO_VIGENTE(ResultadoAccesoQr.MESA_NO_VIGENTE),
	PRESIDENTE_NO_VIGENTE(ResultadoAccesoQr.PRESIDENTE_NO_VIGENTE),
	USUARIO_NO_AUTORIZADO(ResultadoAccesoQr.USUARIO_NO_AUTORIZADO),
	JRV_INCOMPLETA(ResultadoAccesoQr.JRV_INCOMPLETA);

	private final ResultadoAccesoQr resultado;

	CausaRechazoQr(ResultadoAccesoQr resultado) {
		this.resultado = resultado;
	}

	public ResultadoAccesoQr resultado() {
		return resultado;
	}
}
