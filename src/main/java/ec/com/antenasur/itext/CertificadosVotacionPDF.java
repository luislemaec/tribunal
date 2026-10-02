package ec.com.antenasur.itext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfFunction;
import com.itextpdf.text.pdf.PdfShading;
import com.itextpdf.text.pdf.PdfShadingPattern;

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
    private static final BaseColor CELESTE = TipografiaPdf.FONDO_CABECERA;
    /** SVG fuente y Form XObject vectorial creado una vez por documento y reutilizado en cada tarjeta. */
    private final byte[] logoSvg;
    private PdfTemplate logoTec;
    private final Image marcaAguaTec;
    private PdfTemplate patronBandas;

    /** Pesos de la jerarquía del certificado (fuentes compartidas de {@link TipografiaPdf}). */
    private static final TipografiaPdf.Peso REGULAR = TipografiaPdf.Peso.REGULAR;
    private static final TipografiaPdf.Peso MEDIUM = TipografiaPdf.Peso.MEDIUM;
    private static final TipografiaPdf.Peso BOLD = TipografiaPdf.Peso.BOLD;

    record Recursos(byte[] logo) {
        static Recursos delProyecto() throws IOException {
            return new Recursos(grafico("logo-tec-azul.svg"));
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
        logoSvg = recursos.logo();
        marcaAguaTec = Image.getInstance(Recursos.grafico("plantilla-a4-marca-agua.png"));
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
        try {
            logoTec = LogoTecVectorial.crearCompleto(escritor, logoSvg);
            patronBandas = crearBandasSeguridad(escritor);
            for (int inicio = 0; inicio < personas.size(); inicio += 10) {
                int cantidad = Math.min(10, personas.size() - inicio);
                if (inicio > 0) pdf.newPage();
                for (int posicion = 0; posicion < cantidad; posicion++) {
                    CertificadoVotacionDTO persona = personas.get(inicio + posicion);
                    String codigo = codigo(reporte.getProceso().getId(), reporte.getMesa().getId(), persona.personaId());
                    if (!codigos.add(codigo)) throw new DocumentException("Identificador de certificado duplicado");
                    frente(escritor.getDirectContent(), reporte, persona,
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
        cb.restoreState();
        cb.addTemplate(patronBandas, x, y);
    }

    /** Curvas completas de borde a borde, recortadas al interior de la tarjeta.
     * Dos familias suaves ocupan el encabezado y la zona inferior de datos;
     * el reverso deja libre la firma y las zonas silenciosas del código de barras. */
    private PdfTemplate crearBandasSeguridad(PdfWriter writer) {
        PdfTemplate cb = writer.getDirectContent().createTemplate(ANCHO, ALTO);
        float x = 0, y = 0;
        cb.saveState();
        cb.rectangle(x + 3, y + 3, ANCHO - 6, ALTO - 6);
        cb.clip();
        cb.newPath();
        // Azul -> azul claro -> azul: luz central suave, sin colores nuevos ni raster.
        float[] oscuro = rgb(AZUL), claro = rgb(TipografiaPdf.FONDO_CABECERA);
        PdfFunction degradado = PdfFunction.type3(writer, new float[]{0, 1}, null,
                new PdfFunction[]{
                        PdfFunction.type2(writer, new float[]{0, 1}, null, oscuro, claro, 1),
                        PdfFunction.type2(writer, new float[]{0, 1}, null, claro, oscuro, 1)},
                new float[]{0.5f}, new float[]{0, 1, 0, 1});
        PdfShading sombreado = PdfShading.type2(writer, AZUL, new float[]{3, 0, ANCHO - 3, 0},
                new float[]{0, 1}, degradado, new boolean[]{true, true});
        cb.setShadingStroke(new PdfShadingPattern(sombreado));
        PdfGState estado = new PdfGState();
        estado.setStrokeOpacity(0.18f);
        cb.setGState(estado);
        for (int familia = 0; familia < 2; familia++) {
            float base = familia == 0 ? ALTO - 18 : 43;
            for (int curva = 0; curva < 3; curva++) {
                float nivel = y + base - curva * 2.4f;
                cb.setLineWidth(curva == 1 ? 0.32f : 0.22f);
                cb.moveTo(x + 3, nivel);
                cb.curveTo(x + ANCHO * 0.28f, nivel + 9 - familia * 3,
                        x + ANCHO * 0.67f, nivel - 9 + familia * 3,
                        x + ANCHO - 3, nivel + 1.5f);
                cb.stroke();
            }
        }
        cb.restoreState();
        return cb;
    }

    private static float[] rgb(BaseColor color) {
        return new float[]{color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f};
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

    /** Pie: colores medidos en la antigua onda (cert-onda.png). */
    private static final BaseColor PIE_OSCURO = new BaseColor(0x09, 0x3F, 0x78);
    private static final BaseColor PIE_CLARO = new BaseColor(0x0D, 0x57, 0xA4);
    private static final BaseColor PIE_LINEA = new BaseColor(0x78, 0xBB, 0xFD);
    private static final float ALTO_PIE = 9f;

    /** Conjunto centrado: mapa vectorial y PNG oficial, detrás de los datos del anverso. */
    private static final float OPACIDAD_LOGO = 0.07f;
    private static final float ANCHO_MARCA_AGUA = 67.5f;
    private static final float ALTO_MAPA = 43f;
    private static final float OPACIDAD_MAPA = 0.035f;

    private void frente(PdfContentByte cb, ReporteMesaDTO reporte, CertificadoVotacionDTO persona,
            float x, float y) throws DocumentException {
        marco(cb, x, y);
        marcaAgua(cb, x, y);
        pie(cb, x, y);
        encabezado(cb, reporte, x, y);
        datosVotante(cb, reporte, persona, x, y);
    }

    private void marcaAgua(PdfContentByte cb, float x, float y) throws DocumentException {
        MapaEcuador mapa = MapaEcuador.instancia();
        float escalaMapa = ALTO_MAPA / mapa.alto();
        float anchoMapa = mapa.ancho() * escalaMapa;
        float inicio = x + (ANCHO - anchoMapa - 10 - ANCHO_MARCA_AGUA) / 2;
        float centroY = y + ALTO / 2;
        PdfGState tenue = new PdfGState();
        tenue.setFillOpacity(OPACIDAD_MAPA);
        cb.saveState();
        cb.setGState(tenue);
        cb.setColorFill(AZUL);
        for (float[] poligono : mapa.poligonos()) {
            for (int i = 0; i < poligono.length; i += 2) {
                float px = inicio + poligono[i] * escalaMapa;
                float py = centroY - ALTO_MAPA / 2 + (mapa.alto() - poligono[i + 1]) * escalaMapa;
                if (i == 0) cb.moveTo(px, py); else cb.lineTo(px, py);
            }
            cb.closePath();
        }
        cb.fill();
        cb.restoreState();

        marcaAguaTec.scaleToFit(ANCHO_MARCA_AGUA, ALTO_MAPA);
        marcaAguaTec.setAbsolutePosition(inicio + anchoMapa + 10,
                centroY - marcaAguaTec.getScaledHeight() / 2);
        PdfGState transparencia = new PdfGState();
        // El PNG ya contiene alfa 23/255: se ajusta para obtener 7 % efectivo.
        transparencia.setFillOpacity(OPACIDAD_LOGO / (23f / 255f));
        cb.saveState();
        cb.setGState(transparencia);
        cb.addImage(marcaAguaTec);
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
    private void encabezado(PdfContentByte cb, ReporteMesaDTO reporte, float x, float y)
            throws DocumentException {
        // Logo oficial completo: sus letras sustituyen el texto institucional duplicado.
        float escalaLogo = 49 / logoTec.getWidth();
        cb.addTemplate(logoTec, escalaLogo, 0, 0, escalaLogo, x + 9, y + ALTO - 32);
        float inicioTitulo = x + 67;
        float anchoTitulo = ANCHO - 79;
        float centroTitulo = inicioTitulo + anchoTitulo / 2;
        ajustarYCentrar(cb, mensaje("titulo"), centroTitulo, y + ALTO - 17, anchoTitulo, 7.7f, BOLD, AZUL);
        String proceso = reporte.getProceso() != null ? texto(reporte.getProceso().getNombre()) : "";
        String subtitulo = proceso.toUpperCase(Locale.ROOT);
        ajustarYCentrar(cb, subtitulo, centroTitulo, y + ALTO - 25, anchoTitulo, 5.1f, MEDIUM, SUAVE);
        linea(cb, x + 58, y + ALTO - 29,
                x + ANCHO - MARGEN_TARJETA, AZUL, 0.5f);
        centrar(cb, mensaje("certifica"), x + ANCHO / 2, y + ALTO - 39, 5f, REGULAR, SUAVE);
    }

    /** Persona, información electoral y datos territoriales del recinto, en bloques con etiqueta arriba. */
    private void datosVotante(PdfContentByte cb, ReporteMesaDTO reporte, CertificadoVotacionDTO persona,
            float x, float y) throws DocumentException {
        String nombre = (texto(persona.nombres()) + " " + texto(persona.apellidos())).trim();
        var recinto = reporte.getRecinto();
        float izq = x + MARGEN_TARJETA;
        float columna = ANCHO_UTIL_TARJETA / 3;
        float separacion = 4;
        float filaNombre = y + 88;
        float filaCedula = filaNombre - 20;
        float filaRecinto = filaCedula - 20;
        float filaTerritorio = filaRecinto - 20;

        // Persona: una sola línea, conservando únicamente el prefijo que cabe, y cédula.
        etiqueta(cb, mensaje("nombres"), izq, filaNombre);
        Font fuenteNombre = fuente(TAM_NOMBRE, MEDIUM, TEXTO);
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(prefijoNombre(nombre.isEmpty() ? mensaje("sin.dato") : nombre,
                        fuenteNombre, ANCHO_UTIL_TARJETA), fuenteNombre), izq, filaNombre - 8.4f, 0);
        String etiquetaCedula = mensaje("documento").toUpperCase(Locale.ROOT) + ":";
        String documento = texto(persona.documento());
        String documentoImpreso = documento.substring(0, documento.offsetByCodePoints(0,
                Math.min(10, documento.codePointCount(0, documento.length()))));
        bloque(cb, etiquetaCedula, documentoImpreso, izq, filaCedula,
                ANCHO_UTIL_TARJETA, 12, TAM_DATO_ELECTORAL, true);

        // Información electoral: recinto y mesa, sin fecha de sufragio.
        float colMesa = izq + 2 * columna;
        bloque(cb, mensaje("recinto"), recinto == null ? "" : texto(recinto.getNombre()), izq, filaRecinto,
                2 * columna - separacion, 14, TAM_DATO_ELECTORAL, true);
        bloque(cb, mensaje("mesa"), texto(reporte.getMesa().getNombre()), colMesa, filaRecinto,
                columna, 12, TAM_DATO_ELECTORAL, true);

        // Datos territoriales del recinto, en tres columnas.
        bloque(cb, mensaje("provincia"), recinto == null ? "" : texto(recinto.getProvinciaNombre()), izq,
                filaTerritorio, columna - separacion, 12, TAM_DATO_TERRITORIAL, false);
        bloque(cb, mensaje("canton"), recinto == null ? "" : texto(recinto.getCantonNombre()), izq + columna,
                filaTerritorio, columna - separacion, 12, TAM_DATO_TERRITORIAL, false);
        bloque(cb, mensaje("parroquia"), recinto == null ? "" : texto(recinto.getUbicacionNombre()),
                izq + 2 * columna, filaTerritorio, columna - separacion, 12, TAM_DATO_TERRITORIAL, false);
    }

    /** Recorta solo la representación impresa, sin reducir el cuerpo ni alterar el DTO. */
    private static String prefijoNombre(String nombre, Font fuente, float ancho) {
        String resultado = "";
        for (String palabra : nombre.split("\\s+")) {
            String candidato = resultado.isEmpty() ? palabra : resultado + " " + palabra;
            if (new Chunk(candidato, fuente).getWidthPoint() > ancho) break;
            resultado = candidato;
        }
        return resultado;
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
