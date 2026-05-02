package ec.com.antenasur.dto;

import java.io.Serializable;

import ec.com.antenasur.model.tec.MiembroJRV;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista de {@link MiembroJRV}: miembro de Junta Receptora de Votos. Embebe
 * IglesiaPersona y Mesa; aplana periodo y cargo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiembroJRVDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private IglesiaPersonaDTO iglesiaPersona;
    private MesaDTO mesa;
    private Integer periodoId;
    private String periodoNombre;
    private Integer cargoId;
    private String cargoNombre;

    public static MiembroJRVDTO fromEntity(MiembroJRV m) {
        if (m == null) {
            return null;
        }
        MiembroJRVDTO dto = new MiembroJRVDTO();
        dto.setId(m.getId());
        dto.setIglesiaPersona(IglesiaPersonaDTO.fromEntity(m.getIglesiaPersona()));
        dto.setMesa(MesaDTO.fromEntity(m.getMesa()));
        if (m.getPeriodo() != null) {
            dto.setPeriodoId(m.getPeriodo().getId());
            dto.setPeriodoNombre(m.getPeriodo().getNombre());
        }
        if (m.getCargo() != null) {
            dto.setCargoId(m.getCargo().getId());
            dto.setCargoNombre(m.getCargo().getNombre());
        }
        return dto;
    }
}
