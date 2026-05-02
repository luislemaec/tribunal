package ec.com.antenasur.dto;

import java.io.Serializable;

import ec.com.antenasur.model.tec.VwTotalVotos;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista de la {@link VwTotalVotos}: vista BD de agregación de votos. Solo
 * lectura — no expone {@code toEntity}. Aplana las relaciones a recinto,
 * mesa y geograp en pares id+nombre.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VwTotalVotosDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String categoria;
    private Integer totalVotos;
    private Integer orden;
    private Integer recintoId;
    private String recintoNombre;
    private Integer mesaId;
    private String mesaNombre;
    private Integer geograpId;
    private String geograpNombre;
    private Integer cantonId;
    private String cantonNombre;

    public static VwTotalVotosDTO fromEntity(VwTotalVotos v) {
        if (v == null) {
            return null;
        }
        VwTotalVotosDTO dto = new VwTotalVotosDTO();
        dto.setId(v.getId());
        dto.setCategoria(v.getCategoria());
        dto.setTotalVotos(v.getTotalVotos());
        dto.setOrden(v.getOrden());
        if (v.getRecinto() != null) {
            dto.setRecintoId(v.getRecinto().getId());
            dto.setRecintoNombre(v.getRecinto().getNombre());
        }
        if (v.getMesa() != null) {
            dto.setMesaId(v.getMesa().getId());
            dto.setMesaNombre(v.getMesa().getNombre());
        }
        if (v.getGeograp() != null) {
            dto.setGeograpId(v.getGeograp().getId());
            dto.setGeograpNombre(v.getGeograp().getName());
            if (v.getGeograp().getGeograp() != null) {
                dto.setCantonId(v.getGeograp().getGeograp().getId());
                dto.setCantonNombre(v.getGeograp().getGeograp().getName());
            }
        }
        return dto;
    }
}
