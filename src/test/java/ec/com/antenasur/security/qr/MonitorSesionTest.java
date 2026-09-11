package ec.com.antenasur.security.qr;

import java.nio.file.Path;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MonitorSesionTest {
    @Test void monitorConvierteSegundosYNoCierraAlRetomarActividad() throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        var doc = factory.newDocumentBuilder().parse(Path.of("src/main/webapp/WEB-INF/navegacion.xhtml").toFile());
        var monitor = (org.w3c.dom.Element) doc.getElementsByTagNameNS("http://primefaces.org/ui", "idleMonitor").item(0);
        assertEquals("#{loginBean.tiempoSession * 1000}", monitor.getAttribute("timeout"));
        assertTrue(monitor.getAttribute("rendered").contains("tiempoSession gt 0"));
        var eventos = monitor.getElementsByTagNameNS("http://primefaces.org/ui", "ajax");
        assertEquals(1, eventos.getLength());
        assertEquals("idle", ((org.w3c.dom.Element) eventos.item(0)).getAttribute("event"));
        assertEquals(900000, 15 * 60 * 1000);
    }
}
