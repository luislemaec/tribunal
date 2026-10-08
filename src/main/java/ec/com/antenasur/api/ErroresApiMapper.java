package ec.com.antenasur.api;

import jakarta.ejb.EJBAccessException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import ec.com.antenasur.api.ContratosApi.ErrorApi;
import lombok.extern.slf4j.Slf4j;

/**
 * Respuesta JSON uniforme de la API móvil (docs/api-movil.md). Nunca expone trazas ni
 * mensajes internos; el detalle queda solo en el log del servidor.
 */
@Provider
@Slf4j
public class ErroresApiMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable error) {
        if (error instanceof WebApplicationException wae && wae.getResponse().getStatus() < 500) {
            // Errores del protocolo (JSON mal formado, ruta o método inexistente, tipo no soportado).
            int estado = wae.getResponse().getStatus();
            return respuesta(estado, estado == 404 || estado == 405 ? "RECURSO_NO_ENCONTRADO" : "SOLICITUD_INVALIDA",
                    "La solicitud no es válida.");
        }
        if (causa(error, EJBAccessException.class)) {
            return respuesta(Response.Status.FORBIDDEN.getStatusCode(), "SIN_PERMISO",
                    "No tiene permiso para esta operación.");
        }
        log.error("Error no controlado en la API móvil", error);
        return respuesta(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "ERROR_INTERNO",
                "No se pudo completar la solicitud. Inténtelo más tarde.");
    }

    private static boolean causa(Throwable error, Class<? extends Throwable> tipo) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (tipo.isInstance(t)) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }

    private static Response respuesta(int estado, String codigo, String mensaje) {
        return Response.status(estado).type(ApiMovilAplicacion.JSON).entity(new ErrorApi(codigo, mensaje)).build();
    }
}
