package ec.com.antenasur.security.api;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ec.com.antenasur.dto.ConsultaMovilDTO;
import ec.com.antenasur.dto.CronogramaFaseDTO;
import ec.com.antenasur.dto.EscrutinioCabeceraDTO;
import ec.com.antenasur.dto.EscrutinioDTO;
import ec.com.antenasur.dto.FiltroMiembrosDTO;
import ec.com.antenasur.dto.IglesiaDTO;
import ec.com.antenasur.dto.IglesiaPersonaDTO;
import ec.com.antenasur.dto.MesaDTO;
import ec.com.antenasur.dto.MiembroJRVDTO;
import ec.com.antenasur.dto.PadronDTO;
import ec.com.antenasur.dto.PersonaDTO;
import ec.com.antenasur.dto.RecintoDTO;
import ec.com.antenasur.dto.ResumenMiembrosIglesiaDTO;
import ec.com.antenasur.enums.EstadoEscrutinio;
import ec.com.antenasur.facade.UsuarioFacade;
import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.model.Persona;
import ec.com.antenasur.model.Usuario;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.service.IglesiaPersonaService;
import ec.com.antenasur.service.IglesiaService;
import ec.com.antenasur.service.tec.ConsultaMovilService;
import ec.com.antenasur.service.tec.CronogramaService;
import ec.com.antenasur.service.tec.EscrutinioService;
import ec.com.antenasur.service.tec.MiembroJRVService;
import ec.com.antenasur.service.tec.PadronService;
import ec.com.antenasur.service.tec.ProcesoElectoralService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Autorización y alcance del módulo Tribunal de la API móvil: cada consulta declara solo los
 * roles acordados y el alcance (mesa, iglesia, proceso) sale del principal, nunca de la App.
 */
class ConsultaMovilServiceTest {

    private static final int PROCESO = 5;
    private static final int PERSONA = 77;
    private static final int MESA = 7;
    private static final int IGLESIA = 3;

    private Usuario usuario;
    private ConsultaMovilService servicio;
    private EstadoEscrutinio estadoMesa = EstadoEscrutinio.ABIERTO;
    private final List<String> llamadas = new ArrayList<>();
    private FiltroMiembrosDTO filtroRecibido;

    // ------------------------------------------------------------ Autorización declarada

