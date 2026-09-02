package ec.com.antenasur.exception;

import jakarta.ejb.ApplicationException;

/** Excepcion localizada para reglas de pertenencia Persona-Iglesia. */
@ApplicationException(rollback = true)
public class IglesiaPersonaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String messageKey;
    private final Object[] arguments;

    public IglesiaPersonaException(String messageKey, Object... arguments) {
        super(messageKey);
        this.messageKey = messageKey;
        this.arguments = arguments != null ? arguments.clone() : new Object[0];
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArguments() {
        return arguments.clone();
    }
}
