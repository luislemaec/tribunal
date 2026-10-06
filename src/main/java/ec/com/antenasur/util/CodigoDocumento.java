package ec.com.antenasur.util;

import java.security.SecureRandom;

/**
 * Código corto de documentos oficiales impresos en Code128 (actas): prefijo de dos letras
 * más 10 caracteres de Base 32 de Crockford (unos 50 bits aleatorios).
 *
 * <p>Corto a propósito: con 12 caracteres el Code128 cabe en el encabezado de la plantilla
 * A4 con barras de unos 0,38 mm, legibles por cualquier lector; un UUID (36 caracteres)
 * quedaría en unos 0,13 mm. El alfabeto omite I, L, O y U para evitar confusiones al leerlo.
 */
public final class CodigoDocumento {

    /** Acta de actualización de miembros. */
    public static final String PREFIJO_ACTA_ACTUALIZACION = "AM";
    /** Acta de inscripción de lista. */
    public static final String PREFIJO_ACTA_INSCRIPCION = "AI";

    private static final String ALFABETO = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final int LONGITUD = 10;
    private static final SecureRandom GENERADOR = new SecureRandom();

    private CodigoDocumento() {
    }

    public static String generar(String prefijo) {
        StringBuilder codigo = new StringBuilder(prefijo);
        for (int i = 0; i < LONGITUD; i++) {
            codigo.append(ALFABETO.charAt(GENERADOR.nextInt(ALFABETO.length())));
        }
        return codigo.toString();
    }
}
