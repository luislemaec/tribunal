package ec.com.antenasur.dto;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

import ec.com.antenasur.model.IglesiaPersona;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista compuesta de {@link IglesiaPersona}: incluye los DTOs de iglesia y
 * persona embebidos en lugar de las entidades. La vista típicamente edita el
 * binding (asigna persona a iglesia) por lo que ambos DTOs internos pueden
 * existir antes de tener id.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IglesiaPersonaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private IglesiaDTO iglesia;
    private PersonaDTO persona;
    private Timestamp desde;
    private Timestamp hasta;
    private String novedad;
    private Date fechaCrea;
    private Date fechaActualiza;

    /**
     * Indica si este miembro está habilitado para el padrón electoral.
     * {@code null} en registros anteriores a la migración DDL se convierte
     * a {@code true} en {@link #fromEntity(IglesiaPersona)} para mantener
     * retrocompatibilidad: todos los miembros previos quedan habilitados.
     */
    private Boolean habilitadoPadron;

    /** Derivado: true si la fila se editó después de crearse (ciclo de
     *  actualización). Regla: fechaActualiza > fechaCrea + 2 seg. */
    private Boolean actualizada;

    public static IglesiaPersonaDTO fromEntity(IglesiaPersona ip) {
        if (ip == null) {
            return null;
        }
        IglesiaPersonaDTO dto = new IglesiaPersonaDTO();
        dto.setId(ip.getId());
        dto.setIglesia(IglesiaDTO.fromEntity(ip.getIglesia()));
        dto.setPersona(PersonaDTO.fromEntity(ip.getPersona()));
        dto.setDesde(ip.getDesde());
        dto.setHasta(ip.getHasta());
        dto.setNovedad(ip.getNovedad());
        dto.setFechaCrea(ip.getFechaCrea());
        dto.setFechaActualiza(ip.getFechaActualiza());
        dto.setActualizada(esActualizada(ip.getFechaCrea(), ip.getFechaActualiza()));
        // null (registros previos a la migración DDL) → true por retrocompatibilidad
        dto.setHabilitadoPadron(ip.getHabilitadoPadron() == null ? Boolean.TRUE : ip.getHabilitadoPadron());
        return dto;
    }

    private static boolean esActualizada(Date creada, Date modificada) {
        if (modificada == null || creada == null) return false;
        // Tolerancia de 2 segundos: el merge inmediato post-create deja un
        // delta pequeño que no debe contar como "actualización del usuario".
        return modificada.getTime() - creada.getTime() > 2000L;
    }
}
