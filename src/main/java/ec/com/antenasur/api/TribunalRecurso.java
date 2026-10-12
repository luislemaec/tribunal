package ec.com.antenasur.api;

import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import ec.com.antenasur.api.ContratosApi.SolicitudDevolucionActa;
import ec.com.antenasur.api.ContratosApi.SolicitudHabilitacion;
import ec.com.antenasur.api.ContratosApi.SolicitudValidacionActa;
import ec.com.antenasur.dto.ConsultaMovilDTO.Elector;
import ec.com.antenasur.exception.IglesiaPersonaException;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.model.tec.Proceso;
import ec.com.antenasur.service.tec.ConsultaMovilService;
import ec.com.antenasur.service.tec.GestionMovilService;
import ec.com.antenasur.service.tec.ProcesoService;
import ec.com.antenasur.util.Constantes;
import lombok.extern.slf4j.Slf4j;

/**
 * Módulo Tribunal de la App móvil (docs/api-movil.md): consultas por rol, habilitación de
 * miembros y revisión del acta física. La autorización y el
 * alcance (mesa, iglesia, proceso activo) los aplica {@link ConsultaMovilService} con la
 * identidad del usuario; un rol no autorizado recibe 403 {@code SIN_PERMISO}.
 */
@Path("/tribunal")
@RequestScoped
@Produces(ApiMovilAplicacion.JSON)
@Slf4j
public class TribunalRecurso {

    @Inject
    private ConsultaMovilService consultas;

    @Inject
    private GestionMovilService gestion;

    @Inject
    private ProcesoService procesoService;

    @Context
    private HttpServletRequest peticion;

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
            @QueryParam("revisado") Boolean revisado,
            @QueryParam("pagina") @DefaultValue("0") int pagina, @QueryParam("tamano") @DefaultValue("30") int tamano) {
        if (busqueda != null && busqueda.length() > 100) {
            return AutenticacionRecurso.error(Response.Status.BAD_REQUEST, "SOLICITUD_INVALIDA",
                    "La búsqueda es demasiado larga.");
        }
        return Response.ok(consultas.miembros(busqueda, habilitado, revisado, pagina, tamano)).build();
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

    /** Detalle de una mesa del proceso activo (Administrador, Tribunal). */
    @GET
    @Path("/proceso/mesas/{mesaId}")
    public Response detalleMesa(@PathParam("mesaId") Integer mesaId) {
        return okONoEncontrado(consultas.detalleMesa(mesaId), "MESA_NO_ENCONTRADA",
                "La mesa no existe en el proceso electoral activo.");
    }

    /** Foto del acta física vigente de la mesa, para revisarla. */
    @GET
    @Path("/proceso/mesas/{mesaId}/acta-fisica")
    @Produces({ "image/jpeg", ApiMovilAplicacion.JSON })
    public Response imagenActaFisica(@PathParam("mesaId") Integer mesaId) throws java.io.IOException {
        byte[] imagen = consultas.imagenActaFisica(mesaId);
        if (imagen == null) {
            return AutenticacionRecurso.error(Response.Status.NOT_FOUND, "ACTA_NO_DISPONIBLE",
                    "La mesa no tiene una foto del acta física.");
        }
        registrar("DESCARGA ACTA FISICA (App móvil)", "MESA=" + mesaId);
        return Response.ok(imagen, "image/jpeg").header("Cache-Control", "no-store").build();
    }

    /** Marca como datos oficiales lo registrado, tras confirmar que el acta coincide. */
    @POST
    @Path("/proceso/mesas/{mesaId}/acta-fisica/validacion")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response validarActaFisica(@PathParam("mesaId") Integer mesaId, SolicitudValidacionActa solicitud) {
        if (solicitud == null || !Boolean.TRUE.equals(solicitud.getCotejoConfirmado())) {
            return AutenticacionRecurso.error(Response.Status.BAD_REQUEST, "COTEJO_REQUERIDO",
                    "Confirme que el acta física coincide con lo registrado.");
        }
        return revisarActa(() -> gestion.validarActaFisica(mesaId), mesaId, "VALIDA ACTA FISICA SIN CAMBIOS", null);
    }

    /** Observa o rechaza el acta física con un motivo: la mesa vuelve al Presidente. */
    @POST
    @Path("/proceso/mesas/{mesaId}/acta-fisica/devolucion")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response devolverActaFisica(@PathParam("mesaId") Integer mesaId, SolicitudDevolucionActa solicitud) {
        String estado = solicitud != null ? solicitud.getEstado() : null;
        String motivo = solicitud != null ? solicitud.getMotivo() : null;
        return revisarActa(() -> gestion.devolverActaFisica(mesaId, estado, motivo), mesaId,
                "DEVUELVE ACTA FISICA " + estado, motivo);
    }

    @GET
    @Path("/proceso/resultados")
    public Response resultadosProceso() {
        return okONoEncontrado(consultas.resultadosProceso(), "SIN_PROCESO_ACTIVO",
                "No hay un proceso electoral activo.");
    }

    /** Ejecuta la revisión, la registra en la bitácora (formato de actaE) y devuelve el detalle. */
    private Response revisarActa(java.util.function.Supplier<Integer> revision, Integer mesaId, String actividad,
            String motivo) {
        Integer documentoId;
        try {
            documentoId = revision.get();
        } catch (NegocioException e) {
            return AutenticacionRecurso.error(Response.Status.CONFLICT, "REGLA_REVISION", e.getMessage());
        }
        registrar(actividad + " (App móvil)", "MESA=" + mesaId + ";DOCUMENTO=" + documentoId
                + (motivo != null && !motivo.isBlank() ? ";MOTIVO=" + motivo.trim() : ""));
        return Response.ok(consultas.detalleMesa(mesaId)).build();
    }

    /** Bitácora con el mismo formato que la web; el autor sale del principal EJB. */
    private void registrar(String actividad, String datos) {
        try {
            Proceso proceso = new Proceso(peticion.getRemoteAddr());
            String texto = actividad + " | " + datos;
            proceso.setActividad(texto.length() > 1000 ? texto.substring(0, 1000) : texto);
            procesoService.create(proceso);
        } catch (Exception e) {
            log.error("No se pudo registrar la actividad de revisión de la App móvil", e);
        }
    }

    private static Response okONoEncontrado(Object cuerpo, String codigo, String mensaje) {
        return cuerpo != null ? Response.ok(cuerpo).build()
                : AutenticacionRecurso.error(Response.Status.NOT_FOUND, codigo, mensaje);
    }
}
