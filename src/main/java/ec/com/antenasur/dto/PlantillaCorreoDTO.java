package ec.com.antenasur.dto;

import java.io.Serializable;

import ec.com.antenasur.model.tec.PlantillaCorreo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Vista de la entidad {@link PlantillaCorreo}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaCorreoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String asunto;
    private String mensaje;
    private String descripcion;

    public static PlantillaCorreoDTO fromEntity(PlantillaCorreo p) {
        if (p == null) {
            return null;
        }
        PlantillaCorreoDTO dto = new PlantillaCorreoDTO();
        dto.setId(p.getId());
        dto.setAsunto(p.getAsunto());
        dto.setMensaje(p.getMensaje());
        dto.setDescripcion(p.getDescripcion());
        return dto;
    }

    public PlantillaCorreo toEntity() {
        PlantillaCorreo p = new PlantillaCorreo();
        p.setId(this.id);
        p.setAsunto(this.asunto);
        p.setMensaje(this.mensaje);
        p.setDescripcion(this.descripcion);
        return p;
    }
}
