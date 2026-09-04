package ec.com.antenasur.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

import ec.com.antenasur.bean.DocumentoBean;
import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.dto.FilaPadronImportadaDTO;
import ec.com.antenasur.dto.EstadoActaActualizacionDTO;
import ec.com.antenasur.dto.IglesiaDTO;
import ec.com.antenasur.dto.IglesiaPersonaDTO;
import ec.com.antenasur.dto.PersonaDTO;
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

    @Setter
    @Getter
    private Geograp parroquiaSeleccionado, cantonSeleccionado;

    @Setter
    @Getter
    private List<Geograp> cantones, parroquias;

    @Setter
    @Getter
    private List<IglesiaDTO> listaIglesias;

    @Setter
    @Getter
    private List<IglesiaPersonaDTO> listaIglesiaPersona, listaIglesiaPersonaSeleccionados, listaIglesiaPersonaExistente;

    @Setter
    @Getter
    private List<IglesiaPersonaDTO> listaIglesiaPersonaFiltrada;

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
            listaIglesiaPersona = new ArrayList<>();
            parroquiaSeleccionado = cantonSeleccionado = new Geograp();
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
            puedeRegularizarIglesias = esUsuarioTribunal();
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
                listaIglesiaPersona = iglesiaPersonaService.listarDTOsPorIglesia(iglesiaId);
                progreso = iglesiaPersonaService.calcularProgresoActualizacion(iglesiaId);
                actualizarEstadoActaActualizacion();
                return;
            }

            // Camino normal (admin global): permite filtrar por cantón/parroquia.
            cantones = geograpService.findByFatherId(7);
            listaIglesias = iglesiaService.listarDTOs();
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

    public void obtieneParroquias() {
        if (cantonSeleccionado.getId() != null) {
            cantonSeleccionado = geograpService.find(cantonSeleccionado.getId());
            parroquias = geograpService.findByFatherId(cantonSeleccionado.getId());
            listaIglesiaPersona = iglesiaPersonaService.listarDTOsPorParroquias(parroquias);
            listaIglesias = iglesiaService.listarDTOsPorParroquias(parroquias);
        } else {
            if (parroquias != null) {
                parroquias.clear();
            }
            iglesiaSeleccionado = new IglesiaDTO();
            listaIglesias.clear();
            listaIglesiaPersona.clear();
        }
    }

    public void obtieneIglesiasPorParroquia() {
        if (parroquiaSeleccionado.getId() != null) {
            parroquiaSeleccionado = geograpService.find(parroquiaSeleccionado.getId());
            List<Geograp> parroquiasTmp = new ArrayList<>();
            parroquiasTmp.add(parroquiaSeleccionado);
            listaIglesias = iglesiaService.listarDTOsPorParroquias(parroquiasTmp);
            listaIglesiaPersona = iglesiaPersonaService.listarDTOsPorParroquias(parroquiasTmp);
            if (listaIglesias == null || listaIglesias.isEmpty()) {
                JsfUtil.addWarningMessage("No existe registro de Iglesias en " + parroquiaSeleccionado.getName());
            } else {
                JsfUtil.addInfoMessage(listaIglesias.size() + " Iglesias registradas");
            }
        } else {
            iglesiaSeleccionado = new IglesiaDTO();
            listaIglesias.clear();
            listaIglesiaPersona.clear();
        }
    }

    public void obtienePersonasPorIglesias() {
        if (iglesiaSeleccionado != null && iglesiaSeleccionado.getId() != null) {
            iglesiaSeleccionado = iglesiaService.obtenerDTOPorId(iglesiaSeleccionado.getId());
            listaIglesiaPersona = iglesiaPersonaService.listarDTOsPorIglesia(iglesiaSeleccionado.getId());
            if (listaIglesiaPersona == null || listaIglesiaPersona.isEmpty()) {
                JsfUtil.addWarningMessage("No existe registro de personas en " + iglesiaSeleccionado.getNombre());
            } else {
                JsfUtil.addInfoMessage(listaIglesiaPersona.size() + " personas registradas");
            }
        } else {
            listaIglesiaPersona.clear();
        }
    }

    public void inicializaPersonaSeleccionado() {
        iglesiaPersonaSeleccionado = new IglesiaPersonaDTO();
        iglesiaPersonaSeleccionado.setPersona(new PersonaDTO());
        iglesiaPersonaSeleccionado.setIglesia(new IglesiaDTO());
        // Por defecto habilitado para padrón: el admin puede desmarcarlo
        iglesiaPersonaSeleccionado.setHabilitadoPadron(Boolean.TRUE);
        if (!restringidoAIglesia) {
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
                JsfUtil.addErrorMessage(mensaje("form.personas.error.cronograma"));
                PrimeFaces.current().ajax().addCallbackParam("validationFailed", true);
                return;
            }
            IglesiaPersonaDTO persistido = iglesiaPersonaService.guardarDesdeDTO(iglesiaPersonaSeleccionado);
            if (persistido != null) {
                String nombreMiembro = (persistido.getPersona() != null
                        && persistido.getPersona().getNombres() != null)
                        ? persistido.getPersona().getNombres() : "";
                boolean enPadron = Boolean.TRUE.equals(persistido.getHabilitadoPadron());
                String estadoPadron = enPadron ? " · Habilitado para padrón" : " · No habilitado para padrón";
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
                JsfUtil.addErrorMessage(mensaje("form.personas.error.guardar"));
                PrimeFaces.current().ajax().addCallbackParam("validationFailed", true);
            }
        } catch (IglesiaPersonaException e) {
            JsfUtil.addErrorMessage(mensaje(e.getMessageKey(), e.getArguments()));
            PrimeFaces.current().ajax().addCallbackParam("validationFailed", true);
            return;
        } catch (Exception e) {
            log.error("Error al guardar persona", e);
            JsfUtil.addErrorMessage(mensaje("form.personas.error.guardar"));
            PrimeFaces.current().ajax().addCallbackParam("validationFailed", true);
            return;
        }
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
        if (iglesiaSeleccionado != null && iglesiaSeleccionado.getId() != null) {
            listaIglesiaPersona = iglesiaPersonaService.listarDTOsPorIglesia(iglesiaSeleccionado.getId());
            progreso = iglesiaPersonaService.calcularProgresoActualizacion(iglesiaSeleccionado.getId());
            if (restringidoAIglesia) {
                actualizarEstadoActaActualizacion();
            }
            return;
        }
        if (parroquiaSeleccionado != null && parroquiaSeleccionado.getId() != null) {
            List<Geograp> parroquiasFiltro = new ArrayList<>();
            parroquiasFiltro.add(parroquiaSeleccionado);
            listaIglesiaPersona = iglesiaPersonaService.listarDTOsPorParroquias(parroquiasFiltro);
            return;
        }
        if (parroquias != null && !parroquias.isEmpty()) {
            listaIglesiaPersona = iglesiaPersonaService.listarDTOsPorParroquias(parroquias);
            return;
        }
        listaIglesiaPersona = iglesiaPersonaService.listarDTOs();
    }

    public boolean isPuedeGenerarActaActualizacion() {
        return restringidoAIglesia && estadoActaActualizacion != null
                && estadoActaActualizacion.isPuedeGenerar();
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
            List<IglesiaPersonaDTO> lista = listaIglesiaPersonaFiltrada != null
                    ? new ArrayList<>(listaIglesiaPersonaFiltrada)
                    : (listaIglesiaPersona != null
                            ? new ArrayList<>(listaIglesiaPersona) : new ArrayList<>());
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
}
