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
    private static final BaseColor AZUL = TipografiaPdf.AZUL;
    private static final BaseColor BORDE = TipografiaPdf.BORDE;
    private static final BaseColor TEXTO = new BaseColor(30, 36, 42);
    private static final BaseColor SUAVE = new BaseColor(105, 118, 132);
    private static final BaseColor CELESTE = new BaseColor(223, 235, 246);
    /**
     * Imágenes creadas una sola vez por documento y reutilizadas en cada certificado: iText incrusta
     * cada instancia de {@link Image} como un objeto propio, así que crearlas por certificado repetía
     * 30 copias por hoja (162 MB en 100 hojas frente a 2,4 MB reutilizándolas).
     */
    private final Image logoTec;

    /** Pesos de la jerarquía del certificado (fuentes compartidas de {@link TipografiaPdf}). */
    private static final TipografiaPdf.Peso REGULAR = TipografiaPdf.Peso.REGULAR;
    private static final TipografiaPdf.Peso MEDIUM = TipografiaPdf.Peso.MEDIUM;
    private static final TipografiaPdf.Peso BOLD = TipografiaPdf.Peso.BOLD;

    record Recursos(byte[] logo) {
        static Recursos delProyecto() throws IOException {
            return new Recursos(grafico("cert-logo.png"));
        }

        /**
         * Logotipo TEC del certificado, incorporado al classpath para que
         * esté disponible también fuera de una petición web.
         */
        static byte[] grafico(String nombre) throws IOException {
            try (InputStream entrada = CertificadosVotacionPDF.class.getResourceAsStream("/img/" + nombre)) {
                if (entrada == null) throw new IOException("Recurso institucional no disponible: " + nombre);
                return entrada.readAllBytes();
            }
        }
    }

    private CertificadosVotacionPDF(Recursos recursos) throws IOException, DocumentException {
        logoTec = Image.getInstance(recursos.logo());
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
                    frente(escritor.getDirectContent(), reporte, persona, fechaTexto,
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

    /*
     * Anverso, de arriba abajo: identidad institucional → tipo de certificado y proceso → persona
     * (nombre y cédula) → información electoral (recinto, mesa, fecha) → datos territoriales del
     * recinto → pie institucional. Coordenadas relativas a la esquina inferior izquierda de la tarjeta.
     */
    private static final float MARGEN_TARJETA = 12f;
    private static final float ANCHO_UTIL_TARJETA = ANCHO - 2 * MARGEN_TARJETA;
    private static final float TAM_ETIQUETA = 4.6f;
    private static final float TAM_NOMBRE = 7f;
    private static final float TAM_DATO_ELECTORAL = 5.6f;
    private static final float TAM_DATO_TERRITORIAL = 5f;

    /** Franja tricolor medida en la antigua silueta: amarillo, azul y rojo en proporción 60:50:39. */
    private static final BaseColor[] TRICOLOR = {
            new BaseColor(0xFA, 0xD4, 0x1D), new BaseColor(0x02, 0x40, 0x88), new BaseColor(0xE1, 0x1C, 0x23)};
    private static final float[] TRICOLOR_PROPORCION = {60, 50, 39};

    /** Pie: colores medidos en la antigua onda (cert-onda.png). */
    private static final BaseColor PIE_OSCURO = new BaseColor(0x09, 0x3F, 0x78);
    private static final BaseColor PIE_CLARO = new BaseColor(0x0D, 0x57, 0xA4);
    private static final BaseColor PIE_LINEA = new BaseColor(0x78, 0xBB, 0xFD);
    private static final float ALTO_PIE = 9f;

    /** Marca de agua: mapa del Ecuador continental en azul institucional al 8 %. */
    private static final float OPACIDAD_MAPA = 0.08f;
    private static final float ALTO_MAPA = 96f;

    private void frente(PdfContentByte cb, ReporteMesaDTO reporte, CertificadoVotacionDTO persona,
            String fecha, float x, float y) throws DocumentException {
        marco(cb, x, y);
        mapa(cb, x, y);
        pie(cb, x, y);
        encabezado(cb, reporte, fecha, x, y);
        datosVotante(cb, reporte, persona, fecha, x, y);
    }

    /** Silueta vectorial del Ecuador continental, centrada y detrás de todo el contenido. */
    private void mapa(PdfContentByte cb, float x, float y) {
        MapaEcuador mapa = MapaEcuador.instancia();
        float escala = ALTO_MAPA / mapa.alto();
        float origenX = x + (ANCHO - mapa.ancho() * escala) / 2;
        float origenY = y + (ALTO + ALTO_PIE - ALTO_MAPA) / 2 + 1.5f;
        PdfGState transparencia = new PdfGState();
        transparencia.setFillOpacity(OPACIDAD_MAPA);
        cb.saveState();
        cb.setGState(transparencia);
        cb.setColorFill(AZUL);
        for (float[] poligono : mapa.poligonos()) {
            for (int i = 0; i < poligono.length; i += 2) {
                // El SVG crece hacia abajo; el PDF, hacia arriba.
                float px = origenX + poligono[i] * escala;
                float py = origenY + (mapa.alto() - poligono[i + 1]) * escala;
                if (i == 0) cb.moveTo(px, py); else cb.lineTo(px, py);
            }
            cb.closePath();
        }
        cb.fill();
        cb.restoreState();
    }

    /** Pie institucional vectorial: banda azul con la cresta clara de la onda y el lema como texto real. */
    private void pie(PdfContentByte cb, float x, float y) {
        float izq = x + 3, ancho = ANCHO - 6, base = y + 3;
        cb.saveState();
        cb.rectangle(izq, base, ancho, ALTO_PIE);
        cb.clip();
        cb.newPath();
        cb.setColorFill(PIE_OSCURO);
        cb.rectangle(izq, base, ancho, ALTO_PIE);
        cb.fill();
        // Zona central más clara, como el degradado de la onda original.
        cb.setColorFill(PIE_CLARO);
        cb.moveTo(izq + ancho * 0.30f, base);
        cb.curveTo(izq + ancho * 0.45f, base + ALTO_PIE, izq + ancho * 0.75f, base + ALTO_PIE,
                izq + ancho * 0.92f, base);
        cb.closePath();
        cb.fill();
        // Cresta blanca y línea celeste de la onda, en el borde superior.
        cb.setColorFill(BaseColor.WHITE);
        cb.moveTo(izq + ancho * 0.34f, base + ALTO_PIE);
        cb.curveTo(izq + ancho * 0.52f, base + ALTO_PIE - 3.2f, izq + ancho * 0.70f, base + ALTO_PIE - 3.2f,
                izq + ancho * 0.86f, base + ALTO_PIE);
        cb.fill();
        cb.setColorStroke(PIE_LINEA);
        cb.setLineWidth(0.5f);
        cb.moveTo(izq + ancho * 0.36f, base + ALTO_PIE - 0.6f);
        cb.curveTo(izq + ancho * 0.53f, base + ALTO_PIE - 4.4f, izq + ancho * 0.72f, base + ALTO_PIE - 4.2f,
                izq + ancho, base + ALTO_PIE - 2.6f);
        cb.stroke();
        cb.restoreState();
        Font lema = fuente(3.6f, MEDIUM, BaseColor.WHITE);
        Chunk lemaEspaciado = new Chunk(mensaje("lema"), lema);
        lemaEspaciado.setCharacterSpacing(0.35f);
        Phrase texto = new Phrase(lemaEspaciado);
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, texto, izq + 6, base + 3.1f, 0);
    }

    /** Identidad institucional, tipo de certificado, proceso y fórmula de certificación. */
    private void encabezado(PdfContentByte cb, ReporteMesaDTO reporte, String fecha, float x, float y)
            throws DocumentException {
        Image imagen = logoTec;
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
        // Institución (500) → tipo de documento (700) → proceso (500) → fórmula (400).
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(mensaje("tribunal"), fuente(5f, MEDIUM, AZUL)), x + 45, y + ALTO - 12, 0);
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(mensaje("organizacion"), fuente(5f, MEDIUM, AZUL)), x + 45, y + ALTO - 18, 0);

        centrar(cb, mensaje("titulo"), x + ANCHO / 2, y + ALTO - 31, 9.4f, BOLD, AZUL);
        String proceso = reporte.getProceso() != null ? texto(reporte.getProceso().getNombre()) : "";
        String subtitulo = proceso.isEmpty() ? fecha : proceso.toUpperCase(Locale.ROOT);
        ajustarYCentrar(cb, subtitulo, x + ANCHO / 2, y + ALTO - 38, ANCHO - 24, 5.6f, MEDIUM, SUAVE);
        tricolor(cb, x + ANCHO / 2, y + ALTO - 43.5f, 40f, 1.2f);
        centrar(cb, mensaje("certifica"), x + ANCHO / 2, y + ALTO - 49.5f, 5f, REGULAR, SUAVE);
    }

    /** Franja tricolor vectorial centrada en {@code centro}: separa el encabezado del contenido. */
    private void tricolor(PdfContentByte cb, float centro, float y, float ancho, float alto) {
        float total = 0;
        for (float parte : TRICOLOR_PROPORCION) total += parte;
        float inicio = centro - ancho / 2;
        cb.saveState();
        for (int i = 0; i < TRICOLOR.length; i++) {
            float tramo = ancho * TRICOLOR_PROPORCION[i] / total;
            cb.setColorFill(TRICOLOR[i]);
            cb.rectangle(inicio, y, tramo, alto);
            cb.fill();
            inicio += tramo;
        }
        cb.restoreState();
    }

    /** Persona, información electoral y datos territoriales del recinto, en bloques con etiqueta arriba. */
    private void datosVotante(PdfContentByte cb, ReporteMesaDTO reporte, CertificadoVotacionDTO persona,
            String fecha, float x, float y) throws DocumentException {
        String nombre = (texto(persona.nombres()) + " " + texto(persona.apellidos())).trim();
        var recinto = reporte.getRecinto();
        float izq = x + MARGEN_TARJETA;

        // Persona: nombre destacado a todo el ancho (hasta dos líneas) y cédula.
        etiqueta(cb, mensaje("nombres"), izq, y + 83.5f);
        valor(cb, nombre.isEmpty() ? mensaje("sin.dato") : nombre, izq, y + 81.5f, ANCHO_UTIL_TARJETA, 14.5f,
                TAM_NOMBRE, true);
        String etiquetaCedula = mensaje("documento").toUpperCase(Locale.ROOT) + ":";
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(etiquetaCedula, fuente(TAM_ETIQUETA, MEDIUM, AZUL)), izq, y + 59.5f, 0);
        float anchoEtiqueta = new Chunk(etiquetaCedula, fuente(TAM_ETIQUETA, MEDIUM, AZUL)).getWidthPoint();
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(texto(persona.documento()), fuente(TAM_DATO_ELECTORAL, MEDIUM, TEXTO)),
                izq + anchoEtiqueta + 3, y + 59.5f, 0);

        linea(cb, izq, y + 55.5f, x + ANCHO - MARGEN_TARJETA, BORDE, 0.4f);

        // Información electoral: recinto, mesa y fecha de sufragio.
        float colMesa = izq + 116, colFecha = izq + 138;
        bloque(cb, mensaje("recinto"), recinto == null ? "" : texto(recinto.getNombre()), izq, y + 50.5f,
                110, 12, TAM_DATO_ELECTORAL, true);
        bloque(cb, mensaje("mesa"), texto(reporte.getMesa().getNombre()), colMesa, y + 50.5f,
                20, 7, TAM_DATO_ELECTORAL, true);
        bloque(cb, mensaje("fecha"), fecha, colFecha, y + 50.5f,
                x + ANCHO - MARGEN_TARJETA - colFecha, 7, TAM_DATO_ELECTORAL, true);

        // Datos territoriales del recinto, en tres columnas.
        float columna = ANCHO_UTIL_TARJETA / 3;
        bloque(cb, mensaje("provincia"), recinto == null ? "" : texto(recinto.getProvinciaNombre()), izq,
                y + 30, columna - 4, 12, TAM_DATO_TERRITORIAL, false);
        bloque(cb, mensaje("canton"), recinto == null ? "" : texto(recinto.getCantonNombre()), izq + columna,
                y + 30, columna - 4, 12, TAM_DATO_TERRITORIAL, false);
        bloque(cb, mensaje("parroquia"), recinto == null ? "" : texto(recinto.getUbicacionNombre()),
                izq + 2 * columna, y + 30, columna - 4, 12, TAM_DATO_TERRITORIAL, false);
    }

    /** Etiqueta en mayúsculas (500, azul) sobre su línea base. */
    private void etiqueta(PdfContentByte cb, String texto, float x, float base) {
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(texto.toUpperCase(Locale.ROOT), fuente(TAM_ETIQUETA, MEDIUM, AZUL)), x, base, 0);
    }

    /** Etiqueta arriba y valor debajo, dentro del ancho y alto indicados. */
    private void bloque(PdfContentByte cb, String etiqueta, String dato, float x, float base, float ancho,
            float alto, float tamano, boolean destacado) throws DocumentException {
        etiqueta(cb, etiqueta, x, base);
        valor(cb, dato.isEmpty() ? mensaje("sin.dato") : dato, x, base - 2, ancho, alto, tamano, destacado);
    }

    /** Reverso: etiqueta arriba y valor debajo. */
    private void campo(PdfContentByte cb, String etiqueta, String contenido, float x, float y,
            float ancho, float alto, float tamano, boolean destacado) throws DocumentException {
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(etiqueta.toUpperCase(Locale.ROOT), fuente(4.8f, MEDIUM, AZUL)), x, y, 0);
        valor(cb, contenido, x, y - 3, ancho, alto, tamano, destacado);
    }

    private void valor(PdfContentByte cb, String contenido, float x, float techo, float ancho,
            float alto, float tamano, boolean destacado) throws DocumentException {
        if (contenido == null || contenido.isEmpty()) {
            return;
        }
        // Se reduce el cuerpo solo lo necesario; el dato puede ocupar varias
        // líneas dentro del alto reservado antes que volverse ilegible.
        for (float cuerpo = tamano; cuerpo >= 4.8f; cuerpo -= 0.2f) {
            Phrase texto = new Phrase(contenido, fuente(cuerpo, destacado ? MEDIUM : REGULAR, TEXTO));
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
            float ancho, float tamano, TipografiaPdf.Peso peso, BaseColor color) {
        float cuerpo = tamano;
        while (cuerpo > 4f && new Chunk(contenido, fuente(cuerpo, peso, color)).getWidthPoint() > ancho) {
            cuerpo -= 0.2f;
        }
        centrar(cb, contenido, centro, y, cuerpo, peso, color);
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
        // Sección (500) → etiquetas (500) y valores (400) → texto (400) → firma (500).
        centrar(cb, mensaje("detalle"), x + ANCHO / 2, y + ALTO - 13, 6.3f, MEDIUM, AZUL);
        // La ubicación electoral (provincia, cantón y parroquia del recinto) ya
        // consta en el anverso: aquí solo se detalla la pertenencia del votante.
        campo(cb, mensaje("iglesia"), texto(persona.iglesia()), x + 10, y + 115,
                ANCHO - 20, 14, 5.8f, false);
        campo(cb, mensaje("comunidad"), texto(persona.comunidad()), x + 10, y + 92,
                ANCHO - 20, 14, 5.8f, false);
        valor(cb, mensaje("acredita"), x + 10, y + 62, ANCHO - 20, 16, 5.4f, false);
        linea(cb, x + 40, y + 34, x + ANCHO - 40, AZUL, 0.5f);
        centrar(cb, mensaje("firma"), x + ANCHO / 2, y + 28, 5.1f, MEDIUM, AZUL);

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
            float tamano, TipografiaPdf.Peso peso, BaseColor color) {
        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                new Phrase(texto, fuente(tamano, peso, color)), x, y, 0);
    }

    private static Font fuente(float tamano, TipografiaPdf.Peso peso, BaseColor color) {
        return TipografiaPdf.fuente(peso, tamano, color);
    }

    private static String mensaje(String clave) {
        return Constantes.getMensaje("reportesMesa.certificados." + clave);
    }

    private static String texto(String valor) {
        return valor != null ? valor.replaceAll("\\s+", " ").trim() : "";
    }
}
