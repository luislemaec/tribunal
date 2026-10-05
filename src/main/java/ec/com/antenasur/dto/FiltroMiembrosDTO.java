package ec.com.antenasur.dto;

import java.io.Serializable;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Criterios del listado paginado de miembros (pantalla Personas): alcance por iglesia o
 * por ubicación de la iglesia (provincia, cantón o parroquia) y filtros de columna de la
 * tabla. Se resuelven en BD; la tabla nunca carga el listado completo en memoria.
 */
@Data
@NoArgsConstructor
public class FiltroMiembrosDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer iglesiaId;
    private Integer provinciaId;
    private Integer cantonId;
    private Integer parroquiaId;

    /** Contiene, sin distinguir mayúsculas. */
    private String documento;
    private String nombres;
    /** Búsqueda rápida: cédula o nombres. */
    private String busqueda;

    private Boolean habilitado;
    private Boolean revisado;
    private Boolean inconsistencia;

    public FiltroMiembrosDTO(Integer iglesiaId, Integer provinciaId, Integer cantonId, Integer parroquiaId) {
        this.iglesiaId = iglesiaId;
        this.provinciaId = provinciaId;
        this.cantonId = cantonId;
        this.parroquiaId = parroquiaId;
    }

    /** Sin iglesia ni ubicación no se lista nada: se evita cargar el padrón nacional. */
    public boolean tieneAlcance() {
        return iglesiaId != null || provinciaId != null || cantonId != null || parroquiaId != null;
    }
}
