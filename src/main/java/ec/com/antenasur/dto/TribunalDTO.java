package ec.com.antenasur.dto;

import java.io.Serializable;

import ec.com.antenasur.model.tec.Tribunal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista de la entidad {@link Tribunal}. Embebe IglesiaPersona; aplana
 * periodo y cargo en pares id+nombre.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TribunalDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private IglesiaPersonaDTO iglesiaPersona;
    private Integer periodoId;
    private String periodoNombre;
    private Integer cargoId;
    private String cargoNombre;

    public static TribunalDTO fromEntity(Tribunal t) {
        if (t == null) {
            return null;
        }
        TribunalDTO dto = new TribunalDTO();
        dto.setId(t.getId());
        dto.setIglesiaPersona(IglesiaPersonaDTO.fromEntity(t.getIglesiaPersona()));
        if (t.getPeriodo() != null) {
            dto.setPeriodoId(t.getPeriodo().getId());
            dto.setPeriodoNombre(t.getPeriodo().getNombre());
        }
        if (t.getCargo() != null) {
            dto.setCargoId(t.getCargo().getId());
            dto.setCargoNombre(t.getCargo().getNombre());
        }
        return dto;
    }
}
