package ec.com.antenasur.itext;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;

import ec.com.antenasur.dto.CandidatoDTO;
import ec.com.antenasur.dto.ListaDTO;
import ec.com.antenasur.dto.TribunalDTO;

/** Construye el acta de inscripcion sin depender del estado de una vista JSF. */
public final class ActaInscripcionPdf {

    private static final BaseColor AZUL = new BaseColor(24, 82, 133);

    private ActaInscripcionPdf() {
    }

    public static byte[] generar(ListaDTO lista, String procesoNombre,
            List<CandidatoDTO> candidatos, List<TribunalDTO> autoridades,
            String lugar, LocalDateTime fechaGeneracion, String usuario, String codigoDocumento) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            String tituloDocumento = "Acta de Inscripcion - " + texto(lista.getNombre());
            PdfInstitucional.Contexto contexto = PdfInstitucional.crearA4(
                    salida, codigoDocumento, tituloDocumento, fechaGeneracion);
            Document documento = contexto.documento();

            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, AZUL);
            Font subtitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.BLACK);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.BLACK);
            Font pequeno = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.DARK_GRAY);

            Paragraph institucion = new Paragraph("CONPOCIIECH - TRIBUNAL ELECTORAL", titulo);
            institucion.setAlignment(Element.ALIGN_CENTER);
            documento.add(institucion);
            Paragraph encabezado = new Paragraph("ACTA DE INSCRIPCION DE LISTA ELECTORAL", subtitulo);
            encabezado.setAlignment(Element.ALIGN_CENTER);
            encabezado.setSpacingAfter(14);
            documento.add(encabezado);

            PdfPTable datosLista = new PdfPTable(new float[] {1.2f, 3.8f});
            datosLista.setWidthPercentage(100);
            agregarDato(datosLista, "Proceso", procesoNombre, subtitulo, normal);
            agregarDato(datosLista, "Lista", lista.getNombre(), subtitulo, normal);
            agregarDato(datosLista, "Numero", lista.getNumero(), subtitulo, normal);
            agregarDato(datosLista, "Slogan", lista.getSlogan(), subtitulo, normal);
            documento.add(datosLista);

            String fecha = fechaGeneracion.format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy",
                    new Locale("es", "EC")));
            String hora = fechaGeneracion.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            Paragraph comparecencia = new Paragraph(
                    "En " + texto(lugar) + ", a " + fecha + ", a las " + hora
                    + ", se deja constancia de la inscripcion de la lista electoral y de las candidaturas detalladas a continuacion.",
                    normal);
            comparecencia.setAlignment(Element.ALIGN_JUSTIFIED);
            comparecencia.setSpacingBefore(12);
            comparecencia.setSpacingAfter(10);
            documento.add(comparecencia);

            PdfPTable tabla = new PdfPTable(new float[] {0.5f, 2.2f, 3.8f});
            tabla.setWidthPercentage(100);
            tabla.setHeaderRows(1);
            agregarCabecera(tabla, "N.", subtitulo);
            agregarCabecera(tabla, "Dignidad / cargo", subtitulo);
            agregarCabecera(tabla, "Nombres completos", subtitulo);
            int indice = 1;
            for (CandidatoDTO candidato : candidatos) {
                agregarCelda(tabla, String.valueOf(indice++), normal, Element.ALIGN_CENTER);
                agregarCelda(tabla, candidato.getCargoNombre(), normal, Element.ALIGN_LEFT);
                String nombres = candidato.getIglesiaPersona().getPersona().getNombres();
                String apellidos = candidato.getIglesiaPersona().getPersona().getApellidos();
                agregarCelda(tabla, (texto(nombres) + " " + texto(apellidos)).trim(), normal, Element.ALIGN_LEFT);
            }
            documento.add(tabla);

            Paragraph responsabilidad = new Paragraph(
                    "Las autoridades vigentes del Tribunal Electoral certifican la informacion precedente y dejan los siguientes espacios para las firmas de responsabilidad:",
                    normal);
            responsabilidad.setAlignment(Element.ALIGN_JUSTIFIED);
            responsabilidad.setSpacingBefore(16);
            responsabilidad.setSpacingAfter(8);
            documento.add(responsabilidad);

            PdfPTable firmas = new PdfPTable(2);
            firmas.setWidthPercentage(100);
            firmas.setSpacingBefore(4);
            for (TribunalDTO autoridad : autoridades) {
                String nombres = autoridad.getIglesiaPersona().getPersona().getNombres();
                String apellidos = autoridad.getIglesiaPersona().getPersona().getApellidos();
                PdfPCell firma = new PdfPCell(new Phrase(
                        "\n\n_______________________________\n"
                        + (texto(nombres) + " " + texto(apellidos)).trim() + "\n"
                        + texto(autoridad.getCargoNombre()), normal));
                firma.setHorizontalAlignment(Element.ALIGN_CENTER);
                firma.setVerticalAlignment(Element.ALIGN_BOTTOM);
                firma.setMinimumHeight(88);
                firma.setPadding(6);
                firmas.addCell(firma);
            }
            if (autoridades.size() % 2 != 0) {
                PdfPCell vacia = new PdfPCell(new Phrase(""));
                vacia.setBorder(PdfPCell.NO_BORDER);
                firmas.addCell(vacia);
            }
            documento.add(firmas);

            Paragraph auditoria = new Paragraph(
                    "Documento generado por: " + texto(usuario) + " | Fecha: " + fecha + " | Hora: " + hora,
                    pequeno);
            auditoria.setAlignment(Element.ALIGN_RIGHT);
            auditoria.setSpacingBefore(12);
            documento.add(auditoria);
            documento.close();
            return salida.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo construir el acta de inscripcion.", e);
        }
    }

    private static void agregarDato(PdfPTable tabla, String etiqueta, String valor,
            Font fuenteEtiqueta, Font fuenteValor) {
        agregarCelda(tabla, etiqueta, fuenteEtiqueta, Element.ALIGN_LEFT);
        agregarCelda(tabla, valor, fuenteValor, Element.ALIGN_LEFT);
    }

    private static void agregarCabecera(PdfPTable tabla, String valor, Font fuente) {
        PdfPCell celda = new PdfPCell(new Phrase(valor, fuente));
        celda.setBackgroundColor(new BaseColor(232, 240, 247));
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setPadding(6);
        tabla.addCell(celda);
    }

    private static void agregarCelda(PdfPTable tabla, String valor, Font fuente, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto(valor), fuente));
        celda.setHorizontalAlignment(alineacion);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setPadding(6);
        tabla.addCell(celda);
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
