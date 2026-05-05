package ec.com.antenasur.dto;

import java.io.Serializable;

import ec.com.antenasur.model.Persona;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista de la entidad {@link Persona} para la capa UI: incluye los campos
 * editables del formulario (nombres, apellidos, documento, tratamiento,
 * sexo). Excluye los campos de auditoría heredados de {@code EntidadAuditable}
 * y el flag {@code estado} (manejado por soft-delete).
 *
 * <p>Mappers estáticos: {@link #fromEntity(Persona)} para serializar y
 * {@link #toEntity()} para construir una Persona con los campos editables.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String nombres;
    private String apellidos;
    private String documento;
    private String tratamiento;
    private String sexo;

    public static PersonaDTO fromEntity(Persona p) {
        if (p == null) {
            return null;
        }
        PersonaDTO dto = new PersonaDTO();
        dto.setId(p.getId());
        dto.setNombres(p.getNombres());
        dto.setApellidos(p.getApellidos());
        dto.setDocumento(p.getDocumento());
        dto.setTratamiento(p.getTratamiento());
        dto.setSexo(p.getSexo());
        return dto;
    }

    public Persona toEntity() {
        Persona p = new Persona();
        p.setId(this.id);
        p.setNombres(this.nombres);
        p.setApellidos(this.apellidos);
        p.setDocumento(this.documento);
        p.setTratamiento(this.tratamiento);
        p.setSexo(this.sexo);
        return p;
    }
}
