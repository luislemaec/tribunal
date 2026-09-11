package ec.com.antenasur.security.qr;

import java.net.URI;

public final class ConfiguracionQr {
	public static final String ROL = "SITEC-Presidente-mesa";
	public static final String MARCADOR = "TEC-QR";
	public static final String PREFIJO = "TECQR:";
	public static final String SESION = "tec.contextoQr";

	private ConfiguracionQr() {
	}

	public static boolean habilitado() {
		return Boolean.getBoolean("tec.qr.enabled");
	}

	public static URI origen() {
		URI origen = URI.create(System.getProperty("tec.qr.public.base-url", ""));
		TokenActaQr.url(origen, "A".repeat(43));
		return origen;
	}
}
