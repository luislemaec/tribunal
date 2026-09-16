package ec.com.antenasur.report;

import java.io.Serializable;
import java.text.SimpleDateFormat;
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

import com.itextpdf.text.Font;

import ec.com.antenasur.audit.CatalogoActividades;
import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.bean.ProcesoBean;
import ec.com.antenasur.dto.ActividadAuditoriaDTO;
import ec.com.antenasur.dto.FiltroActividadAuditoriaDTO;
import ec.com.antenasur.itext.ReportePFD;
import ec.com.antenasur.model.tec.Proceso;
import ec.com.antenasur.service.tec.ProcesoService;
import ec.com.antenasur.util.Constantes;

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
    private List<String> usuariosAuditoria = Collections.emptyList();
    private List<ActividadAuditoriaDTO> auditoriasCargadas = Collections.emptyList();
    private int primerRegistro;
    private ActividadAuditoriaDTO registroSeleccionado;

    private List<SelectItem> accionesAuditoria = Collections.emptyList();

    public ProcesoController() {
        super("ACTIVIDAD INTERNA", new float[]{16, 48, 45, 45, 70, 34, 130, 40},
                new int[]{1200, 4000, 4000, 4000, 6000, 3000, 12000, 3000},
                new String[]{"Nro", "FECHA / HORA", "USUARIO", "MÓDULO", "ACCIÓN", "RESULTADO", "DETALLE", "IP"}, 0);
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

    public void exportaPDF() {
        try {
            List<Proceso> registros = procesoService.listarAuditoriaParaReporte(filtro);
            setListaDatos(new String[registros.size()][getNumeroColumnas()]);
            SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            int fila = 0;
            for (Proceso item : registros) {
                ActividadAuditoriaDTO actividad = ActividadAuditoriaDTO.fromEntity(item);
                getListaDatos()[fila][0] = String.valueOf(fila + 1);
                getListaDatos()[fila][1] = actividad.getFecha() != null ? formatoFecha.format(actividad.getFecha()) : "";
                getListaDatos()[fila][2] = valor(actividad.getUsuario());
                getListaDatos()[fila][3] = actividad.getModulo();
                getListaDatos()[fila][4] = actividad.getAccion();
                getListaDatos()[fila][5] = actividad.getResultado();
                getListaDatos()[fila][6] = actividad.getDetalle();
                getListaDatos()[fila][7] = valor(actividad.getIp());
                fila++;
            }
            Font fuenteCabecera = Constantes.getFuenteCabeceraDefault(9);
            ReportePFD.nuevoPDFHorizontal(getNombreReporte());
            ReportePFD.creaTablaCabecera(getNumeroColumnas(), getTamanioColumnasPDF(), getNombreReporte(),
                    getNombresColumnas(), fuenteCabecera);
            ReportePFD.creaContenidoTabla(getListaDatos(), getNombresColumnas(), Constantes.getFuenteContenidoDefault(8));
            ReportePFD.getFinalParagraph(loginBean.getUsuario().getUsername());
            ReportePFD.descargarPDF(getNombreReporte());
            procesoBean.okActivityRegister("DESCARGA REPORTE(PDF) " + getNombreReporte(),
                    "NÚMERO DE REGISTROS: " + registros.size());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(ProcesoController.class).error("Error al exportar auditoría", e);
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
    public List<String> getUsuariosAuditoria() { return usuariosAuditoria; }
    public int getPrimerRegistro() { return primerRegistro; }
    public void setPrimerRegistro(int primerRegistro) { this.primerRegistro = primerRegistro; }
    public ActividadAuditoriaDTO getRegistroSeleccionado() { return registroSeleccionado; }
    public void setRegistroSeleccionado(ActividadAuditoriaDTO registroSeleccionado) {
        this.registroSeleccionado = registroSeleccionado;
    }
}
