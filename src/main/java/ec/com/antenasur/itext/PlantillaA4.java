package ec.com.antenasur.itext;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.Barcode;
import com.itextpdf.text.pdf.Barcode128;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * Plantilla institucional A4 ({@code /img/A4TEC.png}) y zona segura de
 * impresión común a todos los documentos PDF del sistema.
 *
 * <p>La plantilla mide 2480 × 3508 px a 300 ppp, es decir A4 exacto
 * (595,28 × 841,89 pt). Sus elementos gráficos fijan el área utilizable:
 *
 * <pre>
 *   banda azul izquierda      x    0 –  209 px  →    0 –  50,2 pt
 *   línea gris vertical       x  239 –  249 px  → 57,4 –  59,8 pt
 *   líneas horizontales       x  480 – 2288 px  → 115,2 – 549,1 pt
 *   línea bajo el encabezado  y  577 –  583 px  → 138,5 – 140,0 pt desde arriba
 *   línea del pie             y 3372 – 3378 px  → 32,6 pt desde abajo
 *   texto del pie             hasta y 3428 px   → 19,2 pt desde abajo
 * </pre>
 *
 * <p>Los márgenes se alinean con las líneas horizontales de la plantilla, de
 * modo que el contenido nunca invade la banda azul, el logotipo, el bloque
 * «DOCUMENTO OFICIAL» ni el pie. Son los únicos valores de márgenes A4 del
 * sistema: {@link PdfInstitucional} los reutiliza en lugar de repetirlos.
 */
public final class PlantillaA4 {

    /** Ruta de la plantilla en el classpath (viaja dentro del WAR). */
    private static final String RUTA_PLANTILLA = "/img/A4TEC.png";

    /**
     * Margen izquierdo: justo después de la línea gris vertical (57,4 – 59,8 pt),
     * que delimita el área de contenido de la plantilla. Aprovecha todo el ancho
     * disponible sin invadir la banda azul ni la propia línea.
     */
    public static final float MARGEN_IZQUIERDO = 60f;
    /** Margen derecho: 595,28 pt de ancho A4 menos el fin de esas líneas (549,1 pt). */
    public static final float MARGEN_DERECHO = 46.2f;
    /**
     * Margen superior: la línea del encabezado está a 140 pt del borde y los
     * 16 pt siguientes alojan el título y el código del documento.
     */
    public static final float MARGEN_SUPERIOR = 156f;
    /**
     * Margen inferior: la línea del pie está a 32,6 pt del borde. El contenido
     * arranca en 48 pt y entre ambos queda la franja donde se imprime la
     * paginación.
     */
    public static final float MARGEN_INFERIOR = 48f;

    /** Línea base del texto de paginación, entre la línea del pie y el contenido. */
    private static final float LINEA_PAGINACION = 37f;

    /** Línea base del título y los datos del documento, bajo la línea del encabezado. */
    private static final float LINEA_DATOS = 688f;

    /**
     * Código de barras en el encabezado: se apoya sobre el bloque «DOCUMENTO
     * OFICIAL» de la plantilla, cuyo texto empieza a 748,9 pt.
     */
    private static final float ANCHO_BARRAS = 160f;
    private static final float ALTO_BARRAS = 26f;
    private static final float BASE_BARRAS = 756f;

    /** Borde inferior de la línea del encabezado de la plantilla, desde abajo. */
    public static final float LIMITE_ENCABEZADO = 701.9f;

    /** Borde superior de la línea del pie de la plantilla, desde abajo. */
    public static final float LIMITE_PIE = 32.6f;

    /** Ancho utilizable para tablas e imágenes. */
    public static final float ANCHO_UTIL = PageSize.A4.getWidth() - MARGEN_IZQUIERDO - MARGEN_DERECHO;
    /** Alto utilizable en una página. */
    public static final float ALTO_UTIL = PageSize.A4.getHeight() - MARGEN_SUPERIOR - MARGEN_INFERIOR;

    private static final BaseColor COLOR_TEXTO = new BaseColor(70, 82, 95);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** La imagen se lee una sola vez por JVM: es la misma en todas las páginas. */
    private static volatile byte[] plantilla;

    private PlantillaA4() {
    }

    /**
     * Bytes de la plantilla. Se leen del classpath, por lo que funciona también
     * fuera de una petición web (tareas programadas, pruebas), a diferencia de
     * los recursos que dependen de {@code FacesContext}.
     */
    private static byte[] bytesPlantilla() throws IOException {
        byte[] copia = plantilla;
        if (copia == null) {
            synchronized (PlantillaA4.class) {
                copia = plantilla;
                if (copia == null) {
                    try (InputStream entrada = PlantillaA4.class.getResourceAsStream(RUTA_PLANTILLA)) {
                        if (entrada == null) {
                            throw new IOException("No se encontró la plantilla institucional " + RUTA_PLANTILLA);
                        }
                        copia = entrada.readAllBytes();
                        plantilla = copia;
                    }
                }
            }
        }
        return copia;
    }

    /**
     * Evento que dibuja la plantilla como fondo de cada página y los datos de
     * identificación del documento.
     *
     * <p>El fondo se escribe en la capa inferior ({@code getDirectContentUnder})
     * al cerrar cada página: no consume espacio del documento ni altera el flujo
     * del contenido, y nunca tapa lo ya escrito.
     */
    public static final class Fondo extends PdfPageEventHelper {

        private final String codigoDocumento;
        private final String tituloDocumento;
        private final String subtitulo;
        private final LocalDateTime fechaGeneracion;
        /** Código de barras opcional, impreso sobre el bloque «DOCUMENTO OFICIAL». */
        private final String codigoBarras;
        /** Imagen de fondo reutilizada en todas las páginas del documento. */
        private Image fondo;
        private Image barras;

