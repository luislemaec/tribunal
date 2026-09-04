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
}
