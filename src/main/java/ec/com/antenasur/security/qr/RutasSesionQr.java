package ec.com.antenasur.security.qr;

/**
 * Lista cerrada; no admitir alias, path parameters ni rutas normalizadas por el
 * cliente.
 */
public final class RutasSesionQr {
	private RutasSesionQr() {
	}

	public static boolean publica(String ruta) {
		return "/acceso-acta".equals(ruta) || "/acceso-acta/canjear".equals(ruta)
				|| "/resources/qr/acceso.js".equals(ruta) || "/resources/qr/acceso.css".equals(ruta);
	}

	public static boolean permitida(String ruta, String metodo) {
		if (ruta == null || ruta.contains("..") || ruta.contains("%") || ruta.contains(";") || ruta.contains("\\"))
			return false;
		if ("/actaE.jsf".equals(ruta))
			return "GET".equals(metodo) || "POST".equals(metodo);
		if (!"GET".equals(metodo))
			return false;
		if (ruta.startsWith("/jakarta.faces.resource/") || ruta.startsWith("/javax.faces.resource/"))
			return true;
		return ruta.startsWith("/resources/") && ruta.matches(".*\\.(css|js|png|jpg|jpeg|svg|woff2?|ttf|gif|ico)$");
	}
}
