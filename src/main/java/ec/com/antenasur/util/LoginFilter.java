package ec.com.antenasur.util;

import java.io.IOException;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import ec.com.antenasur.bean.LoginBean;
import lombok.Getter;
import lombok.Setter;

@WebFilter(filterName = "LoginFilter", urlPatterns = {"/*"}, dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.FORWARD})
public class LoginFilter implements Filter {

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

        if (LoginFilterExcluder.getInstance(req.getContextPath()).isExcludeUrl(req.getRequestURI()) || req.getRequestURI().isEmpty()) {
            next.doFilter(request, response);
            return;
        }

        LoginBean loginBean = (LoginBean) req.getSession().getAttribute("loginBean");
        List<String> listaPermisos = (List<String>) req.getSession().getAttribute("listaPermisos");
        if (loginBean == null) {
            req.getRequestDispatcher("/login.jsf").forward(request, response);
        } else {
            if (loginBean.getUsuario().getId() != null) {
                String pagina = devolverPagina(req.getRequestURL().toString());
                if (validarPagina(pagina, listaPermisos)) {
                    next.doFilter(request, response);
                } else {
                    req.getRequestDispatcher("/errors/permisos.jsf").forward(request, response);
                }
            } else {
                req.getRequestDispatcher("/errors/permisos.jsf").forward(request, response);
            }
        }

    }

    private boolean validarPagina(final String pagina, final List<String> listaPermisos) {
        return listaPermisos.contains(pagina);
    }

    private String devolverPagina(final String url) {
        StringTokenizer str = new StringTokenizer(url == null ? "" : url, "/");
        String retorno = null;
        while (str.hasMoreTokens()) {
            retorno = str.nextToken();
        }
        return retorno;
    }

    @Override
    public void destroy() {
    }

}
