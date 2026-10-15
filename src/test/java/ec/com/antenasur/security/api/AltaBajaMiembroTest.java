package ec.com.antenasur.security.api;

import java.lang.reflect.Proxy;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ec.com.antenasur.dto.PersonaDTO;
import ec.com.antenasur.exception.IglesiaPersonaException;
import ec.com.antenasur.facade.IglesiaFacade;
import ec.com.antenasur.facade.IglesiaPersonaFacade;
import ec.com.antenasur.facade.PersonaFacade;
import ec.com.antenasur.facade.UsuarioFacade;
import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.Persona;
import ec.com.antenasur.model.Usuario;
import ec.com.antenasur.model.generic.EntidadBase;
import ec.com.antenasur.service.IglesiaPersonaService;
import ec.com.antenasur.service.PersonaService;
import ec.com.antenasur.service.tec.CronogramaService;
import ec.com.antenasur.service.tec.GestionMovilService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Agregar y dar de baja miembros desde la App: reutilizan guardarDesdeDTO y eliminarPorId
 * de la web (mismas reglas), la iglesia es siempre la del usuario y la baja es lógica.
 */
class AltaBajaMiembroTest {

    private static final int MI_IGLESIA = 3;
    private static final int OTRA_IGLESIA = 9;
    /** Cédulas con dígito verificador válido. */
    private static final String CEDULA_NUEVA = "1710034065";
    private static final String CEDULA_OTRA = "1710034057";

    /** Base simulada: personas y vínculos (activos o no). */
    private final List<Persona> personas = new ArrayList<>();
    private final List<IglesiaPersona> vinculos = new ArrayList<>();
    private final List<String> escrituras = new ArrayList<>();
    private boolean cronogramaAbierto = true;
    private int secuencia = 100;

    private final IglesiaPersonaService servicio = new IglesiaPersonaService();
    private final GestionMovilService gestion = new GestionMovilService();

    @BeforeEach
    void preparar() throws Exception {
        CronogramaService cronograma = new CronogramaService() {
            @Override
            public boolean permiteEdicionPadron() {
                return cronogramaAbierto;
            }
        };
        UsuarioFacade usuarios = new UsuarioFacade() {
            @Override
            public Usuario findByUsuarioName(String nombre) {
                Usuario u = new Usuario();
                u.setUsername(nombre);
                u.setIglesia(iglesia(MI_IGLESIA));
                return u;
            }
        };
        PersonaFacade personaFacade = new PersonaFacade() {
            @Override
            public Persona find(Integer id) {
                return personas.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
            }

            @Override
            public Persona finByPersonaDocument(String documento) {
                return activaPorCedula(documento);
            }

            @Override
            public Persona buscarPorCedula(String documento) {
                return activaPorCedula(documento);
            }

            @Override
            public boolean existeOtraPersonaActivaConDocumento(String documento, Integer personaId) {
                Persona p = activaPorCedula(documento);
                return p != null && !p.getId().equals(personaId);
            }

            @Override
            public Persona create(Persona p) {
                p.setId(secuencia++);
                p.setEstado(true);
                personas.add(p);
                escrituras.add("crea Persona");
                return p;
            }

            @Override
            public Persona edit(Persona p) {
                escrituras.add("edita Persona" + (Boolean.FALSE.equals(p.getEstado()) ? " (baja)" : ""));
                return p;
            }
        };
        SessionContext contexto = (SessionContext) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { SessionContext.class }, (p, m, a) -> switch (m.getName()) {
                    case "isCallerInRole" -> "SITEC-IglesiaAdmin".equals(a[0]);
                    case "getCallerPrincipal" -> (Principal) () -> "admin.iglesia";
                    default -> throw new UnsupportedOperationException(m.getName());
                });

        inyectar(servicio, "cronogramaService", cronograma);
        inyectar(servicio, "usuarioFacade", usuarios);
        inyectar(servicio, "sessionContext", contexto);
        inyectar(servicio, "personaFacade", personaFacade);
        inyectar(servicio, "iglesiaFacade", new IglesiaFacade() {
            @Override
            public Iglesia find(Integer id) {
                return iglesia(id);
            }
        });
        inyectar(servicio, "iglesiaPersonaFacade", new IglesiaPersonaFacade() {
            @Override
            public IglesiaPersona find(Integer id) {
                return vinculos.stream().filter(v -> v.getId().equals(id)).findFirst().orElse(null);
            }

            @Override
            public IglesiaPersona create(IglesiaPersona v) {
                v.setId(secuencia++);
                v.setEstado(true);
                v.setFechaCrea(new java.util.Date()); // la pone EntidadAuditable en el contenedor
                vinculos.add(v);
                escrituras.add("crea IglesiaPersona");
                return v;
            }

            @Override
            public IglesiaPersona edit(IglesiaPersona v) {
                return v;
            }

            @Override
            public <T extends EntidadBase> T delete(T entidad) {
                entidad.setEstado(false);
                escrituras.add("baja IglesiaPersona");
                return entidad;
            }

            @Override
            public List<IglesiaPersona> listarRelacionesActivasPorDocumento(String documento, boolean bloquear) {
                return activasPorCedula(documento);
            }

            @Override
            public List<IglesiaPersona> listarActivasPorDocumento(String documento, boolean bloquear) {
                return activasPorCedula(documento);
            }

            @Override
            public Set<Integer> listarPersonasConRelacionesActivas(Collection<Integer> ids) {
                Set<Integer> r = new HashSet<>();
                for (IglesiaPersona v : vinculos) {
                    if (Boolean.TRUE.equals(v.getEstado()) && ids.contains(v.getPersona().getId())) {
                        r.add(v.getPersona().getId());
                    }
                }
                return r;
            }
        });

