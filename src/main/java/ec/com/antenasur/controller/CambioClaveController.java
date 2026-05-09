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
import ec.com.antenasur.bean.ProcesoBean;
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
    ProcesoBean procesoBean;

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
            if (usuario == null) {
                JsfUtil.addWarningMessage("Usuario o contraseÃƒÂ±a incorrecta");
                return;
            }
            if (claveTemporal.isEmpty() || clave1.isEmpty() || clave2.isEmpty()) {
                return;
            }
            if (!clave1.equals(clave2)) {
                JsfUtil.addFatalMessage("Las contraseÃƒÂ±as no coinciden");
                return;
            }
            if (!JsfUtil.validarContrasenia(clave1)) {
                return;
            }

            String hash = JsfUtil.claveEncriptadaSHA1(clave2);
            usuario = usuarioService.cambiarContraseniaPorId(usuario.getId(), hash);

            enviarCorreoCambioClave();
            JsfUtil.addSuccessMessage("Cambio de clave exitoso");
            procesoBean.registraActividad("CAMBIA CONTRASEÃƒâ€˜A");
            loginBean.passwordChangued();
            JsfUtil.redirect("/recuperaClaveCorrecto.jsf");
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Problemas al recuperar la contrasenia");
        }
    }

    private void enviarCorreoCambioClave() {
        try {
            HashMap<String, String> parametros = correoService.construirParametrosBase();
            parametros.put("nombreApellido", usuario.getPersonaNombres());
            parametros.put("nombreUsuario", usuario.getUsername());
            parametros.put("clave", clave1);

            List<String> destinatarios = new ArrayList<>();
            destinatarios.add(usuario.getCorreo());

            correoService.enviarNotificacion(destinatarios, plantillaCorreoCambiaClave, parametros,
                    usuario.getId(), Constantes.getPathLogo());
        } catch (Exception e) {
            LOG.error("ERROR AL ENVIAR CORREO DE RECUPERACION", e);
        }
    }
}
