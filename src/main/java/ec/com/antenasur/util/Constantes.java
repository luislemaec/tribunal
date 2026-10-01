package ec.com.antenasur.util;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.pdf.BaseFont;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.text.MessageFormat;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;

/**
 *
 * @author LEMAEDU
 */
public class Constantes {

    private static final String BUNDLE_MESSAGES = "ec.com.antenasur.resources.messages_es";
    private static final String DIRECTORIO_DOCUMENTOS_DEFAULT = "/var/app/tec/documentos";

    /*NOTIFICACIONES*/
    public static final String INSTITUCION = "CONPOCIIECH";
    public static final String SISTEMA = "SISTEMA TEC";

    /*ESTADO DE PROCESO*/
    public static final String ESTADO_PROCESO_NO_INICIADO = "NO INICIADO";
    public static final String ESTADO_TAREA_INICIADA = "EN CURSO";
    public static final String ESTADO_PROCESO_ABORTADO = "ABORTADO";
    public static final String ESTADO_TAREA_COMPLETADA = "COMPLETADO";

    /*TIPOS DE DOCUMENTOS*/
    public static final Integer ACTA_ESCRUTINIO = 1;
    public static final Integer LISTA_MIEMBROS = 2;
    public static final String TIPO_ACTA_INSCRIPCION_GENERADA = "ACTA DE INSCRIPCION GENERADA";
    public static final String TIPO_ACTA_INSCRIPCION_FIRMADA = "ACTA DE INSCRIPCION FIRMADA";
    public static final String TIPO_ACTA_PARCIAL_ESCRUTINIO = "ACTA PARCIAL DE ESCRUTINIO";
    public static final String TIPO_PADRON_ELECTORAL_MESA = "PADRON ELECTORAL DE MESA";
    public static final String TIPO_ACTA_ACTUALIZACION_MIEMBROS = "ACTA DE ACTUALIZACION DE MIEMBROS";
    /**
     * Retorna pirma del correo
     */
    public static final String FIRMA_CORREO = "<div><em>Saludos cordiales,</em><br />"
            + "<h5>" + SISTEMA + "</h5>"
            + "<h5>" + INSTITUCION + "</h5>"
            + "</div>"
            + "<p><strong>IMPORTANTE:</strong> <span style=color: #333333;><em>El env&iacute;o de este correo es autom&aacute;tico, por favor no lo responda. </em></span><br /><span style=color: #333333;></span></p>";

    /**
     * @author Luis Lema Retorna el path de la imagen para enviar al correo
     *
     */
    public static final String getPathLogo() {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        return externalContext.getRealPath("") + File.separator + "resources" + File.separator + "img"
                + File.separator + "logo_consejo_417x150.png";

    }

    /*----------------------------------- FUENTE -----------------------------------*/
    public static Font getFuenteCabeceraDefault(final float tamanioLetra) {
        // Énfasis del documento = Medium (500), igual que la interfaz web (antes Bold).
        return fuentePdf(tamanioLetra, Font.BOLD, BaseColor.BLACK);
    }

    public static Font getFuenteContenidoDefault(final float tamanioLetra) {
        // Texto del documento = Light (300), igual que la interfaz web (antes Regular).
        return fuentePdf(tamanioLetra, Font.NORMAL, BaseColor.BLACK);
    }

