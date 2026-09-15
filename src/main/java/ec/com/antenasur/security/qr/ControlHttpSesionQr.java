package ec.com.antenasur.security.qr;

import java.io.IOException;
import java.util.Locale;
import java.util.logging.Logger;
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
	private static final Logger LOG = Logger.getLogger(ControlHttpSesionQr.class.getName());

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
		boolean rutaPermitida = RutasSesionQr.permitida(ruta, request.getMethod());
		try {
			if (!request.isSecure() || !rutaPermitida)
				throw new AccesoQrException();
			var contexto = alcance.contexto();
			if (contexto == null)
				throw new AccesoQrException();
			alcance.validar(contexto.mesaId(), contexto.procesoId());
		} catch (Exception e) {
			LOG.warning("QR_TEMP_RECHAZO; causa=FILTRO_SESION_PREVIA; metodo=" + request.getMethod()
					+ "; uri=" + request.getRequestURI() + "; servletPath=" + request.getServletPath()
					+ "; dispatcher=" + request.getDispatcherType() + "; tipo=" + tipoPeticion(request)
					+ "; secure=" + request.isSecure() + "; rutaPermitida=" + rutaPermitida
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

	private static String tipoPeticion(HttpServletRequest request) {
		String contentType = request.getContentType();
		if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("multipart/form-data"))
			return "MULTIPART";
		if ("partial/ajax".equals(request.getHeader("Faces-Request")))
			return "JSF_AJAX";
		return "HTTP";
	}
}
