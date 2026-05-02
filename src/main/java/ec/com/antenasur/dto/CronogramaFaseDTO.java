package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.Date;

import ec.com.antenasur.enums.FaseElectoral;
import ec.com.antenasur.enums.SeveridadCronograma;
import ec.com.antenasur.model.tec.CronogramaFase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista de {@link CronogramaFase} para la capa UI. Incluye flags derivados
 * que evitan que la vista calcule lógica de fechas:
 * <ul>
 *   <li>{@code vigente}: now() entre fechaInicio y fechaFin</li>
 *   <li>{@code futura}: now() &lt; fechaInicio</li>
 *   <li>{@code pasada}: now() &gt; fechaFin</li>
 *   <li>{@code diasRestantes}: días hasta fechaFin (si vigente o futura)</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CronogramaFaseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer procesoId;
    private String procesoNombre;
    private FaseElectoral fase;
    private String titulo;
    private String mensaje;
    private SeveridadCronograma severidad;
    private Date fechaInicio;
    private Date fechaFin;
    private Boolean permiteEdicion;
    private Integer orden;

    private Boolean vigente;
    private Boolean futura;
    private Boolean pasada;
    private Long diasRestantes;

    public static CronogramaFaseDTO fromEntity(CronogramaFase f) {
        if (f == null) return null;
        CronogramaFaseDTO dto = new CronogramaFaseDTO();
        dto.setId(f.getId());
        if (f.getProceso() != null) {
            dto.setProcesoId(f.getProceso().getId());
            dto.setProcesoNombre(f.getProceso().getNombre());
        }
        dto.setFase(f.getFase());
        dto.setTitulo(f.getTitulo());
        dto.setMensaje(f.getMensaje());
        dto.setSeveridad(f.getSeveridad());
        dto.setFechaInicio(f.getFechaInicio());
        dto.setFechaFin(f.getFechaFin());
        dto.setPermiteEdicion(f.getPermiteEdicion());
        dto.setOrden(f.getOrden());

        long now = System.currentTimeMillis();
        long ini = f.getFechaInicio() != null ? f.getFechaInicio().getTime() : Long.MAX_VALUE;
        long fin = f.getFechaFin() != null ? f.getFechaFin().getTime() : Long.MIN_VALUE;
        dto.setVigente(now >= ini && now <= fin);
        dto.setFutura(now < ini);
        dto.setPasada(now > fin);
        if (fin >= now) {
            dto.setDiasRestantes((fin - now) / (1000L * 60 * 60 * 24));
        } else {
            dto.setDiasRestantes(0L);
        }
        return dto;
    }

    public CronogramaFase toEntity() {
        CronogramaFase f = new CronogramaFase();
        f.setId(this.id);
        f.setFase(this.fase);
        f.setTitulo(this.titulo);
        f.setMensaje(this.mensaje);
        f.setSeveridad(this.severidad);
        f.setFechaInicio(this.fechaInicio);
        f.setFechaFin(this.fechaFin);
        f.setPermiteEdicion(this.permiteEdicion);
        f.setOrden(this.orden);
        return f;
    }
}
