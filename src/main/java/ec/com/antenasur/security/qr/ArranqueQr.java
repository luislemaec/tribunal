package ec.com.antenasur.security.qr;

import java.util.Set;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class ArranqueQr implements ServletContextListener {
	@Override
	public void contextInitialized(ServletContextEvent evento) {
		if (!ConfiguracionQr.habilitado())
			return;
		ConfiguracionQr.origen(); // Fallar cerrado ante un origen publico invalido.
		var context = evento.getServletContext();
		context.setSessionTrackingModes(Set.of(SessionTrackingMode.COOKIE));
		var cookie = context.getSessionCookieConfig();
		cookie.setSecure(true);
		cookie.setHttpOnly(true);
		cookie.setAttribute("SameSite", "Lax");
	}
}
