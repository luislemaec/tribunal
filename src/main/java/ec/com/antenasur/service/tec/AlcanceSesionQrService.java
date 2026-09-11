package ec.com.antenasur.service.tec;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import ec.com.antenasur.security.qr.*;

/**
 * La concesion server-side complementa (nunca sustituye) la identidad de
 * Elytron.
 */
@Stateless
@DeclareRoles({ ConfiguracionQr.MARCADOR, ConfiguracionQr.ROL })
public class AlcanceSesionQrService {
	
	@Resource
	private SessionContext ejb;
	
	@Inject
	private Instance<HttpServletRequest> solicitudes;
	
	@Inject
	private CanjeAccesoQrService canje;

	/** Consulta transversal: null no concede permisos al servicio que la invoca. */
	@PermitAll
	public ContextoSesionQr contexto() {
		ContextoSesionQr contexto = null;
		var solicitud = solicitudActual();
		if (solicitud != null) {
			contexto = (ContextoSesionQr) solicitud.sesion().getAttribute(ConfiguracionQr.SESION);
			if (contexto != null && !solicitud.segura())
				throw new AccesoQrException();
		}
		boolean identidadQr = ejb.isCallerInRole(ConfiguracionQr.MARCADOR)
				|| (ejb.getCallerPrincipal() != null
						&& ejb.getCallerPrincipal().getName().startsWith(ConfiguracionQr.PREFIJO));
		if (identidadQr && contexto == null)
			throw new AccesoQrException();
		if (contexto != null && (!ConfiguracionQr.habilitado() || !ejb.isCallerInRole(ConfiguracionQr.ROL)
				|| !ejb.isCallerInRole(ConfiguracionQr.MARCADOR)
				|| !(ConfiguracionQr.PREFIJO + contexto.username()).equals(ejb.getCallerPrincipal().getName())))
			throw new AccesoQrException();
		return contexto;
	}

	private SolicitudQr solicitudActual() {
		try {
			var request = solicitudes.get();
			if (request == null) return null;
			// Resolver tambien el proxy CDI aqui; Instance.get() puede devolverlo sin HTTP activo.
			var sesion = request.getSession(false);
			return sesion == null ? null : new SolicitudQr(sesion, request.isSecure());
		} catch (ContextNotActiveException | IllegalStateException e) {
			// Un timer/EJB sin HTTP puede tener contexto CDI pero no ServletRequest.
			// La identidad QR sin solicitud se rechaza en contexto(), nunca se autoriza.
			return null;
		}
	}

	private record SolicitudQr(HttpSession sesion, boolean segura) { }

	@RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal", ConfiguracionQr.ROL })
	public void validar(Integer mesa, Integer proceso) {
		var contexto = contexto();
		if (contexto == null)
			return;
		if (!contexto.mesaId().equals(mesa) || !contexto.procesoId().equals(proceso) || canje
				.validarSesion(contexto.prueba(), proceso, mesa, contexto.username()) != ResultadoAccesoQr.VALIDO)
			throw new AccesoQrException();
	}

	@RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal", ConfiguracionQr.ROL })
	public void impedirOperacionAdministrativa() {
		if (contexto() != null)
			throw new AccesoQrException();
	}
}
