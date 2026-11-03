package ec.com.antenasur.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.Principal;
import java.util.List;
import java.util.Set;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;

import org.junit.jupiter.api.Test;

import ec.com.antenasur.dto.IglesiaDTO;
import ec.com.antenasur.enums.OperacionCronograma;
import ec.com.antenasur.exception.IglesiaPersonaException;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.facade.IglesiaBajaFacade;
import ec.com.antenasur.facade.IglesiaFacade;
import ec.com.antenasur.facade.IglesiaPersonaFacade;
import ec.com.antenasur.facade.UsuarioFacade;
import ec.com.antenasur.facade.tec.ProcesoElectoralFacade;
import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.Usuario;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.service.tec.CronogramaService;

/**
 * Cronograma en el servidor (fase 1) y política central de la excepción (fase 2):
 * SITEC-Administrador y SITEC-Tribunal operan fuera de las fases si hay un proceso activo;
 * SITEC-IglesiaAdmin (también con doble rol) y los demás roles siguen sujetos a la fase.
 */
class CronogramaEnServidorTest {

    private static final String ADMIN = "SITEC-Administrador";
    private static final String TRIBUNAL = "SITEC-Tribunal";
    private static final String IGLESIA_ADMIN = "SITEC-IglesiaAdmin";
    private static final int IGLESIA_ASIGNADA = 7;
    private static final String MSG_FASE = "El registro de iglesias no está habilitado en la fase electoral vigente.";

    // ── Dobles de prueba ──────────────────────────────────────────────────────

    private static SessionContext sesion(String... roles) {
        Set<String> conjunto = Set.of(roles);
        Principal principal = () -> "usuario.prueba";
        return (SessionContext) Proxy.newProxyInstance(SessionContext.class.getClassLoader(),
                new Class<?>[] { SessionContext.class }, (proxy, metodo, args) -> switch (metodo.getName()) {
                    case "isCallerInRole" -> conjunto.contains((String) args[0]);
                    case "getCallerPrincipal" -> principal;
                    case "setRollbackOnly" -> null;
                    default -> throw new UnsupportedOperationException(metodo.getName());
                });
    }

    /** Cronograma real con fases simuladas; la política (permite/exento) es la de producción. */
    private static CronogramaService cronograma(boolean faseAbierta, boolean procesoActivo, SessionContext sesion)
            throws Exception {
        CronogramaService servicio = new CronogramaService() {
            @Override
            public boolean permiteRegistroIglesias() {
                return faseAbierta;
            }

            @Override
            public boolean permiteAsignacionUsuarios() {
                return faseAbierta;
            }

            @Override
            public boolean permiteEdicionPadron() {
                return faseAbierta;
            }
        };
        inyectar(servicio, CronogramaService.class, "sessionContext", sesion);
        inyectar(servicio, CronogramaService.class, "procesoElectoralFacade", new ProcesoElectoralFacade() {
            @Override
            public ProcesoElectoral getActivo() {
                return procesoActivo ? new ProcesoElectoral() : null;
            }
        });
        return servicio;
    }

    private static void inyectar(Object destino, Class<?> clase, String campo, Object valor) throws Exception {
        Field f = clase.getDeclaredField(campo);
        f.setAccessible(true);
        f.set(destino, valor);
    }

    private static UsuarioFacade usuarioConIglesia(Integer iglesiaId) {
        return new UsuarioFacade() {
            @Override
            public Usuario findByUsuarioName(String username) {
                Usuario usuario = new Usuario();
                Iglesia iglesia = new Iglesia();
                iglesia.setId(iglesiaId);
                usuario.setIglesia(iglesia);
                return usuario;
            }
        };
    }

    private static IglesiaDTO iglesia(Integer id) {
        IglesiaDTO dto = new IglesiaDTO();
        dto.setId(id);
        return dto;
    }

    // ── Política central ──────────────────────────────────────────────────────

