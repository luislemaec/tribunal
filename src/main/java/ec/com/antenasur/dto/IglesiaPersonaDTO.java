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

    /**
     * Derivado: true si el vínculo iglesia-persona ya fue revisado dentro del
     * ciclo de actualización. La revisión se persiste mediante f_actualiza.
     */
    private Boolean actualizada;

    /** Cantidad de iglesias activas distintas asociadas al documento. */
    private Integer cantidadIglesiasActivas;

    /** Indicador derivado para impedir que la inconsistencia use el CRUD normal. */
    private Boolean inconsistenciaIglesias;

    /** Cantidad de personas activas que comparten la misma cédula normalizada. */
    private Integer cantidadCedulaDuplicada;

    /** Indicador derivado de cédula duplicada entre personas distintas. */
    private Boolean inconsistenciaCedula;

    /** Nombres de las iglesias activas, utilizado por el reporte de inconsistencias. */
    private String iglesiasActivas;

    /** Estado persistido de esta relacion, incluido en reportes de trazabilidad. */
    private Boolean estadoRelacion;

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
        dto.setEstadoRelacion(ip.getEstado());
        dto.setCantidadIglesiasActivas(0);
        dto.setInconsistenciaIglesias(Boolean.FALSE);
        dto.setCantidadCedulaDuplicada(0);
        dto.setInconsistenciaCedula(Boolean.FALSE);
        // Regla estricta: solo true habilita para el padron.
        dto.setHabilitadoPadron(Boolean.TRUE.equals(ip.getHabilitadoPadron()));
        return dto;
    }

    /** Una inconsistencia puede corresponder a iglesias, cédula o ambas. */
    public boolean isTieneInconsistencia() {
        return Boolean.TRUE.equals(inconsistenciaIglesias)
                || Boolean.TRUE.equals(inconsistenciaCedula);
    }

    private static boolean esActualizada(Date creada, Date modificada) {
        if (modificada == null) {
            return false;
        }
        return creada == null || !modificada.before(creada);
    }
}
