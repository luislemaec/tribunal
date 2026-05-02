package ec.com.antenasur.dto;

import java.io.Serializable;

import ec.com.antenasur.model.tec.CategoriaVoto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Vista de la entidad {@link CategoriaVoto}: categoría de voto del acta. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaVotoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String nombre;
    private Integer categoriaVoto;
    private Integer orden;

    public static CategoriaVotoDTO fromEntity(CategoriaVoto c) {
        if (c == null) {
            return null;
        }
        CategoriaVotoDTO dto = new CategoriaVotoDTO();
        dto.setId(c.getId());
        dto.setNombre(c.getNombre());
        dto.setCategoriaVoto(c.getCategoriaVoto());
        dto.setOrden(c.getOrden());
        return dto;
    }

    public CategoriaVoto toEntity() {
        CategoriaVoto c = new CategoriaVoto();
        c.setId(this.id);
        c.setNombre(this.nombre);
        c.setCategoriaVoto(this.categoriaVoto);
        c.setOrden(this.orden);
        return c;
    }
}