    @Test
    void administradorYTribunalOperanFueraDeFaseConProcesoActivo() throws Exception {
        for (String rol : new String[] { ADMIN, TRIBUNAL }) {
            for (OperacionCronograma op : OperacionCronograma.values()) {
                assertTrue(cronograma(true, true, sesion(rol)).permite(op), rol + " dentro " + op);
                assertTrue(cronograma(false, true, sesion(rol)).permite(op), rol + " fuera " + op);
                assertFalse(cronograma(false, false, sesion(rol)).permite(op), rol + " sin proceso " + op);
            }
        }
    }

    @Test
    void iglesiaAdminDobleRolYOtrosRolesSiguenLaFase() throws Exception {
        String[][] sujetos = { { IGLESIA_ADMIN }, { IGLESIA_ADMIN, ADMIN }, { IGLESIA_ADMIN, TRIBUNAL },
                { "SITEC-Tecnico" }, {} };
        for (String[] roles : sujetos) {
            for (OperacionCronograma op : OperacionCronograma.values()) {
                assertTrue(cronograma(true, true, sesion(roles)).permite(op), String.join("+", roles) + " dentro " + op);
                assertFalse(cronograma(false, true, sesion(roles)).permite(op), String.join("+", roles) + " fuera " + op);
            }
            assertFalse(cronograma(false, true, sesion(roles)).exentoDeCronograma());
        }
        assertFalse(cronograma(true, true, sesion(ADMIN)).permite(null));
    }

    // ── Aviso de miembros (Dashboard y Personas) ──────────────────────────────

    private static CronogramaService conActualizacion(Boolean pasada) {
        return new CronogramaService() {
            @Override
            public ec.com.antenasur.dto.CronogramaFaseDTO getFaseActualizacionMiembros() {
                if (pasada == null) {
                    return null;
                }
                ec.com.antenasur.dto.CronogramaFaseDTO fase = new ec.com.antenasur.dto.CronogramaFaseDTO();
                fase.setPasada(pasada);
                return fase;
            }
        };
    }

    @Test
    void elAvisoDeMiembrosSeOcultaSoloCuandoLaFaseTermino() {
        assertTrue(conActualizacion(true).actualizacionMiembrosFinalizada(), "fase terminada");
        assertFalse(conActualizacion(false).actualizacionMiembrosFinalizada(), "vigente o futura");
        assertFalse(conActualizacion(null).actualizacionMiembrosFinalizada(), "sin fase configurada");
    }

    // ── Iglesias: guardar ─────────────────────────────────────────────────────

    private static IglesiaService iglesiaService(boolean fase, boolean proceso, String... roles) throws Exception {
        SessionContext s = sesion(roles);
        IglesiaService servicio = new IglesiaService();
        inyectar(servicio, IglesiaService.class, "cronogramaService", cronograma(fase, proceso, s));
        inyectar(servicio, IglesiaService.class, "sessionContext", s);
        inyectar(servicio, IglesiaService.class, "usuarioFacade", usuarioConIglesia(IGLESIA_ASIGNADA));
        return servicio;
    }

    private static RuntimeException validar(IglesiaService servicio, IglesiaDTO dto) throws Exception {
        Method m = IglesiaService.class.getDeclaredMethod("validarRegistroPermitido", IglesiaDTO.class);
        m.setAccessible(true);
        try {
            m.invoke(servicio, dto);
            return null;
        } catch (InvocationTargetException e) {
            return (RuntimeException) e.getCause();
        }
    }

    @Test
    void guardarIglesiaFueraDeFase() throws Exception {
        for (String rol : new String[] { ADMIN, TRIBUNAL }) {
            assertNull(validar(iglesiaService(false, true, rol), iglesia(null)), rol + " crea fuera de fase");
            assertNull(validar(iglesiaService(false, true, rol), iglesia(99)), rol + " edita fuera de fase");
            assertEquals(MSG_FASE, validar(iglesiaService(false, false, rol), iglesia(99)).getMessage(),
                    rol + " sin proceso activo");
        }
        assertEquals(MSG_FASE, validar(iglesiaService(false, true, IGLESIA_ADMIN), iglesia(IGLESIA_ASIGNADA)).getMessage());
        assertEquals(MSG_FASE, validar(iglesiaService(false, true, IGLESIA_ADMIN, ADMIN), iglesia(IGLESIA_ASIGNADA))
                .getMessage(), "doble rol");
    }