    /**
     * Fuente de PDF institucional (Montserrat), igual que la interfaz: texto en Light (300), énfasis (negrita) en
     * Medium (500) y cursiva en Light Italic. Si no hay contexto web o falta el archivo (p. ej. pruebas) cae en
     * Helvetica para no interrumpir la generación del documento.
     */
    public static Font fuentePdf(final float tamanio, final int estilo, final BaseColor color) {
        final boolean enfasis = (estilo & Font.BOLD) != 0;
        final boolean cursiva = !enfasis && (estilo & Font.ITALIC) != 0;
        final String archivo = enfasis ? "Montserrat-Medium.ttf"
                : cursiva ? "Montserrat-LightItalic.ttf" : "Montserrat-Light.ttf";
        final String alias = archivo.replace(".ttf", "");
        try {
            if (!FontFactory.isRegistered(alias)) {
                final java.io.File fuente = new java.io.File(getPathFuenteExterna(archivo));
                if (!fuente.isFile()) {
                    throw new IllegalStateException("Fuente no disponible: " + archivo);
                }
                FontFactory.register(fuente.getPath(), alias);
            }
            // Incrustada (subconjunto): el PDF se ve igual en equipos sin Montserrat instalada.
            return FontFactory.getFont(alias, BaseFont.CP1252, BaseFont.EMBEDDED, tamanio, Font.NORMAL, color);
        } catch (RuntimeException e) {
            return FontFactory.getFont(enfasis ? FontFactory.HELVETICA_BOLD
                    : cursiva ? FontFactory.HELVETICA_OBLIQUE : FontFactory.HELVETICA, tamanio, color);
        }
    }

    public static Font getFuente(final String nombreFuenteConExtension, final float tamanioLetra, final int estiloFuente, final BaseColor color) {
        String aliasFuente = nombreFuenteConExtension.replaceAll("ttf", "");
        String pathFuente = getPathFuenteExterna(nombreFuenteConExtension);
        FontFactory.register(pathFuente, aliasFuente);
        return FontFactory.getFont(aliasFuente, tamanioLetra, estiloFuente, color);
    }

    public static final String getPathFuenteExterna(String nombreFuenteConExtension) {
        ExternalContext externalContext = JsfUtil.getExternalContext();
        return externalContext.getRealPath("") + File.separator + "resources" + File.separator + "fonts"
                + File.separator + nombreFuenteConExtension;
    }

    public static final String getPathArchivos() {
        ExternalContext externalContext = JsfUtil.getExternalContext();
        return externalContext.getRealPath("") + File.separator + "resources" + File.separator + "fonts"
                + File.separator;
    }

    /*----------------------------------- FIN FUENTE -----------------------------------*/
    public static final String getHojaEstilo() {
        return "p {font-size: 10pt; margin-top: 1em; margin-bottom: 1em; font-family: montsR; line-height: 1.5;}"
                + "h1{font-size: 20pt; font-family: montsB; color: #185285; margin-bottom: 1.2em;}"
                + "h3{font-size: 18pt; font-family: montsB;}"//CODIGO, CARGO-INSTITUCION
                + "h3{font-size: 16pt; font-family: montsB;}"//CODIGO, CARGO-INSTITUCION
                + "h4{font-size: 14pt; font-family: montsR;}"
                + "h5{font-size: 12pt; font-family: montsR;}"
                + "#url{font-size: 10pt; font-family: montsSB; color: #36A9E1;}" //url                                 
                + "table{width: 100%; border-collapse: collapse; border: 0px solid white; cellspacing:0; font-size: 10pt;}";//TABLAS
    }

