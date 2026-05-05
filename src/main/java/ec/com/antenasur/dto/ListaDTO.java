package ec.com.antenasur.dto;

import java.io.Serializable;

import ec.com.antenasur.model.tec.Lista;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Vista de la entidad {@link Lista}: agrupación política. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String nombre;
    private String slogan;
    private String numero;

    public static ListaDTO fromEntity(Lista l) {
        if (l == null) {
            return null;
        }
        ListaDTO dto = new ListaDTO();
        dto.setId(l.getId());
        dto.setNombre(l.getNombre());
        dto.setSlogan(l.getSlogan());
        dto.setNumero(l.getNumero());
        return dto;
    }

    public Lista toEntity() {
        Lista l = new Lista();
        l.setId(this.id);
        l.setNombre(this.nombre);
        l.setSlogan(this.slogan);
        l.setNumero(this.numero);
        return l;
    }
}
