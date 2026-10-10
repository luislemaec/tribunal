package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;

/**
 * Iglesia activa con su administrador (si lo tiene) y el avance de registro de sus
 * miembros, para la pestaña Administradores de Rep. Registros. Una fila por iglesia.
 */
@Getter
public class AdministradorIglesiaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Integer iglesiaId;
    private final String iglesia;
    private final Integer parroquiaId;
    private final String parroquia;
    private final Integer cantonId;
    private final String canton;
    private final String provincia;
    /** Personas distintas con vínculo activo (misma regla que Registros). */
    private final long registrados;
    private final long habilitados;
    private final long pendientesRevision;

    @Setter
    private String administradorNombre;
    @Setter
    private String administradorUsuario;
    @Setter
    private String administradorCorreo;
    /** usu_permanente: TRUE cuando ya cambió la clave temporal; FALSE o nulo es pendiente. */
    @Setter
    private Boolean administradorPermanente;
    @Setter
    private Date ultimoAcceso;
    /** Calculado con los días de inactividad elegidos en el filtro. */
    @Setter
    private boolean sinAccesoReciente;

    public AdministradorIglesiaDTO(Integer iglesiaId, String iglesia, Integer parroquiaId, String parroquia,
            Integer cantonId, String canton, String provincia, Long registrados, Long habilitados,
            Long pendientesRevision) {
        this.iglesiaId = iglesiaId;
        this.iglesia = iglesia == null ? "" : iglesia;
        this.parroquiaId = parroquiaId;
        this.parroquia = parroquia == null ? "" : parroquia;
        this.cantonId = cantonId;
        this.canton = canton == null ? "" : canton;
        this.provincia = provincia;
        this.registrados = registrados == null ? 0L : registrados;
        this.habilitados = habilitados == null ? 0L : habilitados;
        this.pendientesRevision = pendientesRevision == null ? 0L : pendientesRevision;
    }

    public boolean isTieneAdministrador() {
        return administradorUsuario != null;
    }

    public boolean isAdministradorActivado() {
        return isTieneAdministrador() && Boolean.TRUE.equals(administradorPermanente);
    }

    /** Orden de la lista de acción: sin administrador, pendiente, sin acceso reciente, al día. */
    public int getPrioridad() {
        if (!isTieneAdministrador()) {
            return 0;
        }
        if (!isAdministradorActivado()) {
            return 1;
        }
        return sinAccesoReciente ? 2 : 3;
    }
}
