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
import com.itextpdf.text.FontProvider;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.JsfUtil;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.text.SimpleDateFormat;
import java.util.Date;

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

    private static String PATH_LOGO;

    private static PdfPTable table;

    private static XMLWorkerHelper worker;

    private static InputStream inputStream;

    private static Font fuente;

    private static void inicializa() {
        try {
            worker = XMLWorkerHelper.getInstance();
            /*Agrega Banner cabecera al documento*/
            PATH_LOGO = Constantes.getPathLogo();
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
            String[] listColumNames, Font fuente) {
        try {
            table = new PdfPTable(numColumns);
            //table.setLockedWidth(true);
            table.setWidthPercentage(100);
            addTableToDocument(numColumns, columWidth, tableTitle, listColumNames, fuente);

        } catch (Exception e) {
        }
    }

    public static void addTableHeader(int numColumns, String tableTitle, Font fuente) {

        PdfPCell cell = new PdfPCell(new Paragraph(tableTitle, fuente));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(new BaseColor(187, 222, 251));
        cell.setColspan(numColumns);
        table.addCell(cell);
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
            String[] listColumNames, Font fuente) {
        try {
            table.setTotalWidth(columWidth);
            addTableHeader(numColumns, tableTitle, fuente);

            for (String columName : listColumNames) {
                table.addCell(new PdfPCell(
                        new Paragraph(columName, fuente)));
            }
        } catch (DocumentException ex) {
            Logger.getLogger(ReportePFD.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void creaContenidoTabla(String[][] listaDatos, String[] listColumnNames, Font fuente) {
        try {
            for (String[] medio : listaDatos) {
                for (int i = 0; i < medio.length; i++) {
                    table.addCell(new PdfPCell(new Paragraph(medio[i], fuente)));
                }
            }
            document.add(table);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("ERROR AL CREAR CONTENIDO DE TABLA" + e);
        }
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

    public static void addParagraph(String string) {
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
            HttpServletResponse response = JsfUtil.getHttpServletResponse();
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

    public static void guardarDocumentosActasE(String nombreDocumento) {
        try {
            Path path = Paths.get("/opt/ACTASE/" + nombreDocumento + ".pdf");
            Files.write(path, baos.toByteArray());
        } catch (IOException e) {
            LOG.error("ERROR AL GUARDAR ARCHIVOS" + nombreDocumento, e);
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

    public static void agregaParrafoEnBlanco() {
        try {
            Paragraph parrafo = new Paragraph("\n",
                    FontFactory.getFont("arial", 8, Font.ITALIC, BaseColor.BLACK));
            parrafo.setAlignment(Element.ALIGN_RIGHT);
            document.add(parrafo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void agregaParrafoObservacion(String observacion) {
        try {
            Paragraph parrafo = new Paragraph("\n" + observacion,
                    FontFactory.getFont("arial", 8, Font.ITALIC, BaseColor.RED));
            parrafo.setAlignment(Element.ALIGN_LEFT);
            document.add(parrafo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void agregaHTML(String texto, String css, FontProvider fontProvider) {
        try {
            InputStream inputStreamCss = new ByteArrayInputStream(css.getBytes(("UTF-8")));
            inputStream = new ByteArrayInputStream(texto.getBytes(("UTF-8")));
            worker.parseXHtml(writer, document, inputStream, inputStreamCss, Charset.forName("UTF-8"), fontProvider);
        } catch (IOException ex) {
            Logger.getLogger(ReportePFD.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
