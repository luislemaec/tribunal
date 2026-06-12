package ec.com.antenasur.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ec.com.antenasur.model.tec.ProcesoElectoral;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResultadoPublicoSnapshotDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private ProcesoElectoral procesoActivo;
    private List<ResultadoCategoriaPublicaDTO> resultados = new ArrayList<>();
    private List<ResultadoMesaPublicaDTO> mesasCerradas = new ArrayList<>();
    private long totalMesasProceso;
    private long totalMesasCerradas;
    private long totalVotosRegistrados;
    private BigDecimal porcentajeMesasCerradas = BigDecimal.ZERO;
    private int porcentajeMesasCerradasEntero;
    private String resultadosChartModel = "{}";
    private Date ultimaActualizacion;
}
