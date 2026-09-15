package ec.com.antenasur.controller;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.bean.PlantillaCorreoBean;
import ec.com.antenasur.dto.UsuarioDTO;
import ec.com.antenasur.model.tec.PlantillaCorreo;
import ec.com.antenasur.service.UsuarioService;
import ec.com.antenasur.service.tec.CorreoService;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;

@Named
@RequestScoped
public class CambioClaveController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(CambioClaveController.class);

    @Inject
    LoginBean loginBean;

    @Inject
    private UsuarioService usuarioService;

    @Inject
    private PlantillaCorreoBean plantillaCorreoBean;

    @Inject
    private CorreoService correoService;

    @Setter
    @Getter
    private String clave1, claveTemporal, clave2;

    @Setter
    @Getter
    private UsuarioDTO usuario;

    @Setter
    @Getter
    private PlantillaCorreo plantillaCorreoCambiaClave;

    @PostConstruct
    private void init() {
        try {
            this.usuario = loginBean.getUsuario();
            this.plantillaCorreoCambiaClave = plantillaCorreoBean.obtieneCorreoCambioClave();
        } catch (Exception e) {
            LOG.error("ERROR INICIALIZAR VARIABLES", e);
        }
    }

    public void cambiarClave() throws RuntimeException, IOException, ServletException {
        try {
            if (usuario == null || usuario.getId() == null) {
                JsfUtil.addWarningMessage("No fue posible validar la sesión del usuario");
                return;
            }
            if (claveTemporal == null || clave1 == null || clave2 == null) {
                return;
            }
            if (!clave1.equals(clave2)) {
                JsfUtil.addErrorMessage("Las contraseñas no coinciden");
                return;
            }
            if (!JsfUtil.validarContrasenia(clave1)) {
                return;
            }

            usuario = usuarioService.cambiarContraseniaAutenticada(usuario.getId(), usuario.getUsername(),
                    claveTemporal, clave1);
            if (usuario == null) {
                JsfUtil.addErrorMessage("La contraseña actual no es correcta");
                return;
            }

            enviarCorreoCambioClave();
            loginBean.passwordChangued();
            JsfUtil.redirect("/claveActualizada.jsf");
        } catch (Exception e) {
            LOG.error("Error al cambiar la contraseña", e);
            JsfUtil.addErrorMessage("No fue posible cambiar la contraseña");
        }
    }

    private void enviarCorreoCambioClave() {
        try {
            HashMap<String, String> parametros = correoService.construirParametrosBase();
            parametros.put("nombreApellido", usuario.getPersonaNombres());
            parametros.put("nombreUsuario", usuario.getUsername());

            List<String> destinatarios = new ArrayList<>();
            destinatarios.add(usuario.getCorreo());

            correoService.enviarNotificacion(destinatarios, plantillaCorreoCambiaClave, parametros,
                    usuario.getId(), Constantes.getPathLogo());
        } catch (Exception e) {
            LOG.error("ERROR AL ENVIAR CORREO DE RECUPERACION", e);
        }
    }
}
