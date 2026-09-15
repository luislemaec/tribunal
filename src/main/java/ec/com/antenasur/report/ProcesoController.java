package ec.com.antenasur.report;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;

import com.itextpdf.text.Font;

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
    private int primerRegistro;

    public ProcesoController() {
        super("ACTIVIDAD INTERNA", new float[]{20, 100, 40, 50, 50},
                new int[]{1200, 3000, 4000, 10000, 4000},
                new String[]{"Nro", "ACTIVIDAD", "IP", "USUARIO CREA", "FECHA REGISTRO"}, 0);
    }

    @PostConstruct
    private void init() {
        filtro = new FiltroActividadAuditoriaDTO();
        if (isAdministrador()) usuariosAuditoria = procesoService.listarUsuariosAuditoria();
        auditoriasLazy = new LazyDataModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                return procesoService.contarAuditoria(filtro);
            }

            @Override
            public List<ActividadAuditoriaDTO> load(int first, int pageSize,
                    Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
                return procesoService.buscarAuditoria(filtro, first, pageSize);
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

    public void exportaPDF() {
        try {
            List<Proceso> registros = procesoService.listarAuditoriaParaReporte(filtro);
            setListaDatos(new String[registros.size()][getNumeroColumnas()]);
            int fila = 0;
            for (Proceso item : registros) {
                getListaDatos()[fila][0] = String.valueOf(fila + 1);
                getListaDatos()[fila][1] = item.getActividad();
                getListaDatos()[fila][2] = item.getIp();
                getListaDatos()[fila][3] = item.getUsuarioCrea();
                getListaDatos()[fila][4] = item.getFechaCrea() != null ? item.getFechaCrea().toString() : "";
                fila++;
            }
            Font fuenteCabecera = Constantes.getFuenteCabeceraDefault(10);
            ReportePFD.nuevoPDF(getNombreReporte());
            ReportePFD.creaTablaCabecera(getNumeroColumnas(), getTamanioColumnasPDF(), getNombreReporte(),
                    getNombresColumnas(), fuenteCabecera);
            ReportePFD.creaContenidoTabla(getListaDatos(), getNombresColumnas(), Constantes.getFuenteContenidoDefault(10));
            ReportePFD.getFinalParagraph(loginBean.getUsuario().getUsername());
            ReportePFD.descargarPDF(getNombreReporte());
            procesoBean.okActivityRegister("DESCARGA REPORTE(PDF) " + getNombreReporte(),
                    "NÚMERO DE REGISTROS: " + registros.size());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(ProcesoController.class).error("Error al exportar auditoría", e);
        }
    }

    public FiltroActividadAuditoriaDTO getFiltro() { return filtro; }
    public LazyDataModel<ActividadAuditoriaDTO> getAuditoriasLazy() { return auditoriasLazy; }
    public List<String> getUsuariosAuditoria() { return usuariosAuditoria; }
    public int getPrimerRegistro() { return primerRegistro; }
    public void setPrimerRegistro(int primerRegistro) { this.primerRegistro = primerRegistro; }
}
