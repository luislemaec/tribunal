package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.Date;

import ec.com.antenasur.model.tec.ProcesoElectoral;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcesoElectoralDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String nombre;
    private String descripcion;
    private Date fechaInicio;
    private Date fechaFin;
    private Boolean activo;

    public static ProcesoElectoralDTO fromEntity(ProcesoElectoral p) {
        if (p == null) return null;
        ProcesoElectoralDTO dto = new ProcesoElectoralDTO();
        dto.setId(p.getId());
        dto.setNombre(p.getNombre());
        dto.setDescripcion(p.getDescripcion());
        dto.setFechaInicio(p.getFechaInicio());
        dto.setFechaFin(p.getFechaFin());
        dto.setActivo(p.getActivo());
        return dto;
    }

    public ProcesoElectoral toEntity() {
        ProcesoElectoral p = new ProcesoElectoral();
        p.setId(this.id);
        p.setNombre(this.nombre);
        p.setDescripcion(this.descripcion);
        p.setFechaInicio(this.fechaInicio);
        p.setFechaFin(this.fechaFin);
        p.setActivo(this.activo);
        return p;
    }
}
