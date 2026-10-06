package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

/**
 * Identidad de quien registró una actividad en la bitácora: nombre de acceso guardado
 * en {@code tec.procesos.u_crea}, nombre completo de su persona y roles actuales.
 *
 * <p>La bitácora no guarda el rol vigente al momento de la acción: los roles son los
 * actuales del usuario. Se incluyen usuarios desactivados para conservar la trazabilidad.
 */
@Getter
public class UsuarioAuditoriaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Prefijo técnico de los roles de Elytron; no se muestra al usuario. */
    private static final String PREFIJO_ROL = "SITEC-";

    private final String usuario;
    private final String nombre;
    private final List<String> roles = new ArrayList<>();

    public UsuarioAuditoriaDTO(String usuario, String nombre) {
        this.usuario = usuario;
        this.nombre = nombre;
    }

    /** Agrega un rol sin el prefijo técnico, sin repetir. */
    public void agregarRol(String rol) {
        if (rol == null || rol.isBlank()) {
            return;
        }
        String visible = rol.startsWith(PREFIJO_ROL) ? rol.substring(PREFIJO_ROL.length()) : rol;
        if (!roles.contains(visible)) {
            roles.add(visible);
        }
    }

    /** Roles separados por coma; vacío si no tiene roles activos. */
    public String getRolesTexto() {
        return String.join(", ", roles);
    }

    /** Etiqueta para listas: «Nombre (usuario)», o solo el usuario si no hay persona. */
    public String getEtiqueta() {
        return nombre == null || nombre.isBlank() ? usuario : nombre + " (" + usuario + ")";
    }
}
