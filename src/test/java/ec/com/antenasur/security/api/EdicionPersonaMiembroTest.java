package ec.com.antenasur.security.api;

import java.lang.reflect.Proxy;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.SessionContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ec.com.antenasur.exception.IglesiaPersonaException;
import ec.com.antenasur.facade.IglesiaFacade;
import ec.com.antenasur.facade.IglesiaPersonaFacade;
import ec.com.antenasur.facade.PersonaFacade;
import ec.com.antenasur.facade.UsuarioFacade;
import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.Persona;
import ec.com.antenasur.model.Usuario;
import ec.com.antenasur.service.IglesiaPersonaService;
import ec.com.antenasur.service.ValidadorPersona;
import ec.com.antenasur.service.tec.CronogramaService;
import ec.com.antenasur.service.tec.GestionMovilService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edición de la persona de un miembro desde la App: reutiliza las reglas de la web
 * (guardarDesdeDTO y ValidadorPersona), solo cambia los campos permitidos, respeta el
 * alcance del IglesiaAdmin y no guarda cuando no hay cambios.
 */
class EdicionPersonaMiembroTest {

    private static final int MI_IGLESIA = 3;
    /** Cédula con dígito verificador válido (1710034065). */
    private static final String CEDULA_VALIDA = "1710034065";

    private IglesiaPersona miembro;
    private boolean cronogramaAbierto = true;
    private boolean cedulaDeOtraPersona;
    private final List<String> guardados = new ArrayList<>();
    private final IglesiaPersonaService servicio = new IglesiaPersonaService();
    private final GestionMovilService gestion = new GestionMovilService();

