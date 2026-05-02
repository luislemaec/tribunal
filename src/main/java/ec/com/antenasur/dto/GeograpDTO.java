package ec.com.antenasur.dto;

import java.io.Serializable;

import ec.com.antenasur.model.Geograp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista de la entidad {@link Geograp}: división geográfica jerárquica
 * (provincia/cantón/parroquia). Aplana la relación al padre en
 * {@code padreId} + {@code padreNombre}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeograpDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String name;
    private String codificationInec;
    private String observations;
    private Boolean status;
    private Integer pgloId;
    private Integer zoneId;
    private Integer padreId;
    private String padreNombre;

    public static GeograpDTO fromEntity(Geograp g) {
        if (g == null) {
            return null;
        }
        GeograpDTO dto = new GeograpDTO();
        dto.setId(g.getId());
        dto.setName(g.getName());
        dto.setCodificationInec(g.getCodificationInec());
        dto.setObservations(g.getObservations());
        dto.setStatus(g.getStatus());
        dto.setPgloId(g.getPgloId());
        dto.setZoneId(g.getZoneId());
        if (g.getGeograp() != null) {
            dto.setPadreId(g.getGeograp().getId());
            dto.setPadreNombre(g.getGeograp().getName());
        }
        return dto;
    }

    /**
     * Construye un {@link Geograp} con campos planos. La relación al padre
     * debe resolverse por {@link #padreId} en el caller.
     */
    public Geograp toEntity() {
        Geograp g = new Geograp();
        g.setId(this.id);
        g.setName(this.name);
        g.setCodificationInec(this.codificationInec);
        g.setObservations(this.observations);
        g.setStatus(this.status);
        g.setPgloId(this.pgloId);
        g.setZoneId(this.zoneId);
        return g;
    }
}
