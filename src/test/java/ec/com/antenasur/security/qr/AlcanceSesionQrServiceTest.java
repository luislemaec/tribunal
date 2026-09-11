package ec.com.antenasur.security.qr;

import java.lang.reflect.Proxy;
import java.security.Principal;
import java.util.Set;
import java.util.function.Supplier;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.inject.Instance;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ec.com.antenasur.service.tec.AlcanceSesionQrService;
import ec.com.antenasur.service.tec.CanjeAccesoQrService;
import static org.junit.jupiter.api.Assertions.*;

class AlcanceSesionQrServiceTest {
    private static final Set<String> ELECTORALES = Set.of("SITEC-Administrador", "SITEC-Tribunal", ConfiguracionQr.ROL);
    private static final ContextoSesionQr QR = new ContextoSesionQr("prueba", 1, "presidente", 2, 3, 4);
    private String valorAnterior;

    @BeforeEach void habilitar() { valorAnterior = System.getProperty("tec.qr.enabled"); System.setProperty("tec.qr.enabled", "true"); }
    @AfterEach void restaurar() {
        if (valorAnterior == null) System.clearProperty("tec.qr.enabled"); else System.setProperty("tec.qr.enabled", valorAnterior);
    }

    private AlcanceSesionQrService servicio(Set<String> roles, String principal, Supplier<HttpServletRequest> solicitud,
            ResultadoAccesoQr resultado) throws Exception {
        var servicio = new AlcanceSesionQrService();
        var contexto = (SessionContext) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{SessionContext.class},
                (p, m, a) -> switch (m.getName()) {
                    case "isCallerInRole" -> roles.contains(a[0]);
                    case "getCallerPrincipal" -> (Principal) () -> principal;
                    default -> throw new UnsupportedOperationException(m.getName());
                });
        var instancia = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Instance.class},
                (p, m, a) -> { if ("get".equals(m.getName())) return solicitud.get(); throw new UnsupportedOperationException(m.getName()); });
        inyectar(servicio, "ejb", contexto);
        inyectar(servicio, "solicitudes", instancia);
        inyectar(servicio, "canje", new CanjeAccesoQrService() {
            @Override public ResultadoAccesoQr validarSesion(String prueba, Integer proceso, Integer mesa, String username) {
                assertEquals(QR.prueba(), prueba); assertEquals(2, proceso); assertEquals(4, mesa); assertEquals(QR.username(), username);
                if (resultado == null) fail("Una consulta de presencia no debe validar ni consultar la BD");
                return resultado;
            }
        });
        return servicio;
    }

    private static void inyectar(Object destino, String nombre, Object valor) throws Exception {
        var campo = destino.getClass().getDeclaredField(nombre); campo.setAccessible(true); campo.set(destino, valor);
    }

    private HttpServletRequest solicitud(ContextoSesionQr contexto, boolean segura) {
        var sesion = (HttpSession) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{HttpSession.class},
                (p, m, a) -> { assertEquals("getAttribute", m.getName()); assertEquals(ConfiguracionQr.SESION, a[0]); return contexto; });
        return (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{HttpServletRequest.class},
                (p, m, a) -> switch (m.getName()) {
                    case "getSession" -> { assertEquals(false, a[0]); yield contexto == null ? null : sesion; }
                    case "isSecure" -> segura;
                    default -> throw new UnsupportedOperationException(m.getName());
                });
    }

    @Test void contextoEsLaUnicaConsultaAbiertaDelServicio() throws Exception {
        var clase = AlcanceSesionQrService.class;
        assertFalse(clase.isAnnotationPresent(PermitAll.class));
        assertTrue(clase.getMethod("contexto").isAnnotationPresent(PermitAll.class));
        assertEquals(ELECTORALES, Set.of(clase.getMethod("validar", Integer.class, Integer.class).getAnnotation(RolesAllowed.class).value()));
        assertEquals(ELECTORALES, Set.of(clase.getMethod("impedirOperacionAdministrativa").getAnnotation(RolesAllowed.class).value()));
    }

    @Test void rolesNormalesYAnonimoObtienenNullSinConsultasNiCrearSesion() throws Exception {
        for (String rol : new String[]{"SITEC-Administrador", "SITEC-Tribunal", ConfiguracionQr.ROL, ""}) {
            var servicio = servicio(rol.isEmpty() ? Set.of() : Set.of(rol), "normal", () -> solicitud(null, false), null);
            assertNull(servicio.contexto());
        }
    }

    @Test void procesoAutomaticoSinPeticionNoTieneAlcance() throws Exception {
        assertNull(servicio(Set.of(), "anonymous", () -> null, null).contexto());
        assertNull(servicio(Set.of(), "anonymous", () -> { throw new ContextNotActiveException(); }, null).contexto());
        assertNull(servicio(Set.of(), "anonymous", () -> { throw new IllegalStateException("No HTTP request"); }, null).contexto());
    }

    @Test void proxyCdiSinHttpSeResuelveComoAusenciaPeroNoAutorizaIdentidadQr() throws Exception {
        var proxy = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{HttpServletRequest.class},
                (p, m, a) -> { throw new IllegalStateException("Weld: fuera de una peticion Servlet"); });
        assertNull(servicio(Set.of(), "anonymous", () -> proxy, null).contexto());
        assertThrows(AccesoQrException.class, () -> servicio(Set.of(ConfiguracionQr.MARCADOR),
                "TECQR:presidente", () -> proxy, null).contexto());
    }

    @Test void consultaQrNoConcedeAccesoHastaValidarSesionMesaYProceso() throws Exception {
        var servicio = servicio(Set.of(ConfiguracionQr.ROL, ConfiguracionQr.MARCADOR), "TECQR:presidente",
                () -> solicitud(QR, true), ResultadoAccesoQr.REVOCADO);
        assertEquals(QR, servicio.contexto());
        assertThrows(AccesoQrException.class, () -> servicio.validar(4, 2));
    }

    @Test void qrValidoSoloPermiteMesaProcesoOriginales() throws Exception {
        var servicio = servicio(Set.of(ConfiguracionQr.ROL, ConfiguracionQr.MARCADOR), "TECQR:presidente",
                () -> solicitud(QR, true), ResultadoAccesoQr.VALIDO);
        assertDoesNotThrow(() -> servicio.validar(4, 2));
        assertThrows(AccesoQrException.class, () -> servicio.validar(5, 2));
        assertThrows(AccesoQrException.class, () -> servicio.validar(4, 9));
        assertThrows(AccesoQrException.class, servicio::impedirOperacionAdministrativa);
    }

    @Test void identidadQrSinSesionNoPuedeConvertirseEnAccesoNormal() throws Exception {
        assertThrows(AccesoQrException.class, () -> servicio(Set.of(ConfiguracionQr.MARCADOR), "TECQR:presidente", () -> null, null).contexto());
        assertThrows(AccesoQrException.class, () -> servicio(Set.of(), "TECQR:presidente", () -> solicitud(null, true), null).contexto());
    }

    @Test void sesionNoSustituyeRolesPrincipalNiHttps() throws Exception {
        assertThrows(AccesoQrException.class, () -> servicio(Set.of("SITEC-Administrador"), "normal", () -> solicitud(QR, true), null).contexto());
        var roles = Set.of(ConfiguracionQr.ROL, ConfiguracionQr.MARCADOR);
        assertThrows(AccesoQrException.class, () -> servicio(roles, "TECQR:otro", () -> solicitud(QR, true), null).contexto());
        assertThrows(AccesoQrException.class, () -> servicio(roles, "TECQR:presidente", () -> solicitud(QR, false), null).contexto());
    }

    @Test void permisosDelCanjeSonExplicitosParaEvitarOtraDenegacion() throws Exception {
        var clase = CanjeAccesoQrService.class;
        assertFalse(clase.isAnnotationPresent(PermitAll.class));
        for (var metodo : clase.getDeclaredMethods()) {
            if (!java.lang.reflect.Modifier.isPublic(metodo.getModifiers())) continue;
            if (Set.of("canjear", "cerrar").contains(metodo.getName())) assertTrue(metodo.isAnnotationPresent(PermitAll.class));
            else {
                var roles = metodo.getAnnotation(RolesAllowed.class); assertNotNull(roles, metodo.getName());
                assertEquals("confirmar".equals(metodo.getName()) ? Set.of(ConfiguracionQr.MARCADOR) : ELECTORALES, Set.of(roles.value()));
            }
        }
    }
}
