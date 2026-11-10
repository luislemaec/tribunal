package ec.com.antenasur.itext;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.Barcode;
import com.itextpdf.text.pdf.Barcode128;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfFunction;
import com.itextpdf.text.pdf.PdfGState;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfShading;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * Plantilla institucional A4 y zona segura de impresión común a todos los
 * documentos PDF del sistema.
 *
 * <p>Reproduce el diseño de {@code docs/diseno/A4TEC.png} (2480 × 3508 px a 300 ppp,
 * A4 exacto) con recursos nativos del PDF: banda y degradado como sombreado
 * vectorial, líneas como rectángulos, textos como texto real en Montserrat y
 * solo los dos logotipos TEC (sin versión vectorial) como imagen, recortados de
 * la plantilla sin pérdida. Se compone una vez por documento en un
 * {@link PdfTemplate} que todas las páginas reutilizan. Las coordenadas se
 * expresan en píxeles de la plantilla de diseño, para poder cotejarlas con ella.
 *
 * <p>Los elementos gráficos fijan el área utilizable:
 *
 * <pre>
 *   banda azul izquierda      x    0 –  209 px  →    0 –  50,2 pt
 *   línea gris vertical       centro x 244,5 px  → 58,68 pt (1 pt de grosor; contenido 4 pt después)
 *   líneas horizontales       x  480 – 2288 px  → 115,2 – 549,1 pt
 *   línea bajo el encabezado  y  577 –  583 px  → 138,5 – 140,0 pt desde arriba
 *   línea del pie             y 3372 – 3378 px  → 32,6 pt desde abajo
 *   texto del pie             hasta y 3428 px   → 19,2 pt desde abajo
 * </pre>
 *
 * <p>Los márgenes se alinean con las líneas horizontales de la plantilla, de
 * modo que el contenido nunca invade la banda azul, el logotipo, el bloque
 * «DOCUMENTO OFICIAL» ni el pie. Son los únicos valores de márgenes A4 del
 * sistema: {@link PdfInstitucional} los reutiliza en lugar de repetirlos.
 */
public final class PlantillaA4 {

    /** Logotipos recortados de docs/diseno/A4TEC.png (docs/herramientas/recortar-plantilla-a4.js), en el classpath. */
    private static final String RUTA_LOGO = "/img/plantilla-a4-logo.png";
    private static final String RUTA_MARCA_AGUA = "/img/plantilla-a4-marca-agua.png";

    /** Línea gris vertical: 1 pt de grosor, centrada donde estaba en la plantilla (px 239–249 → 58,68 pt). */
    private static final float CENTRO_LINEA_VERTICAL = 58.68f;
    private static final float GROSOR_LINEA_VERTICAL = 1f;
    /** Separación entre la línea gris vertical y el inicio del contenido. */
    private static final float SEPARACION_LINEA_VERTICAL = 4f;

    /**
     * Margen izquierdo: 4 pt después del borde derecho de la línea gris vertical
     * (58,68 + 0,5 + 4 = 63,18 pt), que delimita el área de contenido de la plantilla.
     */
    public static final float MARGEN_IZQUIERDO = CENTRO_LINEA_VERTICAL + GROSOR_LINEA_VERTICAL / 2
            + SEPARACION_LINEA_VERTICAL;
    /** Margen derecho: 595,28 pt de ancho A4 menos el fin de esas líneas (549,1 pt). */
    public static final float MARGEN_DERECHO = 46.2f;
    /**
     * Margen superior: la línea del encabezado está a 140 pt del borde y los
     * 16 pt siguientes alojan el título y el código del documento.
     */
    public static final float MARGEN_SUPERIOR = 156f;
    /**
     * Margen inferior: la línea del pie está a 32,6 pt del borde. El contenido
     * arranca en 48 pt y entre ambos queda la franja donde se imprime la
     * paginación.
     */
    public static final float MARGEN_INFERIOR = 48f;

    /** Línea base del texto de paginación, entre la línea del pie y el contenido. */
    private static final float LINEA_PAGINACION = 37f;

    /** Línea base del título y los datos del documento, bajo la línea del encabezado. */
    private static final float LINEA_DATOS = 688f;

    /**
     * Código de barras en el encabezado: se apoya sobre el bloque «DOCUMENTO
     * OFICIAL» de la plantilla, cuyo texto empieza a 748,9 pt.
     */
    private static final float ANCHO_BARRAS = 160f;
    private static final float ALTO_BARRAS = 26f;
    private static final float BASE_BARRAS = 756f;

    /** Acta parcial: módulo de 0,72 pt (≈ 0,25 mm), el mínimo cómodo para lectores, y barras más altas. */
    private static final float MODULO_BARRAS_ACTA = 0.72f;
    private static final float ALTO_BARRAS_ACTA = 32f;

