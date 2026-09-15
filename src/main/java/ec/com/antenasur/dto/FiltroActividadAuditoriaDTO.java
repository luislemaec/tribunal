package ec.com.antenasur.dto;

import java.io.Serializable;
import java.time.LocalDate;

import lombok.Data;

/** Criterios de consulta para la bitácora funcional almacenada en tec.procesos. */
@Data
public class FiltroActividadAuditoriaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String usuario;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String accion;
    private String modulo;
    private String resultado;
    private String busqueda;
}
