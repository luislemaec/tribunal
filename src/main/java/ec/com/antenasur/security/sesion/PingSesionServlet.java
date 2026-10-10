package ec.com.antenasur.security.sesion;

import java.io.IOException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import ec.com.antenasur.bean.LoginBean;

/**
 * Mantiene viva la sesión web mientras el usuario trabaja (tec-sesion.js). Obtener la
 * sesión existente renueva su último acceso, así el timeout del servidor (web.xml) cuenta
 * desde la última actividad real y no solo desde la última petición JSF.
 *
 * <p>No crea sesiones ni restaura vistas: responde 204 si hay una sesión iniciada y 401 si
 * no. LoginFilter excluye esta ruta exacta porque no es una página del menú.</p>
 */
@WebServlet("/sesion/ping")
public class PingSesionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Cache-Control", "no-store");
        var sesion = request.getSession(false);
        Object login = sesion == null ? null : sesion.getAttribute("loginBean");
        boolean iniciada = login instanceof LoginBean bean && bean.isLoggedIn() && request.getUserPrincipal() != null;
        response.setStatus(iniciada ? HttpServletResponse.SC_NO_CONTENT : HttpServletResponse.SC_UNAUTHORIZED);
    }
}
