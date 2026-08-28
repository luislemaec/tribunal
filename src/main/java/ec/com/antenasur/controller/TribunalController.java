package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.primefaces.PrimeFaces;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.dto.CatalogoGeneralDTO;
import ec.com.antenasur.dto.ProcesoElectoralDTO;
import ec.com.antenasur.dto.TribunalDTO;
import ec.com.antenasur.service.tec.CatalogoGeneralService;
import ec.com.antenasur.service.tec.ProcesoElectoralService;
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
    private ProcesoElectoralService procesoElectoralService;

    @Inject
    private TribunalService tribunalService;

    @Inject
    private CatalogoGeneralService catalogoService;

    @Setter
    @Getter
    private ProcesoElectoralDTO periodoSeleccionado;

    @Setter
    @Getter
    private TribunalDTO tribunalSeleccionado;

    @Setter
    @Getter
    private List<ProcesoElectoralDTO> listaPeriodos, listaPeriodosSeleccionados;

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
            listaPeriodos = procesoElectoralService.listarDTOs();
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
        periodoSeleccionado = new ProcesoElectoralDTO();
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
            for (ProcesoElectoralDTO item : listaPeriodosSeleccionados) {
                if (item.getId() != null && procesoElectoralService.eliminarPorId(item.getId()) != null) {
                    eliminados++;
                }
            }
        }
        JsfUtil.addInfoMessage(eliminados + " Periodos eliminados");
        PrimeFaces.current().ajax().update("frmPeriodos:pnListaPeriodos", "msgs");
        this.listaPeriodosSeleccionados = null;
    }

    public void eliminarAutoridadSeleccionada() {
        try {
            if (tribunalSeleccionado == null || tribunalSeleccionado.getId() == null) {
                JsfUtil.addWarningMessage("Seleccione una autoridad para eliminar.");
                PrimeFaces.current().ajax().update("frmGlobal:growlGlobal");
                return;
            }
            TribunalDTO eliminada = tribunalService.eliminarPorId(tribunalSeleccionado.getId());
            if (eliminada == null) {
                JsfUtil.addWarningMessage("No se pudo eliminar la autoridad seleccionada.");
            } else {
                JsfUtil.addSuccessMessage("Autoridad eliminada correctamente.");
            }
            tribunalSeleccionado = null;
            cargarAutoridadesTribunal();
            PrimeFaces.current().ajax().update("frmPeriodos:tbtribunal", "frmGlobal:growlGlobal");
        } catch (Exception e) {
            log.error("ERROR AL ELIMINAR AUTORIDAD", e);
            JsfUtil.addErrorMessage("No se pudo eliminar la autoridad. Verifique si tiene relaciones activas.");
            PrimeFaces.current().ajax().update("frmGlobal:growlGlobal");
        }
    }

    /** Prepara una copia de la fila para asignar o reasignar sin alterar la tabla. */
    public void prepararAsignacionAutoridad(TribunalDTO autoridad) {
        if (autoridad == null || autoridad.getCargoId() == null || autoridad.getProcesoId() == null) {
            JsfUtil.addWarningMessage("No se pudo determinar el cargo a asignar.");
            return;
        }
        TribunalDTO seleccion = new TribunalDTO();
        seleccion.setId(autoridad.getId());
        seleccion.setProcesoId(autoridad.getProcesoId());
        seleccion.setProcesoNombre(autoridad.getProcesoNombre());
        seleccion.setPeriodoId(autoridad.getPeriodoId());
        seleccion.setPeriodoNombre(autoridad.getPeriodoNombre());
        seleccion.setCargoId(autoridad.getCargoId());
        seleccion.setCargoNombre(autoridad.getCargoNombre());
        seleccion.setIglesiaPersona(autoridad.getIglesiaPersona());
        tribunalSeleccionado = seleccion;
        cedulaBuscar = null;
    }

    public void guardarAutoridad() {
        try {
            if (tribunalSeleccionado == null) {
                JsfUtil.addWarningMessage("Seleccione una autoridad para guardar.");
                FacesContext.getCurrentInstance().validationFailed();
                return;
            }
            boolean esEdicion = tribunalSeleccionado.getId() != null;
            TribunalDTO persistido = tribunalService.guardarDesdeDTO(tribunalSeleccionado);
            if (persistido != null) {
                tribunalSeleccionado = persistido;
                cargarAutoridadesTribunal();
                JsfUtil.addSuccessMessage(esEdicion ? "Autoridad reasignada correctamente." : "Autoridad asignada correctamente.");
                PrimeFaces.current().ajax().update("frmPeriodos:tbtribunal", "frmGlobal:growlGlobal");
                PrimeFaces.current().executeScript("PF('dlgAsignaAutoridad').hide()");
            }
        } catch (ec.com.antenasur.exception.NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
            FacesContext.getCurrentInstance().validationFailed();
            PrimeFaces.current().ajax().update("frmGlobal:growlGlobal");
        } catch (Exception e) {
            log.error("ERROR AL GUARDAR AUTORIDADES", e);
            JsfUtil.addErrorMessage("No se pudo guardar la autoridad.");
            FacesContext.getCurrentInstance().validationFailed();
            PrimeFaces.current().ajax().update("frmGlobal:growlGlobal");
        }
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
                    "frmPeriodos:outPnlAsignaAutoridad", "frmPeriodos:outPnlFooter", "frmGlobal:growlGlobal");
        } catch (ec.com.antenasur.exception.NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
            FacesContext.getCurrentInstance().validationFailed();
            PrimeFaces.current().ajax().update("frmPeriodos:outPnlAsignaAutoridadBusca",
                    "frmPeriodos:outPnlAsignaAutoridad", "frmPeriodos:outPnlFooter", "frmGlobal:growlGlobal");
        } catch (Exception e) {
            log.error("ERROR AL BUSCAR PERSONA PARA AUTORIDAD", e);
            JsfUtil.addErrorMessage("No se pudo buscar la persona.");
            FacesContext.getCurrentInstance().validationFailed();
        }
    }
}
