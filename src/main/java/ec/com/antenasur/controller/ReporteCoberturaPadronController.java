package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import ec.com.antenasur.dto.ConteoCoberturaDTO;
import ec.com.antenasur.dto.ProcesoElectoralDTO;
import ec.com.antenasur.report.GraficoBarrasJson;
import ec.com.antenasur.service.tec.ReporteCoberturaPadronService;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Pestaña Cobertura del padrón de Rep. Registros. Se crea solo al abrir la
 * pestaña (tabView dinámico), así que sus consultas no se ejecutan antes.
 */
@Named
@ViewScoped
@Slf4j
public class ReporteCoberturaPadronController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ReporteCoberturaPadronService reporteCoberturaPadronService;

    @Getter
    private ProcesoElectoralDTO proceso;

    /** Cantón del filtro; null = Todos los cantones. */
    @Getter
    @Setter
    private Integer cantonId;

    @Getter
    private List<ConteoCoberturaDTO> cantones = Collections.emptyList();

    @Getter
    private List<ConteoCoberturaDTO> categorias = Collections.emptyList();

    @Getter
    private ConteoCoberturaDTO totales = new ConteoCoberturaDTO(null, null, null, 0L, 0L);

    @Getter
    private String grafico = GraficoBarrasJson.VACIO;

    @PostConstruct
    public void init() {
        cargar();
    }

    public void cambiarCanton() {
        cargar();
    }

    private void cargar() {
        try {
            proceso = reporteCoberturaPadronService.procesoActivo();
            if (proceso == null || proceso.getId() == null) {
                proceso = null;
                cantones = Collections.emptyList();
                categorias = Collections.emptyList();
                totales = new ConteoCoberturaDTO(null, null, null, 0L, 0L);
            } else {
                Integer procesoId = proceso.getId();
                List<ConteoCoberturaDTO> porCanton = reporteCoberturaPadronService.porCanton(procesoId);
                cantones = porCanton;
                if (cantonId != null && cantones.stream().noneMatch(c -> cantonId.equals(c.getId()))) {
                    cantonId = null;
                }
                categorias = cantonId == null ? porCanton
                        : reporteCoberturaPadronService.porParroquia(procesoId, cantonId);
                totales = reporteCoberturaPadronService.totales(procesoId, cantonId);
            }
        } catch (Exception e) {
            log.error("Error al cargar Cobertura del padrón cantonId={}", cantonId, e);
            categorias = Collections.emptyList();
            totales = new ConteoCoberturaDTO(null, null, null, 0L, 0L);
            JsfUtil.addErrorMessage(JsfUtil.getMessage("reporteRegistros.error.carga"));
        }
        grafico = GraficoBarrasJson.barras(categorias, ConteoCoberturaDTO::getEtiqueta,
                GraficoBarrasJson.serie("reporteCobertura.serie.empadronados", categorias,
                        ConteoCoberturaDTO::getEmpadronados),
                GraficoBarrasJson.serie("reporteCobertura.serie.sinPadron", categorias,
                        ConteoCoberturaDTO::getSinPadron));
    }

    public boolean isSinProceso() {
        return proceso == null;
    }

    public boolean isTodosLosCantones() {
        return cantonId == null;
    }

    public String getNombreCantonSeleccionado() {
        return cantones.stream().filter(c -> c.getId().equals(cantonId)).findFirst()
                .map(ConteoCoberturaDTO::getEtiqueta).orElse("");
    }

    public String altoGrafico(int barras) {
        return GraficoBarrasJson.alto(barras);
    }
}
