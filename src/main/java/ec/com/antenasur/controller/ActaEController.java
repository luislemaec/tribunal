package ec.com.antenasur.controller;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.itextpdf.text.Font;
import com.itextpdf.text.FontProvider;

import ec.com.antenasur.bean.DocumentoBean;
import ec.com.antenasur.bean.GeograpBean;
import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.bean.ProcesoBean;
import ec.com.antenasur.dto.ActaEGerencialDTO;
import ec.com.antenasur.dto.ProgresoEscrutinioDTO;
import org.primefaces.model.menu.DefaultMenuModel;
import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.MenuModel;
import ec.com.antenasur.dto.CandidatoDTO;
import ec.com.antenasur.dto.EscrutinioCabeceraDTO;
import ec.com.antenasur.dto.EscrutinioDTO;
import ec.com.antenasur.dto.MesaDTO;
import ec.com.antenasur.dto.MiembroJRVDTO;
import ec.com.antenasur.dto.RecintoDTO;
import ec.com.antenasur.enums.EstadoEscrutinio;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.itext.ReportePFD;
import ec.com.antenasur.itext.UtilHtml;
import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.tec.CatalogoGeneral;
import ec.com.antenasur.model.tec.CategoriaVoto;
import ec.com.antenasur.model.tec.Documentos;
import ec.com.antenasur.model.tec.Lista;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.model.tec.PlantillaCorreo;
import ec.com.antenasur.model.tec.TipoDocumento;
import ec.com.antenasur.report.ReportTemplateController;
import ec.com.antenasur.service.tec.CategoriaVotoService;
import ec.com.antenasur.service.tec.ActaFisicaEscrutinioService;
import ec.com.antenasur.service.tec.EscrutinioService;
import ec.com.antenasur.service.tec.ListaService;
import ec.com.antenasur.service.tec.MesaService;
import ec.com.antenasur.service.tec.MiembroJRVService;
import ec.com.antenasur.service.tec.PadronService;
import ec.com.antenasur.service.tec.ProcesoElectoralService;
import ec.com.antenasur.service.tec.PlantillaCorreoService;
import ec.com.antenasur.service.tec.RecintoService;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.RepositorioDocumentos;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.model.file.UploadedFile;

@Named
@ViewScoped
@Slf4j
public class ActaEController implements Serializable {
    @Inject private ec.com.antenasur.service.tec.AlcanceSesionQrService alcanceQr;

    private static final long serialVersionUID = 1L;

    private static final Integer TAMANIO_LETRA = 0;
    private static final String FORMULARIO = "frmActaE";
    /**
     * Provincia operativa del módulo (Chimborazo). Es el mismo identificador de
     * referencia geográfica que ya usaban este controlador, MesaController y
     * RecintoController; se centraliza aquí para no repetirlo.
     */
    private static final int PROVINCIA_OPERATIVA_ID = 7;

    @Inject
    private LoginBean loginBean;

    @Inject
    private ProcesoBean procesoBean;

    @Inject
    private ListaService listaService;

    @Inject
    private PlantillaCorreoService plantillaCorreoService;

    @Inject
    private ProcesoElectoralService procesoElectoralService;

    @Inject
    private GeograpBean geograpBean;

    @Inject
    private RecintoService recintoService;

    @Inject
    private MesaService mesaService;

    @Inject
    private MiembroJRVService miembroJRVService;

    @Inject
    private CategoriaVotoService categoriaVotoService;

    @Inject
    private EscrutinioService escrutinioService;

    @Inject
    private ec.com.antenasur.service.tec.ResumenEscrutinioService resumenEscrutinioService;

    @Inject
    private DocumentoBean documentoBean;

    @Inject
    private PadronService padronService;

    @Inject
    private ActaFisicaEscrutinioService actaFisicaEscrutinioService;

    @Inject
    private ec.com.antenasur.service.tec.DisponibilidadDocumentoMesaService disponibilidadDocumentoMesaService;

    /** Motivo por el que la mesa seleccionada no puede registrar la apertura; null si está habilitada. */
    @Getter
    private String motivoBloqueoApertura;

    @Getter
    @Setter
    private PlantillaCorreo plantillaCorreoSeleccionado;

    @Setter
    @Getter
    private List<Geograp> provincias, cantones, parroquias;

    @Setter
    @Getter
    private Geograp cantonSeleccionado, parroquiaSeleccionado;

    @Setter
    @Getter
    private Integer provinciaFiltroId;

    @Setter
    @Getter
    private Integer procesoConsultaId;

    @Setter
    @Getter
    private EstadoEscrutinio estadoFiltro;

    @Setter
    @Getter
    private List<EstadoEscrutinio> estadosEscrutinio;

    @Setter
    @Getter
    private List<ProcesoElectoral> procesosElectorales;

    @Setter
    @Getter
    private RecintoDTO recintoSeleccionado;

    @Setter
    @Getter
    private List<RecintoDTO> listaRecintos, listaRecintosSeleccionados;

    @Setter
    @Getter
    private List<MesaDTO> listaMesas, listaMesasCerradas;

    @Setter
    @Getter
    private MesaDTO mesaSeleccionado;

    // NOTA: Lista, CatalogoGeneral, CategoriaVoto, Periodo, PlantillaCorreo
    // siguen como entidades; sus DTOs se crean en la iteración de catálogos.
    @Setter
    @Getter
    private Lista listaSeleccionado;

    @Setter
    @Getter
    private List<Lista> listas;

    @Setter
    @Getter
    private List<CatalogoGeneral> cargosCandidatos;

    @Setter
    @Getter
    private List<CategoriaVoto> categoriasVotos;

    @Setter
    @Getter
    private List<EscrutinioDTO> listaCamposActaE;

    @Setter
    @Getter
    private List<Documentos> documentosActa;

    @Getter
    private Documentos actaFisica;

    @Getter
    @Setter
    private transient UploadedFile archivoActaFisica;

    @Setter
    @Getter
    private EscrutinioCabeceraDTO escrutinioCabecera;

    @Setter
    @Getter
    private ProcesoElectoral procesoActivo;

    @Setter
    @Getter
    private CandidatoDTO candidatoSeleccionado;

    @Setter
    @Getter
    private String cedulaBuscar;

    @Setter
    @Getter
    private String observacionApertura;

    @Setter
    @Getter
    private String motivoCambioEstado;

    @Setter
    @Getter
    private Integer totalSufragantesAsignados;

    @Getter
    private boolean accesoRestringidoPresidenteMesa;

    @Getter
    private boolean sinMesaAsignada;

    @Getter
    private boolean usuarioConsultaGerencial;

    @Setter
    @Getter
    private int tabActivoActaE;

    @Setter
    @Getter
    private List<ActaEGerencialDTO> listaConsultaGerencial;

    /**
     * Listado completo del proceso, cargado una sola vez. Los filtros de la pantalla se
     * aplican en memoria sobre esta lista, de modo que cambiar cantón, parroquia, estado
     * o búsqueda no vuelve a consultar la base.
     */
    private List<ActaEGerencialDTO> filasEscrutinioProceso = new ArrayList<>();

    /** Búsqueda compacta por recinto o número de mesa, sobre el listado ya cargado. */
    @Setter
    @Getter
    private String busquedaEscrutinio;

    /**
     * Filtros de columna del listado (Situación y Recinto). Los aplica el propio
     * p:dataTable sobre las filas ya cargadas; se enlazan aquí solo para poder
     * limpiarlos desde el botón «Limpiar».
     */
    @Setter
    @Getter
    private String filtroSituacionTabla;

    @Setter
    @Getter
    private String filtroRecintoTabla;

    private static final String ID_TABLA_ESCRUTINIOS = FORMULARIO + ":tabActaE:tblConsultaGerencial";

    /**
     * Imagen del acta física para el visor, leída solo cuando el usuario pide verla: no se
     * carga al seleccionar la mesa, porque puede pesar varios megabytes.
     */
    private byte[] contenidoActaFisica;

    /**
     * La mesa se abrió en modo consulta (revisor sin permiso de operación): se cargó
     * sin crear la cabecera ni preparar el conteo. Administrador y Tribunal operan, así
     * que para ellos no se activa.
     */
    @Getter
    private boolean mesaSoloLectura;

    @Getter
    private int totalMesasGerencial;

    @Getter
    private int mesasPendientesGerencial;

    @Getter
    private int mesasAbiertasGerencial;

    @Getter
    private int mesasConteoGerencial;

    @Getter
    private int mesasCerradasGerencial;

    /** Mesas cerradas con acta física validada (dato oficial), dentro del listado filtrado. */
    @Getter
    private int mesasValidadasGerencial;

    /** Mesas del proceso con acta física validada; se consulta una vez con el listado. */
    private java.util.Set<Integer> mesasValidadasProceso = new java.util.HashSet<>();

    @Getter
    private int mesasObservadasGerencial;

    @Getter
    private int totalSufragantesGerencial;

    @Getter
    private int totalVotosRegistradosGerencial;

    @Getter
    private int totalVotosValidosGerencial;

    @Getter
    private int totalVotosBlancosGerencial;

    @Getter
    private int totalVotosNulosGerencial;

    @Getter
    private int totalActasGeneradasGerencial;

    @Getter
    private int totalActasPendientesGerencial;

    @PostConstruct
    private void init() {
        inicializaVariables();
        cargaDatosIniciales();
    }

    private void inicializaVariables() {
        this.listaCamposActaE = new ArrayList<>();
        this.documentosActa = new ArrayList<>();
        this.listaConsultaGerencial = new ArrayList<>();
        this.cantonSeleccionado = new Geograp();
        this.parroquiaSeleccionado = new Geograp();
        this.recintoSeleccionado = new RecintoDTO();
        this.mesaSeleccionado = new MesaDTO();
        this.totalSufragantesAsignados = 0;
        this.observacionApertura = "";
        this.motivoCambioEstado = "";
        this.escrutinioCabecera = new EscrutinioCabeceraDTO();
        this.estadosEscrutinio = List.of(EstadoEscrutinio.values());
        limpiarResumenGerencial();
    }

    private void cargaDatosIniciales() {
        this.procesoActivo = procesoElectoralService.getActivo();
        var contextoQr = alcanceQr.contexto();
        if (contextoQr != null) {
            alcanceQr.validar(contextoQr.mesaId(), procesoActivo != null ? procesoActivo.getId() : null);
            procesosElectorales = new ArrayList<>();
            procesosElectorales.add(procesoActivo);
            procesoConsultaId = procesoActivo.getId();
            provincias = new ArrayList<>();
            cantones = new ArrayList<>();
            parroquias = new ArrayList<>();
            listas = new ArrayList<>();
            categoriasVotos = categoriaVotoService.getCategoriasOrdenados(procesoConsultaId);
            accesoRestringidoPresidenteMesa = true;
            usuarioConsultaGerencial = false;
            mesaSeleccionado = mesaService.obtenerDTOPorId(contextoQr.mesaId());
            recintoSeleccionado = mesaSeleccionado.getRecinto();
            listaMesas = new ArrayList<>();
            listaMesas.add(mesaSeleccionado);
            listaRecintos = new ArrayList<>();
            listaRecintos.add(recintoSeleccionado);
            cargaDatosMesaSeleccionada();
            return;
        }
        // La pantalla trabaja siempre con el proceso vigente: no hay selector de proceso,
        // así que no se consulta el catálogo completo de procesos electorales.
        this.procesosElectorales = new ArrayList<>();
        if (procesoActivo != null) {
            this.procesosElectorales.add(procesoActivo);
        }
        this.procesoConsultaId = procesoActivo != null ? procesoActivo.getId() : null;
        cargarGeografiaOperativa();
        this.listaRecintos = new ArrayList<>();
        this.listaMesas = new ArrayList<>();
        this.listas = listaService.findAll();
        this.categoriasVotos = categoriaVotoService.getCategoriasOrdenados(
                procesoActivo != null ? procesoActivo.getId() : null);

        accesoRestringidoPresidenteMesa = esPresidenteMesa();
        usuarioConsultaGerencial = !accesoRestringidoPresidenteMesa && tieneRolConsultaGerencial();
        // El listado es el elemento principal de la pantalla: se carga al entrar, con
        // cuatro consultas en total, para que el usuario vea las mesas y su estado.
        if (usuarioConsultaGerencial) {
            consultarEscrutiniosGerenciales();
        }
        MesaDTO mesaUsuario = obtenerMesaPorUsuario();
        if (accesoRestringidoPresidenteMesa && mesaUsuario == null) {
            sinMesaAsignada = true;
            JsfUtil.addWarningMessageFromBundle("actaE.mensaje.sin.mesa.asignada");
            return;
        }
        if (mesaUsuario != null) {
            mesaSeleccionado = mesaUsuario;
            recintoSeleccionado = mesaUsuario.getRecinto();
            listaMesas = new ArrayList<>();
            listaMesas.add(mesaUsuario);
            listaRecintos = new ArrayList<>();
            if (mesaUsuario.getRecinto() != null) {
                listaRecintos.add(mesaUsuario.getRecinto());
            }
            cargaDatosMesaSeleccionada();
        }
    }

    /**
     * Geografía operativa del módulo: los recintos que se escrutan aquí pertenecen a una
     * sola provincia, así que no se ofrece filtro de provincia. Se fija la provincia
     * operativa y se cargan directamente sus cantones, de modo que el filtrado por
     * provincia que ya existe sigue acotando la consulta sin intervención del usuario.
     */
    private void cargarGeografiaOperativa() {
        provincias = new ArrayList<>();
        cantones = new ArrayList<>();
        parroquias = new ArrayList<>();
        try {
            Geograp provincia = geograpBean.getById(PROVINCIA_OPERATIVA_ID);
            if (provincia != null) {
                provincias.add(provincia);
                provinciaFiltroId = provincia.getId();
                List<Geograp> hijos = geograpBean.getByFatherId(provincia.getId());
                if (hijos != null) {
                    cantones = new ArrayList<>(hijos);
                }
            }
        } catch (Exception e) {
            log.warn("NO SE PUDO CARGAR LA GEOGRAFIA OPERATIVA PARA CONSULTA DE ACTAS", e);
        }
    }

