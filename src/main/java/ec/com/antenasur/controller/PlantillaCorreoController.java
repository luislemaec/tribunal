package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;

import ec.com.antenasur.bean.LoginBean;

import ec.com.antenasur.domain.tec.PlantillaCorreo;

import ec.com.antenasur.service.tec.PlantillaCorreoFacade;
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
public class PlantillaCorreoController implements Serializable {

    private static final String DESTINATION = System.getProperty("java.io.tmpdir");

    private static final long serialVersionUID = 1L;

    private static final String FORMULARIO = "frmPlantillaCorreo";
    private static final String TABLA = "tblPlantillaCorreo";
    private static final String MENSAJE_REGISTRA_OK = "PlantillaCorreo registrado";
    private static final String MENSAJE_ACTUALIZA_OK = "PlantillaCorreo actualizado";
    private static final String MENSAJE_ELIMINA_OK = "PlantillaCorreo eliminado";
    public static final String MENSAJE_CONFORMACION_ELIMINAR = "¿Esta seguro de eliminar?";

    @Inject
    private LoginBean loginBean;

    @Inject
    private PlantillaCorreoFacade plantillaCorreoFacade;

    @Getter
    @Setter
    private PlantillaCorreo plantillaCorreoSeleccionado;

    @Setter
    @Getter
    private List<PlantillaCorreo> plantillasCorreos, plantillasCorreoSeleccionados;

    @PostConstruct
    private void init() {
        try {
            plantillaCorreoSeleccionado = new PlantillaCorreo();
            plantillasCorreos = plantillaCorreoFacade.findAll();
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    /**
     * Inicializa medio seleccionado
     */
    public void inicializaPlantillaCorreoSeleccionado() {
        if (plantillasCorreos != null) {
            plantillasCorreos.clear();
        }
        plantillaCorreoSeleccionado = new PlantillaCorreo();

    }

    public void nuevoRol() {
        inicializaPlantillaCorreoSeleccionado();
    }

    public boolean existeRolesSeleccionados() {
        return this.plantillasCorreoSeleccionados != null && !this.plantillasCorreoSeleccionados.isEmpty();
    }

    public String getMensajeBotonEliminar() {
        if (existeRolesSeleccionados()) {
            int size = this.plantillasCorreoSeleccionados.size();
            return size > 1 ? size + " Roles seleccionadas" : "1 rol seleccionado";
        }
        return "Eliminar";
    }

    public void eliminarRolesSeleccionados() {
        if (plantillasCorreoSeleccionados != null) {
            for (PlantillaCorreo item : plantillasCorreoSeleccionados) {
                plantillaCorreoFacade.delete(item);
            }
        }
        plantillasCorreos = plantillaCorreoFacade.findAll();
        JsfUtil.addInfoMessage(+plantillasCorreoSeleccionados.size() + " Roles eliminados");
        this.plantillasCorreoSeleccionados = null;
        PrimeFaces.current().ajax().update(FORMULARIO, "msgs");

    }

    public void cargarPlantillaCorreoSeleccionado() {
        if (plantillaCorreoSeleccionado != null && plantillaCorreoSeleccionado.getId() != null) {
            plantillaCorreoSeleccionado = plantillaCorreoFacade.find(plantillaCorreoSeleccionado.getId());
        }
    }

    public void buscaRolPorNombre() {
        if (plantillaCorreoSeleccionado != null) {
            PlantillaCorreo plantillaCorreoBuscado = plantillaCorreoFacade.buscarPorAsunto(plantillaCorreoSeleccionado.getAsunto());
            if (plantillaCorreoBuscado != null) {
                plantillaCorreoSeleccionado = plantillaCorreoBuscado;
                JsfUtil.addInfoMessage("PlantillaCorreo " + plantillaCorreoBuscado.getAsunto() + " ya se encuentra registrado ");
            }
        }
    }

    public void actualizarRegistro() {
        try {
            if (plantillaCorreoSeleccionado != null) {
                if (this.plantillaCorreoSeleccionado.getId() != null) {
                    PlantillaCorreo rolActualiza = plantillaCorreoFacade.edit(plantillaCorreoSeleccionado);
                    if (rolActualiza != null) {
                        JsfUtil.addSuccessMessage(MENSAJE_ACTUALIZA_OK);                        
                        PrimeFaces.current().ajax().update("msgs", FORMULARIO);
                    }
                } else {
                    PlantillaCorreo rolNuevo = plantillaCorreoFacade.create(plantillaCorreoSeleccionado);
                    if (rolNuevo != null) {
                        JsfUtil.addSuccessMessage(MENSAJE_REGISTRA_OK);
                        PrimeFaces.current().ajax().update("msgs", FORMULARIO);
                    }
                }                
            }
        } catch (Exception e) {
            log.error("ERROR EN AGUARDAR ROL");
        }
        PrimeFaces.current().executeScript("PF('dlgRol').hide()");
        PrimeFaces.current().ajax().update(FORMULARIO, FORMULARIO + ":" + TABLA);
    }
}