    /**
     *
     * @param value
     * @return
     */
    private static String loadFromMessages(String value) {
        String systemValue = System.getProperty(value);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }
        try {
            String bundleValue = ResourceBundle.getBundle(BUNDLE_MESSAGES).getString(value);
            return bundleValue != null && !bundleValue.isBlank() ? bundleValue.trim() : null;
        } catch (MissingResourceException e) {
            return null;
        }
    }

    public static String getPruebasServer() {
        return loadFromMessages("rpm.server.pruebas");
    }

    public static String getProduccionServer() {
        return loadFromMessages("rpm.server.produccion");
    }

    public static String getDirectorioDocumentos() {
        String directorio = System.getProperty("rpm.files.path");
        if (directorio == null || directorio.isBlank()) {
            directorio = loadFromMessages("rpm.files.path");
        }
        if (directorio == null || directorio.isBlank()) {
            directorio = DIRECTORIO_DOCUMENTOS_DEFAULT;
        }
        return resolverPath(directorio);
    }

    public static String getDirectorioDocumentos(String subdirectorio) {
        if (subdirectorio == null || subdirectorio.isBlank()) {
            return getDirectorioDocumentos();
        }
        Path base = Paths.get(getDirectorioDocumentos()).toAbsolutePath().normalize();
        Path destino = base.resolve(subdirectorio.trim()).normalize();
        if (!destino.startsWith(base)) {
            throw new IllegalArgumentException("Subdirectorio fuera del repositorio institucional.");
        }
        return destino.toString();
    }

    public static String getDirectorioActasEscrutinio() {
        return getDirectorioDocumentos("actas-escrutinio");
    }

    public static String getPathActaEscrutinio(String nombreDocumento) {
        return resolverPath(getDirectorioActasEscrutinio(), nombreDocumento + ".pdf");
    }

    public static String getPathListaMiembros(String nombreDocumento, String extension) {
        return resolverPath(getDirectorioDocumentos("listas-miembros"), nombreDocumento + extension);
    }

    public static String getDirectorioActasInscripcionGeneradas() {
        return resolverPath(getDirectorioDocumentos("actas-inscripcion"), "generadas");
    }

    public static String getDirectorioActasInscripcionFirmadas() {
        return resolverPath(getDirectorioDocumentos("actas-inscripcion"), "firmadas");
    }

    public static String getLugarActaInscripcion() {
        String lugar = loadFromMessages("tec.actas.inscripcion.lugar");
        return lugar != null && !lugar.isBlank() ? lugar : INSTITUCION;
    }

    /**
     * Mínimo de miembros activos esperado en una iglesia. Por debajo de este
     * valor la gestión de iglesias marca el registro para que el administrador
     * lo revise; nunca bloquea ni elimina nada de forma automática.
     *
     * <p>Se configura como el resto de parámetros del sistema: propiedad JVM
     * {@code tec.iglesias.miembros.minimo} o la misma clave en
     * {@code messages_es.properties}. Si no está definida o no es un número
     * válido, se usa 2 (se alerta sobre iglesias con 0 o 1 miembro).
     */
    public static int getMinimoMiembrosIglesia() {
        String valor = loadFromMessages("tec.iglesias.miembros.minimo");
        if (valor != null) {
            try {
                return Integer.parseInt(valor.trim());
            } catch (NumberFormatException e) {
                // Configuración inválida: se mantiene el valor por defecto.
            }
        }
        return 2;
    }

    /**
     * Número mínimo de miembros registrados para generar el acta de actualización de
     * miembros, además de que todos estén actualizados. Configurable con
     * {@code tec.acta.actualizacion.miembros.minimo}; es independiente del mínimo que
     * usa el aviso de revisión del listado de iglesias.
     */
    public static int getMinimoMiembrosActaActualizacion() {
        String valor = loadFromMessages("tec.acta.actualizacion.miembros.minimo");
        if (valor != null) {
            try {
                return Integer.parseInt(valor.trim());
            } catch (NumberFormatException e) {
                // Configuración inválida: se mantiene el valor por defecto.
            }
        }
        return 3;
    }

    public static String getMensaje(String clave, Object... argumentos) {
        String mensaje = loadFromMessages(clave);
        if (mensaje == null) {
            return clave;
        }
        return argumentos == null || argumentos.length == 0
                ? mensaje : MessageFormat.format(mensaje, argumentos);
    }

    private static String resolverPath(String primerSegmento, String... segmentos) {
        if (primerSegmento == null || primerSegmento.isBlank()) {
            primerSegmento = System.getProperty("java.io.tmpdir");
        }
        Path path = Paths.get(primerSegmento.trim());
        if (segmentos != null) {
            for (String segmento : segmentos) {
                if (segmento != null && !segmento.isBlank()) {
                    path = path.resolve(segmento.trim());
                }
            }
        }
        return path.toAbsolutePath().normalize().toString();
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
    
    public static String getRolTecnico() {
        return "Tecnico";
    }

    public static String getRolIglesiaAdmin() {
        return "IglesiaAdmin";
    }

    /**
     * Rol del Tribunal Electoral. El cargo interno (Presidente, Secretario,
     * Vocal, Veedor) se distingue mediante {@code tb_tribunal} para evitar
     * explosión de roles RBAC.
     */
    public static String getRolTribunal() {
        return "Tribunal";
    }

    public static String getRolSecretarioMesa() {
        return "Secretario-mesa";
    }

}