    private MesaDTO obtenerMesaPorUsuario() {
        var contextoQr = alcanceQr.contexto();
        if (contextoQr != null) {
            alcanceQr.validar(contextoQr.mesaId(), contextoQr.procesoId());
            return mesaService.obtenerDTOPorId(contextoQr.mesaId());
        }
        MesaDTO mesaPorJunta = obtenerMesaPorDesignacionJRV();
        if (mesaPorJunta != null) {
            return mesaPorJunta;
        }
        try {
            ec.com.antenasur.model.tec.Mesa m = mesaService.getMesaPorUsuario(loginBean.getUserName());
            return MesaDTO.fromEntity(m);
        } catch (Exception e) {
            return null;
        }
    }

    private MesaDTO obtenerMesaPorDesignacionJRV() {
        try {
            Integer personaId = loginBean != null && loginBean.getUsuario() != null
                    ? loginBean.getUsuario().getPersonaId() : null;
            Integer procesoId = procesoActivo != null ? procesoActivo.getId() : null;
            if (personaId == null || procesoId == null) {
                return null;
            }
            MiembroJRVDTO designacion = miembroJRVService.obtenerDesignacionPorPersonaProceso(personaId, procesoId);
            if (designacion == null || designacion.getMesa() == null
                    || !esCargoPresidenteMesa(designacion.getCargoNombre())) {
                return null;
            }
            sincronizarResponsableMesa(designacion.getMesa());
            return designacion.getMesa();
        } catch (Exception e) {
            log.warn("NO SE PUDO RESOLVER MESA POR DESIGNACION JRV", e);
            return null;
        }
    }

    private void sincronizarResponsableMesa(MesaDTO mesa) {
        if (mesa == null || mesa.getId() == null || loginBean == null
                || loginBean.getUserName() == null || loginBean.getUserName().isBlank()) {
            return;
        }
        if (!loginBean.getUserName().equals(mesa.getResponsable())) {
            MesaDTO mesaActualizada = mesaService.asignarResponsable(mesa.getId(), loginBean.getUserName());
            if (mesaActualizada != null) {
                mesa.setResponsable(mesaActualizada.getResponsable());
            }
        }
    }

    /**
     * Cambio de cantón: recarga las parroquias de ese cantón, invalida la parroquia
     * elegida y refiltra el listado en memoria.
     */
    public void cargaParroquiasPorCanton() {
        try {
            limpiarSeleccionMesa();
            parroquias = new ArrayList<>();
            listaRecintos = new ArrayList<>();
            listaMesas = new ArrayList<>();
            parroquiaSeleccionado = new Geograp();
            if (cantonSeleccionado.getId() != null) {
                this.cantonSeleccionado = geograpBean.getById(this.cantonSeleccionado.getId());
                this.parroquias = geograpBean.getByFatherGeograp(this.cantonSeleccionado);
            }
        } catch (Exception e) {
            log.warn("NO SE PUDO CARGAR PARROQUIAS", e);
        }
        aplicarFiltrosEscrutinio();
    }

    /** Cambio de parroquia: solo refiltra el listado ya cargado. */
    public void cambiarParroquiaEscrutinio() {
        limpiarSeleccionMesa();
        aplicarFiltrosEscrutinio();
    }

    public void cargaMesasPorRecintos() {
        limpiarSeleccionMesa();
        listaMesas = new ArrayList<>();
        if (recintoSeleccionado != null && recintoSeleccionado.getId() != null) {
            recintoSeleccionado = recintoService.obtenerDTOPorId(recintoSeleccionado.getId());
            this.listaMesas = filtrarMesasPorRecintoId(mesaService.listarDTOs(), recintoSeleccionado.getId());
        } else if (listaRecintos != null && !listaRecintos.isEmpty()) {
            List<Integer> recintoIds = new ArrayList<>();
            for (RecintoDTO r : listaRecintos) {
                recintoIds.add(r.getId());
            }
            this.listaMesas = filtrarMesasPorRecintoIds(mesaService.listarDTOs(), recintoIds);
        }
    }

    public void cargaDatosMesaSeleccionada() {
        cargaDatosMesaSeleccionada(true);
    }

    /** Cabecera de una mesa aún sin apertura: solo para mostrar, nunca se persiste desde aquí. */
    private EscrutinioCabeceraDTO cabeceraPendienteEnMemoria(Integer procesoId) {
        if (procesoId == null) {
            return null;
        }
        EscrutinioCabeceraDTO pendiente = new EscrutinioCabeceraDTO();
        pendiente.setMesa(mesaSeleccionado);
        pendiente.setProcesoId(procesoId);
        pendiente.setEstadoEscrutinio(EstadoEscrutinio.PENDIENTE);
        pendiente.setTotalSufragantes(totalSufragantesAsignados);
        pendiente.setTotalVotosRegistrados(0);
        pendiente.setTotalVotosValidos(0);
        pendiente.setTotalVotosBlancos(0);
        pendiente.setTotalVotosNulos(0);
        return pendiente;
    }

    /**
     * @param avisarMesaCerrada false al abrir la mesa desde el listado: la etiqueta «Cerrada» de
     *                          la cabecera ya lo indica y el aviso sería redundante.
     */
    private void cargaDatosMesaSeleccionada(boolean avisarMesaCerrada) {
        if (mesaSeleccionado == null || mesaSeleccionado.getId() == null) {
            limpiarSeleccionMesa();
            return;
        }
        if (!puedeGestionarMesa(mesaSeleccionado.getId())) {
            limpiarActa();
            JsfUtil.addErrorMessageFromBundle("actaE.mensaje.mesa.no.autorizada");
            return;
        }
        mesaSeleccionado = mesaService.obtenerDTOPorId(mesaSeleccionado.getId());
        cargarTotalSufragantes();
        Integer procesoId = (procesoActivo != null) ? procesoActivo.getId() : null;
        List<Integer> categoriaIds = new ArrayList<>();
        if (categoriasVotos != null) {
            for (CategoriaVoto c : categoriasVotos) {
                categoriaIds.add(c.getId());
            }
        }
        this.listaCamposActaE = escrutinioService.prepararActaPorMesaDTO(
                mesaSeleccionado.getId(), procesoId, categoriaIds);
        // Elegir una mesa no escribe nada: la cabecera real se crea al registrar la apertura
        // (abrirMesa), después de validar junta, padrón y proceso. Mientras no exista se usa una
        // cabecera PENDIENTE en memoria; así una mesa sin padrón no deja registros huérfanos.
        EscrutinioCabeceraDTO existente = escrutinioService.buscarCabeceraDTO(mesaSeleccionado.getId(), procesoId);
        escrutinioCabecera = existente != null
                ? escrutinioService.obtenerOCrearCabeceraDTO(mesaSeleccionado.getId(), procesoId, totalSufragantesAsignados)
                : cabeceraPendienteEnMemoria(procesoId);
        if (escrutinioCabecera != null && escrutinioCabecera.getObservacionApertura() != null) {
            observacionApertura = escrutinioCabecera.getObservacionApertura();
        }
        if (avisarMesaCerrada && isMesaCerrada()) {
            JsfUtil.addInfoMessageFromBundle("actaE.mensaje.mesa.cerrada");
        }
        cargarBloqueoApertura();
        cargarDocumentosActa();
        actualizarActaFisicaEnListado(mesaSeleccionado.getId(), actaFisica);
        pasoVisible = getProgresoEscrutinio().getIndice();
    }

    /**
     * Carga el listado completo de escrutinios del proceso vigente con cuatro consultas
     * en total —mesas, cabeceras, sufragantes y actas—, sin ninguna consulta por fila.
     * Los filtros de cantón, parroquia, estado y búsqueda trabajan después en memoria
     * sobre esta lista, así que cambiar un filtro no vuelve a consultar la base.
     */
    public void consultarEscrutiniosGerenciales() {
        filasEscrutinioProceso = new ArrayList<>();
        if (!usuarioConsultaGerencial) {
            listaConsultaGerencial = new ArrayList<>();
            limpiarResumenGerencial();
            JsfUtil.addErrorMessageFromBundle("actaE.mensaje.accesoDenegado");
            return;
        }
        Integer procesoId = procesoConsultaId != null ? procesoConsultaId
                : (procesoActivo != null ? procesoActivo.getId() : null);
        if (procesoId == null) {
            listaConsultaGerencial = new ArrayList<>();
            limpiarResumenGerencial();
            JsfUtil.addWarningMessageFromBundle("actaE.mensaje.sin.proceso");
            return;
        }
        List<MesaDTO> mesas = mesaService.listarDTOs();
        if (mesas == null) {
            mesas = new ArrayList<>();
        }
        List<Integer> mesaIds = new ArrayList<>();
        for (MesaDTO mesa : mesas) {
            if (mesa != null && mesa.getId() != null) {
                mesaIds.add(mesa.getId());
            }
        }
        Map<Integer, EscrutinioCabeceraDTO> cabeceras = escrutinioService.buscarCabecerasDTOPorProceso(procesoId);
        Map<Integer, Long> sufragantes = padronService.contarSufragantesPorMesas(mesaIds, procesoId);
        // Solo las mesas del proceso: con padrón, o con escrutinio ya iniciado (para no ocultar
        // actividad registrada). Las demás no pueden abrirse y no cuentan en el avance.
        mesaIds.removeIf(id -> !sufragantes.containsKey(id) && !cabeceras.containsKey(id));
        mesas.removeIf(mesa -> mesa == null || mesa.getId() == null || !mesaIds.contains(mesa.getId()));
        mesasValidadasProceso = new java.util.HashSet<>(resumenEscrutinioService.mesasValidadas(procesoId, mesaIds));
        Map<Integer, List<Documentos>> actas = documentoBean.getDocumentosPorEntidadesYTipoDoc(
                mesaIds, Constantes.ACTA_ESCRUTINIO);
        // Acta física vigente de todas las mesas: una consulta más en total, no una por mesa.
        Map<Integer, Documentos> actasFisicas = isUsuarioRevisorActas()
                ? actaFisicaEscrutinioService.obtenerVigentesPorMesas(mesaIds, procesoId)
                : new HashMap<>();
        for (MesaDTO mesa : mesas) {
            if (mesa == null || mesa.getId() == null) {
                continue;
            }
            ActaEGerencialDTO fila = construirFilaGerencial(mesa, procesoId, cabeceras, sufragantes, actas);
            Documentos fisica = actasFisicas.get(mesa.getId());
            fila.setActaFisicaEstado(fisica != null
                    ? (fisica.getEstadoRevision() != null ? fisica.getEstadoRevision()
                            : ActaFisicaEscrutinioService.PENDIENTE_REVISION)
                    : null);
            filasEscrutinioProceso.add(fila);
        }
        aplicarFiltrosEscrutinio();
    }

    /**
     * Refleja en memoria el acta física recién cargada, sin volver a consultar el listado.
     */
    private void actualizarActaFisicaEnListado(Integer mesaId, Documentos fisica) {
        if (mesaId == null || filasEscrutinioProceso == null) {
            return;
        }
        for (ActaEGerencialDTO fila : filasEscrutinioProceso) {
            if (mesaId.equals(fila.getMesaId())) {
                if (escrutinioCabecera != null && mesaSeleccionado != null
                        && mesaId.equals(mesaSeleccionado.getId())) {
                    fila.setEstadoEscrutinio(escrutinioCabecera.getEstadoEscrutinio());
                    fila.setFechaApertura(escrutinioCabecera.getFechaApertura());
                    fila.setFechaCierre(escrutinioCabecera.getFechaCierre());
                    fila.setVotosRegistrados(valorEntero(escrutinioCabecera.getTotalVotosRegistrados()));
                    fila.setVotosValidos(valorEntero(escrutinioCabecera.getTotalVotosValidos()));
                    fila.setVotosBlancos(valorEntero(escrutinioCabecera.getTotalVotosBlancos()));
                    fila.setVotosNulos(valorEntero(escrutinioCabecera.getTotalVotosNulos()));
                }
                fila.setActaFisicaEstado(fisica == null ? null
                        : (fisica.getEstadoRevision() != null ? fisica.getEstadoRevision()
                                : ActaFisicaEscrutinioService.PENDIENTE_REVISION));
            }
        }
        aplicarFiltrosEscrutinio();
        refiltrarTablaEscrutinios();
    }

    public ProgresoEscrutinioDTO.Etapa[] getEtapasEscrutinio() {
        return ProgresoEscrutinioDTO.Etapa.values();
    }

    /**
     * Filtro exacto de etapa actual, no acumulativo. Solo usa datos ya cargados.
     */
    public List<jakarta.faces.model.SelectItem> getOpcionesFiltroSituacion() {
        List<jakarta.faces.model.SelectItem> opciones = new ArrayList<>();
        for (ProgresoEscrutinioDTO.Etapa etapa : getEtapasEscrutinio()) {
            opciones.add(new jakarta.faces.model.SelectItem(etapa.name(), Constantes.getMensaje(etapa.getClave())));
        }
        return opciones;
    }

    /** filterFunction de la columna Situación (ver {@link #getOpcionesFiltroSituacion()}). */
    public boolean filtrarPorSituacion(Object valor, Object filtro, Locale locale) {
        String clave = filtro == null ? "" : filtro.toString().trim();
        if (clave.isEmpty()) {
            return true;
        }
        if (!(valor instanceof ActaEGerencialDTO fila)) {
            return false;
        }
        return fila.getProgreso().coincideFiltro(clave);
    }

    /** Recintos presentes en el listado ya filtrado por cantón y parroquia, sin consultar. */
    public List<String> getOpcionesFiltroRecinto() {
        java.util.TreeSet<String> recintos = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (listaConsultaGerencial != null) {
            for (ActaEGerencialDTO fila : listaConsultaGerencial) {
                if (fila.getRecinto() != null && !fila.getRecinto().isBlank()) {
                    recintos.add(fila.getRecinto());
                }
            }
        }
        return new ArrayList<>(recintos);
    }

