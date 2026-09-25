package ec.com.antenasur.itext;

import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import ec.com.antenasur.dto.CertificadoVotacionDTO;
import ec.com.antenasur.dto.MesaDTO;
import ec.com.antenasur.dto.ProcesoElectoralDTO;
import ec.com.antenasur.dto.RecintoDTO;
import ec.com.antenasur.dto.ReporteMesaDTO;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.regex.Pattern;

/** Ejecutable sin contenedor; datos ficticios y recursos del WAR. */
public class CertificadosVotacionPdfCheck {
    public static void main(String[] args) throws Exception {
        ReporteMesaDTO reporte = new ReporteMesaDTO();
        ProcesoElectoralDTO proceso = new ProcesoElectoralDTO();
        proceso.setId(7);
        proceso.setNombre("Elecciones generales de prueba 2027");
        reporte.setProceso(proceso);
        RecintoDTO recinto = new RecintoDTO();
        recinto.setNombre("Unidad Educativa de Prueba");
        recinto.setProvinciaNombre("Chimborazo");
        recinto.setCantonNombre("Riobamba");
        recinto.setUbicacionNombre("Parroquia electoral de prueba");
        reporte.setRecinto(recinto);
        MesaDTO mesa = new MesaDTO();
        mesa.setId(12);
        mesa.setNombre("12");
        reporte.setMesa(mesa);
        var recursos = new CertificadosVotacionPDF.Recursos(
                Files.readAllBytes(Path.of("src/main/resources/img/cert-logo.png")),
                Files.readAllBytes(Path.of("src/main/webapp/resources/fonts/Montserrat-Regular.ttf")),
                Files.readAllBytes(Path.of("src/main/webapp/resources/fonts/Montserrat-Bold.ttf")));
        var fecha = new SimpleDateFormat("yyyy-MM-dd").parse("2027-02-21");
        List<CertificadoVotacionDTO> personas = new ArrayList<>();
        for (int i = 1; i <= 21; i++) {
            personas.add(new CertificadoVotacionDTO(i,
                    "MARIA JOSE ALEJANDRA", "APELLIDO PRIMERO APELLIDO SEGUNDO " + i,
                    "SN-" + i, "Iglesia Evangelica de la Comunidad de Prueba",
                    "Comunidad de Prueba", "Parroquia de Prueba", "Canton de Prueba", "Chimborazo"));
        }
        for (int cantidad : new int[]{1, 10, 11, 21}) {
            byte[] pdf = CertificadosVotacionPDF.generar(reporte, personas.subList(0, cantidad), fecha, recursos);
            if (args.length > 0) Files.write(Path.of(args[0]), pdf);
            PdfReader lector = new PdfReader(pdf);
            comprobar(lector.getNumberOfPages() == 2 * ((cantidad + 9) / 10), "Pares frente/reverso");
            var preferencias = lector.getCatalog().getAsDict(PdfName.VIEWERPREFERENCES);
            comprobar(PdfName.DUPLEXFLIPLONGEDGE.equals(preferencias.getAsName(PdfName.DUPLEX)), "Duplex borde largo");
            comprobar(PdfName.NONE.equals(preferencias.getAsName(PdfName.PRINTSCALING)), "Sin escalado");
            var codigos = new HashSet<String>();
            for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
                boolean frente = pagina % 2 == 1;
                int inicio = (pagina - 1) / 2 * 10;
                int esperados = Math.min(10, cantidad - inicio);
                comprobar(Math.abs(lector.getPageSize(pagina).getWidth() - 595) < 1, "Ancho A4");
                comprobar(Math.abs(lector.getPageSize(pagina).getHeight() - 842) < 1, "Alto A4");
                String texto = PdfTextExtractor.getTextFromPage(lector, pagina);
                if (frente) {
                    comprobar(texto.split("CERTIFICADO DE VOTACI", -1).length - 1 == esperados, "Titulos");
                    comprobar(texto.split("21/02/2027", -1).length - 1 == esperados, "Fecha cronograma");
                    comprobar(!texto.contains("PRESIDENTA/E") && !texto.contains("Este documento"), "Frente sin texto del reverso");
                    for (int fila = 1; fila <= esperados; fila++) {
                        int persona = inicio + fila;
                        comprobar(Pattern.compile("C\\u00c9DULA:\\s+SN-" + persona + "(?:\\s|$)")
                                .matcher(texto).find(), "Persona completa en la pagina correspondiente");
                        String codigo = CertificadosVotacionPDF.codigo(7, 12, persona);
                        comprobar(codigos.add(codigo), "Codigo unico por certificado");
                    }
                    var fuentes = lector.getPageN(pagina).getAsDict(PdfName.RESOURCES).getAsDict(PdfName.FONT);
                    String nombres = fuentes.getKeys().stream().map(k -> fuentes.getAsDict(k)
                            .getAsName(PdfName.BASEFONT).toString()).reduce("", String::concat);
                    comprobar(nombres.contains("Montserrat-Regular") && nombres.contains("Montserrat-Bold"), "Fuentes institucionales");
                } else {
                    comprobar(texto.split("PRESIDENTA/E", -1).length - 1 == esperados, "Firmas completas");
                    comprobar(texto.split("Este documento acredita", -1).length - 1 == esperados, "Texto reverso");
                    comprobar(!texto.contains("CERTIFICADO DE VOTACI") && !texto.contains("SN-"), "Reverso sin datos frontales");
                    comprobar(texto.contains("Riobamba"), "Ubicacion del recinto, no de la iglesia");
                    comprobar(texto.contains("Iglesia Evangelica"), "Conserva informacion de iglesia");
                    for (int fila = 1; fila <= esperados; fila++)
                        comprobar(texto.contains(CertificadosVotacionPDF.codigo(7, 12, inicio + fila)), "Codigo visible en reverso");
                }
                String contenido = new String(lector.getPageContent(pagina), StandardCharsets.ISO_8859_1);
                var rectangulos = Pattern.compile("([\\d.]+) ([\\d.]+) ([\\d.]+) ([\\d.]+) re").matcher(contenido);
                int recortes = 0;
                while (rectangulos.find()) {
                    float ancho = Float.parseFloat(rectangulos.group(3));
                    if (Math.abs(ancho - CertificadosVotacionPDF.ANCHO) > 0.02) continue;
                    comprobar(Math.abs(Float.parseFloat(rectangulos.group(4)) - CertificadosVotacionPDF.ALTO) < 0.02, "Alto certificado");
                    comprobar(Math.abs(Float.parseFloat(rectangulos.group(1))
                            - CertificadosVotacionPDF.x(recortes, !frente)) < 0.02, "Posicion horizontal duplex");
                    comprobar(Math.abs(Float.parseFloat(rectangulos.group(2))
                            - CertificadosVotacionPDF.y(recortes)) < 0.02, "Posicion vertical duplex");
                    recortes++;
                }
                comprobar(recortes == esperados, "Guia de corte por certificado");
            }
            lector.close();
            if (cantidad == 21 && args.length > 0) Files.write(Path.of(args[0]), pdf);
        }
        String codigo = CertificadosVotacionPDF.codigo(7, 12, 1);
        comprobar(codigo.equals(CertificadosVotacionPDF.codigo(7, 12, 1)), "Codigo estable");
        comprobar(!codigo.equals(CertificadosVotacionPDF.codigo(8, 12, 1)), "Codigo por proceso");
        comprobar(!codigo.equals(CertificadosVotacionPDF.codigo(7, 13, 1)), "Codigo por mesa");
        try {
            CertificadosVotacionPDF.generar(reporte, List.of(), fecha, recursos);
            throw new AssertionError("No debe generar padron vacio");
        } catch (IllegalArgumentException esperado) { }
        if (args.length > 1) verificarBarrasRasterizadas(Path.of(args[1]));
        System.out.println("OK: 1, 10, 11 y 21 personas; A4; medidas; duplex; fuentes; codigos; caras separadas.");
    }

    /** Compara cada barra y espacio del PNG a 300 dpi, incluido checksum y stop. */
    private static void verificarBarrasRasterizadas(Path imagen) throws Exception {
        var png = javax.imageio.ImageIO.read(imagen.toFile());
        float escala = 300f / 72;
        for (int i = 0; i < 10; i++) {
            int y = Math.round((842 - CertificadosVotacionPDF.y(i) - 18) * escala);
            int izquierda = Math.round((CertificadosVotacionPDF.x(i, true) + 8) * escala);
            int derecha = Math.round((CertificadosVotacionPDF.x(i, true)
                    + CertificadosVotacionPDF.ANCHO - 8) * escala);
            List<Integer> anchos = new ArrayList<>();
            boolean negroAnterior = false;
            int longitud = 0;
            for (int x = izquierda; x < derecha; x++) {
                boolean negro = (png.getRGB(x, y) & 255) < 128;
                if (negro != negroAnterior) {
                    if (!anchos.isEmpty() || negroAnterior) anchos.add(longitud);
                    longitud = 0;
                    negroAnterior = negro;
                }
                longitud++;
            }
            String codigo = CertificadosVotacionPDF.codigo(7, 12, i + 1);
            byte[] barras = com.itextpdf.text.pdf.Barcode128.getBarsCode128Raw(
                    com.itextpdf.text.pdf.Barcode128.getRawText(codigo, false));
            int totalBarras = 0;
            int modulos = 0;
            for (byte barra : barras) if (barra > 0) { totalBarras++; modulos += barra; }
            comprobar(anchos.size() == totalBarras, "Barras completas a 300 dpi");
            float modulo = anchos.stream().mapToInt(Integer::intValue).sum() / (float) modulos;
            for (int j = 0; j < totalBarras; j++) {
                comprobar(Math.abs(anchos.get(j) - barras[j] * modulo) < 1.2,
                        "Ancho de modulo legible a 300 dpi");
            }
        }
        System.out.println("OK: 10 codigos de barras rasterizados a 300 dpi, sin perdida de modulos.");
    }

    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }
}
