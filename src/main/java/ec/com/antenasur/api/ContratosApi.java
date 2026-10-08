package ec.com.antenasur.api;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Cuerpos JSON de la API móvil (docs/api-movil.md). Sin datos sensibles más allá de los tokens. */
public final class ContratosApi {

    private ContratosApi() {
    }

    @Data
    @NoArgsConstructor
    public static class SolicitudLogin {
        private String usuario;
        private String clave;
        private String dispositivo;
    }

    @Data
    @NoArgsConstructor
    public static class SolicitudRefresh {
        private String refreshToken;
    }

    @Data
    @NoArgsConstructor
    public static class SolicitudCambioClave {
        private String claveActual;
        private String claveNueva;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerfilUsuario {
        private String usuario;
        private String nombre;
        private List<String> roles;
        private Integer iglesiaId;
        private boolean cambioClaveObligatorio;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RespuestaSesion {
        private String accessToken;
        private String refreshToken;
        private String tipo;
        private long expiraEnSegundos;
        private boolean cambioClaveObligatorio;
        private PerfilUsuario usuario;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorApi {
        private String codigo;
        private String mensaje;
    }
}
