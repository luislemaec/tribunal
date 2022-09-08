package ec.com.antenasur.controller;

import ec.com.antenasur.bean.GeograpBean;
import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.domain.Geograp;
import ec.com.antenasur.domain.Iglesia;
import ec.com.antenasur.domain.IglesiaPersona;
import ec.com.antenasur.domain.tec.Mesa;
import ec.com.antenasur.domain.tec.Padron;
import ec.com.antenasur.domain.tec.Periodo;
import ec.com.antenasur.domain.tec.Recinto;
import ec.com.antenasur.service.IglesiaFacade;
import ec.com.antenasur.service.IglesiaPersonaFacade;
import ec.com.antenasur.service.tec.MesaFacade;
import ec.com.antenasur.service.tec.PadronFacade;
import ec.com.antenasur.service.tec.PeriodoFacade;
import ec.com.antenasur.service.tec.RecintoFacade;
import ec.com.antenasur.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;

import org.primefaces.event.SelectEvent;
import org.primefaces.event.TransferEvent;
import org.primefaces.model.DualListModel;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Named
@ViewScoped
@Slf4j
@NoArgsConstructor
public class PadronController implements Serializable {

    private static final String DESTINATION = System.getProperty("java.io.tmpdir");

    private static final long serialVersionUID = 1L;

    //private static final Logger LOG = Logger.getLogger(cargarControl.class);
    @Inject
    private LoginBean loginBean;

    @Inject
    private GeograpBean geograpBean;

    @Inject
    private PadronFacade padronFacade;

    @Inject
    private RecintoFacade recintoFacade;

    @Inject
    private IglesiaFacade iglesiaFacade;

    @Inject
    private MesaFacade mesaFacade;

    @Inject
    private PeriodoFacade periodoFacade;

    @Inject
    private IglesiaPersonaFacade iglesiaPersonaFacade;

    @Setter
    @Getter
    private List<Geograp> cantones, parroquias;

    @Setter
    @Getter
    private Geograp cantonSeleccionado, parroquiaSeleccionado;

    @Setter
    @Getter
    private Recinto recintoSeleccionado;

    @Setter
    @Getter
    private List<Recinto> listaRecintos, listaRecintosSeleccionados;

    @Setter
    @Getter
    private List<Padron> listaPadron, listaPadronSeleccionado;

    @Setter
    @Getter
    private List<Iglesia> listaIglesiasAsignadas, listaIglesiasPorAsignar;

    @Setter
    @Getter
    private List<IglesiaPersona> listaIglesiaPersonas;

    @Setter
    @Getter
    private List<Mesa> listaMesas, listaMesasCerradas;

    @Setter
    @Getter
    private Mesa mesaSeleccionado;

    @Setter
    @Getter
    private Padron padronSeleccionado;

    @Setter
    @Getter
    private DualListModel<Iglesia> listaNombresIglesias;

    @Setter
    @Getter
    private List<String> iglesiasOrigen, iglesiasDestino;

    @Setter
    @Getter
    private Periodo periodoActivo;

