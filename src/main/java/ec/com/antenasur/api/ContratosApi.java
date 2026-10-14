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
    public static class SolicitudHabilitacion {
        private Boolean habilitado;
    }

    /** Datos de la persona que el IglesiaAdmin puede cambiar desde la App (los demás se ignoran). */
    @Data
    @NoArgsConstructor
    public static class SolicitudPersona {
        private String nombres;
        private String documento;
        private String sexo;
        /** Opcional: habilitar para el padrón (null = sin cambio). */
        private Boolean habilitado;
    }

    /** El revisor confirma que el acta física coincide con lo registrado. */
    @Data
    @NoArgsConstructor
    public static class SolicitudValidacionActa {
        private Boolean cotejoConfirmado;
    }

    /** {@code estado}: OBSERVADA o RECHAZADA; {@code motivo} obligatorio. */
    @Data
    @NoArgsConstructor
    public static class SolicitudDevolucionActa {
        private String estado;
        private String motivo;
    }

    /** Token de Firebase del dispositivo; {@code plataforma}: android o ios. */
    @Data
    @NoArgsConstructor
    public static class SolicitudDispositivo {
        private String token;
        private String plataforma;
    }

    @Data
    @NoArgsConstructor
    public static class SolicitudApertura {
        private String observacion;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VotoCategoria {
        private Integer categoriaId;
        private Integer votos;
    }

    /** Conteo completo del acta: una entrada por cada categoría. */
    @Data
    @NoArgsConstructor
    public static class SolicitudConteo {
        private List<VotoCategoria> votos;
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
