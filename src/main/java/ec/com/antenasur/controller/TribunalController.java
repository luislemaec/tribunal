package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.domain.IglesiaPersona;
import ec.com.antenasur.domain.tec.CatalogoGeneral;
import ec.com.antenasur.domain.tec.Tribunal;
import ec.com.antenasur.domain.tec.Periodo;
import ec.com.antenasur.service.tec.CatalogoGeneralFacade;
import ec.com.antenasur.service.IglesiaPersonaFacade;
import ec.com.antenasur.service.tec.TribunalFacade;
import ec.com.antenasur.service.tec.PeriodoFacade;
import ec.com.antenasur.util.JsfUtil;
import java.util.ArrayList;
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
public class TribunalController implements Serializable {

    private static final String DESTINATION = System.getProperty("java.io.tmpdir");

    private static final long serialVersionUID = 1L;

    //private static final Logger LOG = Logger.getLogger(cargarControl.class);
    @Inject
    private LoginBean loginBean;

    @Inject
    private PeriodoFacade periodoFacade;

    @Inject
    private IglesiaPersonaFacade iglesiaPersonaFacade;

    @Inject
    private TribunalFacade tribunalFacade;

    @Inject
    private CatalogoGeneralFacade catalogoFacade;

    @Setter
    @Getter
    private Periodo periodoSeleccionado;

    @Setter
    @Getter
    private Tribunal tribunalSeleccionado;

    @Setter
    @Getter
    private List<Periodo> listaPeriodos, listaPeriodosSeleccionados;

    @Setter
    @Getter
    private List<CatalogoGeneral> listaCargos;

    @Setter
    @Getter
    private List<Tribunal> listaAutoridadesTribunal;

    @Setter
    @Getter
    private String cedulaBuscar;

    @PostConstruct
    private void init() {
        try {
            listaPeriodos = periodoFacade.findAll();
            periodoSeleccionado = listaPeriodos.get(0);
            listaCargos = catalogoFacade.listaCatalogoHijo(2);
            cargarAutoridadesTribunal();

        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    private void cargarAutoridadesTribunal() {
        try {
            listaAutoridadesTribunal = tribunalFacade.getRegistrosActivos();
            if (listaAutoridadesTribunal == null) {
                listaAutoridadesTribunal = new ArrayList<>();

                listaCargos.forEach(cargo -> {
                    creaAutoridadTribunalTempora(cargo);
                });
            } else {
                List<Integer> listaIdCargos = new ArrayList<>();
                listaAutoridadesTribunal.forEach(tribunal -> {
                    listaIdCargos.add(tribunal.getCargo().getId());
                });
                List<CatalogoGeneral> listaCargosTmp = catalogoFacade.listaCatalogoHijo(2, listaIdCargos);

                listaCargosTmp.forEach(cargo -> {
                    creaAutoridadTribunalTempora(cargo);
                });
            }
        } catch (Exception e) {
            log.error("ERROR AL CARGAR AUTORIDADES OBJETOS", e);
        }
    }

    private void creaAutoridadTribunalTempora(CatalogoGeneral cargo) {
        Tribunal tribunalTmp = new Tribunal();
        tribunalTmp.setCargo(cargo);
        tribunalTmp.setPeriodo(periodoSeleccionado);
        listaAutoridadesTribunal.add(tribunalTmp);
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

    public void guardarAutoridad() {
        try {
            if (tribunalSeleccionado != null) {
                if (this.tribunalSeleccionado.getId() != null) {
                    Tribunal tribunalActualiza = tribunalFacade.edit(tribunalSeleccionado);
                    if (tribunalActualiza != null) {
                        JsfUtil.addSuccessMessage("Actualido correctamente");

                        PrimeFaces.current().ajax().update("msgs", "frmPeriodos");
                    }
                } else {
                    tribunalSeleccionado = tribunalFacade.create(tribunalSeleccionado);
                    if (tribunalSeleccionado != null) {
                        JsfUtil.addSuccessMessage("Persona agregado");
                        PrimeFaces.current().ajax().update("msgs", "frmPeriodos");
                    }
                }
            }
        } catch (Exception e) {
            log.error("ERROR AL GUARDAR AUTORIDADES", e);
        }
        PrimeFaces.current().executeScript("PF('dlgPeriodo').hide()");
        PrimeFaces.current().ajax().update("frmPeriodos:msgs", "frmPeriodos:tblPeriodos");
    }

    public void buscaPersona() {
        try {
            IglesiaPersona iglesiaPersonaBuscado = iglesiaPersonaFacade.buscarPorCedulaPersona(cedulaBuscar);
            if (iglesiaPersonaBuscado != null) {
                tribunalSeleccionado.setIglesiaPersona(iglesiaPersonaBuscado);
                JsfUtil.addInfoMessage("PERSONA SELECCIONADA");
            } else {
                JsfUtil.addWarningMessage("PERSONA NO ENCONTRADA");
            }
            PrimeFaces.current().ajax().update("frmPeriodos:outPnlAsignaAutoridadBusca", "frmPeriodos:outPnlAsignaAutoridad", "msgs");
        } catch (Exception e) {
        }
    }
}
