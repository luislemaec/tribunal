package ec.com.antenasur.controller;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.bean.PlantillaCorreoBean;
import ec.com.antenasur.bean.ProcesoBean;
import ec.com.antenasur.domain.Usuario;
import ec.com.antenasur.domain.tec.Correo;
import ec.com.antenasur.domain.tec.PlantillaCorreo;
import ec.com.antenasur.itext.UtilHtml;
import ec.com.antenasur.service.UsuarioFacade;
import ec.com.antenasur.service.tec.CorreoFacade;
import ec.com.antenasur.util.Constantes;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import ec.com.antenasur.util.JsfUtil;
import ec.com.antenasur.util.SendEmail;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import javax.servlet.ServletException;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;

@Named
@RequestScoped
public class CambioClaveController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(CambioClaveController.class);

    @Inject
    LoginBean loginBean;

    @Inject
    ProcesoBean procesoBean;

    @Inject
    private UsuarioFacade usuarioFacade;

    @Inject
    private PlantillaCorreoBean plantillaCorreoBean;

    @Inject
    private CorreoFacade correoFacade;

    @Setter
    @Getter
    private String clave1, claveTemporal, clave2;

    @Setter
    @Getter
    private Usuario usuario;

    @Setter
    @Getter
    private PlantillaCorreo plantillaCorreoCambiaClave;

    @Setter
    @Getter
    private Correo correoEnviado;

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
            if (usuario != null) {
                if (!claveTemporal.isEmpty() && !clave1.isEmpty() && !clave2.isEmpty()) {
                    if (clave1.equals(clave2)) {
                        if (JsfUtil.validarContrasenia(clave1)) {
                            usuario.setContraseniaTemp(null);
                            usuario.setContrasenia(JsfUtil.claveEncriptadaSHA1(clave2));
                            usuario.setPermanente(true);
                            usuarioFacade.edit(usuario);
                            enviarCorreoCambioClave();
                            JsfUtil.addSuccessMessage("Cambio de clave exitoso");
                            //changed = true;
                            
                            procesoBean.registraActividad("CAMBIA CONTRASEÑA");
                            loginBean.passwordChangued();
                            JsfUtil.redirect("/recuperaClaveCorrecto.jsf");                            
                        }
                    } else {
                        JsfUtil.addFatalMessage("Las contraseñas no coinciden");
                    }
                }
            } else {
                JsfUtil.addWarningMessage("Usuario o contraseña incorrecta");
            }
        } catch (Exception e) {

            JsfUtil.addErrorMessage("Problemas al recuperar la contrasenia");
        }
    }

    /**
     *
     */
    private HashMap<String, String> getCompleteDataFromMedia() {
        try {
            HashMap<String, String> parameters = new HashMap<>();

            String date = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
            String hoy = date.substring(0, 10);
            String ahora = date.substring(11, 19);
            parameters.put("fechaRegistro", hoy);
            parameters.put("horaRegistro", ahora);
            parameters.put("nombreApellido", usuario.getPersonsa().getNombres());

            return parameters;
        } catch (Exception e) {
            LOG.error("ERROR EN INICIALIZAR VARIABLES", e);
            return null;
        }
    }

    private String processMessageHTML(String txtHTML, HashMap<String, String> parameters) {
        try {
            return UtilHtml.builTextHTMLToMail(parameters, txtHTML);
        } catch (Exception e) {
            LOG.error("ERROR AGREGAR PARAMETROS", e);
            return "";
        }
    }

    public void enviarCorreoCambioClave() {
        HashMap<String, String> parameters = getCompleteDataFromMedia();
        parameters.put("nombreUsuario", usuario.getUsername());
        parameters.put("clave", clave1);
        List<String> emailsDestino = new ArrayList<>();
        String pathAdjunto = Constantes.getPathLogo();
        try {
            emailsDestino.add(usuario.getCorreo());

            plantillaCorreoCambiaClave.setMensaje(processMessageHTML(plantillaCorreoCambiaClave.getMensaje(), parameters));
            saveNotification(emailsDestino.toString(), plantillaCorreoCambiaClave, parameters);

            SendEmail.correoAdjunto(emailsDestino, plantillaCorreoCambiaClave.getAsunto(),
                    plantillaCorreoCambiaClave.getMensaje(), pathAdjunto);
        } catch (Exception e) {
            LOG.error("ERROR AL ENVIAR CORREO DE RECUPERACION", e);
        }
    }

    /**
     *
     * @param emailsDestino lista de destinatarios a enviar
     * @param subjetc Asunto
     * @param message Texto enviado por correo
     */
    private void saveNotification(String emailsDestino, PlantillaCorreo plantilla, HashMap<String, String> parameters) {
        try {
            parameters.remove("clave");
            correoEnviado = new Correo(emailsDestino, usuario.getId(), plantilla, parameters.toString());
            correoFacade.create(correoEnviado);
        } catch (Exception e) {
            LOG.error("ERROR EN REGISTRAR NOTIFICACIONES", e);
        }
    }
}
