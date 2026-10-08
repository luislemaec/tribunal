package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.ConsultaMovilDTO.AvanceMesa;
import ec.com.antenasur.dto.ConsultaMovilDTO.Elector;
import ec.com.antenasur.dto.ConsultaMovilDTO.Iglesia;
import ec.com.antenasur.dto.ConsultaMovilDTO.Miembro;
import ec.com.antenasur.dto.ConsultaMovilDTO.MiembroJunta;
import ec.com.antenasur.dto.ConsultaMovilDTO.MesaPresidente;
import ec.com.antenasur.dto.ConsultaMovilDTO.Pagina;
import ec.com.antenasur.dto.ConsultaMovilDTO.Proceso;
import ec.com.antenasur.dto.ConsultaMovilDTO.ResultadoCategoria;
import ec.com.antenasur.dto.ConsultaMovilDTO.ResultadosMesa;
import ec.com.antenasur.dto.ConsultaMovilDTO.ResultadosProceso;
import ec.com.antenasur.dto.ConsultaMovilDTO.ResumenProceso;
import ec.com.antenasur.dto.ConsultaMovilDTO.Ubicacion;
import ec.com.antenasur.dto.ConsultaMovilDTO.VotosCategoria;
import ec.com.antenasur.dto.EscrutinioCabeceraDTO;
import ec.com.antenasur.dto.EstadoJuntaDTO;
import ec.com.antenasur.dto.FiltroMiembrosDTO;
import ec.com.antenasur.dto.IglesiaDTO;
import ec.com.antenasur.dto.IglesiaPersonaDTO;
import ec.com.antenasur.dto.MesaDTO;
import ec.com.antenasur.dto.MesaDocumentosDTO;
import ec.com.antenasur.dto.MiembroJRVDTO;
import ec.com.antenasur.dto.PersonaDTO;
import ec.com.antenasur.dto.ResultadoPublicoSnapshotDTO;
import ec.com.antenasur.dto.ResumenDashboardDTO;
import ec.com.antenasur.dto.ResumenMiembrosIglesiaDTO;
import ec.com.antenasur.enums.EstadoEscrutinio;
import ec.com.antenasur.facade.UsuarioFacade;
import ec.com.antenasur.facade.tec.MesaFacade;
import ec.com.antenasur.model.Usuario;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.service.IglesiaPersonaService;
import ec.com.antenasur.service.IglesiaService;

/**
 * Consultas de solo lectura del módulo Tribunal de la App móvil (docs/api-movil.md).
 * Reutiliza los servicios de la web y nunca recibe el alcance desde la App: la mesa del
 * Presidente y la iglesia del IglesiaAdmin se resuelven desde el principal Elytron, y los
 * datos electorales siempre se filtran por el proceso activo ({@code proce_id}).
 *
 * <p>Cada método declara sus roles: los roles sin funciones en la V1 (Tecnico, Gerencial,
 * Supervisor, Superadministrador) reciben {@code EJBAccessException} (403 en la API).
 */
@Stateless
@DeclareRoles({ ConsultaMovilService.ADMINISTRADOR, ConsultaMovilService.TRIBUNAL, ConsultaMovilService.IGLESIA_ADMIN,
        ConsultaMovilService.PRESIDENTE_MESA })
public class ConsultaMovilService {

    static final String ADMINISTRADOR = "SITEC-Administrador";
    static final String TRIBUNAL = "SITEC-Tribunal";
    static final String IGLESIA_ADMIN = "SITEC-IglesiaAdmin";
    static final String PRESIDENTE_MESA = "SITEC-Presidente-mesa";

    /** Tamaño máximo de una página de miembros. */
    public static final int TAMANO_MAXIMO = 100;

    @Resource
    private SessionContext sessionContext;

    @Inject
    private UsuarioFacade usuarioFacade;

    @Inject
    private ProcesoElectoralService procesoElectoralService;

    @Inject
    private CronogramaService cronogramaService;

    @Inject
    private MiembroJRVService miembroJRVService;

    @Inject
    private EscrutinioService escrutinioService;

    @Inject
    private PadronService padronService;

    @Inject
    private IglesiaService iglesiaService;

