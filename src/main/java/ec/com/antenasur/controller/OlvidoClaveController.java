package ec.com.antenasur.controller;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;

import org.apache.log4j.Logger;

import ec.com.antenasur.bean.PlantillaCorreoBean;
import ec.com.antenasur.bean.ProcesoBean;
import ec.com.antenasur.model.Usuario;
import ec.com.antenasur.model.tec.PlantillaCorreo;
import ec.com.antenasur.service.UsuarioService;
import ec.com.antenasur.service.tec.CorreoService;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;

@Named
@RequestScoped
public class OlvidoClaveController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(OlvidoClaveController.class);

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
    private String username, claveTemporal, correo;

    @Setter
    @Getter
    private Usuario usuario;

    @Setter
    @Getter
    private PlantillaCorreo plantillaCorreoRecuperarClave;

    @PostConstruct
    private void init() {
        try {
            this.usuario = new Usuario();
            this.plantillaCorreoRecuperarClave = plantillaCorreoBean.obtieneCorreoRecuperarClave();
        } catch (Exception e) {
            LOG.error("ERROR INICIALIZAR VARIABLES", e);
        }
    }

    public void recuperarClave() throws RuntimeException, IOException, ServletException {
        try {
            if (username == null || username.isEmpty() || correo == null || correo.isEmpty()) {
                return;
            }
            claveTemporal = JsfUtil.generatePassword();
            String hash = JsfUtil.claveEncriptadaSHA1(claveTemporal);
            usuario = usuarioService.iniciarRecuperacionClave(username, correo, claveTemporal, hash);

            if (usuario == null) {
                JsfUtil.addWarningMessage("Datos incorrectos, revise por favor");
                return;
            }
            procesoBean.registraActividad("RECUPERA CLAVE OLVIDADO");
            sendMailRecoveryPassword();
            JsfUtil.redirect("/recuperaClaveCorrecto.jsf");
        } catch (Exception e) {
            LOG.error("ERROR AL RECUPERAR CONTRASEÑA", e);
            JsfUtil.addErrorMessage("Problemas al restablecer la contrasenia");
        }
    }

    private void sendMailRecoveryPassword() {
        try {
            HashMap<String, String> parametros = correoService.construirParametrosBase();
            parametros.put("nombreApellido", usuario.getPersonsa().getNombres());
            parametros.put("nombreUsuario", usuario.getUsername());
            parametros.put("claveTemporal", claveTemporal);

            List<String> destinatarios = new ArrayList<>();
            destinatarios.add(usuario.getCorreo());

            correoService.enviarNotificacion(destinatarios, plantillaCorreoRecuperarClave, parametros,
                    usuario.getId(), Constantes.getPathLogo());
        } catch (Exception e) {
            LOG.error("ERROR AL ENVIAR CORREO DE RECUPERACION", e);
        }
    }
}
