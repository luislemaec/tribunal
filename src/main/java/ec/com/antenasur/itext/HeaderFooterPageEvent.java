package ec.com.antenasur.itext;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.faces.context.FacesContext;
import jakarta.servlet.ServletContext;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import ec.com.antenasur.util.Constantes;

public class HeaderFooterPageEvent extends PdfPageEventHelper {

    private static final BaseColor COLOR_INSTITUCIONAL = new BaseColor(24, 82, 133);
    private static final BaseColor COLOR_TEXTO_SECUNDARIO = new BaseColor(90, 100, 110);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final String codigoDocumento;
    private final String tituloDocumento;
    private final LocalDateTime fechaGeneracion;
    private final String procesoElectoral;
    private final boolean actaParcial;

    public HeaderFooterPageEvent() {
        this("", "Documento electoral", LocalDateTime.now());
    }

    public HeaderFooterPageEvent(String codigoDocumento, String tituloDocumento,
            LocalDateTime fechaGeneracion) {
        this(codigoDocumento, tituloDocumento, fechaGeneracion, null, false);
    }

    private HeaderFooterPageEvent(String codigoDocumento, String tituloDocumento,
            LocalDateTime fechaGeneracion, String procesoElectoral, boolean actaParcial) {
        this.codigoDocumento = codigoDocumento != null ? codigoDocumento : "";
        this.tituloDocumento = tituloDocumento != null && !tituloDocumento.isBlank()
                ? tituloDocumento : "Documento electoral";
        this.fechaGeneracion = fechaGeneracion != null ? fechaGeneracion : LocalDateTime.now();
        this.procesoElectoral = procesoElectoral != null ? procesoElectoral : "";
        this.actaParcial = actaParcial;
    }

    public static HeaderFooterPageEvent paraActaParcial(String tituloDocumento,
            String procesoElectoral, LocalDateTime fechaGeneracion) {
        return new HeaderFooterPageEvent("", tituloDocumento, fechaGeneracion, procesoElectoral, true);
    }

    public void onStartPage(PdfWriter writer, Document document) {
        try {

            ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
            String webRoot = servletContext.getRealPath("/");
            Rectangle pageSize = document.getPageSize();

            /*Agrega Banner cabecera al documento*/
            String pathBannerHeader = webRoot + "/resources/img/bannerHeader.png";
            Image bannerHeader = Image.getInstance(pathBannerHeader);
            bannerHeader.scaleToFit(pageSize.getWidth(), 34);
            bannerHeader.setAbsolutePosition(0, pageSize.getHeight() - 32);
            document.add(bannerHeader);

            /*Agrega logo al documentos*/
            String pathLogo = webRoot + "/resources/img/logo_consejo_417x150.png";
            Image logo = Image.getInstance(pathLogo);
            logo.scaleToFit(actaParcial ? 104 : 128, actaParcial ? 38 : 46);

            PdfPTable cabecera = new PdfPTable(2);
            cabecera.setTotalWidth(document.right() - document.left());
            cabecera.setWidths(new float[]{actaParcial ? 20 : 28, actaParcial ? 80 : 72});
            cabecera.setLockedWidth(true);

            PdfPCell celdaLogo = new PdfPCell(logo, false);
            celdaLogo.setBorder(Rectangle.NO_BORDER);
            celdaLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);
            celdaLogo.setPaddingTop(actaParcial ? 4f : 8f);
            cabecera.addCell(celdaLogo);

            Font titulo = FontFactory.getFont("arial", actaParcial ? 13 : 11, Font.BOLD, COLOR_INSTITUCIONAL);
            Font institucion = FontFactory.getFont("arial", actaParcial ? 10 : 11, Font.BOLD, COLOR_INSTITUCIONAL);
            Font texto = FontFactory.getFont("arial", 8, Font.NORMAL, COLOR_TEXTO_SECUNDARIO);
            Phrase datos = new Phrase();
            datos.add(new Chunk(Constantes.INSTITUCION + "\n", actaParcial ? institucion : titulo));
            datos.add(new Chunk(Constantes.SISTEMA + "\n", texto));
            datos.add(new Chunk(tituloDocumento + "\n", actaParcial ? titulo : texto));
            if (actaParcial) {
                datos.add(new Chunk(procesoElectoral, texto));
            } else {
                datos.add(new Chunk("Codigo: " + codigoDocumento + " | "
                        + fechaGeneracion.format(FORMATO_FECHA), texto));
            }
            PdfPCell celdaTexto = new PdfPCell(datos);
            celdaTexto.setBorder(Rectangle.NO_BORDER);
            celdaTexto.setHorizontalAlignment(actaParcial ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
            celdaTexto.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cabecera.addCell(celdaTexto);

            cabecera.writeSelectedRows(0, -1, document.left(), pageSize.getHeight() - (actaParcial ? 38 : 44), writer.getDirectContent());

        } catch (Exception e) {
            Logger.getLogger(HeaderFooterPageEvent.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public void onEndPage(PdfWriter writer, Document document) {

        try {

            ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
            String webRoot = servletContext.getRealPath("/");
            String pathBannerFooter = webRoot + "/resources/img/bannerFooter.png";

            Image bannerFooter = Image.getInstance(pathBannerFooter);
            bannerFooter.scaleToFit(600, 40);
            bannerFooter.setAbsolutePosition(0, -5);

            document.add(bannerFooter);

            Font footerFont = FontFactory.getFont("arial", 7, Font.NORMAL, COLOR_TEXTO_SECUNDARIO);
            String textoFooter = actaParcial
                    ? Constantes.getMensaje("reportesMesa.acta.pie", writer.getPageNumber())
                    : "Pagina " + writer.getPageNumber()
                    + " | Codigo de validacion: " + codigoDocumento
                    + " | Documento generado electronicamente por el Sistema TEC";
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER,
                    new Phrase(textoFooter, footerFont),
                    (document.right() + document.left()) / 2,
                    document.bottom() - 18,
                    0);

        } catch (Exception e) {
            Logger.getLogger(HeaderFooterPageEvent.class.getName()).log(Level.SEVERE, null, e);
        }
    }

}
