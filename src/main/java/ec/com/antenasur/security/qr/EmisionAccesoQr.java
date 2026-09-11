package ec.com.antenasur.security.qr;

import java.net.URI;

/**
 * Solo vive durante la generacion del PDF. No usar en sesion, logs o auditoria.
 */
public final class EmisionAccesoQr {
	private final Long id;
	private final URI url;

	public EmisionAccesoQr(Long id, URI url) {
		this.id = id;
		this.url = url;
	}

	public Long getId() {
		return id;
	}

	public URI getUrl() {
		return url;
	}

	@Override
	public String toString() {
		return "EmisionAccesoQr[credencial protegida]";
	}
}
