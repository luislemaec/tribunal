package ec.com.antenasur.itext;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontProvider;
import com.itextpdf.text.pdf.BaseFont;

/**
 * Tokens tipográficos y de color de todos los PDF del sistema: fuentes, pesos, colores y estilos por
 * función. Los generadores piden un estilo ({@code TipografiaPdf.Estilo.SECCION.fuente(10)}) en lugar de
 * repetir combinaciones de fuente, peso y color.
 *
 * <p>Jerarquía: institución → tipo de documento → sección → etiqueta → valor → metadato. Pesos de
 * Montserrat disponibles: Regular 400 (texto, valores, metadatos), Medium 500 (secciones, etiquetas,
 * encabezados de tabla y datos destacados; también hace de 600, que no existe en el proyecto) y Bold
 * 700 (títulos de documento). Light 300 solo se usa en la réplica gráfica de la plantilla A4.
 *
 * <p>Las fuentes se leen del classpath ({@code /fonts}) una sola vez por JVM y se incrustan como
 * subconjunto: funciona dentro y fuera de una petición web.
 */
public final class TipografiaPdf {

    /* ---------------------------- Colores ---------------------------- */

    /** Azul institucional: el medido en el diseño oficial de la plantilla A4. */
    public static final BaseColor AZUL = new BaseColor(0x00, 0x43, 0x85);
    /** Texto principal de los documentos. */
    public static final BaseColor TEXTO_PRINCIPAL = BaseColor.BLACK;
    /** Texto secundario: subtítulos, metadatos y pies. */
    public static final BaseColor SECUNDARIO = new BaseColor(90, 100, 110);
    /** Fondo de encabezados de tabla. */
    public static final BaseColor FONDO_CABECERA = new BaseColor(232, 240, 248);
    /** Bordes de tabla y separadores. */
    public static final BaseColor BORDE = new BaseColor(210, 220, 230);
    /** Notas de advertencia (observaciones). */
    public static final BaseColor ALERTA = BaseColor.RED;

    /* --------------------- Tamaños base (puntos) ---------------------- */

    public static final float TAM_TITULO = 15f;
    public static final float TAM_SECCION = 10f;
    public static final float TAM_TEXTO = 9f;
    public static final float TAM_TABLA = 8f;
    public static final float TAM_METADATO = 7.5f;
    public static final float TAM_PIE = 7f;

    /* ----------------------------- Pesos ------------------------------ */

    public enum Peso {
        LIGHT("Montserrat-Light.ttf"),
        REGULAR("Montserrat-Regular.ttf"),
        MEDIUM("Montserrat-Medium.ttf"),
        BOLD("Montserrat-Bold.ttf");

        private final String archivo;

        Peso(String archivo) {
            this.archivo = archivo;
        }

        public BaseFont baseFont() {
            return TipografiaPdf.baseFont(this);
        }
    }

    /* ----------------------- Estilos por función ---------------------- */

    public enum Estilo {
        /** Nombre de la institución en encabezados propios del documento. */
        INSTITUCION(Peso.MEDIUM, AZUL),
        /** Tipo de documento: «ACTA PARCIAL DE ESCRUTINIO», «CERTIFICADO DE VOTACIÓN». */
        TITULO_DOCUMENTO(Peso.BOLD, AZUL),
        /** Proceso u otra línea bajo el título. */
        SUBTITULO(Peso.MEDIUM, SECUNDARIO),
        /** Título de sección dentro del documento. */
        SECCION(Peso.MEDIUM, AZUL),
        /** Rótulo de un dato («Recinto:», «NOMBRES:»). */
        ETIQUETA(Peso.MEDIUM, AZUL),
        /** Dato. */
        VALOR(Peso.REGULAR, TEXTO_PRINCIPAL),
        /** Dato principal (mesa, nombre de la persona, totales). */
        VALOR_DESTACADO(Peso.MEDIUM, TEXTO_PRINCIPAL),
        /** Texto corrido. */
        TEXTO(Peso.REGULAR, TEXTO_PRINCIPAL),
        /** Encabezado de columna. */
        ENCABEZADO_TABLA(Peso.MEDIUM, AZUL),
        /** Celda de tabla. */
        CELDA(Peso.REGULAR, TEXTO_PRINCIPAL),
        /** Fecha, código, usuario que generó el documento. */
        METADATO(Peso.REGULAR, SECUNDARIO),
        /** Observación o advertencia. */
        NOTA(Peso.REGULAR, ALERTA),
        /** Nombre de quien firma. */
        FIRMA(Peso.MEDIUM, TEXTO_PRINCIPAL),
        /** Paginación y pie del documento. */
        PIE(Peso.REGULAR, SECUNDARIO);

        private final Peso peso;
        private final BaseColor color;

        Estilo(Peso peso, BaseColor color) {
            this.peso = peso;
            this.color = color;
        }

        public Font fuente(float tamano) {
            return TipografiaPdf.fuente(peso, tamano, color);
        }

        public Font fuente(float tamano, BaseColor otroColor) {
            return TipografiaPdf.fuente(peso, tamano, otroColor);
        }

        public Peso peso() {
            return peso;
        }
    }

    private static final Map<Peso, BaseFont> FUENTES = new ConcurrentHashMap<>();

    private TipografiaPdf() {
    }

    public static Font fuente(Peso peso, float tamano, BaseColor color) {
        return new Font(baseFont(peso), tamano, Font.NORMAL, color);
    }

    /** Una {@link BaseFont} por peso y JVM; cada PDF incrusta solo los glifos que usa. */
    public static BaseFont baseFont(Peso peso) {
        return FUENTES.computeIfAbsent(peso, TipografiaPdf::crear);
    }

    private static BaseFont crear(Peso peso) {
        String ruta = "/fonts/" + peso.archivo;
        try (InputStream entrada = TipografiaPdf.class.getResourceAsStream(ruta)) {
            if (entrada == null) {
                throw new IllegalStateException("Fuente institucional no disponible: " + ruta);
            }
            return BaseFont.createFont(peso.archivo, BaseFont.CP1252, BaseFont.EMBEDDED, true,
                    entrada.readAllBytes(), null);
        } catch (IOException | com.itextpdf.text.DocumentException e) {
            throw new IllegalStateException("No se pudo cargar la fuente institucional " + ruta, e);
        }
    }

    /**
     * Proveedor de fuentes para el XMLWorker (actas compuestas desde HTML). Traduce las familias de
     * la hoja de estilo ({@code montsR}, {@code montsSB}, {@code montsB}) a Montserrat; cualquier otra
     * familia cae en Regular, nunca en Helvetica.
     */
    public static FontProvider proveedorHtml() {
        return new FontProvider() {
            @Override
            public boolean isRegistered(String familia) {
                return true;
            }

            @Override
            public Font getFont(String familia, String codificacion, boolean incrustada, float tamano,
                    int estilo, BaseColor color) {
                Peso peso = "montsB".equalsIgnoreCase(familia) ? Peso.BOLD
                        : "montsSB".equalsIgnoreCase(familia) || (estilo & Font.BOLD) != 0 ? Peso.MEDIUM
                        : Peso.REGULAR;
                return fuente(peso, tamano > 0 ? tamano : TAM_TEXTO, color != null ? color : TEXTO_PRINCIPAL);
            }
        };
    }
}
