package ec.com.antenasur.dto;

import java.io.Serializable;

import ec.com.antenasur.model.tec.Documentos;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista de la entidad {@link Documentos}. Aplana la relación a
 * {@code TipoDocumento} en {@code tipoDocumentoId}. Excluye
 * {@code contenidoDocumento} (byte[]) — la vista no debe cargar binarios en
 * listados; eso se maneja por endpoint de descarga.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String nombre;
    private String path;
    private Integer tipoDocumentoId;
    private Integer entidadId;
    private String extension;
    private String mime;
    private String codigo;

    public static DocumentoDTO fromEntity(Documentos d) {
        if (d == null) {
            return null;
        }
        DocumentoDTO dto = new DocumentoDTO();
        dto.setId(d.getId());
        dto.setNombre(d.getNombre());
        dto.setPath(d.getPath());
        dto.setEntidadId(d.getEntidadId());
        dto.setExtension(d.getExtension());
        dto.setMime(d.getMime());
        dto.setCodigo(d.getCodigo());
        if (d.getTipoDocumento() != null) {
            dto.setTipoDocumentoId(d.getTipoDocumento().getId());
        }
        return dto;
    }
}