    @PostConstruct
    private void init() {
        try {
            //INICIALIZA VARIABLES
            this.inicializaVariables();
            //CARGA VALORES INICIALES
            this.cargaDatosIniciales();
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    private void inicializaVariables() {
        this.cantonSeleccionado = this.parroquiaSeleccionado = new Geograp();
        this.recintoSeleccionado = new Recinto();
        this.padronSeleccionado = new Padron();
        this.mesaSeleccionado = new Mesa();
    }

    private void cargaDatosIniciales() {
        //PERIODO ACTIVO
        this.periodoActivo = periodoFacade.getPeridoActivo();
        //CANTONES
        this.cantones = geograpBean.getByFatherId(7);
        //RECINTOS
        this.listaRecintos = recintoFacade.findAll();
        //MESAS
        this.listaMesas = mesaFacade.getMesasPorRecintos(listaRecintos);
        //PADRON
        this.listaPadron = padronFacade.getPadronPorMesas(listaMesas);
        //IGLESIAS ASIGNADAS
        PrimeFaces.current().ajax().update("frmIglesias", "msgs");

    }

    private void reseteaVariables() {        
        this.listaRecintos = null;
        this.listaMesas = null;
        this.listaPadron = null;
        this.listaIglesiasPorAsignar = listaIglesiasPorAsignar = null;
        this.listaNombresIglesias = null;
    }

    public void obtieneListaDatosPorCanton() {
        if (cantonSeleccionado.getId() != null) {
            this.cantonSeleccionado = geograpBean.getById(this.cantonSeleccionado.getId());
            reseteaVariables();
            parroquias=null;
            
            this.parroquias = geograpBean.getByFatherId(this.cantonSeleccionado.getId());
            List<Integer> listaIdParroquias = geograpBean.getListaIdSGeograp(parroquias);
            this.cargaDatosGeneraPiklist(parroquias, listaIdParroquias);
        }
    }

    private List<Integer> obtieneIdsIglesiasAsignadas(List<Integer> listaIdIglesias) {
        if (listaIdIglesias != null && !listaIdIglesias.isEmpty()) {
            if (!listaIglesiasAsignadas.isEmpty() && listaIglesiasAsignadas != null) {
                for (Iglesia item : listaIglesiasAsignadas) {
                    listaIdIglesias.add(item.getId());
                }
            }
            return listaIdIglesias;
        } else {
            if (!listaIglesiasAsignadas.isEmpty() && listaIglesiasAsignadas != null) {
                for (Iglesia item : listaIglesiasAsignadas) {
                    listaIdIglesias.add(item.getId());
                }
            }
            return listaIdIglesias;
        }
    }

    private List<Iglesia> obtieneIglesiasEnPadron(List<Padron> listaPadronTmp) {
        if (listaPadronTmp != null && !listaPadronTmp.isEmpty()) {
            List<Iglesia> listaIglesiasTmp = new ArrayList<>();
            for (Padron padron : listaPadronTmp) {
                if (!listaIglesiasTmp.contains(padron.getIglesiaPersona().getIglesia())) {
                    listaIglesiasTmp.add(padron.getIglesiaPersona().getIglesia());
                }
            }
            if (!listaIglesiasTmp.isEmpty()) {
                return listaIglesiasTmp;
            }
        } else {
            return null;
        }
        return null;
    }

    public void obtieneListaDatosPorParroquia() {
        if (this.parroquiaSeleccionado.getId() != null) {
            this.parroquiaSeleccionado = geograpBean.getById(this.parroquiaSeleccionado.getId());

            reseteaVariables();

            List<Geograp> parroquiasTmp = new ArrayList<>();
            List<Integer> parroquiasIdTmp = new ArrayList<>();
            if (this.parroquiaSeleccionado != null) {
                parroquiasTmp.add(this.parroquiaSeleccionado);
                parroquiasIdTmp.add(parroquiaSeleccionado.getId());
            }
            this.cargaDatosGeneraPiklist(parroquiasTmp, parroquiasIdTmp);
        }
    }

    public void obtieneListaDatosPorRecinto() {
        if (recintoSeleccionado.getId() != null) {
            recintoSeleccionado = recintoFacade.find(recintoSeleccionado.getId());

            List<Integer> parroquiasIdTmp = new ArrayList<>();
            List<Geograp> parroquiasTmp = new ArrayList<>();
            if (recintoSeleccionado != null) {
                parroquiasIdTmp.add(recintoSeleccionado.getId());
                parroquiasTmp.add(recintoSeleccionado.getUbicacion());
            }
            this.cargaDatosGeneraPiklist(parroquiasTmp, parroquiasIdTmp);
        }
    }

    private void cargaDatosGeneraPiklist(List<Geograp> parroquiasTmp, List<Integer> listaIdParroquias) {
        if (parroquiasTmp != null) {
            //RECINTOS        
            this.listaRecintos = recintoFacade.getRecintosPorParroquias(parroquiasTmp);
            //MESAS
            if (listaRecintos != null) {
                this.listaMesas = mesaFacade.getMesasPorRecintos(listaRecintos);
                //PADRON
                if (listaMesas != null) {
                    this.listaPadron = padronFacade.getPadronPorMesas(listaMesas);
                    //IDS IGLESIAS COMPLETOS EN PADRON
                    if (listaPadron != null) {
                        this.listaIglesiasAsignadas = obtieneIglesiasEnPadron(listaPadron);
                        //IDS IGLESIAS COMPLETOS EN PADRON POR PARROQUIAS
                        List<Integer> listaIdIglesias = padronFacade.obtieneIglesiasEnPadronCompletasPorUbicacion(listaIdParroquias);
                        //IDS IGLESIAS COMPLETOS EN PADRON
                        listaIdIglesias = obtieneIdsIglesiasAsignadas(listaIdIglesias != null ? listaIdIglesias : new ArrayList());

                        this.listaIglesiasPorAsignar = iglesiaFacade.obtieneIglesiasPorAsignarPorIds(listaIdIglesias, listaIdParroquias);

                        generaPickList();
                    } else {
                        JsfUtil.addInfoMessage("No existe padron");
                    }
                } else {
                    JsfUtil.addInfoMessage("No existe mesas");
                }
            } else {
                JsfUtil.addInfoMessage("No existe recintos");
            }
        }
    }

    public void obtieneListaDatosPorMesa() {
        if (mesaSeleccionado.getId() != null) {
            mesaSeleccionado = mesaFacade.find(mesaSeleccionado.getId());
            List<Geograp> parroquiasTmp = new ArrayList<>();
            List<Integer> parroquiasIdTmp = new ArrayList<>();

            reseteaVariables();
            if (mesaSeleccionado != null) {
                this.recintoSeleccionado = mesaSeleccionado.getRecinto();
                parroquiasTmp.add(recintoSeleccionado.getUbicacion());
            }

            parroquiasIdTmp.add(recintoSeleccionado.getId());

            this.cargaDatosGeneraPiklist(parroquiasTmp, parroquiasIdTmp);

        }
    }

    private void generaPickList() {
        if (listaIglesiasPorAsignar != null && !listaIglesiasPorAsignar.isEmpty()
                && listaIglesiasAsignadas != null && !listaIglesiasAsignadas.isEmpty()) {
            this.listaNombresIglesias = new DualListModel<>(this.listaIglesiasPorAsignar, this.listaIglesiasAsignadas);
        } else {
            JsfUtil.addWarningMessage("No existe recintos ");
        }
        PrimeFaces.current().ajax().update("frmIglesias", "msgs");
    }

    public void onTransfer(TransferEvent event) {
        for (Object item : event.getItems()) {
            asignaIglesiaPersonaPadron((Iglesia) item);
        }
        //PADRON
        this.listaPadron = padronFacade.getAllOrderbyId();
        JsfUtil.addSuccessMessage("Asigmado");
    }

    public void onSelect(SelectEvent<Iglesia> event) {
        JsfUtil.addInfoMessage("Seleccionado " + event.getObject().getNombre());
    }

    /**
     * Crea padron electroral
     */
    private List<Padron> asignaIglesiaPersonaPadron(Iglesia iglesia) {
        try {
            List<Padron> nuevaListaPadron = new ArrayList<>();
            this.listaIglesiaPersonas = iglesiaPersonaFacade.getPersonasIglesiasPorIglesia(iglesia.getId());
            if (listaIglesiaPersonas != null) {
                for (IglesiaPersona iglesiaPersonaTmp : listaIglesiaPersonas) {
                    //Busca si ya esta asignado en padron
                    Padron padronBuscado = padronFacade.buscaPorPesonaPeriodoIglesia(iglesiaPersonaTmp.getId(), periodoActivo.getId());
                    if (padronBuscado == null) {//
                        Padron nuevoPadron = new Padron(mesaSeleccionado, periodoActivo, iglesiaPersonaTmp);
                        nuevoPadron = padronFacade.create(nuevoPadron);
                        nuevaListaPadron.add(nuevoPadron);
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

}
/*404*/
