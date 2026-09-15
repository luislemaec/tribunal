package ec.com.antenasur.service.tec;

import ec.com.antenasur.facade.UsuarioFacade;
import ec.com.antenasur.model.Usuario;
import ec.com.antenasur.model.Persona;
import ec.com.antenasur.security.qr.ContextoSesionQr;
import ec.com.antenasur.exception.NegocioException;
import java.lang.reflect.Proxy;
import java.security.Principal;
import jakarta.ejb.SessionContext;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IdentidadActaFisicaTest {
    private AccesoDocumentoMesaService servicio(boolean qr, int idUsuario, boolean activo) throws Exception {
        var acceso = new AccesoDocumentoMesaService();
        var contexto = new ContextoSesionQr("prueba-interna", 8, "presidente", 1, 29, 32);
        AccesoDocumentoMesaTest.inyectar(acceso, "alcanceQr", new AlcanceSesionQrService() {
            @Override public ContextoSesionQr contexto() { return qr ? contexto : null; }
            @Override public void validar(Integer mesa, Integer proceso) {
                if (!Integer.valueOf(32).equals(mesa) || !Integer.valueOf(1).equals(proceso))
                    throw new NegocioException("alcance");
            }
        });
        AccesoDocumentoMesaTest.inyectar(acceso, "contexto", Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{SessionContext.class}, (p,m,a) -> {
                    if (m.getName().equals("getCallerPrincipal")) return (Principal) () -> qr ? "TECQR:presidente" : "presidente";
                    throw new AssertionError(m.getName());
                }));
        AccesoDocumentoMesaTest.inyectar(acceso, "usuarioFacade", new UsuarioFacade() {
            @Override public Usuario findByUsuarioName(String nombre) {
                assertEquals("presidente", nombre, "No consultar BD con el alias TECQR");
                var persona = new Persona(); persona.setId(12206); persona.setEstado(true);
                var usuario = new Usuario(); usuario.setId(idUsuario); usuario.setEstado(activo); usuario.setPersonsa(persona);
                return usuario;
            }
        });
        return acceso;
    }

    @Test void formYQrResuelvenLaMismaPersonaReal() throws Exception {
        assertEquals(12206, servicio(false,8,true).personaAutenticadaId());
        assertEquals(12206, servicio(true,8,true).personaAutenticadaId());
    }
    @Test void qrNoAceptaUsuarioDistintoNiInactivo() throws Exception {
        var diferente = servicio(true,9,true);
        assertThrows(NegocioException.class, diferente::personaAutenticadaId);
        var inactivo = servicio(true,8,false);
        assertThrows(NegocioException.class, inactivo::personaAutenticadaId);
    }
    @Test void qrConservaRestriccionMesaProceso() throws Exception {
        var acceso = servicio(true,8,true);
        assertDoesNotThrow(() -> acceso.validar(32,1));
        assertThrows(NegocioException.class, () -> acceso.validar(33,1));
        assertThrows(NegocioException.class, () -> acceso.validar(32,2));
    }
    @Test void todosLosMetodosPublicosTienenRolesExplicitosSinPermisosQrAdministrativos() {
        for (var m : ActaFisicaEscrutinioService.class.getDeclaredMethods()) {
            if (!java.lang.reflect.Modifier.isPublic(m.getModifiers())) continue;
            assertFalse(m.isAnnotationPresent(jakarta.annotation.security.PermitAll.class));
            var permiso = m.getAnnotation(jakarta.annotation.security.RolesAllowed.class);
            assertNotNull(permiso, m.getName());
            var esperados = switch (m.getName()) {
                case "cargar", "obtenerVigente" -> java.util.Set.of("SITEC-Administrador", "SITEC-Tribunal", "SITEC-Presidente-mesa");
                case "revisar", "validarActaFinal" -> java.util.Set.of("SITEC-Administrador", "SITEC-Tribunal");
                default -> throw new AssertionError("Definir permisos para " + m.getName());
            };
            assertEquals(esperados, java.util.Set.of(permiso.value()), m.getName());
        }
    }

    @Test void consultaVigenteRechazaOtraMesaOProcesoAntesDeConsultarDocumentos() throws Exception {
        var actas = new ActaFisicaEscrutinioService();
        AccesoDocumentoMesaTest.inyectar(actas, "accesoDocumental", servicio(true,8,true));
        assertThrows(NegocioException.class, () -> actas.obtenerVigente(33,1));
        assertThrows(NegocioException.class, () -> actas.obtenerVigente(32,2));
    }
}
