package ec.com.antenasur.push;

/**
 * Configuración de las notificaciones push (docs/api-movil.md, «Notificaciones»). Todo está
 * desactivado por defecto: sin {@code -Dtec.push.enabled=true} TEC no encola ni envía nada.
 *
 * <ul>
 * <li>{@code tec.push.enabled}: activa el encolado y el envío.</li>
 * <li>{@code tec.push.credenciales}: ruta del JSON de la cuenta de servicio de Firebase.
 * Es un secreto: va fuera del repositorio y del WAR, legible solo por el usuario de WildFly.</li>
 * </ul>
 */
public final class ConfiguracionPush {

    public static final String PROPIEDAD_ACTIVA = "tec.push.enabled";
    public static final String PROPIEDAD_CREDENCIALES = "tec.push.credenciales";

    private ConfiguracionPush() {
    }

    public static boolean activa() {
        return Boolean.parseBoolean(System.getProperty(PROPIEDAD_ACTIVA, "false"));
    }

    public static String rutaCredenciales() {
        String ruta = System.getProperty(PROPIEDAD_CREDENCIALES);
        return ruta == null || ruta.isBlank() ? null : ruta.trim();
    }
}
