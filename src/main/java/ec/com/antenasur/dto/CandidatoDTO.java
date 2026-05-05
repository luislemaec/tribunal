package ec.com.antenasur.dto;

import java.io.Serializable;

import ec.com.antenasur.model.tec.Candidato;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista de la entidad {@link Candidato} para la capa UI. Aplana las relaciones
 * a {@code Lista}, {@code Periodo} y {@code CatalogoGeneral cargo} en pares
 * id+nombre, y embebe el {@link IglesiaPersonaDTO} (la vista típicamente
 * muestra el nombre de la persona y su iglesia). Operaciones que requieren
 * persistir relaciones resuelven cada referencia por id en el service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidatoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private IglesiaPersonaDTO iglesiaPersona;
    private Integer listaId;
    private String listaNombre;
    private String listaNumero;
    private Integer periodoId;
    private String periodoNombre;
    private Integer cargoId;
    private String cargoNombre;

    public static CandidatoDTO fromEntity(Candidato c) {
        if (c == null) {
            return null;
        }
        CandidatoDTO dto = new CandidatoDTO();
        dto.setId(c.getId());
        dto.setIglesiaPersona(IglesiaPersonaDTO.fromEntity(c.getIglesiaPersona()));
        if (c.getLista() != null) {
            dto.setListaId(c.getLista().getId());
            dto.setListaNombre(c.getLista().getNombre());
            dto.setListaNumero(c.getLista().getNumero());
        }
        if (c.getPeriodo() != null) {
            dto.setPeriodoId(c.getPeriodo().getId());
            dto.setPeriodoNombre(c.getPeriodo().getNombre());
        }
        if (c.getCargo() != null) {
            dto.setCargoId(c.getCargo().getId());
            dto.setCargoNombre(c.getCargo().getNombre());
        }
        return dto;
    }

    /**
     * Construye un {@link Candidato} con id (si lo trae). Las relaciones
     * ({@code lista}, {@code periodo}, {@code cargo}, {@code iglesiaPersona})
     * NO se setean aquí — el service las resuelve por id antes de persistir.
     */
    public Candidato toEntity() {
        Candidato c = new Candidato();
        c.setId(this.id);
        return c;
    }
}
