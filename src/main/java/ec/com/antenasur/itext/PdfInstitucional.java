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

    public static final float MARGEN_IZQUIERDO = 36f;
    public static final float MARGEN_DERECHO = 36f;
    public static final float MARGEN_SUPERIOR = 118f;
    public static final float MARGEN_SUPERIOR_HORIZONTAL = 92f;
    public static final float MARGEN_INFERIOR = 48f;

    private PdfInstitucional() {
    }

    public static Contexto crearA4(OutputStream salida, String codigo, String titulo,
            LocalDateTime fechaGeneracion) throws Exception {
        return crear(salida, PageSize.A4, MARGEN_SUPERIOR, codigo, titulo, fechaGeneracion);
    }

    public static Contexto crearA4Horizontal(OutputStream salida, String codigo, String titulo,
            LocalDateTime fechaGeneracion) throws Exception {
        return crear(salida, PageSize.A4.rotate(), MARGEN_SUPERIOR_HORIZONTAL,
                codigo, titulo, fechaGeneracion);
    }

    private static Contexto crear(OutputStream salida, Rectangle pagina, float margenSuperior,
            String codigo, String titulo, LocalDateTime fechaGeneracion) throws Exception {
        LocalDateTime fecha = fechaGeneracion != null ? fechaGeneracion : LocalDateTime.now();
        Document documento = new Document(pagina, MARGEN_IZQUIERDO, MARGEN_DERECHO,
                margenSuperior, MARGEN_INFERIOR);
        PdfWriter writer = PdfWriter.getInstance(documento, salida);
        writer.setPageEvent(new HeaderFooterPageEvent(codigo, titulo, fecha));
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
