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
}
