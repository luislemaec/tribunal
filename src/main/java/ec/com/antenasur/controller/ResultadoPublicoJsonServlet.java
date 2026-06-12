package ec.com.antenasur.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import ec.com.antenasur.dto.ResultadoCategoriaPublicaDTO;
import ec.com.antenasur.dto.ResultadoMesaPublicaDTO;
import ec.com.antenasur.dto.ResultadoPublicoSnapshotDTO;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.service.tec.ResultadosPublicosCacheService;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/public/resultados.json", loadOnStartup = 1)
public class ResultadoPublicoJsonServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final int CACHE_SECONDS = 15;

    @Inject
    private ResultadosPublicosCacheService resultadosPublicosCacheService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ResultadoPublicoSnapshotDTO snapshot = resultadosPublicosCacheService.obtenerSnapshot();
        String etag = construirEtag(snapshot);
        if (etag.equals(request.getHeader("If-None-Match"))) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            aplicarHeadersCache(response, etag);
            return;
        }

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        aplicarHeadersCache(response, etag);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(toJson(snapshot));
        }
    }

    private void aplicarHeadersCache(HttpServletResponse response, String etag) {
        response.setHeader("Cache-Control", "public, max-age=" + CACHE_SECONDS + ", stale-while-revalidate=30");
        response.setHeader("ETag", etag);
        response.setDateHeader("Expires", System.currentTimeMillis() + (CACHE_SECONDS * 1000L));
        response.setHeader("X-Content-Type-Options", "nosniff");
    }

    private String construirEtag(ResultadoPublicoSnapshotDTO snapshot) {
        long fecha = snapshot != null && snapshot.getUltimaActualizacion() != null
                ? snapshot.getUltimaActualizacion().getTime() : 0L;
        long mesas = snapshot != null ? snapshot.getTotalMesasCerradas() : 0L;
        long votos = snapshot != null ? snapshot.getTotalVotosRegistrados() : 0L;
        return "\"" + fecha + "-" + mesas + "-" + votos + "\"";
    }

    private String toJson(ResultadoPublicoSnapshotDTO snapshot) {
        StringBuilder json = new StringBuilder(4096);
        ProcesoElectoral proceso = snapshot.getProcesoActivo();
        json.append('{');
        json.append("\"generatedAt\":\"").append(formatearFechaIso(snapshot.getUltimaActualizacion())).append("\",");
        json.append("\"hasActiveProcess\":").append(proceso != null && proceso.getId() != null).append(',');
        json.append("\"process\":{");
        json.append("\"id\":").append(proceso != null && proceso.getId() != null ? proceso.getId() : "null").append(',');
        json.append("\"name\":\"").append(escaparJson(proceso != null ? proceso.getNombre() : "")).append("\"");
        json.append("},");
        json.append("\"summary\":{");
        json.append("\"totalMesas\":").append(snapshot.getTotalMesasProceso()).append(',');
        json.append("\"mesasCerradas\":").append(snapshot.getTotalMesasCerradas()).append(',');
        json.append("\"mesasPendientes\":").append(Math.max(snapshot.getTotalMesasProceso() - snapshot.getTotalMesasCerradas(), 0L)).append(',');
        json.append("\"totalVotosListas\":").append(snapshot.getTotalVotosRegistrados()).append(',');
        json.append("\"porcentajeMesasCerradas\":").append(toNumero(snapshot.getPorcentajeMesasCerradas())).append(',');
        json.append("\"porcentajeMesasCerradasEntero\":").append(snapshot.getPorcentajeMesasCerradasEntero());
        json.append("},");
        json.append("\"results\":[");
        for (int i = 0; i < snapshot.getResultados().size(); i++) {
            ResultadoCategoriaPublicaDTO item = snapshot.getResultados().get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append('{');
            json.append("\"id\":").append(item.getCategoriaId() != null ? item.getCategoriaId() : "null").append(',');
            json.append("\"name\":\"").append(escaparJson(item.getCategoria())).append("\",");
            json.append("\"votes\":").append(item.getTotalVotos() != null ? item.getTotalVotos() : 0L).append(',');
            json.append("\"percentage\":").append(toNumero(item.getPorcentaje()));
            json.append('}');
        }
        json.append("],");
        json.append("\"tables\":[");
        for (int i = 0; i < snapshot.getMesasCerradas().size(); i++) {
            ResultadoMesaPublicaDTO mesa = snapshot.getMesasCerradas().get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append('{');
            json.append("\"id\":").append(mesa.getMesaId() != null ? mesa.getMesaId() : "null").append(',');
            json.append("\"province\":\"").append(escaparJson(mesa.getProvincia())).append("\",");
            json.append("\"canton\":\"").append(escaparJson(mesa.getCanton())).append("\",");
            json.append("\"parroquia\":\"").append(escaparJson(mesa.getParroquia())).append("\",");
            json.append("\"recinto\":\"").append(escaparJson(mesa.getRecinto())).append("\",");
            json.append("\"mesa\":\"").append(escaparJson(mesa.getMesa())).append("\",");
            json.append("\"sufragantes\":").append(mesa.getSufragantesAsignados() != null ? mesa.getSufragantesAsignados() : 0).append(',');
            json.append("\"votes\":").append(mesa.getVotosRegistrados() != null ? mesa.getVotosRegistrados() : 0).append(',');
            json.append("\"closedAt\":\"").append(formatearFechaIso(mesa.getFechaCierre())).append("\"");
            json.append('}');
        }
        json.append(']');
        json.append('}');
        return json.toString();
    }

    private String toNumero(BigDecimal numero) {
        return numero != null ? numero.toPlainString() : "0";
    }

    private String formatearFechaIso(Date fecha) {
        if (fecha == null) {
            return "";
        }
        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        formato.setTimeZone(TimeZone.getDefault());
        return formato.format(fecha);
    }

    private String escaparJson(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
