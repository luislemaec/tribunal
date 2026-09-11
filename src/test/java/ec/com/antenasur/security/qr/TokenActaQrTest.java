package ec.com.antenasur.security.qr;

import java.net.URI;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TokenActaQrTest {
    @Test void generaTokensOpacosCanonicosSinColisionesEnLaMuestra() {
        var tokens = new HashSet<String>();
        for (int i = 0; i < 1000; i++) {
            String token = TokenActaQr.generar();
            assertEquals(43, token.length());
            assertTrue(TokenActaQr.formatoValido(token));
            assertTrue(tokens.add(token));
            assertTrue(TokenActaQr.hash(token).matches("[0-9a-f]{64}"));
        }
    }

    @Test void rechazaFormatosManipuladosSinNormalizarlos() {
        String token = "A".repeat(43);
        assertTrue(TokenActaQr.formatoValido(token));
        for (String invalido : new String[]{"", " " + token, token + "=", "A".repeat(42),
                "A".repeat(44), "A".repeat(42) + "B", "../" + token, "\n" + token}) {
            assertFalse(TokenActaQr.formatoValido(invalido));
            assertThrows(IllegalArgumentException.class, () -> TokenActaQr.hash(invalido));
        }
        assertFalse(TokenActaQr.formatoValido(null));
    }

    @Test void urlSoloIncluyeTokenOpacoEnConsulta() {
        String token = TokenActaQr.generar();
        URI url = TokenActaQr.url(URI.create("https://tribunal.conpociiech.org/"), token);
        assertEquals("/acceso-acta", url.getPath());
        assertEquals("token=" + token, url.getQuery());
        assertNull(url.getFragment());
        assertTrue(TokenActaQr.urlAccesoValida(url.toString()));
        assertNull(url.getUserInfo());
    }

    @Test void consultaRechazaDuplicadosEscapesYCamposAdicionales() {
        String token = TokenActaQr.generar();
        assertEquals(token, TokenActaQr.tokenConsulta("token=" + token));
        for (String consulta : new String[]{"token=" + token + "&token=" + token, "token=" + token + "&mesa=1",
                "token=%41" + token.substring(1), "TOKEN=" + token, "token=", "token=" + token + "="})
            assertNull(TokenActaQr.tokenConsulta(consulta));
        assertFalse(TokenActaQr.urlAccesoValida(null));
        assertFalse(TokenActaQr.urlAccesoValida("http://dominio/acceso-acta?token=" + token));
    }

    @Test void rechazaOrigenInseguroOConDatosAdicionales() {
        for (String origen : new String[]{"http://tribunal.conpociiech.org", "//tribunal.conpociiech.org",
                "https://u:p@tribunal.conpociiech.org", "https://tribunal.conpociiech.org/?q=1",
                "https://tribunal.conpociiech.org/#fragmento", "https://tribunal.conpociiech.org/otra"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> TokenActaQr.url(URI.create(origen), TokenActaQr.generar()));
        }
    }

    @Test void representacionDeResultadosNoExponeCredenciales() {
        String token = TokenActaQr.generar();
        var emision = new EmisionAccesoQr(1L, TokenActaQr.url(URI.create("https://tribunal.conpociiech.org"), token));
        var canje = new CanjeAccesoQr(ResultadoAccesoQr.VALIDO, token);
        assertFalse(emision.toString().contains(token));
        assertFalse(canje.toString().contains(token));
    }
}
