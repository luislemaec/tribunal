package ec.com.antenasur.security.qr;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.model.AccessAuditory;
import ec.com.antenasur.service.AccessService;
import ec.com.antenasur.service.UsuarioService;
import ec.com.antenasur.service.tec.CanjeAccesoQrService;
import ec.com.antenasur.util.Constantes;
import org.primefaces.model.menu.DefaultMenuModel;

@WebServlet(urlPatterns = { "/acceso-acta", "/acceso-acta/canjear" })
public class AccesoActaServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String CSRF = "tec.qr.csrf";
	private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(AccesoActaServlet.class.getName());
	@Inject
	private CanjeAccesoQrService canje;
	@Inject
	private LimiteIntentosQr limite;
	@Inject
	private UsuarioService usuarios;
	@Inject
	private AccessService accesos;
	@Inject
	private LoginBean login;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		cabeceras(response);
		String causa = causaTransporte(request);
		if (causa != null || !"/acceso-acta".equals(request.getServletPath())) {
			registrarRechazo(causa != null ? causa : "GET_RUTA", request);
			pagina(response, request.getContextPath(), null, "actaQr.error.acceso", 403);
			return;
		}
		if (!limite.permitir(request.getRemoteAddr(), false)) {
			registrarRechazo("GET_LIMITE_INTENTOS", request);
			pagina(response, request.getContextPath(), null, "actaQr.error.limite", 429);
			return;
		}
		if (request.getQueryString() != null) {
			String token = TokenActaQr.tokenConsulta(request.getQueryString());
			if (token == null) {
				registrarRechazo("GET_FORMATO_TOKEN", request);
				pagina(response, request.getContextPath(), null, "actaQr.error.token", 400);
				return;
			}
			// No consumir al escanear: pasar al formulario limpio y a su POST con CSRF.
			response.setStatus(HttpServletResponse.SC_SEE_OTHER);
			response.setHeader("Location", request.getContextPath() + "/acceso-acta#t=" + token);
			return;
		}
		var sesion = request.getSession(true);
		String csrf = TokenActaQr.generar();
		sesion.setAttribute(CSRF, csrf);
		LOG.fine("QR causa=GET_CONFIRMACION_PREPARADA; sin consumo de credencial");
		pagina(response, request.getContextPath(), csrf, "actaQr.procesando", 200);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		cabeceras(response);
		String causa = causaTransporte(request);
		if (causa != null || request.getQueryString() != null || !"/acceso-acta/canjear".equals(request.getServletPath())
				|| !origenPermitido(request.getHeader("Origin"))) {
			registrarRechazo(causa != null ? causa : request.getQueryString() != null ? "POST_QUERY"
					: !"/acceso-acta/canjear".equals(request.getServletPath()) ? "POST_RUTA" : "POST_ORIGIN", request);
			pagina(response, request.getContextPath(), null, "actaQr.error.acceso", 403);
			return;
		}
		if (!limite.permitir(request.getRemoteAddr(), true)) {
			registrarRechazo("POST_LIMITE_INTENTOS", request);
			response.setHeader("Retry-After", "60");
			pagina(response, request.getContextPath(), null, "actaQr.error.limite", 429);
			return;
		}
		String prueba = null;
		String etapa = "FORMULARIO";
		try {
			Map<String, String> campos = leerFormulario(request);
			var previa = request.getSession(false);
			String esperado = previa == null ? null : (String) previa.getAttribute(CSRF);
			if (!iguales(esperado, campos.get("csrf")) || !"si".equals(campos.get("confirmar"))) {
				registrarRechazo(previa == null ? "POST_SIN_SESION" : esperado == null ? "POST_SIN_CSRF"
						: !iguales(esperado, campos.get("csrf")) ? "POST_CSRF" : "POST_SIN_CONFIRMACION", request);
				pagina(response, request.getContextPath(), null, "actaQr.error.acceso", 403);
				return;
			}
			previa.removeAttribute(CSRF);
			etapa = "CANJE_EJB";
			CanjeAccesoQr resultado = canje.canjear(campos.get("token"), request.getRemoteAddr(),
					request.getHeader("User-Agent"));
			if (resultado.getResultado() != ResultadoAccesoQr.VALIDO) {
				registrarRechazo("CANJE_" + resultado.getResultado().name(), request);
				pagina(response, request.getContextPath(), null, "actaQr.error.acceso", 403);
				return;
			}
			prueba = resultado.getPruebaSesion();
			etapa = "RENOVAR_SESION";
			request.logout();
			previa.invalidate();
			var nueva = request.getSession(true);
			request.changeSessionId();
			ContextoSesionQr contexto = resultado.getContexto();
			etapa = "LOGIN_ELYTRON";
			request.login(ConfiguracionQr.PREFIJO + contexto.username(), resultado.getClaveInterna());
			etapa = "IDENTIDAD_ELYTRON";
			if (request.getUserPrincipal() == null || !(ConfiguracionQr.PREFIJO + contexto.username()).equals(request.getUserPrincipal().getName())
					|| !request.isUserInRole(ConfiguracionQr.MARCADOR) || !request.isUserInRole(ConfiguracionQr.ROL))
				throw new ServletException("qr.identidad.no.confirmada");
			etapa = "CONFIRMAR_PUENTE";
			canje.confirmar(prueba, contexto.username(), request.getRemoteAddr(), request.getHeader("User-Agent"));
			request.changeSessionId();
			etapa = "CONTEXTO_USUARIO";
			var datos = usuarios.cargarContextoUsuarioAutenticado(contexto.username(), "SITEC-");
			if (!datos.isResolved() || !contexto.usuarioId().equals(datos.getUsuario().getId()))
				throw new ServletException("qr.contexto.no.confirmado");
			etapa = "SESION_Y_AUDITORIA";
			nueva.setAttribute(ConfiguracionQr.SESION, contexto);
			nueva.setMaxInactiveInterval(15 * 60);
			login.setUserName(contexto.username());
			login.setPassword(null);
			login.setUsuario(datos.getUsuario());
			login.setRoles(List.of(ConfiguracionQr.ROL));
			login.setMenuModel(new DefaultMenuModel());
			login.setLoggedIn(true);
			login.setTiempoSession(nueva.getMaxInactiveInterval());
			nueva.setAttribute("loginBean", login);
			nueva.setAttribute("listaPermisos", List.of("actaE.jsf"));
			var auditoria = new AccessAuditory(contexto.username(), Timestamp.from(Instant.now()),
					request.getRemoteAddr());
			auditoria.setBrowser("QR");
			auditoria.setStatus(true);
			auditoria.setActive(true);
			auditoria.setSession(nueva.getId());
			accesos.create(auditoria);
			response.setStatus(HttpServletResponse.SC_SEE_OTHER);
			response.setHeader("Location", request.getContextPath() + "/actaE.jsf");
		} catch (Exception e) {
			LOG.warning("QR causa=" + etapa + "; excepcion=" + DiagnosticoQr.tipoExcepcion(e));
			if (prueba != null) {
				try {
					canje.cerrar(prueba, request.getRemoteAddr(), request.getHeader("User-Agent"),
							"ERROR_AUTENTICACION");
				} catch (Exception ignorada) {
					getServletContext().log("QR: no se pudo revocar una concesion fallida");
				}
				try {
					request.logout();
				} catch (ServletException ignorada) {
				}
				var sesion = request.getSession(false);
				if (sesion != null)
					sesion.invalidate();
			}
			// No adjuntar la excepcion: proveedores/servidores pueden incluir credenciales
			// en sus mensajes.
			pagina(response, request.getContextPath(), null, "actaQr.error.acceso", 403);
		}
	}

	static boolean iguales(String esperado, String recibido) {
		return TokenActaQr.formatoValido(esperado) && TokenActaQr.formatoValido(recibido) && MessageDigest
				.isEqual(esperado.getBytes(StandardCharsets.US_ASCII), recibido.getBytes(StandardCharsets.US_ASCII));
	}

	private static Map<String, String> leerFormulario(HttpServletRequest request) throws IOException {
		if (request.getContentType() == null
				|| !request.getContentType().toLowerCase(java.util.Locale.ROOT).split(";", 2)[0].trim()
						.equals("application/x-www-form-urlencoded")
				|| request.getContentLengthLong() > 2048)
			throw new IOException("qr.formulario");
		byte[] bytes = request.getInputStream().readNBytes(2049);
		if (bytes.length > 2048)
			throw new IOException("qr.formulario");
		Map<String, String> campos = new HashMap<>();
		for (String par : new String(bytes, StandardCharsets.UTF_8).split("&")) {
			String[] partes = par.split("=", 2);
			if (partes.length != 2)
				throw new IOException("qr.formulario");
			String nombre = URLDecoder.decode(partes[0], StandardCharsets.UTF_8);
			if (!List.of("csrf", "token", "confirmar").contains(nombre)
					|| campos.putIfAbsent(nombre, URLDecoder.decode(partes[1], StandardCharsets.UTF_8)) != null)
				throw new IOException("qr.formulario");
		}
		return campos;
	}

	static boolean origenPermitido(String origen) {
		if (origen == null)
			return false;
		try {
			var recibido = java.net.URI.create(origen);
			var esperado = ConfiguracionQr.origen();
			return "https".equals(recibido.getScheme()) && esperado.getHost().equalsIgnoreCase(recibido.getHost())
					&& (recibido.getPort() == -1 ? 443 : recibido.getPort()) == (esperado.getPort() == -1 ? 443
							: esperado.getPort())
					&& recibido.getRawUserInfo() == null && recibido.getRawQuery() == null
					&& recibido.getRawFragment() == null && "".equals(recibido.getPath());
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	static String causaTransporte(HttpServletRequest request) {
		if (!ConfiguracionQr.habilitado()) return "QR_DESHABILITADO";
		if (!request.isSecure()) return "HTTPS_NO_RECONOCIDO";
		try { ConfiguracionQr.origen(); }
		catch (IllegalArgumentException e) { return "ORIGEN_CONFIGURADO_INVALIDO"; }
		return origenPermitido("https://" + request.getServerName()
				+ (request.getServerPort() == 443 ? "" : ":" + request.getServerPort()))
				? null : "HOST_PUERTO_NO_AUTORIZADO";
	}

	private static void registrarRechazo(String causa, HttpServletRequest request) {
		LOG.warning("QR causa=" + causa + "; secure=" + request.isSecure()
				+ "; puerto=" + request.getServerPort() + "; sesionPrevia=" + (request.getSession(false) != null));
	}

	private static void cabeceras(HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-store, max-age=0");
		response.setHeader("Referrer-Policy", "no-referrer");
		response.setHeader("X-Content-Type-Options", "nosniff");
		response.setHeader("Content-Security-Policy",
				"default-src 'none'; script-src 'self'; style-src 'self'; img-src 'self'; form-action 'self'; base-uri 'none'; frame-ancestors 'none'");
	}

	private static void pagina(HttpServletResponse response, String contexto, String csrf, String clave, int estado)
			throws IOException {
		// Un POST de formulario con no-referrer puede enviar Origin: null.
		// Esta pagina ya tiene URL limpia; solo comparte referencia con el mismo origen.
		if (csrf != null) response.setHeader("Referrer-Policy", "same-origin");
		response.setStatus(estado);
		response.setContentType("text/html;charset=UTF-8");
		String base = escapar(contexto);
		String titulo = escapar(Constantes.getMensaje(csrf == null ? "actaQr.titulo" : "actaQr.procesando"));
		var out = response.getWriter();
		out.print(
				"<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>"
						+ titulo + "</title><link rel=\"stylesheet\" href=\"" + base
						+ "/resources/qr/acceso.css\"><script defer src=\"" + base
						+ "/resources/qr/acceso.js?v=2\"></script></head><body><main>");
		if (csrf == null) out.print("<h1>" + titulo + "</h1><p>" + escapar(Constantes.getMensaje(clave)) + "</p>");
		else out.print("<p id=\"procesando\" role=\"status\">" + titulo + "</p>");
		if (csrf != null)
			out.print("<form method=\"post\" action=\"" + base
					+ "/acceso-acta/canjear\" autocomplete=\"off\" hidden><input type=\"hidden\" name=\"csrf\" value=\"" + csrf
					+ "\"><input type=\"hidden\" name=\"token\" id=\"token\">"
					+ "<input type=\"hidden\" name=\"confirmar\" value=\"si\"></form><p id=\"aviso\" role=\"alert\" hidden>"
					+ escapar(Constantes.getMensaje("actaQr.error.token")) + "</p><noscript><p>"
					+ escapar(Constantes.getMensaje("actaQr.javascript.requerido")) + "</p></noscript>");
		out.print("</main></body></html>");
	}

	private static String escapar(String texto) {
		return texto == null ? ""
				: texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
						.replace("'", "&#39;");
	}
}
