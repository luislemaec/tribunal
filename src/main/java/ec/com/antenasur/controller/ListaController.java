package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.domain.Geograp;
import ec.com.antenasur.domain.tec.Lista;
import ec.com.antenasur.service.tec.ListaFacade;
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
public class ListaController implements Serializable {

    private static final String DESTINATION = System.getProperty("java.io.tmpdir");

    private static final long serialVersionUID = 1L;

    private static final String FORMULARIO = "frmListas";
    private static final String TABLA = "tblListas";
    private static final String MENSAJE_REGISTRA_OK = "Lista registrado";
    private static final String MENSAJE_ACTUALIZA_OK = "Lista actualizado";
    private static final String MENSAJE_ELIMINA_OK = "Lista eliminado";
    public static final String MENSAJE_CONFORMACION_ELIMINAR = "¿Esta seguro de eliminar?";

    @Inject
    private LoginBean loginBean;

    @Inject
    private ListaFacade listaFacade;

    @Setter
    @Getter
    private Lista listaSeleccionado;

    @Setter
    @Getter
    private List<Lista> listas, listasSeleccionadas;

    @PostConstruct
    private void init() {
        try {

            this.listas = this.listasSeleccionadas = new ArrayList<>();
            //Trae cantones de la provincia de Chimborazo

            this.listas = listaFacade.findAll();

        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    public void inicializarSeleccionado() {
        if (listas != null) {
            listas.clear();
        }
        this.listaSeleccionado = new Lista();

    }

    private List<Integer> listaIdParroquias(List<Geograp> parroquias) {
        try {
            List<Integer> listaIdParroquias = null;
            if (parroquias != null) {
                listaIdParroquias = new ArrayList<>();
                for (Geograp item : parroquias) {
                    listaIdParroquias.add(item.getId());
                }
            }
            return listaIdParroquias;
        } catch (Exception e) {
            return null;
        }
    }

    public void nuevo() {
        inicializarSeleccionado();
    }

    public boolean existeSeleccionados() {
        return this.listasSeleccionadas != null && !this.listasSeleccionadas.isEmpty();
    }

    public String getMensajeBotonEliminar() {
        if (existeSeleccionados()) {
            int size = this.listasSeleccionadas.size();
            return size > 1 ? size + " Recintos seleccionadas" : "1 recinto seleccionada";
        }
        return "Eliminar";
    }

    public void eliminarSeleccionado() {
        if (listaSeleccionado != null) {
            listaSeleccionado = listaFacade.delete(listaSeleccionado);
        }
        JsfUtil.addInfoMessage(MENSAJE_ELIMINA_OK);
        PrimeFaces.current().ajax().update(FORMULARIO + ":" + TABLA, "msgs");
    }

    public void guardarSeleccionado() {
        try {
            if (listaSeleccionado != null) {
                if (this.listaSeleccionado.getId() != null) {
                    Lista recintoActualiza = listaFacade.edit(listaSeleccionado);
                    if (recintoActualiza != null) {
                        JsfUtil.addSuccessMessage(MENSAJE_ACTUALIZA_OK);
                        listaSeleccionado = null;
                        listas = listaFacade.findAll();
                        PrimeFaces.current().ajax().update("msgs", FORMULARIO);
                    }
                } else {
                    Lista iglesiaPersonaActualiza = listaFacade.create(listaSeleccionado);
                    if (iglesiaPersonaActualiza != null) {
                        JsfUtil.addSuccessMessage(MENSAJE_REGISTRA_OK);

                        listaSeleccionado = null;
                        listas = listaFacade.findAll();
                        PrimeFaces.current().ajax().update("msgs", FORMULARIO);
                    }
                }
            }
        } catch (Exception e) {
        }
        PrimeFaces.current().executeScript("PF('dlgMesa').hide()");
        PrimeFaces.current().ajax().update(FORMULARIO + ":" + TABLA);
    }

    public void eliminarSeleccionados() {
        if (listasSeleccionadas != null) {
            for (Lista item : listasSeleccionadas) {
                listaFacade.delete(item);
            }
        }
        JsfUtil.addInfoMessage(+listasSeleccionadas.size() + MENSAJE_ELIMINA_OK);
        PrimeFaces.current().ajax().update(FORMULARIO + ":" + TABLA, "msgs");
    }

    public void cargaDatosSeleccionado() {
        try {
            if (listaSeleccionado.getId() != null) {
                if (listaSeleccionado.getId() != null) {
                    //this.cantonSeleccionado = listaSeleccionado.getUbicacion().getGeograp();
                    //this.parroquias = geograpBean.getByFatherId(cantonSeleccionado.getId());
                }
            }
        } catch (Exception e) {
        }
    }

}
