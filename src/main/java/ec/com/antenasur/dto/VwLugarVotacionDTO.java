package ec.com.antenasur.dto;

import java.io.Serializable;

import ec.com.antenasur.model.tec.VwLugarVotacion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista de la {@link VwLugarVotacion}: lugar de votación de una persona.
 * Solo lectura. Todos los campos son strings (la vista BD ya entrega los
 * datos aplanados).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VwLugarVotacionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String iglesia;
    private String comunidad;
    private String cedula;
    private String nombres;
    private String mesa;
    private String recinto;
    private String parroquia;
    private String canton;

    public static VwLugarVotacionDTO fromEntity(VwLugarVotacion v) {
        if (v == null) {
            return null;
        }
        VwLugarVotacionDTO dto = new VwLugarVotacionDTO();
        dto.setId(v.getId());
        dto.setIglesia(v.getIglesia());
        dto.setComunidad(v.getComunidad());
        dto.setCedula(v.getCedula());
        dto.setNombres(v.getNombres());
        dto.setMesa(v.getMesa());
        dto.setRecinto(v.getRecinto());
        dto.setParroquia(v.getParroquia());
        dto.setCanton(v.getCanton());
        return dto;
    }
}
