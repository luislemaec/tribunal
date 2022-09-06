package ec.com.antenasur.report;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.WEEKS;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.time.temporal.ChronoUnit.YEARS;

import java.io.Serializable;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.bean.ProcesoBean;
import ec.com.antenasur.domain.Proceso;
import ec.com.antenasur.itext.ReportePFD;
import ec.com.antenasur.itext.ReporteXLSX;
import ec.com.antenasur.service.ProcesoFacade;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;

@Named
@ViewScoped
public class ProcesoController extends ReportTemplateController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(ReportTemplateController.class);

    private static final Integer TAMANIO_LETRA = 0;

    @Inject
    private LoginBean loginBean;

    @Inject
    private ProcesoBean procesoBean;

    @Inject
    ProcesoFacade procesoFacade;

    @Setter
    @Getter
    private List<Proceso> listaProceso, procesos;

    @Setter
    @Getter
    private LocalDate fechaActual, fechaInicio, fechaFin, minfecha, maxfecha;

    public ProcesoController() {
        //Inicializa Datos Padre
        super(
                "ACTIVIDAD INTERNA",
                new float[]{20, 100, 40, 50, 50},
                new int[]{1200, 3000, 4000, 10000, 4000},
                new String[]{"Nro", "ACTIVIDAD", "IP", "USUARIO CREA", "FECHA REGISTRO"},
                TAMANIO_LETRA
        );
    }

    @PostConstruct
    private void init() {
        try {
            fechaFin = fechaInicio = fechaActual = LocalDate.now();
            listaProceso = procesoFacade.getProcesoPorUsuario(Date.valueOf(fechaInicio.plusDays(-1)), Date.valueOf(fechaFin));
            //listaProceso = procesoFacade.findAll();
            procesaLista();            
        } catch (Exception e) {
            LOG.error("ERROR AL CARGAR DATOS REPORTE " + getNombreReporte(), e);
        }
    }

    private void procesaLista() {
        try {
            procesos = new ArrayList<>();
            for (Proceso proceso : listaProceso) {
                proceso.setHoras(proceso.getFechaCrea().toString().substring(10, 16));
                proceso.setDias(calcularDias(proceso.getFechaCrea()));
                procesos.add(proceso);
            }
        } catch (Exception e) {
            LOG.error("ERROR EN PROCESAR DATOS ", e);
        }
    }

    private String calcularDias(java.util.Date fecha) {
        try {
            String tmp = "";            
            LocalDate fechaTmp = fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            long numeroDiasL = DAYS.between(fechaTmp , fechaActual);
            int dias = (int) numeroDiasL;
            if (dias < 1) {
                tmp = "Hoy";
            }
            if (dias >= 1 && dias <= 7) {
                tmp = dias + "d";
            }
            if (dias > 7 && dias <= 30) {
            	long numeroSemanasL = WEEKS.between(fechaTmp , fechaActual);
            	int semanasInt = (int) numeroSemanasL;                
                tmp = semanasInt + "s";
            }
            if (dias > 30 && dias <= 365) {
            	long numeroMesesL = MONTHS.between(fechaTmp , fechaActual);
            	int mesesInt = (int) numeroMesesL;                
                tmp = mesesInt + "m";
            }
            if (dias > 365) {
            	long anionLong = YEARS.between(fechaTmp , fechaActual);
            	int aniosInt = (int) anionLong;                
                tmp = aniosInt + "a";
            }
            return tmp;
        } catch (Exception e) {
            LOG.error("ERROR EN CALCULAR DIAS ", e);
            return null;
        }

    }

    public void searchData() {
        if (getFechaInicio() != null && getFechaFin() != null) {
            listaProceso = procesoFacade.findAll();
            if (listaProceso != null) {
                JsfUtil.addInfoMessage(listaProceso.size() + " Procesos encontrados");
            } else {
                JsfUtil.addWarningMessage("No se obtubieron resultado de búsqueda");
            }
        }
    }

    public void exportaPDF() {
        try {
            getListaStringDatos();
            ReportePFD.nuevoPDF(getNombreReporte());
            ReportePFD.creaTablaCabecera(getNumeroColumnas(), getTamanioColumnasPDF(), getNombreReporte(), getNombresColumnas());

            ReportePFD.creaContenidoTabla(getListaDatos(), getNombresColumnas(), getTamanioLetra());
            ReportePFD.getFinalParagraph(loginBean.getUsuario().getUsername());
            ReportePFD.descargarPDF(getNombreReporte());
            procesoBean.okActivityRegister("DESCARGA REPORTE(PDF) " + getNombreReporte(), "NÚMERO DE REGISTROS: " + listaProceso.size());

        } catch (Exception e) {
            LOG.error("ERROR AL EXPORTAR EXCEL DATOS REPORTE" + getNombreReporte(), e);
        }
    }

    public void exportaXLS() {
        try {
            getListaStringDatos();
            ReporteXLSX.nuevoExcel(getNombreReporte());
            ReporteXLSX.creaCabeceraTabla(getNombresColumnas(), getTamanioColumnasXLS());

            ReporteXLSX.creaContenidoTabla(getListaDatos(), getNombresColumnas());
            ReporteXLSX.setFinalParagraph(listaProceso.size());
            ReporteXLSX.descargarExcel(getNombreReporte());
            procesoBean.okActivityRegister("DESCARGA REPORTE(XLS) " + getNombreReporte(), "NÚMERO DE REGISTROS: " + listaProceso.size());
        } catch (Exception e) {
            LOG.error("ERROR AL EXPORTAR EXCEL " + getNombreReporte(), e);
        }
    }

    private void getListaStringDatos() {
        try {
            if (listaProceso != null) {
                setListaDatos(new String[listaProceso.size()][getNumeroColumnas()]);
                int fila = 0;
                for (Proceso item : listaProceso) {
                    getListaDatos()[fila][0] = String.valueOf(fila + 1);
                    getListaDatos()[fila][1] = item.getActividad();
                    getListaDatos()[fila][2] = item.getIp();
                    getListaDatos()[fila][3] = item.getUsuarioCrea();
                    getListaDatos()[fila][4] = item.getFechaCrea().toString().substring(0, 16);
                    fila++;
                }
            }
        } catch (Exception e) {
            LOG.error("ERROR AL OBTENER LISTA DE DATOS REPORTE " + getNombreReporte(), e);
        }
    }

}
