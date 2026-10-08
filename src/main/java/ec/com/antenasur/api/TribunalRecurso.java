package ec.com.antenasur.api;

import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import ec.com.antenasur.dto.ConsultaMovilDTO.Elector;
import ec.com.antenasur.service.tec.ConsultaMovilService;

/**
 * Módulo Tribunal de la App móvil, solo lectura (docs/api-movil.md). La autorización y el
 * alcance (mesa, iglesia, proceso activo) los aplica {@link ConsultaMovilService} con la
 * identidad del usuario; un rol no autorizado recibe 403 {@code SIN_PERMISO}.
 */
@Path("/tribunal")
@RequestScoped
@Produces(ApiMovilAplicacion.JSON)
public class TribunalRecurso {

    @Inject
    private ConsultaMovilService consultas;

    @GET
    @Path("/presidente/mesa")
    public Response mesaPresidente() {
        return okONoEncontrado(consultas.mesaPresidente(), "SIN_MESA_ASIGNADA",
                "No tiene una mesa asignada como Presidente en el proceso electoral activo.");
    }

    @GET
    @Path("/presidente/padron")
    public Response padronPresidente() {
        List<Elector> padron = consultas.padronPresidente();
        return okONoEncontrado(padron, "SIN_MESA_ASIGNADA",
                "No tiene una mesa asignada como Presidente en el proceso electoral activo.");
    }

    @GET
    @Path("/iglesia")
    public Response iglesia() {
        return okONoEncontrado(consultas.iglesia(), "SIN_IGLESIA_ASIGNADA", "Su usuario no tiene una iglesia asignada.");
    }

    @GET
    @Path("/iglesia/miembros")
    public Response miembros(@QueryParam("busqueda") String busqueda, @QueryParam("habilitado") Boolean habilitado,
            @QueryParam("pagina") @DefaultValue("0") int pagina, @QueryParam("tamano") @DefaultValue("30") int tamano) {
        if (busqueda != null && busqueda.length() > 100) {
            return AutenticacionRecurso.error(Response.Status.BAD_REQUEST, "SOLICITUD_INVALIDA",
                    "La búsqueda es demasiado larga.");
        }
        return Response.ok(consultas.miembros(busqueda, habilitado, pagina, tamano)).build();
    }

    @GET
    @Path("/proceso/resumen")
    public Response resumenProceso() {
        return okONoEncontrado(consultas.resumenProceso(), "SIN_PROCESO_ACTIVO", "No hay un proceso electoral activo.");
    }

    @GET
    @Path("/proceso/mesas")
    public Response avanceMesas() {
        return Response.ok(consultas.avanceMesas()).build();
    }

    @GET
    @Path("/proceso/resultados")
    public Response resultadosProceso() {
        return okONoEncontrado(consultas.resultadosProceso(), "SIN_PROCESO_ACTIVO",
                "No hay un proceso electoral activo.");
    }

    private static Response okONoEncontrado(Object cuerpo, String codigo, String mensaje) {
        return cuerpo != null ? Response.ok(cuerpo).build()
                : AutenticacionRecurso.error(Response.Status.NOT_FOUND, codigo, mensaje);
    }
}
