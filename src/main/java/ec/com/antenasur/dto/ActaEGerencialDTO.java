package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.Date;

import ec.com.antenasur.enums.EstadoEscrutinio;
import ec.com.antenasur.model.tec.Documentos;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActaEGerencialDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer mesaId;
    private String provincia;
    private String canton;
    private String parroquia;
    /** Ids geográficos, para filtrar el listado en memoria sin volver a consultar. */
    private Integer cantonId;
    private Integer parroquiaId;
    private String recinto;
    private String mesa;
    private String presidenteMesa;
    private EstadoEscrutinio estadoEscrutinio;
    private Integer sufragantesAsignados;
    private Integer votosRegistrados;
    private Integer votosValidos;
    private Integer votosBlancos;
    private Integer votosNulos;
    private Date fechaApertura;
    private Date fechaCierre;
    private Boolean actaPdfGenerada;
    private Documentos documentoActa;
    /** Estado de revisión del acta física vigente; null si la mesa no tiene acta física. */
    private String actaFisicaEstado;

    public ProgresoEscrutinioDTO getProgreso() {
        return ProgresoEscrutinioDTO.determinar(estadoEscrutinio, fechaApertura != null, actaFisicaEstado);
    }


    public boolean isTieneActaFisica() {
        return actaFisicaEstado != null;
    }

    public String getEstadoSeverity() {
        if (estadoEscrutinio == null) {
            return "secondary";
        }
        return switch (estadoEscrutinio) {
            case CERRADO -> "success";
            case OBSERVADO, ANULADO -> "danger";
            case ABIERTO, EN_CONTEO, CONTEO_REGISTRADO, REABIERTO -> "warning";
            default -> "secondary";
        };
    }

    /** Icono del estado, para distinguirlo de un vistazo junto a la etiqueta. */
    public String getEstadoIcono() {
        if (estadoEscrutinio == null) {
            return "pi pi-clock";
        }
        return switch (estadoEscrutinio) {
            case CERRADO -> "pi pi-check-circle";
            case OBSERVADO, ANULADO -> "pi pi-exclamation-triangle";
            case ABIERTO, EN_CONTEO, CONTEO_REGISTRADO, REABIERTO -> "pi pi-spin pi-sync";
            default -> "pi pi-clock";
        };
    }

    /**
     * Clave del rótulo de la acción principal de la fila, según el estado. Mismo patrón
     * que ResumenMesaJrvDTO.getEstadoClave(): el DTO decide la clave y la vista la
     * resuelve contra el bundle. No altera ninguna regla del escrutinio; solo nombra la
     * acción que ya permite el estado.
     */
    public String getAccionClave() {
        if (estadoEscrutinio == null) {
            return "actaE.accion.iniciar";
        }
        return switch (estadoEscrutinio) {
            case PENDIENTE -> "actaE.accion.iniciar";
            case CERRADO, ANULADO -> "actaE.accion.ver";
            default -> "actaE.accion.continuar";
        };
    }

    public String getAccionIcono() {
        if (estadoEscrutinio == null) {
            return "pi pi-play";
        }
        return switch (estadoEscrutinio) {
            case PENDIENTE -> "pi pi-play";
            case CERRADO, ANULADO -> "pi pi-eye";
            default -> "pi pi-pencil";
        };
    }
}
