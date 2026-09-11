package ec.com.antenasur.util;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

/** Descarta la vista anterior sin reenviar su POST a FacesServlet. */
public final class RedireccionSesion {
    private RedireccionSesion() { }

    public static void login(HttpServletRequest request, HttpServletResponse response) throws IOException {
        redirigir(request, response, "/login.jsf");
    }

    public static void permisos(HttpServletRequest request, HttpServletResponse response) throws IOException {
        redirigir(request, response, "/errors/permisos.jsf");
    }

    private static void redirigir(HttpServletRequest request, HttpServletResponse response, String destino)
            throws IOException {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
        String url = request.getContextPath() + destino;
        if ("partial/ajax".equals(request.getHeader("Faces-Request"))) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/xml;charset=UTF-8");
            try {
                var xml = XMLOutputFactory.newFactory().createXMLStreamWriter(response.getWriter());
                xml.writeStartDocument("UTF-8", "1.0");
                xml.writeStartElement("partial-response");
                xml.writeEmptyElement("redirect");
                xml.writeAttribute("url", url);
                xml.writeEndElement();
                xml.writeEndDocument();
                xml.flush();
            } catch (XMLStreamException e) {
                throw new IOException("No se pudo emitir la redireccion de sesion", e);
            }
        } else {
            response.setStatus(HttpServletResponse.SC_SEE_OTHER);
            response.setHeader("Location", url);
        }
    }
}
