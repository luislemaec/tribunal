package ec.com.antenasur.util;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;

import ec.com.antenasur.bean.LoginBean;
import lombok.Getter;
import lombok.Setter;

@WebFilter(filterName = "LoginFilter", urlPatterns = { "/*" }, dispatcherTypes = { DispatcherType.REQUEST,
		DispatcherType.FORWARD })
public class LoginFilter implements Filter {
	private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(LoginFilter.class.getName());

	@jakarta.inject.Inject
	private ec.com.antenasur.security.qr.ControlHttpSesionQr controlQr;

	@jakarta.inject.Inject
	private ec.com.antenasur.security.menu.AutorizacionMenuService autorizacionMenu;

	private String encoding;

	@Getter
	@Setter
	private FilterConfig config;

	@Override
	public void init(FilterConfig config) throws ServletException {
		setConfig(config);
		encoding = config.getInitParameter("requestEncoding");

		if (encoding == null) {
			encoding = "UTF-8";
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain next)
			throws IOException, ServletException {
		if (null == request.getCharacterEncoding()) {
			request.setCharacterEncoding(encoding);
		}

		HttpServletRequest req = (HttpServletRequest) request;
		if (esSolicitudActa(req)) {
			LOG.info("QR_TEMP_ACTA; metodo=" + req.getMethod() + "; uri=" + req.getRequestURI() + "; servletPath="
					+ req.getServletPath() + "; tipo=" + tipoPeticion(req) + "; sesion="
					+ (req.getSession(false) != null) + "; principalPresente=" + (req.getUserPrincipal() != null)
					+ "; rolPresidente=" + req.isUserInRole("SITEC-Presidente-mesa") + "; rolQr="
					+ req.isUserInRole("TEC-QR") + "; secure=" + req.isSecure());
		}
		if (controlQr.procesar(req, (jakarta.servlet.http.HttpServletResponse) response, next))
			return;

		var sesion = req.getSession(false);
		// Un POST de login de una sesion destruida tampoco debe restaurar su ViewState.
		if (sesion == null && "POST".equals(req.getMethod())
				&& LoginFilterExcluder.getInstance(req.getContextPath()).esLogin(req.getRequestURI())) {
			RedireccionSesion.login(req, (jakarta.servlet.http.HttpServletResponse) response);
			return;
		}

		if (LoginFilterExcluder.getInstance(req.getContextPath()).isExcludeUrl(req.getRequestURI())) {
			next.doFilter(request, response);
			return;
		}

		LoginBean loginBean = sesion == null ? null : (LoginBean) sesion.getAttribute("loginBean");
		List<String> listaPermisos = sesion == null ? null : (List<String>) sesion.getAttribute("listaPermisos");
		if (req.getUserPrincipal() == null) {
			if (sesion != null) {
				try {
					sesion.invalidate();
				} catch (IllegalStateException ignorada) {
					/* Peticion concurrente. */ }
			}
			RedireccionSesion.login(req, (jakarta.servlet.http.HttpServletResponse) response);
			return;
		}
		if (loginBean == null || loginBean.getUsuario() == null) {
			RedireccionSesion.login(req, (jakarta.servlet.http.HttpServletResponse) response);
			return;
		} else {
			if (loginBean.getUsuario().getId() != null) {
				String pagina = req.getRequestURI().substring(req.getContextPath().length());
				// La sesion de cambio obligatorio de clave no recibe permisos completos.
				if (Boolean.TRUE.equals(loginBean.getUsuario().getPermanente())) {
					try {
						listaPermisos = autorizacionMenu.paginasActuales();
						sesion.setAttribute("listaPermisos", listaPermisos);
					} catch (jakarta.ejb.EJBException e) {
						RedireccionSesion.permisos(req, (jakarta.servlet.http.HttpServletResponse) response);
						return;
					}
				}
				if (validarPagina(pagina, listaPermisos)) {
					next.doFilter(request, response);
				} else {
					RedireccionSesion.permisos(req, (jakarta.servlet.http.HttpServletResponse) response);
					return;
				}
			} else {
				RedireccionSesion.permisos(req, (jakarta.servlet.http.HttpServletResponse) response);
				return;
			}
		}

	}

	private boolean validarPagina(final String pagina, final List<String> listaPermisos) {
		return ec.com.antenasur.security.menu.PaginasMenu.permite(listaPermisos, pagina);
	}

	private boolean esSolicitudActa(HttpServletRequest req) {
		String ruta = req.getRequestURI().substring(req.getContextPath().length());
		return "/actaE.jsf".equals(ruta) || "/actaE.faces".equals(ruta) || "/actaE.xhtml".equals(ruta)
				|| "/faces/actaE.xhtml".equals(ruta);
	}

	private String tipoPeticion(HttpServletRequest req) {
		String contentType = req.getContentType();
		if (contentType != null && contentType.toLowerCase(java.util.Locale.ROOT).startsWith("multipart/form-data")) {
			return "MULTIPART";
		}
		return "partial/ajax".equals(req.getHeader("Faces-Request")) ? "JSF_AJAX" : "HTTP";
	}

	@Override
	public void destroy() {
	}

}
