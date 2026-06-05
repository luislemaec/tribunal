package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.Date;

import ec.com.antenasur.enums.EstadoEscrutinio;
import ec.com.antenasur.model.tec.EscrutinioCabecera;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EscrutinioCabeceraDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private MesaDTO mesa;
    private Integer procesoId;
    private String procesoNombre;
    private EstadoEscrutinio estadoEscrutinio;
    private String presidenteResponsable;
    private Date fechaApertura;
    private Date fechaInicioConteo;
    private Date fechaCierre;
    private Integer totalSufragantes;
    private Integer totalVotosRegistrados;
    private Integer totalVotosValidos;
    private Integer totalVotosBlancos;
    private Integer totalVotosNulos;
    private String observacionApertura;
    private String observacionConteo;
    private String observacionCierre;

    public static EscrutinioCabeceraDTO fromEntity(EscrutinioCabecera e) {
        if (e == null) {
            return null;
        }
        EscrutinioCabeceraDTO dto = new EscrutinioCabeceraDTO();
        dto.setId(e.getId());
        dto.setMesa(MesaDTO.fromEntity(e.getMesa()));
        if (e.getProceso() != null) {
            dto.setProcesoId(e.getProceso().getId());
            dto.setProcesoNombre(e.getProceso().getNombre());
        }
        dto.setEstadoEscrutinio(e.getEstadoEscrutinio());
        dto.setPresidenteResponsable(e.getPresidenteResponsable());
        dto.setFechaApertura(e.getFechaApertura());
        dto.setFechaInicioConteo(e.getFechaInicioConteo());
        dto.setFechaCierre(e.getFechaCierre());
        dto.setTotalSufragantes(e.getTotalSufragantes());
        dto.setTotalVotosRegistrados(e.getTotalVotosRegistrados());
        dto.setTotalVotosValidos(e.getTotalVotosValidos());
        dto.setTotalVotosBlancos(e.getTotalVotosBlancos());
        dto.setTotalVotosNulos(e.getTotalVotosNulos());
        dto.setObservacionApertura(e.getObservacionApertura());
        dto.setObservacionConteo(e.getObservacionConteo());
        dto.setObservacionCierre(e.getObservacionCierre());
        return dto;
    }
}
