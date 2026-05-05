package ec.com.antenasur.dto;

import java.io.Serializable;

import ec.com.antenasur.enums.EstadoTarea;
import ec.com.antenasur.model.tec.Mesa;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista de la entidad {@link Mesa} para la capa UI. Embebe el
 * {@link RecintoDTO} (la vista típicamente muestra el nombre del recinto
 * junto al de la mesa) y aplana {@code Geograp ubicacion} en id + nombre.
 * Mantiene los campos de cuadre de votos calculados al cerrar el acta y el
 * flag transitorio {@code tieneDocumentos}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MesaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String nombre;
    private RecintoDTO recinto;
    private Integer ubicacionId;
    private String ubicacionNombre;
    private Integer cantonId;
    private String cantonNombre;
    private EstadoTarea estadoTarea;
    private Integer totalVotos;
    private Integer totalPapetelasUso;
    private Integer totalAusentismo;
    private Boolean tieneErrorConteo;
    private String observacion;
    private String responsable;
    private Boolean tieneDocumentos;

    public static MesaDTO fromEntity(Mesa m) {
        if (m == null) {
            return null;
        }
        MesaDTO dto = new MesaDTO();
        dto.setId(m.getId());
        dto.setNombre(m.getNombre());
        dto.setRecinto(RecintoDTO.fromEntity(m.getRecinto()));
        dto.setEstadoTarea(m.getEstadoTarea());
        dto.setTotalVotos(m.getTotalVotos());
        dto.setTotalPapetelasUso(m.getTotalPapetelasUso());
        dto.setTotalAusentismo(m.getTotalAusentismo());
        dto.setTieneErrorConteo(m.getTieneErrorConteo());
        dto.setObservacion(m.getObservacion());
        dto.setResponsable(m.getResponsable());
        dto.setTieneDocumentos(m.getTieneDocumentos());
        if (m.getUbicacion() != null) {
            dto.setUbicacionId(m.getUbicacion().getId());
            dto.setUbicacionNombre(m.getUbicacion().getName());
            if (m.getUbicacion().getGeograp() != null) {
                dto.setCantonId(m.getUbicacion().getGeograp().getId());
                dto.setCantonNombre(m.getUbicacion().getGeograp().getName());
            }
        }
        return dto;
    }

    /**
     * Construye una {@link Mesa} con los campos editables. Las relaciones
     * ({@code recinto}, {@code ubicacion}) deben resolverse en el caller a
     * partir de {@link #recinto}.id y {@link #ubicacionId}.
     */
    public Mesa toEntity() {
        Mesa m = new Mesa();
        m.setId(this.id);
        m.setNombre(this.nombre);
        m.setEstadoTarea(this.estadoTarea);
        m.setTotalVotos(this.totalVotos);
        m.setTotalPapetelasUso(this.totalPapetelasUso);
        m.setTotalAusentismo(this.totalAusentismo);
        m.setTieneErrorConteo(this.tieneErrorConteo);
        m.setObservacion(this.observacion);
        m.setResponsable(this.responsable);
        m.setTieneDocumentos(this.tieneDocumentos);
        return m;
    }
}