        PersonaService personaService = new PersonaService();
        inyectarEn(PersonaService.class, personaService, "personaFacade", personaFacade);
        inyectar(gestion, "iglesiaPersonaService", servicio);
        inyectar(gestion, "cronogramaService", cronograma);
        inyectar(gestion, "personaService", personaService);
        inyectar(gestion, "usuarioFacade", usuarios);
        inyectar(gestion, "sessionContext", contexto);
    }

    // ------------------------------------------------------------------ Agregar

    @Test
    void agregaUnaPersonaNuevaEnLaIglesiaDelUsuarioHabilitadaPorDefecto() {
        var r = gestion.crearMiembro("  PEREZ JUAN ", CEDULA_NUEVA, "m", null);
        assertEquals(List.of("crea Persona", "crea IglesiaPersona"), escrituras);
        IglesiaPersona v = vinculos.get(0);
        assertEquals(MI_IGLESIA, v.getIglesia().getId(), "la iglesia es la del usuario");
        assertEquals("PEREZ JUAN", v.getPersona().getNombres());
        assertEquals("M", v.getPersona().getSexo());
        assertTrue(v.getHabilitadoPadron(), "habilitada por defecto, como en la web");
        assertNotNull(v.getFechaActualiza(), "queda revisada desde su creación");
        assertEquals(v.getId(), r.getId());
        assertTrue(r.isHabilitado());
    }

    @Test
    void sePuedeAgregarSinHabilitarParaElPadron() {
        gestion.crearMiembro("PEREZ JUAN", CEDULA_NUEVA, null, false);
        assertFalse(vinculos.get(0).getHabilitadoPadron());
    }

    @Test
    void reutilizaLaPersonaActivaSinIglesiaYConservaApellidosYTratamiento() {
        Persona existente = persona(CEDULA_NUEVA, true);
        existente.setApellidos("APELLIDO-ORIGINAL");
        existente.setTratamiento("SR");
        gestion.crearMiembro("PEREZ LOPEZ JUAN", CEDULA_NUEVA, "M", true);
        assertEquals(1, personas.size(), "no duplica la persona");
        assertEquals(List.of("edita Persona", "crea IglesiaPersona"), escrituras);
        assertSame(existente, vinculos.get(0).getPersona());
        assertEquals("PEREZ LOPEZ JUAN", existente.getNombres());
        assertEquals("APELLIDO-ORIGINAL", existente.getApellidos());
        assertEquals("SR", existente.getTratamiento());
    }

    @Test
    void unaPersonaDadaDeBajaNoSeReutiliza() {
        persona(CEDULA_NUEVA, false);
        gestion.crearMiembro("PEREZ JUAN", CEDULA_NUEVA, "M", true);
        assertEquals(2, personas.size(), "la eliminada queda para que la restaure el Administrador");
        assertEquals(List.of("crea Persona", "crea IglesiaPersona"), escrituras);
    }

    @Test
    void yaEsMiembroDeEstaIglesia() {
        vinculo(persona(CEDULA_NUEVA, true), MI_IGLESIA);
        assertClave("form.personas.error.ya.miembro", () -> gestion.crearMiembro("X", CEDULA_NUEVA, "M", true));
        assertTrue(escrituras.isEmpty());
    }

    @Test
    void perteneceAOtraIglesia() {
        vinculo(persona(CEDULA_NUEVA, true), OTRA_IGLESIA);
        assertClave("form.personas.error.otra.iglesia", () -> gestion.crearMiembro("X", CEDULA_NUEVA, "M", true));
        assertTrue(escrituras.isEmpty());
    }

    @Test
    void validaLosCamposConLasReglasDeLaWeb() {
        assertClave("form.personas.error.nombres.requerido", () -> gestion.crearMiembro(" ", CEDULA_NUEVA, "M", null));
        assertClave("form.personas.error.documento.invalido",
                () -> gestion.crearMiembro("PEREZ JUAN", "1710034066", "M", null));
        assertClave("form.personas.error.documento.requerido", () -> gestion.crearMiembro("PEREZ JUAN", "", "M", null));
        assertClave("form.personas.error.sexo.invalido",
                () -> gestion.crearMiembro("PEREZ JUAN", CEDULA_NUEVA, "X", null));
        assertTrue(escrituras.isEmpty());
    }

    @Test
    void conElCronogramaCerradoNoSeAgrega() {
        cronogramaAbierto = false;
        assertClave("form.personas.error.cronograma",
                () -> gestion.crearMiembro("PEREZ JUAN", CEDULA_NUEVA, "M", null));
        assertTrue(escrituras.isEmpty());
    }

    // ------------------------------------------------------------------ Dar de baja

    @Test
    void laBajaDesactivaElVinculoYLaPersonaSinOtraIglesia() {
        Persona p = persona(CEDULA_NUEVA, true);
        IglesiaPersona v = vinculo(p, MI_IGLESIA);
        gestion.darDeBajaMiembro(v.getId());
        assertFalse(v.getEstado(), "baja lógica del vínculo");
        assertFalse(p.getEstado(), "sin otra iglesia activa, la persona también");
        assertTrue(vinculos.contains(v) && personas.contains(p), "no se borra ninguna fila");
        assertEquals(List.of("baja IglesiaPersona", "edita Persona (baja)"), escrituras);
    }

    @Test
    void unMiembroYaDadoDeBajaNoEstaDisponible() {
        IglesiaPersona v = vinculo(persona(CEDULA_NUEVA, true), MI_IGLESIA);
        v.setEstado(false);
        assertClave("form.personas.error.miembro.noDisponible", () -> gestion.darDeBajaMiembro(v.getId()));
        assertClave("form.personas.error.miembro.noDisponible", () -> gestion.darDeBajaMiembro(555));
        assertTrue(escrituras.isEmpty());
    }

    @Test
    void unMiembroDeOtraIglesiaNoSeDaDeBaja() {
        IglesiaPersona v = vinculo(persona(CEDULA_OTRA, true), OTRA_IGLESIA);
        assertClave("form.personas.error.iglesia.no.autorizada", () -> gestion.darDeBajaMiembro(v.getId()));
        assertTrue(v.getEstado());
    }

    @Test
    void conVariasIglesiasActivasLaBajaSeRechaza() {
        Persona p = persona(CEDULA_NUEVA, true);
        IglesiaPersona v = vinculo(p, MI_IGLESIA);
        vinculo(p, OTRA_IGLESIA);
        assertClave("form.personas.error.varias.iglesias", () -> gestion.darDeBajaMiembro(v.getId()));
        assertTrue(v.getEstado());
    }

    @Test
    void conElCronogramaCerradoNoSeDaDeBajaEnElServidor() {
        IglesiaPersona v = vinculo(persona(CEDULA_NUEVA, true), MI_IGLESIA);
        cronogramaAbierto = false;
        assertClave("form.personas.error.cronograma", () -> gestion.darDeBajaMiembro(v.getId()));
        assertTrue(v.getEstado());
    }

    @Test
    void soloIglesiaAdminPuedeAgregarODarDeBaja() throws Exception {
        for (String metodo : List.of("crearMiembro", "darDeBajaMiembro")) {
            var m = java.util.Arrays.stream(GestionMovilService.class.getDeclaredMethods())
                    .filter(x -> x.getName().equals(metodo)).findFirst().orElseThrow();
            assertArrayEquals(new String[] { "SITEC-IglesiaAdmin" }, m.getAnnotation(RolesAllowed.class).value(),
                    metodo);
        }
    }

    // ------------------------------------------------------------------

    private Persona activaPorCedula(String documento) {
        return personas.stream().filter(p -> documento.equals(p.getDocumento()) && Boolean.TRUE.equals(p.getEstado()))
                .findFirst().orElse(null);
    }

    private List<IglesiaPersona> activasPorCedula(String documento) {
        return vinculos.stream().filter(v -> Boolean.TRUE.equals(v.getEstado())
                && documento.equals(v.getPersona().getDocumento())).toList();
    }

    private Persona persona(String cedula, boolean activa) {
        Persona p = new Persona();
        p.setId(secuencia++);
        p.setNombres("PERSONA " + cedula);
        p.setDocumento(cedula);
        p.setEstado(activa);
        personas.add(p);
        return p;
    }

    private IglesiaPersona vinculo(Persona persona, int iglesiaId) {
        IglesiaPersona v = new IglesiaPersona(iglesia(iglesiaId), persona);
        v.setId(secuencia++);
        v.setEstado(true);
        v.setHabilitadoPadron(true);
        vinculos.add(v);
        return v;
    }

    private static Iglesia iglesia(int id) {
        Iglesia i = new Iglesia();
        i.setId(id);
        i.setNombre("IGLESIA " + id);
        return i;
    }

    private static void assertClave(String clave, org.junit.jupiter.api.function.Executable accion) {
        assertEquals(clave, assertThrows(IglesiaPersonaException.class, accion).getMessageKey());
    }

    private static void inyectar(Object destino, String nombre, Object valor) throws Exception {
        inyectarEn(destino.getClass(), destino, nombre, valor);
    }

    private static void inyectarEn(Class<?> tipo, Object destino, String nombre, Object valor) throws Exception {
        for (Class<?> c = tipo; c != null; c = c.getSuperclass()) {
            try {
                var campo = c.getDeclaredField(nombre);
                campo.setAccessible(true);
                campo.set(destino, valor);
                return;
            } catch (NoSuchFieldException e) {
                // sigue con la superclase
            }
        }
        throw new NoSuchFieldException(nombre);
    }
}
