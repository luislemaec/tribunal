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
    private static final Logger LOG = LoggerFactory.getLogger(OlvidoClaveController.class);

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
    private String username, correo;

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
            UsuarioService.SolicitudRecuperacionClave solicitud = usuarioService.iniciarRecuperacionClave(username, correo);
            if (solicitud != null) {
                usuario = solicitud.usuario();
                sendMailRecoveryPassword(solicitud.token());
            }
            // El nombre procede exclusivamente de una solicitud validada; no
            // se registra el correo, token ni valores libres del formulario.
            procesoBean.registraSolicitudRecuperacionClave(
                    solicitud != null && solicitud.usuario() != null ? solicitud.usuario().getUsername() : null);
            JsfUtil.redirect("/recuperaClaveCorrecto.jsf");
        } catch (Exception e) {
            LOG.error("ERROR AL RECUPERAR CONTRASEÃƒ‘A", e);
            JsfUtil.addErrorMessage("Problemas al restablecer la contrasenia");
        }
    }

    private void sendMailRecoveryPassword(String token) {
        try {
            HashMap<String, String> parametros = correoService.construirParametrosBase();
            parametros.put("nombreApellido", usuario.getPersonsa().getNombres());
            parametros.put("nombreUsuario", usuario.getUsername());
            parametros.put("enlaceRecuperacion", JsfUtil.getRecoveryPublicBaseUrl() + "/restablecerClave.jsf?token="
                    + java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8));

            List<String> destinatarios = new ArrayList<>();
            destinatarios.add(usuario.getCorreo());

            correoService.enviarNotificacion(destinatarios, plantillaCorreoRecuperarClave, parametros,
                    usuario.getId(), Constantes.getPathLogo());
        } catch (Exception e) {
            LOG.error("ERROR AL ENVIAR CORREO DE RECUPERACION", e);
        }
    }
}
