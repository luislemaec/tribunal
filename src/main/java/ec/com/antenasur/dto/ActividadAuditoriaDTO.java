package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.Date;

import ec.com.antenasur.audit.CatalogoActividades;
import ec.com.antenasur.model.tec.Proceso;
import lombok.Data;

/**
 * Proyección legible de una actividad de {@code tec.procesos}. Módulo, acción,
 * resultado y detalle provienen de {@link CatalogoActividades}; el texto
 * técnico se conserva sin cambios en {@link #actividadOriginal}.
 */
@Data
public class ActividadAuditoriaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private Date fecha;
    private String usuario;
    private String ip;
    private String modulo;
    private String accion;
    private String resultado;
    private String severidadResultado;
    private String detalle;
    private String actividadOriginal;

    public static ActividadAuditoriaDTO fromEntity(Proceso proceso) {
        if (proceso == null) return null;
        ActividadAuditoriaDTO dto = new ActividadAuditoriaDTO();
        dto.setId(proceso.getId());
        dto.setFecha(proceso.getFechaCrea());
        dto.setUsuario(proceso.getUsuarioCrea());
        dto.setIp(proceso.getIp());
        dto.setActividadOriginal(proceso.getActividad());

        CatalogoActividades.ActividadNormalizada actividad = CatalogoActividades.normalizar(proceso.getActividad());
        dto.setModulo(actividad.modulo());
        dto.setAccion(actividad.accion());
        dto.setResultado(actividad.resultado());
        dto.setSeveridadResultado(CatalogoActividades.severidad(actividad.resultado()));
        dto.setDetalle(actividad.detalle());
        return dto;
    }
}
