package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import ec.com.antenasur.dto.AdministradorIglesiaDTO;
import ec.com.antenasur.dto.ConteoAdministradoresDTO;
import ec.com.antenasur.report.GraficoBarrasJson;
import ec.com.antenasur.service.tec.ReporteAdministradoresService;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Pestaña Administradores de Rep. Registros. Se crea solo al abrir la pestaña
 * (tabView dinámico). Consulta una vez al abrirla; el cantón y los días de
 * inactividad se recalculan sobre los datos ya cargados.
 */
@Named
@ViewScoped
@Slf4j
public class ReporteAdministradoresController implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Opciones del filtro de inactividad, en días. */
    @Getter
    private final List<Integer> opcionesInactividad = List.of(7, 15, 30, 60);

    @Inject
    private ReporteAdministradoresService reporteAdministradoresService;

    /** Cantón del filtro; null = Todos los cantones. */
    @Getter
    @Setter
    private Integer cantonId;

    @Getter
    @Setter
    private Integer diasInactividad = 30;

    /** Todas las iglesias activas, cargadas una vez. */
    private List<AdministradorIglesiaDTO> todas = Collections.emptyList();

    @Getter
    private List<ConteoAdministradoresDTO> cantones = Collections.emptyList();

    @Getter
    private List<ConteoAdministradoresDTO> categorias = Collections.emptyList();

    @Getter
    private ConteoAdministradoresDTO totales = new ConteoAdministradoresDTO(null, null, null);

    /** Iglesias del filtro en orden de atención. */
    @Getter
    private List<AdministradorIglesiaDTO> iglesias = Collections.emptyList();

    @Getter
    private String graficoAdministrador = GraficoBarrasJson.VACIO;

    @Getter
    private String graficoActivacion = GraficoBarrasJson.VACIO;

    @Getter
    private String graficoAvance = GraficoBarrasJson.VACIO;

    @PostConstruct
    public void init() {
        try {
            todas = reporteAdministradoresService.iglesias();
        } catch (Exception e) {
            log.error("Error al cargar el reporte de administradores", e);
            todas = Collections.emptyList();
            JsfUtil.addErrorMessage(JsfUtil.getMessage("reporteRegistros.error.carga"));
        }
        recalcular();
    }

    /** Cambio de cantón o de días de inactividad (AJAX): sin nuevas consultas. */
    public void cambiarFiltro() {
        recalcular();
    }

    private void recalcular() {
        if (diasInactividad == null || !opcionesInactividad.contains(diasInactividad)) {
            diasInactividad = 30;
        }
        reporteAdministradoresService.marcarInactividad(todas, diasInactividad);
        cantones = reporteAdministradoresService.porCanton(todas);
        if (cantonId != null && cantones.stream().noneMatch(c -> cantonId.equals(c.getId()))) {
            cantonId = null;
        }
        List<AdministradorIglesiaDTO> filtradas = cantonId == null ? todas
                : todas.stream().filter(i -> cantonId.equals(i.getCantonId())).toList();
        categorias = cantonId == null ? cantones : reporteAdministradoresService.porParroquia(filtradas);
        totales = reporteAdministradoresService.totales(filtradas);
        iglesias = reporteAdministradoresService.ordenarParaAtencion(filtradas);
        construirGraficos(filtradas);
    }

    private void construirGraficos(List<AdministradorIglesiaDTO> filtradas) {
        graficoAdministrador = GraficoBarrasJson.barras(categorias, ConteoAdministradoresDTO::getEtiqueta,
                GraficoBarrasJson.serie("reporteIglesias.serie.conAdministrador", categorias,
                        ConteoAdministradoresDTO::getConAdministrador),
                GraficoBarrasJson.serie("reporteIglesias.serie.sinAdministrador", categorias,
                        ConteoAdministradoresDTO::getSinAdministrador));
        List<ConteoAdministradoresDTO> conAdmin = getCategoriasConAdministrador();
        graficoActivacion = GraficoBarrasJson.barras(conAdmin, ConteoAdministradoresDTO::getEtiqueta,
                GraficoBarrasJson.serie("reporteAdministradores.serie.activados", conAdmin,
                        ConteoAdministradoresDTO::getActivados),
                GraficoBarrasJson.serie("reporteAdministradores.serie.pendientes", conAdmin,
                        ConteoAdministradoresDTO::getPendientesActivar));
        if (filtradas.isEmpty()) {
            graficoAvance = GraficoBarrasJson.VACIO;
            return;
        }
        int[][] avance = reporteAdministradoresService.avanceConYSinAdministrador(filtradas);
        graficoAvance = GraficoBarrasJson.porcentajesAgrupados(
                List.of(JsfUtil.getMessage("reporteIglesias.serie.conAdministrador"),
                        JsfUtil.getMessage("reporteIglesias.serie.sinAdministrador")),
                GraficoBarrasJson.serieValores("reporteAdministradores.serie.porcentajeHabilitados",
                        avance[0][0], avance[1][0]),
                GraficoBarrasJson.serieValores("reporteAdministradores.serie.porcentajeRevisados",
                        avance[0][1], avance[1][1]));
    }

    public List<ConteoAdministradoresDTO> getCategoriasConAdministrador() {
        return categorias.stream().filter(c -> c.getConAdministrador() > 0).toList();
    }

    public boolean isTodosLosCantones() {
        return cantonId == null;
    }

    public String getNombreCantonSeleccionado() {
        return cantones.stream().filter(c -> c.getId().equals(cantonId)).findFirst()
                .map(ConteoAdministradoresDTO::getEtiqueta).orElse("");
    }

    public String altoGrafico(int barras) {
        return GraficoBarrasJson.alto(barras);
    }
}
