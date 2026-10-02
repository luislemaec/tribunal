package ec.com.antenasur.itext;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.faces.context.FacesContext;
import jakarta.servlet.ServletContext;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Image;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import ec.com.antenasur.util.Constantes;

/**
 * Encabezado y pie de los reportes A4 apaisados (no existe versión horizontal de la plantilla A4).
 *
 * <p>Las franjas de color del encabezado y del pie se dibujan como rectángulos vectoriales con los colores
 * medidos de los antiguos {@code bannerHeader.png} y {@code bannerFooter.png}, con la misma geometría con
 * la que se escalaban. El logotipo, que no tiene versión vectorial, se carga una vez por documento.
 */
public class HeaderFooterPageEvent extends PdfPageEventHelper {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Franjas del encabezado: {desde, hasta} en píxeles del antiguo banner (773 × 38 px), escalado a
     * 34 pt de alto y anclado 32 pt bajo el borde superior.
     */
    private static final float[][] FRANJAS_ENCABEZADO = {{0, 388}, {388, 586}, {586, 773}};
    private static final BaseColor[] COLORES_ENCABEZADO = {
            new BaseColor(0x00, 0x9e, 0xdd), new BaseColor(0x7a, 0xb8, 0xe6), new BaseColor(0x29, 0x98, 0xc9)};
    private static final float ESCALA_ENCABEZADO = 34f / 38f;
    /** Franjas del pie: antiguo banner de 773 × 43 px escalado a 600 pt de ancho, 5 pt bajo el borde inferior. */
    private static final float[][] FRANJAS_PIE = {{0, 402}, {402, 773}};
    private static final BaseColor[] COLORES_PIE = {new BaseColor(0xed, 0xed, 0xed), new BaseColor(0xf6, 0xf6, 0xf6)};
    private static final float ESCALA_PIE = 600f / 773f;

    private final String codigoDocumento;
    private final String tituloDocumento;
    private final LocalDateTime fechaGeneracion;

    /** Logotipo cargado una sola vez por documento (este evento se crea por documento). */
    private Image logo;

    public HeaderFooterPageEvent() {
        this("", "Documento electoral", LocalDateTime.now());
    }

    public HeaderFooterPageEvent(String codigoDocumento, String tituloDocumento,
            LocalDateTime fechaGeneracion) {
        this.codigoDocumento = codigoDocumento != null ? codigoDocumento : "";
        this.tituloDocumento = tituloDocumento != null && !tituloDocumento.isBlank()
                ? tituloDocumento : "Documento electoral";
        this.fechaGeneracion = fechaGeneracion != null ? fechaGeneracion : LocalDateTime.now();
    }

    private Image logo() throws Exception {
        if (logo == null) {
            ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
            logo = Image.getInstance(servletContext.getRealPath("/") + "/resources/img/logo_consejo_417x150.png");
        }
        return logo;
    }

    @Override
    public void onStartPage(PdfWriter writer, Document document) {
        try {
            Rectangle pageSize = document.getPageSize();
            franjas(writer.getDirectContentUnder(), FRANJAS_ENCABEZADO, COLORES_ENCABEZADO, ESCALA_ENCABEZADO,
                    pageSize.getHeight() - 32, 38);

            Image imagenLogo = logo();
            imagenLogo.scaleToFit(128, 46);

            PdfPTable cabecera = new PdfPTable(2);
            cabecera.setTotalWidth(document.right() - document.left());
            cabecera.setWidths(new float[]{28, 72});
            cabecera.setLockedWidth(true);

            PdfPCell celdaLogo = new PdfPCell(imagenLogo, false);
            celdaLogo.setBorder(Rectangle.NO_BORDER);
            celdaLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);
            celdaLogo.setPaddingTop(8f);
            cabecera.addCell(celdaLogo);

            // Institución (500) → sistema (metadato) → tipo de documento (500) → código y fecha (metadato).
            Phrase datos = new Phrase();
            datos.add(new Chunk(Constantes.INSTITUCION + "\n", TipografiaPdf.Estilo.INSTITUCION.fuente(11)));
            datos.add(new Chunk(Constantes.SISTEMA + "\n", TipografiaPdf.Estilo.METADATO.fuente(8)));
            datos.add(new Chunk(tituloDocumento + "\n", TipografiaPdf.Estilo.SECCION.fuente(8)));
            datos.add(new Chunk("Codigo: " + codigoDocumento + " | " + fechaGeneracion.format(FORMATO_FECHA),
                    TipografiaPdf.Estilo.METADATO.fuente(8)));
            PdfPCell celdaTexto = new PdfPCell(datos);
            celdaTexto.setBorder(Rectangle.NO_BORDER);
            celdaTexto.setHorizontalAlignment(Element.ALIGN_RIGHT);
            celdaTexto.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cabecera.addCell(celdaTexto);

            cabecera.writeSelectedRows(0, -1, document.left(), pageSize.getHeight() - 44, writer.getDirectContent());

        } catch (Exception e) {
            Logger.getLogger(HeaderFooterPageEvent.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        try {
            franjas(writer.getDirectContentUnder(), FRANJAS_PIE, COLORES_PIE, ESCALA_PIE, -5, 43);
            String textoFooter = "Pagina " + writer.getPageNumber()
                    + " | Codigo de validacion: " + codigoDocumento
                    + " | Documento generado electronicamente por el Sistema TEC";
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER,
                    new Phrase(textoFooter, TipografiaPdf.Estilo.PIE.fuente(TipografiaPdf.TAM_PIE)),
                    (document.right() + document.left()) / 2,
                    document.bottom() - 18,
                    0);
        } catch (Exception e) {
            Logger.getLogger(HeaderFooterPageEvent.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /** Franjas verticales de color, con la escala y posición del banner raster al que sustituyen. */
    private static void franjas(PdfContentByte lienzo, float[][] franjas, BaseColor[] colores, float escala,
            float y, float altoPx) {
        lienzo.saveState();
        for (int i = 0; i < franjas.length; i++) {
            lienzo.setColorFill(colores[i]);
            lienzo.rectangle(franjas[i][0] * escala, y, (franjas[i][1] - franjas[i][0]) * escala, altoPx * escala);
            lienzo.fill();
        }
        lienzo.restoreState();
    }
}
