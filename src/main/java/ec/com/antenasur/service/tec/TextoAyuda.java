package ec.com.antenasur.service.tec;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Normalización de texto del chatbot de ayuda. Indexación y búsqueda usan la misma
 * forma (minúsculas y sin acentos), así «inscripcion» encuentra «inscripción» sin la
 * extensión unaccent de PostgreSQL.
 */
final class TextoAyuda {

    private static final Pattern MARCAS = Pattern.compile("\\p{M}+");
    private static final Pattern DIGITOS_LARGOS = Pattern.compile("\\d{6,}");
    private static final Pattern CORREO = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    private static final Pattern NO_PALABRA = Pattern.compile("[^a-z0-9]+");

    /** Longitud máxima guardada de una consulta sin respuesta. */
    static final int MAX_SIN_RESPUESTA = 200;

    private TextoAyuda() {
    }

    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinAcentos = MARCAS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("");
        return sinAcentos.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    /** Texto indexado: pregunta, palabras clave y respuesta normalizadas. */
    static String textoBusqueda(String pregunta, String palabrasClave, String respuesta) {
        String texto = normalizar(pregunta) + " " + normalizar(palabrasClave) + " " + normalizar(respuesta);
        return texto.length() > 5000 ? texto.substring(0, 5000) : texto.trim();
    }

    /** Consulta OR para to_tsquery: solo palabras [a-z0-9], unidas con «|». Vacía si no hay palabras. */
    static String consultaAlternativa(String normalizado) {
        return Arrays.stream(NO_PALABRA.split(normalizado)).filter(p -> p.length() >= 3).distinct().limit(10)
                .collect(Collectors.joining(" | "));
    }

    /**
     * Texto seguro para guardar como consulta sin respuesta: sin cédulas, teléfonos ni
     * correos (por si el usuario los escribe en el buscador), normalizado y truncado.
     */
    static String paraSinRespuesta(String texto) {
        String limpio = CORREO.matcher(texto == null ? "" : texto).replaceAll("[correo]");
        limpio = DIGITOS_LARGOS.matcher(limpio).replaceAll("######");
        limpio = normalizar(limpio);
        return limpio.length() > MAX_SIN_RESPUESTA ? limpio.substring(0, MAX_SIN_RESPUESTA) : limpio;
    }
}
