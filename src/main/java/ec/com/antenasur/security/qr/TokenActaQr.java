package ec.com.antenasur.security.qr;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Credencial opaca; nunca registrar el token, la URL completa o el fragmento.
 */
public final class TokenActaQr {
	private static final SecureRandom RANDOM = new SecureRandom();

	private TokenActaQr() {
	}

	public static String generar() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	public static boolean formatoValido(String token) {
		if (token == null || !token.matches("[A-Za-z0-9_-]{43}"))
			return false;
		byte[] bytes = Base64.getUrlDecoder().decode(token);
		return bytes.length == 32 && Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(token);
	}

	public static String hash(String token) {
		if (!formatoValido(token))
			throw new IllegalArgumentException("qr.token.formato");
		try {
			return HexFormat.of()
					.formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 no disponible", e);
		}
	}

	public static URI url(URI origen, String token) {
		if (!formatoValido(token) || origen == null || !"https".equals(origen.getScheme()) || origen.getHost() == null
				|| origen.getRawUserInfo() != null || origen.getRawQuery() != null || origen.getRawFragment() != null
				|| (!origen.getPath().isEmpty() && !"/".equals(origen.getPath()))) {
			throw new IllegalArgumentException("qr.configuracion.origen");
		}
		return origen.resolve("/acceso-acta?token=" + token);
	}

	/** Solo un parametro canonico, sin duplicados, escapes ni datos adicionales. */
	public static String tokenConsulta(String consulta) {
		if (consulta == null || !consulta.startsWith("token=")) return null;
		String token = consulta.substring(6);
		return formatoValido(token) ? token : null;
	}

	public static boolean urlAccesoValida(String valor) {
		if (valor == null) return false;
		try {
			URI uri = URI.create(valor);
			return "https".equals(uri.getScheme()) && uri.getHost() != null && uri.getRawUserInfo() == null
					&& "/acceso-acta".equals(uri.getRawPath()) && uri.getRawFragment() == null
					&& tokenConsulta(uri.getRawQuery()) != null;
		} catch (IllegalArgumentException e) { return false; }
	}
}
