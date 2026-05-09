package ec.com.antenasur.report;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.primefaces.event.SelectEvent;

import lombok.Getter;
import lombok.Setter;

public class ReportTemplateController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(ReportTemplateController.class);

    @Setter
    @Getter
    private String nombreReporte;

    @Setter
    @Getter
    private float[] tamanioColumnasPDF;

    @Setter
    @Getter
    private int[] tamanioColumnasXLS;

    @Setter
    @Getter
    private String[] nombresColumnas;

    @Setter
    @Getter
    private int numeroColumnas;

    @Setter
    @Getter
    private int tamanioLetra;

    @Getter
    private final LocalDate fechaActual = LocalDate.now();

    @Getter
    private final LocalDate maxfecha = LocalDate.now();

    @Setter
    @Getter
    private LocalDate fechaInicio, fechaFin, minfecha;

    @Setter
    @Getter
    private String[][] listaDatos;

    @Setter
    @Getter
    private String classification, scoped, type, geograp;

    public ReportTemplateController(String nombreReporte, float[] tamanioColumnasPDF, int[] tamanioColumnasXLS, String[] nombresColumnas, int tamanioLetra) {
        this.nombreReporte = nombreReporte;
        this.tamanioColumnasPDF = tamanioColumnasPDF;
        this.tamanioColumnasXLS = tamanioColumnasXLS;
        this.nombresColumnas = nombresColumnas;
        this.numeroColumnas = this.nombresColumnas.length;
        this.tamanioLetra = tamanioLetra;
    }

    public ReportTemplateController(String nombreReporte, float[] tamanioColumnasPDF, int[] tamanioColumnasXLS, String[] nombresColumnas) {
        this.nombreReporte = nombreReporte;
        this.tamanioColumnasPDF = tamanioColumnasPDF;
        this.tamanioColumnasXLS = tamanioColumnasXLS;
        this.nombresColumnas = nombresColumnas;
        this.numeroColumnas = this.nombresColumnas.length;
        this.tamanioLetra = 0;
    }

    @PostConstruct
    private void init() {
        try {
            /* listClassification = catalogueService.findByFatherNametoAdmin(Constant.getClasificacion());
            listScope = catalogueService.findByFatherName(Constant.getAlcance());
            listType = catalogueService.findByFatherName(Constant.getTipo());
            classification = scoped = type = "TODOS";
             */
        } catch (Exception e) {
            LOG.error("ERROR AL INICIALIZAR VARIABLES", e);
        }
    }

    public void onDateSelect(SelectEvent<LocalDate> event) {
        minfecha = fechaInicio;
    }

}
