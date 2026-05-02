package ec.com.antenasur.dto;

import java.io.Serializable;

import ec.com.antenasur.model.tec.Escrutinio;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista de la entidad {@link Escrutinio} para la capa UI. Embebe el
 * {@link MesaDTO} (la vista del acta muestra la mesa siempre presente) y
 * aplana {@code periodo} y {@code categoria} en pares id+nombre.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EscrutinioDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private MesaDTO mesa;
    private Integer periodoId;
    private String periodoNombre;
    private Integer categoriaId;
    private String categoriaNombre;
    private Integer totalVotos;

    public static EscrutinioDTO fromEntity(Escrutinio e) {
        if (e == null) {
            return null;
        }
        EscrutinioDTO dto = new EscrutinioDTO();
        dto.setId(e.getId());
        dto.setMesa(MesaDTO.fromEntity(e.getMesa()));
        dto.setTotalVotos(e.getTotalVotos());
        if (e.getPeriodo() != null) {
            dto.setPeriodoId(e.getPeriodo().getId());
            dto.setPeriodoNombre(e.getPeriodo().getNombre());
        }
        if (e.getCategoria() != null) {
            dto.setCategoriaId(e.getCategoria().getId());
            dto.setCategoriaNombre(e.getCategoria().getNombre());
        }
        return dto;
    }

    /**
     * Construye un {@link Escrutinio} con id y {@code totalVotos}. Las
     * relaciones (mesa, periodo, categoria) se resuelven en el service por
     * id.
     */
    public Escrutinio toEntity() {
        Escrutinio e = new Escrutinio();
        e.setId(this.id);
        e.setTotalVotos(this.totalVotos);
        return e;
    }
}
