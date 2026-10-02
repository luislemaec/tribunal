package ec.com.antenasur.itext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Silueta del Ecuador continental ({@code /img/ecuador-continental.svg}, Natural Earth 1:50m, dominio público)
 * convertida a polígonos para dibujarla con trazados nativos del PDF: iText 5 no interpreta SVG.
 *
 * <p>El SVG solo usa comandos absolutos {@code M}, {@code L} y {@code Z}. Se lee una vez por JVM.
 */
final class MapaEcuador {

    private static final String RUTA = "/img/ecuador-continental.svg";
    private static final Pattern VIEWBOX = Pattern.compile("viewBox=\"0 0 ([\\d.]+) ([\\d.]+)\"");
    private static final Pattern TRAZADO = Pattern.compile(" d=\"([^\"]+)\"");
    private static final Pattern COMANDO = Pattern.compile("([MLZ])\\s*([-\\d.]+)?\\s*([-\\d.]+)?");

    private static volatile MapaEcuador instancia;

    private final float ancho;
    private final float alto;
    private final List<float[]> poligonos;

    private MapaEcuador(float ancho, float alto, List<float[]> poligonos) {
        this.ancho = ancho;
        this.alto = alto;
        this.poligonos = Collections.unmodifiableList(poligonos);
    }

    static MapaEcuador instancia() {
        MapaEcuador actual = instancia;
        if (actual == null) {
            synchronized (MapaEcuador.class) {
                actual = instancia;
                if (actual == null) {
                    actual = leer();
                    instancia = actual;
                }
            }
        }
        return actual;
    }

    float ancho() {
        return ancho;
    }

    float alto() {
        return alto;
    }

    /** Polígonos como pares x, y consecutivos, en coordenadas del SVG (y crece hacia abajo). */
    List<float[]> poligonos() {
        return poligonos;
    }

    private static MapaEcuador leer() {
        String svg;
        try (InputStream entrada = MapaEcuador.class.getResourceAsStream(RUTA)) {
            if (entrada == null) {
                throw new IllegalStateException("Mapa institucional no disponible: " + RUTA);
            }
            svg = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el mapa " + RUTA, e);
        }
        Matcher caja = VIEWBOX.matcher(svg);
        Matcher trazado = TRAZADO.matcher(svg);
        if (!caja.find() || !trazado.find()) {
            throw new IllegalStateException("Mapa sin viewBox o trazado: " + RUTA);
        }
        List<float[]> poligonos = new ArrayList<>();
        List<Float> actual = new ArrayList<>();
        Matcher comando = COMANDO.matcher(trazado.group(1));
        while (comando.find()) {
            switch (comando.group(1)) {
                case "M" -> {
                    cerrar(actual, poligonos);
                    actual.add(Float.parseFloat(comando.group(2)));
                    actual.add(Float.parseFloat(comando.group(3)));
                }
                case "L" -> {
                    actual.add(Float.parseFloat(comando.group(2)));
                    actual.add(Float.parseFloat(comando.group(3)));
                }
                default -> cerrar(actual, poligonos);
            }
        }
        cerrar(actual, poligonos);
        return new MapaEcuador(Float.parseFloat(caja.group(1)), Float.parseFloat(caja.group(2)), poligonos);
    }

    private static void cerrar(List<Float> actual, List<float[]> poligonos) {
        if (actual.size() >= 6) {
            float[] puntos = new float[actual.size()];
            for (int i = 0; i < puntos.length; i++) {
                puntos[i] = actual.get(i);
            }
            poligonos.add(puntos);
        }
        actual.clear();
    }
}
