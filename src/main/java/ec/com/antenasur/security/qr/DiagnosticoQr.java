package ec.com.antenasur.security.qr;

/** Diagnostico sin mensajes de proveedores, tokens ni credenciales. */
public final class DiagnosticoQr {
    private DiagnosticoQr() { }

    public static String tipoExcepcion(Throwable error) {
        StringBuilder tipos = new StringBuilder();
        for (int i = 0; error != null && i < 8; i++, error = error.getCause()) {
            if (i > 0) tipos.append(" > ");
            tipos.append(error.getClass().getName());
        }
        return tipos.toString();
    }
}
