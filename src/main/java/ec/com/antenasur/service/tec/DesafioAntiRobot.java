package ec.com.antenasur.service.tec;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Desafío anti-robot de prueba de trabajo con el protocolo ALTCHA, implementado en el proyecto
 * (sin servicios externos, sin cookies ni rastreo). El servidor elige un número secreto, publica
 * {@code challenge = SHA-256(salt + número)} firmado con HMAC y el navegador lo encuentra probando
 * números (unos instantes para una persona; costoso a escala para un robot). La verificación
 * comprueba firma, prueba, vencimiento, un tiempo mínimo desde que se emitió y que no se reutilice.
 *
 * <p>Configurable con propiedades del sistema: {@code tec.consulta.desafio.max} (dificultad,
 * 30000) y {@code tec.consulta.desafio.clave} (clave HMAC; si falta, una aleatoria por arranque:
 * los desafíos pendientes caducan al reiniciar, lo que solo obliga a recargar la página).
 */
@ApplicationScoped
public class DesafioAntiRobot {

    static final String ALGORITMO = "SHA-256";
    private static final long VIGENCIA_MS = 10 * 60_000L;
    /** Una persona tarda al menos esto entre abrir la página y consultar. */
    static final long TIEMPO_MINIMO_MS = 1_500L;
    private static final int MAX_USADOS = 100_000;

    /**
     * Datos que la página entrega al navegador. Incluye getters de bean porque el EL de Jakarta
     * EE 10 no resuelve los accesores de los records.
     */
    public record Desafio(String algoritmo, String challenge, String salt, String firma, long maximo) {
        public String getAlgoritmo() {
            return algoritmo;
        }

        public String getChallenge() {
            return challenge;
        }

        public String getSalt() {
            return salt;
        }

        public String getFirma() {
            return firma;
        }

        public long getMaximo() {
            return maximo;
        }
    }

    private final SecureRandom azar = new SecureRandom();
    private final Map<String, Long> usados = new ConcurrentHashMap<>();
    private final byte[] clave;
    private final long maximo;
    private final Clock reloj;

    public DesafioAntiRobot() {
        this(Clock.systemUTC(), System.getProperty("tec.consulta.desafio.clave"),
                Long.getLong("tec.consulta.desafio.max", 30_000L));
    }

    DesafioAntiRobot(Clock reloj, String claveConfigurada, long maximo) {
        this.reloj = reloj;
        this.maximo = Math.max(1_000L, maximo);
        if (claveConfigurada != null && claveConfigurada.length() >= 32) {
            this.clave = claveConfigurada.getBytes(StandardCharsets.UTF_8);
        } else {
            this.clave = new byte[32];
            azar.nextBytes(this.clave);
        }
    }

    public Desafio crear() {
        long ahora = reloj.millis();
        byte[] aleatorio = new byte[12];
        azar.nextBytes(aleatorio);
        String salt = HexFormat.of().formatHex(aleatorio) + "?expires=" + ((ahora + VIGENCIA_MS) / 1000) + "&t=" + ahora;
        long numero = (long) (azar.nextDouble() * (maximo + 1));
        String challenge = sha256(salt + numero);
        return new Desafio(ALGORITMO, challenge, salt, hmac(challenge), maximo);
    }

    /** Verifica la respuesta del navegador; cada desafío se acepta una sola vez. */
    public boolean verificar(String challenge, String salt, String firma, String numero) {
        if (vacio(challenge) || vacio(salt) || vacio(firma) || vacio(numero) || !numero.matches("\\d{1,10}")) {
            return false;
        }
        if (!MessageDigest.isEqual(hmac(challenge).getBytes(StandardCharsets.US_ASCII),
                firma.getBytes(StandardCharsets.US_ASCII))) {
            return false;
        }
        if (!MessageDigest.isEqual(sha256(salt + Long.parseLong(numero)).getBytes(StandardCharsets.US_ASCII),
                challenge.getBytes(StandardCharsets.US_ASCII))) {
            return false;
        }
        long ahora = reloj.millis();
        Long vence = parametro(salt, "expires");
        Long emitido = parametro(salt, "t");
        if (vence == null || emitido == null || ahora > vence * 1000 || ahora - emitido < TIEMPO_MINIMO_MS) {
            return false;
        }
        purgar(ahora);
        if (usados.size() >= MAX_USADOS) {
            return false;
        }
        // putIfAbsent: si dos solicitudes traen el mismo desafío, solo una pasa.
        return usados.putIfAbsent(challenge, vence * 1000) == null;
    }

    private void purgar(long ahora) {
        for (Iterator<Map.Entry<String, Long>> it = usados.entrySet().iterator(); it.hasNext();) {
            if (it.next().getValue() < ahora) {
                it.remove();
            }
        }
    }

    private static Long parametro(String salt, String nombre) {
        int inicio = salt.indexOf('?');
        if (inicio < 0) {
            return null;
        }
        for (String par : salt.substring(inicio + 1).split("&")) {
            String[] kv = par.split("=", 2);
            if (kv.length == 2 && kv[0].equals(nombre) && kv[1].matches("\\d{1,15}")) {
                return Long.parseLong(kv[1]);
            }
        }
        return null;
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank() || valor.length() > 200;
    }

    static String sha256(String texto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String hmac(String texto) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
