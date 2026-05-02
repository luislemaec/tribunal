package ec.com.antenasur.dto;

import java.io.Serializable;

import ec.com.antenasur.model.Menu;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista de la entidad {@link Menu}. Aplana la relación al padre en
 * {@code padreId} + {@code padreNombre}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String nombre;
    private String accion;
    private Boolean nodoFinal;
    private String icono;
    private String imagen;
    private Integer orden;
    private String url;
    private String componenteid;
    private Integer padreId;
    private String padreNombre;

    public static MenuDTO fromEntity(Menu m) {
        if (m == null) {
            return null;
        }
        MenuDTO dto = new MenuDTO();
        dto.setId(m.getId());
        dto.setNombre(m.getNombre());
        dto.setAccion(m.getAccion());
        dto.setNodoFinal(m.getNodoFinal());
        dto.setIcono(m.getIcono());
        dto.setImagen(m.getImagen());
        dto.setOrden(m.getOrden());
        dto.setUrl(m.getUrl());
        dto.setComponenteid(m.getComponenteid());
        if (m.getPadre() != null) {
            dto.setPadreId(m.getPadre().getId());
            dto.setPadreNombre(m.getPadre().getNombre());
        }
        return dto;
    }

    /**
     * Construye un {@link Menu} con campos editables. La relación
     * {@code padre} debe resolverse en el service por {@link #padreId}.
     */
    public Menu toEntity() {
        Menu m = new Menu();
        m.setId(this.id);
        m.setNombre(this.nombre);
        m.setAccion(this.accion);
        m.setNodoFinal(this.nodoFinal);
        m.setIcono(this.icono);
        m.setImagen(this.imagen);
        m.setOrden(this.orden);
        m.setUrl(this.url);
        m.setComponenteid(this.componenteid);
        return m;
    }
}
