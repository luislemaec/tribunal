package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resumen gerencial del proceso electoral activo para el panel de control.
 *
 * <p>Lo construye {@code DashboardResumenService} con consultas agregadas: la
 * vista solo lee estos valores, nunca dispara consultas desde los getters.
 */
@Data
@NoArgsConstructor
public class ResumenDashboardDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Proceso electoral activo; null si no hay ninguno configurado. */
    private String procesoNombre;
    /** Fase del cronograma vigente hoy, si existe. */
    private String faseVigente;

    // ----- Resumen general -----
    private long totalIglesias;
    private long totalRecintos;
    private long totalMesas;
    /** Electores del padrón del proceso (suma de {@link #electoresPorCanton}). */
    private long totalElectores;

    // ----- Avance del proceso -----
    /** Mesas con las cuatro dignidades obligatorias designadas. */
    private long mesasConJuntaCompleta;
    /** Mesas cuyo escrutinio está cerrado. */
    private long mesasCerradas;
    /** Mesas por estado de escrutinio; las que no tienen cabecera no aparecen. */
    private Map<String, Long> mesasPorEstado = new LinkedHashMap<>();

    // ----- Documentos por mesa -----
    private long padronesGenerados;
    private long actasParcialesGeneradas;
    private long certificadosGenerados;
    private long actasFisicasCargadas;

    /** Distribución del padrón por cantón, de mayor a menor. */
    private Map<String, Long> electoresPorCanton = new LinkedHashMap<>();

    /** Situaciones que requieren atención, ya redactadas para la vista. */
    private List<AlertaDashboard> alertas = new ArrayList<>();

    public boolean isSinProceso() {
        return procesoNombre == null || procesoNombre.isBlank();
    }

    public long getMesasSinJunta() {
        return Math.max(0, totalMesas - mesasConJuntaCompleta);
    }

    public long getMesasPendientesCierre() {
        return Math.max(0, totalMesas - mesasCerradas);
    }

    public long getActasFisicasPendientes() {
        return Math.max(0, mesasCerradas - actasFisicasCargadas);
    }

    public int getPorcentajeJuntas() {
        return porcentaje(mesasConJuntaCompleta, totalMesas);
    }

    public int getPorcentajeEscrutinio() {
        return porcentaje(mesasCerradas, totalMesas);
    }

    public int getPorcentajePadrones() {
        return porcentaje(padronesGenerados, totalMesas);
    }

    public int getPorcentajeActasParciales() {
        return porcentaje(actasParcialesGeneradas, totalMesas);
    }

    private static int porcentaje(long parte, long total) {
        return total <= 0 ? 0 : (int) Math.round(parte * 100d / total);
    }

    /** Pendiente o inconsistencia operativa detectada en el proceso activo. */
    @Data
    @NoArgsConstructor
    public static class AlertaDashboard implements Serializable {
        private static final long serialVersionUID = 1L;
        /** Clave del mensaje ya resuelto a texto. */
        private String texto;
        /** Cantidad implicada; 0 cuando la alerta no es numérica. */
        private long cantidad;
        /** {@code warn} o {@code danger}: la vista elige el color del tag. */
        private String severidad;
        /** Página del menú a la que dirige la alerta, sin extensión. */
        private String pagina;

        public AlertaDashboard(String texto, long cantidad, String severidad, String pagina) {
            this.texto = texto;
            this.cantidad = cantidad;
            this.severidad = severidad;
            this.pagina = pagina;
        }
    }
}
