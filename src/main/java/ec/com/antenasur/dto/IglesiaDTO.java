package ec.com.antenasur.dto;

import java.io.Serializable;

import ec.com.antenasur.model.Iglesia;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista de la entidad {@link Iglesia} para la capa UI. Aplana la relación con
 * {@code Geograp ubicacion} en {@code ubicacionId} + {@code ubicacionNombre}
 * para evitar exponer la entidad geográfica completa. Mantiene el flag
 * transitorio {@code tieneDocumentos} (no persistido) que la vista usa para
 * mostrar el ícono de documentos cargados.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IglesiaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String nombre;
    private String comunidad;
    private Integer totalMiembros;
    private String documento;
    /** id de la parroquia (Geograp). */
    private Integer ubicacionId;
    /** nombre de la parroquia. */
    private String ubicacionNombre;
    /** id del cantón (padre de la parroquia). Derivado en {@link #fromEntity}. */
    private Integer cantonId;
    /** nombre del cantón. Derivado en {@link #fromEntity}. */
    private String cantonNombre;
    /** Versión para control de edición concurrente. Nunca editable por el usuario. */
    private Long version;
    private Boolean tieneDocumentos;

    public static IglesiaDTO fromEntity(Iglesia i) {
        if (i == null) {
            return null;
        }
        IglesiaDTO dto = new IglesiaDTO();
        dto.setId(i.getId());
        dto.setNombre(i.getNombre());
        dto.setComunidad(i.getComunidad());
        dto.setTotalMiembros(i.getTotalMiembros());
        dto.setDocumento(i.getDocumento());
        dto.setTieneDocumentos(i.getTieneDocumentos());
        dto.setVersion(i.getVersion());
        if (i.getUbicacion() != null) {
            dto.setUbicacionId(i.getUbicacion().getId());
            dto.setUbicacionNombre(i.getUbicacion().getName());
            if (i.getUbicacion().getGeograp() != null) {
                dto.setCantonId(i.getUbicacion().getGeograp().getId());
                dto.setCantonNombre(i.getUbicacion().getGeograp().getName());
            }
        }
        return dto;
    }

    /**
     * Construye una {@link Iglesia} con los campos editables. La
     * {@code ubicacion} no se setea aquí — el caller debe resolverla por
     * {@link #ubicacionId} y asignarla antes de persistir, para evitar
     * referencias colgadas.
     */
    public Iglesia toEntity() {
        Iglesia i = new Iglesia();
        i.setId(this.id);
        i.setNombre(this.nombre);
        i.setComunidad(this.comunidad);
        i.setTotalMiembros(this.totalMiembros);
        i.setDocumento(this.documento);
        i.setTieneDocumentos(this.tieneDocumentos);
        return i;
    }
}
