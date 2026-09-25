package ec.com.antenasur.itext;

import java.io.OutputStream;
import java.time.LocalDateTime;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;

import ec.com.antenasur.util.Constantes;

/** Fabrica comun para documentos PDF institucionales persistidos. */
public final class PdfInstitucional {

    /**
     * Márgenes A4 vertical: los define la plantilla institucional
     * ({@link PlantillaA4}) y no deben duplicarse en los generadores.
     */
    public static final float MARGEN_IZQUIERDO = PlantillaA4.MARGEN_IZQUIERDO;
    public static final float MARGEN_DERECHO = PlantillaA4.MARGEN_DERECHO;
    public static final float MARGEN_SUPERIOR = PlantillaA4.MARGEN_SUPERIOR;
    public static final float MARGEN_INFERIOR = PlantillaA4.MARGEN_INFERIOR;

    /**
     * A4 apaisado: no existe versión horizontal de la plantilla, por lo que
     * conserva el encabezado gráfico propio y sus márgenes.
     */
    public static final float MARGEN_IZQUIERDO_HORIZONTAL = 36f;
    public static final float MARGEN_DERECHO_HORIZONTAL = 36f;
    public static final float MARGEN_SUPERIOR_HORIZONTAL = 92f;
    public static final float MARGEN_INFERIOR_HORIZONTAL = 48f;

    private PdfInstitucional() {
    }

    /**
     * Documento A4 vertical sobre la plantilla institucional. El encabezado y el
     * pie gráficos los aporta la propia plantilla; el código, la fecha y la
     * paginación se imprimen dentro de la zona segura.
     */
    public static Contexto crearA4(OutputStream salida, String codigo, String titulo,
            LocalDateTime fechaGeneracion) throws Exception {
        LocalDateTime fecha = fechaGeneracion != null ? fechaGeneracion : LocalDateTime.now();
        Document documento = new Document(PageSize.A4, MARGEN_IZQUIERDO, MARGEN_DERECHO,
                MARGEN_SUPERIOR, MARGEN_INFERIOR);
        PdfWriter writer = PdfWriter.getInstance(documento, salida);
        writer.setPageEvent(new PlantillaA4.Fondo(codigo, titulo, null, fecha));
        documento.open();
        aplicarMetadata(documento, titulo);
        return new Contexto(documento, writer, fecha);
    }

    /**
     * A4 apaisado: sin plantilla (no existe versión horizontal), conserva el
     * encabezado gráfico propio.
     */
    public static Contexto crearA4Horizontal(OutputStream salida, String codigo, String titulo,
            LocalDateTime fechaGeneracion) throws Exception {
        LocalDateTime fecha = fechaGeneracion != null ? fechaGeneracion : LocalDateTime.now();
        Document documento = new Document(PageSize.A4.rotate(), MARGEN_IZQUIERDO_HORIZONTAL,
                MARGEN_DERECHO_HORIZONTAL, MARGEN_SUPERIOR_HORIZONTAL, MARGEN_INFERIOR_HORIZONTAL);
        PdfWriter writer = PdfWriter.getInstance(documento, salida);
        writer.setPageEvent(new HeaderFooterPageEvent(codigo, titulo, fecha));
        documento.open();
        aplicarMetadata(documento, titulo);
        return new Contexto(documento, writer, fecha);
    }

    /**
     * Formulario manual de acta parcial: mismo lienzo institucional que el resto
     * de documentos A4; en lugar del código lleva el proceso electoral como dato
     * de encabezado.
     */
    /**
     * Formulario manual de acta parcial sobre la plantilla institucional. El
     * código de barras se imprime en el encabezado, sobre el bloque «DOCUMENTO
     * OFICIAL», para no restar espacio al formulario, que debe caber en una
     * sola hoja.
     */
    public static Contexto crearA4ActaParcial(OutputStream salida, String titulo,
            String procesoElectoral, LocalDateTime fechaGeneracion, String codigoBarras) throws Exception {
        LocalDateTime fecha = fechaGeneracion != null ? fechaGeneracion : LocalDateTime.now();
        Document documento = new Document(PageSize.A4, MARGEN_IZQUIERDO, MARGEN_DERECHO,
                MARGEN_SUPERIOR, MARGEN_INFERIOR);
        PdfWriter writer = PdfWriter.getInstance(documento, salida);
        // El acta lleva su propio título centrado y el proceso electoral en el
        // cuerpo, así que el encabezado solo aporta la plantilla y el código de
        // barras; repetirlos arriba sería redundante.
        writer.setPageEvent(new PlantillaA4.Fondo(null, null, null, fecha, codigoBarras));
        documento.open();
        aplicarMetadata(documento, titulo);
        return new Contexto(documento, writer, fecha);
    }

    public static void aplicarMetadata(Document documento, String titulo) {
        String nombre = titulo != null && !titulo.isBlank() ? titulo : "Documento electoral";
        documento.addAuthor(Constantes.INSTITUCION);
        documento.addCreator(Constantes.SISTEMA);
        documento.addTitle(nombre);
        documento.addSubject("Documento electoral generado por el Sistema TEC");
        documento.addKeywords("SITEC, Tribunal Electoral, CONPOCIIECH, " + nombre);
        documento.addCreationDate();
    }

    public record Contexto(Document documento, PdfWriter writer, LocalDateTime fechaGeneracion) {
    }
}
