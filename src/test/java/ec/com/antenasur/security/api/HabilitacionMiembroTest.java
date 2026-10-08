package ec.com.antenasur.security.api;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ec.com.antenasur.exception.IglesiaPersonaException;
import ec.com.antenasur.facade.IglesiaPersonaFacade;
import ec.com.antenasur.facade.UsuarioFacade;
import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.Persona;
import ec.com.antenasur.model.Usuario;
import ec.com.antenasur.service.IglesiaPersonaService;
import ec.com.antenasur.service.tec.CronogramaService;
import ec.com.antenasur.service.tec.GestionMovilService;

import static org.junit.jupiter.api.Assertions.*;

/** Habilitación de miembros desde la App: solo IglesiaAdmin, su iglesia y con el cronograma abierto. */
class HabilitacionMiembroTest {

    private static final int MI_IGLESIA = 3;

    private IglesiaPersona miembro;
    private boolean cronogramaAbierto = true;
    private boolean guardado;
    private IglesiaPersonaService servicio;

    @Test
    void laEscrituraSoloLaPuedeHacerElIglesiaAdmin() {
        List<Method> publicos = Arrays.stream(GestionMovilService.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()) && !m.isSynthetic()).toList();
        assertEquals(List.of("cambiarHabilitacionMiembro"), publicos.stream().map(Method::getName).toList());
        for (Method m : publicos) {
            assertEquals(Set.of("SITEC-IglesiaAdmin"), Set.of(m.getAnnotation(RolesAllowed.class).value()));
            assertNull(m.getAnnotation(PermitAll.class));
        }
        assertNull(GestionMovilService.class.getAnnotation(RolesAllowed.class));
        assertNull(GestionMovilService.class.getAnnotation(PermitAll.class));
    }

    @BeforeEach
    void preparar() throws Exception {
        miembro = miembro(10, MI_IGLESIA, true);
        miembro.setHabilitadoPadron(false);
        servicio = new IglesiaPersonaService();
        inyectar("cronogramaService", new CronogramaService() {
            @Override
            public boolean permiteEdicionPadron() {
                return cronogramaAbierto;
            }
        });
        inyectar("iglesiaPersonaFacade", new IglesiaPersonaFacade() {
            @Override
            public IglesiaPersona find(Integer id) {
                return miembro.getId().equals(id) ? miembro : null;
            }

            @Override
            public IglesiaPersona edit(IglesiaPersona entidad) {
                guardado = true;
                return entidad;
            }
        });
        inyectar("usuarioFacade", new UsuarioFacade() {
            @Override
            public Usuario findByUsuarioName(String nombre) {
                Usuario u = new Usuario();
                u.setUsername(nombre);
                Iglesia i = new Iglesia();
                i.setId(MI_IGLESIA);
                u.setIglesia(i);
                return u;
            }
        });
        inyectar("sessionContext", (SessionContext) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { SessionContext.class }, (p, m, a) -> switch (m.getName()) {
                    case "isCallerInRole" -> "SITEC-IglesiaAdmin".equals(a[0]);
                    case "getCallerPrincipal" -> (Principal) () -> "admin.iglesia";
                    default -> throw new UnsupportedOperationException(m.getName());
                }));
    }

    @Test
    void habilitaYMarcaComoRevisado() {
        var dto = servicio.cambiarHabilitacion(10, true);
        assertTrue(guardado);
        assertTrue(miembro.getHabilitadoPadron());
        assertNotNull(miembro.getFechaActualiza(), "queda revisado igual que en la web");
        assertEquals(Boolean.TRUE, dto.getHabilitadoPadron());
    }

    @Test
    void conElCronogramaCerradoNoCambiaNada() {
        cronogramaAbierto = false;
        var e = assertThrows(IglesiaPersonaException.class, () -> servicio.cambiarHabilitacion(10, true));
        assertEquals("form.personas.error.cronograma", e.getMessageKey());
        assertFalse(guardado);
        assertFalse(miembro.getHabilitadoPadron());
    }

    @Test
    void unMiembroDeOtraIglesiaSeRechaza() {
        miembro = miembro(10, 99, true);
        var e = assertThrows(IglesiaPersonaException.class, () -> servicio.cambiarHabilitacion(10, true));
        assertEquals("form.personas.error.iglesia.no.autorizada", e.getMessageKey());
        assertFalse(guardado);
    }

    @Test
    void unMiembroInactivoOInexistenteSeRechaza() {
        miembro = miembro(10, MI_IGLESIA, false);
        assertEquals("form.personas.error.miembro.noDisponible",
                assertThrows(IglesiaPersonaException.class, () -> servicio.cambiarHabilitacion(10, true)).getMessageKey());
        assertEquals("form.personas.error.miembro.noDisponible",
                assertThrows(IglesiaPersonaException.class, () -> servicio.cambiarHabilitacion(55, true)).getMessageKey());
        assertFalse(guardado);
    }

    private static IglesiaPersona miembro(int id, int iglesiaId, boolean activo) {
        Iglesia iglesia = new Iglesia();
        iglesia.setId(iglesiaId);
        IglesiaPersona ip = new IglesiaPersona(iglesia, new Persona());
        ip.setId(id);
        ip.setEstado(activo);
        return ip;
    }

    private void inyectar(String nombre, Object valor) throws Exception {
        var campo = IglesiaPersonaService.class.getDeclaredField(nombre);
        campo.setAccessible(true);
        campo.set(servicio, valor);
    }
}
