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
     * A4 apaisado sobre la versión horizontal de la plantilla institucional
     * ({@link PlantillaA4#componerHorizontal}); sus márgenes también los define la plantilla.
     */
    public static final float MARGEN_IZQUIERDO_HORIZONTAL = PlantillaA4.MARGEN_IZQUIERDO;
    public static final float MARGEN_DERECHO_HORIZONTAL = PlantillaA4.MARGEN_DERECHO;
    public static final float MARGEN_SUPERIOR_HORIZONTAL = PlantillaA4.MARGEN_SUPERIOR_HORIZONTAL;
    public static final float MARGEN_INFERIOR_HORIZONTAL = PlantillaA4.MARGEN_INFERIOR;

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
     * A4 apaisado sobre la plantilla institucional horizontal, con la línea técnica
     * «título | Código | fecha» bajo el encabezado, como {@link #crearA4}.
     */
    public static Contexto crearA4Horizontal(OutputStream salida, String codigo, String titulo,
            LocalDateTime fechaGeneracion) throws Exception {
        LocalDateTime fecha = fechaGeneracion != null ? fechaGeneracion : LocalDateTime.now();
        return abrirHorizontal(salida, titulo, fecha, new PlantillaA4.Fondo(codigo, titulo, null, fecha, null, true, true));
    }

    /**
     * A4 apaisado con el encabezado de las actas: plantilla institucional horizontal y código de
     * barras sobre «DOCUMENTO OFICIAL», sin la línea técnica (el documento lleva su título en el
     * cuerpo) y con «Página N | Código de validación: …» en el pie. El código de barras codifica
     * el mismo código del documento.
     */
    public static Contexto crearA4HorizontalConCodigoBarras(OutputStream salida, String titulo,
            LocalDateTime fechaGeneracion, String codigoDocumento) throws Exception {
        LocalDateTime fecha = fechaGeneracion != null ? fechaGeneracion : LocalDateTime.now();
        return abrirHorizontal(salida, titulo, fecha,
                new PlantillaA4.Fondo(codigoDocumento, null, null, fecha, codigoDocumento, false, true));
    }

    private static Contexto abrirHorizontal(OutputStream salida, String titulo, LocalDateTime fecha,
            PlantillaA4.Fondo fondo) throws Exception {
        Document documento = new Document(PageSize.A4.rotate(), MARGEN_IZQUIERDO_HORIZONTAL,
                MARGEN_DERECHO_HORIZONTAL, MARGEN_SUPERIOR_HORIZONTAL, MARGEN_INFERIOR_HORIZONTAL);
        PdfWriter writer = PdfWriter.getInstance(documento, salida);
        writer.setPageEvent(fondo);
        documento.open();
        aplicarMetadata(documento, titulo);
        return new Contexto(documento, writer, fecha);
    }

    /**
     * Documento A4 con encabezado limpio: la plantilla institucional y un código de
     * barras en la cabecera, sobre el bloque «DOCUMENTO OFICIAL», sin la línea técnica
     * de título, código y fecha. Para actas que llevan su propio título en el cuerpo.
     */
    public static Contexto crearA4ConCodigoBarras(OutputStream salida, String titulo,
            LocalDateTime fechaGeneracion, String codigoBarras) throws Exception {
        LocalDateTime fecha = fechaGeneracion != null ? fechaGeneracion : LocalDateTime.now();
        Document documento = new Document(PageSize.A4, MARGEN_IZQUIERDO, MARGEN_DERECHO,
                MARGEN_SUPERIOR, MARGEN_INFERIOR);
        PdfWriter writer = PdfWriter.getInstance(documento, salida);
        // El acta lleva su propio título en el cuerpo, así que el encabezado solo
        // aporta la plantilla y el código de barras; repetirlos arriba sería
        // redundante.
        writer.setPageEvent(new PlantillaA4.Fondo(null, null, null, fecha, codigoBarras));
        documento.open();
        aplicarMetadata(documento, titulo);
        return new Contexto(documento, writer, fecha);
    }

    /**
     * Igual que {@link #crearA4ConCodigoBarras}, pero conserva el código en el pie
     * («Página N | Código de validación: …») para validarlo a simple vista. El código de
     * barras codifica ese mismo código del documento.
     */
    public static Contexto crearA4ConCodigoBarrasYCodigoEnPie(OutputStream salida, String titulo,
            LocalDateTime fechaGeneracion, String codigoDocumento) throws Exception {
        LocalDateTime fecha = fechaGeneracion != null ? fechaGeneracion : LocalDateTime.now();
        Document documento = new Document(PageSize.A4, MARGEN_IZQUIERDO, MARGEN_DERECHO,
                MARGEN_SUPERIOR, MARGEN_INFERIOR);
        PdfWriter writer = PdfWriter.getInstance(documento, salida);
        writer.setPageEvent(new PlantillaA4.Fondo(codigoDocumento, null, null, fecha, codigoDocumento, false));
        documento.open();
        aplicarMetadata(documento, titulo);
        return new Contexto(documento, writer, fecha);
    }

    /**
     * Formulario manual de acta parcial sobre la plantilla institucional. El
     * código de barras se imprime en el encabezado para no restar espacio al
     * formulario, que debe caber en una sola hoja; el proceso electoral va en el
     * cuerpo del acta.
     */
    public static Contexto crearA4ActaParcial(OutputStream salida, String titulo,
            String procesoElectoral, LocalDateTime fechaGeneracion, String codigoBarras) throws Exception {
        return crearA4ConCodigoBarras(salida, titulo, fechaGeneracion, codigoBarras);
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
