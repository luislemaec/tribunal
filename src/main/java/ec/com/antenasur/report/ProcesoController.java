package ec.com.antenasur.report;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.faces.model.SelectItem;
import jakarta.faces.model.SelectItemGroup;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;


import ec.com.antenasur.audit.CatalogoActividades;
import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.bean.ProcesoBean;
import ec.com.antenasur.dto.ActividadAuditoriaDTO;
import ec.com.antenasur.dto.FiltroActividadAuditoriaDTO;
import ec.com.antenasur.dto.UsuarioAuditoriaDTO;
import ec.com.antenasur.itext.ReporteXLSX;
import ec.com.antenasur.service.tec.ProcesoService;
import ec.com.antenasur.util.JsfUtil;

/** Consulta de la bitácora funcional tec.procesos. */
@Named
@ViewScoped
public class ProcesoController extends ReportTemplateController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private LoginBean loginBean;

    @Inject
    private ProcesoBean procesoBean;

    @Inject
    private ProcesoService procesoService;

    private FiltroActividadAuditoriaDTO filtro;
    private LazyDataModel<ActividadAuditoriaDTO> auditoriasLazy;
    private List<UsuarioAuditoriaDTO> usuariosAuditoria = Collections.emptyList();
    private List<ActividadAuditoriaDTO> auditoriasCargadas = Collections.emptyList();
    private int primerRegistro;
    private ActividadAuditoriaDTO registroSeleccionado;

    private List<SelectItem> accionesAuditoria = Collections.emptyList();

    public ProcesoController() {
        // Solo se exporta a Excel: no hay anchos de PDF. Nombre, usuario y rol van en columnas
        // separadas para poder filtrarlos en la hoja.
        super("ACTIVIDAD INTERNA", null,
                new int[]{1200, 4500, 3600, 9000, 5000, 4000, 6000, 3000, 12000, 3600},
                new String[]{"Nro", "FECHA / HORA", "USUARIO", "NOMBRE", "ROL", "MÓDULO", "ACCIÓN", "RESULTADO",
                    "DETALLE", "IP"}, 0);
    }

    @PostConstruct
    private void init() {
        filtro = new FiltroActividadAuditoriaDTO();
        if (isAdministrador()) usuariosAuditoria = procesoService.listarUsuariosAuditoria();
        accionesAuditoria = construirAccionesAuditoria();
        auditoriasLazy = new LazyDataModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                return procesoService.contarAuditoria(filtro);
            }

            @Override
            public List<ActividadAuditoriaDTO> load(int first, int pageSize,
                    Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
                auditoriasCargadas = procesoService.buscarAuditoria(filtro, first, pageSize);
                return auditoriasCargadas;
            }

            @Override
            public String getRowKey(ActividadAuditoriaDTO actividad) {
                return actividad != null && actividad.getId() != null
                        ? actividad.getId().toString() : null;
            }

            @Override
            public ActividadAuditoriaDTO getRowData(String rowKey) {
                if (rowKey == null || auditoriasCargadas == null) {
                    return null;
                }
                for (ActividadAuditoriaDTO actividad : auditoriasCargadas) {
                    if (rowKey.equals(getRowKey(actividad))) {
                        return actividad;
                    }
                }
                return null;
            }
        };
    }

    public void buscar() {
        primerRegistro = 0;
    }

    public void limpiarFiltros() {
        filtro = new FiltroActividadAuditoriaDTO();
        primerRegistro = 0;
    }

    public boolean isAdministrador() {
        return loginBean != null && loginBean.getRoles() != null
                && loginBean.getRoles().contains("SITEC-Administrador");
    }

    /** Opciones del filtro Acción agrupadas por módulo, desde el catálogo único. */
    private static List<SelectItem> construirAccionesAuditoria() {
        List<SelectItem> grupos = new ArrayList<>();
        CatalogoActividades.accionesPorModulo().forEach((modulo, acciones) -> {
            SelectItem[] opciones = acciones.stream().map(a -> new SelectItem(a, a)).toArray(SelectItem[]::new);
            grupos.add(new SelectItemGroup(modulo, null, false, opciones));
        });
        return Collections.unmodifiableList(grupos);
    }

    /**
     * Exporta a Excel todas las actividades que cumplen los filtros (no solo la página
     * visible), con el mismo alcance que la tabla: Administrador ve todo, los demás roles
     * solo sus registros. Usa la infraestructura común ReporteXLSX (encabezado institucional,
     * responsable y total), igual que Personas, Iglesias y Padrón.
     */
    public void exportarExcel() {
        try {
            List<ActividadAuditoriaDTO> registros = procesoService.listarAuditoriaParaExportar(filtro);
            String[][] datos = new String[registros.size()][getNumeroColumnas()];
            SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            for (int fila = 0; fila < registros.size(); fila++) {
                ActividadAuditoriaDTO actividad = registros.get(fila);
                datos[fila][0] = String.valueOf(fila + 1);
                datos[fila][1] = actividad.getFecha() != null ? formatoFecha.format(actividad.getFecha()) : "";
                datos[fila][2] = valor(actividad.getUsuario());
                datos[fila][3] = valor(actividad.getNombreUsuario());
                datos[fila][4] = valor(actividad.getRolesUsuario());
                datos[fila][5] = actividad.getModulo();
                datos[fila][6] = actividad.getAccion();
                datos[fila][7] = actividad.getResultado();
                datos[fila][8] = actividad.getDetalle();
                datos[fila][9] = valor(actividad.getIp());
            }
            Date ahora = new Date();
            synchronized (ReporteXLSX.class) {
                ReporteXLSX.nuevoExcel(getNombreReporte());
                ReporteXLSX.creaEspacioInformativo(new SimpleDateFormat("dd/MM/yyyy").format(ahora),
                        new SimpleDateFormat("HH:mm:ss").format(ahora), ReporteXLSX.getNombreUsuarioAutenticado());
                ReporteXLSX.creaCabeceraTabla(getNombresColumnas(), getTamanioColumnasXLS());
                ReporteXLSX.creaContenidoTabla(datos, getNombresColumnas());
                ReporteXLSX.setFinalParagraph(registros.size());
                ReporteXLSX.descargarExcel("actividad_interna_" + new SimpleDateFormat("yyyyMMdd_HHmm").format(ahora));
            }
            procesoBean.okActivityRegister("DESCARGA REPORTE(XLS) " + getNombreReporte(),
                    "NÚMERO DE REGISTROS: " + registros.size());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(ProcesoController.class).error("Error al exportar auditoría a Excel", e);
            JsfUtil.addErrorMessage("No se pudo generar el archivo Excel.");
        }
    }

    private static String valor(String texto) {
        return texto == null || texto.isBlank() ? "N/A" : texto;
    }

    public List<SelectItem> getAccionesAuditoria() { return accionesAuditoria; }
    public List<String> getModulosAuditoria() { return CatalogoActividades.modulos(); }
    public List<String> getResultadosAuditoria() { return CatalogoActividades.resultados(); }
    public FiltroActividadAuditoriaDTO getFiltro() { return filtro; }
    public LazyDataModel<ActividadAuditoriaDTO> getAuditoriasLazy() { return auditoriasLazy; }
    public List<UsuarioAuditoriaDTO> getUsuariosAuditoria() { return usuariosAuditoria; }
    public int getPrimerRegistro() { return primerRegistro; }
    public void setPrimerRegistro(int primerRegistro) { this.primerRegistro = primerRegistro; }
    public ActividadAuditoriaDTO getRegistroSeleccionado() { return registroSeleccionado; }
    public void setRegistroSeleccionado(ActividadAuditoriaDTO registroSeleccionado) {
        this.registroSeleccionado = registroSeleccionado;
    }
}
