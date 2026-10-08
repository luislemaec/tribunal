package ec.com.antenasur.security.api;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;

import ec.com.antenasur.service.tec.SesionMovilService;
import ec.com.antenasur.service.tec.SesionMovilService.SesionValida;
import ec.com.antenasur.util.LoginFilterExcluder;

import static org.junit.jupiter.api.Assertions.*;

/** Puerta de la API móvil: HTTPS, Bearer, rutas públicas y sesión restringida al cambio de clave. */
class FiltroApiMovilTest {

    private static final String TOKEN = "A".repeat(43);

    @Test
    void extraeElTokenBearer() {
        assertEquals(TOKEN, FiltroApiMovil.tokenBearer(peticion("/api/v1/auth/yo", "Bearer " + TOKEN, true)));
        assertEquals(TOKEN, FiltroApiMovil.tokenBearer(peticion("/api/v1/auth/yo", "bearer  " + TOKEN + " ", true)));
        assertNull(FiltroApiMovil.tokenBearer(peticion("/api/v1/auth/yo", null, true)));
        assertNull(FiltroApiMovil.tokenBearer(peticion("/api/v1/auth/yo", "Basic abc", true)));
        assertNull(FiltroApiMovil.tokenBearer(peticion("/api/v1/auth/yo", "Bearer   ", true)));
    }

    @Test
    void soloLoginYRefreshSonPublicos() {
        assertEquals(java.util.Set.of("/api/v1/auth/login", "/api/v1/auth/refresh"), FiltroApiMovil.RUTAS_PUBLICAS);
    }

    @Test
    void sinHttpsResponde403SinLlegarAlRecurso() throws Exception {
        Respuesta r = filtrar("/api/v1/auth/login", null, false, null);
        assertEquals(403, r.estado);
        assertTrue(r.cuerpo.toString().contains("HTTPS_REQUERIDO"));
        assertFalse(r.continuo);
    }

    @Test
    void lasRutasPublicasPasanSinToken() throws Exception {
        Respuesta r = filtrar("/api/v1/auth/login", null, true, null);
        assertTrue(r.continuo);
        assertEquals("no-store", r.cabeceras.get("Cache-Control"));
    }

    @Test
    void sinTokenOConTokenInvalidoResponde401ConDesafioBearer() throws Exception {
        for (String cabecera : new String[] { null, "Bearer " + TOKEN }) {
            Respuesta r = filtrar("/api/v1/tribunal/proceso/resumen", cabecera, true, null);
            assertEquals(401, r.estado);
            assertEquals("Bearer", r.cabeceras.get("WWW-Authenticate"));
            assertTrue(r.cuerpo.toString().contains("SESION_INVALIDA"));
            assertFalse(r.continuo);
        }
    }

    @Test
    void laSesionRestringidaSoloPermiteCambiarClaveYoYLogout() throws Exception {
        SesionValida restringida = new SesionValida(1L, 10, "nuevo", true);
        Respuesta r = filtrar("/api/v1/tribunal/proceso/resumen", "Bearer " + TOKEN, true, restringida);
        assertEquals(403, r.estado);
        assertTrue(r.cuerpo.toString().contains("CAMBIO_CLAVE_OBLIGATORIO"));
        assertFalse(r.continuo);
        assertEquals(java.util.Set.of("/api/v1/auth/cambiar-clave", "/api/v1/auth/yo", "/api/v1/auth/logout"),
                FiltroApiMovil.RUTAS_CAMBIO_CLAVE);
    }

    @Test
    void elLoginFilterWebNoInterfiereConLaApi() {
        LoginFilterExcluder excluder = LoginFilterExcluder.getInstance("");
        assertTrue(excluder.isExcludeUrl("/api/v1/tribunal/proceso/resumen"));
        assertFalse(excluder.isExcludeUrl("/api/../dashboard.jsf"), "sin atajos con ..");
        assertFalse(excluder.isExcludeUrl("/apix/v1"));
    }

    // ------------------------------------------------------------

    private static final class Respuesta {
        int estado = 200;
        final Map<String, String> cabeceras = new HashMap<>();
        final StringWriter cuerpo = new StringWriter();
        boolean continuo;
    }

    private Respuesta filtrar(String ruta, String autorizacion, boolean segura, SesionValida sesion) throws Exception {
        FiltroApiMovil filtro = new FiltroApiMovil();
        var campo = FiltroApiMovil.class.getDeclaredField("sesiones");
        campo.setAccessible(true);
        campo.set(filtro, new SesionMovilService() {
            @Override
            public SesionValida validarAcceso(String token) {
                return token == null ? null : sesion;
            }
        });
        Respuesta r = new Respuesta();
        PrintWriter escritor = new PrintWriter(r.cuerpo);
        HttpServletResponse respuesta = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { HttpServletResponse.class }, (p, m, a) -> switch (m.getName()) {
                    case "setHeader" -> r.cabeceras.put((String) a[0], (String) a[1]);
                    case "setStatus" -> { r.estado = (Integer) a[0]; yield null; }
                    case "getWriter" -> escritor;
                    case "setContentType", "setCharacterEncoding" -> null;
                    default -> throw new UnsupportedOperationException(m.getName());
                });
        FilterChain cadena = (req, res) -> r.continuo = true;
        filtro.doFilter(peticion(ruta, autorizacion, segura), respuesta, cadena);
        escritor.flush();
        return r;
    }

    private HttpServletRequest peticion(String ruta, String autorizacion, boolean segura) {
        return (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { HttpServletRequest.class }, (p, m, a) -> switch (m.getName()) {
                    case "isSecure" -> segura;
                    case "getRequestURI" -> ruta;
                    case "getContextPath" -> "";
                    case "getHeader" -> "Authorization".equals(a[0]) ? autorizacion : null;
                    default -> throw new UnsupportedOperationException(m.getName());
                });
    }
}