    @Test
    void elAlcanceDeIglesiaAdminNoCambia() throws Exception {
        assertEquals("El usuario IglesiaAdmin solo puede gestionar la iglesia asignada.",
                validar(iglesiaService(true, true, IGLESIA_ADMIN), iglesia(null)).getMessage());
        assertEquals("No tiene permisos para consultar o gestionar esta iglesia.",
                validar(iglesiaService(true, true, IGLESIA_ADMIN), iglesia(99)).getMessage());
        assertNull(validar(iglesiaService(true, true, IGLESIA_ADMIN), iglesia(IGLESIA_ASIGNADA)));
        assertTrue(validar(iglesiaService(true, true, IGLESIA_ADMIN, ADMIN), iglesia(null)) instanceof NegocioException);
    }

    @Test
    void cambiarRucLoDecideElServidorPorRol() throws Exception {
        Method m = IglesiaService.class.getDeclaredMethod("esCallerAdministradorOTribunal");
        m.setAccessible(true);
        assertTrue((boolean) m.invoke(iglesiaService(true, true, ADMIN)));
        assertTrue((boolean) m.invoke(iglesiaService(true, true, TRIBUNAL)));
        assertFalse((boolean) m.invoke(iglesiaService(true, true, IGLESIA_ADMIN)));
        assertTrue((boolean) m.invoke(iglesiaService(true, true, IGLESIA_ADMIN, TRIBUNAL)));
    }

    @Test
    void rolesDeLosMetodosDeEscritura() throws Exception {
        Set<String> tres = Set.of(ADMIN, TRIBUNAL, IGLESIA_ADMIN);
        assertEquals(tres, Set.of(IglesiaService.class.getMethod("guardarDesdeDTO", IglesiaDTO.class)
                .getAnnotation(RolesAllowed.class).value()));
        assertEquals(tres, Set.of(IglesiaService.class.getMethod("guardarDesdeDTO", IglesiaDTO.class, boolean.class)
                .getAnnotation(RolesAllowed.class).value()));
        assertEquals(Set.of(ADMIN, TRIBUNAL), Set.of(IglesiaService.class.getMethod("eliminarPorId", Integer.class)
                .getAnnotation(RolesAllowed.class).value()));
        assertNull(IglesiaService.class.getAnnotation(RolesAllowed.class));
        assertNull(IglesiaService.class.getMethod("obtenerDTOPorId", Integer.class).getAnnotation(RolesAllowed.class));
    }

    /**
     * WildFly (default-missing-method-permissions-deny-access=true) niega los métodos sin
     * anotación de un EJB que tiene seguridad: sin @PermitAll de clase, las lecturas de estos
     * servicios fallan con EJBAccessException (Iglesias, Personas y Dashboard quedan vacíos).
     */
    @Test
    void lecturasAbiertasConSeguridadPorMetodo() {
        for (Class<?> clase : new Class<?>[] { IglesiaService.class, CronogramaService.class }) {
            assertTrue(clase.isAnnotationPresent(jakarta.annotation.security.PermitAll.class), clase.getSimpleName());
        }
    }

    // ── Iglesias: eliminar ────────────────────────────────────────────────────

    private static IglesiaBajaService bajaService(boolean fase, boolean proceso, String... roles) throws Exception {
        IglesiaBajaService baja = new IglesiaBajaService();
        inyectar(baja, IglesiaBajaService.class, "cronogramaService", cronograma(fase, proceso, sesion(roles)));
        // Pasada la regla del cronograma, la iglesia «no existe»: el mensaje distingue hasta dónde llegó.
        inyectar(baja, IglesiaBajaService.class, "bajaFacade", new IglesiaBajaFacade() {
            @Override
            public Iglesia buscarSinFiltro(Integer id) {
                return null;
            }
        });
        return baja;
    }

    @Test
    void eliminarIglesiaFueraDeFase() throws Exception {
        for (String rol : new String[] { ADMIN, TRIBUNAL }) {
            assertEquals("La iglesia ya no existe; actualice la lista.",
                    assertThrows(NegocioException.class, () -> bajaService(false, true, rol).eliminar(1)).getMessage(),
                    rol + " pasa el cronograma");
            assertEquals(MSG_FASE,
                    assertThrows(NegocioException.class, () -> bajaService(false, false, rol).eliminar(1)).getMessage(),
                    rol + " sin proceso activo");
        }
    }

