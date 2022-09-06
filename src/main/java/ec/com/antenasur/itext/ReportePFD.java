package ec.com.antenasur.itext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.text.SimpleDateFormat;
import java.util.Date;
import javax.faces.context.ExternalContext;
import javax.servlet.ServletContext;

/**
 *
 * Genera documentos PDF
 *
 */
public class ReportePFD {

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(ReportePFD.class);

    private static ByteArrayOutputStream baos;

    private static Document document;

    private static PdfWriter writer;

    private static ExternalContext externalContext;

    private static String PATH_LOGO;

    private static PdfPTable table;

    private static void inicializa() {
        try {
            externalContext = FacesContext.getCurrentInstance().getExternalContext();
            ServletContext servletContext = (ServletContext) externalContext.getContext();
            String webRoot = servletContext.getRealPath("/");

            /*Agrega Banner cabecera al documento*/
            PATH_LOGO = webRoot + "/resources/img/logo_consejo_417x150.png";
        } catch (Exception e) {
            LOG.error("ERROR AL INICIALIZAR VALORES" + e);
        }
    }

    public static void nuevoPDF(String nombreReporte) {
        try {
            inicializa();
            document = new Document(PageSize.A4);
            baos = new ByteArrayOutputStream();

            writer = PdfWriter.getInstance(document, baos);
            HeaderFooterPageEvent event = new HeaderFooterPageEvent();
            writer.setPageEvent(event);
            document.open();
            setMetadataDocument(document, nombreReporte);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void nuevoPDFHorizontal(String nombreReporte) {
        try {
            inicializa();
            document = new Document(PageSize.A4.rotate());
            baos = new ByteArrayOutputStream();

            writer = PdfWriter.getInstance(document, baos);
            HeaderFooterPageEvent event = new HeaderFooterPageEvent();
            writer.setPageEvent(event);
            document.open();
            setMetadataDocument(document, nombreReporte);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void creaTablaCabecera(int numColumns, float[] columWidth, String tableTitle,
            String[] listColumNames) {
        try {
            table = new PdfPTable(numColumns);
            //table.setLockedWidth(true);
            table.setWidthPercentage(100);
            addTableToDocument(numColumns, columWidth, tableTitle, listColumNames);

        } catch (Exception e) {
        }
    }

    public static void setMetadataDocument(Document document, String nombreReporte) {
        try {
            document.addAuthor("Luis Lema");
            document.addCreator("Antena Sur");
            document.addTitle(nombreReporte);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void addTableToDocument(int numColumns, float[] columWidth, String tableTitle,
            String[] listColumNames) {
        try {
            table.setTotalWidth(columWidth);
            addTableHeader(numColumns, tableTitle, "arial", 7, Font.BOLD, BaseColor.BLACK);

            for (String columName : listColumNames) {
                table.addCell(new PdfPCell(
                        new Paragraph(columName, FontFactory.getFont("arial", 7, Font.BOLD, BaseColor.BLACK))));
            }

        } catch (DocumentException ex) {
            Logger.getLogger(ReportePFD.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void creaContenidoTabla(String[][] listaDatos, String[] listColumnNames, int sizeFont) {
        try {
            for (String[] medio : listaDatos) {
                for (int i = 0; i < medio.length; i++) {
                    table.addCell(new PdfPCell(new Paragraph(medio[i], FontFactory.getFont("arial", (sizeFont > 0 ? sizeFont : 6), Font.NORMAL, BaseColor.BLACK))));
                }
            }
            document.add(table);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("ERROR AL CREAR CONTENIDO DE TABLA" + e);
        }
    }

    public static void addTableHeader(int numColumns, String tableTitle, String fontname, float size,
            int style, BaseColor color) {

        PdfPCell cell = new PdfPCell(new Paragraph(tableTitle, FontFactory.getFont(fontname, size, style, color)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(new BaseColor(187, 222, 251));
        cell.setColspan(numColumns);
        table.addCell(cell);
    }

    public static void addImagen(String rutaImagen, float fitWidth, float fitHeight, int alignment, Document document)
            throws DocumentException {
        try {
            Image foto = Image.getInstance(rutaImagen);
            foto.scaleToFit(fitWidth, fitHeight);
            foto.setAlignment(alignment);
            document.add(foto);
        } catch (BadElementException | IOException ex) {
            Logger.getLogger(ReportePFD.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void addParagraph(Document document, String string) {
        try {
            document.add(new Paragraph(string));
        } catch (DocumentException ex) {
            Logger.getLogger(ReportePFD.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void descargarPDF(ByteArrayOutputStream baos, String nombreReporte) {
        try {
            HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
            OutputStream out = response.getOutputStream();
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=\"" + nombreReporte + ".pdf\"");
            response.setDateHeader("Expires", 0);

            try {
                baos.writeTo(out);
                out.flush();
            } catch (IOException ex) {
                ex.getStackTrace();
                Logger.getLogger(ReportePFD.class.getName()).log(Level.SEVERE, null, ex);
            }
            out.flush();
            FacesContext.getCurrentInstance().responseComplete();
        } catch (Exception e) {
            e.getStackTrace();
            Logger.getLogger(ReportePFD.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public static void descargarPDF(String nombreReporte) {
        try {
            HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();
            OutputStream out = response.getOutputStream();
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=\"" + nombreReporte + ".pdf\"");
            response.setDateHeader("Expires", 0);

            try {
                baos.writeTo(out);
                out.flush();
            } catch (IOException ex) {
                ex.getStackTrace();
                Logger.getLogger(ReportePFD.class.getName()).log(Level.SEVERE, null, ex);
            }
            out.flush();
            FacesContext.getCurrentInstance().responseComplete();
        } catch (Exception e) {
            e.getStackTrace();
            Logger.getLogger(ReportePFD.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public static void getFinalParagraph(String nombreUsuario) {
        try {
            String date = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
            String finalParagraph = "\t Documento generado por: " + nombreUsuario + " Fecha: " + date.substring(0, 10)
                    + " Hora: " + date.substring(11, 19);

            Paragraph parrafo = new Paragraph(finalParagraph,
                    FontFactory.getFont("arial", 8, Font.ITALIC, BaseColor.BLACK));
            parrafo.setAlignment(Element.ALIGN_RIGHT);
            document.add(parrafo);
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void getWhiteParagraph() {
        try {
            Paragraph parrafo = new Paragraph("\n",
                    FontFactory.getFont("arial", 8, Font.ITALIC, BaseColor.BLACK));
            parrafo.setAlignment(Element.ALIGN_RIGHT);
            document.add(parrafo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
