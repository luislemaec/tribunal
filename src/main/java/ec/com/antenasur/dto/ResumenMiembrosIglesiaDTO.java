package ec.com.antenasur.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Totales consolidados de los miembros activos de una iglesia.
 *
 * <p>Se usa en el dashboard para evitar cargar y recorrer el listado completo
 * de personas durante el renderizado de la vista.</p>
 */
@Getter
@AllArgsConstructor
public class ResumenMiembrosIglesiaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int totalPersonas;
    private final int personasInformacionCompleta;
    private final int personasPendientesRevision;
    /** Miembros cuyo vínculo no está habilitado para el padrón (habilitadoPadron distinto de TRUE). */
    private final int personasNoHabilitadas;
    /** Miembros con relación activa también en otra iglesia: solo Administrador o Tribunal los regularizan. */
    private final int personasEnOtraIglesia;

    public ResumenMiembrosIglesiaDTO(int totalPersonas, int personasInformacionCompleta,
            int personasPendientesRevision) {
        this(totalPersonas, personasInformacionCompleta, personasPendientesRevision, 0, 0);
    }

    /** Hay miembros pendientes de revisión o no habilitados: corresponde el aviso de actualización. */
    public boolean isRequiereAtencion() {
        return personasPendientesRevision > 0 || personasNoHabilitadas > 0;
    }
}
