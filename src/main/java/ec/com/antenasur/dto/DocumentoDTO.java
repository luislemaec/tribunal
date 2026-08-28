package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.Date;

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
    private String tipoDocumentoNombre;
    private Integer entidadId;
    private String extension;
    private String mime;
    private String codigo;
    private String hashSha256;
    private String contextoHash;
    private Integer documentoOrigenId;
    private Integer procesoId;
    private Integer recintoId;
    private Integer mesaId;
    private Date fechaCrea;
    private String usuarioCrea;
    private Boolean disponible;

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
        dto.setHashSha256(d.getHashSha256());
        dto.setContextoHash(d.getContextoHash());
        dto.setFechaCrea(d.getFechaCrea());
        dto.setUsuarioCrea(d.getUsuarioCrea());
        if (d.getDocumentoOrigen() != null) {
            dto.setDocumentoOrigenId(d.getDocumentoOrigen().getId());
        }
        if (d.getTipoDocumento() != null) {
            dto.setTipoDocumentoId(d.getTipoDocumento().getId());
            dto.setTipoDocumentoNombre(d.getTipoDocumento().getNombre());
        }
        if (d.getProceso() != null) {
            dto.setProcesoId(d.getProceso().getId());
        }
        if (d.getRecinto() != null) {
            dto.setRecintoId(d.getRecinto().getId());
        }
        if (d.getMesa() != null) {
            dto.setMesaId(d.getMesa().getId());
        }
        return dto;
    }
}
