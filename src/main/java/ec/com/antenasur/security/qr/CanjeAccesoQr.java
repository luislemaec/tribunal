package ec.com.antenasur.security.qr;

/** Resultado interno del canje. No serializar como respuesta HTTP. */
public final class CanjeAccesoQr {
	private final ResultadoAccesoQr resultado;
	private final String pruebaSesion;
	private final String claveInterna;
	private final ContextoSesionQr contexto;

	public CanjeAccesoQr(ResultadoAccesoQr resultado, String pruebaSesion) {
		this(resultado, pruebaSesion, null, null);
	}

	public CanjeAccesoQr(ResultadoAccesoQr resultado, String pruebaSesion, String claveInterna,
			ContextoSesionQr contexto) {
		this.resultado = resultado;
		this.pruebaSesion = pruebaSesion;
		this.claveInterna = claveInterna;
		this.contexto = contexto;
	}

	public ResultadoAccesoQr getResultado() {
		return resultado;
	}

	public String getPruebaSesion() {
		return pruebaSesion;
	}

	public String getClaveInterna() {
		return claveInterna;
	}

	public ContextoSesionQr getContexto() {
		return contexto;
	}

	@Override
	public String toString() {
		return "CanjeAccesoQr[" + resultado + "]";
	}
}
