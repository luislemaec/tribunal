package ec.com.antenasur.security.qr;

import java.io.IOException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ec.com.antenasur.service.tec.AlcanceSesionQrService;
import ec.com.antenasur.util.RedireccionSesion;

@ApplicationScoped
public class ControlHttpSesionQr {
	@Inject
	private AlcanceSesionQrService alcance;

	public boolean procesar(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
			throws IOException, ServletException {
		var sesion = request.getSession(false);
		boolean restringida = (sesion != null && sesion.getAttribute(ConfiguracionQr.SESION) != null)
				|| request.isUserInRole(ConfiguracionQr.MARCADOR)
				|| (request.getUserPrincipal() != null
						&& request.getUserPrincipal().getName().startsWith(ConfiguracionQr.PREFIJO));
		String ruta = request.getRequestURI().substring(request.getContextPath().length());
		if (!restringida) {
			if (!RutasSesionQr.publica(ruta))
				return false;
			cadena.doFilter(request, response);
			return true;
		}
		response.setHeader("Cache-Control", "no-store");
		response.setHeader("Referrer-Policy", "no-referrer");
		try {
			if (!request.isSecure() || !RutasSesionQr.permitida(ruta, request.getMethod()))
				throw new AccesoQrException();
			var contexto = alcance.contexto();
			if (contexto == null)
				throw new AccesoQrException();
			alcance.validar(contexto.mesaId(), contexto.procesoId());
		} catch (Exception e) {
			java.util.logging.Logger.getLogger(ControlHttpSesionQr.class.getName()).warning(
					"QR causa=FILTRO_SESION_PREVIA; secure=" + request.isSecure()
					+ "; rutaPermitida=" + RutasSesionQr.permitida(ruta, request.getMethod())
					+ "; excepcion=" + DiagnosticoQr.tipoExcepcion(e));
			boolean logoutCorrecto = true;
			try { request.logout(); }
			catch (ServletException ignorada) { logoutCorrecto = false; }
			finally {
				if (sesion != null) {
					try { sesion.invalidate(); }
					catch (IllegalStateException ignorada) { /* Ya destruida por otra peticion. */ }
				}
			}
			if (!logoutCorrecto) {
				// No redirigir una identidad que el contenedor no pudo retirar: evitar bucle.
				response.setStatus(403);
				return true;
			}
			RedireccionSesion.login(request, response);
			return true;
		}
		cadena.doFilter(request, response);
		return true;
	}
}
