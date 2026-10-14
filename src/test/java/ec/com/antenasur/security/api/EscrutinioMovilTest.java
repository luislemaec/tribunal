package ec.com.antenasur.security.api;

import java.lang.reflect.Proxy;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ejb.SessionContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ec.com.antenasur.dto.ConsultaMovilDTO.ActaPresidente;
import ec.com.antenasur.dto.EscrutinioCabeceraDTO;
import ec.com.antenasur.dto.EscrutinioDTO;
import ec.com.antenasur.dto.MesaDTO;
import ec.com.antenasur.enums.EstadoEscrutinio;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.model.tec.CategoriaVoto;
import ec.com.antenasur.model.tec.Documentos;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.service.tec.AccesoDocumentoMesaService;
import ec.com.antenasur.service.tec.ActaFisicaEscrutinioService;
import ec.com.antenasur.service.tec.ReglasFlujoEscrutinio;
import ec.com.antenasur.service.tec.CategoriaVotoService;
import ec.com.antenasur.service.tec.DisponibilidadDocumentoMesaService;
import ec.com.antenasur.service.tec.EscrutinioService;
import ec.com.antenasur.service.tec.GestionMovilService;
import ec.com.antenasur.service.tec.MesaService;
import ec.com.antenasur.service.tec.PadronService;
import ec.com.antenasur.service.tec.ProcesoElectoralService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Escrutinio del Presidente desde la App: la mesa sale de su designación, el acta debe
 * traer exactamente las categorías de la mesa y las transiciones las decide EscrutinioService.
 */
class EscrutinioMovilTest {

    private static final int PROCESO = 5;
    private static final int MESA = 7;
    private static final int LISTA = 1;
    private static final int BLANCOS = 2;
    private static final int PAPELETAS = 3;

    private GestionMovilService servicio;
    private EstadoEscrutinio estado = EstadoEscrutinio.PENDIENTE;
    private String bloqueo;
    private Integer mesaPermitida = MESA;
    private final Map<Integer, Integer> guardados = new HashMap<>();
    private final List<String> llamadas = new ArrayList<>();
    private Documentos actaFisica;

