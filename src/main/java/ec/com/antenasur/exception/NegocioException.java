package ec.com.antenasur.exception;

import javax.ejb.ApplicationException;

/**
 * Excepción para violaciones de reglas de negocio (documento duplicado,
 * conflicto de edición concurrente, etc.).
 *
 * {@code @ApplicationException(rollback=false)} garantiza que EJB no envuelve
 * la excepción en EJBException y no hace rollback de la transacción: el mensaje
 * llega al controlador tal cual para mostrarlo al usuario.
 */
@ApplicationException(rollback = false)
public class NegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NegocioException(String message) {
        super(message);
    }
}
