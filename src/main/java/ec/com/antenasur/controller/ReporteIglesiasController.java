package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import ec.com.antenasur.dto.ConteoIglesiasDTO;
import ec.com.antenasur.dto.IglesiaAtencionDTO;
import ec.com.antenasur.report.GraficoBarrasJson;
import ec.com.antenasur.service.tec.ReporteIglesiasService;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Pestaña Iglesias de Rep. Registros. Se crea solo al abrir la pestaña
 * (tabView dinámico), así que sus consultas no se ejecutan antes.
 */
@Named
@ViewScoped
@Slf4j
public class ReporteIglesiasController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ReporteIglesiasService reporteIglesiasService;

    /** Cantón del filtro; null = Todos los cantones. */
    @Getter
    @Setter
    private Integer cantonId;

    /** Opciones del filtro: cantones con al menos una iglesia activa. */
    @Getter
    private List<ConteoIglesiasDTO> cantones = Collections.emptyList();

    @Getter
    private List<ConteoIglesiasDTO> categorias = Collections.emptyList();

    @Getter
    private ConteoIglesiasDTO totales = vacio();

    @Getter
    private List<IglesiaAtencionDTO> iglesiasAtencion = Collections.emptyList();

    @Getter
    private String graficoAdministrador = GraficoBarrasJson.VACIO;

    @Getter
    private String graficoListaMiembros = GraficoBarrasJson.VACIO;

    @PostConstruct
    public void init() {
        cargar();
    }

    public void cambiarCanton() {
        cargar();
    }

    private void cargar() {
        try {
            List<ConteoIglesiasDTO> porCanton = reporteIglesiasService.porCanton();
            cantones = porCanton;
            if (cantonId != null && cantones.stream().noneMatch(c -> cantonId.equals(c.getId()))) {
                cantonId = null;
            }
            categorias = cantonId == null ? porCanton : reporteIglesiasService.porParroquia(cantonId);
            totales = reporteIglesiasService.totales(cantonId);
            iglesiasAtencion = reporteIglesiasService.iglesiasQueRequierenAtencion(cantonId);
        } catch (Exception e) {
            log.error("Error al cargar el reporte de iglesias cantonId={}", cantonId, e);
            categorias = Collections.emptyList();
            totales = vacio();
            iglesiasAtencion = Collections.emptyList();
            JsfUtil.addErrorMessage(JsfUtil.getMessage("reporteRegistros.error.carga"));
        }
        graficoAdministrador = GraficoBarrasJson.barras(categorias, ConteoIglesiasDTO::getEtiqueta,
                GraficoBarrasJson.serie("reporteIglesias.serie.conAdministrador", categorias,
                        ConteoIglesiasDTO::getConAdministrador),
                GraficoBarrasJson.serie("reporteIglesias.serie.sinAdministrador", categorias,
                        ConteoIglesiasDTO::getSinAdministrador));
        graficoListaMiembros = GraficoBarrasJson.barras(categorias, ConteoIglesiasDTO::getEtiqueta,
                GraficoBarrasJson.serie("reporteIglesias.serie.conLista", categorias,
                        ConteoIglesiasDTO::getConListaMiembros),
                GraficoBarrasJson.serie("reporteIglesias.serie.sinLista", categorias,
                        ConteoIglesiasDTO::getSinListaMiembros));
    }

    private static ConteoIglesiasDTO vacio() {
        return new ConteoIglesiasDTO(null, null, null, 0L, 0L, 0L, 0L);
    }

    public boolean isTodosLosCantones() {
        return cantonId == null;
    }

    public String getNombreCantonSeleccionado() {
        return cantones.stream().filter(c -> c.getId().equals(cantonId)).findFirst()
                .map(ConteoIglesiasDTO::getEtiqueta).orElse("");
    }

    public String altoGrafico(int barras) {
        return GraficoBarrasJson.alto(barras);
    }
}
