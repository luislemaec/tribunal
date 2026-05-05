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

public class HeaderFooterPageEvent extends PdfPageEventHelper {

    public void onStartPage(PdfWriter writer, Document document) {
        try {

            ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
            String webRoot = servletContext.getRealPath("/");

            /*Agrega Banner cabecera al documento*/
            String pathBannerHeader = webRoot + "/resources/img/bannerHeader.png";
            Image bannerHeader = Image.getInstance(pathBannerHeader);
            bannerHeader.scaleToFit(600, 40);
            bannerHeader.setAbsolutePosition(0, 820);
            document.add(bannerHeader);

            /*Agrega logo al documentos*/
            String pathLogo = webRoot + "/resources/img/logo_consejo_417x150.png";
            Image logo = Image.getInstance(pathLogo);
            logo.scaleToFit(150, 60);
            logo.setAlignment(Chunk.ALIGN_CENTER);
            document.add(logo);

            String ipServidor = JsfUtil.obtieneIpServidor();
            if (!ipServidor.equals(Constantes.getProduccionServer())) {
                /*Agrega borrador*/
                String pathBorrador = webRoot + "/resources/img/BORRRADOR.png";
                Image borrador = Image.getInstance(pathBorrador);
                borrador.scaleToFit(PageSize.A4.getWidth(), PageSize.A4.getHeight());
                float x = (PageSize.A4.getWidth() - borrador.getScaledWidth()) / 2;
                float y = (PageSize.A4.getHeight() - borrador.getScaledHeight()) / 2;
                borrador.setAbsolutePosition(x, y);
                document.add(borrador);
            }

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

        } catch (Exception e) {
            Logger.getLogger(HeaderFooterPageEvent.class.getName()).log(Level.SEVERE, null, e);
        }
    }
}
