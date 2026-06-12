package ec.com.antenasur.service.tec;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ec.com.antenasur.dto.ResultadoCategoriaPublicaDTO;
import ec.com.antenasur.dto.ResultadoMesaPublicaDTO;
import ec.com.antenasur.dto.ResultadoPublicoSnapshotDTO;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@Slf4j
public class ResultadosPublicosCacheService {

    private static final String[] COLORES_GRAFICO = {
        "#1d4ed8", "#047857", "#b45309", "#7c3aed", "#be123c",
        "#0891b2", "#4d7c0f", "#c2410c", "#4338ca", "#0f766e"
    };

    @Inject
    private ProcesoElectoralService procesoElectoralService;

    @Inject
    private EscrutinioService escrutinioService;

    @Inject
    private PadronService padronService;

    private ResultadoPublicoSnapshotDTO snapshot = crearSnapshotVacio();

    @PostConstruct
    public void init() {
        refrescarSeguro();
    }

    @Schedule(hour = "*", minute = "*", second = "0,30", persistent = false)
    @Lock(LockType.WRITE)
    public void refrescarProgramado() {
        refrescarSeguro();
    }

    @Lock(LockType.READ)
    public ResultadoPublicoSnapshotDTO obtenerSnapshot() {
        return copiarSnapshot(snapshot);
    }

    private void refrescarSeguro() {
        try {
            snapshot = construirSnapshot();
        } catch (Exception e) {
            log.error("ERROR AL REFRESCAR CACHE DE RESULTADOS PUBLICOS", e);
        }
    }

    private ResultadoPublicoSnapshotDTO construirSnapshot() {
        ResultadoPublicoSnapshotDTO nuevo = crearSnapshotVacio();
        ProcesoElectoral procesoActivo = procesoElectoralService.getActivo();
        nuevo.setProcesoActivo(procesoActivo);
        nuevo.setUltimaActualizacion(new Date());
        if (procesoActivo == null || procesoActivo.getId() == null) {
            return nuevo;
        }
        Integer procesoId = procesoActivo.getId();
        List<ResultadoCategoriaPublicaDTO> resultados = escrutinioService.obtenerResultadosPublicosPorCategoria(procesoId);
        List<ResultadoMesaPublicaDTO> mesasCerradas = escrutinioService.listarMesasCerradasPublicas(procesoId);
        long totalVotosRegistrados = 0L;
        for (ResultadoCategoriaPublicaDTO resultado : resultados) {
            totalVotosRegistrados += resultado.getTotalVotos() != null ? resultado.getTotalVotos() : 0L;
        }
        long totalMesasProceso = padronService.contarMesasPorProceso(procesoId);
        long totalMesasCerradas = escrutinioService.contarMesasCerradasPorProceso(procesoId);
        BigDecimal porcentaje = calcularPorcentajeMesasCerradas(totalMesasProceso, totalMesasCerradas);

        nuevo.setResultados(resultados);
        nuevo.setMesasCerradas(mesasCerradas);
        nuevo.setTotalMesasProceso(totalMesasProceso);
        nuevo.setTotalMesasCerradas(totalMesasCerradas);
        nuevo.setTotalVotosRegistrados(totalVotosRegistrados);
        nuevo.setPorcentajeMesasCerradas(porcentaje);
        nuevo.setPorcentajeMesasCerradasEntero(porcentaje.setScale(0, RoundingMode.HALF_UP).intValue());
        nuevo.setResultadosChartModel(construirModeloGraficoResultados(resultados));
        return nuevo;
    }

    private ResultadoPublicoSnapshotDTO crearSnapshotVacio() {
        ResultadoPublicoSnapshotDTO dto = new ResultadoPublicoSnapshotDTO();
        dto.setUltimaActualizacion(new Date());
        return dto;
    }

    private ResultadoPublicoSnapshotDTO copiarSnapshot(ResultadoPublicoSnapshotDTO origen) {
        ResultadoPublicoSnapshotDTO copia = new ResultadoPublicoSnapshotDTO();
        if (origen == null) {
            return copia;
        }
        copia.setProcesoActivo(origen.getProcesoActivo());
        copia.setResultados(new ArrayList<>(origen.getResultados()));
        copia.setMesasCerradas(new ArrayList<>(origen.getMesasCerradas()));
        copia.setTotalMesasProceso(origen.getTotalMesasProceso());
        copia.setTotalMesasCerradas(origen.getTotalMesasCerradas());
        copia.setTotalVotosRegistrados(origen.getTotalVotosRegistrados());
        copia.setPorcentajeMesasCerradas(origen.getPorcentajeMesasCerradas());
        copia.setPorcentajeMesasCerradasEntero(origen.getPorcentajeMesasCerradasEntero());
        copia.setResultadosChartModel(origen.getResultadosChartModel());
        copia.setUltimaActualizacion(origen.getUltimaActualizacion());
        return copia;
    }

    private BigDecimal calcularPorcentajeMesasCerradas(long totalMesasProceso, long totalMesasCerradas) {
        if (totalMesasProceso <= 0L) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(totalMesasCerradas)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalMesasProceso), 2, RoundingMode.HALF_UP);
    }

    private String construirModeloGraficoResultados(List<ResultadoCategoriaPublicaDTO> resultados) {
        StringBuilder labels = new StringBuilder();
        StringBuilder data = new StringBuilder();
        StringBuilder background = new StringBuilder();
        StringBuilder border = new StringBuilder();
        for (int i = 0; i < resultados.size(); i++) {
            ResultadoCategoriaPublicaDTO item = resultados.get(i);
            if (i > 0) {
                labels.append(',');
                data.append(',');
                background.append(',');
                border.append(',');
            }
            String color = COLORES_GRAFICO[i % COLORES_GRAFICO.length];
            labels.append('"').append(escaparJson(item.getCategoria())).append('"');
            data.append(item.getTotalVotos() != null ? item.getTotalVotos() : 0L);
            background.append('"').append(color).append("CC").append('"');
            border.append('"').append(color).append('"');
        }
        return "{"
                + "\"type\":\"bar\","
                + "\"data\":{"
                + "\"labels\":[" + labels + "],"
                + "\"datasets\":[{"
                + "\"label\":\"Votos por lista\","
                + "\"data\":[" + data + "],"
                + "\"backgroundColor\":[" + background + "],"
                + "\"borderColor\":[" + border + "],"
                + "\"borderWidth\":1,"
                + "\"borderRadius\":6"
                + "}]"
                + "},"
                + "\"options\":{"
                + "\"responsive\":true,"
                + "\"maintainAspectRatio\":false,"
                + "\"plugins\":{"
                + "\"legend\":{\"display\":false},"
                + "\"tooltip\":{\"enabled\":true}"
                + "},"
                + "\"scales\":{"
                + "\"x\":{\"grid\":{\"display\":false},\"ticks\":{\"color\":\"#334155\",\"font\":{\"weight\":\"bold\"}}},"
                + "\"y\":{\"beginAtZero\":true,\"ticks\":{\"precision\":0,\"color\":\"#475569\"},\"grid\":{\"color\":\"#e2e8f0\"}}"
                + "}"
                + "}"
                + "}";
    }

    private String escaparJson(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
