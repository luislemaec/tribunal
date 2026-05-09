package ec.com.antenasur.bean;

import ec.com.antenasur.controller.OlvidoClaveController;
import ec.com.antenasur.model.tec.PlantillaCorreo;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import ec.com.antenasur.service.tec.PlantillaCorreoService;

import java.io.Serializable;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Luis Lema
 */
@Named(value = "plantillaCorreoBean")
@RequestScoped
public class PlantillaCorreoBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(OlvidoClaveController.class);

    private static final String CORREO_RECUPERAR_CLAVE = "CORREO RECUPERAR CLAVE";
    private static final String CORREO_OLVIDO_CONTRASENA = "CORREO OLVIDO CONTRASENA";
    private static final String CORREO_CAMBIO_CONTRASENIA = "CAMBIO CLAVE";

    @Inject
    private PlantillaCorreoService plantillaCorreoService;

    @Getter
    @Setter
    private PlantillaCorreo plantillaCorreo;

    @Setter
    @Getter
    private List<PlantillaCorreo> plantillasCorreos, plantillasCorreoSeleccionados;

    @PostConstruct
    private void init() {
        this.plantillaCorreo = new PlantillaCorreo();
    }

    public PlantillaCorreo obtieneCorreoOlvidoClave() {
        try {
            this.plantillaCorreo = plantillaCorreoService.buscarPorAsunto(CORREO_OLVIDO_CONTRASENA);
            return eliminarLlaves(this.plantillaCorreo);
        } catch (Exception e) {
            LOG.error("ERROR AL RECUPERAR CONTRASEÃƒâ€˜A", e);
            return null;
        }
    }

    public PlantillaCorreo obtieneCorreoCambioClave() {
        try {
            this.plantillaCorreo = plantillaCorreoService.buscarPorAsunto(CORREO_CAMBIO_CONTRASENIA);
            return eliminarLlaves(this.plantillaCorreo);
        } catch (Exception e) {
            LOG.error("ERROR AL RECUPERAR CONTRASEÃƒâ€˜A", e);
            return null;
        }
    }

    public PlantillaCorreo obtieneCorreoRecuperarClave() {
        try {
            this.plantillaCorreo = plantillaCorreoService.buscarPorAsunto(CORREO_RECUPERAR_CLAVE);
            return eliminarLlaves(this.plantillaCorreo);
        } catch (Exception e) {
            LOG.error("ERROR AL RECUPERAR CONTRASEÃƒâ€˜A", e);
            return null;
        }
    }

    public PlantillaCorreo obtieneCorreoPorAsunto(String asunto) {
        try {
            this.plantillaCorreo = plantillaCorreoService.buscarPorAsunto(asunto);
            return eliminarLlaves(this.plantillaCorreo);
        } catch (Exception e) {
            LOG.error("ERROR AL RECUPERAR CONTRASEÃƒâ€˜A", e);
            return null;
        }
    }

    public PlantillaCorreo eliminarLlaves(PlantillaCorreo plantillaCorreo) {
        try {
            plantillaCorreo.setMensaje(plantillaCorreo.getMensaje().replaceAll("\\{|\\}", ""));
            return plantillaCorreo;
        } catch (Exception e) {
            LOG.error("ERROR AL RECUPERAR CONTRASEÃƒâ€˜A", e);
            return null;
        }
    }

}
