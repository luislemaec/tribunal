package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;

import ec.com.antenasur.bean.GeograpBean;
import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.domain.Geograp;
import ec.com.antenasur.domain.tec.Recinto;
import ec.com.antenasur.service.tec.RecintoFacade;
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
public class RecintoController implements Serializable {

    private static final String DESTINATION = System.getProperty("java.io.tmpdir");

    private static final long serialVersionUID = 1L;

    //private static final Logger LOG = Logger.getLogger(cargarControl.class);
    @Inject
    private LoginBean loginBean;

    @Inject
    private RecintoFacade recintoFacade;

    @Inject
    private GeograpBean geograpBean;

    @Setter
    @Getter
    private Recinto recintoSeleccionado;

    @Setter
    @Getter
    private List<Geograp> cantones, parroquias;

    @Setter
    @Getter
    private Geograp cantonSeleccionado, parroquiaSeleccionado;

    @Setter
    @Getter
    private List<Recinto> listaRecintos, listaRecintosSeleccionados;

    @PostConstruct
    private void init() {
        try {
            cantonSeleccionado = parroquiaSeleccionado = new Geograp();
            this.listaRecintos = this.listaRecintosSeleccionados = new ArrayList<>();
            //Trae cantones de la provincia de Chimborazo
            this.cantones = geograpBean.getByFatherId(7);
            this.listaRecintos = recintoFacade.findAll();
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    public void inicializaRecintoSeleccionado() {
        if (listaRecintos != null) {
            listaRecintos.clear();
        }
        this.recintoSeleccionado = new Recinto();
        this.recintoSeleccionado.setUbicacion(new Geograp());
    }

    public void nuevaRecinto() {
        inicializaRecintoSeleccionado();
    }

    public boolean existeRecintosSeleccionados() {
        return this.listaRecintosSeleccionados != null && !this.listaRecintosSeleccionados.isEmpty();
    }

    public String getMensajeBotonEliminar() {
        if (existeRecintosSeleccionados()) {
            int size = this.listaRecintosSeleccionados.size();
            return size > 1 ? size + " Recintos seleccionadas" : "1 recinto seleccionada";
        }
        return "Eliminar";
    }

    public void eliminarRecintoSeleccionado() {
        if (recintoSeleccionado != null) {
            recintoSeleccionado = recintoFacade.delete(recintoSeleccionado);
        }
        JsfUtil.addInfoMessage(" Registro eliminado");
        PrimeFaces.current().ajax().update("frmPersonas:tblRecintos", "msgs");
    }

    public void obtieneParroquias() {
        if (cantonSeleccionado.getId() != null) {
            cantonSeleccionado = geograpBean.getById(cantonSeleccionado.getId());
            parroquias = geograpBean.getByFatherId(cantonSeleccionado.getId());

            listaRecintos = recintoFacade.getRecintosPorParroquias(parroquias);
        } else {
            parroquias.clear();
            recintoSeleccionado = null;
            listaRecintos.clear();
        }
    }

    public void obtieneRecintosPorParroquia() {
        if (parroquiaSeleccionado.getId() != null) {
            parroquiaSeleccionado = geograpBean.getById(parroquiaSeleccionado.getId());

            List<Geograp> parroquiasTmp = new ArrayList<>();
            if (parroquiaSeleccionado != null) {
                parroquiasTmp.add(parroquiaSeleccionado);
            }
            listaRecintos = recintoFacade.getRecintosPorParroquias(parroquiasTmp);
            if (listaRecintos == null) {

                JsfUtil.addWarningMessage("No existe registro de Iglesias en " + parroquiaSeleccionado.getName());
            } else {
                JsfUtil.addInfoMessage(listaRecintos.size() + " Iglesias registradas");
            }
        } else {
            recintoSeleccionado = new Recinto();
            listaRecintos.clear();
        }
    }

    public void guardarRecintoSeleccionado() {
        try {
            if (recintoSeleccionado != null) {
                if (this.recintoSeleccionado.getId() != null) {
                    Recinto recintoActualiza = recintoFacade.edit(recintoSeleccionado);
                    if (recintoActualiza != null) {
                        JsfUtil.addSuccessMessage("Persona actualido");

                        recintoSeleccionado = null;
                        listaRecintos = recintoFacade.findAll();
                        PrimeFaces.current().ajax().update("msgs", "frmRecintos");
                    }
                } else {
                    Recinto iglesiaPersonaActualiza = recintoFacade.create(recintoSeleccionado);
                    if (iglesiaPersonaActualiza != null) {
                        JsfUtil.addSuccessMessage("Persona agregado");

                        recintoSeleccionado = null;
                        listaRecintos = recintoFacade.findAll();
                        PrimeFaces.current().ajax().update("msgs", "frmRecintos");
                    }
                }
            }
        } catch (Exception e) {
        }
        PrimeFaces.current().executeScript("PF('dlgRecinto').hide()");
        PrimeFaces.current().ajax().update("frmRecintos:messages", "frmRecintos:tblRecintos");
    }

    public void eliminarRecintosSeleccionados() {
        if (listaRecintosSeleccionados != null) {
            for (Recinto item : listaRecintosSeleccionados) {
                recintoFacade.delete(item);
            }
        }
        JsfUtil.addInfoMessage(+listaRecintosSeleccionados.size() + " Registros eliminadas");
        PrimeFaces.current().ajax().update("frmRecintos:tblRecintos", "msgs");
    }

    public void cagraDatosRecintoSeleccionado() {
        try {
            if (recintoSeleccionado.getId() != null) {
                if (recintoSeleccionado.getUbicacion().getId() != null) {
                    this.cantonSeleccionado = recintoSeleccionado.getUbicacion().getGeograp();
                    this.parroquias = geograpBean.getByFatherId(cantonSeleccionado.getId());
                }
            }
        } catch (Exception e) {
        }
    }
}