    /** Acta parcial: la fecha va sobre la línea, así que el título arranca 10 pt más arriba. */
    public static final float MARGEN_SUPERIOR_ACTA = 146f;

    /** Borde inferior de la línea del encabezado de la plantilla, desde abajo. */
    public static final float LIMITE_ENCABEZADO = 701.9f;

    /** Borde superior de la línea del pie de la plantilla, desde abajo. */
    public static final float LIMITE_PIE = 32.6f;

    /** Ancho utilizable para tablas e imágenes. */
    public static final float ANCHO_UTIL = PageSize.A4.getWidth() - MARGEN_IZQUIERDO - MARGEN_DERECHO;
    /** Alto utilizable en una página. */
    public static final float ALTO_UTIL = PageSize.A4.getHeight() - MARGEN_SUPERIOR - MARGEN_INFERIOR;

    private static final BaseColor COLOR_TEXTO = new BaseColor(70, 82, 95);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /* ------------------------------------------------------------------
     * Diseño medido sobre docs/diseno/A4TEC.png (no viaja en el WAR). Colores tomados píxel a píxel; la
     * opacidad reproduce el canal alfa de la plantilla (n / 255).
     * ------------------------------------------------------------------ */

    /** Ancho y alto de la plantilla de diseño en píxeles (300 ppp). */
    private static final float DISENO_ANCHO = 2480f;
    private static final float DISENO_ALTO = 3508f;
    /** Puntos por píxel de diseño, igual que el antiguo escalado de la imagen a A4. */
    private static final float PX_X = PageSize.A4.getWidth() / DISENO_ANCHO;
    private static final float PX_Y = PageSize.A4.getHeight() / DISENO_ALTO;

    /* ------------------------------------------------------------------
     * A4 horizontal ({@link #componerHorizontal}): la misma plantilla reacomodada con un
     * encabezado compacto (logotipo más pequeño y código de barras más grande), para dejar
     * más alto a las tablas apaisadas. Va después de PX_X/PX_Y porque los usa.
     * ------------------------------------------------------------------ */

    /** Página A4 apaisada (841,89 × 595,28 pt). */
    public static final float ANCHO_HORIZONTAL = PageSize.A4.rotate().getWidth();
    public static final float ALTO_HORIZONTAL = PageSize.A4.rotate().getHeight();

    /** Lo que se desplazan a la derecha los elementos anclados al margen derecho. */
    private static final float DESPLAZAMIENTO_HORIZONTAL = ANCHO_HORIZONTAL - PageSize.A4.getWidth();

    /** Escala del logotipo con su identificación y del bloque «DOCUMENTO OFICIAL». */
    private static final float ESCALA_IDENTIFICACION_HORIZONTAL = 0.72f;
    private static final float ESCALA_OFICIAL_HORIZONTAL = 0.85f;

    /** Borde superior del logotipo, desde abajo (26 pt bajo el borde de la hoja). */
    private static final float TOPE_ENCABEZADO_HORIZONTAL = ALTO_HORIZONTAL - 26f;

    /** Línea base de «CONPOCIIECH» y de «Elecciones 2026» en el encabezado compacto. */
    private static final float BASE_ENCABEZADO_HORIZONTAL = TOPE_ENCABEZADO_HORIZONTAL
            - ESCALA_IDENTIFICACION_HORIZONTAL * ((DISENO_ALTO - 169) - (DISENO_ALTO - 468)) * PX_Y;

    /** Borde inferior de la línea del encabezado horizontal, desde abajo (≈ 90 pt desde arriba). */
    private static final float LINEA_ENCABEZADO_HORIZONTAL = BASE_ENCABEZADO_HORIZONTAL - 12f;

    /** Margen superior horizontal: 8 pt bajo la línea; los laterales son los del vertical. */
    public static final float MARGEN_SUPERIOR_HORIZONTAL = ALTO_HORIZONTAL - LINEA_ENCABEZADO_HORIZONTAL + 8f;

    /** Margen inferior horizontal: la paginación va bajo la línea del pie, así que el contenido baja hasta ella. */
    public static final float MARGEN_INFERIOR_HORIZONTAL = 40f;

    /** Línea base del texto de contexto, apoyado sobre la línea del encabezado horizontal. */
    private static final float BASE_CONTEXTO_HORIZONTAL = LINEA_ENCABEZADO_HORIZONTAL + 7 * PX_Y + 3f;

    /** Paginación horizontal: centrada entre el lema (hasta x 1314 px) y el contacto, en la línea del pie. */
    private static final float CENTRO_PAGINACION_HORIZONTAL = (1314 * PX_X + 1826 * PX_X + DESPLAZAMIENTO_HORIZONTAL) / 2;
    private static final float BASE_PAGINACION_HORIZONTAL = (DISENO_ALTO - 3421) * PX_Y;

