package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.Date;

import ec.com.antenasur.model.tec.Periodo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Vista de la entidad {@link Periodo}: período electoral. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PeriodoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String nombre;
    private String descripcion;
    private Date fechaInicio;
    private Date fechaFin;

    public static PeriodoDTO fromEntity(Periodo p) {
        if (p == null) {
            return null;
        }
        PeriodoDTO dto = new PeriodoDTO();
        dto.setId(p.getId());
        dto.setNombre(p.getNombre());
        dto.setDescripcion(p.getDescripcion());
        dto.setFechaInicio(p.getFechaInicio());
        dto.setFechaFin(p.getFechaFin());
        return dto;
    }

    public Periodo toEntity() {
        Periodo p = new Periodo();
        p.setId(this.id);
        p.setNombre(this.nombre);
        p.setDescripcion(this.descripcion);
        p.setFechaInicio(this.fechaInicio);
        p.setFechaFin(this.fechaFin);
        return p;
    }
}