    @BeforeEach
    void preparar() throws Exception {
        miembro = miembro(10, MI_IGLESIA);
        CronogramaService cronograma = new CronogramaService() {
            @Override
            public boolean permiteEdicionPadron() {
                return cronogramaAbierto;
            }
        };
        inyectar(servicio, "cronogramaService", cronograma);
        inyectar(servicio, "iglesiaPersonaFacade", new IglesiaPersonaFacade() {
            @Override
            public IglesiaPersona find(Integer id) {
                return miembro.getId().equals(id) ? miembro : null;
            }

            @Override
            public IglesiaPersona edit(IglesiaPersona entidad) {
                guardados.add("IglesiaPersona");
                return entidad;
            }

            @Override
            public List<IglesiaPersona> listarRelacionesActivasPorDocumento(String documento, boolean bloquear) {
                return List.of(miembro);
            }
        });
        inyectar(servicio, "personaFacade", new PersonaFacade() {
            @Override
            public Persona edit(Persona entidad) {
                guardados.add("Persona");
                return entidad;
            }

            @Override
            public boolean existeOtraPersonaActivaConDocumento(String documento, Integer personaId) {
                return cedulaDeOtraPersona;
            }
        });
        inyectar(servicio, "iglesiaFacade", new IglesiaFacade() {
            @Override
            public Iglesia find(Integer id) {
                return miembro.getIglesia().getId().equals(id) ? miembro.getIglesia() : null;
            }
        });
        inyectar(servicio, "usuarioFacade", new UsuarioFacade() {
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
        inyectar(servicio, "sessionContext", (SessionContext) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { SessionContext.class }, (p, m, a) -> switch (m.getName()) {
                    case "isCallerInRole" -> "SITEC-IglesiaAdmin".equals(a[0]);
                    case "getCallerPrincipal" -> (Principal) () -> "admin.iglesia";
                    default -> throw new UnsupportedOperationException(m.getName());
                }));
        inyectar(gestion, "iglesiaPersonaService", servicio);
        inyectar(gestion, "cronogramaService", cronograma);
    }

    @Test
    void consultaElDetalleDeUnMiembroDeSuIglesia() {
        var dto = servicio.obtenerMiembroParaEdicion(10);
        assertEquals("PEREZ JUAN", dto.getPersona().getNombres());
        assertEquals(CEDULA_VALIDA, dto.getPersona().getDocumento());
        assertTrue(guardados.isEmpty(), "consultar no modifica nada");
    }

    @Test
    void guardarSinCambiosNoEscribeNada() {
        var r = gestion.actualizarPersonaMiembro(10, " PEREZ JUAN ", CEDULA_VALIDA, "m", null);
        assertTrue(guardados.isEmpty(), "sin revisión ni actividad ficticia");
        assertEquals("PEREZ JUAN", r.getNombres());
        assertTrue(r.isPermiteEdicion());
    }

    @Test
    void guardarConCambiosActualizaSoloLosCamposPermitidos() {
        miembro.setHabilitadoPadron(null); // valor heredado: no debe convertirse en false
        miembro.getPersona().setApellidos("APELLIDO-ORIGINAL");
        miembro.getPersona().setTratamiento("SR");
        var r = gestion.actualizarPersonaMiembro(10, "PEREZ LOPEZ JUAN", CEDULA_VALIDA, "M", null);
        assertEquals(List.of("Persona", "IglesiaPersona"), guardados);
        Persona p = miembro.getPersona();
        assertEquals("PEREZ LOPEZ JUAN", p.getNombres());
        assertEquals("APELLIDO-ORIGINAL", p.getApellidos(), "campos no editables se conservan");
        assertEquals("SR", p.getTratamiento());
        assertEquals(MI_IGLESIA, miembro.getIglesia().getId(), "la iglesia no cambia");
        // Con habilitación nula guardarDesdeDTO aplica su valor por defecto (habilitado), nunca false.
        assertEquals(Boolean.TRUE, miembro.getHabilitadoPadron(), "editar la persona no deshabilita al miembro");
        assertNotNull(miembro.getFechaActualiza(), "queda revisado, igual que al editar en la web");
        assertEquals("PEREZ LOPEZ JUAN", r.getNombres());
    }

    @Test
    void soloLaHabilitacionNoModificaNiAuditaPersona() {
        var r = gestion.actualizarPersonaMiembro(10, "PEREZ JUAN", CEDULA_VALIDA, "M", false);
        assertEquals(List.of("IglesiaPersona"), guardados, "solo el vínculo iglesia-persona");
        assertFalse(miembro.getHabilitadoPadron());
        assertEquals("PEREZ JUAN", miembro.getPersona().getNombres());
        assertFalse(r.isHabilitado());
        assertTrue(r.isRevisado(), "queda revisado, igual que el interruptor");
    }

    @Test
    void datosYHabilitacionSeGuardanJuntosComoEnLaWeb() {
        var r = gestion.actualizarPersonaMiembro(10, "PEREZ LOPEZ JUAN", CEDULA_VALIDA, "M", false);
        assertEquals(List.of("Persona", "IglesiaPersona"), guardados, "una sola llamada a guardarDesdeDTO");
        assertEquals("PEREZ LOPEZ JUAN", miembro.getPersona().getNombres());
        assertFalse(miembro.getHabilitadoPadron());
        assertFalse(r.isHabilitado());
    }

    @Test
    void laMismaHabilitacionNoCuentaComoCambio() {
        gestion.actualizarPersonaMiembro(10, "PEREZ JUAN", CEDULA_VALIDA, "M", true);
        assertTrue(guardados.isEmpty());
    }

    @Test
    void conElCronogramaCerradoTampocoCambiaLaHabilitacion() {
        cronogramaAbierto = false;
        assertClave("form.personas.error.cronograma",
                () -> gestion.actualizarPersonaMiembro(10, "PEREZ JUAN", CEDULA_VALIDA, "M", false));
        assertTrue(miembro.getHabilitadoPadron());
        assertTrue(guardados.isEmpty());
    }

    @Test
    void validaLosCamposConLasReglasDeLaWeb() {
        assertClave("form.personas.error.nombres.requerido",
                () -> gestion.actualizarPersonaMiembro(10, "   ", CEDULA_VALIDA, "M", null));
        assertClave("form.personas.error.nombres.largo",
                () -> gestion.actualizarPersonaMiembro(10, "A".repeat(101), CEDULA_VALIDA, "M", null));
        assertClave("form.personas.error.sexo.invalido",
                () -> gestion.actualizarPersonaMiembro(10, "PEREZ JUAN", CEDULA_VALIDA, "X", null));
        assertClave("form.personas.error.documento.invalido",
                () -> gestion.actualizarPersonaMiembro(10, "PEREZ JUAN", "1710034066", "M", null));
        assertClave("form.personas.error.documento.requerido",
                () -> gestion.actualizarPersonaMiembro(10, "PEREZ JUAN", "", "M", null));
        assertTrue(guardados.isEmpty());
    }

    @Test
    void unaCedulaDeOtraPersonaSeRechaza() {
        cedulaDeOtraPersona = true;
        assertClave("form.personas.error.documento.duplicado",
                () -> gestion.actualizarPersonaMiembro(10, "PEREZ JUAN", "1710034057", "M", null));
        assertFalse(guardados.contains("Persona"));
    }

    @Test
    void unMiembroDeOtraIglesiaNoSeConsultaNiSeEdita() {
        miembro = miembro(10, 99);
        assertClave("form.personas.error.iglesia.no.autorizada", () -> servicio.obtenerMiembroParaEdicion(10));
        assertClave("form.personas.error.iglesia.no.autorizada",
                () -> gestion.actualizarPersonaMiembro(10, "OTRO NOMBRE", CEDULA_VALIDA, "M", null));
        assertTrue(guardados.isEmpty());
    }

    @Test
    void unIdInexistenteOInactivoSeRechaza() {
        assertClave("form.personas.error.miembro.noDisponible", () -> servicio.obtenerMiembroParaEdicion(55));
        miembro.setEstado(false);
        assertClave("form.personas.error.miembro.noDisponible", () -> servicio.obtenerMiembroParaEdicion(10));
    }

    @Test
    void conElCronogramaCerradoNoSeGuarda() {
        cronogramaAbierto = false;
        assertClave("form.personas.error.cronograma",
                () -> gestion.actualizarPersonaMiembro(10, "PEREZ LOPEZ JUAN", CEDULA_VALIDA, "M", null));
        assertTrue(guardados.isEmpty());
    }

    @Test
    void elValidadorCompartidoAceptaUnaCedulaValida() {
        assertDoesNotThrow(() -> ValidadorPersona.validarCedula(CEDULA_VALIDA));
        assertDoesNotThrow(() -> ValidadorPersona.validarNombres("A".repeat(100)));
        assertDoesNotThrow(() -> ValidadorPersona.validarSexo(null));
    }

    private static void assertClave(String clave, org.junit.jupiter.api.function.Executable accion) {
        assertEquals(clave, assertThrows(IglesiaPersonaException.class, accion).getMessageKey());
    }

    private static IglesiaPersona miembro(int id, int iglesiaId) {
        Iglesia iglesia = new Iglesia();
        iglesia.setId(iglesiaId);
        Persona persona = new Persona();
        persona.setId(77);
        persona.setNombres("PEREZ JUAN");
        persona.setDocumento(CEDULA_VALIDA);
        persona.setSexo("M");
        persona.setEstado(true);
        IglesiaPersona ip = new IglesiaPersona(iglesia, persona);
        ip.setId(id);
        ip.setEstado(true);
        ip.setHabilitadoPadron(true);
        return ip;
    }

    private static void inyectar(Object destino, String nombre, Object valor) throws Exception {
        var campo = destino.getClass().getDeclaredField(nombre);
        campo.setAccessible(true);
        campo.set(destino, valor);
    }
}
