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
    /** Votos válidos: suma de los votos de las listas. */
    private long totalVotosRegistrados;
    /** Votos en blanco y nulos: informativos, no se asignan a ninguna lista. */
    private long totalVotosBlancos;
    private long totalVotosNulos;
    private BigDecimal porcentajeMesasCerradas = BigDecimal.ZERO;
    private int porcentajeMesasCerradasEntero;
    private String resultadosChartModel = "{}";
    private Date ultimaActualizacion;
    /** Fase SUFRAGIO del cronograma: los resultados se publican desde {@code finSufragio}. */
    private Date inicioSufragio;
    private Date finSufragio;

    /**
     * Los resultados son públicos solo desde el fin del sufragio. Sin fase SUFRAGIO activa
     * con fecha de fin, no se publican.
     */
    public boolean isPublicacionHabilitada(Date ahora) {
        return finSufragio != null && ahora != null && !ahora.before(finSufragio);
    }
}
