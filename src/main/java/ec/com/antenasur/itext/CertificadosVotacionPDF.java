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
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
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
    private final Image logo;
    private final BaseFont regular;
    private final BaseFont negrita;

    record Recursos(byte[] logo, byte[] regular, byte[] negrita) {
        static Recursos delProyecto() throws IOException {
            return new Recursos(leer("/resources/img/logo_consejo_417x150.png"),
                    leer("/resources/fonts/Montserrat-Regular.ttf"),
                    leer("/resources/fonts/Montserrat-Bold.ttf"));
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
        logo = Image.getInstance(recursos.logo());
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
                    reverso(escritor.getDirectContent(), x(posicion, true), y(posicion));
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
        cb.restoreState();
    }

    private void frente(PdfContentByte cb, ReporteMesaDTO reporte, CertificadoVotacionDTO persona,
            String fecha, String codigo, float x, float y) throws DocumentException {
        marco(cb, x, y);
        Image imagen = Image.getInstance(logo);
        imagen.scaleToFit(45, 22);
        imagen.setAbsolutePosition(x + 7, y + ALTO - 27);
        cb.addImage(imagen);
        centrar(cb, mensaje("titulo"), x + 132, y + ALTO - 15, 7.6f, true, AZUL);
        centrar(cb, fecha, x + 132, y + ALTO - 25, 7, false, TEXTO);
        cb.saveState();
        cb.setColorStroke(AZUL);
        cb.setLineWidth(0.7f);
        cb.moveTo(x + 7, y + ALTO - 31);
        cb.lineTo(x + ANCHO - 7, y + ALTO - 31);
        cb.stroke();
        cb.restoreState();

        String[][] campos = {
            {"provincia", persona.provincia()}, {"canton", persona.canton()},
            {"parroquia", persona.parroquia()}, {"iglesia", persona.iglesia()},
            {"comunidad", persona.comunidad()}, {"recinto", reporte.getRecinto().getNombre()},
            {"mesa", reporte.getMesa().getNombre()}, {"documento", persona.documento()},
            {"nombres", (texto(persona.nombres()) + " " + texto(persona.apellidos())).trim()}
        };
        boolean cabe = false;
        for (int decimas = 70; decimas >= 60; decimas -= 2) {
            float tamano = decimas / 10f;
            Phrase datos = new Phrase();
            for (int i = 0; i < campos.length; i++) {
                if (i > 0) datos.add(new Chunk("\n", fuente(tamano, false, TEXTO)));
                datos.add(new Chunk(mensaje(campos[i][0]) + ": ", fuente(tamano, true, TEXTO)));
                datos.add(new Chunk(texto(campos[i][1]), fuente(tamano, false, TEXTO)));
            }
            ColumnText columna = new ColumnText(cb);
            columna.setSimpleColumn(datos, x + 8, y + 32, x + ANCHO - 8, y + ALTO - 34,
                    tamano + 0.6f, Element.ALIGN_LEFT);
            if (!ColumnText.hasMoreText(columna.go(true))) {
                columna.setText(datos);
                columna.setYLine(y + ALTO - 34);
                columna.go();
                cabe = true;
                break;
            }
        }
        if (!cabe) throw new DocumentException("Los datos exceden el area legible del certificado");

        Barcode128 barras = new Barcode128();
        barras.setCode(codigo);
        barras.setFont(null);
        barras.setX(0.7f);
        barras.setBarHeight(17f);
        Image imagenBarras = barras.createImageWithBarcode(cb, BaseColor.BLACK, BaseColor.BLACK);
        // No escalar: conserva modulo de 0,7 pt y zonas silenciosas mayores a 10 modulos.
        imagenBarras.setAbsolutePosition(x + (ANCHO - imagenBarras.getScaledWidth()) / 2, y + 12);
        cb.addImage(imagenBarras);
        centrar(cb, codigo, x + ANCHO / 2, y + 6, 4.8f, false, TEXTO);
    }

    private void reverso(PdfContentByte cb, float x, float y) throws DocumentException {
        marco(cb, x, y);
        ColumnText texto = new ColumnText(cb);
        texto.setSimpleColumn(new Phrase(mensaje("acredita"), fuente(9, false, TEXTO)),
                x + 20, y + 76, x + ANCHO - 20, y + 112, 12, Element.ALIGN_CENTER);
        if (ColumnText.hasMoreText(texto.go())) throw new DocumentException("Texto del reverso excedido");
        cb.saveState();
        cb.setColorStroke(AZUL);
        cb.setLineWidth(0.5f);
        cb.moveTo(x + 32, y + 35);
        cb.lineTo(x + ANCHO - 32, y + 35);
        cb.stroke();
        cb.restoreState();
        centrar(cb, mensaje("firma"), x + ANCHO / 2, y + 23, 7.5f, true, AZUL);
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
