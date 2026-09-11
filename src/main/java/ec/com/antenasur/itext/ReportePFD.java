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
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.FontProvider;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.Barcode;
import com.itextpdf.text.pdf.Barcode128;
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
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import ec.com.antenasur.dto.EscrutinioDTO;
import ec.com.antenasur.dto.CertificadoVotacionDTO;
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
    private static final BaseColor COLOR_SUBTOTAL = new BaseColor(246, 248, 250);
    private static final BaseColor COLOR_TOTAL = new BaseColor(235, 241, 247);
    private static final float[] ANCHOS_ACTA_PARCIAL = new float[]{50, 18, 32};
    private static final int MAX_LISTAS_ACTA_PARCIAL = 9;
    private static ByteArrayOutputStream baos;

    private static Document document;

    private static PdfWriter writer;

    private static String PATH_LOGO;

    private static PdfPTable table;

    private static XMLWorkerHelper worker;

    private static InputStream inputStream;

    private static Font fuente;

    private static String codigoDocumentoActual;

    /** Certificados institucionales a doble cara, sin modificar el padron. */
    public static byte[] generarCertificadosVotacion(ReporteMesaDTO reporte,
            List<CertificadoVotacionDTO> personas, Date fechaSufragio) throws DocumentException {
        try {
            return CertificadosVotacionPDF.generar(reporte, personas, fechaSufragio,
                    CertificadosVotacionPDF.Recursos.delProyecto());
        } catch (IOException e) {
            throw new DocumentException(e);
        }
    }

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

    /**
     * Genera el formulario en blanco de acta parcial para diligenciamiento manual.
     * El acta final con resultados pertenece exclusivamente al flujo de cierre.
     */
    public static byte[] generarActaParcial(ReporteMesaDTO reporte, String codigo,
            LocalDateTime fechaGeneracion, String usuario) throws DocumentException {
        return generarFormularioActaParcial(reporte, codigo, codigo, fechaGeneracion, usuario);
    }

    /** Conserva compatibilidad con los llamados preexistentes. */
    public static byte[] generarActaParcialPreelectoral(ReporteMesaDTO reporte, String codigo,
            LocalDateTime fechaGeneracion, String usuario) throws DocumentException {
        return generarFormularioActaParcial(reporte, codigo, codigo, fechaGeneracion, usuario);
    }

    public static byte[] generarFormularioActaParcial(ReporteMesaDTO reporte, String codigo,
            LocalDateTime fechaGeneracion, String usuario) throws DocumentException {
        return generarFormularioActaParcial(reporte, codigo, codigo, fechaGeneracion, usuario);
    }

    public static byte[] generarFormularioActaParcial(ReporteMesaDTO reporte, String folio,
            String codigoBarras, LocalDateTime fechaGeneracion, String usuario) throws DocumentException {
        return generarFormularioActaParcial(reporte, folio, codigoBarras, fechaGeneracion, usuario, null);
    }

    public static byte[] generarFormularioActaParcial(ReporteMesaDTO reporte, String folio,
            String codigoBarras, LocalDateTime fechaGeneracion, String usuario, String accesoQr) throws DocumentException {
        if (!ec.com.antenasur.security.qr.TokenActaQr.urlAccesoValida(accesoQr))
            throw new DocumentException(Constantes.getMensaje("actaQr.error.pdf.obligatorio"));
        if (reporte == null || reporte.getMesa() == null || reporte.getProceso() == null) {
            throw new DocumentException("No existe informacion suficiente para generar el acta parcial.");
        }
        var firmantes = reporte.getMiembrosJrv().stream().filter(ReportePFD::esFirmanteActaParcial).toList();
        if (firmantes.size() != 2 || firmantes.stream()
                .filter(m -> m.getCargoNombre().trim().toUpperCase(java.util.Locale.ROOT).startsWith("PRESIDENTE")).count() != 1)
            throw new DocumentException(Constantes.getMensaje("actaQr.error.pdf.firmantes"));
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        PdfInstitucional.Contexto contexto;
        try {
            contexto = PdfInstitucional.crearA4ActaParcial(salida,
                    Constantes.getMensaje("reportesMesa.acta.titulo"),
                    reporte.getProceso().getNombre(), fechaGeneracion);
        } catch (Exception e) {
            throw new DocumentException(e);
        }
        Document pdf = contexto.documento();
        try {
            agregarInformacionActaParcial(pdf, reporte);
            agregarResultadosActaParcial(pdf, reporte, accesoQr != null);
            agregarFirmasJrv(pdf, reporte, accesoQr);
            agregarIdentificacionActaParcial(pdf, contexto.writer(), codigoBarras);
        } finally {
            pdf.close();
        }
        byte[] contenido = salida.toByteArray();
        com.itextpdf.text.pdf.PdfReader comprobacion = null;
        try {
            comprobacion = new com.itextpdf.text.pdf.PdfReader(contenido);
            if (comprobacion.getNumberOfPages() != 1)
                throw new DocumentException(Constantes.getMensaje("actaQr.error.pdf.paginas"));
        } catch (java.io.IOException e) {
            throw new DocumentException(Constantes.getMensaje("actaQr.error.pdf.obligatorio"));
        } finally {
            if (comprobacion != null) comprobacion.close();
        }
        return contenido;
    }

    private static void agregarInformacionActaParcial(Document pdf, ReporteMesaDTO reporte)
            throws DocumentException {
        PdfPTable informacion = new PdfPTable(4);
        informacion.setWidthPercentage(100);
        informacion.setWidths(new float[]{14, 58, 10, 18});
        informacion.setSpacingAfter(8f);
        agregarDatoActa(informacion, Constantes.getMensaje("reportesMesa.filtro.recinto"),
                reporte.getRecinto() != null ? reporte.getRecinto().getNombre() : "", false);
        agregarDatoActa(informacion, Constantes.getMensaje("reportesMesa.filtro.mesa"),
                reporte.getMesa().getNombre(), true);
        agregarDatoActaConExtension(informacion,
                Constantes.getMensaje("reportesMesa.acta.ubicacion.geografica"),
                ubicacionGeografica(reporte), 3, false);
        agregarDatoActa(informacion, Constantes.getMensaje("reportesMesa.acta.fecha.sufragio"),
                reporte.getFechaSufragio() != null
                        ? new SimpleDateFormat("dd/MM/yyyy").format(reporte.getFechaSufragio()) : "", false);
        agregarCeldaInformacionVacia(informacion, 2);
        pdf.add(informacion);
    }

    private static String ubicacionGeografica(ReporteMesaDTO reporte) {
        if (reporte.getRecinto() == null) return "";
        return texto(reporte.getRecinto().getProvinciaNombre()) + " / "
                + texto(reporte.getRecinto().getCantonNombre()) + " / "
                + texto(reporte.getRecinto().getUbicacionNombre());
    }

    private static void agregarResultadosActaParcial(Document pdf, ReporteMesaDTO reporte, boolean conQr)
            throws DocumentException {
        Paragraph titulo = new Paragraph(Constantes.getMensaje("reportesMesa.acta.resultados.titulo"),
                FontFactory.getFont("arial", 10, Font.BOLD, COLOR_INSTITUCIONAL));
        titulo.setSpacingAfter(8f);
        pdf.add(titulo);
        int cantidadListas = (int) reporte.getEscrutinios().stream()
                .filter(ReportePFD::esListaOCandidato).count();
        if (cantidadListas > MAX_LISTAS_ACTA_PARCIAL) {
            throw new DocumentException(Constantes.getMensaje("reportesMesa.acta.error.listas.exceso"));
        }
        float alturaCasilla = alturaCasillaActaParcial(cantidadListas) - (conQr ? 4f : 0f);
        PdfPTable resultados = new PdfPTable(3);
        resultados.setWidthPercentage(100);
        resultados.setWidths(ANCHOS_ACTA_PARCIAL);
        resultados.setHeaderRows(1);
        resultados.setSpacingAfter(7f);
        agregarCabecera(resultados, Constantes.getMensaje("reportesMesa.acta.columna.lista"));
        agregarCabecera(resultados, Constantes.getMensaje("reportesMesa.acta.columna.numeros"));
        agregarCabecera(resultados, Constantes.getMensaje("reportesMesa.acta.columna.letras"));
        for (EscrutinioDTO item : reporte.getEscrutinios()) {
            if (esListaOCandidato(item)) {
                agregarFilaManual(resultados, item.getCategoriaNombre(), Font.NORMAL, alturaCasilla,
                        BaseColor.WHITE, false);
            }
        }
        agregarFilaManual(resultados, Constantes.getMensaje("reportesMesa.acta.subtotal.validos"), Font.BOLD,
                alturaCasilla, COLOR_SUBTOTAL, false);
        agregarFilaManual(resultados, Constantes.getMensaje("reportesMesa.acta.votos.nulos"), Font.NORMAL,
                alturaCasilla, BaseColor.WHITE, false);
        agregarFilaManual(resultados, Constantes.getMensaje("reportesMesa.acta.votos.blancos"), Font.NORMAL,
                alturaCasilla, BaseColor.WHITE, false);
        agregarFilaManual(resultados, Constantes.getMensaje("reportesMesa.acta.total.votos"), Font.BOLD,
                alturaCasilla, COLOR_TOTAL, true);
        pdf.add(resultados);
        agregarPapeletasRestantes(pdf, alturaCasilla);
    }

    private static boolean esListaOCandidato(EscrutinioDTO item) {
        if (item == null || item.getCategoriaNombre() == null) return false;
        String tipo = texto(item.getCategoriaTipo());
        return "LISTA".equalsIgnoreCase(tipo) || "LEGACY".equalsIgnoreCase(tipo);
    }

    private static float alturaCasillaActaParcial(int cantidadListas) {
        if (cantidadListas <= 3) return 31f;
        if (cantidadListas <= 5) return 28f;
        if (cantidadListas <= 8) return 24f;
        return 20f;
    }

    private static void agregarPapeletasRestantes(Document pdf, float alturaCasilla) throws DocumentException {
        PdfPTable control = new PdfPTable(3);
        control.setWidthPercentage(100);
        control.setWidths(ANCHOS_ACTA_PARCIAL);
        control.setSpacingBefore(8f);
        control.setSpacingAfter(10f);
        agregarFilaManual(control, Constantes.getMensaje("reportesMesa.acta.papeletas.restantes"), Font.BOLD,
                alturaCasilla, COLOR_SUBTOTAL, false);
        pdf.add(control);
        agregarHoraFinEscrutinio(pdf);
        agregarObservacionActaParcial(pdf);
    }

    private static void agregarHoraFinEscrutinio(Document pdf) throws DocumentException {
        PdfPTable horaFin = new PdfPTable(new float[]{35, 65});
        horaFin.setWidthPercentage(100);
        horaFin.setSpacingBefore(8f);
        horaFin.setSpacingAfter(8f);
        PdfPCell etiqueta = new PdfPCell(new Phrase(Constantes.getMensaje("reportesMesa.acta.hora.fin") + ":",
                FontFactory.getFont("arial", 8, Font.BOLD, COLOR_INSTITUCIONAL)));
        etiqueta.setBorder(PdfPCell.NO_BORDER);
        etiqueta.setVerticalAlignment(Element.ALIGN_MIDDLE);
        etiqueta.setPadding(4f);
        horaFin.addCell(etiqueta);
        PdfPCell espacio = new PdfPCell();
        espacio.setMinimumHeight(20f);
        espacio.setBorder(PdfPCell.BOTTOM);
        espacio.setBorderColor(COLOR_INSTITUCIONAL);
        espacio.setBorderWidthBottom(0.7f);
        horaFin.addCell(espacio);
        pdf.add(horaFin);
    }

    private static void agregarObservacionActaParcial(Document pdf) throws DocumentException {
        Paragraph titulo = new Paragraph(Constantes.getMensaje("reportesMesa.acta.observacion"),
                FontFactory.getFont("arial", 9, Font.BOLD, COLOR_INSTITUCIONAL));
        titulo.setSpacingAfter(3f);
        pdf.add(titulo);
        PdfPTable lineas = new PdfPTable(1);
        lineas.setWidthPercentage(100);
        lineas.setSpacingAfter(9f);
        for (int indice = 0; indice < 3; indice++) {
            PdfPCell linea = new PdfPCell();
            linea.setMinimumHeight(18f);
            linea.setBorder(PdfPCell.BOTTOM);
            linea.setBorderColor(COLOR_BORDE_TABLA);
            linea.setBorderWidthBottom(0.6f);
            lineas.addCell(linea);
        }
        pdf.add(lineas);
    }

    private static void agregarIdentificacionActaParcial(Document pdf, PdfWriter writer,
            String codigoBarras) throws DocumentException {
        Barcode128 codigo = new Barcode128();
        codigo.setCodeType(Barcode.CODE128);
        codigo.setCode(codigoBarras);
        codigo.setBarHeight(24f);
        codigo.setX(0.9f);
        codigo.setFont(null);
        Image imagen = codigo.createImageWithBarcode(writer.getDirectContent(), BaseColor.BLACK, BaseColor.BLACK);
        imagen.scaleToFit(170f, 30f);
        PdfPTable identificacion = new PdfPTable(1);
        identificacion.setWidthPercentage(100);
        identificacion.setSpacingBefore(12f);
        PdfPCell barras = new PdfPCell(imagen, false);
        barras.setBorder(PdfPCell.NO_BORDER);
        barras.setHorizontalAlignment(Element.ALIGN_CENTER);
        barras.setPaddingTop(3f);
        identificacion.addCell(barras);
        pdf.add(identificacion);
    }

    private static void agregarFilaManual(PdfPTable tabla, String etiqueta, int estilo, float altura,
            BaseColor fondo, boolean bordeSuperiorMarcado) {
        tabla.addCell(crearCeldaActa(etiqueta, Element.ALIGN_LEFT, estilo, altura, fondo, bordeSuperiorMarcado));
        tabla.addCell(crearCasillaManualActa(altura, fondo, bordeSuperiorMarcado));
        tabla.addCell(crearCasillaManualActa(altura, fondo, bordeSuperiorMarcado));
    }

    private static PdfPCell crearCeldaActa(String valor, int alineacion, int estilo, float altura,
            BaseColor fondo, boolean bordeSuperiorMarcado) {
        Paragraph contenido = new Paragraph(texto(valor),
                FontFactory.getFont("arial", 8, estilo, BaseColor.BLACK));
        contenido.setAlignment(alineacion);
        PdfPCell celda = new PdfPCell(contenido);
        celda.setMinimumHeight(altura);
        celda.setBorderColor(COLOR_BORDE_TABLA);
        celda.setBackgroundColor(fondo);
        celda.setPadding(5f);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setHorizontalAlignment(alineacion);
        if (bordeSuperiorMarcado) celda.setBorderWidthTop(1.4f);
        return celda;
    }

    private static PdfPCell crearCasillaManualActa(float altura, BaseColor fondo,
            boolean bordeSuperiorMarcado) {
        PdfPCell celda = new PdfPCell();
        celda.setMinimumHeight(altura);
        celda.setBorderColor(COLOR_BORDE_TABLA);
        celda.setBackgroundColor(fondo);
        celda.setPadding(4f);
        if (bordeSuperiorMarcado) celda.setBorderWidthTop(1.4f);
        return celda;
    }

    private static void agregarFirmasJrv(Document pdf, ReporteMesaDTO reporte, String accesoQr) throws DocumentException {
        Paragraph titulo = new Paragraph(Constantes.getMensaje("reportesMesa.acta.firmas.titulo"),
                FontFactory.getFont("arial", 10, Font.BOLD, COLOR_INSTITUCIONAL));
        titulo.setSpacingAfter(4f);
        pdf.add(titulo);

        PdfPTable firmas = new PdfPTable(accesoQr == null ? 2 : 3);
        firmas.setWidthPercentage(100);
        firmas.setWidths(accesoQr == null ? new float[]{50, 50} : new float[]{40, 40, 20});
        firmas.setSpacingBefore(10f);
        int firmantesAgregados = 0;
        for (MiembroJRVDTO miembro : reporte.getMiembrosJrv()) {
            if (!esFirmanteActaParcial(miembro)) {
                continue;
            }
            String nombre = "";
            if (miembro.getIglesiaPersona() != null) {
                if (miembro.getIglesiaPersona().getPersona() != null) {
                    nombre = texto(miembro.getIglesiaPersona().getPersona().getNombres()) + " "
                            + texto(miembro.getIglesiaPersona().getPersona().getApellidos());
                }
            }
            Paragraph contenido = new Paragraph();
            contenido.add(new Chunk("\n\n________________________\n",
                    FontFactory.getFont("arial", 8, Font.NORMAL, BaseColor.BLACK)));
            contenido.add(new Chunk(nombre.trim() + "\n",
                    FontFactory.getFont("arial", 8, Font.NORMAL, BaseColor.BLACK)));
            contenido.add(new Chunk(texto(miembro.getCargoNombre()),
                    FontFactory.getFont("arial", 8, Font.BOLD, BaseColor.BLACK)));
            PdfPCell celda = new PdfPCell(contenido);
            celda.setBorder(PdfPCell.NO_BORDER);
            celda.setHorizontalAlignment(Element.ALIGN_CENTER);
            celda.setMinimumHeight(68f);
            celda.setVerticalAlignment(Element.ALIGN_BOTTOM);
            celda.setPadding(4f);
            firmas.addCell(celda);
            firmantesAgregados++;
        }
        if (firmantesAgregados % 2 != 0) {
            PdfPCell vacia = new PdfPCell();
            vacia.setBorder(PdfPCell.NO_BORDER);
            firmas.addCell(vacia);
        }
        if (accesoQr != null) {
            Image qr = new BarcodeQRCode(accesoQr, 320, 320, null).getImage();
            qr.scaleAbsolute(78f, 78f);
            qr.setAlignment(Element.ALIGN_CENTER);
            PdfPCell celdaQr = new PdfPCell();
            celdaQr.setBorder(PdfPCell.NO_BORDER);
            celdaQr.addElement(qr);
            Paragraph aviso = new Paragraph(Constantes.getMensaje("actaQr.pdf.acceso"),
                    FontFactory.getFont("arial", 7, Font.NORMAL, BaseColor.BLACK));
            aviso.setAlignment(Element.ALIGN_CENTER);
            celdaQr.addElement(aviso);
            firmas.addCell(celdaQr);
        }
        pdf.add(firmas);
    }

    private static boolean esFirmanteActaParcial(MiembroJRVDTO miembro) {
        if (miembro == null || miembro.getCargoNombre() == null) return false;
        String cargo = miembro.getCargoNombre().trim().toUpperCase(java.util.Locale.ROOT);
        return "PRESIDENTE".equals(cargo) || "PRESIDENTE DE MESA".equals(cargo)
                || "SECRETARIO".equals(cargo) || "SECRETARIO DE MESA".equals(cargo);
    }

    private static void agregarDato(PdfPTable tabla, String etiqueta, String valor) {
        agregarCelda(tabla, etiqueta, Element.ALIGN_LEFT, Font.BOLD);
        agregarCelda(tabla, texto(valor), Element.ALIGN_LEFT, Font.NORMAL);
    }

    private static void agregarDatoActa(PdfPTable tabla, String etiqueta, String valor, boolean mesa) {
        Font etiquetaFont = FontFactory.getFont("arial", 8, Font.BOLD, COLOR_INSTITUCIONAL);
        Font valorFont = FontFactory.getFont("arial", mesa ? 12 : 8, mesa ? Font.BOLD : Font.NORMAL,
                BaseColor.BLACK);
        PdfPCell etiquetaCelda = new PdfPCell(new Phrase(texto(etiqueta).toUpperCase() + ":", etiquetaFont));
        etiquetaCelda.setBorder(PdfPCell.NO_BORDER);
        etiquetaCelda.setBackgroundColor(COLOR_CABECERA_TABLA);
        etiquetaCelda.setPadding(5f);
        etiquetaCelda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tabla.addCell(etiquetaCelda);
        PdfPCell valorCelda = new PdfPCell(new Phrase(texto(valor), valorFont));
        valorCelda.setBorder(PdfPCell.NO_BORDER);
        valorCelda.setBackgroundColor(COLOR_CABECERA_TABLA);
        valorCelda.setPadding(5f);
        valorCelda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (texto(valor).isBlank()) {
            valorCelda.setBorder(PdfPCell.BOTTOM);
            valorCelda.setBorderColor(COLOR_INSTITUCIONAL);
            valorCelda.setBorderWidthBottom(0.7f);
        }
        tabla.addCell(valorCelda);
    }

    private static void agregarDatoActaConExtension(PdfPTable tabla, String etiqueta, String valor,
            int columnasValor, boolean destacado) {
        agregarDatoActaConExtension(tabla, etiqueta, valor, columnasValor, destacado, 0f);
    }

    private static void agregarDatoActaConExtension(PdfPTable tabla, String etiqueta, String valor,
            int columnasValor, boolean destacado, float alturaMinima) {
        Font etiquetaFont = FontFactory.getFont("arial", 8, Font.BOLD, COLOR_INSTITUCIONAL);
        Font valorFont = FontFactory.getFont("arial", destacado ? 10 : 8,
                destacado ? Font.BOLD : Font.NORMAL, BaseColor.BLACK);
        PdfPCell etiquetaCelda = new PdfPCell(new Phrase(texto(etiqueta).toUpperCase() + ":", etiquetaFont));
        etiquetaCelda.setBorder(PdfPCell.NO_BORDER);
        etiquetaCelda.setBackgroundColor(COLOR_CABECERA_TABLA);
        etiquetaCelda.setPadding(5f);
        etiquetaCelda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (alturaMinima > 0) etiquetaCelda.setMinimumHeight(alturaMinima);
        tabla.addCell(etiquetaCelda);
        PdfPCell valorCelda = new PdfPCell(new Phrase(texto(valor), valorFont));
        valorCelda.setColspan(columnasValor);
        valorCelda.setBorder(PdfPCell.NO_BORDER);
        valorCelda.setBackgroundColor(COLOR_CABECERA_TABLA);
        valorCelda.setPadding(5f);
        valorCelda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (texto(valor).isBlank()) {
            valorCelda.setBorder(PdfPCell.BOTTOM);
            valorCelda.setBorderColor(COLOR_INSTITUCIONAL);
            valorCelda.setBorderWidthBottom(0.7f);
        }
        if (alturaMinima > 0) valorCelda.setMinimumHeight(alturaMinima);
        tabla.addCell(valorCelda);
    }

    private static void agregarCeldaInformacionVacia(PdfPTable tabla, int columnas) {
        PdfPCell celda = new PdfPCell();
        celda.setColspan(columnas);
        celda.setBorder(PdfPCell.NO_BORDER);
        celda.setBackgroundColor(COLOR_CABECERA_TABLA);
        tabla.addCell(celda);
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
