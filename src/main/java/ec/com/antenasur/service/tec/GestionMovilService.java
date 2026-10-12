package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.ConsultaMovilDTO.ActaFisica;
import ec.com.antenasur.dto.ConsultaMovilDTO.ActaPresidente;
import ec.com.antenasur.dto.ConsultaMovilDTO.CategoriaConteo;
import ec.com.antenasur.dto.ConsultaMovilDTO.Miembro;
import ec.com.antenasur.dto.ConsultaMovilDTO.Ubicacion;
import ec.com.antenasur.dto.EscrutinioCabeceraDTO;
import ec.com.antenasur.dto.EscrutinioDTO;
import ec.com.antenasur.dto.IglesiaPersonaDTO;
import ec.com.antenasur.dto.MesaDTO;
import ec.com.antenasur.dto.PersonaDTO;
import ec.com.antenasur.enums.EstadoEscrutinio;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.model.tec.CategoriaVoto;
import ec.com.antenasur.model.tec.Documentos;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.service.IglesiaPersonaService;

/**
 * Operaciones de la App móvil que modifican datos (docs/api-movil.md). Separadas de las
 * consultas para que cada escritura declare sus roles de forma explícita; las reglas de
 * negocio viven en los servicios de la web, que se reutilizan sin duplicarlas.
 *
 * <p>Escrutinio del Presidente: la mesa sale de su designación JRV como PRESIDENTE en el
 * proceso activo ({@link AccesoDocumentoMesaService#mesaPermitida}), nunca de la App. Las
 * transiciones (apertura, conteo, cierre) las valida {@link EscrutinioService} igual que en
 * actaE; aquí se agregan las comprobaciones que en la web hace ActaEController (mesa propia,
 * acta completa y votos no negativos).
 */
@Stateless
@DeclareRoles({ GestionMovilService.ADMINISTRADOR, GestionMovilService.TRIBUNAL, GestionMovilService.IGLESIA_ADMIN,
        GestionMovilService.PRESIDENTE_MESA })
public class GestionMovilService {

    static final String ADMINISTRADOR = "SITEC-Administrador";
    static final String TRIBUNAL = "SITEC-Tribunal";
    static final String IGLESIA_ADMIN = "SITEC-IglesiaAdmin";
    static final String PRESIDENTE_MESA = "SITEC-Presidente-mesa";

    /** Límite de cordura por categoría; una mesa real tiene cientos de electores. */
    static final int VOTOS_MAXIMOS = 100_000;

    @Resource
    private SessionContext sessionContext;

    @Inject
    private IglesiaPersonaService iglesiaPersonaService;

    @Inject
    private ProcesoElectoralService procesoElectoralService;

    @Inject
    private AccesoDocumentoMesaService accesoDocumental;

    @Inject
    private MesaService mesaService;

    @Inject
    private EscrutinioService escrutinioService;

    @Inject
    private CategoriaVotoService categoriaVotoService;

    @Inject
    private PadronService padronService;

    @Inject
    private DisponibilidadDocumentoMesaService disponibilidadDocumental;

    @Inject
    private ActaFisicaEscrutinioService actaFisicaService;

    // ---------------------------------------------------------------- IglesiaAdmin

    /**
     * El IglesiaAdmin habilita o deshabilita a un miembro de su iglesia para participar en
     * las elecciones. IglesiaPersonaService valida el cronograma y el alcance, y marca al
     * miembro como revisado (igual que la web).
     */
    @RolesAllowed(IGLESIA_ADMIN)
    public Miembro cambiarHabilitacionMiembro(Integer miembroId, boolean habilitado) {
        IglesiaPersonaDTO ip = iglesiaPersonaService.cambiarHabilitacion(miembroId, habilitado);
        PersonaDTO persona = ip.getPersona();
        String nombre = persona == null ? null
                : ((persona.getApellidos() == null ? "" : persona.getApellidos().trim()) + " "
                        + (persona.getNombres() == null ? "" : persona.getNombres().trim())).trim();
        return new Miembro(ip.getId(), nombre, Boolean.TRUE.equals(ip.getHabilitadoPadron()), true);
    }

