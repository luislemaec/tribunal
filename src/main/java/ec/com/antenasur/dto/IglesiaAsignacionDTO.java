package ec.com.antenasur.dto;

import java.io.Serializable;

import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.model.Usuario;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista combinada de {@link Iglesia} + su {@link Usuario} IglesiaAdmin (si lo
 * tiene) para la pantalla de Asignación de Usuarios. Aplana los datos del
 * admin a un nivel para que la tabla los muestre sin lazy-loads.
 *
 * <p>Construir con {@link #fromEntity(Iglesia, Usuario)} pasando la iglesia y,
 * opcionalmente, el usuario IglesiaAdmin asignado a esa iglesia (null si la
 * iglesia aún no tiene admin).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IglesiaAsignacionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ── Datos de iglesia ────────────────────────────────────────────────────
    private Integer id;
    private String nombre;
    private String comunidad;
    private String documento;
    private Integer ubicacionId;
    private String ubicacionNombre;
    private Integer cantonId;
    private String cantonNombre;
    private Integer provinciaId;
    private String provinciaNombre;

    // ── Estado de asignación ────────────────────────────────────────────────
    /** true si la iglesia ya tiene un Usuario IglesiaAdmin asignado. */
    private Boolean tieneAdmin;

    // ── Datos del admin (null si tieneAdmin = false) ────────────────────────
    private Integer adminUsuarioId;
    private String adminUsername;
    private String adminCorreo;
    private String adminPersonaNombres;
    private String adminPersonaApellidos;
    private String adminPersonaDocumento;

    public static IglesiaAsignacionDTO fromEntity(Iglesia i, Usuario admin) {
        if (i == null) {
            return null;
        }
        IglesiaAsignacionDTO dto = new IglesiaAsignacionDTO();
        dto.setId(i.getId());
        dto.setNombre(i.getNombre());
        dto.setComunidad(i.getComunidad());
        dto.setDocumento(i.getDocumento());
        if (i.getUbicacion() != null) {
            dto.setUbicacionId(i.getUbicacion().getId());
            dto.setUbicacionNombre(i.getUbicacion().getName());
            if (i.getUbicacion().getGeograp() != null) {
                dto.setCantonId(i.getUbicacion().getGeograp().getId());
                dto.setCantonNombre(i.getUbicacion().getGeograp().getName());
                if (i.getUbicacion().getGeograp().getGeograp() != null) {
                    dto.setProvinciaId(i.getUbicacion().getGeograp().getGeograp().getId());
                    dto.setProvinciaNombre(i.getUbicacion().getGeograp().getGeograp().getName());
                }
            }
        }
        if (admin != null) {
            dto.setTieneAdmin(true);
            dto.setAdminUsuarioId(admin.getId());
            dto.setAdminUsername(admin.getUsername());
            dto.setAdminCorreo(admin.getCorreo());
            if (admin.getPersonsa() != null) {
                dto.setAdminPersonaNombres(admin.getPersonsa().getNombres());
                dto.setAdminPersonaApellidos(admin.getPersonsa().getApellidos());
                dto.setAdminPersonaDocumento(admin.getPersonsa().getDocumento());
            }
        } else {
            dto.setTieneAdmin(false);
        }
        return dto;
    }
}