    /** Código de barras horizontal: más ancho y alto que en vertical, alineado con el logotipo. */
    private static final float ANCHO_BARRAS_HORIZONTAL = 250f;
    private static final float ALTO_BARRAS_HORIZONTAL = 30f;
    private static final float BASE_BARRAS_HORIZONTAL = TOPE_ENCABEZADO_HORIZONTAL - ALTO_BARRAS_HORIZONTAL;

    /** Centro de la marca de agua en píxeles de diseño (x 503–2118, y 1378–2186). */
    private static final float CENTRO_MARCA_AGUA_X = 1310.5f;
    private static final float CENTRO_MARCA_AGUA_Y = 1782f;

    /** Texto institucional de la plantilla. */
    private static final BaseColor COLOR_INSTITUCIONAL = new BaseColor(0x00, 0x43, 0x85);
    /** Texto de la marca de agua (#004285 al 9 %). */
    private static final BaseColor COLOR_MARCA_AGUA = new BaseColor(0x00, 0x42, 0x85);
    private static final float OPACIDAD_MARCA_AGUA = 23f / 255f;
    /** Línea vertical gris junto a la banda (#131F27 al 25 %). */
    private static final BaseColor COLOR_LINEA_VERTICAL = new BaseColor(0x13, 0x1F, 0x27);
    private static final float OPACIDAD_LINEA_VERTICAL = 64f / 255f;
    /** Línea del encabezado (#122028, opaca). */
    private static final BaseColor COLOR_LINEA_ENCABEZADO = new BaseColor(0x12, 0x20, 0x28);
    /** Línea del pie (#111F28 al 35 %). */
    private static final BaseColor COLOR_LINEA_PIE = new BaseColor(0x11, 0x1F, 0x28);
    private static final float OPACIDAD_LINEA_PIE = 89f / 255f;

    /**
     * Banda izquierda (0–209 px): #003B8D sólido hasta y = 1800 px y, desde ahí,
     * degradado no lineal hasta #3166AA en el borde inferior. Paradas medidas cada
     * 100 px como promedio de la fila: {y en px, color RGB}.
     */
    private static final int[][] PARADAS_BANDA = {
            {1800, 0x003b8d}, {1900, 0x003d8f}, {2000, 0x003f90}, {2100, 0x004092}, {2200, 0x004293},
            {2300, 0x004494}, {2400, 0x004796}, {2500, 0x004a97}, {2600, 0x004c99}, {2700, 0x044f9b},
            {2800, 0x0c529d}, {2900, 0x14549e}, {3000, 0x1957a0}, {3100, 0x1d5ba2}, {3200, 0x235da4},
            {3300, 0x2760a6}, {3400, 0x2d63a8}, {3508, 0x3166aa}};

    /** Textos de la plantilla, literales (el proceso «Elecciones 2026» es fijo en el diseño). */
    private static final String TEXTO_TRIBUNAL = "TRIBUNAL ELECTORAL";
    private static final String TEXTO_CONPOCIIECH = "CONPOCIIECH";
    private static final String TEXTO_DOCUMENTO_OFICIAL = "DOCUMENTO OFICIAL";
    private static final String TEXTO_PROCESO = "Elecciones 2026";
    private static final String TEXTO_PIE_INSTITUCION = "Tribunal Electoral Conpociiech";
    private static final String TEXTO_PIE_LEMA = "Transparencia y Democracia";
    private static final String TEXTO_PIE_CONTACTO = "Contacto: 098 607 6345";

    /** Logotipos leídos una sola vez por JVM, del classpath (funciona también sin petición web). */
    private static final Map<String, byte[]> RECURSOS = new ConcurrentHashMap<>();

    private PlantillaA4() {
    }

    private static byte[] recurso(String ruta) throws IOException {
        byte[] bytes = RECURSOS.get(ruta);
        if (bytes == null) {
            try (InputStream entrada = PlantillaA4.class.getResourceAsStream(ruta)) {
                if (entrada == null) {
                    throw new IOException("No se encontró el recurso de la plantilla institucional " + ruta);
                }
                bytes = entrada.readAllBytes();
                RECURSOS.put(ruta, bytes);
            }
        }
        return bytes;
    }

    private static float x(float px) {
        return px * PX_X;
    }

    /** Coordenada PDF (desde abajo) del borde superior de la fila {@code px} de la plantilla. */
    private static float y(float px) {
        return (DISENO_ALTO - px) * PX_Y;
    }

