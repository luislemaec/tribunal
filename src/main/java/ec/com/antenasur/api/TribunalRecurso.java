package ec.com.antenasur.api;

import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import ec.com.antenasur.api.ContratosApi.SolicitudHabilitacion;
import ec.com.antenasur.dto.ConsultaMovilDTO.Elector;
import ec.com.antenasur.exception.IglesiaPersonaException;
import ec.com.antenasur.service.tec.ConsultaMovilService;
import ec.com.antenasur.service.tec.GestionMovilService;
import ec.com.antenasur.util.Constantes;

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

    @Inject
    private GestionMovilService gestion;

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

    /** Habilita o deshabilita a un miembro de la iglesia del IglesiaAdmin (marca revisado). */
    @PUT
    @Path("/iglesia/miembros/{id}/habilitacion")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response cambiarHabilitacion(@PathParam("id") Integer id, SolicitudHabilitacion solicitud) {
        if (id == null || solicitud == null || solicitud.getHabilitado() == null) {
            return AutenticacionRecurso.error(Response.Status.BAD_REQUEST, "SOLICITUD_INVALIDA",
                    "Indique si el miembro queda habilitado.");
        }
        try {
            return Response.ok(gestion.cambiarHabilitacionMiembro(id, solicitud.getHabilitado())).build();
        } catch (IglesiaPersonaException e) {
            String mensaje = Constantes.getMensaje(e.getMessageKey(), e.getArguments());
            return switch (e.getMessageKey()) {
                case "form.personas.error.cronograma" ->
                    AutenticacionRecurso.error(Response.Status.CONFLICT, "EDICION_CERRADA", mensaje);
                case "form.personas.error.iglesia.no.autorizada" ->
                    AutenticacionRecurso.error(Response.Status.FORBIDDEN, "SIN_PERMISO", mensaje);
                case "form.personas.error.miembro.noDisponible" ->
                    AutenticacionRecurso.error(Response.Status.NOT_FOUND, "MIEMBRO_NO_DISPONIBLE", mensaje);
                default -> AutenticacionRecurso.error(Response.Status.BAD_REQUEST, "SOLICITUD_INVALIDA", mensaje);
            };
        }
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