    // ── Asignación de administrador ───────────────────────────────────────────

    @Test
    void asignarAdministradorFueraDeFase() throws Exception {
        Method validar = AsignacionAdministradorIglesiaService.class.getDeclaredMethod("validarAsignacionBase",
                Integer.class, Integer.class);
        validar.setAccessible(true);
        for (boolean proceso : new boolean[] { true, false }) {
            AsignacionAdministradorIglesiaService servicio = new AsignacionAdministradorIglesiaService();
            inyectar(servicio, AsignacionAdministradorIglesiaService.class, "cronogramaService",
                    cronograma(false, proceso, sesion(TRIBUNAL)));
            inyectar(servicio, AsignacionAdministradorIglesiaService.class, "iglesiaFacade", new IglesiaFacade() {
                @Override
                public Iglesia findForAdminAssignment(Integer id) {
                    return null;
                }
            });
            InvocationTargetException e = assertThrows(InvocationTargetException.class, () -> validar.invoke(servicio, 1, 2));
            assertEquals(proceso ? "iglesias.admin.error.iglesia.no.disponible" : "iglesias.admin.error.fase.no.permite",
                    e.getCause().getMessage(), "proceso activo=" + proceso);
        }
    }

    // ── Personas ──────────────────────────────────────────────────────────────

    private static IglesiaPersonaService personaService(boolean fase, boolean proceso, String... roles) throws Exception {
        IglesiaPersonaService servicio = new IglesiaPersonaService();
        inyectar(servicio, IglesiaPersonaService.class, "cronogramaService", cronograma(fase, proceso, sesion(roles)));
        inyectar(servicio, IglesiaPersonaService.class, "iglesiaPersonaFacade", new IglesiaPersonaFacade() {
            @Override
            public IglesiaPersona find(Integer id) {
                return null;
            }
        });
        return servicio;
    }

    @Test
    void eliminarMiembroFueraDeFase() throws Exception {
        for (String rol : new String[] { ADMIN, TRIBUNAL }) {
            assertNull(personaService(false, true, rol).eliminarPorId(5), rol + " pasa el cronograma");
            assertEquals(0, personaService(false, true, rol).eliminarPorIds(List.of(5, 6)));
        }
        for (String[] roles : new String[][] { { IGLESIA_ADMIN }, { IGLESIA_ADMIN, ADMIN } }) {
            IglesiaPersonaException e = assertThrows(IglesiaPersonaException.class,
                    () -> personaService(false, true, roles).eliminarPorId(5));
            assertEquals("form.personas.error.cronograma", e.getMessageKey());
            assertThrows(IglesiaPersonaException.class, () -> personaService(false, true, roles).eliminarPorIds(List.of(5)));
        }
        assertThrows(IglesiaPersonaException.class, () -> personaService(false, false, ADMIN).eliminarPorId(5));
    }

    @Test
    void habilitacionMasivaFueraDeFase() throws Exception {
        for (boolean proceso : new boolean[] { true, false }) {
            SessionContext s = sesion(ADMIN);
            HabilitacionMasivaService servicio = new HabilitacionMasivaService();
            inyectar(servicio, HabilitacionMasivaService.class, "sessionContext", s);
            inyectar(servicio, HabilitacionMasivaService.class, "cronogramaService", cronograma(false, proceso, s));
            inyectar(servicio, HabilitacionMasivaService.class, "iglesiaFacade", new IglesiaFacade() {
                @Override
                public Iglesia find(Integer id) {
                    return null;
                }
            });
            IglesiaPersonaException e = assertThrows(IglesiaPersonaException.class, () -> servicio.habilitar(1, "127.0.0.1"));
            assertEquals(proceso ? "form.personas.error.iglesia.inactiva" : "form.personas.error.cronograma",
                    e.getMessageKey(), "proceso activo=" + proceso);
        }
    }
}