    /**
     * Compone la plantilla completa en un {@link PdfTemplate}: se crea una vez por
     * documento y cada página la referencia, de modo que sus recursos (logotipos,
     * fuentes, sombreado) se incrustan una sola vez.
     */
    static PdfTemplate componer(PdfWriter writer) throws Exception {
        float ancho = PageSize.A4.getWidth(), alto = PageSize.A4.getHeight();
        PdfTemplate t = writer.getDirectContent().createTemplate(ancho, alto);

        // Banda izquierda con su degradado.
        t.saveState();
        t.rectangle(0, 0, x(210), alto);
        t.clip();
        t.newPath();
        t.paintShading(sombreadoBanda(writer));
        t.restoreState();

        // Línea vertical gris, línea del encabezado y línea del pie.
        rectangulo(t, CENTRO_LINEA_VERTICAL - GROSOR_LINEA_VERTICAL / 2, 0, GROSOR_LINEA_VERTICAL, alto,
                COLOR_LINEA_VERTICAL, OPACIDAD_LINEA_VERTICAL);
        rectangulo(t, x(480), y(584), x(1809), 7 * PX_Y, COLOR_LINEA_ENCABEZADO, 1f);
        rectangulo(t, x(480), y(3379), x(1809), 7 * PX_Y, COLOR_LINEA_PIE, OPACIDAD_LINEA_PIE);

        marcaAgua(t);
        identificacion(t);
        documentoOficial(t);
        pieInstitucional(t);
        contacto(t);
        return t;
    }

    /**
     * La misma plantilla institucional en A4 horizontal (841,89 × 595,28 pt).
     *
     * <p>Reutiliza cada grupo del diseño vertical sin redibujarlo: el logotipo y su
     * identificación se mantienen arriba a la izquierda, «DOCUMENTO OFICIAL» arriba a la
     * derecha, el pie abajo (el contacto a la derecha), la marca de agua centrada en el área
     * de contenido y la banda con su degradado a lo alto de la página. Las líneas se
     * alargan hasta el margen derecho. El encabezado es compacto (logotipo al 72 % y
     * bloque oficial al 85 %) y su línea queda a unos 90 pt del borde, para ganar filas
     * en las tablas apaisadas.
     */
    static PdfTemplate componerHorizontal(PdfWriter writer) throws Exception {
        float ancho = ANCHO_HORIZONTAL, alto = ALTO_HORIZONTAL;
        PdfTemplate t = writer.getDirectContent().createTemplate(ancho, alto);

        // Banda: el degradado vertical, escalado a la altura apaisada.
        t.saveState();
        t.rectangle(0, 0, x(210), alto);
        t.clip();
        t.newPath();
        t.concatCTM(1, 0, 0, alto / PageSize.A4.getHeight(), 0, 0);
        t.paintShading(sombreadoBanda(writer));
        t.restoreState();

        rectangulo(t, CENTRO_LINEA_VERTICAL - GROSOR_LINEA_VERTICAL / 2, 0, GROSOR_LINEA_VERTICAL, alto,
                COLOR_LINEA_VERTICAL, OPACIDAD_LINEA_VERTICAL);
        float largoLineas = x(1809) + DESPLAZAMIENTO_HORIZONTAL;
        rectangulo(t, x(480), LINEA_ENCABEZADO_HORIZONTAL, largoLineas, 7 * PX_Y, COLOR_LINEA_ENCABEZADO, 1f);
        rectangulo(t, x(480), y(3379), largoLineas, 7 * PX_Y, COLOR_LINEA_PIE, OPACIDAD_LINEA_PIE);

        // Marca de agua centrada en el área de contenido (entre las dos líneas).
        float centroX = (MARGEN_IZQUIERDO + ancho - MARGEN_DERECHO) / 2;
        float centroY = (LINEA_ENCABEZADO_HORIZONTAL + LIMITE_PIE) / 2;
        trasladar(t, centroX - x(CENTRO_MARCA_AGUA_X), centroY - y(CENTRO_MARCA_AGUA_Y), PlantillaA4::marcaAgua);

        // Encabezado compacto: logotipo reducido anclado arriba a la izquierda (su esquina
        // superior izquierda, x 449 px / y 169 px) y bloque oficial anclado a la derecha, con
        // «Elecciones 2026» sobre la misma línea base que «CONPOCIIECH».
        escalar(t, x(449), y(169), MARGEN_IZQUIERDO_LOGO, TOPE_ENCABEZADO_HORIZONTAL,
                ESCALA_IDENTIFICACION_HORIZONTAL, PlantillaA4::identificacion);
        escalar(t, x(2276), y(466), ancho - MARGEN_DERECHO, BASE_ENCABEZADO_HORIZONTAL,
                ESCALA_OFICIAL_HORIZONTAL, PlantillaA4::documentoOficial);

        // Pie: anclado abajo; el contacto, a la derecha.
        pieInstitucional(t);
        trasladar(t, DESPLAZAMIENTO_HORIZONTAL, 0, PlantillaA4::contacto);
        return t;
    }

    /** Grupo de la plantilla dibujado con las coordenadas del diseño vertical. */
    @FunctionalInterface
    private interface Grupo {
        void dibujar(PdfTemplate t) throws Exception;
    }

