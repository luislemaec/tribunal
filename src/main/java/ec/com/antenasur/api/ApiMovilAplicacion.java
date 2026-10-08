package ec.com.antenasur.api;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;

/**
 * API REST para la App móvil CONPOCIIECH (docs/api-movil.md). La protege
 * {@link ec.com.antenasur.security.api.FiltroApiMovil}.
 */
@ApplicationPath("/api/v1")
public class ApiMovilAplicacion extends Application {

    /** JSON con charset explícito: sin él algunos clientes (p. ej. PowerShell 5.1) leen Latin-1. */
    public static final String JSON = MediaType.APPLICATION_JSON + ";charset=UTF-8";
}
