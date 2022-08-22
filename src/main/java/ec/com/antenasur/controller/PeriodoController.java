package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.domain.Cargo;
import ec.com.antenasur.domain.Periodo;
import ec.com.antenasur.service.CargoFacade;
import ec.com.antenasur.service.PeriodoFacade;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Named
@ViewScoped
@Slf4j
public class PeriodoController implements Serializable {

    private static final String DESTINATION = System.getProperty("java.io.tmpdir");

    private static final long serialVersionUID = 1L;

    //private static final Logger LOG = Logger.getLogger(cargarControl.class);
    @Inject
    private LoginBean loginBean;

    @Inject
    private PeriodoFacade periodoFacade;

    @Inject
    private CargoFacade cargoFacade;

    @Setter
    @Getter
    private Periodo periodoSeleccionado;

    @Setter
    @Getter
    private List<Periodo> listaPeriodos, listaPeriodosSeleccionados;

    @Setter
    @Getter
    private List<Cargo> listaCargos;

    @PostConstruct
    private void init() {
        try {
            listaPeriodos = periodoFacade.findAll();
            listaCargos = cargoFacade.findAll();

        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    public void inicializaPersonaSeleccionado() {

        periodoSeleccionado = new Periodo();

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
        if (listaPeriodosSeleccionados != null) {
            for (Periodo item : listaPeriodosSeleccionados) {                
                periodoFacade.delete(item);
            }
        }
        JsfUtil.addInfoMessage(+listaPeriodosSeleccionados.size() + " Personas eliminadas");
        PrimeFaces.current().ajax().update("frmPeriodos:pnListaPeriodos", "msgs");
        this.listaPeriodosSeleccionados = null;
    }

    public void guardar() {
        try {
            if (periodoSeleccionado != null) {            	
                if (this.periodoSeleccionado.getId() != null) {                	                
                    Periodo periodoActualiza = periodoFacade.edit(periodoSeleccionado);
                    if (periodoActualiza != null) {
                        JsfUtil.addSuccessMessage("Actualido correctamente");

                        listaPeriodos = periodoFacade.findAll();
                        PrimeFaces.current().ajax().update("msgs", "frmPeriodos");
                    }
                } else {
                    periodoSeleccionado = periodoFacade.create(periodoSeleccionado);
                    if (periodoSeleccionado != null) {
                        JsfUtil.addSuccessMessage("Persona agregado");
                        listaPeriodos = periodoFacade.findAll();
                        PrimeFaces.current().ajax().update("msgs", "frmPeriodos");
                    }
                }
            }
        } catch (Exception e) {
        }
        PrimeFaces.current().executeScript("PF('dlgPeriodo').hide()");
        PrimeFaces.current().ajax().update("frmPeriodos:msgs", "frmPeriodos:tblPeriodos");
    }
}
