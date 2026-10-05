package ec.com.antenasur.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import jakarta.el.ValueExpression;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.ValidatorException;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.PrimeFaces;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

import ec.com.antenasur.bean.DocumentoBean;
import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.dto.FilaPadronImportadaDTO;
import ec.com.antenasur.dto.EstadoActaActualizacionDTO;
import ec.com.antenasur.dto.FiltroMiembrosDTO;
import ec.com.antenasur.dto.GeograpDTO;
import ec.com.antenasur.dto.IglesiaDTO;
import ec.com.antenasur.dto.IglesiaPersonaDTO;
import ec.com.antenasur.dto.PersonaDTO;
import ec.com.antenasur.dto.PersonaEliminadaDTO;
import ec.com.antenasur.dto.PersonaHistorialDTO;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.exception.IglesiaPersonaException;
import ec.com.antenasur.itext.ReporteXLSX;
import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.tec.Documentos;
import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.model.tec.TipoDocumento;
import ec.com.antenasur.service.GeograpService;
import ec.com.antenasur.service.IglesiaPersonaService;
import ec.com.antenasur.service.IglesiaService;
import ec.com.antenasur.service.PersonaBajaService;
import ec.com.antenasur.service.PersonaService;
import ec.com.antenasur.dto.CronogramaFaseDTO;
import ec.com.antenasur.service.tec.CronogramaService;
import ec.com.antenasur.service.tec.ActaActualizacionMiembrosService;
import ec.com.antenasur.service.tec.MesaService;
import ec.com.antenasur.service.tec.PadronService;
import ec.com.antenasur.service.tec.RecintoService;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.ExcelPadronParser;
import ec.com.antenasur.util.JsfUtil;
import ec.com.antenasur.util.RepositorioDocumentos;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Named
@ViewScoped
@Slf4j
public class PersonaController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private LoginBean loginBean;

    @Inject
    private DocumentoBean documentoBean;

    @Inject
    private PersonaService personaService;

    @Inject
    private PersonaBajaService personaBajaService;

    @Inject
    private IglesiaService iglesiaService;

    @Inject
    private RecintoService recintoService;

    @Inject
    private MesaService mesaService;

    @Inject
    private PadronService padronService;

    @Inject
    private IglesiaPersonaService iglesiaPersonaService;

    @Inject
    private GeograpService geograpService;

    @Inject
    private CronogramaService cronogramaService;

    @Inject
    private ActaActualizacionMiembrosService actaActualizacionMiembrosService;

    @Setter
    @Getter
    private PersonaDTO personaSeleccionado;

    @Setter
    @Getter
    private IglesiaDTO iglesiaSeleccionado;

    @Setter
    @Getter
    private IglesiaPersonaDTO iglesiaPersonaSeleccionado;

    /** Persona cuyo historial se muestra en el diálogo; solo para el encabezado. */
    @Setter
    @Getter
    private PersonaDTO personaHistorialSeleccionada;

    /** Revisiones de auditoría de la persona mostrada en el diálogo de historial. */
    @Getter
    private List<PersonaHistorialDTO> historialPersona = Collections.emptyList();

    /** Personas eliminadas; se llena solo al abrir el panel y se libera al cerrarlo. */
    @Getter
    private List<PersonaEliminadaDTO> personasEliminadas = Collections.emptyList();

    @Setter
    @Getter
    private PersonaEliminadaDTO personaEliminadaSeleccionada;

    /**
     * Filtros geográficos como ids planos (igual que
     * {@code ReporteMesaController.cantonDocumentalId}/{@code parroquiaDocumentalId}):
     * evita atar el valor del combo a una propiedad anidada de una entidad
     * compartida, que es lo que causaba el aliasing entre cantón y parroquia.
     */
    @Setter
    @Getter
    private Integer provinciaId, cantonId, parroquiaId;

    /**
     * Opciones de los filtros dependientes: solo ubicaciones con iglesias activas,
     * resueltas con DISTINCT en BD (nunca el catálogo geográfico completo).
     */
    @Getter
    private List<GeograpDTO> provincias = new ArrayList<>(), cantones = new ArrayList<>(),
            parroquias = new ArrayList<>();

    @Setter
    @Getter
    private List<IglesiaDTO> listaIglesias;

    @Setter
    @Getter
    private List<IglesiaPersonaDTO> listaIglesiaPersonaSeleccionados, listaIglesiaPersonaExistente;

    /**
     * Tabla de miembros paginada en BD: solo se cargan las filas visibles. Ordenar,
     * filtrar y contar se resuelven en SQL con el alcance vigente (iglesia o ubicación).
     */
    @Getter
    private final MiembrosLazy modeloMiembros = new MiembrosLazy();

    @Getter
    private List<IglesiaPersonaDTO> iglesiasActivasPersona = new ArrayList<>();

    @Setter
    @Getter
    private Integer iglesiaPersonaDefinitivaId;

    @Getter
    private boolean requiereRegularizacion;

    @Getter
    private boolean puedeRegularizarIglesias;

    @Getter
    private boolean puedeGenerarReporteInconsistencias;

    @Setter
    @Getter
    private UploadedFile file;

    @Setter
    @Getter
    private StreamedContent fileDown;

    @Setter
    @Getter
    private InputStream in;

    @Setter
    @Getter
    private XSSFWorkbook excelMigracion;

    @Setter
    @Getter
    private List<PersonaDTO> listaPersonas;

    /** Criterio y resultados de la consulta secundaria, exclusivamente de lectura. */
    @Setter
    @Getter
    private String criterioBusquedaPersona;

    @Getter
    private List<IglesiaPersonaDTO> resultadosBusquedaPersonas = new ArrayList<>();

    /**
     * Bandera derivada del usuario logueado: true si su rol es IglesiaAdmin
     * y tiene una iglesia asignada. Cuando es true, la vista debe ocultar
     * los selectores de cantón/parroquia/iglesia (operará solo sobre su iglesia).
     */
    @Getter
    private boolean restringidoAIglesia;

    /** Progreso de actualización: [total, actualizados, porcentaje]. */
    private int[] progreso = {0, 0, 0};

    @Getter
    private EstadoActaActualizacionDTO estadoActaActualizacion
            = new EstadoActaActualizacionDTO(0, 0, false, null);

    public int getTotalMiembros() { return progreso[0]; }
    public int getMiembrosActualizados() { return progreso[1]; }
    public int getMiembrosPendientes() { return progreso[0] - progreso[1]; }
    public int getPorcentajeActualizacion() { return progreso[2]; }
    public boolean isActualizacionCompleta() { return progreso[0] > 0 && progreso[1] == progreso[0]; }

    /**
     * Fase de menor orden vigente en el proceso activo (banner general del
     * proceso). Puede ser cualquier fase del ciclo electoral.
     */
    @Getter
    private CronogramaFaseDTO faseVigente;

    /**
     * Configuración de {@link ec.com.antenasur.enums.FaseElectoral#ACTUALIZACION_MIEMBROS}
     * en el proceso activo (presente aunque la fase no esté vigente ahora).
     * El flag {@code vigente} del DTO indica si la ventana de fechas está activa.
     * Alimenta el card central del timeline de cronograma.
     */
    @Getter
    private CronogramaFaseDTO faseActualizacionMiembros;

    /**
     * Fase inmediatamente anterior a ACTUALIZACION_MIEMBROS por {@code orden}.
     * Puede ser pasada, vigente o futura (sin filtro de fecha). Null si no existe.
     */
    @Getter
    private CronogramaFaseDTO faseAnterior;

    /**
     * Fase inmediatamente siguiente a ACTUALIZACION_MIEMBROS por {@code orden}.
     * Puede ser pasada, vigente o futura (sin filtro de fecha). Null si no existe.
     */
    @Getter
    private CronogramaFaseDTO faseSiguiente;

    /** Indica si la fase activa permite editar el padrón. Bloquea botones. */
    @Getter
    private boolean puedeEditarPadron;

    @PostConstruct
    private void init() {
        try {
            listaIglesias = new ArrayList<>();
            iglesiaSeleccionado = new IglesiaDTO();

            // Cronograma electoral: timeline del módulo personas.
            // faseVigente            → fase de mayor prioridad (menor orden) activa ahora.
            // faseActualizacionMiembros → config de ACTUALIZACION_MIEMBROS (sin filtro de fecha).
            // faseAnterior / faseSiguiente → fases adyacentes por orden (sin filtro de fecha).
            // puedeEditarPadron     → evaluado sobre TODAS las fases activas simultáneas.
            faseVigente = cronogramaService.getFaseVigenteDelProcesoActivo();
            faseActualizacionMiembros = cronogramaService.getFaseActualizacionMiembros();
            faseAnterior = cronogramaService.getFaseAnteriorAActualizacion();
            faseSiguiente = cronogramaService.getFaseSiguienteAActualizacion();
            puedeEditarPadron = cronogramaService.permiteEdicionPadron();
            // Tribunal conserva su alcance actual y Administrador puede
            // regularizar inconsistencias entre iglesias desde el mismo flujo.
            puedeRegularizarIglesias = esUsuarioTribunal() || esUsuarioAdministrador();
            puedeGenerarReporteInconsistencias = puedeRegularizarIglesias || esUsuarioAdministrador();

            // Detección de rol IglesiaAdmin: si el usuario logueado tiene este
            // rol y una iglesia asignada, lo confinamos a esa iglesia y
            // precargamos sus miembros directamente.
            if (esUsuarioIglesiaAdmin()) {
                restringidoAIglesia = true;
                if (!esUsuarioIglesiaAdminConIglesia()) {
                    JsfUtil.addWarningMessageFromBundle("form.iglesias.mensaje.sin.asignacion");
                    return;
                }
                Integer iglesiaId = loginBean.getUsuario().getIglesiaId();
                iglesiaSeleccionado = iglesiaService.obtenerDTOPorId(iglesiaId);
                listaIglesias = new ArrayList<>();
                listaIglesias.add(iglesiaSeleccionado);
                progreso = iglesiaPersonaService.calcularProgresoActualizacion(iglesiaId);
                actualizarEstadoActaActualizacion();
                return;
            }

            // Camino normal (admin global): filtros Provincia → Cantón → Parroquia limitados
            // a ubicaciones con iglesias activas. Si solo una provincia tiene iglesias queda
            // seleccionada y se carga su listado; sin provincia el listado queda vacío.
            provincias = iglesiaService.listarProvinciasConIglesias();
            if (provincias.size() == 1) {
                provinciaId = provincias.get(0).getId();
                cantones = iglesiaService.listarCantonesConIglesias(provinciaId);
            }
            recargarIglesiasYListado();
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    private boolean esUsuarioIglesiaAdminConIglesia() {
        return esUsuarioIglesiaAdmin() && loginBean.getUsuario().getIglesiaId() != null;
    }

    private boolean esUsuarioIglesiaAdmin() {
        if (loginBean == null || loginBean.getUsuario() == null || loginBean.getRoles() == null) {
            return false;
        }
        String prefijo = (String) JsfUtil.getProperty("roles.sitec", true);
        String rolIglesia = (prefijo == null ? "" : prefijo) + Constantes.getRolIglesiaAdmin();
        for (String r : loginBean.getRoles()) {
            if (rolIglesia.equals(r)) {
                return true;
            }
        }
        return false;
    }

    private boolean esUsuarioTribunal() {
        if (loginBean == null || loginBean.getRoles() == null) {
            return false;
        }
        String prefijo = (String) JsfUtil.getProperty("roles.sitec", true);
        String rolTribunal = (prefijo == null ? "" : prefijo) + Constantes.getRolTribunal();
        return loginBean.getRoles().contains(rolTribunal);
    }

    private boolean esUsuarioAdministrador() {
        if (loginBean == null || loginBean.getRoles() == null) {
            return false;
        }
        String prefijo = (String) JsfUtil.getProperty("roles.sitec", true);
        String rolAdministrador = (prefijo == null ? "" : prefijo) + Constantes.getRolAdministrador();
        return loginBean.getRoles().contains(rolAdministrador);
    }

    /** Abre la consulta sin alterar el listado ni la selección del CRUD principal. */
    public void prepararBusquedaPersonas() {
        criterioBusquedaPersona = null;
        resultadosBusquedaPersonas = new ArrayList<>();
    }

    /** Busca por cédula o coincidencia parcial de nombres y apellidos. */
    public void buscarPersonasParaConsulta() {
        if (criterioBusquedaPersona == null || criterioBusquedaPersona.isBlank()) {
            JsfUtil.addWarningMessage(mensaje("form.personas.busqueda.criterio.requerido"));
            resultadosBusquedaPersonas = new ArrayList<>();
            return;
        }
        try {
            resultadosBusquedaPersonas = iglesiaPersonaService.buscarPersonasParaConsulta(
                    criterioBusquedaPersona.trim());
        } catch (IglesiaPersonaException e) {
            resultadosBusquedaPersonas = new ArrayList<>();
            JsfUtil.addErrorMessage(mensaje(e.getMessageKey(), e.getArguments()));
        } catch (Exception e) {
            resultadosBusquedaPersonas = new ArrayList<>();
            log.error("Error al consultar personas", e);
            JsfUtil.addErrorMessage(mensaje("form.personas.busqueda.error"));
        }
    }

    /**
     * Cambio de provincia: limpia cantón, parroquia e iglesia y carga los cantones de
     * la provincia que tienen iglesias activas.
     */
    public void obtieneCantones() {
        cantonId = null;
        parroquiaId = null;
        parroquias = new ArrayList<>();
        cantones = iglesiaService.listarCantonesConIglesias(provinciaId);
        recargarIglesiasYListado();
    }

    /**
     * Cambio de cantón: limpia parroquia e iglesia y carga las parroquias del cantón
     * que tienen iglesias activas. Si se limpia, conserva el alcance de la provincia.
     */
    public void obtieneParroquias() {
        parroquiaId = null;
        parroquias = iglesiaService.listarParroquiasConIglesias(cantonId);
        recargarIglesiasYListado();
    }

    /** Cambio de parroquia: si se limpia, conserva el alcance del cantón activo. */
    public void obtieneIglesiasPorParroquia() {
        recargarIglesiasYListado();
        if (parroquiaId != null) {
            JsfUtil.addInfoMessage(listaIglesias.size() + " Iglesias registradas");
        }
    }

    /**
     * Reinicia la iglesia elegida, recarga el combo de iglesias con la ubicación activa
     * y delega el listado en {@link #refrescarListadoMiembrosActual()}, el único punto
     * que aplica los criterios activos.
     */
    private void recargarIglesiasYListado() {
        iglesiaSeleccionado = new IglesiaDTO();
        listaIglesias = iglesiaService.listarDTOsPorUbicacion(provinciaId, cantonId, parroquiaId);
        refrescarListadoMiembrosActual();
        reiniciarPaginacion();
    }

    /** Un alcance nuevo empieza en la primera página de la tabla. */
    private void reiniciarPaginacion() {
        FacesContext contexto = FacesContext.getCurrentInstance();
        UIComponent tabla = contexto == null ? null
                : contexto.getViewRoot().findComponent("frmPersonas:tblPersonas");
        if (tabla instanceof DataTable dataTable) {
            dataTable.setFirst(0);
        }
    }

    /** Hay iglesia o ubicación elegida: la tabla y la exportación tienen qué mostrar. */
    public boolean isListadoConAlcance() {
        return filtroAlcance().tieneAlcance();
    }

    /** Alcance vigente del listado; sin filtros de columna. */
    private FiltroMiembrosDTO filtroAlcance() {
        Integer iglesiaId = iglesiaSeleccionado != null ? iglesiaSeleccionado.getId() : null;
        return iglesiaId != null ? new FiltroMiembrosDTO(iglesiaId, null, null, null)
                : new FiltroMiembrosDTO(null, provinciaId, cantonId, parroquiaId);
    }

    /** Cambio de iglesia: si se limpia, conserva el alcance de parroquia/cantón activo. */
    public void obtienePersonasPorIglesias() {
        if (iglesiaSeleccionado != null && iglesiaSeleccionado.getId() != null) {
            iglesiaSeleccionado = iglesiaService.obtenerDTOPorId(iglesiaSeleccionado.getId());
        } else {
            iglesiaSeleccionado = new IglesiaDTO();
        }
        refrescarListadoMiembrosActual();
        reiniciarPaginacion();
        if (iglesiaSeleccionado.getId() != null) {
            // progreso[0] es el total de miembros de la iglesia, ya calculado al refrescar.
            if (getTotalMiembros() == 0) {
                JsfUtil.addWarningMessage("No existe registro de personas en " + iglesiaSeleccionado.getNombre());
            } else {
                JsfUtil.addInfoMessage(getTotalMiembros() + " personas registradas");
            }
        }
    }

    public void inicializaPersonaSeleccionado() {
        iglesiaPersonaSeleccionado = new IglesiaPersonaDTO();
        iglesiaPersonaSeleccionado.setPersona(new PersonaDTO());
        iglesiaPersonaSeleccionado.setIglesia(new IglesiaDTO());
        // Por defecto habilitado para participar en las elecciones: el admin puede desmarcarlo
        iglesiaPersonaSeleccionado.setHabilitadoPadron(Boolean.TRUE);
        if (restringidoAIglesia && iglesiaSeleccionado != null
                && iglesiaSeleccionado.getId() != null) {
            // El valor del formulario procede del contexto autenticado, no de
            // un parámetro enviado por el navegador.
            iglesiaPersonaSeleccionado.setIglesia(iglesiaSeleccionado);
        } else {
            this.iglesiaSeleccionado = new IglesiaDTO();
        }
        this.personaSeleccionado = new PersonaDTO();
        limpiarEstadoRegularizacion();
    }

    public void nuevaPersona() {
        inicializaPersonaSeleccionado();
    }

    public void prepararEdicion(IglesiaPersonaDTO miembro) {
        if (Boolean.TRUE.equals(miembro != null ? miembro.getInconsistenciaIglesias() : null)) {
            iglesiaPersonaSeleccionado = null;
            JsfUtil.addWarningMessage(mensaje("form.personas.inconsistencia.edicion.bloqueada"));
            PrimeFaces.current().ajax().addCallbackParam("dialogReady", false);
            return;
        }
        iglesiaPersonaSeleccionado = miembro;
        if (restringidoAIglesia && iglesiaSeleccionado != null
                && iglesiaSeleccionado.getId() != null) {
            // Mantiene el único campo Iglesia alineado al alcance persistido
            // del usuario; el servicio valida también la relación original.
            iglesiaPersonaSeleccionado.setIglesia(iglesiaSeleccionado);
        }
        limpiarEstadoRegularizacion();
        PrimeFaces.current().ajax().addCallbackParam("dialogReady", true);
    }

    public void prepararRegularizacion(IglesiaPersonaDTO miembro) {
        limpiarEstadoRegularizacion();
        if (!puedeRegularizarIglesias) {
            JsfUtil.addErrorMessage(mensaje("form.personas.regularizacion.error.permiso"));
            PrimeFaces.current().ajax().addCallbackParam("dialogReady", false);
            return;
        }
        if (miembro == null || !Boolean.TRUE.equals(miembro.getInconsistenciaIglesias())) {
            JsfUtil.addWarningMessage(mensaje("form.personas.regularizacion.error.no.requerida"));
            PrimeFaces.current().ajax().addCallbackParam("dialogReady", false);
            return;
        }
        iglesiaPersonaSeleccionado = miembro;
        cargarEstadoRegularizacion();
        if (!requiereRegularizacion) {
            iglesiaPersonaSeleccionado = null;
            JsfUtil.addWarningMessage(mensaje("form.personas.regularizacion.error.no.requerida"));
            PrimeFaces.current().ajax().addCallbackParam("dialogReady", false);
            return;
        }
        PrimeFaces.current().ajax().addCallbackParam("dialogReady", true);
    }

    public boolean existeIglesiaPersonasSeleccionadas() {
        return this.listaIglesiaPersonaSeleccionados != null && !this.listaIglesiaPersonaSeleccionados.isEmpty();
    }

    public String getMensajeBotonEliminar() {
        if (existeIglesiaPersonasSeleccionadas()) {
            int size = this.listaIglesiaPersonaSeleccionados.size();
            return size > 1 ? size + " personas seleccionadas" : "1 persona seleccionada";
        }
        return "Eliminar";
    }

    public void eliminarIglesiaPersonaSeleccionadas() {
        List<Integer> ids = new ArrayList<>();
        if (listaIglesiaPersonaSeleccionados != null && !listaIglesiaPersonaSeleccionados.isEmpty()) {
            for (IglesiaPersonaDTO item : listaIglesiaPersonaSeleccionados) {
                if (puedeEliminarMiembro(item)) {
                    ids.add(item.getId());
                }
            }
        }
        eliminarMiembrosPorIds(ids);
    }

    public void eliminarIglesiaPersonaSeleccionada() {
        List<Integer> ids = new ArrayList<>();
        if (puedeEliminarMiembro(iglesiaPersonaSeleccionado)) {
            ids.add(iglesiaPersonaSeleccionado.getId());
        }
        eliminarMiembrosPorIds(ids);
    }

    private void eliminarMiembrosPorIds(List<Integer> ids) {
        int eliminadas;
        try {
            eliminadas = iglesiaPersonaService.eliminarPorIds(ids);
        } catch (IglesiaPersonaException e) {
            JsfUtil.addErrorMessage(mensaje(e.getMessageKey(), e.getArguments()));
            return;
        }
        refrescarListadoMiembrosActual();
        if (eliminadas > 0) {
            JsfUtil.addInfoMessage(eliminadas + " Personas eliminadas");
        } else {
            JsfUtil.addWarningMessage("No se encontraron miembros seleccionados para eliminar");
        }
        this.listaIglesiaPersonaSeleccionados = null;
        this.iglesiaPersonaSeleccionado = null;
        PrimeFaces.current().ajax().update(
                "frmPersonas:tblPersonas", "frmPersonas:btnEliminaRegistros",
                "frmPersonas:panelResumenMiembros");
    }

    public void buscaPersonaPorCedula() {
        if (iglesiaPersonaSeleccionado == null || iglesiaPersonaSeleccionado.getPersona() == null) {
            return;
        }
        PersonaDTO encontrada = personaService.buscarDTOPorDocumento(iglesiaPersonaSeleccionado.getPersona().getDocumento());
        if (encontrada != null) {
            iglesiaPersonaSeleccionado.setPersona(encontrada);
            cargarEstadoRegularizacion();
            if (requiereRegularizacion) {
                JsfUtil.addWarningMessage(mensaje("form.personas.error.varias.iglesias"));
            } else if (!iglesiasActivasPersona.isEmpty()) {
                String iglesia = iglesiasActivasPersona.get(0).getIglesia() != null
                        ? iglesiasActivasPersona.get(0).getIglesia().getNombre() : "";
                JsfUtil.addWarningMessage(mensaje("form.personas.error.otra.iglesia", iglesia));
            } else {
                JsfUtil.addInfoMessage(mensaje("form.personas.persona.existente", encontrada.getDocumento()));
            }
        }
    }

    /** Valida la cédula antes de consultar o persistir la persona. */
    public void validarCedula(FacesContext contexto, UIComponent componente, Object valor) {
        String cedula = valor != null ? valor.toString().trim() : "";
        if (cedula.isEmpty()) {
            return;
        }
        if (!cedula.matches("\\d{10}") || !JsfUtil.validarCedulaORUC(cedula)) {
            throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    mensaje("form.personas.error.documento.invalido"), null));
        }
    }

    /** Evita que un valor compuesto solo por espacios supere la regla requerida. */
    public void validarNombres(FacesContext contexto, UIComponent componente, Object valor) {
        if (valor == null || valor.toString().trim().isEmpty()) {
            throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    mensaje("form.personas.error.nombres.requerido"), null));
        }
    }

    public void actualizarPersona() {
        try {
            boolean esActualizacion = iglesiaPersonaSeleccionado != null
                    && iglesiaPersonaSeleccionado.getId() != null;
            // Si el contexto está restringido a una iglesia (IglesiaAdmin),
            // forzamos el binding a esa iglesia para evitar registros cruzados.
            if (restringidoAIglesia && iglesiaPersonaSeleccionado != null
                    && iglesiaSeleccionado != null && iglesiaSeleccionado.getId() != null) {
                iglesiaPersonaSeleccionado.setIglesia(iglesiaSeleccionado);
            }
            if (!cronogramaService.permiteEdicionPadron()) {
                rechazarGuardado("form.personas.error.cronograma");
                return;
            }
            IglesiaPersonaDTO persistido = iglesiaPersonaService.guardarDesdeDTO(iglesiaPersonaSeleccionado);
            if (persistido != null) {
                String nombreMiembro = (persistido.getPersona() != null
                        && persistido.getPersona().getNombres() != null)
                        ? persistido.getPersona().getNombres() : "";
                boolean enPadron = Boolean.TRUE.equals(persistido.getHabilitadoPadron());
                String estadoPadron = " · " + mensaje(enPadron
                        ? "form.personas.habilitacion.guardado.si" : "form.personas.habilitacion.guardado.no");
                if (esActualizacion) {
                    JsfUtil.addSuccessMessage("Miembro actualizado correctamente: "
                            + nombreMiembro + estadoPadron);
                } else {
                    JsfUtil.addSuccessMessage("Miembro registrado correctamente: "
                            + nombreMiembro + estadoPadron);
                }
                personaSeleccionado = null;
                iglesiaPersonaSeleccionado = null;
                limpiarEstadoRegularizacion();
                refrescarListadoMiembrosActual();
                PrimeFaces.current().ajax().update(
                        "frmPersonas:tblPersonas", "frmPersonas:panelResumenMiembros");
            } else {
                rechazarGuardado("form.personas.error.guardar");
            }
        } catch (IglesiaPersonaException e) {
            rechazarGuardado(e.getMessageKey(), e.getArguments());
            return;
        } catch (Exception e) {
            if (esViolacionRelacionActiva(e)) {
                // El servicio valida antes de insertar; llegar aquí solo es posible si
                // otro guardado simultáneo ganó la carrera. El trigger protegió los datos.
                log.warn("Guardado de miembro rechazado por relación activa existente (SQLState 23505)");
                rechazarGuardado("form.personas.error.relacion.activa");
            } else {
                log.error("Error al guardar persona", e);
                rechazarGuardado("form.personas.error.guardar");
            }
            return;
        }
    }

    /** Violación 23505 del trigger fn_validar_iglesia_activa_persona en la cadena de causas. */
    private static boolean esViolacionRelacionActiva(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof java.sql.SQLException sql && "23505".equals(sql.getSQLState())) {
                return true;
            }
            if (causa.getCause() == causa) {
                break;
            }
        }
        return false;
    }

    private void rechazarGuardado(String clave, Object... argumentos) {
        String texto = mensaje(clave, argumentos);
        FacesContext contexto = FacesContext.getCurrentInstance();
        if ("form.personas.error.documento.duplicado".equals(clave)) {
            contexto.addMessage("frmPersonas:cedula", new FacesMessage(FacesMessage.SEVERITY_ERROR, texto, null));
            PrimeFaces.current().focus("frmPersonas:cedula");
        } else {
            // Mensaje global: lo muestra el growl de la plantilla (globalOnly, autoUpdate).
            contexto.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, texto, null));
        }
        PrimeFaces.current().ajax().addCallbackParam("validationFailed", true);
    }

    public void regularizarIglesias() {
        try {
            if (!puedeRegularizarIglesias) {
                throw new IglesiaPersonaException("form.personas.regularizacion.error.permiso");
            }
            IglesiaPersonaDTO persistido = iglesiaPersonaService.regularizarDesdeDTO(
                    iglesiaPersonaSeleccionado, iglesiaPersonaDefinitivaId);
            if (persistido == null) {
                throw new IglesiaPersonaException("form.personas.error.guardar");
            }
            String nombre = persistido.getPersona() != null
                    ? safe(persistido.getPersona().getNombres()) : "";
            JsfUtil.addSuccessMessage(mensaje("form.personas.regularizacion.exito", nombre));
            iglesiaPersonaSeleccionado = null;
            limpiarEstadoRegularizacion();
            refrescarListadoMiembrosActual();
            PrimeFaces.current().ajax().update(
                    "frmPersonas:tblPersonas", "frmPersonas:btnEliminaRegistros",
                    "frmPersonas:panelResumenMiembros");
        } catch (IglesiaPersonaException e) {
            JsfUtil.addErrorMessage(mensaje(e.getMessageKey(), e.getArguments()));
            PrimeFaces.current().ajax().addCallbackParam("validationFailed", true);
        } catch (Exception e) {
            log.error("Error al regularizar iglesias de la persona", e);
            JsfUtil.addErrorMessage(mensaje("form.personas.error.guardar"));
            PrimeFaces.current().ajax().addCallbackParam("validationFailed", true);
        }
    }

    private void cargarEstadoRegularizacion() {
        iglesiasActivasPersona = new ArrayList<>();
        requiereRegularizacion = false;
        if (iglesiaPersonaSeleccionado == null || iglesiaPersonaSeleccionado.getPersona() == null
                || iglesiaPersonaSeleccionado.getPersona().getDocumento() == null) {
            return;
        }
        iglesiasActivasPersona = iglesiaPersonaService.listarDTOsActivosPorDocumento(
                iglesiaPersonaSeleccionado.getPersona().getDocumento());
        requiereRegularizacion = iglesiasActivasPersona.stream()
                .anyMatch(relacion -> Boolean.TRUE.equals(relacion.getInconsistenciaIglesias()));
    }

    private void limpiarEstadoRegularizacion() {
        iglesiasActivasPersona = new ArrayList<>();
        iglesiaPersonaDefinitivaId = null;
        requiereRegularizacion = false;
    }

    private String mensaje(String clave, Object... argumentos) {
        Object valor = JsfUtil.getProperty(clave, true);
        String patron = valor != null ? valor.toString() : clave;
        return MessageFormat.format(patron, argumentos != null ? argumentos : new Object[0]);
    }

    private void refrescarListadoMiembrosActual() {
        // La tabla no guarda filas: MiembrosLazy consulta su página en BD con el
        // alcance vigente cada vez que se renderiza. Aquí solo se recalcula el
        // resumen de la iglesia elegida.
        if (iglesiaSeleccionado != null && iglesiaSeleccionado.getId() != null) {
            progreso = iglesiaPersonaService.calcularProgresoActualizacion(iglesiaSeleccionado.getId());
            if (restringidoAIglesia) {
                actualizarEstadoActaActualizacion();
            }
        }
    }

    public boolean isPuedeGenerarActaActualizacion() {
        return restringidoAIglesia && estadoActaActualizacion != null
                && estadoActaActualizacion.isPuedeGenerar();
    }

    /**
     * Ayuda del botón «Generar acta» con la causa concreta cuando está deshabilitado:
     * no alcanzar el mínimo de miembros registrados o no tenerlos todos actualizados.
     * La regla la decide el servicio; aquí solo se explica.
     */
    public String getAyudaActaActualizacion() {
        if (isPuedeGenerarActaActualizacion()) {
            return JsfUtil.getMessage("actaActualizacion.ayuda.generar");
        }
        int minimo = Constantes.getMinimoMiembrosActaActualizacion();
        if (estadoActaActualizacion != null && estadoActaActualizacion.getTotalMiembros() < minimo) {
            return JsfUtil.getMessage("actaActualizacion.error.minimo", minimo);
        }
        return JsfUtil.getMessage("actaActualizacion.ayuda.incompleta");
    }

    public boolean isActaActualizacionDisponible() {
        return estadoActaActualizacion != null && estadoActaActualizacion.getDocumentoId() != null;
    }

    public void generarActaActualizacionIglesia() {
        try {
            if (!restringidoAIglesia || iglesiaSeleccionado == null || iglesiaSeleccionado.getId() == null) {
                JsfUtil.addErrorMessage(mensaje("actaActualizacion.error.no.autorizada"));
                return;
            }
            actaActualizacionMiembrosService.generarParaUsuarioActual(iglesiaSeleccionado.getId());
            actualizarEstadoActaActualizacion();
            JsfUtil.addSuccessMessage(mensaje("actaActualizacion.exito.generada"));
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("ERROR AL GENERAR ACTA DE ACTUALIZACION DE MIEMBROS", e);
            JsfUtil.addErrorMessage(mensaje("actaActualizacion.error.generar"));
        }
        PrimeFaces.current().ajax().update("frmPersonas:panelResumenMiembros", ":frmGlobal:growlGlobal");
    }

    public StreamedContent getActaActualizacionDescargable() {
        if (!isActaActualizacionDisponible() || iglesiaSeleccionado == null
                || iglesiaSeleccionado.getId() == null) {
            return null;
        }
        try {
            Documentos documento = actaActualizacionMiembrosService.obtenerDocumentoParaUsuarioActual(
                    iglesiaSeleccionado.getId(), estadoActaActualizacion.getDocumentoId());
            return documentoBean.obtenerArchivo(documento);
        } catch (Exception e) {
            log.error("ERROR AL PREPARAR DESCARGA DEL ACTA DE ACTUALIZACION", e);
            return null;
        }
    }

    private void actualizarEstadoActaActualizacion() {
        if (!restringidoAIglesia || iglesiaSeleccionado == null || iglesiaSeleccionado.getId() == null) {
            estadoActaActualizacion = new EstadoActaActualizacionDTO(0, 0, false, null);
            return;
        }
        estadoActaActualizacion = actaActualizacionMiembrosService
                .evaluarParaUsuarioActual(iglesiaSeleccionado.getId());
    }

    private boolean puedeEliminarMiembro(IglesiaPersonaDTO miembro) {
        if (miembro == null || miembro.getId() == null) {
            return false;
        }
        if (!restringidoAIglesia) {
            return true;
        }
        Integer iglesiaAsignadaId = iglesiaSeleccionado != null ? iglesiaSeleccionado.getId() : null;
        Integer iglesiaMiembroId = miembro.getIglesia() != null ? miembro.getIglesia().getId() : null;
        return iglesiaAsignadaId != null && iglesiaAsignadaId.equals(iglesiaMiembroId);
    }

    // ── Auditoría de personas: mismos roles y criterios que Iglesias ─────────

    /**
     * La trazabilidad del registro (fechas de creación/actualización e historial
     * de auditoría) es información de control interno: solo la ve el
     * administrador del sistema. Mismo criterio que
     * {@code IglesiaController.isPuedeVerAuditoria()}.
     */
    public boolean isPuedeVerAuditoria() {
        return loginBean != null && loginBean.getRoles() != null
                && loginBean.getRoles().contains("SITEC-Administrador");
    }

    /**
     * Las personas eliminadas y su restauración son competencia del Tribunal y
     * del administrador. La vista usa este indicador para mostrar la opción; la
     * autorización real la aplica {@link PersonaBajaService}.
     */
    public boolean isPuedeGestionarEliminadas() {
        return loginBean != null && loginBean.getRoles() != null
                && (loginBean.getRoles().contains("SITEC-Administrador")
                || loginBean.getRoles().contains("SITEC-Tribunal"));
    }

    /**
     * Historial de cambios desde la columna Acciones. Se carga solo al abrir el
     * diálogo (una consulta) y con la misma validación de pertenencia que el
     * resto de acciones por fila.
     */
    public void verHistorialPersonaFila(IglesiaPersonaDTO miembro) {
        historialPersona = Collections.emptyList();
        personaHistorialSeleccionada = null;
        if (!isPuedeVerAuditoria()) {
            // El botón no se renderiza para otros roles; se valida igualmente en
            // el servidor para no depender solo de la vista.
            JsfUtil.addErrorMessage(mensaje("form.personas.hist.error.permiso"));
            return;
        }
        if (miembro == null || miembro.getPersona() == null || miembro.getPersona().getId() == null
                || !puedeEliminarMiembro(miembro)) {
            JsfUtil.addErrorMessage(mensaje("form.personas.hist.error.acceso"));
            return;
        }
        try {
            personaHistorialSeleccionada = miembro.getPersona();
            historialPersona = personaService.obtenerHistorial(miembro.getPersona().getId());
        } catch (Exception e) {
            log.error("Error al obtener historial de persona id={}", miembro.getPersona().getId(), e);
            personaHistorialSeleccionada = null;
            JsfUtil.addErrorMessage(mensaje("form.personas.hist.error.carga"));
        }
    }

    /** Libera el historial al cerrar el diálogo para no retenerlo en la vista. */
    public void cerrarHistorial() {
        historialPersona = Collections.emptyList();
        personaHistorialSeleccionada = null;
    }

    /**
     * Carga las personas eliminadas. Se invoca desde el botón «Ver eliminadas»:
     * esta lista no se consulta durante la carga inicial de la pantalla.
     */
    public void abrirEliminadas() {
        personasEliminadas = Collections.emptyList();
        personaEliminadaSeleccionada = null;
        if (!isPuedeGestionarEliminadas()) {
            JsfUtil.addErrorMessage(mensaje("form.personas.eliminadas.error.permiso"));
            return;
        }
        try {
            personasEliminadas = personaBajaService.listarEliminadas();
        } catch (Exception e) {
            log.error("Error al listar personas eliminadas", e);
            JsfUtil.addErrorMessage(mensaje("form.personas.eliminadas.error.carga"));
        }
    }

    /** Libera la lista al cerrar el panel para no retenerla en la vista. */
    public void cerrarEliminadas() {
        personasEliminadas = Collections.emptyList();
        personaEliminadaSeleccionada = null;
    }

    /** Restaura la persona seleccionada junto con lo que su baja desactivó. */
    public void restaurarPersona() {
        if (personaEliminadaSeleccionada == null || personaEliminadaSeleccionada.getId() == null) {
            return;
        }
        if (!isPuedeGestionarEliminadas()) {
            JsfUtil.addErrorMessage(mensaje("form.personas.eliminadas.error.permiso"));
            return;
        }
        Integer personaId = personaEliminadaSeleccionada.getId();
        try {
            PersonaBajaService.Resultado resultado = personaBajaService.restaurar(personaId);
            JsfUtil.addInfoMessage(mensaje("form.personas.eliminadas.exito",
                    resultado.persona(), resultado.membresias()));
            if (resultado.tieneOmitidas()) {
                // Cada persona solo puede pertenecer a una iglesia activa: esas
                // membresías se dejaron como estaban y el operador debe resolverlas.
                JsfUtil.addWarningMessage(describirOmitidas(resultado.omitidas()));
            }
            personaEliminadaSeleccionada = null;
            abrirEliminadas();
            // El botón declara el update de :frmPersonas; aquí solo se recarga el modelo.
            refrescarListadoMiembrosActual();
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Error al restaurar persona id={}", personaId, e);
            JsfUtil.addErrorMessage(mensaje("form.personas.eliminadas.error.restaurar"));
        }
    }

    /**
     * Mensaje de las membresías que no se pudieron reactivar. Se detallan los
     * primeros casos y se resume el resto para no saturar la pantalla.
     */
    private String describirOmitidas(List<String> omitidas) {
        int detalladas = Math.min(omitidas.size(), 3);
        StringBuilder detalle = new StringBuilder();
        for (int i = 0; i < detalladas; i++) {
            if (i > 0) {
                detalle.append("; ");
            }
            detalle.append(omitidas.get(i));
        }
        if (omitidas.size() > detalladas) {
            detalle.append(mensaje("form.personas.eliminadas.omitidas.resto",
                    omitidas.size() - detalladas));
        }
        return mensaje("form.personas.eliminadas.omitidas", omitidas.size(), detalle.toString());
    }

    /**
     * Genera y descarga el acta PDF de actualización de miembros de la iglesia
     * actualmente cargada en {@code iglesiaSeleccionado}. El acta marca
     * "PARCIAL" cuando el porcentaje no es 100% para evidenciar que aún hay
     * miembros pendientes. Solo lista los miembros ya marcados como actualizados.
     */
    public void generarActaActualizacion() {
        try {
            if (iglesiaSeleccionado == null || iglesiaSeleccionado.getId() == null) {
                JsfUtil.addWarningMessage("Seleccione una iglesia primero");
                return;
            }
            List<IglesiaPersonaDTO> actualizados = iglesiaPersonaService
                    .listarDTOsActualizadosPorIglesia(iglesiaSeleccionado.getId());

            String nombreReporte = "ACTA_ACTUALIZACION_" + iglesiaSeleccionado.getNombre()
                    .replaceAll("[^A-Za-z0-9]", "_");
            ec.com.antenasur.itext.ReportePFD.nuevoPDF(nombreReporte);

            String tituloPrefijo = isActualizacionCompleta() ? "" : "[PARCIAL " + getPorcentajeActualizacion() + "%] ";
            String titulo = tituloPrefijo + "ACTA DE ACTUALIZACIÓN DE MIEMBROS";
            String[] columnas = {"#", "CÉDULA", "NOMBRES", "FECHA ACTUALIZACIÓN"};
            float[] anchos = {30, 90, 200, 120};

            com.itextpdf.text.Font fuenteCab = ec.com.antenasur.util.Constantes.getFuenteCabeceraDefault(10);
            ec.com.antenasur.itext.ReportePFD.creaTablaCabecera(columnas.length, anchos, titulo, columnas, fuenteCab);

            // Encabezado adicional con datos de la iglesia
            ec.com.antenasur.itext.ReportePFD.addParagraph(
                    "Iglesia: " + iglesiaSeleccionado.getNombre()
                            + "  |  Comunidad: " + (iglesiaSeleccionado.getComunidad() == null ? "—" : iglesiaSeleccionado.getComunidad())
                            + "  |  Total miembros: " + getTotalMiembros()
                            + "  |  Actualizados: " + getMiembrosActualizados()
                            + "  |  Pendientes: " + getMiembrosPendientes());
            ec.com.antenasur.itext.ReportePFD.agregaParrafoEnBlanco();

            String[][] datos = new String[actualizados.size()][columnas.length];
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
            for (int i = 0; i < actualizados.size(); i++) {
                IglesiaPersonaDTO ip = actualizados.get(i);
                datos[i][0] = String.valueOf(i + 1);
                datos[i][1] = ip.getPersona() != null ? safe(ip.getPersona().getDocumento()) : "";
                datos[i][2] = ip.getPersona() != null ? safe(ip.getPersona().getNombres()) : "";
                datos[i][3] = ip.getFechaActualiza() != null ? fmt.format(ip.getFechaActualiza()) : "";
            }
            com.itextpdf.text.Font fuenteCont = ec.com.antenasur.util.Constantes.getFuenteContenidoDefault(9);
            ec.com.antenasur.itext.ReportePFD.creaContenidoTabla(datos, columnas, fuenteCont);

            String userName = (loginBean.getUsuario() != null && loginBean.getUsuario().getUsername() != null)
                    ? loginBean.getUsuario().getUsername() : "—";
            ec.com.antenasur.itext.ReportePFD.getFinalParagraph(userName);
            ec.com.antenasur.itext.ReportePFD.descargarPDF(nombreReporte);
        } catch (Exception e) {
            log.error("Error al generar acta de actualización", e);
            JsfUtil.addErrorMessage("No se pudo generar el acta. Intente nuevamente.");
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    public void exportarExcel() {
        try {
            // Todos los miembros del alcance con los filtros y el orden vigentes de la tabla,
            // no solo la página visible.
            List<IglesiaPersonaDTO> lista = iglesiaPersonaService.listarMiembrosCompleto(
                    modeloMiembros.filtroVigente(), modeloMiembros.campoOrden, modeloMiembros.descendente);
            String fecha = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
            String hora = new SimpleDateFormat("HH:mm:ss").format(new Date());

            String[] columnas = {
                mensaje("form.personas.exportar.col.numero"),
                mensaje("form.personas.exportar.col.identificacion"),
                mensaje("form.personas.exportar.col.nombres"),
                mensaje("form.personas.exportar.col.sexo"),
                mensaje("form.personas.exportar.col.iglesia"),
                mensaje("form.personas.exportar.col.padron"),
                mensaje("form.personas.exportar.col.revision"),
                mensaje("form.personas.exportar.col.inconsistencia"),
                mensaje("form.personas.exportar.col.cantidad.iglesias"),
                mensaje("form.personas.exportar.col.fecha.actualizacion")
            };
            int[] anchos = {1800, 4500, 9500, 2500, 9000,
                4500, 4500, 6500, 4500, 6000};

            String[][] datos = new String[lista.size()][columnas.length];
            SimpleDateFormat fmtFechaHora = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            for (int i = 0; i < lista.size(); i++) {
                IglesiaPersonaDTO ip = lista.get(i);
                PersonaDTO persona = ip.getPersona();
                IglesiaDTO iglesia = ip.getIglesia();

                datos[i][0] = String.valueOf(i + 1);
                datos[i][1] = persona != null ? safe(persona.getDocumento()) : "";
                datos[i][2] = persona != null
                        ? (safe(persona.getApellidos()) + " " + safe(persona.getNombres())).trim() : "";
                datos[i][3] = persona != null ? safe(persona.getSexo()) : "";
                datos[i][4] = iglesia != null ? safe(iglesia.getNombre())
                        : (iglesiaSeleccionado != null ? safe(iglesiaSeleccionado.getNombre()) : "");
                datos[i][5] = Boolean.TRUE.equals(ip.getHabilitadoPadron())
                        ? mensaje("form.personas.exportar.estado.habilitado")
                        : mensaje("form.personas.exportar.estado.no.habilitado");
                datos[i][6] = Boolean.TRUE.equals(ip.getActualizada())
                        ? mensaje("form.personas.exportar.estado.revisado")
                        : mensaje("form.personas.exportar.estado.pendiente");
                datos[i][7] = Boolean.TRUE.equals(ip.getInconsistenciaIglesias())
                        ? mensaje("form.personas.inconsistencia.con")
                        : mensaje("form.personas.inconsistencia.sin");
                datos[i][8] = String.valueOf(ip.getCantidadIglesiasActivas() != null
                        ? ip.getCantidadIglesiasActivas() : 0);
                datos[i][9] = ip.getFechaActualiza() != null
                        ? fmtFechaHora.format(ip.getFechaActualiza()) : "";
            }

            synchronized (ReporteXLSX.class) {
                ReporteXLSX.nuevoExcel(mensaje("form.personas.exportar.titulo"));
                ReporteXLSX.creaEspacioInformativo(
                        fecha, hora, ReporteXLSX.getNombreUsuarioAutenticado());
                ReporteXLSX.creaCabeceraTabla(columnas, anchos);
                ReporteXLSX.creaContenidoTabla(datos, columnas);
                ReporteXLSX.setFinalParagraph(lista.size());
                String marcaTiempo = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
                ReporteXLSX.descargarExcel("personas_" + marcaTiempo);
            }
        } catch (Exception e) {
            log.error("Error al exportar listado de personas a Excel", e);
            JsfUtil.addErrorMessage(mensaje("form.personas.exportar.error"));
        }
    }

    public void handleFileUpload(FileUploadEvent event) {
        try {
            file = event.getFile();
            if (file != null && file.getContent() != null && file.getContent().length > 0 && file.getFileName() != null) {
                excelMigracion = new XSSFWorkbook(file.getInputStream());
                if (excelMigracion == null) {
                    JsfUtil.addFatalMessage("Error al procesar archivo");
                } else {
                    cargaArchivoABD();
                    JsfUtil.addInfoMessage("Archivo cargado correctamente");
                }
            }
        } catch (Exception e) {
            JsfUtil.addFatalMessage("Error en formato de archivo");
            file = null;
            log.error("ERROR AL CARGAR ARCHIVO", e);
        }
    }

    public void cargaArchivoABD() {
        if (file != null) {
            procesaArchivo(file);
        }
    }

    public boolean guardarArchivoExcel() {
        Path archivoAlmacenado = null;
        try {
            if (file == null || file.getContent() == null || file.getContent().length == 0
                    || file.getFileName() == null || !file.getFileName().toLowerCase().endsWith(".xlsx")) {
                throw new IOException(Constantes.getMensaje("documentos.error.xlsx"));
            }
            if (iglesiaSeleccionado == null || iglesiaSeleccionado.getId() == null) {
                throw new IOException(Constantes.getMensaje("documentos.error.entidad"));
            }
            String nombreArchivo = iglesiaSeleccionado.getNombre() + "-"
                    + JsfUtil.getFechaStringYYYYMMddHHmm(new Date()) + "-"
                    + UUID.randomUUID().toString().substring(0, 8);
            byte[] contenido = file.getContent();
            archivoAlmacenado = RepositorioDocumentos.escribirAtomico(
                    "listas-miembros", nombreArchivo + ".xlsx", contenido);

            Documentos documentoNuevo = new Documentos(nombreArchivo, archivoAlmacenado.toString(),
                    new TipoDocumento(Constantes.LISTA_MIEMBROS), iglesiaSeleccionado.getId(), ".xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", nombreArchivo);
            documentoNuevo.setHashSha256(RepositorioDocumentos.sha256(contenido));
            Documentos persistido = documentoBean.guardarDocumentoPersistido(documentoNuevo);
            if (persistido == null || persistido.getId() == null) {
                throw new IOException(Constantes.getMensaje("documentos.error.metadata"));
            }

            JsfUtil.addSuccessMessage(Constantes.getMensaje("documentos.success.stored", nombreArchivo));
            return true;
        } catch (Exception e) {
            RepositorioDocumentos.eliminarSilencioso(archivoAlmacenado);
            log.error("ERROR AL GUARDAR ARCHIVOS", e);
            JsfUtil.addErrorMessage(e.getMessage() != null
                    ? e.getMessage() : Constantes.getMensaje("documentos.error.storage"));
            return false;
        }
    }

    public void exportarInconsistenciasExcel() {
        if (!puedeGenerarReporteInconsistencias) {
            JsfUtil.addErrorMessage(mensaje("form.personas.inconsistencias.error.permiso"));
            return;
        }
        try {
            List<IglesiaPersonaDTO> relaciones = iglesiaPersonaService.listarInconsistenciasIglesias();
            if (relaciones.isEmpty()) {
                JsfUtil.addInfoMessage(mensaje("form.personas.inconsistencias.sin.datos"));
                return;
            }
            String fecha = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
            String hora = new SimpleDateFormat("HH:mm:ss").format(new Date());
            String[] columnas = {
                mensaje("form.personas.inconsistencias.reporte.col.numero"),
                mensaje("form.personas.inconsistencias.reporte.col.identificacion"),
                mensaje("form.personas.inconsistencias.reporte.col.nombres"),
                mensaje("form.personas.inconsistencias.reporte.col.cantidad"),
                mensaje("form.personas.inconsistencias.reporte.col.iglesias"),
                mensaje("form.personas.inconsistencias.reporte.col.relacion"),
                mensaje("form.personas.inconsistencias.reporte.col.provincia"),
                mensaje("form.personas.inconsistencias.reporte.col.canton"),
                mensaje("form.personas.inconsistencias.reporte.col.parroquia"),
                mensaje("form.personas.inconsistencias.reporte.col.estado.relacion"),
                mensaje("form.personas.inconsistencias.reporte.col.estado.inconsistencia"),
                mensaje("form.personas.inconsistencias.reporte.col.fecha")
            };
            int[] anchos = {1800, 4500, 9500, 4500, 12000, 9000,
                5500, 5500, 5500, 4500, 6500, 6000};
            String[][] datos = new String[relaciones.size()][columnas.length];
            String fechaGeneracion = fecha + " " + hora;
            for (int i = 0; i < relaciones.size(); i++) {
                IglesiaPersonaDTO relacion = relaciones.get(i);
                PersonaDTO persona = relacion.getPersona();
                IglesiaDTO iglesia = relacion.getIglesia();
                datos[i][0] = String.valueOf(i + 1);
                datos[i][1] = persona != null ? safe(persona.getDocumento()) : "";
                datos[i][2] = persona != null
                        ? (safe(persona.getApellidos()) + " " + safe(persona.getNombres())).trim() : "";
                datos[i][3] = String.valueOf(relacion.getCantidadIglesiasActivas());
                datos[i][4] = safe(relacion.getIglesiasActivas());
                datos[i][5] = iglesia != null ? safe(iglesia.getNombre()) : "";
                datos[i][6] = iglesia != null ? safe(iglesia.getProvinciaNombre()) : "";
                datos[i][7] = iglesia != null ? safe(iglesia.getCantonNombre()) : "";
                datos[i][8] = iglesia != null ? safe(iglesia.getUbicacionNombre()) : "";
                datos[i][9] = Boolean.TRUE.equals(relacion.getEstadoRelacion())
                        ? mensaje("form.personas.inconsistencias.estado.activa")
                        : mensaje("form.personas.inconsistencias.estado.inactiva");
                datos[i][10] = Boolean.TRUE.equals(relacion.getInconsistenciaIglesias())
                        ? mensaje("form.personas.inconsistencias.estado.pendiente")
                        : mensaje("form.personas.inconsistencias.estado.historica");
                datos[i][11] = fechaGeneracion;
            }
            synchronized (ReporteXLSX.class) {
                ReporteXLSX.nuevoExcel(mensaje("form.personas.inconsistencias.reporte.titulo"));
                ReporteXLSX.creaEspacioInformativo(
                        fecha, hora, ReporteXLSX.getNombreUsuarioAutenticado());
                ReporteXLSX.creaCabeceraTabla(columnas, anchos);
                ReporteXLSX.creaContenidoTabla(datos, columnas);
                ReporteXLSX.setFinalParagraph(relaciones.size());
                String marcaTiempo = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
                ReporteXLSX.descargarExcel("personas_inconsistencias_" + marcaTiempo);
            }
        } catch (IglesiaPersonaException e) {
            JsfUtil.addErrorMessage(mensaje(e.getMessageKey(), e.getArguments()));
        } catch (Exception e) {
            log.error("Error al generar reporte de inconsistencias de iglesias", e);
            JsfUtil.addErrorMessage(mensaje("form.personas.inconsistencias.error.reporte"));
        }
    }

    public void procesaArchivo(UploadedFile file) {
        try {
            if (file == null || file.getContent() == null || file.getContent().length == 0
                    || file.getFileName() == null) {
                return;
            }
            if (!guardarArchivoExcel()) {
                return;
            }
            if (excelMigracion == null) {
                JsfUtil.addWarningMessage("Archivo formato incorrecto");
                return;
            }

            List<FilaPadronImportadaDTO> filas = ExcelPadronParser.parsear(excelMigracion);
            for (FilaPadronImportadaDTO filaDto : filas) {
                Mesa mesa = filaDto.getNombreMesa() != null
                        ? mesaService.buscaPorNombreMesa(filaDto.getNombreMesa()) : null;
                Geograp ubicacion = filaDto.getUbicacionId() != null
                        ? geograpService.find(filaDto.getUbicacionId()) : null;
                if (ubicacion != null) {
                    filaDto.getIglesia().setUbicacion(ubicacion);
                }
                padronService.importarFilaPadron(filaDto.getPersona(), filaDto.getIglesia(), mesa);
            }

            listaPersonas = personaService.listarDTOs();
            excelMigracion.close();
        } catch (Exception e) {
            log.error("ERROR AL CARGAR ARCHIVO", e);
        }
    }

    /**
     * Modelo paginado de la tabla de miembros. Traduce filtros y orden de las columnas
     * de PrimeFaces a {@link FiltroMiembrosDTO} y conserva los últimos aplicados para
     * que la exportación use exactamente los mismos criterios.
     */
    public class MiembrosLazy extends LazyDataModel<IglesiaPersonaDTO> {

        private static final long serialVersionUID = 1L;
        private static final String VARIABLE_FILA = "iglPers.";

        private List<IglesiaPersonaDTO> pagina = new ArrayList<>();
        private Map<String, FilterMeta> filtrosColumna = new HashMap<>();
        private String campoOrden;
        private boolean descendente;

        @Override
        public int count(Map<String, FilterMeta> filtros) {
            FiltroMiembrosDTO filtro = construirFiltro(filtros);
            return filtro.tieneAlcance() ? (int) iglesiaPersonaService.contarMiembros(filtro) : 0;
        }

        @Override
        public List<IglesiaPersonaDTO> load(int primero, int tamanio, Map<String, SortMeta> orden,
                Map<String, FilterMeta> filtros) {
            filtrosColumna = filtros == null ? new HashMap<>() : new HashMap<>(filtros);
            SortMeta activo = orden == null ? null : orden.values().stream()
                    .filter(s -> s.getOrder() != null && (s.getOrder().isAscending() || s.getOrder().isDescending()))
                    .findFirst().orElse(null);
            campoOrden = activo == null ? null : campo(activo.getField(), activo.getSortBy());
            descendente = activo != null && activo.getOrder() == SortOrder.DESCENDING;
            FiltroMiembrosDTO filtro = construirFiltro(filtrosColumna);
            pagina = filtro.tieneAlcance()
                    ? iglesiaPersonaService.listarMiembros(filtro, primero, tamanio, campoOrden, descendente)
                    : new ArrayList<>();
            return pagina;
        }

        /** Alcance vigente más los últimos filtros de columna aplicados en la tabla. */
        FiltroMiembrosDTO filtroVigente() {
            return construirFiltro(filtrosColumna);
        }

        private FiltroMiembrosDTO construirFiltro(Map<String, FilterMeta> filtros) {
            FiltroMiembrosDTO filtro = filtroAlcance();
            if (filtros == null) {
                return filtro;
            }
            for (Map.Entry<String, FilterMeta> entrada : filtros.entrySet()) {
                FilterMeta meta = entrada.getValue();
                Object valor = meta == null ? null : meta.getFilterValue();
                if (valor == null || String.valueOf(valor).isBlank()) {
                    continue;
                }
                String texto = String.valueOf(valor).trim();
                if (meta.isGlobalFilter() || FilterMeta.GLOBAL_FILTER_KEY.equals(entrada.getKey())) {
                    filtro.setBusqueda(texto);
                    continue;
                }
                String campo = campo(meta.getField(), meta.getFilterBy());
                switch (campo == null ? entrada.getKey() : campo) {
                    case "persona.documento" -> filtro.setDocumento(texto);
                    case "persona.nombres" -> filtro.setNombres(texto);
                    case "habilitadoPadron" -> filtro.setHabilitado(Boolean.valueOf(texto));
                    case "actualizada" -> filtro.setRevisado(Boolean.valueOf(texto));
                    case "tieneInconsistencia" -> filtro.setInconsistencia(Boolean.valueOf(texto));
                    default -> { }
                }
            }
            return filtro;
        }

        /** Campo de la columna: su atributo field o, si falta, la expresión #{iglPers.campo}. */
        private String campo(String field, ValueExpression expresion) {
            String valor = field;
            if ((valor == null || valor.isBlank()) && expresion != null) {
                valor = expresion.getExpressionString();
            }
            if (valor == null) {
                return null;
            }
            valor = valor.trim();
            if (valor.startsWith("#{") && valor.endsWith("}")) {
                valor = valor.substring(2, valor.length() - 1);
            }
            return valor.startsWith(VARIABLE_FILA) ? valor.substring(VARIABLE_FILA.length()) : valor;
        }

        @Override
        public String getRowKey(IglesiaPersonaDTO miembro) {
            return miembro == null || miembro.getId() == null ? null : miembro.getId().toString();
        }

        /** La selección puede abarcar varias páginas: se busca en la página y en lo ya seleccionado. */
        @Override
        public IglesiaPersonaDTO getRowData(String clave) {
            List<IglesiaPersonaDTO> seleccion = listaIglesiaPersonaSeleccionados == null
                    ? List.of() : listaIglesiaPersonaSeleccionados;
            return java.util.stream.Stream.concat(pagina.stream(), seleccion.stream())
                    .filter(m -> m.getId() != null && m.getId().toString().equals(clave))
                    .findFirst().orElse(null);
        }
    }
}
