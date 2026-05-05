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
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.JsfUtil;

/**
 * Clase agrega cabecera y pie de página a documentos generados con iText
 *
 * @author Luis Lema <lemaedu@gmail.com> / Consejo de Comunicacación
 * @version 1.0.0 / 30-11-2021
 *
 */
public class HeaderFooterPageEventDraft extends PdfPageEventHelper {

    public void onStartPage(PdfWriter writer, Document document) {
        try {

            ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
            String webRoot = servletContext.getRealPath("/");

            /*Agrega Banner cabecera al documento*/
            String pathBannerHeader = webRoot + "/resources/images/agreement/bannerHeader.png";
            Image bannerHeader = Image.getInstance(pathBannerHeader);
            bannerHeader.scaleToFit(600, 40);
            bannerHeader.setAbsolutePosition(0, 820);
            document.add(bannerHeader);

            /*Agrega logo a los documentos*/
            String pathLogo = webRoot + "/resources/images/agreement/logo_certificate_417x150.png";
            Image logo = Image.getInstance(pathLogo);
            logo.scaleToFit(150, 60);
            logo.setAlignment(Chunk.ALIGN_CENTER);
            document.add(logo);

            /**
             * Agrega imagen borrador
             */
            String ipServidor = JsfUtil.obtieneIpServidor();
            if (!ipServidor.equals(Constantes.getProduccionServer())) {
                String pathBorrador = webRoot + "/resources/images/BORRRADOR.png";
                Image imgBorrador = Image.getInstance(pathBorrador);
                imgBorrador.scaleToFit(PageSize.A4.getWidth(), PageSize.A4.getHeight());
                float x = (PageSize.A4.getWidth() - imgBorrador.getScaledWidth()) / 2;
                float y = (PageSize.A4.getHeight() - imgBorrador.getScaledHeight()) / 2;
                imgBorrador.setAbsolutePosition(x, y);
                document.add(imgBorrador);
            }
        } catch (Exception e) {
            Logger.getLogger(HeaderFooterPageEventDraft.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /**
     *
     * @param writer
     * @param document
     */
    public void onEndPage(PdfWriter writer, Document document) {
        try {

            ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
            String webRoot = servletContext.getRealPath("/");
            String pathBannerFooter = webRoot + "/resources/images/agreement/bannerFooter.png";

            Image bannerFooter = Image.getInstance(pathBannerFooter);
            bannerFooter.scaleToFit(600, 40);
            bannerFooter.setAbsolutePosition(0, -5);

            document.add(bannerFooter);

        } catch (Exception e) {
            Logger.getLogger(HeaderFooterPageEventDraft.class.getName()).log(Level.SEVERE, null, e);
        }
    }
}
