package ec.com.antenasur.itext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletResponse;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.FontProvider;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.BarcodeQRCode;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.JsfUtil;
import ec.com.antenasur.util.RepositorioDocumentos;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import ec.com.antenasur.dto.EscrutinioDTO;
import ec.com.antenasur.dto.MiembroJRVDTO;
import ec.com.antenasur.dto.ReporteMesaDTO;

/**
 *
 * Genera documentos PDF
 *
 */
public class ReportePFD {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ReportePFD.class);
    private static final BaseColor COLOR_INSTITUCIONAL = new BaseColor(24, 82, 133);
    private static final BaseColor COLOR_CABECERA_TABLA = new BaseColor(232, 240, 248);
    private static final BaseColor COLOR_BORDE_TABLA = new BaseColor(210, 220, 230);
    private static ByteArrayOutputStream baos;

    private static Document document;

    private static PdfWriter writer;

    private static String PATH_LOGO;

    private static PdfPTable table;

    private static XMLWorkerHelper worker;

    private static InputStream inputStream;

    private static Font fuente;

    private static String codigoDocumentoActual;

    private static void inicializa() {
        try {
            worker = XMLWorkerHelper.getInstance();
            /*Agrega Banner cabecera al documento*/
            PATH_LOGO = Constantes.getPathLogo();
        } catch (Exception e) {
            LOG.error("ERROR AL INICIALIZAR VALORES" + e);
        }
    }

    public static void nuevoPDF(String nombreReporte) {
        try {
            inicializa();
            codigoDocumentoActual = nombreReporte;
            baos = new ByteArrayOutputStream();
            PdfInstitucional.Contexto contexto = PdfInstitucional.crearA4(
                    baos, nombreReporte, nombreReporte, LocalDateTime.now());
            document = contexto.documento();
            writer = contexto.writer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void nuevoPDFHorizontal(String nombreReporte) {
        try {
            inicializa();
            codigoDocumentoActual = nombreReporte;
            baos = new ByteArrayOutputStream();
            PdfInstitucional.Contexto contexto = PdfInstitucional.crearA4Horizontal(
                    baos, nombreReporte, nombreReporte, LocalDateTime.now());
            document = contexto.documento();
            writer = contexto.writer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void creaTablaCabecera(int numColumns, float[] columWidth, String tableTitle,
            String[] listColumNames, Font fuente) {
        try {
            table = new PdfPTable(numColumns);
            table.setWidthPercentage(100);
            table.setSpacingBefore(8f);
            table.setSpacingAfter(10f);
            addTableToDocument(numColumns, columWidth, tableTitle, listColumNames, fuente);

        } catch (Exception e) {
            LOG.error("ERROR AL CREAR CABECERA DE TABLA PDF", e);
        }
    }

    public static void addTableHeader(int numColumns, String tableTitle, Font fuente) {

        Font fuenteTituloTabla = FontFactory.getFont("arial", 9, Font.BOLD, BaseColor.WHITE);
        PdfPCell cell = new PdfPCell(new Paragraph(tableTitle, fuenteTituloTabla));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(7f);
        cell.setPaddingBottom(7f);
        cell.setPaddingLeft(6f);
        cell.setPaddingRight(6f);
        cell.setBackgroundColor(COLOR_INSTITUCIONAL);
        cell.setBorderColor(COLOR_INSTITUCIONAL);
        cell.setColspan(numColumns);
        table.addCell(cell);
    }

    public static void setMetadataDocument(Document document, String nombreReporte) {
        try {
            PdfInstitucional.aplicarMetadata(document, nombreReporte);
        } catch (Exception e) {
            LOG.error("ERROR AL ASIGNAR METADATOS PDF", e);
        }

    }

    /** Genera un acta informativa sin modificar ni cerrar el escrutinio de la mesa. */
    public static byte[] generarActaParcial(ReporteMesaDTO reporte, String codigo,
            LocalDateTime fechaGeneracion, String usuario) throws DocumentException {
        if (reporte == null || reporte.getMesa() == null || reporte.getProceso() == null) {
            throw new DocumentException("No existe informacion suficiente para generar el acta parcial.");
        }
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        PdfInstitucional.Contexto contexto;
        try {
            contexto = PdfInstitucional.crearA4(salida, codigo,
                    Constantes.getMensaje("reportesMesa.acta.titulo"), fechaGeneracion);
        } catch (Exception e) {
            throw new DocumentException(e);
        }
        Document pdf = contexto.documento();
        try {
            agregarInformacionActaParcial(pdf, reporte, fechaGeneracion, usuario);
            agregarResultadosActaParcial(pdf, reporte);
            agregarFirmasJrv(pdf, reporte);
        } finally {
            pdf.close();
        }
        return salida.toByteArray();
    }

    private static void agregarInformacionActaParcial(Document pdf, ReporteMesaDTO reporte,
            LocalDateTime fecha, String usuario) throws DocumentException {
        PdfPTable informacion = new PdfPTable(2);
        informacion.setWidthPercentage(100);
        informacion.setWidths(new float[]{25, 75});
        informacion.setSpacingAfter(12f);
        agregarDato(informacion, Constantes.getMensaje("reportesMesa.filtro.proceso"),
                reporte.getProceso().getNombre());
        agregarDato(informacion, Constantes.getMensaje("reportesMesa.filtro.recinto"),
                reporte.getRecinto() != null ? reporte.getRecinto().getNombre() : "");
        agregarDato(informacion, Constantes.getMensaje("reportesMesa.filtro.mesa"),
                reporte.getMesa().getNombre());
        agregarDato(informacion, Constantes.getMensaje("reportesMesa.documento.fecha"),
                fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        agregarDato(informacion, Constantes.getMensaje("reportesMesa.documento.responsable"), usuario);
        pdf.add(informacion);
    }

    private static void agregarResultadosActaParcial(Document pdf, ReporteMesaDTO reporte)
            throws DocumentException {
        PdfPTable resultados = new PdfPTable(2);
        resultados.setWidthPercentage(100);
        resultados.setWidths(new float[]{80, 20});
        resultados.setHeaderRows(1);
        resultados.setSpacingAfter(14f);
        agregarCabecera(resultados, Constantes.getMensaje("reportesMesa.escrutinio.categoria"));
        agregarCabecera(resultados, Constantes.getMensaje("reportesMesa.escrutinio.votos"));
        for (EscrutinioDTO item : reporte.getEscrutinios()) {
            agregarCelda(resultados, item.getCategoriaNombre(), Element.ALIGN_LEFT, Font.NORMAL);
            agregarCelda(resultados, String.valueOf(valor(item.getTotalVotos())), Element.ALIGN_RIGHT, Font.NORMAL);
        }
        if (reporte.getCabecera() != null) {
            agregarFilaTotal(resultados, Constantes.getMensaje("reportesMesa.total.validos"),
                    reporte.getCabecera().getTotalVotosValidos());
            agregarFilaTotal(resultados, Constantes.getMensaje("reportesMesa.total.nulos"),
                    reporte.getCabecera().getTotalVotosNulos());
            agregarFilaTotal(resultados, Constantes.getMensaje("reportesMesa.total.blancos"),
                    reporte.getCabecera().getTotalVotosBlancos());
            agregarFilaTotal(resultados, Constantes.getMensaje("reportesMesa.total.registrados"),
                    reporte.getCabecera().getTotalVotosRegistrados());
        }
        pdf.add(resultados);
    }

    private static void agregarFirmasJrv(Document pdf, ReporteMesaDTO reporte) throws DocumentException {
        Paragraph titulo = new Paragraph(Constantes.getMensaje("reportesMesa.jrv.titulo"),
                FontFactory.getFont("arial", 10, Font.BOLD, COLOR_INSTITUCIONAL));
        titulo.setSpacingAfter(10f);
        pdf.add(titulo);

        PdfPTable firmas = new PdfPTable(2);
        firmas.setWidthPercentage(100);
        firmas.setWidths(new float[]{50, 50});
        firmas.setSpacingBefore(20f);
        for (MiembroJRVDTO miembro : reporte.getMiembrosJrv()) {
            String nombre = "";
            String documentoPersona = "";
            String iglesia = "";
            if (miembro.getIglesiaPersona() != null) {
                if (miembro.getIglesiaPersona().getPersona() != null) {
                    nombre = texto(miembro.getIglesiaPersona().getPersona().getNombres()) + " "
                            + texto(miembro.getIglesiaPersona().getPersona().getApellidos());
                    documentoPersona = texto(miembro.getIglesiaPersona().getPersona().getDocumento());
                }
                if (miembro.getIglesiaPersona().getIglesia() != null) {
                    iglesia = texto(miembro.getIglesiaPersona().getIglesia().getNombre());
                }
            }
            String contenido = "\n\n_______________________________\n"
                    + texto(miembro.getCargoNombre()) + "\n" + nombre.trim() + "\n"
                    + Constantes.getMensaje("reportesMesa.jrv.cedula") + ": " + documentoPersona + "\n"
                    + Constantes.getMensaje("reportesMesa.jrv.iglesia") + ": " + iglesia;
            PdfPCell celda = new PdfPCell(new Paragraph(contenido,
                    FontFactory.getFont("arial", 8, Font.NORMAL, BaseColor.BLACK)));
            celda.setBorder(PdfPCell.NO_BORDER);
            celda.setHorizontalAlignment(Element.ALIGN_CENTER);
            celda.setPadding(8f);
            firmas.addCell(celda);
        }
        if (reporte.getMiembrosJrv().size() % 2 != 0) {
            PdfPCell vacia = new PdfPCell();
            vacia.setBorder(PdfPCell.NO_BORDER);
            firmas.addCell(vacia);
        }
        pdf.add(firmas);
    }

    private static void agregarDato(PdfPTable tabla, String etiqueta, String valor) {
        agregarCelda(tabla, etiqueta, Element.ALIGN_LEFT, Font.BOLD);
        agregarCelda(tabla, texto(valor), Element.ALIGN_LEFT, Font.NORMAL);
    }

    private static void agregarCabecera(PdfPTable tabla, String valor) {
        PdfPCell celda = new PdfPCell(new Paragraph(valor,
                FontFactory.getFont("arial", 8, Font.BOLD, COLOR_INSTITUCIONAL)));
        celda.setBackgroundColor(COLOR_CABECERA_TABLA);
        celda.setBorderColor(COLOR_BORDE_TABLA);
        celda.setPadding(6f);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        tabla.addCell(celda);
    }

    private static void agregarFilaTotal(PdfPTable tabla, String etiqueta, Integer valor) {
        agregarCelda(tabla, etiqueta, Element.ALIGN_RIGHT, Font.BOLD);
        agregarCelda(tabla, String.valueOf(valor(valor)), Element.ALIGN_RIGHT, Font.BOLD);
    }

    private static void agregarCelda(PdfPTable tabla, String valor, int alineacion, int estilo) {
        PdfPCell celda = new PdfPCell(new Paragraph(texto(valor),
                FontFactory.getFont("arial", 8, estilo, BaseColor.BLACK)));
        celda.setBorderColor(COLOR_BORDE_TABLA);
        celda.setPadding(5f);
        celda.setHorizontalAlignment(alineacion);
        tabla.addCell(celda);
    }

    private static int valor(Integer numero) {
        return numero != null ? numero : 0;
    }

    private static String texto(String valor) {
        return valor != null ? valor : "";
    }

    public static void addTableToDocument(int numColumns, float[] columWidth, String tableTitle,
            String[] listColumNames, Font fuente) {
        try {
            table.setTotalWidth(columWidth);
            addTableHeader(numColumns, tableTitle, fuente);

            Font fuenteEncabezado = FontFactory.getFont("arial", 8, Font.BOLD, COLOR_INSTITUCIONAL);
            for (String columName : listColumNames) {
                PdfPCell header = new PdfPCell(new Paragraph(columName, fuenteEncabezado));
                header.setBackgroundColor(COLOR_CABECERA_TABLA);
                header.setBorderColor(COLOR_BORDE_TABLA);
                header.setPaddingTop(6f);
                header.setPaddingBottom(6f);
                header.setPaddingLeft(5f);
                header.setPaddingRight(5f);
                header.setHorizontalAlignment(Element.ALIGN_CENTER);
                header.setVerticalAlignment(Element.ALIGN_MIDDLE);
                table.addCell(header);
            }
            table.setHeaderRows(2);
        } catch (DocumentException ex) {
            Logger.getLogger(ReportePFD.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void creaContenidoTabla(String[][] listaDatos, String[] listColumnNames, Font fuente) {
        try {
            for (String[] medio : listaDatos) {
                for (int i = 0; i < medio.length; i++) {
                    PdfPCell celda = new PdfPCell(new Paragraph(medio[i], fuente));
                    celda.setPadding(5f);
                    celda.setBorderColor(COLOR_BORDE_TABLA);
                    celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    celda.setHorizontalAlignment(esNumero(medio[i]) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                    table.addCell(celda);
                }
            }
            document.add(table);
        } catch (Exception e) {
            LOG.error("ERROR AL CREAR CONTENIDO DE TABLA" + e);
        }
    }

    private static boolean esNumero(String valor) {
        return valor != null && valor.trim().matches("-?\\d+(\\.\\d+)?");
    }

    public static void addImagen(String rutaImagen, float fitWidth, float fitHeight, int alignment, Document document)
            throws DocumentException {
        try {
            Image foto = Image.getInstance(rutaImagen);
            foto.scaleToFit(fitWidth, fitHeight);
            foto.setAlignment(alignment);
            document.add(foto);
        } catch (BadElementException | IOException ex) {
            Logger.getLogger(ReportePFD.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void addParagraph(String string) {
        try {
            Paragraph paragraph = new Paragraph(string, FontFactory.getFont("arial", 9, Font.NORMAL, BaseColor.BLACK));
            paragraph.setSpacingAfter(6f);
            paragraph.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(paragraph);
        } catch (DocumentException ex) {
            Logger.getLogger(ReportePFD.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void agregaTituloSeccion(String titulo) {
        try {
            Paragraph paragraph = new Paragraph(titulo,
                    FontFactory.getFont("arial", 11, Font.BOLD, COLOR_INSTITUCIONAL));
            paragraph.setSpacingBefore(10f);
            paragraph.setSpacingAfter(6f);
            document.add(paragraph);
        } catch (DocumentException ex) {
            Logger.getLogger(ReportePFD.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static String getCodigoDocumentoActual() {
        return codigoDocumentoActual != null ? codigoDocumentoActual : "";
    }

    public static void descargarPDF(ByteArrayOutputStream baos, String nombreReporte) {
        try {
            HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
            OutputStream out = response.getOutputStream();
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=\"" + nombreReporte + ".pdf\"");
            response.setDateHeader("Expires", 0);

            try {
                baos.writeTo(out);
                out.flush();
            } catch (IOException ex) {
                ex.getStackTrace();
                Logger.getLogger(ReportePFD.class.getName()).log(Level.SEVERE, null, ex);
            }
            out.flush();
            FacesContext.getCurrentInstance().responseComplete();
        } catch (Exception e) {
            e.getStackTrace();
            Logger.getLogger(ReportePFD.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public static void descargarPDF(String nombreReporte) {
        try {
            HttpServletResponse response = JsfUtil.getHttpServletResponse();
            OutputStream out = response.getOutputStream();
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=\"" + nombreReporte + ".pdf\"");
            response.setDateHeader("Expires", 0);

            try {
                baos.writeTo(out);
                out.flush();
            } catch (IOException ex) {
                ex.getStackTrace();
                Logger.getLogger(ReportePFD.class.getName()).log(Level.SEVERE, null, ex);
            }
            out.flush();
            FacesContext.getCurrentInstance().responseComplete();
        } catch (Exception e) {
            e.getStackTrace();
            Logger.getLogger(ReportePFD.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public static void guardarDocumentosActasE(String nombreDocumento) {
        try {
            guardarDocumentosActasEObligatorio(nombreDocumento);
        } catch (IOException e) {
            LOG.error("ERROR AL GUARDAR ARCHIVOS" + nombreDocumento, e);
        }

    }

    public static String guardarDocumentosActasEObligatorio(String nombreDocumento) throws IOException {
        if (baos == null || baos.size() == 0) {
            throw new IOException("No existe contenido PDF generado para guardar.");
        }
        Path path = RepositorioDocumentos.escribirAtomico(
                "actas-escrutinio", nombreDocumento + ".pdf", baos.toByteArray());
        return path.toString();
    }

    public static String calcularHashSha256Actual() throws IOException {
        if (baos == null || baos.size() == 0) {
            throw new IOException("No existe contenido PDF generado para calcular hash.");
        }
        return calcularSha256(baos.toByteArray());
    }

    public static String calcularSha256(byte[] contenido) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(contenido);
            StringBuilder resultado = new StringBuilder();
            for (byte b : hash) {
                resultado.append(String.format("%02x", b));
            }
            return resultado.toString();
        } catch (Exception e) {
            throw new IOException("No se pudo calcular el hash SHA-256 del documento.", e);
        }
    }

    public static String calcularSha256(Path path) throws IOException {
        return calcularSha256(Files.readAllBytes(path));
    }

    public static void agregaCodigoVerificacion(String codigoActa, String contenidoQr) {
        try {
            PdfPTable tabla = new PdfPTable(2);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{75, 25});

            Paragraph texto = new Paragraph("Codigo de verificacion: " + codigoActa + "\n"
                    + "Este documento puede verificarse con el codigo institucional impreso en el acta.",
                    FontFactory.getFont("arial", 8, Font.NORMAL, BaseColor.BLACK));
            PdfPCell celdaTexto = new PdfPCell(texto);
            celdaTexto.setBorder(PdfPCell.NO_BORDER);
            celdaTexto.setVerticalAlignment(Element.ALIGN_MIDDLE);
            tabla.addCell(celdaTexto);

            BarcodeQRCode qr = new BarcodeQRCode(contenidoQr, 90, 90, null);
            Image qrImage = qr.getImage();
            qrImage.scaleToFit(70, 70);
            PdfPCell celdaQr = new PdfPCell(qrImage, false);
            celdaQr.setBorder(PdfPCell.NO_BORDER);
            celdaQr.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tabla.addCell(celdaQr);

            document.add(tabla);
        } catch (Exception e) {
            LOG.error("ERROR AL AGREGAR CODIGO QR AL PDF", e);
        }
    }

    public static void getFinalParagraph(String nombreUsuario) {
        try {
            String date = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
            String finalParagraph = "\t Documento generado por: " + nombreUsuario + " Fecha: " + date.substring(0, 10)
                    + " Hora: " + date.substring(11, 19);

            Paragraph parrafo = new Paragraph(finalParagraph,
                    FontFactory.getFont("arial", 8, Font.ITALIC, BaseColor.BLACK));
            parrafo.setAlignment(Element.ALIGN_RIGHT);
            document.add(parrafo);
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void cerrarDocumento() {
        try {
            if (document != null && document.isOpen()) {
                document.close();
            }
        } catch (Exception e) {
            LOG.error("ERROR AL CERRAR DOCUMENTO PDF", e);
        }
    }

    public static void agregaParrafoEnBlanco() {
        try {
            Paragraph parrafo = new Paragraph("\n",
                    FontFactory.getFont("arial", 8, Font.ITALIC, BaseColor.BLACK));
            parrafo.setAlignment(Element.ALIGN_RIGHT);
            document.add(parrafo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void agregaParrafoObservacion(String observacion) {
        try {
            Paragraph parrafo = new Paragraph("\n" + observacion,
                    FontFactory.getFont("arial", 8, Font.ITALIC, BaseColor.RED));
            parrafo.setAlignment(Element.ALIGN_LEFT);
            document.add(parrafo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void agregaHTML(String texto, String css, FontProvider fontProvider) {
        try {
            InputStream inputStreamCss = new ByteArrayInputStream(css.getBytes(("UTF-8")));
            inputStream = new ByteArrayInputStream(texto.getBytes(("UTF-8")));
            worker.parseXHtml(writer, document, inputStream, inputStreamCss, Charset.forName("UTF-8"), fontProvider);
        } catch (IOException ex) {
            Logger.getLogger(ReportePFD.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
