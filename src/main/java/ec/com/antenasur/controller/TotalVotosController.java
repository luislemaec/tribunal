package ec.com.antenasur.controller;

import ec.com.antenasur.bean.GeograpBean;
import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.bean.MesaBean;
import ec.com.antenasur.bean.RecintoBean;
import ec.com.antenasur.domain.Geograp;
import ec.com.antenasur.domain.tec.Mesa;
import ec.com.antenasur.domain.tec.Recinto;
import ec.com.antenasur.domain.tec.VwTotalVotos;
import ec.com.antenasur.service.tec.VwTotalVotosFacade;
import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.axes.cartesian.CartesianScales;
import org.primefaces.model.charts.axes.cartesian.linear.CartesianLinearAxes;
import org.primefaces.model.charts.axes.cartesian.linear.CartesianLinearTicks;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.bar.BarChartOptions;
import org.primefaces.model.charts.donut.DonutChartModel;
import org.primefaces.model.charts.hbar.HorizontalBarChartModel;
import org.primefaces.model.charts.optionconfig.animation.Animation;
import org.primefaces.model.charts.optionconfig.legend.Legend;
import org.primefaces.model.charts.optionconfig.legend.LegendLabel;
import org.primefaces.model.charts.optionconfig.title.Title;

import org.primefaces.model.charts.hbar.HorizontalBarChartDataSet;

import org.primefaces.model.charts.donut.DonutChartDataSet;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Named
@ViewScoped
@Slf4j
public class TotalVotosController implements Serializable {

    private static final String DESTINATION = System.getProperty("java.io.tmpdir");

    private static final long serialVersionUID = 1L;

    private static final String FORMULARIO = "frmMesas";
    private static final String TABLA = "tblMesas";
    private static final String MENSAJE_REGISTRA_OK = "Mesa registrado";
    private static final String MENSAJE_ACTUALIZA_OK = "Mesa actualizado";
    private static final String MENSAJE_ELIMINA_OK = "Mesa eliminado";
    public static final String MENSAJE_CONFORMACION_ELIMINAR = "¿Esta seguro de eliminar?";

    @Inject
    private LoginBean loginBean;

    @Inject
    VwTotalVotosFacade vwTotalVotosFacade;

    @Inject
    private GeograpBean geograpBean;

    @Inject
    private MesaBean mesaBean;

    @Inject
    private RecintoBean recintoBean;

    @Setter
    @Getter
    private List<VwTotalVotos> totalVotos;

    @Setter
    @Getter
    private List<Object[]> votos;

    @Getter
    private List<Object[]> votosPorCanton, votosPorParroquia, votosPorRecinto;

    @Setter
    @Getter
    private List<Geograp> cantones, parroquias;

    @Setter
    @Getter
    private Geograp cantonSeleccionado, parroquiaSeleccionado;

    @Setter
    @Getter
    private Recinto recintoSeleccionado;

    @Setter
    @Getter
    private List<Recinto> recintos;

    @Setter
    @Getter
    private List<Mesa> mesas, mesasEscrutadas;

    @Setter
    @Getter
    private Mesa mesaSeleccionado;

    @Setter
    @Getter
    private BarChartModel barModel;

    @Setter
    @Getter
    private DonutChartModel donutModel;

    @Setter
    @Getter
    private HorizontalBarChartModel hbarModel;

    @Setter
    @Getter
    private float porcentajeMesasEscrutadas;

    @Setter
    @Getter
    private int totalVotantes;

