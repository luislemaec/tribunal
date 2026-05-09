package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.primefaces.PrimeFaces;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.dto.PlantillaCorreoDTO;
import ec.com.antenasur.service.tec.PlantillaCorreoService;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Named
@ViewScoped
@Slf4j
public class PlantillaCorreoController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String FORMULARIO = "frmPlantillaCorreo";
    private static final String TABLA = "tblPlantillaCorreo";
    private static final String MENSAJE_REGISTRA_OK = "PlantillaCorreo registrado";
    private static final String MENSAJE_ACTUALIZA_OK = "PlantillaCorreo actualizado";

    @Inject
    private LoginBean loginBean;

    @Inject
    private PlantillaCorreoService plantillaCorreoService;

    @Getter
    @Setter
    private PlantillaCorreoDTO plantillaCorreoSeleccionado;

    @Setter
    @Getter
    private List<PlantillaCorreoDTO> plantillasCorreos, plantillasCorreoSeleccionados;

    @PostConstruct
    private void init() {
        try {
            plantillaCorreoSeleccionado = new PlantillaCorreoDTO();
            plantillasCorreos = plantillaCorreoService.listarDTOs();
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    public void inicializaPlantillaCorreoSeleccionado() {
        if (plantillasCorreos != null) {
            plantillasCorreos.clear();
        }
        plantillaCorreoSeleccionado = new PlantillaCorreoDTO();
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
            return size > 1 ? size + " Plantillas seleccionadas" : "1 plantilla seleccionada";
        }
        return "Eliminar";
    }

    public void eliminarRolesSeleccionados() {
        int eliminados = 0;
        if (plantillasCorreoSeleccionados != null) {
            for (PlantillaCorreoDTO item : plantillasCorreoSeleccionados) {
                if (item.getId() != null && plantillaCorreoService.eliminarPorId(item.getId()) != null) {
                    eliminados++;
                }
            }
        }
        plantillasCorreos = plantillaCorreoService.listarDTOs();
        JsfUtil.addInfoMessage(eliminados + " Plantillas eliminadas");
        this.plantillasCorreoSeleccionados = null;
        PrimeFaces.current().ajax().update(FORMULARIO, "msgs");
    }

    public void cargarPlantillaCorreoSeleccionado() {
        if (plantillaCorreoSeleccionado != null && plantillaCorreoSeleccionado.getId() != null) {
            plantillaCorreoSeleccionado = plantillaCorreoService.obtenerDTOPorId(plantillaCorreoSeleccionado.getId());
        }
    }

    public void buscaRolPorNombre() {
        if (plantillaCorreoSeleccionado != null && plantillaCorreoSeleccionado.getAsunto() != null) {
            PlantillaCorreoDTO buscada = plantillaCorreoService.buscarDTOPorAsunto(plantillaCorreoSeleccionado.getAsunto());
            if (buscada != null) {
                plantillaCorreoSeleccionado = buscada;
                JsfUtil.addInfoMessage("PlantillaCorreo " + buscada.getAsunto() + " ya se encuentra registrado ");
            }
        }
    }

    public void actualizarRegistro() {
        try {
            if (plantillaCorreoSeleccionado == null) {
                return;
            }
            boolean esEdicion = plantillaCorreoSeleccionado.getId() != null;
            PlantillaCorreoDTO persistida = plantillaCorreoService.guardarDesdeDTO(plantillaCorreoSeleccionado);
            if (persistida != null) {
                JsfUtil.addSuccessMessage(esEdicion ? MENSAJE_ACTUALIZA_OK : MENSAJE_REGISTRA_OK);
                PrimeFaces.current().ajax().update("msgs", FORMULARIO);
            }
        } catch (Exception e) {
            log.error("ERROR EN GUARDAR PLANTILLA", e);
        }
        PrimeFaces.current().executeScript("PF('dlgRol').hide()");
        PrimeFaces.current().ajax().update(FORMULARIO, FORMULARIO + ":" + TABLA);
    }
}
