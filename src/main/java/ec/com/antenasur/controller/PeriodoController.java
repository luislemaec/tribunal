package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.primefaces.PrimeFaces;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.dto.CargoDTO;
import ec.com.antenasur.dto.PeriodoDTO;
import ec.com.antenasur.service.tec.CargoService;
import ec.com.antenasur.service.tec.PeriodoService;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Named
@ViewScoped
@Slf4j
public class PeriodoController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private LoginBean loginBean;

    @Inject
    private PeriodoService periodoService;

    @Inject
    private CargoService cargoService;

    @Setter
    @Getter
    private PeriodoDTO periodoSeleccionado;

    @Setter
    @Getter
    private List<PeriodoDTO> listaPeriodos, listaPeriodosSeleccionados;

    @Setter
    @Getter
    private List<CargoDTO> listaCargos;

    @PostConstruct
    private void init() {
        try {
            listaPeriodos = periodoService.listarDTOs();
            listaCargos = cargoService.listarDTOs();
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    public void inicializaPersonaSeleccionado() {
        periodoSeleccionado = new PeriodoDTO();
    }

    public void nuevo() {
        inicializaPersonaSeleccionado();
    }

    public boolean existePeriodosSeleccionadas() {
        return this.listaPeriodosSeleccionados != null && !this.listaPeriodosSeleccionados.isEmpty();
    }

    public String getMensajeBotonEliminar() {
        if (existePeriodosSeleccionadas()) {
            int size = this.listaPeriodosSeleccionados.size();
            return size > 1 ? size + " periodos seleccionadas" : "1 periodo seleccionada";
        }
        return "Eliminar";
    }

    public void eliminarPeriodoSeleccionados() {
        int eliminados = 0;
        if (listaPeriodosSeleccionados != null) {
            for (PeriodoDTO item : listaPeriodosSeleccionados) {
                if (item.getId() != null && periodoService.eliminarPorId(item.getId()) != null) {
                    eliminados++;
                }
            }
        }
        JsfUtil.addInfoMessage(eliminados + " Periodos eliminados");
        PrimeFaces.current().ajax().update("frmPeriodos:pnListaPeriodos", "msgs");
        this.listaPeriodosSeleccionados = null;
    }

    public void guardar() {
        try {
            if (periodoSeleccionado == null) {
                return;
            }
            boolean esEdicion = periodoSeleccionado.getId() != null;
            PeriodoDTO persistido = periodoService.guardarDesdeDTO(periodoSeleccionado);
            if (persistido != null) {
                JsfUtil.addSuccessMessage(esEdicion ? "Actualido correctamente" : "Periodo agregado");
                listaPeriodos = periodoService.listarDTOs();
                PrimeFaces.current().ajax().update("msgs", "frmPeriodos");
            }
        } catch (Exception e) {
            log.error("ERROR AL GUARDAR PERIODO", e);
        }
        PrimeFaces.current().executeScript("PF('dlgPeriodo').hide()");
        PrimeFaces.current().ajax().update("frmPeriodos:msgs", "frmPeriodos:tblPeriodos");
    }
}
