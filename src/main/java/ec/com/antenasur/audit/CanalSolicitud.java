package ec.com.antenasur.audit;

/**
 * Canal de la petición en curso, para la auditoría. {@code FiltroApiMovil} marca cada petición
 * de la API móvil ({@code /api/*}) y la desmarca al terminar; las llamadas EJB locales corren
 * en el mismo hilo, así que la bitácora automática de {@code AbstractFacade} sabe que el cambio
 * vino de la App sin tocar ninguna regla de negocio. Sin marca: web o proceso interno.
 */
public final class CanalSolicitud {

    /** Texto con el que la bitácora identifica a la App. */
    public static final String APP_MOVIL = "App móvil";

    /** IP del cliente de la App; null fuera de una petición de la API. */
    private static final ThreadLocal<String> APP = new ThreadLocal<>();

    private CanalSolicitud() {
    }

    /** Marca la petición en curso como de la App móvil. */
    public static void marcarApp(String ip) {
        APP.set(ip == null ? "" : ip);
    }

    /** Quita la marca (siempre en un finally: los hilos del servidor se reutilizan). */
    public static void limpiar() {
        APP.remove();
    }

    public static boolean esApp() {
        return APP.get() != null;
    }

    /** IP del cliente de la App, o null si la petición no viene de la App. */
    public static String ipApp() {
        String ip = APP.get();
        return ip == null || ip.isEmpty() ? null : ip;
    }
}
