package ec.com.antenasur.security.qr;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import ec.com.antenasur.service.tec.CanjeAccesoQrService;

@WebListener
public class CierreSesionQrListener implements HttpSessionListener {
	@Inject
	private CanjeAccesoQrService canje;

	@Override
	public void sessionDestroyed(HttpSessionEvent evento) {
		var contexto = (ContextoSesionQr) evento.getSession().getAttribute(ConfiguracionQr.SESION);
		if (contexto == null)
			return;
		java.util.logging.Logger.getLogger(CierreSesionQrListener.class.getName()).info(
				"QR causa=SESION_HTTP_DESTRUIDA; proceso=" + contexto.procesoId() + "; mesa=" + contexto.mesaId());
		try {
			canje.cerrar(contexto.prueba(), "", "", "SESION_CERRADA");
		} catch (Exception e) {
			evento.getSession().getServletContext().log("QR: no se pudo registrar cierre de concesion");
		}
	}
}
