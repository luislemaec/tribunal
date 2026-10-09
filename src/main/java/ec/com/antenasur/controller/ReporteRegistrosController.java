package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.ToLongFunction;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import ec.com.antenasur.dto.ConteoRegistrosDTO;
import ec.com.antenasur.service.tec.ReporteRegistrosService;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Rep. Registros: personas habilitadas por cantón o, con un cantón
 * seleccionado, por parroquia; más habilitados vs. no habilitados y revisados
 * vs. pendientes de la misma agrupación.
 *
 * <p>Los gráficos se entregan como JSON de Chart.js (PrimeFaces 15 ya no incluye
 * {@code org.primefaces.model.charts.*}), sin colores: los aplica el extender
 * {@code tecGraficoTemaRegistros} con las variables del tema.</p>
 */
@Named
@ViewScoped
@Slf4j
public class ReporteRegistrosController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ReporteRegistrosService reporteRegistrosService;

    /** Cantón del filtro; null = Todos los cantones. */
    @Getter
    @Setter
    private Integer cantonId;

    /** Opciones del filtro: cantones con al menos una persona habilitada. */
    @Getter
    private List<ConteoRegistrosDTO> cantones = Collections.emptyList();

    /** Filas de la agrupación vigente (cantones o parroquias). */
    @Getter
    private List<ConteoRegistrosDTO> categorias = Collections.emptyList();

    @Getter
    private ConteoRegistrosDTO totales = new ConteoRegistrosDTO(null, null, null, 0L, 0L, 0L);

    @Getter
    private String graficoHabilitados = "{}";

    @Getter
    private String graficoHabilitacion = "{}";

    @Getter
    private String graficoRevision = "{}";

    @PostConstruct
    public void init() {
        cargar();
    }

    /** Cambio del filtro Cantón (AJAX). */
    public void cambiarCanton() {
        cargar();
    }

    private void cargar() {
        try {
            List<ConteoRegistrosDTO> porCanton = reporteRegistrosService.porCanton();
            cantones = porCanton.stream().filter(c -> c.getHabilitados() > 0).toList();
            if (cantonId != null && cantones.stream().noneMatch(c -> cantonId.equals(c.getId()))) {
                cantonId = null;
            }
            categorias = cantonId == null ? porCanton : reporteRegistrosService.porParroquia(cantonId);
            totales = reporteRegistrosService.totales(cantonId);
        } catch (Exception e) {
            log.error("Error al cargar Rep. Registros cantonId={}", cantonId, e);
            categorias = Collections.emptyList();
            totales = new ConteoRegistrosDTO(null, null, null, 0L, 0L, 0L);
            JsfUtil.addErrorMessage(JsfUtil.getMessage("reporteRegistros.error.carga"));
        }
        construirGraficos();
    }

    private void construirGraficos() {
        List<ConteoRegistrosDTO> conHabilitados = getCategoriasConHabilitados();
        graficoHabilitados = conHabilitados.isEmpty() ? "{}"
                : grafico(conHabilitados, false, serie("reporteRegistros.serie.habilitados",
                        conHabilitados, ConteoRegistrosDTO::getHabilitados));
        graficoHabilitacion = categorias.isEmpty() ? "{}"
                : grafico(categorias, true,
                        serie("reporteRegistros.serie.habilitados", categorias, ConteoRegistrosDTO::getHabilitados),
                        serie("reporteRegistros.serie.noHabilitados", categorias, ConteoRegistrosDTO::getNoHabilitados));
        graficoRevision = categorias.isEmpty() ? "{}"
                : grafico(categorias, true,
                        serie("reporteRegistros.serie.revisados", categorias, ConteoRegistrosDTO::getRevisados),
                        serie("reporteRegistros.serie.pendientes", categorias, ConteoRegistrosDTO::getPendientesRevision));
    }

    /** Barras horizontales: legibles con nombres largos y con muchas categorías. */
    private static String grafico(List<ConteoRegistrosDTO> filas, boolean apilado, String... series) {
        StringBuilder etiquetas = new StringBuilder();
        for (ConteoRegistrosDTO fila : filas) {
            if (etiquetas.length() > 0) {
                etiquetas.append(',');
            }
            etiquetas.append('"').append(escaparJson(fila.getEtiqueta())).append('"');
        }
        String ejes = apilado
                ? "\"x\":{\"stacked\":true,\"beginAtZero\":true,\"ticks\":{\"precision\":0}},\"y\":{\"stacked\":true}"
                : "\"x\":{\"beginAtZero\":true,\"ticks\":{\"precision\":0}}";
        return "{\"type\":\"bar\",\"data\":{\"labels\":[" + etiquetas + "],\"datasets\":["
                + String.join(",", series) + "]},"
                + "\"options\":{\"indexAxis\":\"y\",\"maintainAspectRatio\":false,"
                + "\"plugins\":{\"legend\":{\"display\":" + apilado + "}},"
                + "\"scales\":{" + ejes + "}}}";
    }

    private static String serie(String claveEtiqueta, List<ConteoRegistrosDTO> filas,
            ToLongFunction<ConteoRegistrosDTO> valor) {
        StringBuilder datos = new StringBuilder();
        for (ConteoRegistrosDTO fila : filas) {
            if (datos.length() > 0) {
                datos.append(',');
            }
            datos.append(valor.applyAsLong(fila));
        }
        return "{\"label\":\"" + escaparJson(JsfUtil.getMessage(claveEtiqueta)) + "\",\"data\":[" + datos
                + "],\"borderRadius\":4}";
    }

    private static String escaparJson(String valor) {
        return valor == null ? "" : valor.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public List<ConteoRegistrosDTO> getCategoriasConHabilitados() {
        return categorias.stream().filter(c -> c.getHabilitados() > 0).toList();
    }

    public boolean isTodosLosCantones() {
        return cantonId == null;
    }

    public String getNombreCantonSeleccionado() {
        return cantones.stream().filter(c -> c.getId().equals(cantonId)).findFirst()
                .map(ConteoRegistrosDTO::getEtiqueta).orElse("");
    }

    /** Alto del gráfico según el número de barras, para que ninguna etiqueta se comprima. */
    public String altoGrafico(int barras) {
        return Math.max(10, 3 + barras * 2) + "rem";
    }
}