    /** Quita los filtros de columna y vuelve a la primera página. */
    private void limpiarFiltrosTabla() {
        filtroSituacionTabla = null;
        filtroRecintoTabla = null;
        jakarta.faces.context.FacesContext contexto = jakarta.faces.context.FacesContext.getCurrentInstance();
        jakarta.faces.component.UIComponent tabla = contexto == null ? null
                : contexto.getViewRoot().findComponent(ID_TABLA_ESCRUTINIOS);
        if (tabla instanceof org.primefaces.component.datatable.DataTable dataTable) {
            dataTable.reset();
        }
    }

    /**
     * Aplica en memoria los filtros visibles: cantón, parroquia, estado y búsqueda por
     * recinto o mesa. No consulta la base.
     */
    public void aplicarFiltrosEscrutinio() {
        Integer cantonId = cantonSeleccionado != null ? cantonSeleccionado.getId() : null;
        Integer parroquiaId = parroquiaSeleccionado != null ? parroquiaSeleccionado.getId() : null;
        String texto = busquedaEscrutinio == null ? "" : busquedaEscrutinio.trim().toLowerCase(Locale.ROOT);
        List<ActaEGerencialDTO> resultado = new ArrayList<>();
        for (ActaEGerencialDTO fila : filasEscrutinioProceso) {
            if (cantonId != null && !cantonId.equals(fila.getCantonId())) {
                continue;
            }
            if (parroquiaId != null && !parroquiaId.equals(fila.getParroquiaId())) {
                continue;
            }
            if (estadoFiltro != null && !estadoFiltro.equals(fila.getEstadoEscrutinio())) {
                continue;
            }
            if (!texto.isEmpty() && !contieneTexto(fila.getRecinto(), texto)
                    && !contieneTexto(fila.getMesa(), texto)) {
                continue;
            }
            resultado.add(fila);
        }
        listaConsultaGerencial = resultado;
        calcularResumenGerencial();
    }

