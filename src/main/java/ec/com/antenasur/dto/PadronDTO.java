package ec.com.antenasur.dto;

import java.io.Serializable;

import ec.com.antenasur.model.tec.Padron;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista de la entidad {@link Padron} para la capa UI. Embebe los DTOs de
 * {@code IglesiaPersona} y {@code Mesa}, aplana {@code periodo} en
 * {@code periodoId} + {@code periodoNombre}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PadronDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private IglesiaPersonaDTO iglesiaPersona;
    private MesaDTO mesa;
    private Integer periodoId;
    private String periodoNombre;
    private Boolean sufrago;

    public static PadronDTO fromEntity(Padron p) {
        if (p == null) {
            return null;
        }
        PadronDTO dto = new PadronDTO();
        dto.setId(p.getId());
        dto.setIglesiaPersona(IglesiaPersonaDTO.fromEntity(p.getIglesiaPersona()));
        dto.setMesa(MesaDTO.fromEntity(p.getMesa()));
        dto.setSufrago(p.getSufrago());
        if (p.getPeriodo() != null) {
            dto.setPeriodoId(p.getPeriodo().getId());
            dto.setPeriodoNombre(p.getPeriodo().getNombre());
        }
        return dto;
    }

    /**
     * Construye un {@link Padron} con id (si lo trae) y {@code sufrago}. Las
     * relaciones (mesa, periodo, iglesiaPersona) se resuelven en el service
     * por id.
     */
    public Padron toEntity() {
        Padron p = new Padron();
        p.setId(this.id);
        p.setSufrago(this.sufrago);
        return p;
    }
}
