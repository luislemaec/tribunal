package ec.com.antenasur.bean;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import ec.com.antenasur.model.tec.Documentos;
import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.dto.DocumentoDTO;
import ec.com.antenasur.service.tec.DocumentoService;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.JsfUtil;
import ec.com.antenasur.util.RepositorioDocumentos;
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

    @Inject
    private DocumentoService documentoService;

    @Inject
    private ProcesoBean procesoBean;

    @Setter
    @Getter
    private Documentos documento;

    @Setter
    @Getter
    private List<Documentos> documentos;

    @Setter
    private StreamedContent file;

    public StreamedContent getFile() {
        file = null;
        try {
            file = construirDescarga(documento);
            return file;
        } catch (IOException e) {
            notificarErrorDescarga(e);
            return null;
        }
    }

    /** Construye la descarga directamente desde el documento de la fila seleccionada. */
    public StreamedContent obtenerArchivo(Documentos item) {
        documento = item;
        try {
            file = construirDescarga(item);
            return file;
        } catch (IOException e) {
            notificarErrorDescarga(e);
            return null;
        }
    }

    public StreamedContent obtenerArchivo(DocumentoDTO item) {
        if (item == null || item.getId() == null) {
            documento = null;
            notificarErrorDescarga(new IOException(Constantes.getMensaje("documentos.error.not.selected")));
            return null;
        }
        return obtenerArchivo(documentoService.find(item.getId()));
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
            guardarDocumentoPersistido(documento);
        } catch (Exception e) {
            log.error("ERROR GUARDAR DOCUMENTO", e);
        }
    }

    public Documentos guardarDocumentoPersistido(Documentos documento) {
        if (documento == null) {
            return null;
        }
        return documentoService.create(documento);
    }

    public Documentos guardarDocumentoMesa(Documentos documento, Integer mesaId,
            Integer procesoId, Integer recintoId) {
        return documentoService.registrarDocumentoMesa(documento, mesaId, procesoId, recintoId);
    }

    /**
     * Descarga y visualiza a una buena ventana
     *
     * @throws IOException
     */
    public void descargarArchivoDirectorio() throws IOException {
        file = construirDescarga(documento);
    }

    private StreamedContent construirDescarga(Documentos item) throws IOException {
        if (item == null || item.getNombre() == null) {
            throw new IOException(Constantes.getMensaje("documentos.error.not.selected"));
        }
        Path path = RepositorioDocumentos.resolverRutaAlmacenada(item.getPath());
        StreamedContent descarga = DefaultStreamedContent.builder()
                .contentType(tipoMime(item, path))
                .name(nombreDescarga(item))
                .contentLength(Files.size(path))
                .stream(() -> abrirStream(path))
                .build();
        procesoBean.okActivityRegister("DESCARGA DOCUMENTO " + item.getNombre(), item.getPath());
        return descarga;
    }

    private InputStream abrirStream(Path path) {
        try {
            return Files.newInputStream(path);
        } catch (IOException e) {
            log.error("ERROR ABRIR DOCUMENTO PARA DESCARGA {}", path, e);
            throw new UncheckedIOException(e);
        }
    }

    private String nombreDescarga(Documentos item) throws IOException {
        String nombre = item.getNombre();
        String extension = extensionDocumento(item);
        if (!extension.isBlank() && !nombre.toLowerCase().endsWith(extension.toLowerCase())) {
            nombre += extension;
        }
        return RepositorioDocumentos.nombreArchivoSeguro(nombre);
    }

    private String tipoMime(Documentos item, Path path) throws IOException {
        if (item.getMime() != null
                && item.getMime().matches("^[A-Za-z0-9!#$&^_.+-]+/[A-Za-z0-9!#$&^_.+-]+$")) {
            return item.getMime().toLowerCase();
        }
        String detectado = Files.probeContentType(path);
        return detectado != null ? detectado : "application/octet-stream";
    }

    private String extensionDocumento(Documentos item) {
        return item.getExtension() != null ? item.getExtension() : "";
    }

    private void notificarErrorDescarga(Exception e) {
        String nombre = documento != null && documento.getNombre() != null
                ? documento.getNombre() : "";
        log.error("ERROR DESCARGAR DOCUMENTO {}", nombre, e);
        JsfUtil.addErrorMessage(Constantes.getMensaje("documentos.error.download", nombre));
    }

}
