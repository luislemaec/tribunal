package ec.com.antenasur.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import ec.com.antenasur.model.Persona;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista de la entidad {@link Persona} para la capa UI: incluye los campos
 * editables del formulario (nombres, apellidos, documento, tratamiento,
 * sexo) y, solo como lectura, la trazabilidad heredada de
 * {@code EntidadAuditable} (f_crea / f_actualiza / u_crea / u_actualiza).
 * Excluye el flag {@code estado} (manejado por soft-delete).
 *
 * <p>Mappers estáticos: {@link #fromEntity(Persona)} para serializar y
 * {@link #toEntity()} para construir una Persona con los campos editables;
 * {@link #toEntity()} no propaga los campos de auditoría, que los gestiona la
 * capa de persistencia.
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
    /** Fecha real de registro (f_crea). */
    private Date fechaCrea;
    /** Fecha de la última modificación (f_actualiza); null si nunca se modificó. */
    private Date fechaActualiza;
    /** Usuario que registró la persona (u_crea). */
    private String usuarioCrea;
    /** Usuario de la última modificación (u_actualiza); null si nunca se modificó. */
    private String usuarioActualiza;

    /**
     * true si se registró desde
     * {@link IglesiaDTO#INICIO_IDENTIFICADOR_NUEVA} y aún no transcurren
     * {@link IglesiaDTO#MESES_VIGENCIA_NUEVA} meses. Reutiliza las constantes de
     * Iglesias para que ambos módulos usen la misma vigencia. Se calcula con la
     * fecha actual; no consulta la base de datos.
     */
    public boolean isNuevo() {
        if (fechaCrea == null) {
            return false;
        }
        LocalDateTime registro = LocalDateTime.ofInstant(fechaCrea.toInstant(), ZoneId.systemDefault());
        return !registro.toLocalDate().isBefore(IglesiaDTO.INICIO_IDENTIFICADOR_NUEVA)
                && LocalDateTime.now().isBefore(registro.plusMonths(IglesiaDTO.MESES_VIGENCIA_NUEVA));
    }

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
        dto.setFechaCrea(p.getFechaCrea());
        dto.setFechaActualiza(p.getFechaActualiza());
        dto.setUsuarioCrea(p.getUsuarioCrea());
        dto.setUsuarioActualiza(p.getUsuarioActualiza());
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
