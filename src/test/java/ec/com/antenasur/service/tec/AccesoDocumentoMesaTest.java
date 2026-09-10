package ec.com.antenasur.service.tec;

import java.lang.reflect.Proxy;
import java.security.Principal;
import java.util.Set;
import jakarta.ejb.SessionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.DenyAll;
import static org.junit.jupiter.api.Assertions.*;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.facade.UsuarioFacade;
import ec.com.antenasur.model.Usuario;
import ec.com.antenasur.model.Persona;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.dto.MesaDTO;
import ec.com.antenasur.dto.MiembroJRVDTO;

class AccesoDocumentoMesaTest {
    static void inyectar(Object destino, String nombre, Object valor) throws Exception {
        var campo = destino.getClass().getDeclaredField(nombre); campo.setAccessible(true); campo.set(destino, valor);
    }
    private AccesoDocumentoMesaService acceso(Set<String> roles) throws Exception {
        var servicio = new AccesoDocumentoMesaService();
        var contexto = (SessionContext) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{SessionContext.class},
                (p,m,a) -> switch (m.getName()) {
                    case "isCallerInRole" -> roles.contains(a[0]);
                    case "getCallerPrincipal" -> (Principal) () -> "usuario-autenticado";
                    default -> throw new UnsupportedOperationException(m.getName());
                });
        inyectar(servicio, "contexto", contexto);
        inyectar(servicio, "procesoService", new ProcesoElectoralService() {
            @Override public ProcesoElectoral getActivo() { var p = new ProcesoElectoral(); p.setId(1); return p; }
        });
        inyectar(servicio, "usuarioFacade", new UsuarioFacade() {
            @Override public Usuario findByUsuarioName(String nombre) {
                assertEquals("usuario-autenticado", nombre);
                var u = new Usuario(); var persona = new Persona(); persona.setId(70); u.setPersonsa(persona); return u;
            }
        });
        inyectar(servicio, "miembroService", new MiembroJRVService() {
            @Override public MiembroJRVDTO obtenerDesignacionPresidentePorPersonaProceso(Integer persona, Integer proceso) {
                assertEquals(70, persona); assertEquals(1, proceso);
                var d = new MiembroJRVDTO(); var mesa = new MesaDTO(); mesa.setId(4); d.setMesa(mesa); return d;
            }
        });
        return servicio;
    }
    @Test void anonimoNoTieneAcceso() throws Exception {
        var s = acceso(Set.of()); assertThrows(NegocioException.class, () -> s.validar(4,1));
    }
    @Test void presidenteSoloAccedeASuMesa() throws Exception {
        var s = acceso(Set.of("SITEC-Presidente-mesa"));
        assertDoesNotThrow(() -> s.validar(4,1)); assertThrows(NegocioException.class, () -> s.validar(5,1));
    }
    @Test void otroRolNoAmpliaAlcanceDelPresidente() throws Exception {
        var s = acceso(Set.of("SITEC-Presidente-mesa", "SITEC-Administrador"));
        assertThrows(NegocioException.class, () -> s.validar(5,1));
    }
    @ParameterizedTest
    @ValueSource(strings = {"SITEC-Administrador", "SITEC-Tribunal"})
    void ambosRevisoresPuedenInvocarYAccederAlProcesoActivo(String rol) throws Exception {
        var clase = AccesoDocumentoMesaService.class;
        assertTrue(Set.of(clase.getAnnotation(RolesAllowed.class).value()).contains(rol));
        var s = acceso(Set.of(rol));
        assertNull(s.mesaPermitida(1));
        assertTrue(s.esRevisor());
        assertDoesNotThrow(() -> s.validar(5,1));
        assertDoesNotThrow(() -> s.exigirRevisor(5,1));
        assertThrows(NegocioException.class, () -> s.validar(5,2));
        assertThrows(NegocioException.class, () -> s.mesaPermitida(null));
        assertThrows(NegocioException.class, () -> s.validar(null,1));
    }
    @Test void todosLosMetodosEjbTienenPermisosExplicitosSinAperturaGeneral() {
        var clase = AccesoDocumentoMesaService.class;
        assertNotNull(clase.getAnnotation(RolesAllowed.class));
        assertFalse(clase.isAnnotationPresent(PermitAll.class));
        assertFalse(clase.isAnnotationPresent(DenyAll.class));
        Set<String> permitidos = Set.of("SITEC-Administrador", "SITEC-Tribunal", "SITEC-Presidente-mesa");
        for (var metodo : clase.getDeclaredMethods()) {
            if (!java.lang.reflect.Modifier.isPublic(metodo.getModifiers())) continue;
            assertFalse(metodo.isAnnotationPresent(PermitAll.class), metodo.getName());
            assertFalse(metodo.isAnnotationPresent(DenyAll.class), metodo.getName());
            var permiso = metodo.getAnnotation(RolesAllowed.class);
            if (permiso == null) permiso = clase.getAnnotation(RolesAllowed.class);
            assertEquals(permitidos, Set.of(permiso.value()), metodo.getName());
        }
    }
    @Test void presidenteNoPuedeValidarResultados() throws Exception {
        var s = acceso(Set.of("SITEC-Presidente-mesa")); assertThrows(NegocioException.class, () -> s.exigirRevisor(4,1));
    }
}
