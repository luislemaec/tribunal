package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import ec.com.antenasur.dto.ActividadAuditoriaDTO;
import ec.com.antenasur.dto.UsuarioEnLineaDTO;
import ec.com.antenasur.service.tec.ReporteActividadService;
import ec.com.antenasur.itext.ReporteRegistrosXLSX;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Pestaña Actividad de Rep. Registros: usuarios en línea y últimos eventos en línea de
 * tiempo. Se crea solo al abrir la pestaña (tabView dinámico) y no consulta al crearse:
 * ver {@link #cargar()}. Las transacciones por día y por módulo se retiraron porque
 * recorrían la bitácora del período y demoraban la carga; no se consultan.
 */
@Named
@ViewScoped
@Slf4j
public class ReporteActividadController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int EVENTOS_RECIENTES = 20;

    @Inject
    private ReporteActividadService reporteActividadService;

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
        cargarEnLinea();
        cargado = true;
    }

    /** Botón Actualizar de usuarios en línea y últimos eventos. */
    public void actualizarEnLinea() {
        cargar();
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

    /**
     * Excel con lo que muestra la pestaña (usuarios en línea y últimos eventos), tal como se
     * cargó: es una foto de ese momento; el botón Actualizar la renueva.
     */
    public void exportarExcel() {
        if (!cargado) {
            cargar();
        }
        ExportacionExcelRegistros.descargar("reporteRegistros.excel.archivo.actividad",
                logo -> ReporteRegistrosXLSX.actividad(ExportacionExcelRegistros.encabezado(
                        JsfUtil.getMessage("reporteRegistros.excel.instantanea"), true),
                        getUsuariosWeb(), getUsuariosApp(), usuariosEnLinea, eventos, logo));
    }

    public long getUsuariosWeb() {
        return usuariosEnLinea.stream().filter(u -> u.getCanales().contains(UsuarioEnLineaDTO.WEB)).count();
    }

    public long getUsuariosApp() {
        return usuariosEnLinea.stream().filter(u -> u.getCanales().contains(UsuarioEnLineaDTO.APP)).count();
    }
}