    // ---------------------------------------------------------------- Presidente de mesa

    /** Acta de la mesa del Presidente: estado, conteo actual, cuadre y acta física. */
    @RolesAllowed(PRESIDENTE_MESA)
    public ActaPresidente actaPresidente() {
        Contexto c = contexto();
        return acta(c);
    }

    /** Registra la apertura de la mesa (TEC valida junta, padrón y documentos). */
    @RolesAllowed(PRESIDENTE_MESA)
    public ActaPresidente abrirMesa(String observacion) {
        Contexto c = contexto();
        escrutinioService.abrirEscrutinioDTO(c.mesa.getId(), c.procesoId, usuarioActual(), recortar(observacion, 500),
                sufragantes(c));
        return acta(c);
    }

    /**
     * Guarda el conteo como borrador. Debe incluir todas las categorías del acta; queda
     * CONTEO_REGISTRADO si el cuadre de papeletas es cero y EN_CONTEO si no.
     */
    @RolesAllowed(PRESIDENTE_MESA)
    public ActaPresidente guardarConteo(Map<Integer, Integer> votosPorCategoria) {
        Contexto c = contexto();
        exigirMesaAbierta(c);
        escrutinioService.guardarBorradorConteoDTO(c.mesa.getId(), c.procesoId, itemsDelActa(c, votosPorCategoria),
                sufragantes(c));
        return acta(c);
    }

    /**
     * Cierra la mesa con el conteo enviado. Igual que en la web, exige haber guardado el
     * conteo antes (estado EN_CONTEO, CONTEO_REGISTRADO o REABIERTO); después ya no se modifica.
     */
    @RolesAllowed(PRESIDENTE_MESA)
    public ActaPresidente cerrarMesa(Map<Integer, Integer> votosPorCategoria) {
        Contexto c = contexto();
        exigirMesaAbierta(c);
        escrutinioService.guardarActaCompletaDTO(c.mesa.getId(), itemsDelActa(c, votosPorCategoria));
        return acta(c);
    }

    /**
     * Sube la foto (JPEG) del acta física firmada de una mesa cerrada. ActaFisicaEscrutinioService
     * valida de nuevo rol, mesa, autoría, estado y archivo, y la deja PENDIENTE_REVISION.
     */
    @RolesAllowed(PRESIDENTE_MESA)
    public ActaPresidente subirActaFisica(byte[] jpeg) {
        Contexto c = contexto();
        actaFisicaService.cargar(c.mesa.getId(), c.procesoId, "acta-fisica.jpg", "image/jpeg", jpeg);
        return acta(c);
    }

    // ---------------------------------------------------------------- Revisión del acta (solo Tribunal)

    /**
     * Marca como datos oficiales lo registrado por el Presidente, cuando el revisor confirmó
     * que el acta física coincide (mismo servicio que «Marcar como datos oficiales» de actaE).
     * Devuelve el id del documento revisado, para la bitácora.
     */
    @RolesAllowed(TRIBUNAL)
    public Integer validarActaFisica(Integer mesaId) {
        Integer procesoId = procesoActivoId();
        Documentos acta = actaRevisable(mesaId, procesoId);
        actaFisicaService.validarDatosRegistrados(acta.getId(), mesaId, procesoId);
        return acta.getId();
    }