    @Inject
    private IglesiaPersonaService iglesiaPersonaService;

    @Inject
    private DashboardResumenService dashboardResumenService;

    @Inject
    private ResultadosPublicosCacheService resultadosPublicos;

    @Inject
    private MesaFacade mesaFacade;

    // ---------------------------------------------------------------- Presidente de mesa

    /**
     * Mesa del Presidente según su designación JRV como PRESIDENTE en el proceso activo
     * (misma regla que actaE.xhtml). {@code null} si no tiene mesa asignada.
     */
    @RolesAllowed(PRESIDENTE_MESA)
    public MesaPresidente mesaPresidente() {
        ProcesoElectoral proceso = procesoElectoralService.getActivo();
        MesaDTO mesa = mesaDelPresidente(proceso);
        if (mesa == null) {
            return null;
        }
        Integer procesoId = proceso.getId();
        EscrutinioCabeceraDTO cabecera = escrutinioService.buscarCabeceraDTO(mesa.getId(), procesoId);
        EstadoEscrutinio estado = cabecera != null && cabecera.getEstadoEscrutinio() != null
                ? cabecera.getEstadoEscrutinio() : EstadoEscrutinio.PENDIENTE;

        List<MiembroJunta> junta = new ArrayList<>();
        for (MiembroJRVDTO miembro : miembroJRVService.listarDTOsPorMesaProceso(mesa.getId(), procesoId)) {
            junta.add(new MiembroJunta(miembro.getCargoNombre(),
                    nombre(miembro.getIglesiaPersona() != null ? miembro.getIglesiaPersona().getPersona() : null)));
        }

        ResultadosMesa resultados = null;
        if (estado == EstadoEscrutinio.CERRADO) {
            List<VotosCategoria> categorias = escrutinioService.listarDTOsPorMesaYProceso(mesa.getId(), procesoId)
                    .stream()
                    .filter(e -> !EscrutinioService.esCategoriaPapeletas(e.getCategoriaNombre()))
                    .map(e -> new VotosCategoria(e.getCategoriaNombre(), e.getCategoriaTipo(), e.getTotalVotos()))
                    .toList();
            resultados = new ResultadosMesa(cabecera.getTotalSufragantes(), cabecera.getTotalVotosRegistrados(),
                    cabecera.getTotalVotosValidos(), cabecera.getTotalVotosBlancos(), cabecera.getTotalVotosNulos(),
                    categorias);
        }
        return new MesaPresidente(proceso(proceso), ubicacion(mesa), estado.name(),
                cabecera != null ? cabecera.getFechaApertura() : null,
                cabecera != null ? cabecera.getFechaCierre() : null,
                padronService.contarSufragantesPorMesaYProceso(mesa.getId(), procesoId), junta, resultados);
    }

