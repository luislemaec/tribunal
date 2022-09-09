package ec.com.antenasur.util;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

/**
 *
 * @author LEMAEDU
 */
public class Constantes {

    /*NOTIFICACIONES*/
    public static final String INSTITUCION = "Tribunal electoral de CONPOCIIECH";
    public static final String SISTEMA = "SISTEMA TEC";

    /*ESTADO DE PROCESO*/
    public static final String ESTADO_PROCESO_NO_INICIADO = "NO INICIADO";
    public static final String ESTADO_TAREA_INICIADA = "EN CURSO";
    public static final String ESTADO_PROCESO_ABORTADO = "ABORTADO";
    public static final String ESTADO_TAREA_COMPLETADA = "COMPLETADO";

    /**
     * Retorna pirma del correo
     */
    public static final String FIRMA_CORREO = "<div><em>Saludos cordiales,</em><br />"
            + "<h5>" + SISTEMA + "</h5>"
            + "<h5>" + INSTITUCION + "</h5>"
            + "</div>"
            + "<p><strong>IMPORTANTE:</strong> <span style=color: #333333;><em>El env&iacute;o de este correo es autom&aacute;tico, por favor no lo responda. </em></span><br /><span style=color: #333333;><em>Si tiene alguna inquietud puede contactarse al correo electr&oacute;nico: rpm.medios@consejodecomunicacion.gob.ec o al tel&eacute;fono: (02) 3938720 ext. 2233</em></span></p>";

    /**
     * @author Luis Lema Retorna el path de la imagen para enviar al correo
     *
     */
    public static final String getPathLogo() {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        return externalContext.getRealPath("") + File.separator + "resources" + File.separator + "img"
                + File.separator + "logo_consejo_417x150.png";

    }

    public static final String getMontserratRegular() {
        ExternalContext externalContext = JsfUtil.getExternalContext();
        return externalContext.getRealPath("") + File.separator + "resources" + File.separator + "fonts"
                + File.separator + "Montserrat-Regular.ttf";
    }

    public static final String getHojaEstilo() {
        return "p {font-size: 10pt; margin-top: 1em; margin-bottom: 1em; font-family: montsR;}"
                + "h1{font-size: 37pt; font-family: montsB; color: #185285; margin-bottom: 1.2em;}"
                + "#url{font-size: 18pt; font-family: montsSB; color: #36A9E1;}" //url                                                               
                + "h3{font-size: 13pt; font-family: montsB;}"//CODIGO, CARGO-INSTITUCION
                + "h4{font-size: 13pt; font-family: montsR; margin-top:6em}";//AUTORIDAD
    }

    /**
     *
     * @param value
     * @return
     */
    private static String loadFromExternalProperties(String value) {
        try {
            String realAdd = System.getProperty("jboss.home.dir") + "/standalone/configuration/rpm-catalogos.properties";
            Properties properties = new Properties();
            properties.load(new FileInputStream(realAdd));
            return properties.get(value).toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static String getPruebasServer() {
        return loadFromExternalProperties("rpm.server.pruebas");
    }

    public static String getProduccionServer() {
        return loadFromExternalProperties("rpm.server.produccion");
    }

    public static String getRolSuperadministrador() {
        return "Superadministrador";
    }

    public static String getRolAdministrador() {
        return "Administrador";
    }

    public static String getRolPresidente() {
        return "Presidente";
    }

    public static String getRolVicepresidente() {
        return "vicepresidente";
    }

    public static String getRolSecretario() {
        return "Secretario";
    }

    public static String getRolTesorero() {
        return "Tesorero";
    }

    public static String getRolIglesias() {
        return "Iglesia";
    }

    public static String getRolPresidenteMesa() {
        return "Presidente-mesa";
    }

    public static String getRolTesoreosMesa() {
        return "Tesorero-mesa";
    }

    public static String getRolSecretarioMesa() {
        return "Secretario-mesa";
    }

}
