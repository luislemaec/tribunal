package ec.com.antenasur.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import ec.com.antenasur.api.ContratosApi.SolicitudApertura;
import ec.com.antenasur.api.ContratosApi.SolicitudConteo;
import ec.com.antenasur.api.ContratosApi.VotoCategoria;
import ec.com.antenasur.dto.ConsultaMovilDTO.ActaPresidente;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.model.tec.Proceso;
import ec.com.antenasur.service.tec.GestionMovilService;
import ec.com.antenasur.service.tec.ProcesoService;
import lombok.extern.slf4j.Slf4j;

/**
 * Escrutinio de la mesa del Presidente desde la App móvil (docs/api-movil.md): apertura,
 * conteo, cierre y foto del acta física. Solo en línea; la mesa la resuelve TEC desde la
 * designación JRV y {@link GestionMovilService} exige el rol {@code SITEC-Presidente-mesa}.
 * Una regla de negocio incumplida responde 409 {@code REGLA_ESCRUTINIO} con el mensaje de TEC.
 */
@Path("/tribunal/presidente/acta")
@RequestScoped
@Produces(ApiMovilAplicacion.JSON)
@Slf4j
public class EscrutinioRecurso {

    /** Mismo tope que ActaFisicaEscrutinioService. */
    static final int TAMANO_MAXIMO_ACTA = 10 * 1024 * 1024;

    @Inject
    private GestionMovilService gestion;

    @Inject
    private ProcesoService procesoService;

    @Context
    private HttpServletRequest peticion;

    @GET
    public Response acta() {
        return ejecutar(gestion::actaPresidente, null);
    }

    @POST
    @Path("/apertura")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response abrir(SolicitudApertura solicitud) {
        String observacion = solicitud != null ? solicitud.getObservacion() : null;
        return ejecutar(() -> gestion.abrirMesa(observacion), "APERTURA MESA");
    }

    @PUT
    @Path("/conteo")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response guardarConteo(SolicitudConteo solicitud) {
        Map<Integer, Integer> votos = votos(solicitud);
        if (votos == null) {
            return solicitudInvalida();
        }
        return ejecutar(() -> gestion.guardarConteo(votos), "GUARDA BORRADOR ACTA MESA");
    }

    @POST
    @Path("/cierre")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response cerrar(SolicitudConteo solicitud) {
        Map<Integer, Integer> votos = votos(solicitud);
        if (votos == null) {
            return solicitudInvalida();
        }
        return ejecutar(() -> gestion.cerrarMesa(votos), "CIERRE MESA");
    }

    @PUT
    @Path("/acta-fisica")
    @Consumes("image/jpeg")
    public Response subirActaFisica(byte[] jpeg) {
        if (jpeg == null || jpeg.length == 0) {
            return AutenticacionRecurso.error(Response.Status.BAD_REQUEST, "SOLICITUD_INVALIDA",
                    "Adjunte la foto del acta física.");
        }
        if (jpeg.length > TAMANO_MAXIMO_ACTA) {
            return AutenticacionRecurso.error(Response.Status.REQUEST_ENTITY_TOO_LARGE, "ARCHIVO_DEMASIADO_GRANDE",
                    "La foto del acta supera los 10 MB.");
        }
        return ejecutar(() -> gestion.subirActaFisica(jpeg), "CARGA ACTA FISICA MESA");
    }

    // ------------------------------------------------------------------

    /**
     * Ejecuta la operación y, si modifica datos, la registra en la bitácora con el mismo
     * formato que actaE (el autor sale del principal EJB).
     */
    private Response ejecutar(Supplier<ActaPresidente> operacion, String actividad) {
        ActaPresidente acta;
        try {
            acta = operacion.get();
        } catch (NegocioException e) {
            return AutenticacionRecurso.error(Response.Status.CONFLICT, "REGLA_ESCRUTINIO", e.getMessage());
        }
        if (actividad != null) {
            registrar(actividad + " " + acta.getUbicacion().getMesa() + " (App móvil) | "
                    + acta.getUbicacion().getMesaId());
        }
        return Response.ok(acta).build();
    }

    private void registrar(String actividad) {
        try {
            Proceso proceso = new Proceso(peticion.getRemoteAddr());
            proceso.setActividad(actividad);
            procesoService.create(proceso);
        } catch (Exception e) {
            log.error("No se pudo registrar la actividad de escrutinio de la App móvil", e);
        }
    }

    /** Votos por categoría; {@code null} si faltan datos o hay categorías repetidas. */
    private static Map<Integer, Integer> votos(SolicitudConteo solicitud) {
        List<VotoCategoria> lista = solicitud != null ? solicitud.getVotos() : null;
        if (lista == null || lista.isEmpty()) {
            return null;
        }
        Map<Integer, Integer> votos = new HashMap<>();
        for (VotoCategoria v : lista) {
            if (v == null || v.getCategoriaId() == null || v.getVotos() == null
                    || votos.putIfAbsent(v.getCategoriaId(), v.getVotos()) != null) {
                return null;
            }
        }
        return votos;
    }

    private static Response solicitudInvalida() {
        return AutenticacionRecurso.error(Response.Status.BAD_REQUEST, "SOLICITUD_INVALIDA",
                "Registre los votos de cada categoría una sola vez.");
    }
}
