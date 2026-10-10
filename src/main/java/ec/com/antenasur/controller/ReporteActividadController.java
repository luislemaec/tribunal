package ec.com.antenasur.controller;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import ec.com.antenasur.dto.ActividadAuditoriaDTO;
import ec.com.antenasur.dto.UsuarioEnLineaDTO;
import ec.com.antenasur.report.GraficoBarrasJson;
import ec.com.antenasur.service.tec.ReporteActividadService;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Pestaña Actividad de Rep. Registros: transacciones de la bitácora por día y por
 * módulo, últimos eventos en línea de tiempo y usuarios en línea. Se crea solo al
 * abrir la pestaña (tabView dinámico) y no consulta al crearse: ver {@link #cargar()}.
 */
@Named
@ViewScoped
@Slf4j
public class ReporteActividadController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int EVENTOS_RECIENTES = 20;

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");

    @Getter
    private final List<Integer> opcionesPeriodo = List.of(7, 30, 90);

    @Inject
    private ReporteActividadService reporteActividadService;

    @Getter
    @Setter
    private Integer dias = 30;

    @Getter
    private long totalPeriodo;

    @Getter
    private long modulosConActividad;

    @Getter
    private String graficoPorDia = GraficoBarrasJson.VACIO;

    @Getter
    private List<Map.Entry<String, Long>> porModulo = Collections.emptyList();

    @Getter
    private String graficoPorModulo = GraficoBarrasJson.VACIO;

    @Getter
    private List<ActividadAuditoriaDTO> eventos = Collections.emptyList();

    @Getter
    private List<UsuarioEnLineaDTO> usuariosEnLinea = Collections.emptyList();

    /** false hasta la primera carga: la vista muestra marcadores mientras llegan los datos. */
    @Getter
    private boolean cargado;

    /**
     * Primera carga, en segundo plano: la pestaña se dibuja al instante y un
     * p:remoteCommand (autoRun) pide los datos justo después.
     */
    public void cargar() {
        cargarResumen();
        cargarEnLinea();
        cargado = true;
    }

    /** Cambio de periodo (AJAX): solo el resumen de transacciones. */
    public void cambiarPeriodo() {
        if (!cargado) {
            cargar();
            return;
        }
        cargarResumen();
    }

    /** Botón Actualizar de usuarios en línea y últimos eventos. */
    public void actualizarEnLinea() {
        if (!cargado) {
            cargar();
            return;
        }
        cargarEnLinea();
    }

    private void cargarResumen() {
        if (dias == null || !opcionesPeriodo.contains(dias)) {
            dias = 30;
        }
        try {
            Map<LocalDate, Long> porDia = reporteActividadService.transaccionesPorDia(dias);
            totalPeriodo = porDia.values().stream().mapToLong(Long::longValue).sum();
            List<String> etiquetas = porDia.keySet().stream().map(DIA::format).toList();
            graficoPorDia = totalPeriodo == 0 ? GraficoBarrasJson.VACIO
                    : GraficoBarrasJson.serieTemporal(etiquetas, GraficoBarrasJson.serieValores(
                            "reporteActividad.serie.transacciones",
                            porDia.values().stream().mapToLong(Long::longValue).toArray()));
            // SimpleEntry es serializable (el bean es @ViewScoped); las entradas del mapa no.
            porModulo = new ArrayList<>();
            reporteActividadService.transaccionesPorModulo(dias)
                    .forEach((modulo, total) -> porModulo.add(new AbstractMap.SimpleEntry<>(modulo, total)));
            modulosConActividad = porModulo.size();
            graficoPorModulo = GraficoBarrasJson.barras(porModulo, Map.Entry::getKey,
                    GraficoBarrasJson.serie("reporteActividad.serie.transacciones", porModulo, Map.Entry::getValue));
        } catch (Exception e) {
            log.error("Error al cargar el resumen de transacciones dias={}", dias, e);
            totalPeriodo = 0;
            porModulo = Collections.emptyList();
            graficoPorDia = GraficoBarrasJson.VACIO;
            graficoPorModulo = GraficoBarrasJson.VACIO;
            JsfUtil.addErrorMessage(JsfUtil.getMessage("reporteRegistros.error.carga"));
        }
    }

    private void cargarEnLinea() {
        try {
            usuariosEnLinea = reporteActividadService.usuariosEnLinea();
            eventos = reporteActividadService.ultimosEventos(EVENTOS_RECIENTES);
        } catch (Exception e) {
            log.error("Error al cargar usuarios en linea y ultimos eventos", e);
            usuariosEnLinea = Collections.emptyList();
            eventos = Collections.emptyList();
            JsfUtil.addErrorMessage(JsfUtil.getMessage("reporteRegistros.error.carga"));
        }
    }

    public long getUsuariosWeb() {
        return usuariosEnLinea.stream().filter(u -> u.getCanales().contains(UsuarioEnLineaDTO.WEB)).count();
    }

    public long getUsuariosApp() {
        return usuariosEnLinea.stream().filter(u -> u.getCanales().contains(UsuarioEnLineaDTO.APP)).count();
    }

    public String altoGrafico(int barras) {
        return GraficoBarrasJson.alto(barras);
    }
}
