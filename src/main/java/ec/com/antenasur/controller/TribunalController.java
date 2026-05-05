package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.dto.CatalogoGeneralDTO;
import ec.com.antenasur.dto.PeriodoDTO;
import ec.com.antenasur.dto.TribunalDTO;
import ec.com.antenasur.service.tec.CatalogoGeneralService;
import ec.com.antenasur.service.tec.PeriodoService;
import ec.com.antenasur.service.tec.TribunalService;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Named
@ViewScoped
@Slf4j
public class TribunalController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Integer ID_CARGO_PADRE = 2;

    @Inject
    private LoginBean loginBean;

    @Inject
    private PeriodoService periodoService;

    @Inject
    private TribunalService tribunalService;

    @Inject
    private CatalogoGeneralService catalogoService;

    @Setter
    @Getter
    private PeriodoDTO periodoSeleccionado;

    @Setter
    @Getter
    private TribunalDTO tribunalSeleccionado;

    @Setter
    @Getter
    private List<PeriodoDTO> listaPeriodos, listaPeriodosSeleccionados;

    @Setter
    @Getter
    private List<CatalogoGeneralDTO> listaCargos;

    @Setter
    @Getter
    private List<TribunalDTO> listaAutoridadesTribunal;

    @Setter
    @Getter
    private String cedulaBuscar;

    @PostConstruct
    private void init() {
        try {
            listaPeriodos = periodoService.listarDTOs();
            if (listaPeriodos != null && !listaPeriodos.isEmpty()) {
                periodoSeleccionado = listaPeriodos.get(0);
            }
            listaCargos = catalogoService.listarDTOsHijosDe(ID_CARGO_PADRE);
            cargarAutoridadesTribunal();
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    private void cargarAutoridadesTribunal() {
        Integer periodoId = (periodoSeleccionado != null) ? periodoSeleccionado.getId() : null;
        listaAutoridadesTribunal = tribunalService.listarAutoridadesConPlaceholders(periodoId, ID_CARGO_PADRE);
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

    public void guardarAutoridad() {
        try {
            if (tribunalSeleccionado == null) {
                return;
            }
            boolean esEdicion = tribunalSeleccionado.getId() != null;
            TribunalDTO persistido = tribunalService.guardarDesdeDTO(tribunalSeleccionado);
            if (persistido != null) {
                tribunalSeleccionado = persistido;
                JsfUtil.addSuccessMessage(esEdicion ? "Actualido correctamente" : "Autoridad agregada");
                PrimeFaces.current().ajax().update("msgs", "frmPeriodos");
            }
        } catch (Exception e) {
            log.error("ERROR AL GUARDAR AUTORIDADES", e);
        }
        PrimeFaces.current().executeScript("PF('dlgPeriodo').hide()");
        PrimeFaces.current().ajax().update("frmPeriodos:msgs", "frmPeriodos:tblPeriodos");
    }

    public void buscaPersona() {
        try {
            TribunalDTO actualizado = tribunalService.asignarPersonaPorCedula(tribunalSeleccionado, cedulaBuscar);
            if (actualizado != null) {
                tribunalSeleccionado = actualizado;
                JsfUtil.addInfoMessage("PERSONA SELECCIONADA");
            } else {
                JsfUtil.addWarningMessage("PERSONA NO ENCONTRADA");
            }
            PrimeFaces.current().ajax().update("frmPeriodos:outPnlAsignaAutoridadBusca",
                    "frmPeriodos:outPnlAsignaAutoridad", "msgs");
        } catch (Exception e) {
        }
    }
}
