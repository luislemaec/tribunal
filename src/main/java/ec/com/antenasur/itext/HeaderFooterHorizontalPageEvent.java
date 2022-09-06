package ec.com.antenasur.itext;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;

public class HeaderFooterHorizontalPageEvent extends PdfPageEventHelper {

    public void onStartPage(PdfWriter writer, Document document) {
        try {

            ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
            String webRoot = servletContext.getRealPath("/");


            /*Agrega Banner cabecera al documento*/
            String pathBannerHeader = webRoot + "/resources/images/certificate/ENCABEZADO.png";
            Image imgHeader = Image.getInstance(pathBannerHeader);

            imgHeader.scaleToFit(PageSize.A4.rotate().getWidth(), PageSize.A4.rotate().getHeight());
            float xh = (PageSize.A4.rotate().getWidth() - imgHeader.getScaledWidth()) / 2;
            float yh = PageSize.A4.rotate().getHeight();
            imgHeader.setAbsolutePosition(xh, yh - (imgHeader.getScaledHeight()));


            /*Agrega logo al documentos*/
            String pathLogo = webRoot + "/resources/images/certificate/LOGO-CONSEJO.png";
            Image logo = Image.getInstance(pathLogo);
            logo.scaleToFit(150, 60);
            logo.setAlignment(Chunk.ALIGN_CENTER);

            /*Agrega borrador*/
            String pathBorrador = webRoot + "/resources/images/certificate/FONDO.png";
            Image borrador = Image.getInstance(pathBorrador);
            borrador.scaleToFit(PageSize.A4.rotate().getWidth(), PageSize.A4.rotate().getHeight());
            float x = (PageSize.A4.rotate().getWidth() - borrador.getScaledWidth()) / 2;
            float y = (PageSize.A4.rotate().getHeight() - borrador.getScaledHeight()) / 2;
            borrador.setAbsolutePosition(x, y);
            document.add(borrador);
            document.add(logo);
            document.add(imgHeader);

        } catch (Exception e) {
            Logger.getLogger(HeaderFooterHorizontalPageEvent.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public void onEndPage(PdfWriter writer, Document document) {

        try {

            ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
            String webRoot = servletContext.getRealPath("/");
            String pathBannerFooter = webRoot + "/resources/images/certificate/PIE.png";

            Image imgFooter = Image.getInstance(pathBannerFooter);

            imgFooter.scaleToFit(PageSize.A4.rotate().getWidth(), PageSize.A4.rotate().getHeight());
            float x = (PageSize.A4.rotate().getWidth() - imgFooter.getScaledWidth()) / 2;
            float y = (PageSize.A4.rotate().getHeight() - imgFooter.getScaledHeight()) / 2;
            imgFooter.setAbsolutePosition(x, -0);

            document.add(imgFooter);

        } catch (Exception e) {
            Logger.getLogger(HeaderFooterHorizontalPageEvent.class.getName()).log(Level.SEVERE, null, e);
        }
    }
}
