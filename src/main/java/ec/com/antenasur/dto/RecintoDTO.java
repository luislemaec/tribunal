package ec.com.antenasur.dto;

import java.io.Serializable;

import ec.com.antenasur.enums.EstadoTarea;
import ec.com.antenasur.model.tec.Recinto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista de la entidad {@link Recinto} para la capa UI. Aplana
 * {@code Geograp ubicacion} en {@code ubicacionId} + {@code ubicacionNombre}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecintoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String nombre;
    private Integer ubicacionId;
    private String ubicacionNombre;
    private Integer cantonId;
    private String cantonNombre;
    private EstadoTarea estadoTarea;

    public static RecintoDTO fromEntity(Recinto r) {
        if (r == null) {
            return null;
        }
        RecintoDTO dto = new RecintoDTO();
        dto.setId(r.getId());
        dto.setNombre(r.getNombre());
        dto.setEstadoTarea(r.getEstadoTarea());
        if (r.getUbicacion() != null) {
            dto.setUbicacionId(r.getUbicacion().getId());
            dto.setUbicacionNombre(r.getUbicacion().getName());
            if (r.getUbicacion().getGeograp() != null) {
                dto.setCantonId(r.getUbicacion().getGeograp().getId());
                dto.setCantonNombre(r.getUbicacion().getGeograp().getName());
            }
        }
        return dto;
    }

    /**
     * Construye un {@link Recinto} con campos editables. La {@code ubicacion}
     * la resuelve el caller a partir de {@link #ubicacionId}.
     */
    public Recinto toEntity() {
        Recinto r = new Recinto();
        r.setId(this.id);
        r.setNombre(this.nombre);
        r.setEstadoTarea(this.estadoTarea);
        return r;
    }
}