    /** Padrón de la mesa del Presidente en el proceso activo, ordenado por nombre. */
    @RolesAllowed(PRESIDENTE_MESA)
    public List<Elector> padronPresidente() {
        ProcesoElectoral proceso = procesoElectoralService.getActivo();
        MesaDTO mesa = mesaDelPresidente(proceso);
        if (mesa == null) {
            return null;
        }
        return padronService.listarDTOsPorMesaIdsYProceso(List.of(mesa.getId()), proceso.getId()).stream()
                .map(p -> new Elector(nombre(p.getIglesiaPersona() != null ? p.getIglesiaPersona().getPersona() : null),
                        p.getIglesiaPersona() != null && p.getIglesiaPersona().getIglesia() != null
                                ? p.getIglesiaPersona().getIglesia().getNombre() : null,
                        p.getSufrago()))
                .sorted(Comparator.comparing(Elector::getNombre, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private MesaDTO mesaDelPresidente(ProcesoElectoral proceso) {
        if (proceso == null || proceso.getId() == null) {
            return null;
        }
        Usuario usuario = usuarioActual();
        if (usuario == null || usuario.getPersonsa() == null) {
            return null;
        }
        MiembroJRVDTO designacion = miembroJRVService
                .obtenerDesignacionPresidentePorPersonaProceso(usuario.getPersonsa().getId(), proceso.getId());
        return designacion != null ? designacion.getMesa() : null;
    }

    // ---------------------------------------------------------------- IglesiaAdmin

    /** Datos y resumen de miembros de la iglesia asignada al usuario. */
    @RolesAllowed(IGLESIA_ADMIN)
    public Iglesia iglesia() {
        Integer iglesiaId = iglesiaDelUsuario();
        IglesiaDTO iglesia = iglesiaId != null ? iglesiaService.obtenerDTOPorId(iglesiaId) : null;
        if (iglesia == null) {
            return null;
        }
        ResumenMiembrosIglesiaDTO resumen = iglesiaPersonaService.obtenerResumenMiembrosActivosPorIglesia(iglesiaId);
        int total = resumen.getTotalPersonas();
        return new Iglesia(iglesia.getId(), iglesia.getNombre(), iglesia.getComunidad(), iglesia.getUbicacionNombre(),
                iglesia.getCantonNombre(), iglesia.getProvinciaNombre(), total,
                Math.max(0, total - resumen.getPersonasNoHabilitadas()), resumen.getPersonasNoHabilitadas(),
                resumen.getPersonasInformacionCompleta(), resumen.getPersonasPendientesRevision(),
                resumen.getPersonasEnOtraIglesia());
    }

    /**
     * Miembros activos de la iglesia del usuario, por páginas y ordenados por nombre. El
     * alcance lo aplica IglesiaPersonaService desde el principal (no se recibe la iglesia).
     */
    @RolesAllowed(IGLESIA_ADMIN)
    public Pagina<Miembro> miembros(String busqueda, Boolean habilitado, int pagina, int tamano) {
        Integer iglesiaId = iglesiaDelUsuario();
        int tam = Math.min(Math.max(tamano, 1), TAMANO_MAXIMO);
        int pag = Math.max(pagina, 0);
        if (iglesiaId == null) {
            return new Pagina<>(0, pag, tam, List.of());
        }
        FiltroMiembrosDTO filtro = new FiltroMiembrosDTO(iglesiaId, null, null, null);
        filtro.setBusqueda(busqueda == null || busqueda.isBlank() ? null : busqueda.trim());
        filtro.setHabilitado(habilitado);
        long total = iglesiaPersonaService.contarMiembros(filtro);
        List<Miembro> elementos = new ArrayList<>();
        for (IglesiaPersonaDTO miembro : iglesiaPersonaService.listarMiembros(filtro, pag * tam, tam,
                "persona.nombres", false)) {
            elementos.add(new Miembro(nombre(miembro.getPersona()), Boolean.TRUE.equals(miembro.getHabilitadoPadron()),
                    Boolean.TRUE.equals(miembro.getActualizada())));
        }
        return new Pagina<>(total, pag, tam, elementos);
    }

    private Integer iglesiaDelUsuario() {
        Usuario usuario = usuarioActual();
        return usuario != null && usuario.getIglesia() != null ? usuario.getIglesia().getId() : null;
    }

    // ---------------------------------------------------------------- Tribunal / Administrador

    /** Resumen del proceso activo (el mismo del dashboard web). */
    @RolesAllowed({ ADMINISTRADOR, TRIBUNAL })
    public ResumenProceso resumenProceso() {
        ProcesoElectoral activo = procesoElectoralService.getActivo();
        if (activo == null || activo.getId() == null) {
            return null;
        }
        ResumenDashboardDTO r = dashboardResumenService.consultar();
        return new ResumenProceso(new Proceso(activo.getId(), activo.getNombre(), r.getFaseVigente()),
                r.getTotalIglesias(), r.getTotalRecintos(), r.getTotalMesas(), r.getTotalElectores(),
                r.getMesasConJuntaCompleta(), r.getMesasCerradas(), r.getPorcentajeEscrutinio(),
                r.getMesasPorEstado());
    }

    /** Estado de cada mesa activa en el proceso activo (tres consultas en total). */
    @RolesAllowed({ ADMINISTRADOR, TRIBUNAL })
    public List<AvanceMesa> avanceMesas() {
        ProcesoElectoral activo = procesoElectoralService.getActivo();
        if (activo == null || activo.getId() == null) {
            return List.of();
        }
        List<MesaDocumentosDTO> mesas = mesaFacade.listarResumenDocumentos(activo.getId(), null, null, null);
        Map<Integer, EscrutinioCabeceraDTO> cabeceras = escrutinioService.buscarCabecerasDTOPorProceso(activo.getId());
        List<Integer> ids = mesas.stream().map(MesaDocumentosDTO::getMesaId).filter(Objects::nonNull).toList();
        Map<Integer, EstadoJuntaDTO> juntas = ids.isEmpty() ? Map.of()
                : miembroJRVService.consultarEstadosJuntas(activo.getId(), ids);
        List<AvanceMesa> avance = new ArrayList<>();
        for (MesaDocumentosDTO mesa : mesas) {
            EscrutinioCabeceraDTO cabecera = cabeceras.get(mesa.getMesaId());
            EstadoJuntaDTO junta = juntas.get(mesa.getMesaId());
            avance.add(new AvanceMesa(
                    new Ubicacion(mesa.getMesaId(), mesa.getMesa(), mesa.getRecinto(), mesa.getParroquia(),
                            mesa.getCanton()),
                    cabecera != null && cabecera.getEstadoEscrutinio() != null ? cabecera.getEstadoEscrutinio().name()
                            : EstadoEscrutinio.PENDIENTE.name(),
                    junta != null && junta.isCompleta()));
        }
        return avance;
    }

    /** Resultados consolidados de las mesas cerradas (caché de resultados de TEC). */
    @RolesAllowed({ ADMINISTRADOR, TRIBUNAL })
    public ResultadosProceso resultadosProceso() {
        ResultadoPublicoSnapshotDTO s = resultadosPublicos.obtenerSnapshot();
        if (s == null || s.getProcesoActivo() == null) {
            return null;
        }
        List<ResultadoCategoria> categorias = s.getResultados().stream()
                .map(r -> new ResultadoCategoria(r.getCategoria(), r.getTipo(), lista(r.getListaNumero(), r.getListaNombre()),
                        r.getTotalVotos() != null ? r.getTotalVotos() : 0L, r.getPorcentaje()))
                .toList();
        return new ResultadosProceso(s.getProcesoActivo().getNombre(), s.getTotalMesasProceso(),
                s.getTotalMesasCerradas(), s.getPorcentajeMesasCerradasEntero(), s.getTotalVotosRegistrados(),
                s.getTotalVotosBlancos(), s.getTotalVotosNulos(), categorias, s.getUltimaActualizacion());
    }

    // ----------------------------------------------------------------

    private Usuario usuarioActual() {
        if (sessionContext == null || sessionContext.getCallerPrincipal() == null) {
            return null;
        }
        return usuarioFacade.findByUsuarioName(sessionContext.getCallerPrincipal().getName());
    }

    private Proceso proceso(ProcesoElectoral proceso) {
        var fase = cronogramaService.getFaseVigenteDelProcesoActivo();
        return new Proceso(proceso.getId(), proceso.getNombre(), fase != null ? fase.getTitulo() : null);
    }

    private static Ubicacion ubicacion(MesaDTO mesa) {
        return new Ubicacion(mesa.getId(), mesa.getNombre(),
                mesa.getRecinto() != null ? mesa.getRecinto().getNombre() : null,
                mesa.getRecinto() != null ? mesa.getRecinto().getUbicacionNombre() : mesa.getUbicacionNombre(),
                mesa.getRecinto() != null ? mesa.getRecinto().getCantonNombre() : mesa.getCantonNombre());
    }

    private static String nombre(PersonaDTO persona) {
        if (persona == null) {
            return null;
        }
        String completo = ((persona.getApellidos() == null ? "" : persona.getApellidos().trim()) + " "
                + (persona.getNombres() == null ? "" : persona.getNombres().trim())).trim();
        return completo.isEmpty() ? null : completo;
    }

    private static String lista(String numero, String nombre) {
        if (numero == null && nombre == null) {
            return null;
        }
        return ((numero != null ? "Lista " + numero : "") + (nombre != null ? " " + nombre : "")).trim();
    }
}
