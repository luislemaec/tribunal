package ec.com.antenasur.security.qr;

import jakarta.ejb.ApplicationException;
import ec.com.antenasur.util.Constantes;

@ApplicationException(rollback = true)
public class AccesoQrException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public AccesoQrException() {
		super(Constantes.getMensaje("actaQr.error.no.disponible"));
	}
}