    private static void trasladar(PdfTemplate t, float dx, float dy, Grupo grupo) throws Exception {
        escalar(t, 0, 0, dx, dy, 1f, grupo);
    }

    /** Dibuja el grupo escalado, llevando su punto de anclaje (origenX, origenY) a (destinoX, destinoY). */
    private static void escalar(PdfTemplate t, float origenX, float origenY, float destinoX, float destinoY,
            float escala, Grupo grupo) throws Exception {
        t.saveState();
        t.concatCTM(escala, 0, 0, escala, destinoX - escala * origenX, destinoY - escala * origenY);
        grupo.dibujar(t);
        t.restoreState();
    }

    /** El logotipo horizontal conserva la posición horizontal del vertical (x 449 px). */
    private static final float MARGEN_IZQUIERDO_LOGO = 449 * PX_X;

    /*
     * Pesos de Montserrat equivalentes a los de la plantilla: Light (300) para «TRIBUNAL
     * ELECTORAL», Medium (500) para los textos regulares y Bold (700) para los destacados.
     */

    /** Marca de agua: logotipo (sus píxeles ya traen la transparencia) e identificación. */
    private static void marcaAgua(PdfTemplate t) throws Exception {
        imagen(t, recurso(RUTA_MARCA_AGUA), 538, 1378, 1580, 516);
        texto(t, TipografiaPdf.Peso.LIGHT.baseFont(), TEXTO_TRIBUNAL, 503, 2093, 2045, 81, COLOR_MARCA_AGUA,
                OPACIDAD_MARCA_AGUA);
        texto(t, TipografiaPdf.Peso.BOLD.baseFont(), TEXTO_CONPOCIIECH, 796, 1796, 2186, 75, COLOR_MARCA_AGUA,
                OPACIDAD_MARCA_AGUA);
    }

    /** Encabezado izquierdo: logotipo e identificación bajo él. */
    private static void identificacion(PdfTemplate t) throws Exception {
        imagen(t, recurso(RUTA_LOGO), 462, 169, 586, 194);
        texto(t, TipografiaPdf.Peso.LIGHT.baseFont(), TEXTO_TRIBUNAL, 449, 1037, 416, 29, COLOR_INSTITUCIONAL, 1f);
        texto(t, TipografiaPdf.Peso.BOLD.baseFont(), TEXTO_CONPOCIIECH, 558, 927, 468, 27, COLOR_INSTITUCIONAL, 1f);
    }

    /** Encabezado derecho: bloque «DOCUMENTO OFICIAL» y proceso. */
    private static void documentoOficial(PdfTemplate t) {
        texto(t, TipografiaPdf.Peso.BOLD.baseFont(), TEXTO_DOCUMENTO_OFICIAL, 1827, 2254, 418, 30,
                COLOR_INSTITUCIONAL, 1f);
        texto(t, TipografiaPdf.Peso.MEDIUM.baseFont(), TEXTO_PROCESO, 1804, 2276, 466, 29, COLOR_INSTITUCIONAL, 1f);
    }

    /** Pie izquierdo: institución • lema. */
    private static void pieInstitucional(PdfTemplate t) {
        texto(t, TipografiaPdf.Peso.BOLD.baseFont(), TEXTO_PIE_INSTITUCION, 480, 856, 3418, 20,
                COLOR_INSTITUCIONAL, 1f);
        t.saveState();
        t.setColorFill(COLOR_INSTITUCIONAL);
        t.circle(x(874.5f), y(3411.5f), 5.5f * PX_X);
        t.fill();
        t.restoreState();
        texto(t, TipografiaPdf.Peso.MEDIUM.baseFont(), TEXTO_PIE_LEMA, 889, 1314, 3421, 18, COLOR_INSTITUCIONAL, 1f);
    }

    /** Pie derecho: contacto. */
    private static void contacto(PdfTemplate t) {
        texto(t, TipografiaPdf.Peso.MEDIUM.baseFont(), TEXTO_PIE_CONTACTO, 1826, 2271, 3424, 18,
                COLOR_INSTITUCIONAL, 1f);
    }


