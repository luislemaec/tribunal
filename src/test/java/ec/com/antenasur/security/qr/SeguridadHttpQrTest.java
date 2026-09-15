package ec.com.antenasur.security.qr;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SeguridadHttpQrTest {
    @Test void permiteSoloActaYRecursos() {
        assertTrue(RutasSesionQr.permitida("/actaE.jsf", "GET"));
        assertTrue(RutasSesionQr.permitida("/actaE.jsf", "POST"));
        assertTrue(RutasSesionQr.permitida("/actaE.xhtml", "GET"));
        assertTrue(RutasSesionQr.permitida("/actaE.xhtml", "POST"));
        assertTrue(RutasSesionQr.permitida("/faces/actaE.xhtml", "POST"));
        assertTrue(RutasSesionQr.permitida("/actaE.faces", "POST"));
        assertTrue(RutasSesionQr.permitida("/jakarta.faces.resource/primefaces.js.jsf", "GET"));
        assertFalse(RutasSesionQr.permitida("/jakarta.faces.resource/primefaces.js.jsf", "POST"));
        for (String ruta : new String[]{"/dashboard.jsf", "/usuarios.jsf", "/faces/actaE.jsf",
                "/actaE.jsf;jsessionid=123", "/resources/../usuarios.jsf", "/%61ctaE.jsf", "/acceso-acta/canjear"})
            assertFalse(RutasSesionQr.permitida(ruta, "GET"), ruta);
    }

    @Test void rutasPublicasSonExactas() {
        assertTrue(RutasSesionQr.publica("/acceso-acta"));
        assertTrue(RutasSesionQr.publica("/acceso-acta/canjear"));
        assertFalse(RutasSesionQr.publica("/acceso-acta/otra"));
        assertFalse(RutasSesionQr.publica("/resources/qr/secretos.txt"));
    }

    @Test void csrfRequiereCoincidenciaYFormatoCanonico() {
        String token = TokenActaQr.generar();
        assertTrue(AccesoActaServlet.iguales(token, token));
        assertFalse(AccesoActaServlet.iguales(token, TokenActaQr.generar()));
        assertFalse(AccesoActaServlet.iguales(null, null));
        assertFalse(AccesoActaServlet.iguales("", ""));
        assertFalse(AccesoActaServlet.iguales(token, token + "="));
    }

    @Test void limitaCanjesPorIpYSinBloquearOtrasIps() {
        var reloj = new Reloj();
        var limite = new LimiteIntentosQr(reloj);
        for (int i = 0; i < 10; i++) assertTrue(limite.permitir("ip1", true));
        assertFalse(limite.permitir("ip1", true));
        assertTrue(limite.permitir("ip2", true));
        assertTrue(limite.permitir("ip1", false));
        reloj.ahora = reloj.ahora.plusSeconds(60);
        assertTrue(limite.permitir("ip1", true));
    }

    @Test void limitaLlegadasSinConsumirIntentosDeCanje() {
        var limite = new LimiteIntentosQr(new Reloj());
        for (int i = 0; i < 60; i++) assertTrue(limite.permitir("ip", false));
        assertFalse(limite.permitir("ip", false));
        assertTrue(limite.permitir("ip", true));
    }

    @Test void estructurasInternasNoPublicanSecretosEnToString() {
        String secreto = TokenActaQr.generar();
        var contexto = new ContextoSesionQr(secreto, 1, "usuario", 2, 3, 4);
        assertFalse(contexto.toString().contains(secreto));
        var resultado = new CanjeAccesoQr(ResultadoAccesoQr.VALIDO, secreto, secreto, contexto);
        assertFalse(resultado.toString().contains(secreto));
    }

    private static class Reloj extends Clock {
        Instant ahora = Instant.parse("2026-11-30T12:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zona) { return this; }
        public Instant instant() { return ahora; }
    }
}
