package ec.com.antenasur.push;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

/**
 * Cliente mínimo de Firebase Cloud Messaging (API HTTP v1) sin dependencias adicionales:
 * firma el JWT de la cuenta de servicio (RS256) para obtener un token OAuth2 y envía cada
 * mensaje a un token de dispositivo. Nunca registra tokens, credenciales ni contenido.
 */
public class ClienteFcm {

    static final String ALCANCE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final Duration ESPERA = Duration.ofSeconds(15);

    /** Resultado de un envío: el token inválido se desactiva; el resto se reintenta. */
    public enum Resultado {
        ENVIADO, TOKEN_INVALIDO, REINTENTAR
    }

    private final HttpClient http;
    private final String proyecto;
    private final String cuenta;
    private final String urlToken;
    private final PrivateKey clave;
    private String tokenAcceso;
    private Instant tokenVence = Instant.EPOCH;

    ClienteFcm(HttpClient http, String credencialesJson) throws GeneralSecurityException {
        JsonObject c = Json.createReader(new StringReader(credencialesJson)).readObject();
        this.http = http;
        this.proyecto = c.getString("project_id");
        this.cuenta = c.getString("client_email");
        this.urlToken = c.getString("token_uri", "https://oauth2.googleapis.com/token");
        this.clave = clavePrivada(c.getString("private_key"));
    }

    /** Lee la cuenta de servicio de la ruta configurada. */
    public static ClienteFcm desdeArchivo(String ruta) throws IOException, GeneralSecurityException {
        return new ClienteFcm(HttpClient.newBuilder().connectTimeout(ESPERA).build(),
                Files.readString(Path.of(ruta), StandardCharsets.UTF_8));
    }

    public String proyecto() {
        return proyecto;
    }

    /** Envía un aviso a un dispositivo. {@code ruta} viaja como dato para abrir la pantalla. */
    public synchronized Resultado enviar(String tokenDispositivo, String titulo, String cuerpo, String ruta)
            throws IOException, InterruptedException, GeneralSecurityException {
        JsonObjectBuilder datos = Json.createObjectBuilder();
        if (ruta != null) {
            datos.add("ruta", ruta);
        }
        JsonObject mensaje = Json.createObjectBuilder().add("message", Json.createObjectBuilder()
                .add("token", tokenDispositivo)
                .add("notification", Json.createObjectBuilder().add("title", titulo).add("body", cuerpo))
                .add("data", datos)
                .add("android", Json.createObjectBuilder().add("priority", "HIGH"))
                .add("apns", Json.createObjectBuilder().add("payload",
                        Json.createObjectBuilder().add("aps", Json.createObjectBuilder().add("sound", "default")))))
                .build();
        HttpResponse<String> respuesta = http.send(HttpRequest.newBuilder()
                .uri(URI.create("https://fcm.googleapis.com/v1/projects/" + proyecto + "/messages:send"))
                .timeout(ESPERA)
                .header("Authorization", "Bearer " + tokenAcceso())
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(mensaje.toString(), StandardCharsets.UTF_8))
                .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return clasificar(respuesta.statusCode(), respuesta.body());
    }

    /**
     * Solo se desactiva un token cuando FCM lo declara inválido (UNREGISTERED, o
     * INVALID_ARGUMENT referido al token). Cualquier otro error se reintenta: un fallo del
     * mensaje o de la cuenta no debe desactivar todos los dispositivos.
     */
    static Resultado clasificar(int estado, String cuerpo) {
        if (estado >= 200 && estado < 300) {
            return Resultado.ENVIADO;
        }
        String codigo = codigoError(cuerpo);
        if ("UNREGISTERED".equals(codigo) || (estado == 404 && codigo == null)) {
            return Resultado.TOKEN_INVALIDO;
        }
        if ("INVALID_ARGUMENT".equals(codigo) && cuerpo != null
                && cuerpo.toLowerCase(java.util.Locale.ROOT).contains("registration token")) {
            return Resultado.TOKEN_INVALIDO;
        }
        return Resultado.REINTENTAR;
    }

    private static String codigoError(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            return null;
        }
        try {
            JsonObject error = Json.createReader(new StringReader(cuerpo)).readObject().getJsonObject("error");
            JsonArray detalles = error != null ? error.getJsonArray("details") : null;
            if (detalles != null) {
                for (JsonValue d : detalles) {
                    if (d instanceof JsonObject o && o.containsKey("errorCode")) {
                        return o.getString("errorCode");
                    }
                }
            }
            return error != null && error.containsKey("status") ? error.getString("status") : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Token OAuth2 de la cuenta de servicio, renovado 5 minutos antes de vencer. */
    private String tokenAcceso() throws IOException, InterruptedException, GeneralSecurityException {
        if (tokenAcceso != null && Instant.now().isBefore(tokenVence.minusSeconds(300))) {
            return tokenAcceso;
        }
        String formulario = "grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer",
                StandardCharsets.UTF_8) + "&assertion=" + jwt(Instant.now());
        HttpResponse<String> respuesta = http.send(HttpRequest.newBuilder().uri(URI.create(urlToken)).timeout(ESPERA)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formulario)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (respuesta.statusCode() != 200) {
            throw new IOException("Firebase rechazó la credencial de la cuenta de servicio (HTTP "
                    + respuesta.statusCode() + ").");
        }
        JsonObject json = Json.createReader(new StringReader(respuesta.body())).readObject();
        tokenAcceso = json.getString("access_token");
        tokenVence = Instant.now().plusSeconds(json.getJsonNumber("expires_in").longValue());
        return tokenAcceso;
    }

    /** JWT firmado con la clave de la cuenta de servicio (RS256), válido una hora. */
    String jwt(Instant ahora) throws GeneralSecurityException {
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        String cabecera = b64.encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String reclamos = b64.encodeToString(Json.createObjectBuilder()
                .add("iss", cuenta).add("scope", ALCANCE).add("aud", urlToken)
                .add("iat", ahora.getEpochSecond()).add("exp", ahora.getEpochSecond() + 3600)
                .build().toString().getBytes(StandardCharsets.UTF_8));
        Signature firma = Signature.getInstance("SHA256withRSA");
        firma.initSign(clave);
        firma.update((cabecera + "." + reclamos).getBytes(StandardCharsets.US_ASCII));
        return cabecera + "." + reclamos + "." + b64.encodeToString(firma.sign());
    }

    private static PrivateKey clavePrivada(String pem) throws GeneralSecurityException {
        String base64 = pem.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
    }
}
