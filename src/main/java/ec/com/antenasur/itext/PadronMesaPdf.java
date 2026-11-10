package ec.com.antenasur.itext;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;

/**
 * Padrón electoral de la mesa para firma manuscrita: A4 horizontal con el encabezado
 * institucional, los datos de la mesa y la tabla de electores con la columna FIRMA vacía.
 *
 * <p>Recibe las filas ya compuestas (mismos datos y orden que el padrón de la mesa) para no
 * consultar nada por su cuenta. La cabecera de la tabla (con la mesa y el recinto) se repite
 * en cada página y ninguna fila se parte entre páginas.
 */
public final class PadronMesaPdf {

    /**
     * Nro. | Identificación | Nombres completos | Iglesia y comunidad | Firma, en pt sobre el ancho
     * útil de la plantilla horizontal (≈ 732 pt). FIRMA ≈ 49 mm.
     */
    static final float[] ANCHOS = {31f, 86f, 230f, 245.5f, 140f};

    /** Posiciones en la fila de datos de la iglesia y de la comunidad, que comparten celda. */
    private static final int COLUMNA_IGLESIA = 3;
    private static final int COLUMNA_COMUNIDAD = 4;

    /** Columnas con datos; la última (FIRMA) siempre queda vacía. */
    public static final int COLUMNAS_DATOS = 5;

    /** Alto mínimo de fila (≈ 8,5 mm) para firmar a mano. */
    static final float ALTO_FILA = 24f;

    private static final float TAM_CELDA = 8.5f;
    /** Comunidad, en la segunda línea de la celda de la iglesia. */
    private static final float TAM_COMUNIDAD = 7f;
    private static final float TAM_CABECERA = 8f;
    private static final float BORDE = 0.5f;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private PadronMesaPdf() {
    }

    /**
     * Textos del documento, tomados de los mensajes de la aplicación por quien lo invoca.
     *
     * @param titulo      título del documento
     * @param columnas    los cinco encabezados de la tabla (el último es FIRMA)
     * @param datosMesa   pares etiqueta/valor del encabezado (proceso, provincia, …, mesa)
     * @param contextoTabla línea repetida en cada página junto a la línea del encabezado (recinto y mesa)
     * @param pie         línea final «Documento generado por …»
     */
    public record Textos(String titulo, String[] columnas, List<String[]> datosMesa, String contextoTabla,
            String pie) {
    }

    /**
     * Genera el PDF.
     *
     * @param filas  una fila por elector con {@link #COLUMNAS_DATOS} valores, en el orden del padrón
     * @param codigo código institucional del documento (encabezado y pie de página)
     */
    public static byte[] generar(List<String[]> filas, Textos textos, String codigo, LocalDateTime fecha)
            throws DocumentException {
        if (filas == null || filas.isEmpty()) {
            throw new DocumentException("El padrón de la mesa no tiene electores.");
        }
        if (textos.columnas() == null || textos.columnas().length != ANCHOS.length) {
            throw new DocumentException("El padrón requiere " + ANCHOS.length + " columnas.");
        }
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document pdf;
        try {
            pdf = PdfInstitucional.crearA4HorizontalConCodigoBarras(salida, textos.titulo(), fecha, codigo,
                    textos.contextoTabla()).documento();
        } catch (Exception e) {
            throw new DocumentException(e);
        }
        try {
            agregarTitulo(pdf, textos);
            pdf.add(tablaElectores(filas, textos));
            Paragraph pie = new Paragraph(textos.pie(), TipografiaPdf.Estilo.METADATO.fuente(TipografiaPdf.TAM_METADATO));
            pie.setSpacingBefore(8f);
            pie.setAlignment(Element.ALIGN_RIGHT);
            pdf.add(pie);
        } finally {
            pdf.close();
        }
        return salida.toByteArray();
    }

    /** Pie del Excel original: «… Fecha: dd-MM-yyyy Hora: HH:mm:ss». */
    public static String[] fechaHora(LocalDateTime fecha) {
        return new String[] { fecha.format(FECHA), fecha.format(HORA) };
    }