    @Test
    void cadaConsultaDeclaraExactamenteLosRolesAcordados() {
        Map<String, Set<String>> esperado = Map.of(
                "mesaPresidente", Set.of("SITEC-Presidente-mesa"),
                "padronPresidente", Set.of("SITEC-Presidente-mesa"),
                "iglesia", Set.of("SITEC-IglesiaAdmin"),
                "miembros", Set.of("SITEC-IglesiaAdmin"),
                "resumenProceso", Set.of("SITEC-Administrador", "SITEC-Tribunal"),
                "avanceMesas", Set.of("SITEC-Administrador", "SITEC-Tribunal"),
                "resultadosProceso", Set.of("SITEC-Administrador", "SITEC-Tribunal"));
        List<Method> publicos = Arrays.stream(ConsultaMovilService.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()) && !m.isSynthetic()).toList();
        assertEquals(esperado.keySet(), publicos.stream().map(Method::getName).collect(Collectors.toSet()),
                "toda consulta nueva debe declarar sus roles en esta prueba");
        for (Method m : publicos) {
            RolesAllowed roles = m.getAnnotation(RolesAllowed.class);
            assertNotNull(roles, m.getName() + " sin @RolesAllowed");
            assertEquals(esperado.get(m.getName()), Set.of(roles.value()), m.getName());
            assertNull(m.getAnnotation(PermitAll.class), m.getName());
        }
        assertNull(ConsultaMovilService.class.getAnnotation(PermitAll.class));
        assertNull(ConsultaMovilService.class.getAnnotation(RolesAllowed.class),
                "sin roles de clase: un método nuevo sin anotación quedaría con permisos amplios");
    }

    @Test
    void ningunaConsultaAutorizaRolesSinFuncionesEnV1() {
        Set<String> sinV1 = Set.of("SITEC-Tecnico", "SITEC-Gerencial", "SITEC-Supervisor", "SITEC-Superadministrador");
        Set<String> concedidos = new TreeSet<>();
        for (Method m : ConsultaMovilService.class.getDeclaredMethods()) {
            RolesAllowed roles = m.getAnnotation(RolesAllowed.class);
            if (roles != null) {
                concedidos.addAll(List.of(roles.value()));
            }
        }
        concedidos.retainAll(sinV1);
        assertTrue(concedidos.isEmpty(), "roles sin funciones en V1: " + concedidos);
    }

    @Test
    void losRecursosRestNoUsanPermitAll() {
        for (Class<?> recurso : List.of(ec.com.antenasur.api.AutenticacionRecurso.class,
                ec.com.antenasur.api.TribunalRecurso.class)) {
            assertNull(recurso.getAnnotation(PermitAll.class), recurso.getSimpleName());
            for (Method m : recurso.getDeclaredMethods()) {
                assertNull(m.getAnnotation(PermitAll.class), recurso.getSimpleName() + "." + m.getName());
            }
        }
    }

    // ------------------------------------------------------------ Alcance por principal

    @BeforeEach
    void preparar() throws Exception {
        usuario = new Usuario();
        usuario.setUsername("presidente");
        Persona persona = new Persona();
        persona.setId(PERSONA);
        usuario.setPersonsa(persona);
        Iglesia iglesia = new Iglesia();
        iglesia.setId(IGLESIA);
        usuario.setIglesia(iglesia);

        servicio = new ConsultaMovilService();
        inyectar("sessionContext", contexto("presidente"));
        inyectar("usuarioFacade", new UsuarioFacade() {
            @Override
            public Usuario findByUsuarioName(String nombre) {
                llamadas.add("usuario:" + nombre);
                return usuario.getUsername().equals(nombre) ? usuario : null;
            }
        });
        inyectar("procesoElectoralService", new ProcesoElectoralService() {
            @Override
            public ProcesoElectoral getActivo() {
                ProcesoElectoral p = new ProcesoElectoral();
                p.setId(PROCESO);
                p.setNombre("Elecciones 2026");
                return p;
            }
        });
        inyectar("cronogramaService", new CronogramaService() {
            @Override
            public CronogramaFaseDTO getFaseVigenteDelProcesoActivo() {
                return null;
            }

            @Override
            public boolean permiteEdicionPadron() {
                return true;
            }
        });
        inyectar("miembroJRVService", new MiembroJRVService() {
            @Override
            public MiembroJRVDTO obtenerDesignacionPresidentePorPersonaProceso(Integer personaId, Integer procesoId) {
                llamadas.add("designacion:" + personaId + ":" + procesoId);
                if (personaId != PERSONA) {
                    return null;
                }
                MiembroJRVDTO d = new MiembroJRVDTO();
                d.setMesa(mesa());
                d.setCargoNombre("PRESIDENTE");
                return d;
            }

            @Override
            public List<MiembroJRVDTO> listarDTOsPorMesaProceso(Integer mesaId, Integer procesoId) {
                llamadas.add("junta:" + mesaId + ":" + procesoId);
                MiembroJRVDTO m = new MiembroJRVDTO();
                m.setCargoNombre("PRESIDENTE");
                IglesiaPersonaDTO ip = new IglesiaPersonaDTO();
                PersonaDTO p = new PersonaDTO();
                p.setNombres("Ana");
                p.setApellidos("Pérez");
                ip.setPersona(p);
                m.setIglesiaPersona(ip);
                return List.of(m);
            }
        });
        inyectar("escrutinioService", new EscrutinioService() {
            @Override
            public EscrutinioCabeceraDTO buscarCabeceraDTO(Integer mesaId, Integer procesoId) {
                llamadas.add("cabecera:" + mesaId + ":" + procesoId);
                EscrutinioCabeceraDTO c = new EscrutinioCabeceraDTO();
                c.setEstadoEscrutinio(estadoMesa);
                c.setTotalSufragantes(3);
                c.setTotalVotosRegistrados(3);
                return c;
            }

            @Override
            public List<EscrutinioDTO> listarDTOsPorMesaYProceso(Integer mesaId, Integer procesoId) {
                llamadas.add("votos:" + mesaId + ":" + procesoId);
                EscrutinioDTO lista = new EscrutinioDTO();
                lista.setCategoriaNombre("LISTA 1");
                lista.setTotalVotos(2);
                EscrutinioDTO papeletas = new EscrutinioDTO();
                papeletas.setCategoriaNombre("PAPELETAS NO UTILIZADAS");
                papeletas.setTotalVotos(9);
                return List.of(lista, papeletas);
            }
        });
        inyectar("padronService", new PadronService() {
            @Override
            public int contarSufragantesPorMesaYProceso(Integer mesaId, Integer procesoId) {
                llamadas.add("contar:" + mesaId + ":" + procesoId);
                return 3;
            }

            @Override
            public List<PadronDTO> listarDTOsPorMesaIdsYProceso(List<Integer> mesaIds, Integer procesoId) {
                llamadas.add("padron:" + mesaIds + ":" + procesoId);
                return List.of(elector("Zoila", "Andrade"), elector("Ana", "Bravo"));
            }
        });
        inyectar("iglesiaService", new IglesiaService() {
            @Override
            public IglesiaDTO obtenerDTOPorId(Integer id) {
                llamadas.add("iglesia:" + id);
                IglesiaDTO i = new IglesiaDTO();
                i.setId(id);
                i.setNombre("Iglesia Central");
                return i;
            }
        });
        inyectar("iglesiaPersonaService", new IglesiaPersonaService() {
            @Override
            public ResumenMiembrosIglesiaDTO obtenerResumenMiembrosActivosPorIglesia(Integer iglesiaId) {
                llamadas.add("resumen:" + iglesiaId);
                return new ResumenMiembrosIglesiaDTO(10, 8, 2, 4, 1);
            }

            @Override
            public long contarMiembros(FiltroMiembrosDTO filtro) {
                filtroRecibido = filtro;
                return 1;
            }

            @Override
            public List<IglesiaPersonaDTO> listarMiembros(FiltroMiembrosDTO filtro, int primero, int maximo,
                    String campoOrden, boolean descendente) {
                llamadas.add("miembros:" + primero + ":" + maximo + ":" + campoOrden);
                IglesiaPersonaDTO ip = new IglesiaPersonaDTO();
                PersonaDTO p = new PersonaDTO();
                p.setNombres("Rosa");
                p.setApellidos("Cruz");
                p.setDocumento("0600000000");
                ip.setPersona(p);
                ip.setHabilitadoPadron(true);
                return List.of(ip);
            }
        });
    }

    @Test
    void laMesaDelPresidenteSaleDeSuDesignacionEnElProcesoActivo() {
        ConsultaMovilDTO.MesaPresidente m = servicio.mesaPresidente();
        assertNotNull(m);
        assertEquals(MESA, m.getUbicacion().getMesaId());
        assertTrue(llamadas.contains("usuario:presidente"));
        assertTrue(llamadas.contains("designacion:" + PERSONA + ":" + PROCESO));
        assertTrue(llamadas.contains("cabecera:" + MESA + ":" + PROCESO), "siempre filtrado por proce_id");
        assertTrue(llamadas.contains("junta:" + MESA + ":" + PROCESO));
        assertEquals("Pérez Ana", m.getJunta().get(0).getNombre());
        assertEquals("ABIERTO", m.getEstado());
        assertNull(m.getResultados(), "sin resultados mientras la mesa no esté cerrada");
        assertFalse(llamadas.stream().anyMatch(l -> l.startsWith("votos:")), "ni siquiera se consultan");
    }

    @Test
    void conLaMesaCerradaEntregaResultadosSinPapeletas() {
        estadoMesa = EstadoEscrutinio.CERRADO;
        ConsultaMovilDTO.ResultadosMesa r = servicio.mesaPresidente().getResultados();
        assertNotNull(r);
        assertEquals(List.of("LISTA 1"), r.getCategorias().stream().map(ConsultaMovilDTO.VotosCategoria::getCategoria).toList());
    }

    @Test
    void sinDesignacionComoPresidenteNoHayMesa() throws Exception {
        usuario.getPersonsa().setId(999);
        assertNull(servicio.mesaPresidente());
        assertNull(servicio.padronPresidente());
        assertFalse(llamadas.stream().anyMatch(l -> l.startsWith("cabecera:") || l.startsWith("padron:")));
    }

    @Test
    void otroPrincipalNoObtieneLaMesaAjena() throws Exception {
        inyectar("sessionContext", contexto("intruso"));
        assertNull(servicio.mesaPresidente());
        assertNull(servicio.padronPresidente());
    }

    @Test
    void elPadronEsElDeSuMesaYProcesoOrdenadoYSinCedulas() {
        List<ConsultaMovilDTO.Elector> padron = servicio.padronPresidente();
        assertTrue(llamadas.contains("padron:[" + MESA + "]:" + PROCESO));
        assertEquals(List.of("Andrade Zoila", "Bravo Ana"), padron.stream().map(ConsultaMovilDTO.Elector::getNombre).toList());
    }

    @Test
    void laIglesiaSaleDelUsuarioYNoDeLaPeticion() {
        ConsultaMovilDTO.Iglesia i = servicio.iglesia();
        assertEquals(IGLESIA, i.getId());
        assertTrue(llamadas.contains("iglesia:" + IGLESIA));
        assertTrue(llamadas.contains("resumen:" + IGLESIA));
        assertEquals(6, i.getMiembrosHabilitados());
        assertEquals(4, i.getMiembrosNoHabilitados());
        assertTrue(i.isPermiteEdicion(), "la App sabe si el cronograma permite editar");
    }

    @Test
    void sinIglesiaAsignadaNoHayDatos() {
        usuario.setIglesia(null);
        assertNull(servicio.iglesia());
        assertEquals(0, servicio.miembros(null, null, 0, 30).getTotal());
        assertNull(filtroRecibido, "no consulta miembros sin iglesia");
    }

    @Test
    void losMiembrosSeFiltranPorSuIglesiaConPaginaAcotadaYSinCedula() {
        ConsultaMovilDTO.Pagina<ConsultaMovilDTO.Miembro> p = servicio.miembros("  rosa ", true, 2, 1_000);
        assertEquals(IGLESIA, filtroRecibido.getIglesiaId());
        assertNull(filtroRecibido.getProvinciaId());
        assertEquals("rosa", filtroRecibido.getBusqueda());
        assertEquals(Boolean.TRUE, filtroRecibido.getHabilitado());
        assertEquals(ConsultaMovilService.TAMANO_MAXIMO, p.getTamano());
        assertTrue(llamadas.contains("miembros:" + 2 * ConsultaMovilService.TAMANO_MAXIMO + ":"
                + ConsultaMovilService.TAMANO_MAXIMO + ":persona.nombres"));
        assertEquals("Cruz Rosa", p.getElementos().get(0).getNombre());
        assertTrue(Arrays.stream(ConsultaMovilDTO.Miembro.class.getDeclaredFields())
                .noneMatch(f -> f.getName().toLowerCase().contains("documento") || f.getName().toLowerCase().contains("cedula")));
    }

    @Test
    void paginaNegativaSeNormaliza() {
        ConsultaMovilDTO.Pagina<ConsultaMovilDTO.Miembro> p = servicio.miembros(null, null, -3, 0);
        assertEquals(0, p.getPagina());
        assertEquals(1, p.getTamano());
    }

    // ------------------------------------------------------------

    private static MesaDTO mesa() {
        MesaDTO m = new MesaDTO();
        m.setId(MESA);
        m.setNombre("MESA 7");
        RecintoDTO r = new RecintoDTO();
        r.setNombre("Escuela Central");
        m.setRecinto(r);
        return m;
    }

    private static PadronDTO elector(String nombres, String apellidos) {
        PersonaDTO p = new PersonaDTO();
        p.setNombres(nombres);
        p.setApellidos(apellidos);
        p.setDocumento("0600000000");
        IglesiaPersonaDTO ip = new IglesiaPersonaDTO();
        ip.setPersona(p);
        PadronDTO padron = new PadronDTO();
        padron.setIglesiaPersona(ip);
        return padron;
    }

    private SessionContext contexto(String principal) {
        return (SessionContext) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { SessionContext.class },
                (p, m, a) -> switch (m.getName()) {
                    case "getCallerPrincipal" -> (Principal) () -> principal;
                    default -> throw new UnsupportedOperationException(m.getName());
                });
    }

    private void inyectar(String nombre, Object valor) throws Exception {
        var campo = ConsultaMovilService.class.getDeclaredField(nombre);
        campo.setAccessible(true);
        campo.set(servicio, valor);
    }
}