    /**
     * Observa o rechaza el acta física con un motivo obligatorio: el acta queda devuelta, la
     * mesa vuelve a REABIERTO para que el Presidente corrija y se le notifica (si hay push).
     */
    @RolesAllowed(TRIBUNAL)
    public Integer devolverActaFisica(Integer mesaId, String estado, String motivo) {
        if (!ActaFisicaEscrutinioService.OBSERVADA.equals(estado) && !ActaFisicaEscrutinioService.RECHAZADA.equals(estado)) {
            throw new NegocioException("Indique si el acta queda observada o rechazada.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new NegocioException("Indique el motivo de la devolución del acta.");
        }
        Integer procesoId = procesoActivoId();
        Documentos acta = actaRevisable(mesaId, procesoId);
        actaFisicaService.devolverActaFisica(acta.getId(), estado, recortar(motivo, 500));
        return acta.getId();
    }

    private Integer procesoActivoId() {
        ProcesoElectoral proceso = procesoElectoralService.getActivo();
        if (proceso == null || proceso.getId() == null) {
            throw new NegocioException("No hay un proceso electoral activo.");
        }
        return proceso.getId();
    }

    /** Acta vigente de una mesa cerrada y aún no validada (misma regla que actaE). */
    private Documentos actaRevisable(Integer mesaId, Integer procesoId) {
        EscrutinioCabeceraDTO cabecera = mesaId != null ? escrutinioService.buscarCabeceraDTO(mesaId, procesoId) : null;
        if (cabecera == null || cabecera.getEstadoEscrutinio() != EstadoEscrutinio.CERRADO) {
            throw new NegocioException("La mesa debe estar cerrada para revisar su acta física.");
        }
        Documentos acta = actaFisicaService.obtenerVigente(mesaId, procesoId);
        if (acta == null) {
            throw new NegocioException("La mesa no tiene una foto del acta física para revisar.");
        }
        if (ActaFisicaEscrutinioService.VALIDADA.equals(acta.getEstadoRevision())) {
            throw new NegocioException("El acta física de la mesa ya fue validada.");
        }
        return acta;
    }

    // ----------------------------------------------------------------

    private record Contexto(Integer procesoId, MesaDTO mesa) {
    }

    private Contexto contexto() {
        ProcesoElectoral proceso = procesoElectoralService.getActivo();
        if (proceso == null || proceso.getId() == null) {
            throw new NegocioException("No hay un proceso electoral activo.");
        }
        Integer mesaId = accesoDocumental.mesaPermitida(proceso.getId());
        if (mesaId == null) {
            // Solo ocurre con roles revisores sin designación: la App no les ofrece estas acciones.
            throw new NegocioException("No tiene una mesa asignada como Presidente en el proceso electoral activo.");
        }
        return new Contexto(proceso.getId(), mesaService.obtenerDTOPorId(mesaId));
    }

    private ActaPresidente acta(Contexto c) {
        Integer mesaId = c.mesa.getId();
        EscrutinioCabeceraDTO cabecera = escrutinioService.buscarCabeceraDTO(mesaId, c.procesoId);
        EstadoEscrutinio estado = cabecera != null && cabecera.getEstadoEscrutinio() != null
                ? cabecera.getEstadoEscrutinio() : EstadoEscrutinio.PENDIENTE;
        int sufragantes = cabecera != null && cabecera.getTotalSufragantes() != null && cabecera.getTotalSufragantes() > 0
                ? cabecera.getTotalSufragantes() : sufragantes(c);

        List<CategoriaConteo> categorias = new ArrayList<>();
        int emitidos = 0;
        int papeletas = 0;
        for (EscrutinioDTO item : plantilla(c)) {
            boolean esPapeletas = EscrutinioService.esCategoriaPapeletas(item.getCategoriaNombre());
            int votos = item.getTotalVotos() != null ? item.getTotalVotos() : 0;
            if (esPapeletas) {
                papeletas += votos;
            } else {
                emitidos += votos;
            }
            categorias.add(new CategoriaConteo(item.getCategoriaId(), item.getCategoriaNombre(), item.getCategoriaTipo(),
                    esPapeletas, votos));
        }

        boolean pendiente = estado == EstadoEscrutinio.PENDIENTE;
        boolean editable = estado == EstadoEscrutinio.ABIERTO || estado == EstadoEscrutinio.EN_CONTEO
                || estado == EstadoEscrutinio.CONTEO_REGISTRADO || estado == EstadoEscrutinio.REABIERTO;
        boolean cerrable = estado == EstadoEscrutinio.EN_CONTEO || estado == EstadoEscrutinio.CONTEO_REGISTRADO
                || estado == EstadoEscrutinio.REABIERTO;
        String bloqueo = pendiente ? disponibilidadDocumental.motivoBloqueoApertura(c.procesoId, mesaId) : null;

        ActaFisica actaFisica = null;
        Documentos vigente = estado == EstadoEscrutinio.CERRADO ? actaFisicaService.obtenerVigente(mesaId, c.procesoId) : null;
        if (vigente != null) {
            actaFisica = new ActaFisica(vigente.getVersion(), vigente.getEstadoRevision(), vigente.getObservacionRevision());
        }
        boolean puedeSubir = estado == EstadoEscrutinio.CERRADO
                && (vigente == null || !ActaFisicaEscrutinioService.VALIDADA.equals(vigente.getEstadoRevision()));

        return new ActaPresidente(ubicacion(c.mesa), estado.name(), sufragantes, emitidos, papeletas,
                EscrutinioService.calcularCuadrePapeletas(sufragantes, emitidos, papeletas),
                cabecera != null ? cabecera.getObservacionConteo() : null, bloqueo,
                pendiente && bloqueo == null, editable && !categorias.isEmpty(), cerrable && !categorias.isEmpty(),
                puedeSubir, categorias, actaFisica);
    }

    /** Ítems del acta (existentes o por crear) para las categorías del proceso. */
    private List<EscrutinioDTO> plantilla(Contexto c) {
        List<Integer> categoriaIds = new ArrayList<>();
        for (CategoriaVoto categoria : categoriaVotoService.getCategoriasOrdenados(c.procesoId)) {
            categoriaIds.add(categoria.getId());
        }
        return escrutinioService.prepararActaPorMesaDTO(c.mesa.getId(), c.procesoId, categoriaIds);
    }

    /**
     * Aplica los votos enviados sobre la plantilla del acta. Exige exactamente las categorías
     * del acta (ni faltantes ni ajenas), sin negativos: la App no puede crear ni omitir ítems.
     */
    private List<EscrutinioDTO> itemsDelActa(Contexto c, Map<Integer, Integer> votos) {
        List<EscrutinioDTO> items = plantilla(c);
        if (items.isEmpty()) {
            throw new NegocioException("El acta no tiene categorías de votos configuradas.");
        }
        Map<Integer, Integer> pendientes = votos == null ? Map.of() : new HashMap<>(votos);
        for (EscrutinioDTO item : items) {
            Integer valor = pendientes.remove(item.getCategoriaId());
            if (valor == null) {
                throw new NegocioException("Falta registrar los votos de " + item.getCategoriaNombre() + ".");
            }
            if (valor < 0 || valor > VOTOS_MAXIMOS) {
                throw new NegocioException("Los votos de " + item.getCategoriaNombre() + " no son válidos.");
            }
            item.setTotalVotos(valor);
        }
        if (!pendientes.isEmpty()) {
            throw new NegocioException("El conteo incluye categorías que no pertenecen al acta de la mesa.");
        }
        return items;
    }

    private void exigirMesaAbierta(Contexto c) {
        EscrutinioCabeceraDTO cabecera = escrutinioService.buscarCabeceraDTO(c.mesa.getId(), c.procesoId);
        EstadoEscrutinio estado = cabecera != null ? cabecera.getEstadoEscrutinio() : null;
        if (estado == null || estado == EstadoEscrutinio.PENDIENTE) {
            throw new NegocioException("Debe registrar la apertura de la mesa antes de registrar el conteo.");
        }
    }

    private int sufragantes(Contexto c) {
        return padronService.contarSufragantesPorMesaYProceso(c.mesa.getId(), c.procesoId);
    }

    private String usuarioActual() {
        return sessionContext != null && sessionContext.getCallerPrincipal() != null
                ? sessionContext.getCallerPrincipal().getName() : null;
    }

    private static Ubicacion ubicacion(MesaDTO mesa) {
        return new Ubicacion(mesa.getId(), mesa.getNombre(),
                mesa.getRecinto() != null ? mesa.getRecinto().getNombre() : null,
                mesa.getRecinto() != null ? mesa.getRecinto().getUbicacionNombre() : mesa.getUbicacionNombre(),
                mesa.getRecinto() != null ? mesa.getRecinto().getCantonNombre() : mesa.getCantonNombre());
    }

    private static String recortar(String texto, int maximo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.strip();
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo);
    }
}