    /** Degradado vertical de la banda como sombreado axial con una función por tramo entre paradas. */
    private static PdfShading sombreadoBanda(PdfWriter writer) {
        int tramos = PARADAS_BANDA.length - 1;
        float inicio = PARADAS_BANDA[0][0], fin = PARADAS_BANDA[tramos][0];
        PdfFunction[] funciones = new PdfFunction[tramos];
        float[] limites = new float[tramos - 1];
        float[] codificacion = new float[tramos * 2];
        for (int i = 0; i < tramos; i++) {
            funciones[i] = PdfFunction.type2(writer, new float[] {0, 1}, null,
                    rgb(PARADAS_BANDA[i][1]), rgb(PARADAS_BANDA[i + 1][1]), 1);
            if (i > 0) {
                limites[i - 1] = (PARADAS_BANDA[i][0] - inicio) / (fin - inicio);
            }
            codificacion[i * 2] = 0;
            codificacion[i * 2 + 1] = 1;
        }
        PdfFunction degradado = PdfFunction.type3(writer, new float[] {0, 1}, null, funciones, limites, codificacion);
        // Eje de y = 1800 px (t = 0) al borde inferior (t = 1); por encima, extendido: color sólido.
        return PdfShading.type2(writer, new BaseColor(0x00, 0x3b, 0x8d), new float[] {0, y(inicio), 0, y(fin)},
                new float[] {0, 1}, degradado, new boolean[] {true, true});
    }

    private static float[] rgb(int color) {
        return new float[] {((color >> 16) & 0xff) / 255f, ((color >> 8) & 0xff) / 255f, (color & 0xff) / 255f};
    }

    private static void rectangulo(PdfTemplate t, float x, float y, float ancho, float alto,
            BaseColor color, float opacidad) {
        t.saveState();
        opacidad(t, opacidad);
        t.setColorFill(color);
        t.rectangle(x, y, ancho, alto);
        t.fill();
        t.restoreState();
    }

    private static void imagen(PdfTemplate t, byte[] png, float xPx, float yPx, float anchoPx, float altoPx)
            throws Exception {
        Image imagen = Image.getInstance(png);
        imagen.scaleAbsolute(anchoPx * PX_X, altoPx * PX_Y);
        imagen.setAbsolutePosition(x(xPx), y(yPx + altoPx));
        t.addImage(imagen);
    }

    /**
     * Texto real en la caja medida en la plantilla: el cuerpo sale de la altura de
     * mayúscula ({@code capPx}), la línea base es la última fila de tinta y el
     * espaciado entre letras se calcula para que la tinta ocupe exactamente de
     * {@code x0Px} a {@code x1Px}, como el texto original.
     */
    private static void texto(PdfTemplate t, BaseFont fuente, String texto, float x0Px, float x1Px,
            float basePx, float capPx, BaseColor color, float opacidad) {
        float altoMayuscula = fuente.getFontDescriptor(BaseFont.CAPHEIGHT, 1000);
        float cuerpo = capPx * PX_Y * 1000f / altoMayuscula;
        char primera = texto.charAt(0), ultima = texto.charAt(texto.length() - 1);
        int[] cajaPrimera = fuente.getCharBBox(primera), cajaUltima = fuente.getCharBBox(ultima);
        float sangria = cajaPrimera != null ? cajaPrimera[0] * cuerpo / 1000f : 0;
        float sobranteFinal = cajaUltima != null
                ? fuente.getWidthPoint(ultima, cuerpo) - cajaUltima[2] * cuerpo / 1000f : 0;
        float tinta = fuente.getWidthPoint(texto, cuerpo) - sangria - sobranteFinal;
        float objetivo = (x1Px - x0Px + 1) * PX_X;
        float espaciado = texto.length() > 1 ? (objetivo - tinta) / (texto.length() - 1) : 0;
        t.saveState();
        opacidad(t, opacidad);
        t.beginText();
        t.setFontAndSize(fuente, cuerpo);
        t.setColorFill(color);
        t.setCharacterSpacing(espaciado);
        t.setTextMatrix(x(x0Px) - sangria, y(basePx + 1));
        t.showText(texto);
        t.endText();
        t.restoreState();
    }

    private static void opacidad(PdfContentByte lienzo, float opacidad) {
        if (opacidad < 1f) {
            PdfGState estado = new PdfGState();
            estado.setFillOpacity(opacidad);
            lienzo.setGState(estado);
        }
    }

    /**
     * Evento que dibuja la plantilla como fondo de cada página y los datos de
     * identificación del documento.
     *
     * <p>El fondo se escribe en la capa inferior ({@code getDirectContentUnder})
     * al cerrar cada página: no consume espacio del documento ni altera el flujo
     * del contenido, y nunca tapa lo ya escrito.
     */
    public static final class Fondo extends PdfPageEventHelper {

        private final String codigoDocumento;
        private final String tituloDocumento;
        private final String subtitulo;
        private final LocalDateTime fechaGeneracion;
        /** Código de barras opcional, impreso sobre el bloque «DOCUMENTO OFICIAL». */
        private final String codigoBarras;
        /**
         * Imprime la línea técnica «título | Código | fecha» bajo el encabezado. Las actas que
         * llevan título y datos en el cuerpo la omiten y conservan el código en el pie.
         */
        private final boolean lineaDatos;
        /** Plantilla compuesta una vez y reutilizada en todas las páginas del documento. */
        private PdfTemplate fondo;
        private Image barras;

