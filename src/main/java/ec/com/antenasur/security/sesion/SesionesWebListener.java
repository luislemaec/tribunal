package ec.com.antenasur.security.sesion;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

/** Da de baja la sesión del registro de usuarios en línea al cerrarse o expirar. */
@WebListener
public class SesionesWebListener implements HttpSessionListener {

    @Inject
    private SesionesWebEnLinea sesionesWebEnLinea;

    @Override
    public void sessionDestroyed(HttpSessionEvent evento) {
        sesionesWebEnLinea.quitar(evento.getSession());
    }
}
