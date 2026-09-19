package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Persona dada de baja, con los datos de la revisión de Envers en que se
 * eliminó y el diagnóstico de si puede restaurarse.
 *
 * <p>Mismo patrón que {@link IglesiaEliminadaDTO}: se construye únicamente al
 * abrir el diálogo de eliminados y se libera al cerrarlo.
 */
@Data
@NoArgsConstructor
public class PersonaEliminadaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String nombres;
    private String apellidos;
    private String documento;
    private String sexo;
    /** Última iglesia conocida de la persona; referencia para el operador. */
    private String iglesiaNombre;

    /** Revisión de Envers en que se dio de baja; null si no hay rastro auditado. */
    private Integer revisionBaja;
    private Date fechaBaja;
    private String usuarioBaja;

    /**
     * Motivo por el que la restauración no está permitida (cédula ya usada por
     * otra persona activa, por ejemplo); null si sí puede restaurarse.
     */
    private String motivoBloqueo;

    /**
     * Solo se restaura si hay revisión de baja —sin ella no se puede saber qué
     * membresías reactivar— y ninguna regla de negocio lo impide.
     */
    public boolean isRestaurable() {
        return revisionBaja != null && motivoBloqueo == null;
    }

    public boolean isBloqueada() {
        return motivoBloqueo != null;
    }
}
