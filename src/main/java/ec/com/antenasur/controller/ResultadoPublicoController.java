package ec.com.antenasur.controller;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ec.com.antenasur.dto.ResultadoCategoriaPublicaDTO;
import ec.com.antenasur.dto.ResultadoMesaPublicaDTO;
import ec.com.antenasur.dto.ResultadoPublicoSnapshotDTO;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.service.tec.ResultadosPublicosCacheService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Named
@ViewScoped
@Slf4j
public class ResultadoPublicoController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ResultadosPublicosCacheService resultadosPublicosCacheService;

    @Getter
    private ProcesoElectoral procesoActivo;

    @Getter
    private List<ResultadoCategoriaPublicaDTO> resultados = new ArrayList<>();

    @Getter
    private List<ResultadoMesaPublicaDTO> mesasCerradas = new ArrayList<>();

    @Getter
    @Setter
    private String cantonFiltro;

    @Getter
    @Setter
    private String parroquiaFiltro;

    @Getter
    @Setter
    private String recintoFiltro;

    @Getter
    private long totalMesasProceso;

    @Getter
    private long totalMesasCerradas;

    @Getter
    private long totalVotosRegistrados;

    @Getter
    private String resultadosChartModel = "{}";

    @Getter
    private Date ultimaActualizacion;

    @PostConstruct
    public void init() {
        actualizar();
    }

    public void actualizar() {
        try {
            ResultadoPublicoSnapshotDTO snapshot = resultadosPublicosCacheService.obtenerSnapshot();
            procesoActivo = snapshot.getProcesoActivo();
            resultados = snapshot.getResultados();
            mesasCerradas = snapshot.getMesasCerradas();
            totalMesasProceso = snapshot.getTotalMesasProceso();
            totalMesasCerradas = snapshot.getTotalMesasCerradas();
            totalVotosRegistrados = snapshot.getTotalVotosRegistrados();
            resultadosChartModel = snapshot.getResultadosChartModel();
            ultimaActualizacion = snapshot.getUltimaActualizacion();
        } catch (Exception e) {
            log.error("ERROR AL ACTUALIZAR RESULTADOS PUBLICOS", e);
        }
    }

    public boolean isHayProcesoActivo() {
        return procesoActivo != null && procesoActivo.getId() != null;
    }

    public long getTotalMesasPendientes() {
        long pendientes = totalMesasProceso - totalMesasCerradas;
        return Math.max(pendientes, 0L);
    }

    public BigDecimal getPorcentajeMesasCerradas() {
        ResultadoPublicoSnapshotDTO snapshot = resultadosPublicosCacheService.obtenerSnapshot();
        return snapshot.getPorcentajeMesasCerradas() != null ? snapshot.getPorcentajeMesasCerradas() : BigDecimal.ZERO;
    }

    public int getPorcentajeMesasCerradasEntero() {
        ResultadoPublicoSnapshotDTO snapshot = resultadosPublicosCacheService.obtenerSnapshot();
        return snapshot.getPorcentajeMesasCerradasEntero();
    }

    public void limpiarFiltrosMesas() {
        cantonFiltro = null;
        parroquiaFiltro = null;
        recintoFiltro = null;
    }

    public void onCantonFiltroChange() {
        parroquiaFiltro = null;
        recintoFiltro = null;
    }

    public void onParroquiaFiltroChange() {
        recintoFiltro = null;
    }

    public List<String> getCantonesFiltro() {
        Set<String> valores = new LinkedHashSet<>();
        for (ResultadoMesaPublicaDTO mesa : mesasCerradas) {
            agregarValorFiltro(valores, mesa.getCanton());
        }
        return new ArrayList<>(valores);
    }

    public List<String> getParroquiasFiltro() {
        Set<String> valores = new LinkedHashSet<>();
        for (ResultadoMesaPublicaDTO mesa : mesasCerradas) {
            if (coincideFiltro(cantonFiltro, mesa.getCanton())) {
                agregarValorFiltro(valores, mesa.getParroquia());
            }
        }
        return new ArrayList<>(valores);
    }

    public List<String> getRecintosFiltro() {
        Set<String> valores = new LinkedHashSet<>();
        for (ResultadoMesaPublicaDTO mesa : mesasCerradas) {
            if (coincideFiltro(cantonFiltro, mesa.getCanton())
                    && coincideFiltro(parroquiaFiltro, mesa.getParroquia())) {
                agregarValorFiltro(valores, mesa.getRecinto());
            }
        }
        return new ArrayList<>(valores);
    }

    public List<ResultadoMesaPublicaDTO> getMesasCerradasFiltradas() {
        List<ResultadoMesaPublicaDTO> filtradas = new ArrayList<>();
        for (ResultadoMesaPublicaDTO mesa : mesasCerradas) {
            if (coincideFiltro(cantonFiltro, mesa.getCanton())
                    && coincideFiltro(parroquiaFiltro, mesa.getParroquia())
                    && coincideFiltro(recintoFiltro, mesa.getRecinto())) {
                filtradas.add(mesa);
            }
        }
        return filtradas;
    }

    private void agregarValorFiltro(Set<String> valores, String valor) {
        if (valor != null && !valor.isBlank()) {
            valores.add(valor);
        }
    }

    private boolean coincideFiltro(String filtro, String valor) {
        return filtro == null || filtro.isBlank() || filtro.equals(valor);
    }

}
