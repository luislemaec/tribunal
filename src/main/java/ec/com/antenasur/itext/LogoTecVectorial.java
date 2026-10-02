package ec.com.antenasur.itext;

import java.io.ByteArrayInputStream;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.pdf.PdfFunction;
import com.itextpdf.text.pdf.PdfShading;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import org.w3c.dom.Element;

/** Adaptador del SVG institucional suministrado: trazados absolutos M/L/Z y degradado axial.
 * No es un intérprete SVG general. Recorta únicamente las siglas TEC para el encabezado
 * del certificado, que ya escribe la institución como texto real. */
final class LogoTecVectorial {
    private static final Pattern COMANDO = Pattern.compile("([ML])\\s*(-?\\d+(?:\\.\\d+)?)[ ,]+(-?\\d+(?:\\.\\d+)?)|([Zz])");

    private LogoTecVectorial() { }

    static PdfTemplate crear(PdfWriter writer, byte[] svg) {
        return crear(writer, svg, 274, 275, 1575, 511);
    }

    /** Logo oficial completo, sin márgenes vacíos del lienzo SVG, para marca de agua. */
    static PdfTemplate crearCompleto(PdfWriter writer, byte[] svg) {
        return crear(writer, svg, 237, 275, 1612, 806);
    }

    private static PdfTemplate crear(PdfWriter writer, byte[] svg,
            float izquierda, float arriba, float ancho, float alto) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var documento = factory.newDocumentBuilder().parse(new ByteArrayInputStream(svg));
            var gradientes = documento.getElementsByTagName("linearGradient");
            if (gradientes.getLength() != 1) throw new IllegalArgumentException("Degradado TEC requerido");
            var gradiente = (Element) gradientes.item(0);
            if (!"tecBlue".equals(gradiente.getAttribute("id"))
                    || !"userSpaceOnUse".equals(gradiente.getAttribute("gradientUnits"))
                    || !gradiente.getAttribute("y1").equals(gradiente.getAttribute("y2")))
                throw new IllegalArgumentException("Degradado TEC no compatible");
            var stops = gradiente.getElementsByTagName("stop");
            int tramos = stops.getLength() - 1;
            if (tramos < 1) throw new IllegalArgumentException("Paradas TEC requeridas");
            var funciones = new PdfFunction[tramos];
            float[] limites = new float[tramos - 1], codificacion = new float[tramos * 2];
            for (int i = 0; i < tramos; i++) {
                funciones[i] = PdfFunction.type2(writer, new float[]{0, 1}, null,
                        rgb((Element) stops.item(i)), rgb((Element) stops.item(i + 1)), 1);
                if (i > 0) limites[i - 1] = Float.parseFloat(((Element) stops.item(i)).getAttribute("offset"));
                codificacion[i * 2 + 1] = 1;
            }
            var funcion = PdfFunction.type3(writer, new float[]{0, 1}, null, funciones, limites, codificacion);
            var sombreado = PdfShading.type2(writer, TipografiaPdf.AZUL,
                    new float[]{Float.parseFloat(gradiente.getAttribute("x1")) - izquierda, 0,
                            Float.parseFloat(gradiente.getAttribute("x2")) - izquierda, 0},
                    new float[]{0, 1}, funcion, new boolean[]{true, true});
            PdfTemplate plantilla = writer.getDirectContent().createTemplate(ancho, alto);
            plantilla.rectangle(0, 0, ancho, alto);
            plantilla.clip();
            plantilla.newPath();
            var paths = documento.getElementsByTagName("path");
            if (paths.getLength() != 3) throw new IllegalArgumentException("Trazados TEC no compatibles");
            for (int i = 0; i < paths.getLength(); i++) {
                Element path = (Element) paths.item(i);
                if (!"evenodd".equals(path.getAttribute("fill-rule")))
                    throw new IllegalArgumentException("Regla de relleno TEC no compatible");
                plantilla.saveState();
                String datos = path.getAttribute("d");
                var matcher = COMANDO.matcher(datos);
                int fin = 0;
                while (matcher.find()) {
                    if (!datos.substring(fin, matcher.start()).isBlank())
                        throw new IllegalArgumentException("Comando SVG TEC no compatible");
                    if (matcher.group(4) != null) plantilla.closePath();
                    else {
                        float x = Float.parseFloat(matcher.group(2)) - izquierda;
                        float y = alto - (Float.parseFloat(matcher.group(3)) - arriba);
                        if ("M".equals(matcher.group(1))) plantilla.moveTo(x, y);
                        else plantilla.lineTo(x, y);
                    }
                    fin = matcher.end();
                }
                if (!datos.substring(fin).isBlank()) throw new IllegalArgumentException("Trazado SVG TEC incompleto");
                String fill = path.getAttribute("fill");
                if ("url(#tecBlue)".equals(fill)) {
                    plantilla.eoClip();
                    plantilla.newPath();
                    plantilla.paintShading(sombreado);
                } else {
                    int color = Integer.parseInt("#fff".equals(fill) ? "ffffff" : fill.substring(1), 16);
                    // BaseColor(int) interpreta ARGB: un RGB de seis dígitos daría alfa cero.
                    plantilla.setColorFill(new BaseColor((color >> 16) & 255,
                            (color >> 8) & 255, color & 255));
                    plantilla.eoFill();
                }
                plantilla.restoreState();
            }
            return plantilla;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo componer el logo TEC vectorial", e);
        }
    }

    private static float[] rgb(Element stop) {
        int color = Integer.parseInt(stop.getAttribute("stop-color").substring(1), 16);
        return new float[]{((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f, (color & 255) / 255f};
    }
}
