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

    /* CONSTANTES para estado */
    public static final boolean ACTIVO = true;
    public static final boolean INACTIVO = false;


    /*NOTIFICACIONES*/
    public static final String INSTITUCION = "Consejo de Regulación, Desarrollo y Promoción de la Información y Comunicación";
    public static final String SISTEMA = "SISTEMA TEC";

    /**
     * Retorna pirma del correo
     */
    public static final String FIRMA_CORREO = "<div><em>Saludos cordiales,</em><br />"
            + "<h5>" + SISTEMA + "</h5>"
            + "<h5>" + INSTITUCION + "</h5>"
            + "</div>"
            + "<p><strong>IMPORTANTE:</strong> <span style=color: #333333;><em>El env&iacute;o de este correo es autom&aacute;tico, por favor no lo responda. </em></span><br /><span style=color: #333333;><em>Si tiene alguna inquietud puede contactarse al correo electr&oacute;nico: rpm.medios@consejodecomunicacion.gob.ec o al tel&eacute;fono: (02) 3938720 ext. 2233</em></span></p>"
            + "<p>&nbsp;</p>";

    /**
     * @author Luis Lema Retorna el path de la imagen para enviar al correo
     *
     */
    public static final String getUrlAdjunto() {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        return externalContext.getRealPath("") + File.separator + "resources" + File.separator + "img"
                + File.separator + "logo_consejo_417x150.png";

    }

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

    public static String getFuntionalAdministrator() {
        return "Administrador - Funcional";
    }

    public static String getAdministrator() {
        return "Administrador";
    }

    public static String getAutoridad() {
        return "Autoridad";
    }

}