    private static boolean contieneTexto(String valor, String texto) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(texto);
    }

    /** Búsqueda y cambio de estado: solo refiltran lo ya cargado. */
    public void buscarEnListadoEscrutinios() {
        aplicarFiltrosEscrutinio();
    }

    public void limpiarFiltrosGerenciales() {
        if (!usuarioConsultaGerencial) {
            return;
        }
        cantonSeleccionado = new Geograp();
        parroquiaSeleccionado = new Geograp();
        recintoSeleccionado = new RecintoDTO();
        mesaSeleccionado = new MesaDTO();
        estadoFiltro = null;
        busquedaEscrutinio = null;
        limpiarFiltrosTabla();
        procesoConsultaId = procesoActivo != null ? procesoActivo.getId() : null;
        // Recupera la provincia operativa y sus cantones, no deja la geografía vacía.
        cargarGeografiaOperativa();
        parroquias = new ArrayList<>();
        listaRecintos = new ArrayList<>();
        listaMesas = new ArrayList<>();
        limpiarSeleccionMesa();
        // Sin filtros, el listado vuelve a mostrar todas las mesas ya cargadas.
        aplicarFiltrosEscrutinio();
    }

    public void registrarApertura() {
        try {
            if (!mesaSeleccionadaValida()) {
                JsfUtil.addWarningMessageFromBundle("actaE.mensaje.seleccione.mesa");
                return;
            }
            if (!puedeGestionarMesa(mesaSeleccionado.getId())) {
                JsfUtil.addErrorMessageFromBundle("actaE.mensaje.mesa.no.autorizada");
                return;
            }
            Integer procesoId = procesoActivo != null ? procesoActivo.getId() : null;
            escrutinioCabecera = escrutinioService.abrirEscrutinioDTO(mesaSeleccionado.getId(), procesoId,
                    loginBean.getUserName(), observacionApertura, totalSufragantesAsignados);
            procesoBean.okActivityRegister("APERTURA MESA " + mesaSeleccionado.getNombre(), mesaSeleccionado.getId().toString());
            JsfUtil.addSuccessMessageFromBundle("actaE.mensaje.apertura.ok");
            cargaDatosMesaSeleccionada();
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("ERROR AL REGISTRAR APERTURA DE MESA", e);
            JsfUtil.addErrorMessageFromBundle("actaE.mensaje.error");
        }
    }

    public void guardarBorradorConteo() {
        if (!validarOperacionConteo(false)) {
            return;
        }
        try {
            Integer procesoId = procesoActivo != null ? procesoActivo.getId() : null;
            escrutinioCabecera = escrutinioService.guardarBorradorConteoDTO(
                    mesaSeleccionado.getId(), procesoId, listaCamposActaE, totalSufragantesAsignados);
            procesoBean.okActivityRegister("GUARDA BORRADOR ACTA MESA " + mesaSeleccionado.getNombre(),
                    mesaSeleccionado.getId().toString());
            if (escrutinioCabecera != null && escrutinioCabecera.getObservacionConteo() != null
                    && !escrutinioCabecera.getObservacionConteo().isBlank()) {
                JsfUtil.addWarningMessage("Conteo guardado con observacion: " + escrutinioCabecera.getObservacionConteo());
            } else {
                JsfUtil.addSuccessMessageFromBundle("actaE.mensaje.borrador.ok");
            }
            cargaDatosMesaSeleccionada();
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("ERROR AL GUARDAR BORRADOR DE ACTA", e);
            JsfUtil.addErrorMessageFromBundle("actaE.mensaje.error");
        }
    }

    public void cerrarMesa() {
        if (this.listaCamposActaE == null || this.listaCamposActaE.isEmpty()
                || mesaSeleccionado == null || mesaSeleccionado.getId() == null) {
            return;
        }
        if (!validarOperacionConteo(true)) {
            return;
        }
        try {
            Integer procesoId = procesoActivo != null ? procesoActivo.getId() : null;
            MesaDTO mesaCerrada = escrutinioService.guardarActaCompletaDTO(
                    mesaSeleccionado.getId(), listaCamposActaE);
            if (mesaCerrada != null) {
                mesaSeleccionado = mesaCerrada;
            }
            escrutinioCabecera = escrutinioService.obtenerOCrearCabeceraDTO(
                    mesaSeleccionado.getId(), procesoId, totalSufragantesAsignados);
            cargaDatosMesaSeleccionada();
            // Cerrada la mesa, el paso siguiente del flujo es cargar y contrastar el acta física.
            tabActivoActaE = indiceTab(TAB_ESCRUTINIO);
            if (escrutinioCabecera != null && escrutinioCabecera.getObservacionConteo() != null
                    && !escrutinioCabecera.getObservacionConteo().isBlank()) {
                JsfUtil.addWarningMessageFromBundle("actaE.mensaje.cierre.con.observacion");
            } else {
                JsfUtil.addSuccessMessageFromBundle("actaE.mensaje.cierre.ok");
            }
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("ERROR AL CERRAR MESA", e);
            JsfUtil.addErrorMessageFromBundle("actaE.mensaje.error");
        }
    }

    public void cargarActaFisica() {
        try {
            // Presidente (su mesa), operadores y revisores. El servicio vuelve a validar
            // autoría, mesa cerrada y que el acta vigente no esté ya validada.
            if (!mesaSeleccionadaValida() || !isMesaCerrada()
                    || !(isUsuarioRevisorActas() || puedeGestionarMesa(mesaSeleccionado.getId()))) {
                log.warn("ACTA_FISICA causa=CONTEXTO_VISTA_NO_AUTORIZADO");
                JsfUtil.addErrorMessageFromBundle("actaE.actaFisica.no.autorizada");
                return;
            }
            if (archivoActaFisica == null || archivoActaFisica.getContent() == null) {
                log.warn("ACTA_FISICA causa=ARCHIVO_NO_RECIBIDO");
                JsfUtil.addWarningMessageFromBundle("actaE.actaFisica.archivo.requerido");
                return;
            }
            actaFisica = actaFisicaEscrutinioService.cargar(mesaSeleccionado.getId(), procesoActivo.getId(),
                    archivoActaFisica.getFileName(),
                    archivoActaFisica.getContentType(), archivoActaFisica.getContent());
            archivoActaFisica = null;
            contenidoActaFisica = null;
            pasoVisible = 4;
            verActaFisica();
            org.primefaces.PrimeFaces.current().ajax().update("frmActaE:tabActaE:contenidoEtapa");
            actualizarActaFisicaEnListado(mesaSeleccionado.getId(), actaFisica);
            actualizarIndicadoresProgreso();
            if (usuarioConsultaGerencial) {
                // La situación de la mesa cambió: se refresca su fila sin volver a consultar.
                org.primefaces.PrimeFaces.current().ajax().update(
                        "frmActaE:tabActaE:tblConsultaGerencial", "frmActaE:tabActaE:pnlResumenGerencial");
            }
            JsfUtil.addSuccessMessageFromBundle("actaE.actaFisica.cargada");
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("ACTA_FISICA causa=CONTROLLER; excepcion={}",
                    ec.com.antenasur.security.qr.DiagnosticoQr.tipoExcepcion(e));
            JsfUtil.addErrorMessageFromBundle("actaE.actaFisica.error");
        }
    }

    /**
     * CÓDIGO HEREDADO: genera el acta PDF antigua del escrutinio (tipo de documento
     * {@link Constantes#ACTA_ESCRUTINIO}). Ninguna vista la invoca: el acta oficial de la
     * mesa se genera en Docs. mesa (reportesMesa, ACTA_PARCIAL) con sus propias reglas.
     * Se conserva solo porque existen PDF ya generados que actaE, el dashboard y mesas
     * siguen listando y descargando. Candidata a retirarse tras confirmar en la base que
     * no hay documentos tipo 1 recientes. No volver a exponerla: un PDF generado antes
     * de la revisión oficial podría contradecir los datos validados.
     */
    public void generarActaMesaCerrada() {
        try {
            if (!mesaSeleccionadaValida()) {
                JsfUtil.addWarningMessageFromBundle("actaE.mensaje.seleccione.mesa");
                return;
            }
            if (!puedeGestionarMesa(mesaSeleccionado.getId())) {
                JsfUtil.addErrorMessageFromBundle("actaE.mensaje.mesa.no.autorizada");
                return;
            }
            cargarDocumentosActa();
            if (!isPuedeGenerarActaPdf()) {
                if (!isMesaCerrada()) {
                    JsfUtil.addWarningMessageFromBundle("actaE.mensaje.mesa.no.cerrada");
                } else {
                    JsfUtil.addInfoMessageFromBundle("actaE.mensaje.pdf.ya.existe");
                }
                return;
            }
            if (listaCamposActaE == null || listaCamposActaE.isEmpty()) {
                JsfUtil.addWarningMessageFromBundle("actaE.mensaje.sin.categorias");
                return;
            }
            ReportTemplateController documentoActaE = inicializaReporte();
            getListaStringDatos(documentoActaE);
            String observacion = escrutinioCabecera != null && escrutinioCabecera.getObservacionCierre() != null
                    ? escrutinioCabecera.getObservacionCierre() : "";
            exportaPDF(documentoActaE, observacion);
            cargarDocumentosActa();
            procesoBean.okActivityRegister("REGENERA ACTA PDF " + documentoActaE.getNombreReporte(),
                    "MESA " + mesaSeleccionado.getId());
            JsfUtil.addSuccessMessageFromBundle("actaE.mensaje.pdf.regenerado");
        } catch (Exception e) {
            log.error("ERROR AL REGENERAR ACTA DE MESA CERRADA", e);
            JsfUtil.addErrorMessageFromBundle("actaE.mensaje.pdf.error.regenerar");
        }
    }

    public void observarEscrutinio() {
        cambiarEstadoAdministrativo(EstadoEscrutinio.OBSERVADO, "actaE.mensaje.observar.ok");
    }

    public void anularEscrutinio() {
        cambiarEstadoAdministrativo(EstadoEscrutinio.ANULADO, "actaE.mensaje.anular.ok");
    }

    public void reabrirEscrutinio() {
        cambiarEstadoAdministrativo(EstadoEscrutinio.REABIERTO, "actaE.mensaje.reabrir.ok");
    }

    private void cambiarEstadoAdministrativo(EstadoEscrutinio estadoNuevo, String mensajeOk) {
        try {
            if (!mesaSeleccionadaValida()) {
                JsfUtil.addWarningMessageFromBundle("actaE.mensaje.seleccione.mesa");
                return;
            }
            if (!puedeCambiarEstadoAdministrativo(estadoNuevo)) {
                JsfUtil.addErrorMessageFromBundle("actaE.mensaje.accesoDenegado");
                return;
            }
            if (requiereMotivo(estadoNuevo) && (motivoCambioEstado == null || motivoCambioEstado.trim().isEmpty())) {
                JsfUtil.addWarningMessageFromBundle("actaE.mensaje.motivo.requerido");
                return;
            }
            EstadoEscrutinio estadoAnterior = escrutinioCabecera != null
                    ? escrutinioCabecera.getEstadoEscrutinio() : null;
            Integer procesoId = procesoActivo != null ? procesoActivo.getId() : null;
            escrutinioCabecera = escrutinioService.cambiarEstadoCabeceraDTO(
                    mesaSeleccionado.getId(), procesoId, estadoNuevo, motivoCambioEstado);
            auditarCambioEstado(estadoAnterior, estadoNuevo, motivoCambioEstado);
            motivoCambioEstado = "";
            JsfUtil.addSuccessMessageFromBundle(mensajeOk);
            cargaDatosMesaSeleccionada();
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("ERROR AL CAMBIAR ESTADO DE ESCRUTINIO", e);
            JsfUtil.addErrorMessageFromBundle("actaE.mensaje.estado.error");
        }
    }

    private ReportTemplateController inicializaReporte() {
        Integer procesoId = procesoActivo != null ? procesoActivo.getId() : 0;
        return new ReportTemplateController(
                "ACTA-" + procesoId + "-M" + mesaSeleccionado.getId() + "-" + JsfUtil.getFechaStringYYYYMMddHHmm(new Date()),
                new float[]{20, 100, 40},
                new int[]{1200, 3000, 4000},
                new String[]{"Nro", "CATEGORIA", "TOTAL VOTOS"},
                TAMANIO_LETRA);
    }

    private void getListaStringDatos(ReportTemplateController documentoActaE) {
        try {
            if (listaCamposActaE == null) {
                return;
            }
            documentoActaE.setListaDatos(new String[listaCamposActaE.size() + 1][documentoActaE.getNumeroColumnas()]);
            int fila = 0;
            int totalVotos = 0;
            for (EscrutinioDTO item : listaCamposActaE) {
                documentoActaE.getListaDatos()[fila][0] = String.valueOf(fila + 1);
                documentoActaE.getListaDatos()[fila][1] = item.getCategoriaNombre() != null ? item.getCategoriaNombre() : "";
                documentoActaE.getListaDatos()[fila][2] = item.getTotalVotos() != null ? item.getTotalVotos().toString() : "0";
                totalVotos += (item.getTotalVotos() != null ? item.getTotalVotos() : 0);
                fila++;
            }
            documentoActaE.getListaDatos()[fila][0] = "";
            documentoActaE.getListaDatos()[fila][1] = "TOTAL";
            documentoActaE.getListaDatos()[fila][2] = String.valueOf(totalVotos);
        } catch (Exception e) {
            log.error("ERROR AL OBTENER LISTA DE DATOS REPORTE " + documentoActaE.getNombreReporte(), e);
        }
    }

    /**
     * Construye el HashMap de parámetros del acta. Resuelve la cadena
     * provincia/cantón/parroquia por id contra GeograpBean en lugar de
     * navegar relaciones lazy de la entidad Mesa.
     */
    private HashMap<String, String> getDatosActaE() {
        try {
            Date fechaActual = new Date();
            HashMap<String, String> parametros = new HashMap<>();
            if (mesaSeleccionado == null) {
                return parametros;
            }
            cargarResponsablesJRV(parametros);

            Geograp parroquia = (obtenerParroquiaId(mesaSeleccionado) != null)
                    ? geograpBean.getById(obtenerParroquiaId(mesaSeleccionado)) : null;
            Geograp canton = (parroquia != null) ? parroquia.getGeograp() : null;
            Geograp provincia = (canton != null) ? canton.getGeograp() : null;

            parametros.put("nombreProvinica", provincia != null ? provincia.getName() : "");
            parametros.put("nombreCanton", canton != null ? canton.getName() : "");
            parametros.put("nombreParroquia", parroquia != null ? parroquia.getName() : "");
            parametros.put("fechaActa", JsfUtil.getFechaParaActas(fechaActual));
            parametros.put("horaRegistro", JsfUtil.getHoraStringHHmmss(fechaActual));

            Integer recintoId = (mesaSeleccionado.getRecinto() != null) ? mesaSeleccionado.getRecinto().getId() : null;
            String recintoNombre = (mesaSeleccionado.getRecinto() != null) ? mesaSeleccionado.getRecinto().getNombre() : "";
            parametros.put("numeroJunta", recintoId != null ? recintoId.toString() : "");
            parametros.put("numeroMesa", mesaSeleccionado.getId().toString());
            parametros.put("nombreRecinto", recintoNombre);
            parametros.put("fechaRegistro", JsfUtil.getFechaStringddMMYY(fechaActual));
            return parametros;
        } catch (Exception e) {
            log.error("ERROR EN INICIALIZAR VARIABLES", e);
            return null;
        }
    }

    private void cargarResponsablesJRV(HashMap<String, String> parametros) {
        inicializarResponsablesJRV(parametros);
        Integer mesaId = mesaSeleccionado != null ? mesaSeleccionado.getId() : null;
        Integer procesoId = procesoActivo != null ? procesoActivo.getId() : null;
        if (mesaId == null || procesoId == null) {
            return;
        }
        List<MiembroJRVDTO> miembros = miembroJRVService.listarDTOsPorMesaProceso(mesaId, procesoId);
        if (miembros == null) {
            return;
        }
        for (MiembroJRVDTO miembro : miembros) {
            if (miembro == null || miembro.getCargoNombre() == null) {
                continue;
            }
            String cargo = miembro.getCargoNombre().trim().toUpperCase();
            if (cargo.contains("PRESIDENTE")) {
                asignarResponsableJRV(parametros, "Presidente", miembro);
            } else if (cargo.contains("SECRETARIO")) {
                asignarResponsableJRV(parametros, "Secretario", miembro);
            } else if (cargo.contains("TESORERO") || cargo.contains("TESOREO")) {
                asignarResponsableJRV(parametros, "Tesorero", miembro);
            } else if (cargo.contains("VOCAL")) {
                asignarResponsableJRV(parametros, "Vocal", miembro);
            }
        }
    }

    private void inicializarResponsablesJRV(HashMap<String, String> parametros) {
        asignarResponsableVacio(parametros, "Presidente");
        asignarResponsableVacio(parametros, "Secretario");
        asignarResponsableVacio(parametros, "Tesorero");
        asignarResponsableVacio(parametros, "Vocal");
        // Alias historico con error ortografico para plantillas existentes.
        parametros.put("nombreTesoreo", "");
        parametros.put("documentoTesoreo", "");
        parametros.put("cargoTesoreo", "TESORERO");
    }

    private void asignarResponsableVacio(HashMap<String, String> parametros, String dignidad) {
        parametros.put("nombre" + dignidad, "");
        parametros.put("documento" + dignidad, "");
        parametros.put("cargo" + dignidad, dignidad.toUpperCase());
        parametros.put("iglesia" + dignidad, "");
    }

    private void asignarResponsableJRV(HashMap<String, String> parametros, String dignidad, MiembroJRVDTO miembro) {
        String nombre = "";
        String documento = "";
        String iglesia = "";
        if (miembro.getIglesiaPersona() != null) {
            if (miembro.getIglesiaPersona().getPersona() != null) {
                nombre = nombreCompletoPersona(miembro.getIglesiaPersona().getPersona());
                documento = textoNulo(miembro.getIglesiaPersona().getPersona().getDocumento());
            }
            if (miembro.getIglesiaPersona().getIglesia() != null) {
                iglesia = textoNulo(miembro.getIglesiaPersona().getIglesia().getNombre());
            }
        }
        parametros.put("nombre" + dignidad, nombre);
        parametros.put("documento" + dignidad, documento);
        parametros.put("cargo" + dignidad, textoNulo(miembro.getCargoNombre()));
        parametros.put("iglesia" + dignidad, iglesia);
        if ("Tesorero".equals(dignidad)) {
            parametros.put("nombreTesoreo", nombre);
            parametros.put("documentoTesoreo", documento);
            parametros.put("cargoTesoreo", textoNulo(miembro.getCargoNombre()));
        }
    }

    private String nombreCompletoPersona(ec.com.antenasur.dto.PersonaDTO persona) {
        if (persona == null) {
            return "";
        }
        String nombres = textoNulo(persona.getNombres());
        String apellidos = textoNulo(persona.getApellidos());
        return (nombres + " " + apellidos).trim();
    }

    private String textoNulo(String valor) {
        return valor != null ? valor.trim() : "";
    }

    public String getDirectorioActasEscrutinio() {
        return Constantes.getDirectorioActasEscrutinio();
    }

    public boolean isPuedeGenerarActaPdf() {
        return isMesaCerrada() && getDocumentoActaValido() == null;
    }

    public boolean isActaPdfRegistradaNoDisponible() {
        return isMesaCerrada() && documentosActa != null && !documentosActa.isEmpty() && getDocumentoActaValido() == null;
    }

    public Documentos getDocumentoActaValido() {
        if (documentosActa == null || documentosActa.isEmpty()) {
            return null;
        }
        for (int i = documentosActa.size() - 1; i >= 0; i--) {
            Documentos documento = documentosActa.get(i);
            if (esDocumentoActaValido(documento)) {
                return documento;
            }
        }
        return null;
    }

    public boolean esDocumentoActaValido(Documentos documento) {
        if (documento == null || documento.getPath() == null || documento.getPath().isBlank()) {
            return false;
        }
        try {
            Path path = RepositorioDocumentos.resolverRutaAlmacenada(documento.getPath());
            if (documento.getHashSha256() == null || documento.getHashSha256().isBlank()) {
                return true;
            }
            String hashActual = ReportePFD.calcularSha256(path);
            return documento.getHashSha256().equalsIgnoreCase(hashActual);
        } catch (Exception e) {
            log.warn("NO SE PUDO VALIDAR DOCUMENTO DE ACTA {}", documento.getPath(), e);
            return false;
        }
    }

    /** Votos emitidos más papeletas no utilizadas: todo lo registrado en el conteo. */
    public int getTotalVotosRegistrados() {
        return escrutinioService.calcularTotalVotos(listaCamposActaE);
    }

    /** Votos emitidos (válidos, nulos y blancos), sin las papeletas no utilizadas. */
    public int getVotosEmitidos() {
        return getTotalVotosRegistrados() - getPapeletasNoUtilizadas();
    }

    public int getPapeletasNoUtilizadas() {
        int papeletas = 0;
        if (listaCamposActaE != null) {
            for (EscrutinioDTO item : listaCamposActaE) {
                if (item != null && item.getTotalVotos() != null
                        && EscrutinioService.esCategoriaPapeletas(item.getCategoriaNombre())) {
                    papeletas += item.getTotalVotos();
                }
            }
        }
        return papeletas;
    }

    /** Cuadre de papeletas (0 = cuadra); mismo cálculo que guarda EscrutinioService. */
    public int getDiferenciaConteo() {
        return EscrutinioService.calcularCuadrePapeletas(
                totalSufragantesAsignados != null ? totalSufragantesAsignados : 0,
                getVotosEmitidos(), getPapeletasNoUtilizadas());
    }

    public boolean isMesaSeleccionadaValida() {
        return mesaSeleccionadaValida();
    }

    public boolean isMesaAbierta() {
        return mesaSeleccionadaValida() && escrutinioCabecera != null
                && (EstadoEscrutinio.ABIERTO.equals(escrutinioCabecera.getEstadoEscrutinio())
                || EstadoEscrutinio.EN_CONTEO.equals(escrutinioCabecera.getEstadoEscrutinio())
                || EstadoEscrutinio.CONTEO_REGISTRADO.equals(escrutinioCabecera.getEstadoEscrutinio())
                || EstadoEscrutinio.REABIERTO.equals(escrutinioCabecera.getEstadoEscrutinio()));
    }

    public boolean isMesaCerrada() {
        return mesaSeleccionadaValida() && escrutinioCabecera != null
                && EstadoEscrutinio.CERRADO.equals(escrutinioCabecera.getEstadoEscrutinio());
    }

    public boolean isPuedeRegistrarApertura() {
        return mesaSeleccionadaValida()
                && isPuedeOperarActa()
                && !sinMesaAsignada
                && !isMesaAbierta()
                && !isMesaCerrada()
                && motivoBloqueoApertura == null;
    }

    /** La apertura está pendiente y la mesa no cumple las condiciones para iniciarla. */
    public boolean isAperturaBloqueada() {
        return mesaSeleccionadaValida() && motivoBloqueoApertura != null;
    }

    /**
     * Solo se evalúa mientras la apertura está pendiente: una vez registrada, los cambios
     * posteriores en la junta o el padrón no bloquean el escrutinio en curso.
     */
    private void cargarBloqueoApertura() {
        motivoBloqueoApertura = null;
        if (!mesaSeleccionadaValida() || mesaSoloLectura || escrutinioCabecera == null
                || (escrutinioCabecera.getEstadoEscrutinio() != null
                        && !EstadoEscrutinio.PENDIENTE.equals(escrutinioCabecera.getEstadoEscrutinio()))) {
            return;
        }
        Integer procesoId = procesoActivo != null ? procesoActivo.getId() : null;
        motivoBloqueoApertura = disponibilidadDocumentoMesaService.motivoBloqueoApertura(
                procesoId, mesaSeleccionado.getId());
    }

    public boolean isPuedeEditarConteo() {
        return mesaSeleccionadaValida() && isPuedeOperarActa() && isMesaAbierta() && !isMesaCerrada();
    }

    public boolean isPuedeCerrarMesa() {
        return isPuedeEditarConteo()
                && escrutinioCabecera != null
                && (EstadoEscrutinio.EN_CONTEO.equals(escrutinioCabecera.getEstadoEscrutinio())
                || EstadoEscrutinio.CONTEO_REGISTRADO.equals(escrutinioCabecera.getEstadoEscrutinio())
                || EstadoEscrutinio.REABIERTO.equals(escrutinioCabecera.getEstadoEscrutinio()))
                && listaCamposActaE != null && !listaCamposActaE.isEmpty();
    }

    public String getEstadoValidacionCierreTexto() {
        if (isMesaCerrada()) {
            return JsfUtil.getProperty("actaE.validacion.cerrada", true);
        }
        return isPuedeCerrarMesa()
                ? JsfUtil.getProperty("actaE.validacion.lista", true)
                : JsfUtil.getProperty("actaE.validacion.pendiente", true);
    }

    public String getEstadoValidacionCierreSeverity() {
        if (isMesaCerrada()) {
            return "success";
        }
        return isPuedeCerrarMesa() ? "success" : "warning";
    }

    public String getEstadoMesaTexto() {
        if (isMesaCerrada()) {
            return JsfUtil.getProperty("actaE.estado.cerrada", true);
        }
        if (escrutinioCabecera != null && EstadoEscrutinio.OBSERVADO.equals(escrutinioCabecera.getEstadoEscrutinio())) {
            return JsfUtil.getProperty("actaE.estado.observada", true);
        }
        if (escrutinioCabecera != null && EstadoEscrutinio.ANULADO.equals(escrutinioCabecera.getEstadoEscrutinio())) {
            return JsfUtil.getProperty("actaE.estado.anulada", true);
        }
        if (escrutinioCabecera != null && EstadoEscrutinio.REABIERTO.equals(escrutinioCabecera.getEstadoEscrutinio())) {
            return JsfUtil.getProperty("actaE.estado.reabierta", true);
        }
        if (isMesaAbierta()) {
            return getTotalVotosRegistrados() > 0
                    ? JsfUtil.getProperty("actaE.estado.en.conteo", true)
                    : JsfUtil.getProperty("actaE.estado.abierta", true);
        }
        return JsfUtil.getProperty("actaE.estado.pendiente", true);
    }

    public String getEstadoMesaSeverity() {
        if (isMesaCerrada()) {
            return "success";
        }
        if (escrutinioCabecera != null && (EstadoEscrutinio.OBSERVADO.equals(escrutinioCabecera.getEstadoEscrutinio())
                || EstadoEscrutinio.ANULADO.equals(escrutinioCabecera.getEstadoEscrutinio()))) {
            return "danger";
        }
        if (isMesaAbierta()) {
            return "warning";
        }
        return "secondary";
    }

    private boolean validarOperacionConteo(boolean cierreFinal) {
        if (!mesaSeleccionadaValida()) {
            JsfUtil.addWarningMessageFromBundle("actaE.mensaje.seleccione.mesa");
            return false;
        }
        if (!puedeGestionarMesa(mesaSeleccionado.getId())) {
            JsfUtil.addErrorMessageFromBundle("actaE.mensaje.mesa.no.autorizada");
            return false;
        }
        if (!isMesaAbierta()) {
            JsfUtil.addWarningMessageFromBundle("actaE.mensaje.apertura.requerida");
            return false;
        }
        if (listaCamposActaE == null || listaCamposActaE.isEmpty()) {
            JsfUtil.addWarningMessageFromBundle("actaE.mensaje.sin.categorias");
            return false;
        }
        for (EscrutinioDTO item : listaCamposActaE) {
            if (item.getTotalVotos() != null && item.getTotalVotos() < 0) {
                JsfUtil.addErrorMessageFromBundle("actaE.mensaje.votos.negativos");
                return false;
            }
        }
        return true;
    }

    private boolean puedeGestionarMesa(Integer mesaId) {
        if (alcanceQr.contexto() != null) {
            alcanceQr.validar(mesaId, procesoActivo != null ? procesoActivo.getId() : null);
            return true;
        }
        if (!accesoRestringidoPresidenteMesa) {
            return tieneRolOperacionActa();
        }
        MesaDTO mesaUsuario = obtenerMesaPorUsuario();
        return mesaUsuario != null && mesaUsuario.getId() != null && mesaUsuario.getId().equals(mesaId);
    }

    private boolean esPresidenteMesa() {
        return loginBean != null && loginBean.getRoles() != null
                && loginBean.getRoles().contains("SITEC-Presidente-mesa");
    }

    private boolean esCargoPresidenteMesa(String cargoNombre) {
        return cargoNombre != null && cargoNombre.trim().toUpperCase().contains("PRESIDENTE");
    }

    public boolean isPuedeOperarActa() {
        return !sinMesaAsignada && (accesoRestringidoPresidenteMesa || tieneRolOperacionActa());
    }

    public boolean isPuedeObservarEscrutinio() {
        return mesaSeleccionadaValida() && tieneRolSupervisorOAdministrador()
                && escrutinioCabecera != null
                && !EstadoEscrutinio.CERRADO.equals(escrutinioCabecera.getEstadoEscrutinio())
                && !EstadoEscrutinio.ANULADO.equals(escrutinioCabecera.getEstadoEscrutinio())
                && !EstadoEscrutinio.OBSERVADO.equals(escrutinioCabecera.getEstadoEscrutinio());
    }

    public boolean isPuedeAnularEscrutinio() {
        return mesaSeleccionadaValida() && tieneRolAdministrador()
                && escrutinioCabecera != null
                && !EstadoEscrutinio.CERRADO.equals(escrutinioCabecera.getEstadoEscrutinio())
                && !EstadoEscrutinio.ANULADO.equals(escrutinioCabecera.getEstadoEscrutinio());
    }

    /**
     * Reabrir desde el control administrativo. Una mesa cerrada que ya tiene acta física
     * no se reabre aquí: se hace desde la revisión oficial («Devolver al presidente» o
     * «Revertir validación»), para que el motivo quede registrado en el acta física.
     */
    public boolean isPuedeReabrirEscrutinio() {
        return mesaSeleccionadaValida() && tieneRolAdministrador()
                && escrutinioCabecera != null
                && (EstadoEscrutinio.CERRADO.equals(escrutinioCabecera.getEstadoEscrutinio())
                || EstadoEscrutinio.OBSERVADO.equals(escrutinioCabecera.getEstadoEscrutinio())
                || EstadoEscrutinio.ANULADO.equals(escrutinioCabecera.getEstadoEscrutinio()))
                && !isReaperturaPorRevisionOficial();
    }

    /** Mesa cerrada con acta física: la reapertura corresponde a la revisión oficial. */
    public boolean isReaperturaPorRevisionOficial() {
        return mesaSeleccionadaValida() && tieneRolAdministrador() && isMesaCerrada() && actaFisica != null;
    }

    // ── Control administrativo (observar, anular, reabrir) ────────────────────────
    // Solo se ofrecen las acciones que admite el estado actual. El motivo se pide
    // después de elegir la acción, así queda claro a cuál corresponde.

    private static final String ADMIN_OBSERVAR = "OBSERVAR";
    private static final String ADMIN_ANULAR = "ANULAR";
    private static final String ADMIN_REABRIR = "REABRIR";

    /** Acción administrativa elegida y pendiente de motivo; null si no hay ninguna. */
    @Getter
    private String accionAdministrativa;

    public boolean isHayControlAdministrativo() {
        return isPuedeObservarEscrutinio() || isPuedeAnularEscrutinio() || isPuedeReabrirEscrutinio()
                || isReaperturaPorRevisionOficial();
    }

    public void prepararAccionAdministrativa(String accion) {
        boolean permitida = (ADMIN_OBSERVAR.equals(accion) && isPuedeObservarEscrutinio())
                || (ADMIN_ANULAR.equals(accion) && isPuedeAnularEscrutinio())
                || (ADMIN_REABRIR.equals(accion) && isPuedeReabrirEscrutinio());
        if (!permitida) {
            JsfUtil.addErrorMessageFromBundle("actaE.mensaje.accesoDenegado");
            return;
        }
        accionAdministrativa = accion;
        motivoCambioEstado = "";
    }

    public void cancelarAccionAdministrativa() {
        accionAdministrativa = null;
        motivoCambioEstado = "";
    }

    /** Ejecuta la acción elegida con los controles de siempre (permiso, motivo y transición). */
    public void confirmarAccionAdministrativa() {
        String accion = accionAdministrativa;
        EstadoEscrutinio antes = escrutinioCabecera != null ? escrutinioCabecera.getEstadoEscrutinio() : null;
        if (ADMIN_OBSERVAR.equals(accion)) {
            observarEscrutinio();
        } else if (ADMIN_ANULAR.equals(accion)) {
            anularEscrutinio();
        } else if (ADMIN_REABRIR.equals(accion)) {
            reabrirEscrutinio();
        } else {
            JsfUtil.addErrorMessageFromBundle("actaE.mensaje.accesoDenegado");
            return;
        }
        // Se cierra solo si el estado cambió; si falló (p. ej., motivo vacío) sigue abierta.
        EstadoEscrutinio despues = escrutinioCabecera != null ? escrutinioCabecera.getEstadoEscrutinio() : null;
        if (despues != antes) {
            accionAdministrativa = null;
        }
    }

    private boolean mesaSeleccionadaValida() {
        return mesaSeleccionado != null && mesaSeleccionado.getId() != null;
    }

    private void cargarTotalSufragantes() {
        if (!mesaSeleccionadaValida()) {
            totalSufragantesAsignados = 0;
            return;
        }
        Integer procesoId = procesoActivo != null ? procesoActivo.getId() : null;
        if (procesoId == null) {
            totalSufragantesAsignados = 0;
            return;
        }
        totalSufragantesAsignados = padronService.contarSufragantesPorMesaYProceso(
                mesaSeleccionado.getId(), procesoId);
    }

    private void cargarDocumentosActa() {
        documentosActa = new ArrayList<>();
        actaFisica = null;
        // Una transcripción o una acción con motivo pertenecen al acta que se revisaba.
        cancelarRevisionActa();
        cancelarAccionRevision();
        cotejoConfirmado = false;
        accionAdministrativa = null;
        if (!mesaSeleccionadaValida()) {
            return;
        }
        List<Documentos> documentos = documentoBean.getDocumentosPorEntidadYTipoDoc(
                mesaSeleccionado.getId(), Constantes.ACTA_ESCRUTINIO);
        if (documentos != null) {
            for (Documentos documento : documentos) {
                if (esDocumentoDelProcesoActivo(documento)) {
                    documentosActa.add(documento);
                }
            }
        }
        if (procesoActivo != null && procesoActivo.getId() != null) {
            actaFisica = actaFisicaEscrutinioService.obtenerVigente(mesaSeleccionado.getId(), procesoActivo.getId());
        }
    }

    private boolean esDocumentoDelProcesoActivo(Documentos documento) {
        if (documento == null || procesoActivo == null || procesoActivo.getId() == null
                || mesaSeleccionado == null || mesaSeleccionado.getId() == null) {
            return false;
        }
        return esDocumentoDelProceso(documento, mesaSeleccionado.getId(), procesoActivo.getId());
    }

    private void limpiarActa() {
        listaCamposActaE = new ArrayList<>();
        documentosActa = new ArrayList<>();
        actaFisica = null;
        contenidoActaFisica = null;
        mesaSoloLectura = false;
        mesaSeleccionado = new MesaDTO();
        escrutinioCabecera = new EscrutinioCabeceraDTO();
        totalSufragantesAsignados = 0;
        motivoBloqueoApertura = null;
    }

    private String getPlantillaDocumento(String nombrePlantilla) {
        try {
            HashMap<String, String> parametros = getDatosActaE();
            this.plantillaCorreoSeleccionado = plantillaCorreoService.buscarPorAsunto(nombrePlantilla);
            this.plantillaCorreoSeleccionado.setMensaje(plantillaCorreoSeleccionado.getMensaje().replaceAll("\\{|\\}", ""));
            this.plantillaCorreoSeleccionado.setMensaje(UtilHtml.builTextHTMLToMail(parametros, plantillaCorreoSeleccionado.getMensaje()));
            return this.plantillaCorreoSeleccionado.getMensaje();
        } catch (Exception e) {
            return null;
        }
    }

    /** CÓDIGO HEREDADO: solo lo usa {@link #generarActaMesaCerrada()}; ver su nota. */
    public String exportaPDF(ReportTemplateController documentoActaE, String observacion) throws Exception {
        alcanceQr.validar(mesaSeleccionado != null ? mesaSeleccionado.getId() : null,
                procesoActivo != null ? procesoActivo.getId() : null);
        cargarDocumentosActa();
        if (getDocumentoActaValido() != null) {
            throw new IllegalStateException("El acta PDF ya fue generada y validada para esta mesa.");
        }
        String txtContenidoActaE = getPlantillaDocumento("BIENVENIDO");
        String txtResponsableActaE = getPlantillaDocumento("RESPONSABLES ACTA ESCRUTINIOS");
        if (txtContenidoActaE == null || txtContenidoActaE.isBlank()
                || txtResponsableActaE == null || txtResponsableActaE.isBlank()) {
            throw new IllegalStateException("No se pudo resolver la plantilla del acta de escrutinio.");
        }

        String extencion = ".pdf";
        String pathCompleto = Constantes.getPathActaEscrutinio(documentoActaE.getNombreReporte());
        String codigoActa = documentoActaE.getNombreReporte();

        Documentos documentoNuevo = new Documentos(documentoActaE.getNombreReporte(), pathCompleto, new TipoDocumento(Constantes.ACTA_ESCRUTINIO),
                mesaSeleccionado.getId(), extencion, "application/pdf", codigoActa);

        String pathCss = Constantes.getHojaEstilo();
        float tamanioLetra = 10;
        Font fuenteCabecerta = Constantes.getFuenteCabeceraDefault(tamanioLetra);
        Font fuenteContenido = Constantes.getFuenteContenidoDefault(tamanioLetra);

            // Las familias de la hoja de estilo (montsR, montsSB, montsB) se resuelven a Montserrat
            // Regular, Medium y Bold; antes montsSB y montsB no estaban registradas y salían en Helvetica.
            FontProvider fontProvider = ec.com.antenasur.itext.TipografiaPdf.proveedorHtml();
            ReportePFD.nuevoPDF(documentoActaE.getNombreReporte());
            ReportePFD.agregaHTML(txtContenidoActaE, pathCss, fontProvider);
            ReportePFD.creaTablaCabecera(documentoActaE.getNumeroColumnas(), documentoActaE.getTamanioColumnasPDF(),
                    "RESULTADOS DEL ESCRUTINIO", documentoActaE.getNombresColumnas(), fuenteCabecerta);
        ReportePFD.creaContenidoTabla(documentoActaE.getListaDatos(), documentoActaE.getNombresColumnas(), fuenteContenido);
        ReportePFD.agregaParrafoEnBlanco();
        if (observacion != null && !observacion.isBlank()) {
            ReportePFD.agregaParrafoObservacion(observacion);
        }
        ReportePFD.agregaHTML(txtResponsableActaE, pathCss, fontProvider);
        ReportePFD.agregaCodigoVerificacion(codigoActa, construirContenidoQr(codigoActa));
        ReportePFD.cerrarDocumento();
        documentoNuevo.setHashSha256(ReportePFD.calcularHashSha256Actual());
        String archivoGenerado = ReportePFD.guardarDocumentosActasEObligatorio(documentoActaE.getNombreReporte());
        documentoNuevo.setPath(archivoGenerado);
        try {
            this.guardarDocumentoBD(documentoNuevo);
        } catch (Exception e) {
            RepositorioDocumentos.eliminarSilencioso(Path.of(archivoGenerado));
            throw e;
        }
        procesoBean.okActivityRegister("GENERA " + documentoActaE.getNombreReporte(), documentoActaE.getNombreReporte() + ".pdf");
        return archivoGenerado;
    }

    private Documentos guardarDocumentoBD(Documentos documentoNuevo) {
        Integer recintoId = mesaSeleccionado != null && mesaSeleccionado.getRecinto() != null
                ? mesaSeleccionado.getRecinto().getId() : null;
        Integer procesoId = procesoActivo != null ? procesoActivo.getId() : null;
        Documentos documentoPersistido = documentoBean.guardarDocumentoMesa(
                documentoNuevo, mesaSeleccionado.getId(), procesoId, recintoId);
        if (documentoPersistido == null || documentoPersistido.getId() == null) {
            throw new IllegalStateException("No se pudo registrar el documento generado.");
        }
        return documentoPersistido;
    }

    private String construirContenidoQr(String codigoActa) {
        StringBuilder contenido = new StringBuilder();
        contenido.append("codigo=").append(codigoActa);
        contenido.append(";proceso=").append(procesoActivo != null ? procesoActivo.getId() : "");
        contenido.append(";mesa=").append(mesaSeleccionado != null ? mesaSeleccionado.getId() : "");
        contenido.append(";recinto=").append(mesaSeleccionado != null && mesaSeleccionado.getRecinto() != null
                ? mesaSeleccionado.getRecinto().getId() : "");
        contenido.append(";fecha=").append(JsfUtil.getFechaStringYYYYMMddHHmm(new Date()));
        contenido.append(";usuario=").append(loginBean != null ? loginBean.getUserName() : "");
        return contenido.toString();
    }

    /**
     * Construye una fila del listado sin consultar: todo lo que necesita llega en los
     * mapas precargados y en el propio MesaDTO. Sin columna de provincia ni de
     * presidente de mesa, se evitan las dos consultas por fila que exigían.
     */
    private ActaEGerencialDTO construirFilaGerencial(MesaDTO mesa, Integer procesoId,
            Map<Integer, EscrutinioCabeceraDTO> cabeceras, Map<Integer, Long> sufragantes,
            Map<Integer, List<Documentos>> actas) {
        ActaEGerencialDTO fila = new ActaEGerencialDTO();
        fila.setMesaId(mesa.getId());
        fila.setMesa(textoNulo(mesa.getNombre()));
        RecintoDTO recinto = mesa.getRecinto();
        fila.setRecinto(recinto != null ? textoNulo(recinto.getNombre()) : "");
        fila.setParroquia(obtenerParroquiaNombre(mesa));
        fila.setCanton(obtenerCantonNombre(mesa));
        fila.setParroquiaId(obtenerParroquiaId(mesa));
        fila.setCantonId(obtenerCantonId(mesa));

        EscrutinioCabeceraDTO cabecera = cabeceras.get(mesa.getId());
        if (cabecera != null) {
            fila.setEstadoEscrutinio(cabecera.getEstadoEscrutinio() != null
                    ? cabecera.getEstadoEscrutinio() : EstadoEscrutinio.PENDIENTE);
            fila.setPresidenteMesa(textoNulo(cabecera.getPresidenteResponsable()));
            fila.setFechaApertura(cabecera.getFechaApertura());
            fila.setFechaCierre(cabecera.getFechaCierre());
            fila.setSufragantesAsignados(valorEntero(cabecera.getTotalSufragantes()));
            fila.setVotosRegistrados(valorEntero(cabecera.getTotalVotosRegistrados()));
            fila.setVotosValidos(valorEntero(cabecera.getTotalVotosValidos()));
            fila.setVotosBlancos(valorEntero(cabecera.getTotalVotosBlancos()));
            fila.setVotosNulos(valorEntero(cabecera.getTotalVotosNulos()));
        } else {
            fila.setEstadoEscrutinio(EstadoEscrutinio.PENDIENTE);
            Long total = sufragantes.get(mesa.getId());
            fila.setSufragantesAsignados(total == null ? 0
                    : (total > Integer.MAX_VALUE ? Integer.MAX_VALUE : total.intValue()));
            fila.setVotosRegistrados(0);
            fila.setVotosValidos(0);
            fila.setVotosBlancos(0);
            fila.setVotosNulos(0);
        }
        Documentos actaValida = seleccionarActaValida(actas.get(mesa.getId()), mesa.getId(), procesoId);
        fila.setDocumentoActa(actaValida);
        fila.setActaPdfGenerada(actaValida != null);
        return fila;
    }

    /**
     * Acta válida del proceso entre los documentos ya cargados de esa mesa. Aplica los
     * mismos criterios que la consulta unitaria, sin volver a la base.
     */
    private Documentos seleccionarActaValida(List<Documentos> documentos, Integer mesaId, Integer procesoId) {
        if (documentos == null || mesaId == null || procesoId == null) {
            return null;
        }
        for (Documentos documento : documentos) {
            if (esDocumentoDelProceso(documento, mesaId, procesoId) && esDocumentoActaValido(documento)) {
                return documento;
            }
        }
        return null;
    }

    /**
     * Acción principal del listado: toma la mesa de la fila como contexto de trabajo,
     * igual que hacían los combos de recinto y mesa. No amplía permisos: solo actúa si
     * el usuario ya puede operar el acta, y respeta la restricción del presidente de
     * mesa, que únicamente puede trabajar sobre la suya.
     */
    public void gestionarEscrutinioMesa(ActaEGerencialDTO fila) {
        if (fila == null || fila.getMesaId() == null) {
            return;
        }
        boolean opera = isPuedeOperarActa();
        boolean revisa = isUsuarioRevisorActas();
        if (!opera && !revisa) {
            JsfUtil.addErrorMessageFromBundle("actaE.mensaje.accesoDenegado");
            return;
        }
        if (accesoRestringidoPresidenteMesa
                && (mesaSeleccionado == null || !fila.getMesaId().equals(mesaSeleccionado.getId()))) {
            JsfUtil.addErrorMessageFromBundle("actaE.mensaje.accesoDenegado");
            return;
        }
        MesaDTO mesa = mesaService.obtenerDTOPorId(fila.getMesaId());
        if (mesa == null) {
            JsfUtil.addWarningMessageFromBundle("actaE.mensaje.sinResultados");
            return;
        }
        mesaSeleccionado = mesa;
        recintoSeleccionado = mesa.getRecinto() != null ? mesa.getRecinto() : new RecintoDTO();
        contenidoActaFisica = null;
        if (opera) {
            // Operador (Administrador o Tribunal): flujo de siempre, que prepara el acta para registrar.
            mesaSoloLectura = false;
            cargaDatosMesaSeleccionada(false);
        } else {
            // Revisor sin operación: consulta, sin crear cabecera ni conteo.
            cargarMesaSoloLectura();
        }
        // Los demás tabs trabajan sobre la mesa elegida: se abre su escrutinio.
        tabActivoActaE = indiceTab(TAB_ESCRUTINIO);
    }

    /**
     * Acción rápida del listado: abre la mesa directamente en el tab «Acta física» y, si
     * ya tiene acta, la muestra en el visor. Así Tribunal y Administrador cargan la que
     * falta o revisan la existente sin pasar por el resto de tabs.
     */
    public void abrirActaFisicaMesa(ActaEGerencialDTO fila) {
        gestionarEscrutinioMesa(fila);
        if (!isPuedeConsultarMesa()) {
            return;
        }
        tabActivoActaE = indiceTab(TAB_ESCRUTINIO);
        pasoVisible = actaFisica == null ? 3 : 4;
        if (actaFisica != null) {
            verActaFisica();
        }
    }

    private static final String TAB_MESAS = "mesas";
    private static final String TAB_ESCRUTINIO = "escrutinio";

    /**
     * Índice de un tab entre los que están visibles. PrimeFaces cuenta solo los tabs
     * renderizados, y cuáles lo están depende del rol y de la mesa elegida; este orden
     * y estas condiciones son exactamente los de actaE.xhtml.
     */
    private int indiceTab(String clave) {
        List<String> visibles = new ArrayList<>();
        if (usuarioConsultaGerencial) {
            visibles.add(TAB_MESAS);
        }
        if (isPuedeConsultarMesa()) {
            // Escrutinio (resumen, apertura, conteo y cierre) y Acta física.
            visibles.add(TAB_ESCRUTINIO);
        }
        int indice = visibles.indexOf(clave);
        return indice < 0 ? 0 : indice;
    }

    /** Índice del tab «Acta física», para el acceso directo desde Escrutinio. */
    public int getIndiceTabActaFisica() {
        return indiceTab(TAB_ESCRUTINIO);
    }

    /** Pasos del flujo de la mesa, en orden; sus rótulos son actaE.paso.&lt;paso&gt;. */
    private static final String[] PASOS_ESCRUTINIO = {"apertura", "conteo", "cierre", "actaFisica", "validacion"};

    public String[] getPasosEscrutinio() {
        return PASOS_ESCRUTINIO;
    }

    /**
     * Índice del paso en curso (0 a 4), o 5 cuando la mesa ya es dato oficial. Se deduce
     * de estados existentes: cabecera del escrutinio, acta física vigente y su revisión.
     */
    public int getPasoEscrutinio() {
        ProgresoEscrutinioDTO progreso = getProgresoEscrutinio();
        return progreso.isOficial() ? 5 : progreso.getIndice();
    }

    public ProgresoEscrutinioDTO getProgresoEscrutinio() {
        return ProgresoEscrutinioDTO.determinar(
                escrutinioCabecera == null ? null : escrutinioCabecera.getEstadoEscrutinio(),
                escrutinioCabecera != null && escrutinioCabecera.getFechaApertura() != null,
                actaFisica == null ? null : (actaFisica.getEstadoRevision() == null
                        ? ActaFisicaEscrutinioService.PENDIENTE_REVISION : actaFisica.getEstadoRevision()));
    }

    private Integer pasoVisible;

    public int getPasoVisible() {
        return pasoVisible == null ? getProgresoEscrutinio().getIndice() : pasoVisible;
    }

    public boolean isPasoDisponible(int paso) {
        return isPuedeConsultarMesa() && paso >= 0 && paso <= 4
                && (paso <= getProgresoEscrutinio().getIndice()
                    || (paso == 4 && isMesaCerrada() && actaFisica != null));
    }

    public void seleccionarPaso(int paso) {
        if (!isPasoDisponible(paso)) {
            JsfUtil.addWarningMessageFromBundle("actaE.mensaje.accionBloqueada");
            return;
        }
        pasoVisible = paso;
        if (paso == 4 && actaFisica != null && !isActaFisicaEnVisor()) {
            verActaFisica();
        }
    }

    /** Navegacion de consulta; no cambia estados ni concede permisos operativos. */
    public MenuModel getModeloPasosEscrutinio() {
        ProgresoEscrutinioDTO progreso = getProgresoEscrutinio();
        DefaultMenuModel modelo = new DefaultMenuModel();
        for (ProgresoEscrutinioDTO.Etapa etapa : getEtapasEscrutinio()) {
            String estado = progreso.estadoPaso(etapa.getIndice());
            // Steps ya representa el numero; la etiqueta del filtro lo incluye por separado.
            String etiqueta = Constantes.getMensaje("actaE.paso." + PASOS_ESCRUTINIO[etapa.getIndice()]);
            modelo.getElements().add(DefaultMenuItem.builder()
                    .value(etiqueta + " - " + Constantes.getMensaje("actaE.avance." + estado))
                    .icon(progreso.completado(etapa.getIndice()) ? "pi pi-check" : null)
                    .command("#{actaEController.seleccionarPaso(" + etapa.getIndice() + ")}")
                    .process("@this :frmActaE:tabActaE:contenidoEtapa")
                    .update(":frmActaE:tabActaE:contenidoEtapa :frmActaE:tabActaE:progresoMesa :frmGlobal:growlGlobal")
                    .disabled(!isPasoDisponible(etapa.getIndice()))
                    .containerStyleClass("actae-step-" + estado).build());
        }
        return modelo;
    }

    /**
     * Carga una mesa para consulta, sin escribir nada. A diferencia de
     * {@link #cargaDatosMesaSeleccionada()}, que prepara los registros de conteo vacíos
     * (prepararActaPorMesaDTO) y ajusta los sufragantes de una cabecera ya existente,
     * aquí solo se leen los que ya hay: un revisor que abre una mesa sin datos no debe
     * generarlos.
     */
    private void cargarMesaSoloLectura() {
        mesaSoloLectura = true;
        motivoBloqueoApertura = null;
        cargarTotalSufragantes();
        Integer procesoId = procesoActivo != null ? procesoActivo.getId() : null;
        EscrutinioCabeceraDTO cabecera = escrutinioService.buscarCabeceraDTO(mesaSeleccionado.getId(), procesoId);
        escrutinioCabecera = cabecera != null ? cabecera : new EscrutinioCabeceraDTO();
        List<EscrutinioDTO> registrados = escrutinioService.listarDTOsPorMesaYProceso(mesaSeleccionado.getId(), procesoId);
        listaCamposActaE = registrados != null ? new ArrayList<>(registrados) : new ArrayList<>();
        observacionApertura = escrutinioCabecera.getObservacionApertura() != null
                ? escrutinioCabecera.getObservacionApertura() : "";
        cargarDocumentosActa();
        pasoVisible = getProgresoEscrutinio().getIndice();
    }

    /** Hay una mesa elegida y el usuario puede verla, como operador o como revisor. */
    public boolean isPuedeConsultarMesa() {
        return mesaSeleccionadaValida() && (isPuedeOperarActa() || isUsuarioRevisorActas());
    }

    /** Apertura, conteo y cierre: solo quien opera, nunca en modo consulta. */
    public boolean isPuedeOperarMesaSeleccionada() {
        return mesaSeleccionadaValida() && isPuedeOperarActa() && !mesaSoloLectura;
    }

    /**
     * Carga o reemplazo del acta física: mesa cerrada, usuario que opera la mesa o
     * revisor, y acta vigente no validada. El servicio vuelve a comprobarlo todo.
     */
    public boolean isPuedeCargarActaFisica() {
        return isMesaCerrada() && (isPuedeOperarActa() || isUsuarioRevisorActas())
                && (actaFisica == null || !ActaFisicaEscrutinioService.VALIDADA.equals(actaFisica.getEstadoRevision()));
    }

    /**
     * Lee la imagen del acta física para el visor, reutilizando la misma descarga
     * autorizada que usa reportesMesa (DocumentoBean#obtenerArchivo). Se invoca bajo
     * demanda, al pulsar «Ver acta física».
     */
    public void verActaFisica() {
        contenidoActaFisica = null;
        if (actaFisica == null || !isPuedeConsultarMesa()) {
            JsfUtil.addWarningMessageFromBundle("actaE.actaFisica.no.disponible");
            return;
        }
        try {
            var archivo = documentoBean.obtenerArchivo(actaFisica);
            if (archivo == null) {
                JsfUtil.addErrorMessageFromBundle("actaE.actaFisica.no.disponible");
                return;
            }
            try (var contenido = archivo.getStream().get()) {
                contenidoActaFisica = contenido.readAllBytes();
            }
        } catch (Exception e) {
            log.error("ACTA_FISICA causa=VISOR; excepcion={}",
                    ec.com.antenasur.security.qr.DiagnosticoQr.tipoExcepcion(e));
            JsfUtil.addErrorMessageFromBundle("actaE.actaFisica.no.disponible");
        }
    }

    public boolean isActaFisicaEnVisor() {
        return contenidoActaFisica != null;
    }

    // ── Votos registrados con la estructura del acta física ──────────────────────
    // Para cotejar fila por fila con la imagen: listas, subtotal de válidos, nulos,
    // blancos, total de votos y, aparte, papeletas no utilizadas. La clasificación y los
    // totales son los de RevisionActaFinalDTO, los mismos con los que el Tribunal valida
    // después el acta; aquí no se calcula nada propio ni se altera el conteo.

    /** Totales oficiales del acta calculados sobre los votos registrados de la mesa. */
    public ec.com.antenasur.dto.RevisionActaFinalDTO getComparacionActa() {
        ec.com.antenasur.dto.RevisionActaFinalDTO comparacion = new ec.com.antenasur.dto.RevisionActaFinalDTO();
        comparacion.setResultados(listaCamposActaE != null ? listaCamposActaE : new ArrayList<>());
        return comparacion;
    }

    /** Filas CANDIDATO / LISTA, en el orden de la categoría, que es el del acta impresa. */
    public List<EscrutinioDTO> getFilasListasActa() {
        return filtrarPorClase("LISTA");
    }

    /** Hay una categoría «PAPELETAS RESTANTES» en el conteo de esta mesa. */
    public boolean isPapeletasRegistradas() {
        return !filtrarPorClase("PAPELETAS").isEmpty();
    }

    /**
     * Categorías registradas que no encajan en ninguna fila del acta. Se muestran aparte
     * para no ocultar ningún dato; en un proceso bien configurado la lista está vacía.
     */
    public List<EscrutinioDTO> getOtrosRegistrosActa() {
        List<EscrutinioDTO> otros = new ArrayList<>();
        if (listaCamposActaE != null) {
            for (EscrutinioDTO item : listaCamposActaE) {
                if (item != null && ec.com.antenasur.dto.RevisionActaFinalDTO.clasificar(item) == null) {
                    otros.add(item);
                }
            }
        }
        return otros;
    }

    private List<EscrutinioDTO> filtrarPorClase(String clase) {
        List<EscrutinioDTO> filas = new ArrayList<>();
        if (listaCamposActaE != null) {
            for (EscrutinioDTO item : listaCamposActaE) {
                if (item != null && clase.equals(ec.com.antenasur.dto.RevisionActaFinalDTO.clasificar(item))) {
                    filas.add(item);
                }
            }
        }
        return filas;
    }

    // ── Revisión oficial del acta física (Administrador y Tribunal) ──────────────
    // Reutiliza la validación existente (ActaFisicaEscrutinioService#validarActaFinal):
    // el revisor transcribe cada línea del acta, incluidos subtotal y total, y solo se
    // valida si todo cuadra. Si no concuerda puede devolver la mesa al presidente
    // (acta OBSERVADA o RECHAZADA con motivo y escrutinio reabierto). Una validación se
    // revierte con motivo. Todas las reglas se comprueban de nuevo en el servidor.

    /** Transcripción en curso; null fuera del modo revisión. */
    @Getter
    private ec.com.antenasur.dto.RevisionActaFinalDTO revisionActa;

    /** Copias editables de la transcripción, por categoría, para enlazarlas junto a cada fila. */
    @Getter
    private Map<Integer, EscrutinioDTO> revisionPorCategoria = new HashMap<>();

    /** Acción con motivo en preparación: DEVOLVER o REVERTIR; null si no hay ninguna. */
    @Getter
    private String accionRevisionActa;

    @Getter
    @Setter
    private String estadoDevolucionActa = ActaFisicaEscrutinioService.OBSERVADA;

    @Getter
    @Setter
    private String motivoRevisionActa;

    /** El revisor declara que cotejó el acta física y coincide con lo registrado. */
    @Getter
    @Setter
    private boolean cotejoConfirmado;

    private static final String ACCION_DEVOLVER = "DEVOLVER";
    private static final String ACCION_REVERTIR = "REVERTIR";

    public boolean isActaFisicaValidada() {
        return actaFisica != null && ActaFisicaEscrutinioService.VALIDADA.equals(actaFisica.getEstadoRevision());
    }

    /** Acta devuelta al presidente: se muestra el motivo a todos los que ven la mesa. */
    public boolean isActaFisicaDevuelta() {
        return actaFisica != null && actaFisica.getObservacionRevision() != null
                && (ActaFisicaEscrutinioService.OBSERVADA.equals(actaFisica.getEstadoRevision())
                || ActaFisicaEscrutinioService.RECHAZADA.equals(actaFisica.getEstadoRevision()));
    }

    /** Validar o devolver: revisor, mesa cerrada y acta física vigente aún no validada. */
    public boolean isPuedeRevisarActaFisica() {
        return isUsuarioRevisorActas() && isMesaCerrada() && actaFisica != null && !isActaFisicaValidada();
    }

    public boolean isPuedeRevertirValidacion() {
        return isUsuarioRevisorActas() && isMesaCerrada() && isActaFisicaValidada();
    }

    public boolean isModoRevisionActa() {
        return revisionActa != null;
    }

    /** Copia de la transcripción para la primera categoría de la clase (NULOS, BLANCOS, PAPELETAS). */
    public EscrutinioDTO revisionDeClase(String clase) {
        List<EscrutinioDTO> registrados = filtrarPorClase(clase);
        return registrados.isEmpty() ? null : revisionPorCategoria.get(registrados.get(0).getCategoriaId());
    }

    /**
     * Abre la transcripción con los valores registrados como punto de partida; el
     * revisor los corrige según el acta. Subtotal y total declarados empiezan vacíos:
     * se copian del acta, no se calculan.
     */
    public void iniciarRevisionActa() {
        if (!isPuedeRevisarActaFisica() || listaCamposActaE == null || listaCamposActaE.isEmpty()) {
            JsfUtil.addErrorMessageFromBundle("actaE.mensaje.accesoDenegado");
            return;
        }
        revisionActa = new ec.com.antenasur.dto.RevisionActaFinalDTO();
        revisionPorCategoria = new HashMap<>();
        for (EscrutinioDTO registrado : listaCamposActaE) {
            EscrutinioDTO copia = new EscrutinioDTO();
            copia.setId(registrado.getId());
            copia.setMesa(registrado.getMesa());
            copia.setProcesoId(registrado.getProcesoId());
            copia.setCategoriaId(registrado.getCategoriaId());
            copia.setCategoriaNombre(registrado.getCategoriaNombre());
            copia.setCategoriaTipo(registrado.getCategoriaTipo());
            copia.setTotalVotos(registrado.getTotalVotos());
            revisionActa.getResultados().add(copia);
            revisionPorCategoria.put(copia.getCategoriaId(), copia);
        }
        accionRevisionActa = null;
    }

    public void cancelarRevisionActa() {
        revisionActa = null;
        revisionPorCategoria = new HashMap<>();
    }

    /** Cualquier cambio en la transcripción obliga a confirmar de nuevo la revisión. */
    public void cambiarRevisionActa() {
        if (revisionActa != null) {
            revisionActa.setRevisada(false);
        }
    }

    public void validarActaFisica() {
        try {
            if (!isPuedeRevisarActaFisica() || revisionActa == null) {
                JsfUtil.addErrorMessageFromBundle("actaE.mensaje.accesoDenegado");
                return;
            }
            if (!revisionActa.isRevisada() || !revisionActa.isCuadrada()) {
                JsfUtil.addWarningMessageFromBundle("actaE.oficial.validar.requisitos");
                return;
            }
            Integer procesoId = procesoActivo.getId();
            actaFisicaEscrutinioService.validarActaFinal(actaFisica.getId(), mesaSeleccionado.getId(), procesoId, revisionActa);
            auditarRevisionActa("VALIDA ACTA FISICA", null);
            cancelarRevisionActa();
            recargarTrasRevision();
            JsfUtil.addSuccessMessage(Constantes.getMensaje("reportesMesa.actaFisica.validada"));
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("ERROR AL VALIDAR ACTA FISICA DE MESA {}", mesaSeleccionado != null ? mesaSeleccionado.getId() : null, e);
            JsfUtil.addErrorMessage(Constantes.getMensaje("reportesMesa.actaFisica.error.validar"));
        }
    }

    /**
     * Camino habitual: el acta coincide con lo registrado y el revisor solo confirma, sin
     * digitar nada. Exige haber abierto el acta en el visor y marcado el cotejo; el
     * servidor toma los votos de la base, no de la vista.
     */
    public void marcarDatosOficiales() {
        try {
            if (!isPuedeRevisarActaFisica() || isModoRevisionActa()) {
                JsfUtil.addErrorMessageFromBundle("actaE.mensaje.accesoDenegado");
                return;
            }
            if (!isActaFisicaEnVisor() || !cotejoConfirmado) {
                JsfUtil.addWarningMessageFromBundle("actaE.oficial.cotejo.requerido");
                return;
            }
            actaFisicaEscrutinioService.validarDatosRegistrados(actaFisica.getId(), mesaSeleccionado.getId(),
                    procesoActivo.getId());
            auditarRevisionActa("VALIDA ACTA FISICA SIN CAMBIOS", null);
            recargarTrasRevision();
            JsfUtil.addSuccessMessage(Constantes.getMensaje("reportesMesa.actaFisica.validada"));
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("ERROR AL MARCAR DATOS OFICIALES DE MESA {}", mesaSeleccionado != null ? mesaSeleccionado.getId() : null, e);
            JsfUtil.addErrorMessage(Constantes.getMensaje("reportesMesa.actaFisica.error.validar"));
        }
    }

    public void prepararDevolucionActa() {
        prepararAccionRevision(ACCION_DEVOLVER, isPuedeRevisarActaFisica());
    }

    public void prepararReversionValidacion() {
        prepararAccionRevision(ACCION_REVERTIR, isPuedeRevertirValidacion());
    }

    private void prepararAccionRevision(String accion, boolean permitida) {
        if (!permitida) {
            JsfUtil.addErrorMessageFromBundle("actaE.mensaje.accesoDenegado");
            return;
        }
        cancelarRevisionActa();
        accionRevisionActa = accion;
        estadoDevolucionActa = ActaFisicaEscrutinioService.OBSERVADA;
        motivoRevisionActa = null;
    }

    public void cancelarAccionRevision() {
        accionRevisionActa = null;
        motivoRevisionActa = null;
    }

    /** Ejecuta la acción preparada (devolver o revertir) con su motivo. */
    public void confirmarAccionRevision() {
        try {
            if (motivoRevisionActa == null || motivoRevisionActa.isBlank()) {
                JsfUtil.addWarningMessageFromBundle("actaE.mensaje.motivo.requerido");
                return;
            }
            if (ACCION_DEVOLVER.equals(accionRevisionActa) && isPuedeRevisarActaFisica()) {
                if (!ActaFisicaEscrutinioService.OBSERVADA.equals(estadoDevolucionActa)
                        && !ActaFisicaEscrutinioService.RECHAZADA.equals(estadoDevolucionActa)) {
                    JsfUtil.addErrorMessageFromBundle("actaE.mensaje.accesoDenegado");
                    return;
                }
                actaFisicaEscrutinioService.devolverActaFisica(actaFisica.getId(), estadoDevolucionActa, motivoRevisionActa);
                auditarRevisionActa("DEVUELVE ACTA FISICA " + estadoDevolucionActa, motivoRevisionActa);
                JsfUtil.addSuccessMessageFromBundle("actaE.oficial.devuelta");
            } else if (ACCION_REVERTIR.equals(accionRevisionActa) && isPuedeRevertirValidacion()) {
                actaFisicaEscrutinioService.revertirValidacion(actaFisica.getId(), motivoRevisionActa);
                auditarRevisionActa("REVIERTE VALIDACION ACTA FISICA", motivoRevisionActa);
                JsfUtil.addSuccessMessageFromBundle("actaE.oficial.revertida");
            } else {
                JsfUtil.addErrorMessageFromBundle("actaE.mensaje.accesoDenegado");
                return;
            }
            cancelarAccionRevision();
            recargarTrasRevision();
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("ERROR EN REVISION DE ACTA FISICA DE MESA {}", mesaSeleccionado != null ? mesaSeleccionado.getId() : null, e);
            JsfUtil.addErrorMessageFromBundle("actaE.oficial.error");
        }
    }

    /**
     * Relee la mesa sin crear nada (la cabecera y el conteo ya existen) y refresca su
     * fila en el listado, porque cambian los votos y la situación.
     */
    private void recargarTrasRevision() {
        Integer procesoId = procesoActivo != null ? procesoActivo.getId() : null;
        EscrutinioCabeceraDTO cabecera = escrutinioService.buscarCabeceraDTO(mesaSeleccionado.getId(), procesoId);
        escrutinioCabecera = cabecera != null ? cabecera : new EscrutinioCabeceraDTO();
        List<EscrutinioDTO> registrados = escrutinioService.listarDTOsPorMesaYProceso(mesaSeleccionado.getId(), procesoId);
        listaCamposActaE = registrados != null ? new ArrayList<>(registrados) : new ArrayList<>();
        cargarDocumentosActa();
        actualizarIndicadoresProgreso();
        if (usuarioConsultaGerencial) {
            consultarEscrutiniosGerenciales();
            refiltrarTablaEscrutinios();
            org.primefaces.PrimeFaces.current().ajax().update(
                    "frmActaE:tabActaE:tblConsultaGerencial", "frmActaE:tabActaE:pnlResumenGerencial");
        }
    }

    private void actualizarIndicadoresProgreso() {
        org.primefaces.PrimeFaces.current().ajax().update(
                "frmActaE:tabActaE:progresoMesa", "frmActaE:tabActaE:etiquetasEstadoMesa");
    }

    /** Reaplica filtros activos al cambiar de etapa sin reiniciar orden ni pagina. */
    private void refiltrarTablaEscrutinios() {
        var faces = jakarta.faces.context.FacesContext.getCurrentInstance();
        if (!usuarioConsultaGerencial || faces == null || faces.getViewRoot() == null) return;
        var componente = faces.getViewRoot().findComponent("frmActaE:tabActaE:tblConsultaGerencial");
        if (componente instanceof org.primefaces.component.datatable.DataTable tabla) {
            int primero = tabla.getFirst();
            tabla.filterAndSort();
            int filas = tabla.getRows();
            int total = tabla.getRowCount();
            tabla.setFirst(total == 0 ? 0 : (filas > 0 && primero >= total
                    ? ((total - 1) / filas) * filas : primero));
        }
    }

    private void auditarRevisionActa(String actividad, String motivo) {
        String datos = "MESA=" + (mesaSeleccionado != null ? mesaSeleccionado.getId() : "")
                + ";PROCESO=" + (procesoActivo != null ? procesoActivo.getId() : "")
                + ";DOCUMENTO=" + (actaFisica != null ? actaFisica.getId() : "")
                + ";USUARIO=" + (loginBean != null ? loginBean.getUserName() : "")
                + ";FECHA=" + JsfUtil.getFechaStringYYYYMMddHHmm(new Date())
                + (motivo != null ? ";MOTIVO=" + motivo.trim() : "");
        procesoBean.okActivityRegister(actividad, datos);
    }

    public org.primefaces.model.StreamedContent getImagenActaFisica() {
        if (contenidoActaFisica == null) {
            return null;
        }
        byte[] contenido = contenidoActaFisica;
        return org.primefaces.model.DefaultStreamedContent.builder().contentType("image/jpeg")
                .stream(() -> new java.io.ByteArrayInputStream(contenido)).build();
    }

    private void calcularResumenGerencial() {
        limpiarResumenGerencial();
        if (listaConsultaGerencial == null) {
            return;
        }
        totalMesasGerencial = listaConsultaGerencial.size();
        for (ActaEGerencialDTO fila : listaConsultaGerencial) {
            EstadoEscrutinio estado = fila.getEstadoEscrutinio() != null
                    ? fila.getEstadoEscrutinio() : EstadoEscrutinio.PENDIENTE;
            if (EstadoEscrutinio.PENDIENTE.equals(estado)) {
                mesasPendientesGerencial++;
            } else if (EstadoEscrutinio.ABIERTO.equals(estado)) {
                mesasAbiertasGerencial++;
            } else if (EstadoEscrutinio.EN_CONTEO.equals(estado)
                    || EstadoEscrutinio.CONTEO_REGISTRADO.equals(estado)
                    || EstadoEscrutinio.REABIERTO.equals(estado)) {
                mesasConteoGerencial++;
            } else if (EstadoEscrutinio.CERRADO.equals(estado)) {
                mesasCerradasGerencial++;
                // La consulta cubre el listado inicial; el progreso refleja una validación
                // hecha después en la misma vista.
                if (mesasValidadasProceso.contains(fila.getMesaId()) || fila.getProgreso().isOficial()) {
                    mesasValidadasGerencial++;
                }
            } else if (EstadoEscrutinio.OBSERVADO.equals(estado)) {
                mesasObservadasGerencial++;
            }
            totalSufragantesGerencial += valorEntero(fila.getSufragantesAsignados());
            totalVotosRegistradosGerencial += valorEntero(fila.getVotosRegistrados());
            totalVotosValidosGerencial += valorEntero(fila.getVotosValidos());
            totalVotosBlancosGerencial += valorEntero(fila.getVotosBlancos());
            totalVotosNulosGerencial += valorEntero(fila.getVotosNulos());
            if (Boolean.TRUE.equals(fila.getActaPdfGenerada())) {
                totalActasGeneradasGerencial++;
            }
        }
        totalActasPendientesGerencial = Math.max(0, totalMesasGerencial - totalActasGeneradasGerencial);
    }

    private void limpiarResumenGerencial() {
        totalMesasGerencial = 0;
        mesasPendientesGerencial = 0;
        mesasAbiertasGerencial = 0;
        mesasConteoGerencial = 0;
        mesasCerradasGerencial = 0;
        mesasValidadasGerencial = 0;
        mesasObservadasGerencial = 0;
        totalSufragantesGerencial = 0;
        totalVotosRegistradosGerencial = 0;
        totalVotosValidosGerencial = 0;
        totalVotosBlancosGerencial = 0;
        totalVotosNulosGerencial = 0;
        totalActasGeneradasGerencial = 0;
        totalActasPendientesGerencial = 0;
    }

    private void limpiarSeleccionMesa() {
        listaCamposActaE = new ArrayList<>();
        documentosActa = new ArrayList<>();
        contenidoActaFisica = null;
        escrutinioCabecera = new EscrutinioCabeceraDTO();
        totalSufragantesAsignados = 0;
        observacionApertura = "";
        motivoBloqueoApertura = null;
    }

    /**
     * Revisores de actas: Administrador y Tribunal. Ven el listado de todas las mesas y
     * visualizan y cargan el acta física. La operación de mesas va aparte, en
     * {@link #tieneRolOperacionActa()} y en el presidente de mesa. Es el
     * mismo criterio que AccesoDocumentoMesaService#esRevisor aplica en el servidor.
     */
    public boolean isUsuarioRevisorActas() {
        return !accesoRestringidoPresidenteMesa
                && (tieneRol("SITEC-Administrador") || tieneRol("SITEC-Tribunal"));
    }

    private boolean tieneRolConsultaGerencial() {
        return tieneRol("SITEC-Administrador")
                || tieneRol("SITEC-Tribunal")
                || tieneRol("SITEC-Gerencial")
                || tieneRol("SITEC-Supervisor")
                || tieneRol("SITEC-SuperAdministrador")
                || tieneRol("SITEC-Superadministrador");
    }

    /**
     * Operación de cualquier mesa (apertura, conteo, cierre, acta PDF). Tribunal tiene el
     * mismo acceso funcional que Administrador. El presidente de mesa no pasa por aquí:
     * {@link #puedeGestionarMesa} lo limita a su mesa, aunque sume otros roles.
     */
    private boolean tieneRolOperacionActa() {
        return tieneRolAdministrador()
                || tieneRol("SITEC-Supervisor");
    }

    private boolean tieneRolSupervisorOAdministrador() {
        return tieneRol("SITEC-Supervisor") || tieneRolAdministrador();
    }

    /** Administrador o Tribunal: mismas acciones administrativas (observar, anular, reabrir). */
    private boolean tieneRolAdministrador() {
        return tieneRol("SITEC-Administrador")
                || tieneRol("SITEC-Tribunal")
                || tieneRol("SITEC-SuperAdministrador")
                || tieneRol("SITEC-Superadministrador");
    }

    private boolean puedeCambiarEstadoAdministrativo(EstadoEscrutinio estadoNuevo) {
        if (EstadoEscrutinio.OBSERVADO.equals(estadoNuevo)) {
            return isPuedeObservarEscrutinio();
        }
        if (EstadoEscrutinio.ANULADO.equals(estadoNuevo)) {
            return isPuedeAnularEscrutinio();
        }
        if (EstadoEscrutinio.REABIERTO.equals(estadoNuevo)) {
            return isPuedeReabrirEscrutinio();
        }
        return false;
    }

    private boolean requiereMotivo(EstadoEscrutinio estadoNuevo) {
        return EstadoEscrutinio.ANULADO.equals(estadoNuevo)
                || EstadoEscrutinio.REABIERTO.equals(estadoNuevo)
                || EstadoEscrutinio.OBSERVADO.equals(estadoNuevo);
    }

    private void auditarCambioEstado(EstadoEscrutinio estadoAnterior, EstadoEscrutinio estadoNuevo, String motivo) {
        String datos = "MESA=" + (mesaSeleccionado != null ? mesaSeleccionado.getId() : "")
                + ";PROCESO=" + (procesoActivo != null ? procesoActivo.getId() : "")
                + ";USUARIO=" + (loginBean != null ? loginBean.getUserName() : "")
                + ";FECHA=" + JsfUtil.getFechaStringYYYYMMddHHmm(new Date())
                + ";ESTADO_ANTERIOR=" + (estadoAnterior != null ? estadoAnterior.name() : "")
                + ";ESTADO_NUEVO=" + (estadoNuevo != null ? estadoNuevo.name() : "")
                + ";MOTIVO=" + (motivo != null ? motivo.trim() : "");
        procesoBean.okActivityRegister("CAMBIO ESTADO ESCRUTINIO " + estadoNuevo, datos);
    }

    private boolean tieneRol(String rol) {
        return loginBean != null && loginBean.getRoles() != null && loginBean.getRoles().contains(rol);
    }

    private boolean esDocumentoDelProceso(Documentos documento, Integer mesaId, Integer procesoId) {
        if (documento == null || mesaId == null || procesoId == null) {
            return false;
        }
        String prefijo = "ACTA-" + procesoId + "-M" + mesaId + "-";
        String codigo = documento.getCodigo() != null ? documento.getCodigo() : "";
        String nombre = documento.getNombre() != null ? documento.getNombre() : "";
        return codigo.startsWith(prefijo) || nombre.startsWith(prefijo);
    }

    private Integer obtenerParroquiaId(MesaDTO mesa) {
        if (mesa == null) {
            return null;
        }
        if (mesa.getRecinto() != null && mesa.getRecinto().getUbicacionId() != null) {
            return mesa.getRecinto().getUbicacionId();
        }
        return mesa.getUbicacionId();
    }

    private Integer obtenerCantonId(MesaDTO mesa) {
        if (mesa == null) {
            return null;
        }
        if (mesa.getRecinto() != null && mesa.getRecinto().getCantonId() != null) {
            return mesa.getRecinto().getCantonId();
        }
        return mesa.getCantonId();
    }

    private String obtenerCantonNombre(MesaDTO mesa) {
        if (mesa == null) {
            return "";
        }
        if (mesa.getRecinto() != null && mesa.getRecinto().getCantonNombre() != null) {
            return textoNulo(mesa.getRecinto().getCantonNombre());
        }
        return textoNulo(mesa.getCantonNombre());
    }

    private String obtenerParroquiaNombre(MesaDTO mesa) {
        if (mesa == null) {
            return "";
        }
        if (mesa.getRecinto() != null && mesa.getRecinto().getUbicacionNombre() != null) {
            return textoNulo(mesa.getRecinto().getUbicacionNombre());
        }
        return textoNulo(mesa.getUbicacionNombre());
    }

    private int valorEntero(Integer valor) {
        return valor != null ? valor : 0;
    }

    public int getPorcentajeAvanceGerencial() {
        if (totalMesasGerencial == 0) {
            return 0;
        }
        // Avance = mesas con acta física validada (dato oficial), no solo cerradas.
        return Math.round((mesasValidadasGerencial * 100f) / totalMesasGerencial);
    }

    public int getPorcentajeParticipacionGerencial() {
        if (totalSufragantesGerencial == 0) {
            return 0;
        }
        return Math.round((totalVotosRegistradosGerencial * 100f) / totalSufragantesGerencial);
    }

    private static List<MesaDTO> filtrarMesasPorRecintoId(List<MesaDTO> mesas, Integer recintoId) {
        List<MesaDTO> resultado = new ArrayList<>();
        if (mesas == null || recintoId == null) {
            return resultado;
        }
        for (MesaDTO m : mesas) {
            if (m.getRecinto() != null && recintoId.equals(m.getRecinto().getId())) {
                resultado.add(m);
            }
        }
        return resultado;
    }

    private static List<MesaDTO> filtrarMesasPorRecintoIds(List<MesaDTO> mesas, List<Integer> recintoIds) {
        List<MesaDTO> resultado = new ArrayList<>();
        if (mesas == null || recintoIds == null) {
            return resultado;
        }
        for (MesaDTO m : mesas) {
            if (m.getRecinto() != null && recintoIds.contains(m.getRecinto().getId())) {
                resultado.add(m);
            }
        }
        return resultado;
    }
}
