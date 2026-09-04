package ec.com.antenasur.itext;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BarcodeQRCode;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;

import ec.com.antenasur.dto.IglesiaDTO;
import ec.com.antenasur.dto.IglesiaPersonaDTO;
import ec.com.antenasur.util.Constantes;

/** Genera el acta institucional de actualizacion de miembros de una iglesia. */
public final class ActaActualizacionMiembrosPdf {

    private static final BaseColor AZUL_INSTITUCIONAL = new BaseColor(24, 82, 133);

    private ActaActualizacionMiembrosPdf() {
    }

    public static byte[] generar(IglesiaDTO iglesia, String procesoNombre,
            List<IglesiaPersonaDTO> miembros, String presidenteTribunal,
            String secretarioTribunal, String administradorIglesia,
            LocalDateTime fechaGeneracion, String codigoDocumento, String payloadQr) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            PdfInstitucional.Contexto contexto = PdfInstitucional.crearA4(salida, codigoDocumento,
                    Constantes.getMensaje("actaActualizacion.pdf.titulo"), fechaGeneracion);
            Document documento = contexto.documento();
            try {
                Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, AZUL_INSTITUCIONAL);
                Font subtitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.BLACK);
                Font normal = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.BLACK);
                Font pequeno = FontFactory.getFont(FontFactory.HELVETICA, 7, BaseColor.DARK_GRAY);

                agregarTitulo(documento, titulo, subtitulo);
                agregarDatos(documento, iglesia, procesoNombre, miembros.size(), fechaGeneracion, subtitulo, normal);
                agregarMiembros(documento, miembros, subtitulo, normal);
                agregarFirmas(documento, presidenteTribunal, secretarioTribunal, administradorIglesia, normal);
                agregarVerificacion(documento, codigoDocumento, payloadQr, pequeno);
            } finally {
                documento.close();
            }
            return salida.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo construir el acta de actualizacion de miembros.", e);
        }
    }

    private static void agregarTitulo(Document documento, Font titulo, Font subtitulo) throws Exception {
        Paragraph institucion = new Paragraph(Constantes.INSTITUCION, titulo);
        institucion.setAlignment(Element.ALIGN_CENTER);
        documento.add(institucion);

        Paragraph encabezado = new Paragraph(Constantes.getMensaje("actaActualizacion.pdf.titulo"), subtitulo);
        encabezado.setAlignment(Element.ALIGN_CENTER);
        encabezado.setSpacingAfter(12f);
        documento.add(encabezado);
    }

    private static void agregarDatos(Document documento, IglesiaDTO iglesia, String procesoNombre,
            int totalMiembros, LocalDateTime fechaGeneracion, Font etiqueta, Font valor) throws Exception {
        PdfPTable datos = new PdfPTable(new float[]{1.4f, 3.6f});
        datos.setWidthPercentage(100);
        agregarDato(datos, Constantes.getMensaje("actaActualizacion.pdf.proceso"), procesoNombre, etiqueta, valor);
        agregarDato(datos, Constantes.getMensaje("actaActualizacion.pdf.iglesia"),
                iglesia != null ? iglesia.getNombre() : "", etiqueta, valor);
        agregarDato(datos, Constantes.getMensaje("actaActualizacion.pdf.comunidad"),
                iglesia != null ? iglesia.getComunidad() : "", etiqueta, valor);
        agregarDato(datos, Constantes.getMensaje("actaActualizacion.pdf.totalMiembros"),
                String.valueOf(totalMiembros), etiqueta, valor);
        agregarDato(datos, Constantes.getMensaje("actaActualizacion.pdf.fecha"),
                fechaGeneracion.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), etiqueta, valor);
        documento.add(datos);
    }

    private static void agregarMiembros(Document documento, List<IglesiaPersonaDTO> miembros,
            Font cabecera, Font normal) throws Exception {
        Paragraph seccion = new Paragraph(Constantes.getMensaje("actaActualizacion.pdf.miembros"), cabecera);
        seccion.setSpacingBefore(13f);
        seccion.setSpacingAfter(6f);
        documento.add(seccion);

        PdfPTable tabla = new PdfPTable(new float[]{0.45f, 1.3f, 3.5f, 0.85f});
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        agregarCabecera(tabla, Constantes.getMensaje("actaActualizacion.pdf.col.numero"), cabecera);
        agregarCabecera(tabla, Constantes.getMensaje("actaActualizacion.pdf.col.documento"), cabecera);
        agregarCabecera(tabla, Constantes.getMensaje("actaActualizacion.pdf.col.nombre"), cabecera);
        agregarCabecera(tabla, Constantes.getMensaje("actaActualizacion.pdf.col.sexo"), cabecera);
        int numero = 1;
        for (IglesiaPersonaDTO miembro : miembros) {
            agregarCelda(tabla, String.valueOf(numero++), normal, Element.ALIGN_CENTER);
            agregarCelda(tabla, miembro != null && miembro.getPersona() != null
                    ? miembro.getPersona().getDocumento() : "", normal, Element.ALIGN_LEFT);
            agregarCelda(tabla, miembro != null && miembro.getPersona() != null
                    ? miembro.getPersona().getNombres() : "", normal, Element.ALIGN_LEFT);
            agregarCelda(tabla, miembro != null && miembro.getPersona() != null
                    ? miembro.getPersona().getSexo() : "", normal, Element.ALIGN_CENTER);
        }
        documento.add(tabla);
    }

    private static void agregarFirmas(Document documento, String presidenteTribunal,
            String secretarioTribunal, String administradorIglesia, Font normal) throws Exception {
        Paragraph seccion = new Paragraph(Constantes.getMensaje("actaActualizacion.pdf.firmas"), normal);
        seccion.setSpacingBefore(18f);
        seccion.setSpacingAfter(8f);
        documento.add(seccion);

        PdfPTable firmas = new PdfPTable(3);
        firmas.setWidthPercentage(100);
        firmas.setWidths(new float[]{1f, 1f, 1f});
        firmas.addCell(celdaFirma(presidenteTribunal,
                Constantes.getMensaje("actaActualizacion.pdf.firma.presidente"), normal));
        firmas.addCell(celdaFirma(secretarioTribunal,
                Constantes.getMensaje("actaActualizacion.pdf.firma.secretario"), normal));
        firmas.addCell(celdaFirma(administradorIglesia,
                Constantes.getMensaje("actaActualizacion.pdf.firma.administrador"), normal));
        documento.add(firmas);
    }

    private static void agregarVerificacion(Document documento, String codigo, String payloadQr,
            Font pequeno) throws Exception {
        PdfPTable verificacion = new PdfPTable(new float[]{3.8f, 1f});
        verificacion.setWidthPercentage(100);
        verificacion.setSpacingBefore(14f);
        PdfPCell texto = new PdfPCell(new Phrase(
                Constantes.getMensaje("actaActualizacion.pdf.verificacion", codigo), pequeno));
        texto.setBorder(PdfPCell.NO_BORDER);
        texto.setVerticalAlignment(Element.ALIGN_MIDDLE);
        verificacion.addCell(texto);
        BarcodeQRCode qr = new BarcodeQRCode(payloadQr, 115, 115, null);
        Image imagenQr = qr.getImage();
        imagenQr.scaleToFit(76f, 76f);
        PdfPCell qrCelda = new PdfPCell(imagenQr, false);
        qrCelda.setBorder(PdfPCell.NO_BORDER);
        qrCelda.setHorizontalAlignment(Element.ALIGN_RIGHT);
        verificacion.addCell(qrCelda);
        documento.add(verificacion);
    }

    private static PdfPCell celdaFirma(String nombre, String cargo, Font fuente) {
        PdfPCell celda = new PdfPCell(new Phrase("\n\n_______________________________\n"
                + texto(nombre) + "\n" + texto(cargo), fuente));
        celda.setBorder(PdfPCell.NO_BORDER);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setVerticalAlignment(Element.ALIGN_BOTTOM);
        celda.setMinimumHeight(78f);
        celda.setPadding(5f);
        return celda;
    }

    private static void agregarDato(PdfPTable tabla, String etiqueta, String valor,
            Font fuenteEtiqueta, Font fuenteValor) {
        agregarCelda(tabla, etiqueta, fuenteEtiqueta, Element.ALIGN_LEFT);
        agregarCelda(tabla, valor, fuenteValor, Element.ALIGN_LEFT);
    }

    private static void agregarCabecera(PdfPTable tabla, String texto, Font fuente) {
        PdfPCell celda = new PdfPCell(new Phrase(texto(texto), fuente));
        celda.setBackgroundColor(new BaseColor(232, 240, 247));
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setPadding(5f);
        tabla.addCell(celda);
    }

    private static void agregarCelda(PdfPTable tabla, String texto, Font fuente, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto(texto), fuente));
        celda.setHorizontalAlignment(alineacion);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setPadding(5f);
        tabla.addCell(celda);
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