        public Fondo(String codigoDocumento, String tituloDocumento, String subtitulo,
                LocalDateTime fechaGeneracion) {
            this(codigoDocumento, tituloDocumento, subtitulo, fechaGeneracion, null);
        }

        public Fondo(String codigoDocumento, String tituloDocumento, String subtitulo,
                LocalDateTime fechaGeneracion, String codigoBarras) {
            this(codigoDocumento, tituloDocumento, subtitulo, fechaGeneracion, codigoBarras, true);
        }

        public Fondo(String codigoDocumento, String tituloDocumento, String subtitulo,
                LocalDateTime fechaGeneracion, String codigoBarras, boolean lineaDatos) {
            this(codigoDocumento, tituloDocumento, subtitulo, fechaGeneracion, codigoBarras, lineaDatos, false);
        }

        /** {@code horizontal}: usa la plantilla A4 apaisada ({@link PlantillaA4#componerHorizontal}). */
        public Fondo(String codigoDocumento, String tituloDocumento, String subtitulo,
                LocalDateTime fechaGeneracion, String codigoBarras, boolean lineaDatos, boolean horizontal) {
            this(codigoDocumento, tituloDocumento, subtitulo, fechaGeneracion, codigoBarras, lineaDatos, horizontal,
                    null);
        }

        /** {@code contexto}: texto repetido en cada página, apoyado sobre la línea del encabezado horizontal. */
        public Fondo(String codigoDocumento, String tituloDocumento, String subtitulo,
                LocalDateTime fechaGeneracion, String codigoBarras, boolean lineaDatos, boolean horizontal,
                String contexto) {
            this.contexto = contexto;
            this.codigoDocumento = codigoDocumento;
            this.tituloDocumento = tituloDocumento;
            this.subtitulo = subtitulo;
            this.fechaGeneracion = fechaGeneracion;
            this.codigoBarras = codigoBarras;
            this.lineaDatos = lineaDatos;
            this.horizontal = horizontal;
        }

        private final boolean horizontal;
        private final String contexto;
        /** Acta parcial: código de barras a tamaño legible y fecha sobre la línea del encabezado. */
        private boolean actaParcial;

        /** Fondo del Acta parcial de escrutinio. */
        public static Fondo actaParcial(LocalDateTime fechaGeneracion, String codigoBarras) {
            Fondo fondo = new Fondo(null, null, null, fechaGeneracion, codigoBarras);
            fondo.actaParcial = true;
            return fondo;
        }

        private float anchoPagina() {
            return horizontal ? ANCHO_HORIZONTAL : PageSize.A4.getWidth();
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                dibujarFondo(writer);
                dibujarCodigoBarras(writer);
                dibujarContexto(writer);
                if (lineaDatos) {
                    dibujarDatosDocumento(writer, document);
                }
                dibujarPaginacion(writer, document);
            } catch (Exception e) {
                // Un fallo de maquetación no debe impedir la emisión del
                // documento: el contenido funcional ya está escrito.
                java.util.logging.Logger.getLogger(PlantillaA4.class.getName())
                        .log(java.util.logging.Level.SEVERE, "No se pudo aplicar la plantilla institucional", e);
            }
        }

        private void dibujarFondo(PdfWriter writer) throws Exception {
            if (fondo == null) {
                fondo = horizontal ? componerHorizontal(writer) : componer(writer);
            }
            // El mismo XObject en todas las páginas: la plantilla se incrusta una
            // sola vez y el peso del PDF no crece con el número de páginas.
            writer.getDirectContentUnder().addTemplate(fondo, 0, 0);
        }

        /**
         * Código de barras del documento sobre el bloque «DOCUMENTO OFICIAL».
         *
         * <p>Esa franja de la plantilla (a partir de 360 pt de ancho y por
         * encima de 749 pt de alto) está libre de gráfica, de modo que el
         * código queda en el encabezado, sin restar espacio al contenido ni
         * taparse con el logotipo.
         */
        private void dibujarCodigoBarras(PdfWriter writer) {
            if (codigoBarras == null || codigoBarras.isBlank()) {
                return;
            }
            if (barras == null) {
                Barcode128 codigo = new Barcode128();
                codigo.setCodeType(Barcode.CODE128);
                codigo.setCode(codigoBarras);
                codigo.setBarHeight(actaParcial ? ALTO_BARRAS_ACTA : 22f);
                codigo.setX(actaParcial ? MODULO_BARRAS_ACTA : 0.9f);
                codigo.setFont(null);
                barras = codigo.createImageWithBarcode(writer.getDirectContent(), BaseColor.BLACK, BaseColor.BLACK);
                // El acta parcial no se escala: el ancho de cada barra es el módulo legible por los lectores.
                if (horizontal) {
                    // Ancho y alto independientes: las barras ganan altura de lectura sin
                    // estrecharse más de lo necesario para caber en el encabezado.
                    barras.scaleAbsolute(Math.min(barras.getWidth(), ANCHO_BARRAS_HORIZONTAL), ALTO_BARRAS_HORIZONTAL);
                } else if (!actaParcial) {
                    barras.scaleToFit(ANCHO_BARRAS, ALTO_BARRAS);
                }
            }
            float x = anchoPagina() - MARGEN_DERECHO - barras.getScaledWidth();
            try {
                barras.setAbsolutePosition(x, horizontal ? BASE_BARRAS_HORIZONTAL : BASE_BARRAS);
                writer.getDirectContent().addImage(barras);
            } catch (Exception e) {
                java.util.logging.Logger.getLogger(PlantillaA4.class.getName())
                        .log(java.util.logging.Level.SEVERE, "No se pudo imprimir el código de barras", e);
            }
        }

