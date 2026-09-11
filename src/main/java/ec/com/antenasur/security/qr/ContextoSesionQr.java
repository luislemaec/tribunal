package ec.com.antenasur.security.qr;

import java.io.Serializable;

/**
 * Solo HttpSession del servidor; no se expone como JSON ni en campos de
 * formulario.
 */
public record ContextoSesionQr(String prueba, Integer usuarioId, String username, Integer procesoId, Integer recintoId,
		Integer mesaId) implements Serializable {

	private static final long serialVersionUID = 1L;
	@Override
	public String toString() {
		return "ContextoSesionQr[protegido]";
	}
}
