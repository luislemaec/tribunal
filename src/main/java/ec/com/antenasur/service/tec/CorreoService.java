package ec.com.antenasur.service.tec;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import ec.com.antenasur.facade.tec.CorreoFacade;
import ec.com.antenasur.itext.UtilHtml;
import ec.com.antenasur.model.tec.Correo;
import ec.com.antenasur.model.tec.PlantillaCorreo;
import ec.com.antenasur.service.AbstractService;
import ec.com.antenasur.util.SendEmail;

@Stateless
public class CorreoService extends AbstractService<Correo, Integer, CorreoFacade> {

    private static final Logger LOG = Logger.getLogger(CorreoService.class);

    @Inject
    private CorreoFacade correoFacade;

    @Override
    protected CorreoFacade getFacade() {
        return correoFacade;
    }

    /**
     * Construye el HashMap base que las plantillas suelen necesitar:
     * {@code fechaRegistro}, {@code horaRegistro}. Los callers añaden los
     * parámetros específicos (nombre, clave, etc.) sobre el resultado.
     */
    public HashMap<String, String> construirParametrosBase() {
        HashMap<String, String> parametros = new HashMap<>();
        String date = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
        parametros.put("fechaRegistro", date.substring(0, 10));
        parametros.put("horaRegistro", date.substring(11, 19));
        return parametros;
    }

    /**
     * Procesa la plantilla con los parámetros, envía el correo (con adjunto si
     * se indica path) y persiste el registro de notificación. Los parámetros
     * sensibles ({@code clave}) se eliminan antes de persistir.
     *
     * @param destinatarios lista de emails destino (no null/vacía)
     * @param plantilla plantilla a usar; su {@code mensaje} es modificado in-place
     * @param parametros sustituciones para la plantilla
     * @param usuarioId id del usuario asociado al envío
     * @param pathAdjunto path absoluto del adjunto, o null para sin adjunto
     * @return el {@code Correo} persistido, o null si no se pudo enviar
     */
    public Correo enviarNotificacion(List<String> destinatarios, PlantillaCorreo plantilla,
            HashMap<String, String> parametros, Integer usuarioId, String pathAdjunto) {
        if (destinatarios == null || destinatarios.isEmpty() || plantilla == null) {
            return null;
        }
        plantilla.setMensaje(UtilHtml.builTextHTMLToMail(parametros, plantilla.getMensaje()));

        HashMap<String, String> parametrosPersistencia = new HashMap<>(parametros);
        parametrosPersistencia.remove("clave");
        parametrosPersistencia.remove("claveTemporal");
        Correo registro = new Correo(destinatarios.toString(), usuarioId, plantilla,
                parametrosPersistencia.toString());
        Correo persistido = correoFacade.create(registro);

        try {
            SendEmail.correoAdjunto(destinatarios, plantilla.getAsunto(), plantilla.getMensaje(), pathAdjunto);
        } catch (Exception e) {
            LOG.error("ERROR AL ENVIAR CORREO " + plantilla.getAsunto(), e);
        }
        return persistido;
    }
}