        /**
         * Título, código y fecha bajo la línea del encabezado de la plantilla,
         * dentro de la zona segura. Sustituye al logotipo y al banner que antes
         * dibujaba el código, ya presentes en la plantilla.
         */
        private void dibujarDatosDocumento(PdfWriter writer, Document document) {
            PdfContentByte lienzo = writer.getDirectContent();
            Font fuenteTitulo = TipografiaPdf.fuente(TipografiaPdf.Peso.MEDIUM, 9.5f, COLOR_TEXTO);
            Font fuenteDatos = TipografiaPdf.fuente(TipografiaPdf.Peso.REGULAR, TipografiaPdf.TAM_METADATO, COLOR_TEXTO);
            // Misma distancia bajo la línea del encabezado que en vertical.
            float baseTitulo = horizontal ? LINEA_ENCABEZADO_HORIZONTAL - (LIMITE_ENCABEZADO - LINEA_DATOS)
                    : actaParcial ? LIMITE_ENCABEZADO + 7 * PX_Y + 3f : LINEA_DATOS;

            if (tituloDocumento != null && !tituloDocumento.isBlank()) {
                ColumnText.showTextAligned(lienzo, Element.ALIGN_LEFT,
                        new Phrase(tituloDocumento, fuenteTitulo), MARGEN_IZQUIERDO, baseTitulo, 0);
            }
            String datos = subtitulo != null && !subtitulo.isBlank() ? subtitulo : construirDatos();
            if (datos != null && !datos.isBlank()) {
                ColumnText.showTextAligned(lienzo, Element.ALIGN_RIGHT,
                        new Phrase(datos, fuenteDatos), anchoPagina() - MARGEN_DERECHO, baseTitulo, 0);
            }
        }

        private String construirDatos() {
            StringBuilder datos = new StringBuilder();
            if (codigoDocumento != null && !codigoDocumento.isBlank()) {
                datos.append("Código: ").append(codigoDocumento);
            }
            if (fechaGeneracion != null) {
                if (datos.length() > 0) {
                    datos.append("  |  ");
                }
                datos.append(fechaGeneracion.format(FORMATO_FECHA));
            }
            return datos.toString();
        }

        /** Paginación sobre la línea del pie, sin invadir el texto de la plantilla. */
        private void dibujarPaginacion(PdfWriter writer, Document document) {
            Font fuente = TipografiaPdf.fuente(TipografiaPdf.Peso.REGULAR, TipografiaPdf.TAM_PIE, COLOR_TEXTO);
            String texto = "Página " + writer.getPageNumber();
            // El A4 horizontal (padrón) solo pagina: el código queda en el código de barras.
            if (!horizontal && codigoDocumento != null && !codigoDocumento.isBlank()) {
                texto = texto + "  |  Código de validación: " + codigoDocumento;
            }
            if (horizontal) {
                // Bajo la línea del pie, centrada entre el lema y el contacto de la plantilla.
                ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER,
                        new Phrase(texto, fuente), CENTRO_PAGINACION_HORIZONTAL, BASE_PAGINACION_HORIZONTAL, 0);
                return;
            }
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase(texto, fuente), anchoPagina() - MARGEN_DERECHO, LINEA_PAGINACION, 0);
        }

        /** Texto de contexto a la derecha, apoyado sobre la línea del encabezado: no resta alto al contenido. */
        private void dibujarContexto(PdfWriter writer) {
            if (!horizontal || contexto == null || contexto.isBlank()) {
                return;
            }
            // Termina antes del bloque «DOCUMENTO OFICIAL / Elecciones 2026» para no encimarse con él.
            float fin = anchoPagina() - MARGEN_DERECHO - ESCALA_OFICIAL_HORIZONTAL * (2276 - 1804) * PX_X - 12f;
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase(contexto, TipografiaPdf.Estilo.SUBTITULO.fuente(TipografiaPdf.TAM_METADATO)),
                    fin, BASE_CONTEXTO_HORIZONTAL, 0);
        }
    }
}
