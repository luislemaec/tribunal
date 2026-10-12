package ec.com.antenasur.api;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import ec.com.antenasur.api.ContratosApi.SolicitudDispositivo;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.service.tec.DispositivoMovilService;

/**
 * Registro del dispositivo para las notificaciones push (docs/api-movil.md). La App lo
 * llama al iniciar sesión y al renovarse el token de Firebase, y da de baja al cerrar sesión.
 */
@Path("/dispositivos")
@RequestScoped
@Produces(ApiMovilAplicacion.JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DispositivoRecurso {

    @Inject
    private DispositivoMovilService dispositivos;

    @POST
    public Response registrar(SolicitudDispositivo solicitud) {
        try {
            dispositivos.registrar(solicitud != null ? solicitud.getToken() : null,
                    solicitud != null ? solicitud.getPlataforma() : null);
            return Response.noContent().build();
        } catch (NegocioException e) {
            return AutenticacionRecurso.error(Response.Status.BAD_REQUEST, "SOLICITUD_INVALIDA", e.getMessage());
        }
    }

    @POST
    @Path("/baja")
    public Response darDeBaja(SolicitudDispositivo solicitud) {
        dispositivos.darDeBaja(solicitud != null ? solicitud.getToken() : null);
        return Response.noContent().build();
    }
}
