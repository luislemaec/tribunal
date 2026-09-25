package ec.com.antenasur.itext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.faces.context.FacesContext;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.Barcode128;
import com.itextpdf.text.pdf.BarcodeQRCode;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfGState;
import com.itextpdf.text.pdf.PdfWriter;

import ec.com.antenasur.dto.CertificadoVotacionDTO;
import ec.com.antenasur.dto.ReporteMesaDTO;
import ec.com.antenasur.util.Constantes;

/** Composicion de certificados recortables; no consulta ni modifica persistencia. */
final class CertificadosVotacionPDF {
    static final float ANCHO = 75f * 72f / 25.4f;
    static final float ALTO = 50f * 72f / 25.4f;
    private static final BaseColor AZUL = new BaseColor(24, 82, 133);
    private static final BaseColor BORDE = new BaseColor(210, 220, 230);
    private static final BaseColor TEXTO = new BaseColor(30, 36, 42);
    private static final BaseColor SUAVE = new BaseColor(105, 118, 132);
    private static final BaseColor CELESTE = new BaseColor(223, 235, 246);
    private final byte[] logoTec;
    private final byte[] onda;
    private final byte[] silueta;
    private final BaseFont regular;
    private final BaseFont negrita;

    record Recursos(byte[] logo, byte[] regular, byte[] negrita) {
        static Recursos delProyecto() throws IOException {
            return new Recursos(grafico("cert-logo.png"),
                    leer("/resources/fonts/Montserrat-Regular.ttf"),
                    leer("/resources/fonts/Montserrat-Bold.ttf"));
        }

        /**
         * Elementos gráficos de la identidad del certificado (logotipo TEC,
         * onda institucional y silueta), incorporados al classpath para que
         * estén disponibles también fuera de una petición web.
         */
        static byte[] grafico(String nombre) throws IOException {
            try (InputStream entrada = CertificadosVotacionPDF.class.getResourceAsStream("/img/" + nombre)) {
                if (entrada == null) throw new IOException("Recurso institucional no disponible: " + nombre);
                return entrada.readAllBytes();
            }
        }

        private static byte[] leer(String ruta) throws IOException {
            // Streams funcionan tanto en WAR empaquetado como en deployment expandido.
            FacesContext faces = FacesContext.getCurrentInstance();
            if (faces == null) throw new IOException("No existe contexto para recursos institucionales");
            try (InputStream entrada = faces.getExternalContext().getResourceAsStream(ruta)) {
                if (entrada == null) throw new IOException("Recurso institucional no disponible: " + ruta);
                return entrada.readAllBytes();
            }
        }
    }

    private CertificadosVotacionPDF(Recursos recursos) throws IOException, DocumentException {
        logoTec = recursos.logo();
        onda = Recursos.grafico("cert-onda.png");
        silueta = Recursos.grafico("cert-silueta.png");
        regular = BaseFont.createFont("Montserrat-Regular.ttf", BaseFont.IDENTITY_H,
                BaseFont.EMBEDDED, true, recursos.regular(), null);
        negrita = BaseFont.createFont("Montserrat-Bold.ttf", BaseFont.IDENTITY_H,
                BaseFont.EMBEDDED, true, recursos.negrita(), null);
    }

    static byte[] generar(ReporteMesaDTO reporte, List<CertificadoVotacionDTO> personas,
            Date fecha, Recursos recursos) throws DocumentException {
        if (personas == null || personas.isEmpty() || fecha == null || reporte == null
                || reporte.getProceso() == null || reporte.getProceso().getId() == null
                || reporte.getMesa() == null || reporte.getMesa().getId() == null) {
            throw new IllegalArgumentException("Padron, proceso, mesa y fecha requeridos");
        }
        try {
            return new CertificadosVotacionPDF(recursos).componer(reporte, personas, fecha);
        } catch (IOException e) {
            throw new DocumentException(e);
        }
    }

