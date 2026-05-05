package ec.com.antenasur.dto;

import java.io.Serializable;

import ec.com.antenasur.model.Usuario;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista segura de un {@link Usuario} para la capa UI: omite
 * {@code contrasenia}, {@code contraseniaTemp}, {@code link} y los campos de
 * auditoría (heredados de {@code EntidadAuditable}). Los datos de la persona
 * se aplanan en {@code personaId}, {@code personaDocumento} y
 * {@code personaNombres} para evitar exponer la entidad relacionada completa.
 *
 * <p>Mappers estáticos: {@link #fromEntity(Usuario)} para serializar y
 * {@link #toEntity()} para construir un Usuario con los campos editables. La
 * contraseña nunca se asigna desde el DTO — para eso están
 * {@code UsuarioService.cambiarContrasenia} e
 * {@code iniciarRecuperacionClave}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String username;
    private String correo;
    private Boolean permanente;
    private Boolean estado;
    /** Flag derivado: true si el usuario tiene una clave temporal pendiente
     *  (la clave temporal en sí NO se expone). */
    private Boolean tienePasswordTemporal;

    private Integer personaId;
    private String personaDocumento;
    private String personaNombres;
    private String personaApellidos;

    /** Iglesia asociada (solo aplica al rol IglesiaAdmin). Null = usuario global. */
    private Integer iglesiaId;
    private String iglesiaNombre;

    public static UsuarioDTO fromEntity(Usuario u) {
        if (u == null) {
            return null;
        }
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(u.getId());
        dto.setUsername(u.getUsername());
        dto.setCorreo(u.getCorreo());
        dto.setPermanente(u.getPermanente());
        dto.setEstado(u.getEstado());
        dto.setTienePasswordTemporal(u.getContraseniaTemp() != null);
        if (u.getPersonsa() != null) {
            dto.setPersonaId(u.getPersonsa().getId());
            dto.setPersonaDocumento(u.getPersonsa().getDocumento());
            dto.setPersonaNombres(u.getPersonsa().getNombres());
            dto.setPersonaApellidos(u.getPersonsa().getApellidos());
        }
        if (u.getIglesia() != null) {
            dto.setIglesiaId(u.getIglesia().getId());
            dto.setIglesiaNombre(u.getIglesia().getNombre());
        }
        return dto;
    }

    /**
     * Construye un {@link Usuario} con los campos editables. NO setea
     * {@code contrasenia}, {@code contraseniaTemp}, ni la {@code Persona}
     * asociada (la persona se resuelve aparte para evitar relaciones
     * incompletas).
     */
    public Usuario toEntity() {
        Usuario u = new Usuario();
        u.setId(this.id);
        u.setUsername(this.username);
        u.setCorreo(this.correo);
        u.setPermanente(this.permanente);
        u.setEstado(this.estado);
        return u;
    }
}
