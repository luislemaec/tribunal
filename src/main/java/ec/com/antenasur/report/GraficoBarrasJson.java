package ec.com.antenasur.report;

import java.util.List;
import java.util.function.Function;
import java.util.function.ToLongFunction;

import ec.com.antenasur.util.JsfUtil;

/**
 * Configuración Chart.js (JSON) de barras horizontales para {@code p:chart}.
 *
 * <p>PrimeFaces 15 ya no incluye {@code org.primefaces.model.charts.*}: el
 * componente recibe la configuración como JSON. No lleva colores; los aplica el
 * extender {@code tecGraficoTemaRegistros} con las variables del tema activo.
 * Barras horizontales: legibles con nombres largos y con muchas categorías.</p>
 */
public final class GraficoBarrasJson {

    public static final String VACIO = "{}";

    private GraficoBarrasJson() {
    }

    /** Una serie: clave de mensaje para la leyenda y valor por fila. */
    public static <T> String serie(String claveEtiqueta, List<T> filas, ToLongFunction<T> valor) {
        StringBuilder datos = new StringBuilder();
        for (T fila : filas) {
            if (datos.length() > 0) {
                datos.append(',');
            }
            datos.append(valor.applyAsLong(fila));
        }
        return "{\"label\":\"" + escapar(JsfUtil.getMessage(claveEtiqueta)) + "\",\"data\":[" + datos
                + "],\"borderRadius\":4}";
    }

    /** Con más de una serie se apilan y se muestra la leyenda. */
    public static <T> String barras(List<T> filas, Function<T, String> etiqueta, String... series) {
        if (filas == null || filas.isEmpty()) {
            return VACIO;
        }
        boolean apilado = series.length > 1;
        StringBuilder etiquetas = new StringBuilder();
        for (T fila : filas) {
            if (etiquetas.length() > 0) {
                etiquetas.append(',');
            }
            etiquetas.append('"').append(escapar(etiqueta.apply(fila))).append('"');
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

    /** Serie con valores ya calculados (por ejemplo, porcentajes de pocos grupos). */
    public static String serieValores(String claveEtiqueta, long... valores) {
        StringBuilder datos = new StringBuilder();
        for (long valor : valores) {
            if (datos.length() > 0) {
                datos.append(',');
            }
            datos.append(valor);
        }
        return "{\"label\":\"" + escapar(JsfUtil.getMessage(claveEtiqueta)) + "\",\"data\":[" + datos
                + "],\"borderRadius\":4}";
    }

    /** Barras agrupadas (no apiladas) en escala 0-100 para comparar porcentajes entre grupos. */
    public static String porcentajesAgrupados(List<String> etiquetas, String... series) {
        StringBuilder labels = new StringBuilder();
        for (String etiqueta : etiquetas) {
            if (labels.length() > 0) {
                labels.append(',');
            }
            labels.append('"').append(escapar(etiqueta)).append('"');
        }
        return "{\"type\":\"bar\",\"data\":{\"labels\":[" + labels + "],\"datasets\":["
                + String.join(",", series) + "]},"
                + "\"options\":{\"indexAxis\":\"y\",\"maintainAspectRatio\":false,"
                + "\"plugins\":{\"legend\":{\"display\":true}},"
                + "\"scales\":{\"x\":{\"beginAtZero\":true,\"max\":100,\"ticks\":{\"precision\":0}}}}}";
    }

    /** Barras verticales de una serie temporal: el tiempo en el eje x, como se lee habitualmente. */
    public static String serieTemporal(List<String> etiquetas, String serie) {
        StringBuilder labels = new StringBuilder();
        for (String etiqueta : etiquetas) {
            if (labels.length() > 0) {
                labels.append(',');
            }
            labels.append('"').append(escapar(etiqueta)).append('"');
        }
        return "{\"type\":\"bar\",\"data\":{\"labels\":[" + labels + "],\"datasets\":[" + serie + "]},"
                + "\"options\":{\"maintainAspectRatio\":false,\"plugins\":{\"legend\":{\"display\":false}},"
                + "\"scales\":{\"x\":{\"ticks\":{\"autoSkip\":true,\"maxRotation\":0}},"
                + "\"y\":{\"beginAtZero\":true,\"ticks\":{\"precision\":0}}}}}";
    }

    /** Alto del gráfico según el número de barras, para que ninguna etiqueta se comprima. */
    public static String alto(int barras) {
        return Math.max(10, 3 + barras * 2) + "rem";
    }

    private static String escapar(String valor) {
        return valor == null ? "" : valor.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
