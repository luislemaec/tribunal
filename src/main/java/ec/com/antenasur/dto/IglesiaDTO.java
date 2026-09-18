package ec.com.antenasur.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import ec.com.antenasur.model.Iglesia;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista de la entidad {@link Iglesia} para la capa UI. Aplana la relación con
 * {@code Geograp ubicacion} en {@code ubicacionId} + {@code ubicacionNombre}
 * para evitar exponer la entidad geográfica completa. Mantiene el flag
 * transitorio {@code tieneDocumentos} (no persistido) que la vista usa para
 * mostrar el ícono de documentos cargados.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IglesiaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String nombre;
    private String comunidad;
    private Integer totalMiembros;
    private String documento;
    /** id de la parroquia (Geograp). */
    private Integer ubicacionId;
    /** nombre de la parroquia. */
    private String ubicacionNombre;
    /** id del cantón (padre de la parroquia). Derivado en {@link #fromEntity}. */
    private Integer cantonId;
    /** nombre del cantón. Derivado en {@link #fromEntity}. */
    private String cantonNombre;
    /** id de la provincia (padre del cantón). Derivado en {@link #fromEntity}. */
    private Integer provinciaId;
    /** nombre de la provincia. Derivado en {@link #fromEntity}. */
    private String provinciaNombre;
    /** Versión para control de edición concurrente. Nunca editable por el usuario. */
    private Long version;
    private Boolean tieneDocumentos;
    /** Indicador de UI calculado en bloque para el filtro de administradores. */
    private Boolean tieneAdministrador;
    /** Nombre del administrador activo, cuando existe. */
    private String administradorNombre;
    /** Fecha real de registro (f_crea). */
    private Date fechaCrea;
    /** Fecha de la última modificación (f_actualiza); null si nunca se modificó. */
    private Date fechaActualiza;
    /** Usuario que registró la iglesia (u_crea). */
    private String usuarioCrea;
    /** Usuario de la última modificación (u_actualiza); null si nunca se modificó. */
    private String usuarioActualiza;
    /**
     * Miembros activos de la iglesia. Lo calcula el servicio con una consulta
     * agregada para toda la lista; nunca se consulta desde el getter.
     */
    private int miembrosActivos;

    /**
     * Cantidad de miembros por debajo del mínimo configurado
     * ({@code tec.iglesias.miembros.minimo}): la vista lo señala para revisión,
     * sin bloquear ninguna operación.
     */
    public boolean isMiembrosBajoMinimo() {
        return miembrosActivos < ec.com.antenasur.util.Constantes.getMinimoMiembrosIglesia();
    }

    /** Primer día desde el cual se identifica visualmente una iglesia como "Nueva". */
    public static final LocalDate INICIO_IDENTIFICADOR_NUEVA = LocalDate.of(2026, 9, 16);
    /** Meses que la marca "Nueva" se mantiene desde la fecha real de registro. */
    public static final int MESES_VIGENCIA_NUEVA = 3;

    /**
     * true si se registró desde {@link #INICIO_IDENTIFICADOR_NUEVA} y aún no
     * transcurren {@link #MESES_VIGENCIA_NUEVA} meses desde su registro. Se
     * calcula con la fecha actual; no consulta la base de datos.
     */
    public boolean isNueva() {
        if (fechaCrea == null) {
            return false;
        }
        LocalDateTime registro = LocalDateTime.ofInstant(fechaCrea.toInstant(), ZoneId.systemDefault());
        return !registro.toLocalDate().isBefore(INICIO_IDENTIFICADOR_NUEVA)
                && LocalDateTime.now().isBefore(registro.plusMonths(MESES_VIGENCIA_NUEVA));
    }

    public static IglesiaDTO fromEntity(Iglesia i) {
        if (i == null) {
            return null;
        }
        IglesiaDTO dto = new IglesiaDTO();
        dto.setId(i.getId());
        dto.setNombre(i.getNombre());
        dto.setComunidad(i.getComunidad());
        dto.setTotalMiembros(i.getTotalMiembros());
        dto.setDocumento(i.getDocumento());
        dto.setTieneDocumentos(i.getTieneDocumentos());
        dto.setVersion(i.getVersion());
        dto.setFechaCrea(i.getFechaCrea());
        dto.setFechaActualiza(i.getFechaActualiza());
        dto.setUsuarioCrea(i.getUsuarioCrea());
        dto.setUsuarioActualiza(i.getUsuarioActualiza());
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
        return dto;
    }

    /**
     * Construye una {@link Iglesia} con los campos editables. La
     * {@code ubicacion} no se setea aquí — el caller debe resolverla por
     * {@link #ubicacionId} y asignarla antes de persistir, para evitar
     * referencias colgadas.
     */
    public Iglesia toEntity() {
        Iglesia i = new Iglesia();
        i.setId(this.id);
        i.setNombre(this.nombre);
        i.setComunidad(this.comunidad);
        i.setTotalMiembros(this.totalMiembros);
        i.setDocumento(this.documento);
        i.setTieneDocumentos(this.tieneDocumentos);
        return i;
    }
}
