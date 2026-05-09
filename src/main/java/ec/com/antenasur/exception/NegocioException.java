package ec.com.antenasur.exception;

import jakarta.ejb.ApplicationException;

/**
 * ExcepciÃ³n para violaciones de reglas de negocio (documento duplicado,
 * conflicto de ediciÃ³n concurrente, etc.).
 *
 * {@code @ApplicationException(rollback=false)} garantiza que EJB no envuelve
 * la excepciÃ³n en EJBException y no hace rollback de la transacciÃ³n: el mensaje
 * llega al controlador tal cual para mostrarlo al usuario.
 */
@ApplicationException(rollback = false)
public class NegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NegocioException(String message) {
        super(message);
    }
}