    private byte[] componer(ReporteMesaDTO reporte, List<CertificadoVotacionDTO> personas,
            Date fecha) throws DocumentException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document pdf = new Document(PageSize.A4);
        PdfWriter escritor = PdfWriter.getInstance(pdf, salida);
        escritor.addViewerPreference(com.itextpdf.text.pdf.PdfName.PRINTSCALING,
                com.itextpdf.text.pdf.PdfName.NONE);
        escritor.addViewerPreference(com.itextpdf.text.pdf.PdfName.DUPLEX,
                com.itextpdf.text.pdf.PdfName.DUPLEXFLIPLONGEDGE);
        PdfInstitucional.aplicarMetadata(pdf, mensaje("titulo"));
        pdf.open();
        Set<String> codigos = new HashSet<>();
        String fechaTexto = new SimpleDateFormat("dd/MM/yyyy").format(fecha);
        try {
            for (int inicio = 0; inicio < personas.size(); inicio += 10) {
                int cantidad = Math.min(10, personas.size() - inicio);
                if (inicio > 0) pdf.newPage();
                for (int posicion = 0; posicion < cantidad; posicion++) {
                    CertificadoVotacionDTO persona = personas.get(inicio + posicion);
                    String codigo = codigo(reporte.getProceso().getId(), reporte.getMesa().getId(), persona.personaId());
                    if (!codigos.add(codigo)) throw new DocumentException("Identificador de certificado duplicado");
                    frente(escritor.getDirectContent(), reporte, persona, fechaTexto, codigo,
                            x(posicion, false), y(posicion));
                }
                pdf.newPage();
                for (int posicion = 0; posicion < cantidad; posicion++) {
                    // El reverso lleva el código de barras de su titular, por lo
                    // que se recalcula el mismo identificador del frente.
                    CertificadoVotacionDTO persona = personas.get(inicio + posicion);
                    reverso(escritor.getDirectContent(), reporte, persona,
                            codigo(reporte.getProceso().getId(), reporte.getMesa().getId(), persona.personaId()),
                            x(posicion, true), y(posicion));
                }
            }
        } finally {
            pdf.close();
        }
        return salida.toByteArray();
    }

    static float x(int posicion, boolean reverso) {
        int columna = reverso ? 1 - posicion % 2 : posicion % 2;
        return (PageSize.A4.getWidth() - 2 * ANCHO) / 2 + columna * ANCHO;
    }

    static float y(int posicion) {
        return (PageSize.A4.getHeight() + 5 * ALTO) / 2 - (posicion / 2 + 1) * ALTO;
    }

    /** Token opaco estable de 128 bits; no es una firma digital ni prueba de sufragio. */
    static String codigo(Integer proceso, Integer mesa, Integer persona) {
        if (proceso == null || mesa == null || persona == null) {
            throw new IllegalArgumentException("Identificadores requeridos para certificado");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    ("TEC-CERT-V1|" + proceso + "|" + mesa + "|" + persona).getBytes(StandardCharsets.UTF_8));
            // 40 digitos permiten Code 128 C compacto, sin imprimir cedula ni IDs en claro.
            return String.format(Locale.ROOT, "%040d", new BigInteger(1, Arrays.copyOf(digest, 16)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void marco(PdfContentByte cb, float x, float y) {
        cb.saveState();
        cb.setColorStroke(new BaseColor(155, 155, 155));
        cb.setLineWidth(0.3f);
        cb.setLineDash(2f, 2f);
        cb.rectangle(x, y, ANCHO, ALTO);
        cb.stroke();
        cb.setLineDash(0);
        cb.setColorStroke(BORDE);
        cb.rectangle(x + 3, y + 3, ANCHO - 6, ALTO - 6);
        cb.stroke();
        // Banda superior en azul claro: identidad institucional discreta, sin
        // bordes gruesos ni recuadros añadidos.
        cb.setColorFill(CELESTE);
        cb.rectangle(x + 3, y + ALTO - 6, ANCHO - 6, 3f);
        cb.fill();
        cb.setColorStroke(CELESTE);
        cb.setLineWidth(0.3f);
        for (int curva = 0; curva < 4; curva++) {
            cb.moveTo(x + ANCHO - 60, y + ALTO - 7 - curva * 2);
            cb.curveTo(x + ANCHO - 28, y + ALTO - 7 - curva * 2,
                    x + ANCHO - 20, y + ALTO - 24 - curva * 2,
                    x + ANCHO - 5, y + ALTO - 24 - curva * 2);
            cb.stroke();
        }
        cb.restoreState();
    }

    private void frente(PdfContentByte cb, ReporteMesaDTO reporte, CertificadoVotacionDTO persona,
            String fecha, String codigo, float x, float y) throws DocumentException {
        marco(cb, x, y);
        silueta(cb, x, y);
        onda(cb, x, y);
        encabezado(cb, reporte, fecha, x, y);
        datosVotante(cb, reporte, persona, fecha, x, y);
        validacionQr(cb, codigo, x, y);
    }

    /**
     * Silueta institucional atenuada en el costado derecho, detrás de todo el
     * contenido, como marca de agua del certificado.
     */
    private void silueta(PdfContentByte cb, float x, float y) throws DocumentException {
        try {
            Image imagen = Image.getInstance(silueta);
            imagen.scaleToFit(52, 42);
            imagen.setAbsolutePosition(x + ANCHO - imagen.getScaledWidth() - 6,
                    y + (ALTO - imagen.getScaledHeight()) / 2 - 4);
            PdfGState transparencia = new PdfGState();
            transparencia.setFillOpacity(0.12f);
            cb.saveState();
            cb.setGState(transparencia);
            cb.addImage(imagen);
            cb.restoreState();
        } catch (Exception e) {
            throw new DocumentException(e);
        }
    }

    /** Onda institucional del pie, con el lema de transparencia. */
    private void onda(PdfContentByte cb, float x, float y) throws DocumentException {
        try {
            Image imagen = Image.getInstance(onda);
            imagen.scaleAbsolute(ANCHO - 6, (ANCHO - 6) * imagen.getHeight() / imagen.getWidth());
            imagen.setAbsolutePosition(x + 3, y + 3.5f);
            cb.saveState();
            cb.rectangle(x + 3, y + 3, ANCHO - 6, 9f);
            cb.clip();
            cb.newPath();
            cb.addImage(imagen);
            cb.restoreState();
        } catch (Exception e) {
            throw new DocumentException(e);
        }
    }

    /** Logotipo, identificación institucional, título y proceso electoral. */
    private void encabezado(PdfContentByte cb, ReporteMesaDTO reporte, String fecha, float x, float y)
            throws DocumentException {
        Image imagen;
        try {
            imagen = Image.getInstance(logoTec);
        } catch (IOException e) {
            throw new DocumentException(e);
        }
        // scaleToFit conserva la proporción original del logotipo.
        imagen.scaleToFit(30, 12);
        imagen.setAbsolutePosition(x + 9, y + ALTO - 19);
        cb.addImage(imagen);
        cb.saveState();
        cb.setColorStroke(BORDE);
        cb.moveTo(x + 42, y + ALTO - 20);
        cb.lineTo(x + 42, y + ALTO - 8);
        cb.stroke();
        cb.restoreState();
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(mensaje("tribunal"), fuente(5f, true, AZUL)), x + 45, y + ALTO - 12, 0);
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(mensaje("organizacion"), fuente(5f, true, AZUL)), x + 45, y + ALTO - 18, 0);

        centrar(cb, mensaje("titulo"), x + ANCHO / 2, y + ALTO - 31, 9.4f, true, AZUL);
        String proceso = reporte.getProceso() != null ? texto(reporte.getProceso().getNombre()) : "";
        String subtitulo = proceso.isEmpty() ? fecha : proceso.toUpperCase(Locale.ROOT);
        ajustarYCentrar(cb, subtitulo, x + ANCHO / 2, y + ALTO - 38, ANCHO - 24, 5.6f, SUAVE);
        centrar(cb, mensaje("certifica"), x + ANCHO / 2, y + 96, 4.7f, false, SUAVE);

    }

    /** Anverso: identidad, recinto, fecha real de sufragio y consulta QR. */
    private void datosVotante(PdfContentByte cb, ReporteMesaDTO reporte, CertificadoVotacionDTO persona,
            String fecha, float x, float y) throws DocumentException {
        String nombre = (texto(persona.nombres()) + " " + texto(persona.apellidos())).trim();
        var recinto = reporte.getRecinto();
        fila(cb, "nombres", nombre, x, y + 91, 13, true);
        fila(cb, "documento", texto(persona.documento()), x, y + 78, 7, true);
        fila(cb, "provincia", recinto == null ? "" : texto(recinto.getProvinciaNombre()), x, y + 71, 7, false);
        fila(cb, "canton", recinto == null ? "" : texto(recinto.getCantonNombre()), x, y + 64, 7, false);
        fila(cb, "parroquia", recinto == null ? "" : texto(recinto.getUbicacionNombre()), x, y + 57, 7, false);
        fila(cb, "recinto", recinto == null ? "" : texto(recinto.getNombre()), x, y + 50, 13, false);
        campo(cb, mensaje("mesa"), texto(reporte.getMesa().getNombre()), x + 62, y + 34, 43, 7, 5f, true);
        campo(cb, mensaje("fecha"), fecha, x + 110, y + 34, 91, 7, 5f, true);
    }

    private void fila(PdfContentByte cb, String clave, String dato, float x, float techo,
            float alto, boolean destacado) throws DocumentException {
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(mensaje(clave).toUpperCase(Locale.ROOT) + ":", fuente(4.8f, true, AZUL)),
                x + 12, techo - 5.6f, 0);
        valor(cb, dato.isEmpty() ? mensaje("sin.dato") : dato, x + 62, techo,
                ANCHO - 74, alto, 5f, destacado);
    }

    private void campo(PdfContentByte cb, String etiqueta, String contenido, float x, float y,
            float ancho, float alto, float tamano, boolean destacado) throws DocumentException {
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(etiqueta.toUpperCase(Locale.ROOT), fuente(4.7f, true, AZUL)), x, y, 0);
        valor(cb, contenido, x, y - 3, ancho, alto, tamano, destacado);
    }

    /**
     * Consulta en línea del certificado. Conserva exactamente el payload
     * existente; no equivale a una firma digital. La única firma del documento
     * es la de la junta, en el reverso.
     */
    private void validacionQr(PdfContentByte cb, String codigo, float x, float y) throws DocumentException {
        cb.saveState();
        cb.setColorFill(BaseColor.WHITE);
        cb.rectangle(x + 8, y + 9, 36, 34);
        cb.fill();
        cb.restoreState();
        Image qr = new BarcodeQRCode(mensaje("verificacion.url") + codigo, 240, 240, null).getImage();
        qr.scaleAbsolute(32f, 32f);
        qr.setAbsolutePosition(x + 10, y + 10);
        cb.addImage(qr);
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(mensaje("consulta"), fuente(4f, true, AZUL)), x + 44, y + 20, 0);
    }

    private void valor(PdfContentByte cb, String contenido, float x, float techo, float ancho,
            float alto, float tamano, boolean destacado) throws DocumentException {
        if (contenido == null || contenido.isEmpty()) {
            return;
        }
        // Se reduce el cuerpo solo lo necesario; el dato puede ocupar varias
        // líneas dentro del alto reservado antes que volverse ilegible.
        for (float cuerpo = tamano; cuerpo >= 4.8f; cuerpo -= 0.2f) {
            Phrase texto = new Phrase(contenido, fuente(cuerpo, destacado, TEXTO));
            ColumnText columna = new ColumnText(cb);
            columna.setSimpleColumn(texto, x, techo - alto, x + ancho, techo,
                    cuerpo + 0.8f, Element.ALIGN_LEFT);
            if (!ColumnText.hasMoreText(columna.go(true))) {
                columna.setText(texto);
                columna.setYLine(techo);
                columna.go();
                return;
            }
        }
        throw new DocumentException("Los datos exceden el area legible del certificado");
    }

    /** Texto centrado que se reduce hasta caber en el ancho indicado. */
    private void ajustarYCentrar(PdfContentByte cb, String contenido, float centro, float y,
            float ancho, float tamano, BaseColor color) {
        float cuerpo = tamano;
        while (cuerpo > 4f && new Chunk(contenido, fuente(cuerpo, false, color)).getWidthPoint() > ancho) {
            cuerpo -= 0.2f;
        }
        centrar(cb, contenido, centro, y, cuerpo, false, color);
    }

    private void linea(PdfContentByte cb, float desde, float y, float hasta, BaseColor color, float grosor) {
        cb.saveState();
        cb.setColorStroke(color);
        cb.setLineWidth(grosor);
        cb.moveTo(desde, y);
        cb.lineTo(hasta, y);
        cb.stroke();
        cb.restoreState();
    }

    /** Reverso: datos completos, acreditacion manual y Code 128 sin reducir modulos. */
    private void reverso(PdfContentByte cb, ReporteMesaDTO reporte, CertificadoVotacionDTO persona,
            String codigo, float x, float y) throws DocumentException {
        marco(cb, x, y);
        centrar(cb, mensaje("detalle"), x + ANCHO / 2, y + ALTO - 13, 6.3f, true, AZUL);
        // La ubicación electoral (provincia, cantón y parroquia del recinto) ya
        // consta en el anverso: aquí solo se detalla la pertenencia del votante.
        campo(cb, mensaje("iglesia"), texto(persona.iglesia()), x + 10, y + 115,
                ANCHO - 20, 14, 5.8f, false);
        campo(cb, mensaje("comunidad"), texto(persona.comunidad()), x + 10, y + 92,
                ANCHO - 20, 14, 5.8f, false);
        valor(cb, mensaje("acredita"), x + 10, y + 62, ANCHO - 20, 16, 5.4f, false);
        linea(cb, x + 40, y + 34, x + ANCHO - 40, AZUL, 0.5f);
        centrar(cb, mensaje("firma"), x + ANCHO / 2, y + 28, 5.1f, true, AZUL);

        Barcode128 barras = new Barcode128();
        barras.setCode(codigo);
        barras.setFont(null);
        barras.setX(0.7f);
        barras.setBarHeight(15f);
        Image imagenBarras = barras.createImageWithBarcode(cb, BaseColor.BLACK, BaseColor.BLACK);
        if (imagenBarras.getScaledWidth() + 14 > ANCHO - 6)
            throw new DocumentException("Codigo de barras excede el ancho imprimible");
        // Quiet zones blancas de al menos 10 modulos por extremo.
        imagenBarras.setAbsolutePosition(x + (ANCHO - imagenBarras.getScaledWidth()) / 2, y + 10);
        cb.addImage(imagenBarras);
    }


    private void centrar(PdfContentByte cb, String texto, float x, float y,
            float tamano, boolean bold, BaseColor color) {
        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                new Phrase(texto, fuente(tamano, bold, color)), x, y, 0);
    }

    private Font fuente(float tamano, boolean bold, BaseColor color) {
        return new Font(bold ? negrita : regular, tamano, Font.NORMAL, color);
    }

    private static String mensaje(String clave) {
        return Constantes.getMensaje("reportesMesa.certificados." + clave);
    }

    private static String texto(String valor) {
        return valor != null ? valor.replaceAll("\\s+", " ").trim() : "";
    }
}