    @PostConstruct
    private void init() {
        try {
            totalVotos = new ArrayList();
            cantonSeleccionado = new Geograp();
            parroquiaSeleccionado = new Geograp();
            recintoSeleccionado = new Recinto();
            mesaSeleccionado = new Mesa();
            parroquias = new ArrayList();
            this.cantones = geograpBean.getByFatherId(7);

            this.cargaParroquiaInicial();

        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    private void cargaParroquiaInicial() {
        try {
            if (cantones != null) {
                for (Geograp canton : cantones) {
                    List<Geograp> parroquiasTmp = new ArrayList();
                    parroquiasTmp = geograpBean.getByFatherGeograp(canton);
                    for (Geograp parroquia : parroquiasTmp) {
                        parroquias.add(parroquia);
                    }
                }
                this.cargaRecintosPorParroquia(parroquias);
                this.cargaMesasPorRecintos(recintos);
                this.cargaVotosPorMesas(mesas);
                getReporteEstadistica();
            }
        } catch (Exception e) {
            log.error("ERROR EN CARGAR PARROQUIAS INICIAL", e);
        }

    }

    /**
     * Metodo para llamar desde formulario
     */
    public void cargaParroquiasPorCanton() {
        try {
            if (cantonSeleccionado.getId() != null) {
                this.cantonSeleccionado = geograpBean.getById(this.cantonSeleccionado.getId());
                this.parroquias = geograpBean.getByFatherGeograp(this.cantonSeleccionado);
                this.cargaRecintosPorParroquia(parroquias);
                this.cargaMesasPorRecintos(recintos);
                this.cargaVotosPorMesas(mesas);
            }
        } catch (Exception e) {
            log.error("ERROR EN CARGAR PARROQUIAS POR CANTON", e);
        }
    }

    /**
     * Metodo para llamar desde formulario
     */
    public void cargaRecintosPorParroquia() {
        if (parroquiaSeleccionado != null) {
            this.parroquiaSeleccionado = geograpBean.getById(this.parroquiaSeleccionado.getId());
            List<Geograp> parroquiasTmp = new ArrayList();
            parroquiasTmp.add(parroquiaSeleccionado);
            this.cargaRecintosPorParroquia(parroquiasTmp);
            this.cargaMesasPorRecintos(recintos);
            this.cargaVotosPorMesas(mesas);
        }
    }

    /**
     * Metodo para llamar desde formulario
     */
    public void cargaMesasPorRecintos() {
        if (recintoSeleccionado != null) {
            this.recintoSeleccionado = recintoBean.recintosPorId(this.recintoSeleccionado.getId());
            List<Recinto> recintoTmp = new ArrayList();
            recintoTmp.add(recintoSeleccionado);
            this.cargaMesasPorRecintos(recintoTmp);
            this.cargaVotosPorMesas(mesas);
        }
    }

    /**
     * Metodo para llamar desde formulario
     */
    public List<Mesa> cargaMesasPorRecinto(Recinto recinto) {
        try {
            if (recinto != null) {
                return mesaBean.mesasPorRecinto(recinto);
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }

    }

    /**
     * Metodo para llamar desde formulario
     */
    public void cargaVotosPorMesas() {
        if (mesaSeleccionado != null) {
            this.mesaSeleccionado = mesaBean.mesaPorId(this.mesaSeleccionado.getId());
            List<Mesa> mesasTmp = new ArrayList();
            mesasTmp.add(mesaSeleccionado);
            this.cargaVotosPorMesas(mesasTmp);
        }
    }

    private void cargaRecintosPorParroquia(List<Geograp> parroquiasTmp) {
        try {
            if (recintos != null && !recintos.isEmpty()) {
                recintos.clear();
            }
            this.recintos = recintoBean.recintosPorParroquias(parroquiasTmp);
        } catch (Exception e) {
            log.error("ERROR EN CARGAR RECINTOS POR PARROQUIAS", e);
        }

    }

    public void cargaMesasPorRecintos(List<Recinto> recintosTmp) {
        try {
            if (mesas != null && !mesas.isEmpty()) {
                mesas.clear();
            }
            if (mesasEscrutadas != null && !mesasEscrutadas.isEmpty()) {
                mesasEscrutadas.clear();
            }
            this.mesas = mesaBean.mesasPorRecintos(recintosTmp);
            this.mesasEscrutadas = mesaBean.mesasEscrutadasPorRecintos(recintosTmp);

            if (mesas != null) {
                totalVotantes = 0;
                for (Mesa mesaTmp : mesas) {
                    totalVotantes = totalVotantes + mesaTmp.getTotalVotos();
                }
            }

            if (mesasEscrutadas != null && mesas != null) {
                porcentajeMesasEscrutadas = (mesasEscrutadas.size() * 100) / mesas.size();
            } else {
                porcentajeMesasEscrutadas = 0;
            }

        } catch (Exception e) {
            log.error("ERROR EN CARGAR MESAS POR RECINTOS", e);
        }

    }

    public void cargaVotosPorMesas(List<Mesa> mesasTmp) {
        try {
            this.votos = vwTotalVotosFacade.votosPorMesas(mesasTmp);
            getReporteEstadistica();
        } catch (Exception e) {
            log.error("ERROR EN CARGAR VOTOS POR MESAS", e);
        }

    }

    private void getReporteEstadistica() {
        try {
            createBarModel();
            //createDonutModel();
            createHorizontalBarModel();
        } catch (Exception e) {
            log.error("ERROR AL CARGAR DATOS REPORTE ESTADISTICO", e);
        }
    }

    public void createBarModel() {
        barModel = new BarChartModel();
        ChartData data = new ChartData();

        BarChartDataSet barDataSet = new BarChartDataSet();
        barDataSet.setLabel("TOTAL VOTOS");

        List<Number> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (Object[] item : votos) {
            values.add((Number) item[1]);
            labels.add((String) item[0]);
        }
        barDataSet.setData(values);

        List<String> bgColor = new ArrayList<>();
        bgColor.add("rgba(33, 97, 140)");
        bgColor.add("rgba(75, 192, 192)");
        bgColor.add("rgba(54, 162, 235)");
        bgColor.add("rgba(229, 231, 233)");
        bgColor.add("rgba(85, 85, 85)");
        bgColor.add("rgba(153, 102, 255, 0.2)");
        bgColor.add("rgba(201, 203, 207, 0.2)");
        barDataSet.setBackgroundColor(bgColor);

        List<String> borderColor = new ArrayList<>();
        borderColor.add("rgb(33, 97, 140)");
        borderColor.add("rgb(75, 192, 192)");
        borderColor.add("rgb(54, 162, 235)");
        borderColor.add("rgb(229, 231, 233)");
        borderColor.add("rgb(85, 85, 85)");
        borderColor.add("rgb(153, 102, 255)");
        borderColor.add("rgb(201, 203, 207)");
        barDataSet.setBorderColor(borderColor);
        barDataSet.setBorderWidth(1);

        data.addChartDataSet(barDataSet);

        data.setLabels(labels);
        barModel.setData(data);

        //Options
        BarChartOptions options = new BarChartOptions();
        CartesianScales cScales = new CartesianScales();
        CartesianLinearAxes linearAxes = new CartesianLinearAxes();
        linearAxes.setOffset(true);
        CartesianLinearTicks ticks = new CartesianLinearTicks();
        ticks.setBeginAtZero(true);
        linearAxes.setTicks(ticks);
        cScales.addYAxesData(linearAxes);
        options.setScales(cScales);

        Title title = new Title();
        title.setDisplay(true);
        title.setText("VOTOS POR LISTAS");
        options.setTitle(title);

        Legend legend = new Legend();
        legend.setDisplay(true);
        legend.setPosition("top");
        LegendLabel legendLabels = new LegendLabel();
        legendLabels.setFontStyle("bold");
        legendLabels.setFontColor("#2980B9");
        legendLabels.setFontSize(24);
        legend.setLabels(legendLabels);
        options.setLegend(legend);

        // disable animation
        Animation animation = new Animation();
        animation.setDuration(1000);
        options.setAnimation(animation);

        barModel.setOptions(options);
    }

    public void createHorizontalBarModel() {
        hbarModel = new HorizontalBarChartModel();
        ChartData data = new ChartData();

        HorizontalBarChartDataSet hbarDataSet = new HorizontalBarChartDataSet();
        hbarDataSet.setLabel("Total votos");

        List<Number> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (Object[] item : votos) {
            if (item[0] != null) {
                values.add((Number) item[1]);
                labels.add((String) item[0]);
            }
        }
        hbarDataSet.setData(values);

        List<String> bgColor = new ArrayList<>();
        bgColor.add("rgba(33, 97, 140)");
        bgColor.add("rgba(75, 192, 192)");
        bgColor.add("rgba(54, 162, 235)");
        bgColor.add("rgba(229, 231, 233)");
        bgColor.add("rgba(85, 85, 85)");
        bgColor.add("rgba(153, 102, 255)");
        bgColor.add("rgba(201, 203, 207)");
        hbarDataSet.setBackgroundColor(bgColor);

        List<String> borderColor = new ArrayList<>();
        borderColor.add("rgb(33, 97, 140)");
        borderColor.add("rgb(75, 192, 192)");
        borderColor.add("rgb(54, 162, 235)");
        borderColor.add("rgb(229, 231, 233)");
        borderColor.add("rgb(85, 85, 85)");
        borderColor.add("rgb(153, 102, 255)");
        borderColor.add("rgb(201, 203, 207)");
        hbarDataSet.setBorderColor(borderColor);
        hbarDataSet.setBorderWidth(1);

        data.addChartDataSet(hbarDataSet);

        data.setLabels(labels);
        hbarModel.setData(data);

        //Options
        BarChartOptions options = new BarChartOptions();
        CartesianScales cScales = new CartesianScales();
        CartesianLinearAxes linearAxes = new CartesianLinearAxes();
        linearAxes.setOffset(true);
        CartesianLinearTicks ticks = new CartesianLinearTicks();
        ticks.setBeginAtZero(true);
        linearAxes.setTicks(ticks);
        cScales.addXAxesData(linearAxes);
        options.setScales(cScales);

        Title title = new Title();
        title.setDisplay(true);
        title.setText("RESUMEN VOTOS");
        options.setTitle(title);

        hbarModel.setOptions(options);
    }

    public void createDonutModel() {
        donutModel = new DonutChartModel();
        ChartData data = new ChartData();

        List<Number> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (Object[] item : votos) {
            values.add((Number) item[1]);
            labels.add((String) item[0]);
        }

        DonutChartDataSet dataSet = new DonutChartDataSet();

        dataSet.setData(values);

        List<String> bgColors = new ArrayList<>();
        bgColors.add("rgb(33, 97, 140)");
        bgColors.add("rgb(75, 192, 192)");
        bgColors.add("rgb(54, 162, 235)");

        bgColors.add("rgb(229, 231, 233)");
        bgColors.add("rgb(85, 85, 85)");
        dataSet.setBackgroundColor(bgColors);

        data.addChartDataSet(dataSet);

        data.setLabels(labels);

        donutModel.setData(data);
    }

}