    private static void agregarTitulo(Document pdf, Textos textos) throws DocumentException {
        Paragraph titulo = new Paragraph(textos.titulo(), TipografiaPdf.Estilo.TITULO_DOCUMENTO.fuente(14));
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingAfter(6f);
        pdf.add(titulo);

        // Datos de la mesa en tres pares etiqueta/valor por fila.
        PdfPTable datos = new PdfPTable(6);
        datos.setWidthPercentage(100);
        datos.setWidths(new float[] {90f, 167f, 60f, 197f, 60f, 196f});
        datos.setSpacingAfter(8f);
        Font etiqueta = TipografiaPdf.Estilo.ETIQUETA.fuente(8);
        Font valor = TipografiaPdf.Estilo.VALOR.fuente(8.5f);
        List<String[]> pares = textos.datosMesa() != null ? textos.datosMesa() : List.of();
        int celdas = 0;
        for (String[] par : pares) {
            datos.addCell(celdaDato(par.length > 0 ? par[0] + ":" : "", etiqueta));
            datos.addCell(celdaDato(par.length > 1 ? par[1] : "", valor));
            celdas += 2;
        }
        while (celdas % 6 != 0) {
            datos.addCell(celdaDato("", valor));
            celdas++;
        }
        if (celdas > 0) {
            pdf.add(datos);
        }
    }

    private static PdfPTable tablaElectores(List<String[]> filas, Textos textos) throws DocumentException {
        PdfPTable tabla = new PdfPTable(ANCHOS.length);
        tabla.setWidthPercentage(100);
        tabla.setWidths(ANCHOS);
        tabla.setHeaderRows(1);
        // Una fila que no cabe pasa completa a la página siguiente.
        tabla.setSplitLate(true);
        tabla.setComplete(true);

        Font cabecera = TipografiaPdf.Estilo.ENCABEZADO_TABLA.fuente(TAM_CABECERA);
        for (String columna : textos.columnas()) {
            PdfPCell celda = new PdfPCell(new Phrase(columna.toUpperCase(new java.util.Locale("es", "EC")), cabecera));
            celda.setBackgroundColor(TipografiaPdf.FONDO_CABECERA);
            celda.setBorderColor(TipografiaPdf.BORDE);
            celda.setBorderWidth(BORDE);
            celda.setHorizontalAlignment(Element.ALIGN_CENTER);
            celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
            celda.setMinimumHeight(18f);
            celda.setPadding(3f);
            tabla.addCell(celda);
        }

        Font texto = TipografiaPdf.Estilo.CELDA.fuente(TAM_CELDA);
        Font comunidad = TipografiaPdf.Estilo.CELDA.fuente(TAM_COMUNIDAD);
        for (String[] fila : filas) {
            for (int i = 0; i < COLUMNA_IGLESIA; i++) {
                tabla.addCell(celdaElector(new Phrase(valor(fila, i), texto),
                        i == 0 ? Element.ALIGN_CENTER : Element.ALIGN_LEFT));
            }
            // Iglesia con su peso normal y, en segunda línea, la comunidad en menor tamaño.
            Phrase iglesia = new Phrase(valor(fila, COLUMNA_IGLESIA), texto);
            String nombreComunidad = valor(fila, COLUMNA_COMUNIDAD);
            if (!nombreComunidad.isBlank()) {
                iglesia.add(new Chunk("\n" + nombreComunidad, comunidad));
            }
            tabla.addCell(celdaElector(iglesia, Element.ALIGN_LEFT));
            // FIRMA: siempre en blanco.
            tabla.addCell(celdaElector(new Phrase("", texto), Element.ALIGN_LEFT));
        }
        return tabla;
    }

    private static String valor(String[] fila, int indice) {
        return indice < fila.length && fila[indice] != null ? fila[indice] : "";
    }

    private static PdfPCell celdaElector(Phrase contenido, int alineacion) {
        PdfPCell celda = new PdfPCell(contenido);
        // Interlineado ajustado: iglesia y comunidad caben en el alto mínimo de la fila.
        celda.setLeading(0f, 1.1f);
        celda.setBorderColor(TipografiaPdf.BORDE);
        celda.setBorderWidth(BORDE);
        celda.setHorizontalAlignment(alineacion);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setMinimumHeight(ALTO_FILA);
        celda.setPaddingLeft(4f);
        celda.setPaddingRight(4f);
        celda.setPaddingTop(2f);
        celda.setPaddingBottom(4f);
        return celda;
    }

    private static PdfPCell celdaDato(String valor, Font fuente) {
        PdfPCell celda = new PdfPCell(new Phrase(valor, fuente));
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setPaddingBottom(3f);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return celda;
    }
}
