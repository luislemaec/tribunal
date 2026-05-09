package ec.com.antenasur.bean;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.compress.utils.IOUtils;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import ec.com.antenasur.model.tec.Documentos;
import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.service.tec.DocumentoService;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Named(value = "documentoBean")
@RequestScoped
@Slf4j
public class DocumentoBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String PATH_DESTINO = System.getProperty("java.io.tmpdir") + File.separator;

    @Inject
    private DocumentoService documentoService;

    @Setter
    @Getter
    private Documentos documento;

    @Setter
    @Getter
    private List<Documentos> documentos;

    @Setter
    private StreamedContent file;

    public StreamedContent getFile() {
        try {
            descargarArchivoDirectorio();
            return file;
        } catch (IOException ex) {
            Logger.getLogger(DocumentoBean.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public List<Documentos> getDocumentoPorMesa(Mesa mesa) {
        return documentoService.getDocumentosPorMesa(mesa);
    }

    public List<Documentos> getDocumentosPorEntidadYTipoDoc(int entidadId, int tipoDocId) {
        return documentoService.getDocumentosPorEntidadYTipoDoc(entidadId, tipoDocId);
    }

    public boolean getTieneDocumentosPorEntidadYTipoDoc(int entidadId, int tipoDocId) {
        return documentoService.getTieneDocumentosPorEntidadYTipoDoc(entidadId, tipoDocId);
    }

    public void guardarDocumento(Documentos documento) {
        try {
            if (documento != null) {
                documento = documentoService.create(documento);
            }
        } catch (Exception e) {
        }
    }

    public void descargaDocumento() throws IOException {
        try {
            HttpServletResponse response = JsfUtil.getHttpServletResponse();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (documento.getNombre() != null) {
                InputStream inp = new FileInputStream(documento.getPath());
                OutputStream out = response.getOutputStream();
                response.setContentType("application/octet-stream");
                response.setHeader("Content-Disposition", "attachment;filename=\"" + documento.getNombre() + ".pdf\"");
                response.setDateHeader("Expires", 0);
                baos.writeTo(out);
                out.flush();
                FacesContext.getCurrentInstance().responseComplete();
            }
        } catch (Exception e) {
            log.error("ERROR DESCARGAR DOCUMENTO", e);
        }
    }

    /**
     * Descarga y visualiza a una buena ventana
     *
     * @throws IOException
     */
    public void descargarArchivoDirectorio() throws IOException {
        if (documento.getNombre() != null) {
            try {
                InputStream inp = new FileInputStream(documento.getPath());
                byte[] imageInByte = IOUtils.toByteArray(inp);
                file = DefaultStreamedContent.builder()
                        .contentType("application/octet-stream")
                        .name(documento.getNombre() + documento.getExtension())
                        .stream(() -> new ByteArrayInputStream(imageInByte))
                        .build();

            } catch (Exception e) {
                log.error("ERROR DESCARGAR DOCUMENTO", e);
            }
        }
    }

}