        public Fondo(String codigoDocumento, String tituloDocumento, String subtitulo,
                LocalDateTime fechaGeneracion) {
            this(codigoDocumento, tituloDocumento, subtitulo, fechaGeneracion, null);
        }

        public Fondo(String codigoDocumento, String tituloDocumento, String subtitulo,
                LocalDateTime fechaGeneracion, String codigoBarras) {
            this.codigoDocumento = codigoDocumento;
            this.tituloDocumento = tituloDocumento;
            this.subtitulo = subtitulo;
            this.fechaGeneracion = fechaGeneracion;
            this.codigoBarras = codigoBarras;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                dibujarFondo(writer);
                dibujarCodigoBarras(writer);
                dibujarDatosDocumento(writer, document);
                dibujarPaginacion(writer, document);
            } catch (Exception e) {
                // Un fallo de maquetación no debe impedir la emisión del
                // documento: el contenido funcional ya está escrito.
                java.util.logging.Logger.getLogger(PlantillaA4.class.getName())
                        .log(java.util.logging.Level.SEVERE, "No se pudo aplicar la plantilla institucional", e);
            }
        }

        private void dibujarFondo(PdfWriter writer) throws Exception {
            if (fondo == null) {
                fondo = Image.getInstance(bytesPlantilla());
                // Escalado proporcional exacto al tamaño de página A4.
                fondo.scaleAbsolute(PageSize.A4.getWidth(), PageSize.A4.getHeight());
                fondo.setAbsolutePosition(0, 0);
            }
            // La misma instancia en todas las páginas: iText incrusta la imagen
            // una sola vez y las demás páginas referencian ese objeto, de modo
            // que el peso del PDF no crece con el número de páginas.
            writer.getDirectContentUnder().addImage(fondo);
        }

        /**
         * Código de barras del documento sobre el bloque «DOCUMENTO OFICIAL».
         *
         * <p>Esa franja de la plantilla (a partir de 360 pt de ancho y por
         * encima de 749 pt de alto) está libre de gráfica, de modo que el
         * código queda en el encabezado, sin restar espacio al contenido ni
         * taparse con el logotipo.
         */
        private void dibujarCodigoBarras(PdfWriter writer) {
            if (codigoBarras == null || codigoBarras.isBlank()) {
                return;
            }
            if (barras == null) {
                Barcode128 codigo = new Barcode128();
                codigo.setCodeType(Barcode.CODE128);
                codigo.setCode(codigoBarras);
                codigo.setBarHeight(22f);
                codigo.setX(0.9f);
                codigo.setFont(null);
                barras = codigo.createImageWithBarcode(writer.getDirectContent(), BaseColor.BLACK, BaseColor.BLACK);
                barras.scaleToFit(ANCHO_BARRAS, ALTO_BARRAS);
            }
            float x = PageSize.A4.getWidth() - MARGEN_DERECHO - barras.getScaledWidth();
            try {
                barras.setAbsolutePosition(x, BASE_BARRAS);
                writer.getDirectContent().addImage(barras);
            } catch (Exception e) {
                java.util.logging.Logger.getLogger(PlantillaA4.class.getName())
                        .log(java.util.logging.Level.SEVERE, "No se pudo imprimir el código de barras", e);
            }
        }

        /**
         * Título, código y fecha bajo la línea del encabezado de la plantilla,
         * dentro de la zona segura. Sustituye al logotipo y al banner que antes
         * dibujaba el código, ya presentes en la plantilla.
         */
        private void dibujarDatosDocumento(PdfWriter writer, Document document) {
            PdfContentByte lienzo = writer.getDirectContent();
            Font fuenteTitulo = FontFactory.getFont("arial", 9.5f, Font.BOLD, COLOR_TEXTO);
            Font fuenteDatos = FontFactory.getFont("arial", 7.5f, Font.NORMAL, COLOR_TEXTO);
            float baseTitulo = LINEA_DATOS;

            if (tituloDocumento != null && !tituloDocumento.isBlank()) {
                ColumnText.showTextAligned(lienzo, Element.ALIGN_LEFT,
                        new Phrase(tituloDocumento, fuenteTitulo), MARGEN_IZQUIERDO, baseTitulo, 0);
            }
            String datos = subtitulo != null && !subtitulo.isBlank() ? subtitulo : construirDatos();
            if (datos != null && !datos.isBlank()) {
                ColumnText.showTextAligned(lienzo, Element.ALIGN_RIGHT,
                        new Phrase(datos, fuenteDatos), PageSize.A4.getWidth() - MARGEN_DERECHO, baseTitulo, 0);
            }
        }

        private String construirDatos() {
            StringBuilder datos = new StringBuilder();
            if (codigoDocumento != null && !codigoDocumento.isBlank()) {
                datos.append("Código: ").append(codigoDocumento);
            }
            if (fechaGeneracion != null) {
                if (datos.length() > 0) {
                    datos.append("  |  ");
                }
                datos.append(fechaGeneracion.format(FORMATO_FECHA));
            }
            return datos.toString();
        }

        /** Paginación sobre la línea del pie, sin invadir el texto de la plantilla. */
        private void dibujarPaginacion(PdfWriter writer, Document document) {
            Font fuente = FontFactory.getFont("arial", 7, Font.NORMAL, COLOR_TEXTO);
            String texto = "Página " + writer.getPageNumber();
            if (codigoDocumento != null && !codigoDocumento.isBlank()) {
                texto = texto + "  |  Código de validación: " + codigoDocumento;
            }
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase(texto, fuente), PageSize.A4.getWidth() - MARGEN_DERECHO, LINEA_PAGINACION, 0);
        }
    }
}
