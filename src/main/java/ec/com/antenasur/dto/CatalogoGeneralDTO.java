package ec.com.antenasur.dto;

import java.io.Serializable;

import ec.com.antenasur.model.tec.CatalogoGeneral;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista de la entidad {@link CatalogoGeneral}: catálogo jerárquico (cargos,
 * tipos, etc.). Aplana la relación al padre en {@code padreId} +
 * {@code padreNombre} para evitar exponer la entidad relacionada.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogoGeneralDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String nombre;
    private String descripcion;
    private Integer historial;
    private Integer orden;
    private String info;
    private Integer padreId;
    private String padreNombre;

    public static CatalogoGeneralDTO fromEntity(CatalogoGeneral c) {
        if (c == null) {
            return null;
        }
        CatalogoGeneralDTO dto = new CatalogoGeneralDTO();
        dto.setId(c.getId());
        dto.setNombre(c.getNombre());
        dto.setDescripcion(c.getDescripcion());
        dto.setHistorial(c.getHistorial());
        dto.setOrden(c.getOrden());
        dto.setInfo(c.getInfo());
        if (c.getPadre() != null) {
            dto.setPadreId(c.getPadre().getId());
            dto.setPadreNombre(c.getPadre().getNombre());
        }
        return dto;
    }

    /**
     * Construye un {@link CatalogoGeneral} con campos editables. La relación
     * {@code padre} debe resolverse en el service por {@link #padreId}.
     */
    public CatalogoGeneral toEntity() {
        CatalogoGeneral c = new CatalogoGeneral();
        c.setId(this.id);
        c.setNombre(this.nombre);
        c.setDescripcion(this.descripcion);
        c.setHistorial(this.historial);
        c.setOrden(this.orden);
        c.setInfo(this.info);
        return c;
    }
}
