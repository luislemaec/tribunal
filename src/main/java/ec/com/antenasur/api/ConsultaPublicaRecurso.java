package ec.com.antenasur.api;

import java.text.SimpleDateFormat;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import ec.com.antenasur.api.ContratosApi.DesafioApi;
import ec.com.antenasur.api.ContratosApi.LugarVotacionApi;
import ec.com.antenasur.api.ContratosApi.RespuestaLugarVotacion;
import ec.com.antenasur.api.ContratosApi.SolicitudLugarVotacion;
import ec.com.antenasur.dto.LugarVotacionPublicoDTO;
import ec.com.antenasur.service.tec.ConsultaLugarVotacionService;
import ec.com.antenasur.service.tec.ConsultaLugarVotacionService.Resultado;
import ec.com.antenasur.service.tec.ConsultaLugarVotacionService.Verificacion;
import ec.com.antenasur.service.tec.DesafioAntiRobot;
import ec.com.antenasur.util.Constantes;
import lombok.extern.slf4j.Slf4j;

/**
 * Consulta pública del lugar de votación desde la App (docs/api-movil.md), sin sesión. Usa el
 * mismo servicio que la página web consultar.jsf: solo cédula exacta, proceso activo, iniciales
 * enmascaradas y sin iglesia; mismo desafío anti-robot (la App lo resuelve), mismos límites por IP
 * y global, y la misma caché. Es pública en {@code FiltroApiMovil.RUTAS_PUBLICAS}.
 */
@Path("/publico/lugar-votacion")
@RequestScoped
@Produces(ApiMovilAplicacion.JSON)
@Slf4j
public class ConsultaPublicaRecurso {

    @Inject
    private ConsultaLugarVotacionService consulta;

    @Context
    private HttpServletRequest peticion;

    /** Desafío nuevo; la App lo resuelve antes de consultar. */
    @GET
    @Path("/desafio")
    public Response desafio() {
        DesafioAntiRobot.Desafio d = consulta.nuevoDesafio();
        return Response.ok(new DesafioApi(d.algoritmo(), d.challenge(), d.salt(), d.firma(), d.maximo())).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response consultar(SolicitudLugarVotacion solicitud) {
        if (solicitud == null) {
            return AutenticacionRecurso.error(Response.Status.BAD_REQUEST, "SOLICITUD_INVALIDA",
                    Constantes.getMensaje("consultaLugar.mensaje.cedula.requerida"));
        }
        Resultado resultado;
        try {
            resultado = consulta.consultar(solicitud.getCedula(), ip(), new Verificacion(solicitud.getChallenge(),
                    solicitud.getSalt(), solicitud.getFirma(), solicitud.getNumero(), null));
        } catch (RuntimeException e) {
            log.error("Error en la consulta pública de lugar de votación desde la App", e);
            return AutenticacionRecurso.error(Response.Status.INTERNAL_SERVER_ERROR, "ERROR_INTERNO",
                    Constantes.getMensaje("consultaLugar.mensaje.error"));
        }
        return respuesta(resultado);
    }

    private static Response respuesta(Resultado resultado) {
        Salida salida = traducir(resultado);
        return Response.status(salida.http()).entity(salida.cuerpo()).build();
    }

    /** Código HTTP y cuerpo JSON de la respuesta. */
    public record Salida(int http, Object cuerpo) {
    }

    /** Traducción del resultado del servicio a HTTP; pura, para probarla sin contenedor JAX-RS. */
    public static Salida traducir(Resultado resultado) {
        return switch (resultado.estado()) {
            case ENCONTRADO -> new Salida(200, new RespuestaLugarVotacion("ENCONTRADO", null,
                    lugar(resultado.lugar())));
            case NO_ENCONTRADO -> new Salida(200, new RespuestaLugarVotacion("NO_ENCONTRADO",
                    Constantes.getMensaje("consultaLugar.mensaje.no.encontrado"), null));
            case SIN_PROCESO -> new Salida(200, new RespuestaLugarVotacion("SIN_PROCESO",
                    Constantes.getMensaje("consultaLugar.mensaje.sin.proceso"), null));
            case CEDULA_INVALIDA -> error(400, "CEDULA_INVALIDA", "consultaLugar.mensaje.cedula.invalida");
            case VERIFICACION -> error(400, "VERIFICACION_FALLIDA", "consultaLugar.mensaje.verificacion");
            case LIMITE -> error(429, "DEMASIADAS_CONSULTAS", "consultaLugar.mensaje.limite");
            case OCUPADO -> error(503, "SERVICIO_OCUPADO", "consultaLugar.mensaje.ocupado");
        };
    }

    private static Salida error(int http, String codigo, String clave) {
        return new Salida(http, new ContratosApi.ErrorApi(codigo, Constantes.getMensaje(clave)));
    }

    private static LugarVotacionApi lugar(LugarVotacionPublicoDTO l) {
        String fecha = l.getFechaSufragio() == null ? null
                : new SimpleDateFormat("yyyy-MM-dd").format(l.getFechaSufragio());
        return new LugarVotacionApi(l.getNombreEnmascarado(), l.getProceso(), fecha, l.getRecinto(), l.getMesa(),
                l.getParroquia(), l.getCanton());
    }

    private String ip() {
        return peticion == null ? null : peticion.getRemoteAddr();
    }
}
