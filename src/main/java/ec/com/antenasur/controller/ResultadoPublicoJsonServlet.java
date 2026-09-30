package ec.com.antenasur.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
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
    private static final Set<String> ALLOWED_ORIGINS = new HashSet<>(Arrays.asList(
            "https://resultados.conpociiech.org",
            "http://resultados.conpociiech.org",
            "http://localhost:8080",
            "http://127.0.0.1:8080"
    ));

    @Inject
    private ResultadosPublicosCacheService resultadosPublicosCacheService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ResultadoPublicoSnapshotDTO snapshot = resultadosPublicosCacheService.obtenerSnapshot();
        String datos = datosJson(snapshot);
        String etag = construirEtag(datos);
        if (etag.equals(request.getHeader("If-None-Match"))) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            aplicarHeadersCors(request, response);
            aplicarHeadersCache(response, etag);
            return;
        }

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        aplicarHeadersCors(request, response);
        aplicarHeadersCache(response, etag);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(toJson(snapshot, datos));
        }
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        aplicarHeadersCors(request, response);
        response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, If-None-Match");
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private void aplicarHeadersCors(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Vary", "Origin");
        }
    }

    private void aplicarHeadersCache(HttpServletResponse response, String etag) {
        response.setHeader("Cache-Control", "public, max-age=" + CACHE_SECONDS + ", stale-while-revalidate=30");
        response.setHeader("ETag", etag);
        response.setDateHeader("Expires", System.currentTimeMillis() + (CACHE_SECONDS * 1000L));
        response.setHeader("X-Content-Type-Options", "nosniff");
    }

    /**
     * ETag calculado sobre los datos publicados, sin la hora de generación: la caché se
     * reconstruye cada 30 segundos y, si nada cambió, el navegador recibe un 304.
     */
    private String construirEtag(String datos) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(datos.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(34).append('"');
            for (int i = 0; i < 16; i++) {
                hex.append(String.format("%02x", hash[i]));
            }
            return hex.append('"').toString();
        } catch (NoSuchAlgorithmException e) {
            return "\"" + Integer.toHexString(datos.hashCode()) + "\"";
        }
    }

    private String toJson(ResultadoPublicoSnapshotDTO snapshot, String datos) {
        return "{\"generatedAt\":\"" + formatearFechaIso(snapshot.getUltimaActualizacion()) + "\","
                + datos.substring(1);
    }

    /** Contenido publicado sin la hora de generación; base del cuerpo y del ETag. */
    private String datosJson(ResultadoPublicoSnapshotDTO snapshot) {
        StringBuilder json = new StringBuilder(4096);
        ProcesoElectoral proceso = snapshot.getProcesoActivo();
        json.append('{');
        json.append("\"hasActiveProcess\":").append(proceso != null && proceso.getId() != null).append(',');
        json.append("\"process\":{");
        json.append("\"id\":").append(proceso != null && proceso.getId() != null ? proceso.getId() : "null").append(',');
        json.append("\"name\":\"").append(escaparJson(proceso != null ? proceso.getNombre() : "")).append("\"");
        json.append("},");
        // Los resultados solo se publican desde el fin del sufragio (fase SUFRAGIO del cronograma).
        // Antes, el JSON no lleva votos ni mesas: la página muestra la cuenta regresiva.
        boolean publicado = snapshot.isPublicacionHabilitada(new Date());
        json.append("\"publication\":{");
        json.append("\"available\":").append(publicado).append(',');
        json.append("\"votingStart\":\"").append(formatearFechaIso(snapshot.getInicioSufragio())).append("\",");
        json.append("\"votingEnd\":\"").append(formatearFechaIso(snapshot.getFinSufragio())).append("\"");
        json.append("},");
        if (!publicado) {
            json.append("\"summary\":{\"totalMesas\":").append(snapshot.getTotalMesasProceso())
                    .append(",\"mesasCerradas\":0,\"mesasPendientes\":").append(snapshot.getTotalMesasProceso())
                    .append(",\"totalVotosListas\":0,\"validVotes\":0,\"blankVotes\":0,\"nullVotes\":0,\"totalVotes\":0")
                    .append(",\"porcentajeMesasCerradas\":0,\"porcentajeMesasCerradasEntero\":0},");
            json.append("\"results\":[],\"tables\":[]}");
            return json.toString();
        }
        json.append("\"summary\":{");
        json.append("\"totalMesas\":").append(snapshot.getTotalMesasProceso()).append(',');
        json.append("\"mesasCerradas\":").append(snapshot.getTotalMesasCerradas()).append(',');
        json.append("\"mesasPendientes\":").append(Math.max(snapshot.getTotalMesasProceso() - snapshot.getTotalMesasCerradas(), 0L)).append(',');
        json.append("\"totalVotosListas\":").append(snapshot.getTotalVotosRegistrados()).append(',');
        // Válidos = suma de las listas; blancos y nulos son informativos (no pertenecen a ninguna lista).
        json.append("\"validVotes\":").append(snapshot.getTotalVotosRegistrados()).append(',');
        json.append("\"blankVotes\":").append(snapshot.getTotalVotosBlancos()).append(',');
        json.append("\"nullVotes\":").append(snapshot.getTotalVotosNulos()).append(',');
        json.append("\"totalVotes\":").append(snapshot.getTotalVotosRegistrados() + snapshot.getTotalVotosBlancos()
                + snapshot.getTotalVotosNulos()).append(',');
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
            json.append("\"number\":\"").append(escaparJson(item.getListaNumero())).append("\",");
            json.append("\"listName\":\"").append(escaparJson(item.getListaNombre())).append("\",");
            json.append("\"slogan\":\"").append(escaparJson(item.getListaSlogan())).append("\",");
            json.append("\"order\":").append(item.getOrden() != null ? item.getOrden() : "null").append(',');
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
            // votes = votos emitidos (válidos, blancos y nulos); las papeletas no utilizadas no suman.
            json.append("\"votes\":").append(mesa.getVotosRegistrados() != null ? mesa.getVotosRegistrados() : 0).append(',');
            json.append("\"validVotes\":").append(mesa.getVotosValidos() != null ? mesa.getVotosValidos() : 0).append(',');
            json.append("\"blankVotes\":").append(mesa.getVotosBlancos() != null ? mesa.getVotosBlancos() : 0).append(',');
            json.append("\"nullVotes\":").append(mesa.getVotosNulos() != null ? mesa.getVotosNulos() : 0).append(',');
            json.append("\"closedAt\":\"").append(formatearFechaIso(mesa.getFechaCierre())).append("\",");
            json.append("\"validatedAt\":\"").append(formatearFechaIso(mesa.getFechaValidacion())).append("\",");
            // Votos de cada lista en la mesa: {"id de la lista en results": votos}.
            json.append("\"listVotes\":{");
            boolean primero = true;
            for (java.util.Map.Entry<Integer, Long> voto : mesa.getVotosPorLista().entrySet()) {
                if (!primero) {
                    json.append(',');
                }
                primero = false;
                json.append('"').append(voto.getKey()).append("\":").append(voto.getValue() != null ? voto.getValue() : 0L);
            }
            json.append('}');
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
