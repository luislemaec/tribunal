/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

/**
 *
 * @author luis lema
 */
public class ReportePFD {

    public static void nuevoPDF(Document document, ByteArrayOutputStream baos, String nombreReporte) {
        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            HeaderFooterPageEvent event = new HeaderFooterPageEvent();
            writer.setPageEvent(event);
            document.open();
            setMetadataDocument(document, nombreReporte);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setMetadataDocument(Document document, String nombreReporte) {
        try {

            document.addAuthor("Consejo de Comunicación");
            document.addCreator("Consejo de Comunicación");
            document.addTitle(nombreReporte);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void getFinalParagraph(Document document, String userName) {
        try {
            String date = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
            String finalParagraph = "\t Documento generado por: " + userName + " Fecha: " + date.substring(0, 10)
                    + " Hora: " + date.substring(11, 19);

            Paragraph parrafo = new Paragraph(finalParagraph,
                    FontFactory.getFont("arial", 8, Font.ITALIC, BaseColor.BLACK));
            parrafo.setAlignment(Element.ALIGN_RIGHT);
            document.add(parrafo);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void addTableToDocument(PdfPTable table, int numColumns, float[] columWidth, String tableTitle, String[] listColumNames) {
        try {
            table.setTotalWidth(columWidth);
            addTableHeader(table, numColumns, tableTitle, "arial", 7, Font.BOLD, BaseColor.BLACK);

            for (String columName : listColumNames) {
                table.addCell(new PdfPCell(new Paragraph(columName.toUpperCase(), FontFactory.getFont("arial", 7, Font.BOLD, BaseColor.BLACK))));
            }

        } catch (DocumentException ex) {
            Logger.getLogger(ReportePFD.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void addTableHeader(PdfPTable table, int numColumns, String tableTitle, String fontname, float size, int style, BaseColor color) {

        PdfPCell cell = new PdfPCell(new Paragraph(tableTitle, FontFactory.getFont(fontname, size, style, color)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(BaseColor.GRAY);
        cell.setColspan(numColumns);
        table.addCell(cell);
    }

    public static void addImagen(String rutaImagen, float fitWidth, float fitHeight, int alignment, Document document) throws DocumentException {
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
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment;filename=\"" + nombreReporte + ".pdf\"");
            response.setDateHeader("Expires", 0);

            try {
                baos.writeTo(out);
                out.flush();
            } catch (IOException ex) {
                Logger.getLogger(ReportePFD.class.getName()).log(Level.SEVERE, null, ex);
                ex.printStackTrace();
            }
            out.flush();
            FacesContext.getCurrentInstance().responseComplete();
        } catch (Exception e) {
            Logger.getLogger(ReportePFD.class.getName()).log(Level.SEVERE, null, e);
        }
    }
}