    @BeforeEach
    void preparar() throws Exception {
        servicio = new GestionMovilService();
        inyectar("sessionContext", Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { SessionContext.class },
                (p, m, a) -> switch (m.getName()) {
                    case "getCallerPrincipal" -> (Principal) () -> "presidente";
                    default -> throw new UnsupportedOperationException(m.getName());
                }));
        inyectar("procesoElectoralService", new ProcesoElectoralService() {
            @Override
            public ProcesoElectoral getActivo() {
                ProcesoElectoral p = new ProcesoElectoral();
                p.setId(PROCESO);
                return p;
            }
        });
        inyectar("accesoDocumental", new AccesoDocumentoMesaService() {
            @Override
            public Integer mesaPermitida(Integer procesoId) {
                assertEquals(PROCESO, procesoId);
                if (mesaPermitida == null) {
                    throw new NegocioException("No autorizado para esta mesa.");
                }
                return mesaPermitida;
            }
        });
        inyectar("mesaService", new MesaService() {
            @Override
            public MesaDTO obtenerDTOPorId(Integer id) {
                MesaDTO m = new MesaDTO();
                m.setId(id);
                m.setNombre("MESA " + id);
                return m;
            }
        });
        inyectar("categoriaVotoService", new CategoriaVotoService() {
            @Override
            public List<CategoriaVoto> getCategoriasOrdenados(Integer procesoId) {
                return List.of(categoria(LISTA), categoria(BLANCOS), categoria(PAPELETAS));
            }
        });
        inyectar("padronService", new PadronService() {
            @Override
            public int contarSufragantesPorMesaYProceso(Integer mesaId, Integer procesoId) {
                return 10;
            }
        });
        inyectar("disponibilidadDocumental", new DisponibilidadDocumentoMesaService() {
            @Override
            public String motivoBloqueoApertura(Integer proceso, Integer mesa) {
                return bloqueo;
            }
        });
        inyectar("actaFisicaService", new ActaFisicaEscrutinioService() {
            @Override
            public Documentos obtenerVigente(Integer mesaId, Integer procesoId) {
                return actaFisica;
            }

            @Override
            public Documentos cargar(Integer mesaId, Integer procesoId, String nombre, String mime, byte[] contenido) {
                llamadas.add("acta:" + mesaId + ":" + procesoId + ":" + mime + ":" + contenido.length);
                // Nueva versión vigente, pendiente de revisión (ActaFisicaEscrutinioService.cargar).
                actaFisica = new Documentos();
                actaFisica.setEstadoRevision(ActaFisicaEscrutinioService.PENDIENTE_REVISION);
                return actaFisica;
            }
        });
        inyectar("escrutinioService", new EscrutinioService() {
            @Override
            public EscrutinioCabeceraDTO buscarCabeceraDTO(Integer mesaId, Integer procesoId) {
                assertEquals(MESA, mesaId);
                assertEquals(PROCESO, procesoId);
                if (estado == EstadoEscrutinio.PENDIENTE) {
                    return null;
                }
                EscrutinioCabeceraDTO c = new EscrutinioCabeceraDTO();
                c.setEstadoEscrutinio(estado);
                c.setTotalSufragantes(10);
                return c;
            }

            @Override
            public List<EscrutinioDTO> prepararActaPorMesaDTO(Integer mesaId, Integer procesoId, List<Integer> categoriaIds) {
                assertEquals(List.of(LISTA, BLANCOS, PAPELETAS), categoriaIds);
                List<EscrutinioDTO> items = new ArrayList<>();
                for (Integer id : categoriaIds) {
                    EscrutinioDTO e = new EscrutinioDTO();
                    e.setCategoriaId(id);
                    e.setCategoriaNombre(nombre(id));
                    e.setProcesoId(procesoId);
                    e.setTotalVotos(guardados.get(id));
                    items.add(e);
                }
                return items;
            }

            @Override
            public EscrutinioCabeceraDTO abrirEscrutinioDTO(Integer mesaId, Integer procesoId, String presidente,
                    String observacion, Integer totalSufragantes) {
                llamadas.add("abrir:" + mesaId + ":" + presidente + ":" + observacion + ":" + totalSufragantes);
                estado = EstadoEscrutinio.ABIERTO;
                return null;
            }

            @Override
            public EscrutinioCabeceraDTO guardarBorradorConteoDTO(Integer mesaId, Integer procesoId, List<EscrutinioDTO> items,
                    Integer totalSufragantes) {
                llamadas.add("borrador:" + mesaId);
                items.forEach(i -> guardados.put(i.getCategoriaId(), i.getTotalVotos()));
                estado = EstadoEscrutinio.CONTEO_REGISTRADO;
                return null;
            }

            @Override
            public MesaDTO guardarActaCompletaDTO(Integer mesaId, List<EscrutinioDTO> items) {
                llamadas.add("cierre:" + mesaId);
                if (estado == EstadoEscrutinio.ABIERTO) {
                    throw new NegocioException("Debe guardar el conteo antes de cerrar la mesa.");
                }
                // Misma regla que EscrutinioService.guardarActaCompleta: acta física antes del cierre.
                if (!ReglasFlujoEscrutinio.actaValidaParaCierre(actaFisica != null,
                        actaFisica != null ? actaFisica.getEstadoRevision() : null)) {
                    throw new NegocioException("Antes de cerrar la mesa cargue el acta física firmada.");
                }
                items.forEach(i -> guardados.put(i.getCategoriaId(), i.getTotalVotos()));
                estado = EstadoEscrutinio.CERRADO;
                return null;
            }
        });
    }

    @Test
    void laMesaSaleDeLaDesignacionYSinElla_seRechaza() {
        assertEquals(MESA, servicio.actaPresidente().getUbicacion().getMesaId());
        mesaPermitida = null;
        assertThrows(NegocioException.class, () -> servicio.actaPresidente());
        assertThrows(NegocioException.class, () -> servicio.abrirMesa(null));
        assertTrue(llamadas.isEmpty());
    }

    @Test
    void mesaPendiente_soloSePuedeAbrirSiNoHayBloqueo() {
        ActaPresidente a = servicio.actaPresidente();
        assertEquals("PENDIENTE", a.getEstado());
        assertTrue(a.isPuedeAbrir());
        assertFalse(a.isPuedeEditarConteo());
        bloqueo = "La junta está incompleta.";
        a = servicio.actaPresidente();
        assertFalse(a.isPuedeAbrir());
        assertEquals("La junta está incompleta.", a.getMotivoBloqueoApertura());
    }

    @Test
    void abrirRegistraAlPresidenteYLosSufragantesDelPadron() {
        ActaPresidente a = servicio.abrirMesa("  Inicio normal  ");
        assertEquals(List.of("abrir:" + MESA + ":presidente:Inicio normal:10"), llamadas);
        assertEquals("ABIERTO", a.getEstado());
        assertTrue(a.isPuedeEditarConteo());
        assertFalse(a.isPuedeCerrar(), "primero debe guardarse el conteo");
    }

    @Test
    void noSeRegistraConteoSinApertura() {
        assertThrows(NegocioException.class, () -> servicio.guardarConteo(conteo(5, 3, 2)));
        assertFalse(llamadas.contains("borrador:" + MESA));
    }

    @Test
    void elConteoDebeTraerExactamenteLasCategoriasDelActa() {
        estado = EstadoEscrutinio.ABIERTO;
        Map<Integer, Integer> faltante = new HashMap<>(conteo(5, 3, 2));
        faltante.remove(BLANCOS);
        assertTrue(assertThrows(NegocioException.class, () -> servicio.guardarConteo(faltante)).getMessage()
                .contains("BLANCOS"));
        Map<Integer, Integer> ajena = new HashMap<>(conteo(5, 3, 2));
        ajena.put(99, 1);
        assertThrows(NegocioException.class, () -> servicio.guardarConteo(ajena));
        assertThrows(NegocioException.class, () -> servicio.guardarConteo(conteo(-1, 3, 2)));
        assertThrows(NegocioException.class, () -> servicio.guardarConteo(conteo(100_001, 0, 0)));
        assertTrue(llamadas.isEmpty(), "nada llega a EscrutinioService");
    }

    @Test
    void guardarConteoCalculaElCuadreSinContarPapeletasComoVotos() {
        estado = EstadoEscrutinio.ABIERTO;
        ActaPresidente a = servicio.guardarConteo(conteo(5, 3, 2));
        assertEquals(8, a.getVotosEmitidos());
        assertEquals(2, a.getPapeletasNoUtilizadas());
        assertEquals(0, a.getCuadre());
        // Orden oficial: con el conteo guardado sigue el acta física; aún no se puede cerrar.
        assertTrue(a.isPuedeSubirActaFisica());
        assertFalse(a.isPuedeCerrar(), "falta el acta física");
        assertEquals("ACTA_FISICA", a.getFase().getFase());
        assertEquals(3, a.getFase().getNumero());
        assertTrue(a.getCategorias().stream().filter(c -> c.getCategoriaId() == PAPELETAS).findFirst().orElseThrow()
                .isPapeletas());
    }

    @Test
    void ordenOficial_aperturaConteoActaFisicaCierreValidacion() {
        estado = EstadoEscrutinio.ABIERTO;
        assertEquals("CONTEO", servicio.actaPresidente().getFase().getFase());
        assertFalse(servicio.actaPresidente().isPuedeSubirActaFisica(), "sin conteo guardado no hay acta");
        assertThrows(NegocioException.class, () -> servicio.cerrarMesa(conteo(5, 3, 2)), "conteo sin guardar");
        servicio.guardarConteo(conteo(5, 3, 2));
        assertThrows(NegocioException.class, () -> servicio.cerrarMesa(conteo(5, 3, 2)), "acta física sin cargar");
        assertFalse(llamadas.contains("cierre-ok"));

        ActaPresidente a = servicio.subirActaFisica(new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF });
        assertTrue(llamadas.contains("acta:" + MESA + ":" + PROCESO + ":image/jpeg:3"));
        assertEquals("CIERRE", a.getFase().getFase());
        assertTrue(a.isPuedeCerrar());
        assertTrue(a.isPuedeEditarConteo(), "el conteo sigue editable; el Tribunal contrasta al validar");

        a = servicio.cerrarMesa(conteo(5, 3, 2));
        assertEquals("CERRADO", a.getEstado());
        assertEquals("DATO_OFICIAL", a.getFase().getFase(), "fase Validación");
        assertFalse(a.isPuedeEditarConteo());
        assertFalse(a.isPuedeCerrar());
    }

    @Test
    void trasDevolverElActa_seExigeUnaNuevaVersionParaCerrar() {
        estado = EstadoEscrutinio.REABIERTO;
        actaFisica = new Documentos();
        actaFisica.setEstadoRevision(ActaFisicaEscrutinioService.OBSERVADA);
        ActaPresidente a = servicio.actaPresidente();
        assertEquals("CONTEO", a.getFase().getFase());
        assertEquals("REABIERTO", a.getFase().getIncidencia());
        assertTrue(a.isPuedeEditarConteo());
        assertTrue(a.isPuedeSubirActaFisica());
        assertFalse(a.isPuedeCerrar(), "el acta devuelta no sirve para cerrar");
        assertThrows(NegocioException.class, () -> servicio.cerrarMesa(conteo(5, 3, 2)));
        a = servicio.subirActaFisica(new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF });
        assertTrue(a.isPuedeCerrar(), "con la nueva versión se puede cerrar");
    }

    @Test
    void mesaCerradaSinActa_delFlujoAnterior_puedeCargarla() {
        estado = EstadoEscrutinio.CERRADO;
        ActaPresidente a = servicio.actaPresidente();
        assertTrue(a.isPuedeSubirActaFisica());
        assertEquals("ACTA_FISICA", a.getFase().getFase());
        assertFalse(a.isPuedeEditarConteo());
    }

    @Test
    void conElActaFisicaValidadaYaNoSeSubeOtra() {
        estado = EstadoEscrutinio.CERRADO;
        actaFisica = new Documentos();
        actaFisica.setEstadoRevision(ActaFisicaEscrutinioService.VALIDADA);
        ActaPresidente a = servicio.actaPresidente();
        assertFalse(a.isPuedeSubirActaFisica());
        assertEquals(ActaFisicaEscrutinioService.VALIDADA, a.getActaFisica().getEstadoRevision());
    }

    // ------------------------------------------------------------

    private static Map<Integer, Integer> conteo(int lista, int blancos, int papeletas) {
        return Map.of(LISTA, lista, BLANCOS, blancos, PAPELETAS, papeletas);
    }

    private static String nombre(int id) {
        return switch (id) {
            case LISTA -> "LISTA 1";
            case BLANCOS -> "BLANCOS";
            default -> "PAPELETAS NO UTILIZADAS";
        };
    }

    private static CategoriaVoto categoria(int id) {
        CategoriaVoto c = new CategoriaVoto();
        c.setId(id);
        return c;
    }

    private void inyectar(String nombre, Object valor) throws Exception {
        var campo = GestionMovilService.class.getDeclaredField(nombre);
        campo.setAccessible(true);
        campo.set(servicio, valor);
    }
}
