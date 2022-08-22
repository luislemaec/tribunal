package ec.com.antenasur.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.Serializable;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.naming.InitialContext;

public class SendEmail implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String CONTENT_TYPE = "text/html; charset=utf-8";
    private static final String PASSWORD_MAIL = "lui$.l3m@"; // "Usuario01";

    /**
     * Enviar Correo a Varios Destinos en lista con adjunto
     *
     * @param emailsDestino List<String>
     * @param asunto String
     * @param mensaje String
     * @param pathAdjunto String
     * @param nombreAdjunto String
     * @return boolean
     */
    public static boolean correoAdjunto(List<String> emailsDestino, String asunto, String mensaje, String pathAdjunto)
            throws Exception {
        try {
            InitialContext ctx = new InitialContext();
            // Session session = (Session) ctx.lookup("jdniMail"); //Glassfish-payara SERVER
            Session session = (Session) ctx.lookup("java:jboss/mail/Rpm");

            mensaje = "<img src='cid:image'>" + mensaje;
            // Se compone la parte del texto
            BodyPart texto = new MimeBodyPart();
            texto.setContent(mensaje, CONTENT_TYPE);

            // Se compone el adjunto con la imagen
            BodyPart adjunto = new MimeBodyPart();
            adjunto.setDataHandler(new DataHandler(new FileDataSource(pathAdjunto)));
            adjunto.setHeader("Content-ID", "<image>");
            adjunto.setFileName("consejo-img");

            // Una MultiParte para agrupar texto e imagen.
            MimeMultipart multiParte = new MimeMultipart();
            multiParte.addBodyPart(texto);
            multiParte.addBodyPart(adjunto);

            MimeMessage message = new MimeMessage(session);

            // Se compone el correo, dando to, from, subject y el
            // contenido.
            // MimeMessage message = new MimeMessage(session);\
            message.setFrom(new InternetAddress(session.getProperties().getProperty("mail.from")));
            for (String email : emailsDestino) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
            }
            message.setSubject(asunto);
            message.setContent(multiParte);

            // Se envia el correo.
            Transport t = session.getTransport(session.getProperties().getProperty("mail.transport.protocol"));

            t.connect(session.getProperties().getProperty("mail.from"), PASSWORD_MAIL);// <=Contraseña del correo
            // registrado en Jboss
            message.saveChanges();
            t.sendMessage(message, message.getAllRecipients());
            t.close();
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Enviar Correo a Varios Destinos en lista con adjunto
     *
     * @param emailsDestino List<String>
     * @param asunto String
     * @param mensaje String
     * @param pathAdjunto String
     * @param nombreAdjunto String
     * @return boolean
     */
    public static boolean sendEmailListaMedios(List<String> emailsDestino, String asunto, String mensaje,
            String pathAdjunto, ByteArrayOutputStream baos) {
        try {
            InitialContext ctx = new InitialContext();

            Session session = (Session) ctx.lookup("java:jboss/mail/Rpm");

            // Se compone la parte del texto
            ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
            String phatLogo = externalContext.getRealPath("") + File.separator + "resources" + File.separator + "img"
                    + File.separator + "logo_consejo_417x150.png";

            mensaje = "<img src='cid:image'>" + mensaje;
            BodyPart texto = new MimeBodyPart();
            texto.setContent(mensaje, CONTENT_TYPE);

            BodyPart adjuntoLogo = new MimeBodyPart();
            adjuntoLogo.setDataHandler(new DataHandler(new FileDataSource(phatLogo)));
            adjuntoLogo.setHeader("Content-ID", "<image>");
            adjuntoLogo.setFileName("consejo-logo");

            // Se compone el adjunto con la imagen
            BodyPart adjunto = new MimeBodyPart();
            adjunto.setDataHandler(new DataHandler(new FileDataSource(pathAdjunto)));
            // adjunto.setHeader("Content-ID", "<image>");
            // adjunto.setFileName("consejo-logo");

            // Una MultiParte para agrupar texto e imagen.
            MimeMultipart multiParte = new MimeMultipart();
            // *************************************************************************/
            ByteArrayDataSource ds = new ByteArrayDataSource(baos.toByteArray(), "application/pdf");
            adjunto.setDataHandler(new DataHandler(ds));
            adjunto.setFileName("Lista_medios_seleccionados.pdf");
            // multiParte.addBodyPart(adjunto);
            // *************************************************************************/
            multiParte.addBodyPart(adjuntoLogo);
            multiParte.addBodyPart(texto);
            multiParte.addBodyPart(adjunto);

            MimeMessage message = new MimeMessage(session);

            // Se compone el correo, dando to, from, subject y el
            // contenido.
            // MimeMessage message = new MimeMessage(session);\
            message.setFrom(new InternetAddress(session.getProperties().getProperty("mail.from")));
            for (String email : emailsDestino) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
            }
            message.setSubject(asunto);
            message.setContent(multiParte);

            // Se envia el correo.
            Transport t = session.getTransport(session.getProperties().getProperty("mail.transport.protocol"));

            t.connect(session.getProperties().getProperty("mail.from"), PASSWORD_MAIL);// <=Contraseña del correo
            // registrado en Jboss
            message.saveChanges();
            t.sendMessage(message, message.getAllRecipients());
            t.close();
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }
}
