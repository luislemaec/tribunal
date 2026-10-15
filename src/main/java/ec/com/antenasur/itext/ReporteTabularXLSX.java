package ec.com.antenasur.itext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Base de los Excel tabulares de Rep. Registros (una pestaña por archivo, solo datos): libro
 * sin estado estático con el encabezado institucional de {@link ReporteXLSX} en cada hoja,
 * estilos comunes y una distribución centralizada de anchos y alturas.
 *
 * <p>Distribución ({@link Hoja#cerrar()}): cada columna toma el ancho de su valor más largo en
 * una línea, entre un mínimo y un máximo; si algún texto necesitaría más de dos líneas, la
 * columna se ensancha (hasta un 40 % sobre su máximo) hasta que todos quepan en dos. Solo las
 * celdas que no caben en una línea se ajustan (wrapText) y su fila toma dos líneas. Nada se
 * recorta: un valor excepcional que aun así necesite más líneas hace crecer solo su fila.
 */
public final class ReporteTabularXLSX implements AutoCloseable {

    /** Fila de cabecera de las tablas: la misma que fija el panel de {@link ReporteXLSX}. */
    public static final int FILA_CABECERA = 5;

    /** Altura de una línea de Calibri 11 (la fuente por defecto del libro), en puntos. */
    private static final float ALTO_LINEA = 15f;

    /** Cabeceras: siempre dos líneas, la misma altura en todas las tablas. */
    private static final float ALTO_CABECERA = 2 * ALTO_LINEA + 2f;

    private final XSSFWorkbook libro = new XSSFWorkbook();
    private final Estilos estilos = new Estilos(libro);
    private final String titulo;
    private final byte[] logo;
    private final String[] contexto;

    /**
     * @param titulo   nombre del reporte, bajo el título institucional de cada hoja
     * @param logo     logo institucional (PNG); null para omitirlo
     * @param contexto líneas bajo el título (filtros aplicados, fecha y usuario); máximo dos
     */
    public ReporteTabularXLSX(String titulo, byte[] logo, String... contexto) {
        this.titulo = titulo;
        this.logo = logo;
        this.contexto = contexto == null ? new String[0] : contexto;
    }

    /** Hoja nueva con el encabezado institucional y las líneas de contexto. */
    public Hoja hoja(String nombre) {
        XSSFSheet sheet = libro.createSheet(nombre);
        ReporteXLSX.crearEncabezadoInstitucional(libro, sheet, titulo,
                logo == null ? InputStream.nullInputStream() : new java.io.ByteArrayInputStream(logo));
        Hoja hoja = new Hoja(sheet, estilos, titulo);
        for (int i = 0; i < contexto.length && i < 2; i++) {
            // Una línea cada una: se extienden sobre las celdas vacías de la derecha.
            hoja.nota(2 + i, 2, contexto[i]);
        }
        return hoja;
    }

    public byte[] bytes() throws IOException {
        libro.setActiveSheet(0);
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            libro.write(salida);
            return salida.toByteArray();
        }
    }

    @Override
    public void close() throws IOException {
        libro.close();
    }

    /**
     * Una hoja: crea las celdas con su estilo según el tipo de dato, mide el texto y en
     * {@link #cerrar()} fija anchos y alturas.
     */
    public static final class Hoja {
        /** Ancho por defecto de una columna sin límites propios (caracteres). */
        private static final int MINIMO = 10;
        private static final int MAXIMO = 30;

        private final XSSFSheet sheet;
        private final Estilos e;
        private final String titulo;
        private final Map<Integer, int[]> limites = new HashMap<>();
        /** Ancho requerido por columna, en caracteres de una línea. */
        private final Map<Integer, Double> requerido = new HashMap<>();
        /** Celdas de texto que podrían necesitar ajuste: {celda, estilo base, factor}. */
        private final List<Object[]> textos = new ArrayList<>();

        private Hoja(XSSFSheet sheet, Estilos e, String titulo) {
            this.sheet = sheet;
            this.e = e;
            this.titulo = titulo;
        }

        public XSSFSheet sheet() {
            return sheet;
        }

        /** Mínimo y máximo de ancho de la columna, en caracteres. */
        public Hoja limites(int columna, int minimo, int maximo) {
            limites.put(columna, new int[] { minimo, maximo });
            return this;
        }

        public Row fila(int indice) {
            Row r = sheet.getRow(indice);
            return r != null ? r : sheet.createRow(indice);
        }

        /** Títulos de una tabla: dos líneas, la misma altura en todas. */
        public void cabecera(int fila, String... titulos) {
            Row r = fila(fila);
            r.setHeightInPoints(ALTO_CABECERA);
            for (int i = 0; i < titulos.length; i++) {
                crear(r, i, titulos[i], e.cabecera);
                double largo = medir(titulos[i]) * 1.08;
                double palabra = 0;
                for (String p : titulos[i].split(" ")) {
                    palabra = Math.max(palabra, medir(p) * 1.08);
                }
                exigir(i, Math.max(Math.ceil(largo / 2) + 2, palabra + 2));
            }
        }

        public void texto(Row r, int columna, String valor) {
            medible(crear(r, columna, valor, e.texto), valor, e.texto, 1.0);
        }

        public void negrita(Row r, int columna, String valor) {
            medible(crear(r, columna, valor, e.negrita), valor, e.negrita, 1.08);
        }

        /** Texto secundario (gris), p. ej. la explicación de un indicador. */
        public void detalle(Row r, int columna, String valor) {
            medible(crear(r, columna, valor, e.detalle), valor, e.detalle, 1.0);
        }

        /** Estados, «Sí/No» y códigos cortos. */
        public void centrado(Row r, int columna, String valor) {
            medible(crear(r, columna, valor, e.centrado), valor, e.centrado, 1.0);
        }

        public void numero(Row r, int columna, long valor) {
            Cell c = r.createCell(columna);
            c.setCellValue(valor);
            c.setCellStyle(e.numero);
            exigir(columna, String.format("%,d", valor).length() + 3);
        }

        public void porcentaje(Row r, int columna, long valor) {
            Cell c = r.createCell(columna);
            c.setCellValue(valor);
            c.setCellStyle(e.porcentaje);
            exigir(columna, 7);
        }

        public void fecha(Row r, int columna, Date valor) {
            if (valor == null) {
                centrado(r, columna, "");
                return;
            }
            Cell c = r.createCell(columna);
            c.setCellValue(valor);
            c.setCellStyle(e.fecha);
            exigir(columna, 17);
        }

        /** Título de una sección de la hoja. */
        public void seccion(int fila, String valor) {
            crear(fila(fila), 0, valor, e.seccion);
        }

        /** Nota de una línea (sin medir): se extiende sobre las celdas vacías de la derecha. */
        public void nota(int fila, String valor) {
            nota(fila, 0, valor);
        }

        public void nota(int fila, int columna, String valor) {
            crear(fila(fila), columna, valor, e.nota);
        }

        /** Autofiltro sobre una tabla con datos (cabecera incluida). */
        public void autofiltro(int filaCabecera, int ultimaFila, int columnas) {
            if (ultimaFila > filaCabecera) {
                sheet.setAutoFilter(new CellRangeAddress(filaCabecera, ultimaFila, 0, columnas - 1));
            }
        }

        /**
         * Fija anchos y alturas. Se llama una vez, con todas las celdas ya escritas.
         *
         * <ol>
         * <li>Ancho base: el valor más largo en una línea, entre el mínimo y el máximo.</li>
         * <li>Si con ese ancho algún texto de la columna necesitaría más de dos líneas, la
         * columna se ensancha (hasta un 40 % sobre su máximo) hasta que todos quepan en dos.</li>
         * <li>Solo las celdas que no caben en una línea se ajustan; la fila toma sus líneas.</li>
         * </ol>
         */
        public void cerrar() {
            Map<Integer, List<String>> porColumna = new HashMap<>();
            for (Object[] t : textos) {
                Cell celda = (Cell) t[0];
                porColumna.computeIfAbsent(celda.getColumnIndex(), k -> new ArrayList<>()).add(celda.getStringCellValue());
            }
            Map<Integer, Integer> anchos = new HashMap<>();
            for (Map.Entry<Integer, Double> col : requerido.entrySet()) {
                int[] lim = limites.getOrDefault(col.getKey(), new int[] { MINIMO, MAXIMO });
                int ancho = (int) Math.ceil(Math.max(lim[0], Math.min(lim[1], col.getValue())));
                int extendido = (int) Math.ceil(lim[1] * 1.4);
                List<String> valores = porColumna.getOrDefault(col.getKey(), List.of());
                while (ancho < extendido && maximoLineas(valores, ancho - 1) > 2) {
                    ancho++;
                }
                anchos.put(col.getKey(), ancho);
                sheet.setColumnWidth(col.getKey(), Math.min(255, ancho) * 256);
            }
            Map<Integer, Integer> lineas = new HashMap<>();
            for (Object[] t : textos) {
                Cell celda = (Cell) t[0];
                int disponible = anchos.getOrDefault(celda.getColumnIndex(), MAXIMO) - 1;
                int n = lineas(celda.getStringCellValue(), disponible, (Double) t[2]);
                if (n > 1) {
                    celda.setCellStyle(e.ajustado((XSSFCellStyle) t[1]));
                    lineas.merge(celda.getRowIndex(), n, Math::max);
                }
            }
            for (Map.Entry<Integer, Integer> l : lineas.entrySet()) {
                fila(l.getKey()).setHeightInPoints(l.getValue() * ALTO_LINEA + 2f);
            }
            asegurarTitulo(anchos);
        }

        private int maximoLineas(List<String> valores, double disponible) {
            int maximo = 1;
            for (String v : valores) {
                maximo = Math.max(maximo, lineas(v, disponible, 1.08));
                if (maximo > 2) {
                    break;
                }
            }
            return maximo;
        }

        /**
         * Líneas que ocupa el texto al ajustarse en {@code disponible} caracteres, partiendo por
         * palabras como Excel (una palabra más ancha que la celda se corta en varias líneas).
         */
        private static int lineas(String texto, double disponible, double factor) {
            if (texto == null || texto.isBlank() || disponible <= 0) {
                return 1;
            }
            int lineas = 1;
            double actual = 0;
            for (String palabra : texto.trim().split("\\s+")) {
                double ancho = medir(palabra) * factor;
                double conEspacio = actual == 0 ? ancho : actual + 0.5 + ancho;
                if (conEspacio <= disponible) {
                    actual = conEspacio;
                } else if (ancho <= disponible) {
                    lineas++;
                    actual = ancho;
                } else {
                    int partes = (int) Math.ceil(ancho / disponible);
                    lineas += actual == 0 ? partes - 1 : partes;
                    actual = ancho - (partes - 1) * disponible;
                }
            }
            return lineas;
        }

        /**
         * El título del reporte va combinado en C:F (encabezado de {@link ReporteXLSX}). Si esas
         * columnas son angostas en esta hoja, F se ensancha para que quepa en una línea en
         * lugar de recortarse.
         */
        private void asegurarTitulo(Map<Integer, Integer> anchos) {
            double necesario = medir(titulo) * 14 / 11 * 1.1 + 4;
            double actual = 0;
            for (int c = 2; c <= 5; c++) {
                actual += anchos.containsKey(c) ? anchos.get(c) : sheet.getColumnWidth(c) / 256.0;
            }
            if (actual < necesario) {
                double f = anchos.containsKey(5) ? anchos.get(5) : sheet.getColumnWidth(5) / 256.0;
                sheet.setColumnWidth(5, (int) Math.ceil(f + necesario - actual) * 256);
            }
        }

        private Cell crear(Row r, int columna, String valor, XSSFCellStyle estilo) {
            Cell c = r.createCell(columna);
            c.setCellValue(valor == null ? "" : valor);
            c.setCellStyle(estilo);
            return c;
        }

        private void medible(Cell celda, String valor, XSSFCellStyle estilo, double factor) {
            exigir(celda.getColumnIndex(), medir(valor) * factor + 1);
            textos.add(new Object[] { celda, estilo, factor });
        }

        private void exigir(int columna, double caracteres) {
            requerido.merge(columna, caracteres, Math::max);
        }

        /**
         * Ancho aproximado en caracteres de Calibri 11 (la unidad de ancho de Excel es el
         * dígito «0»): mayúsculas más anchas, minúsculas y espacios más angostos.
         */
        private static double medir(String texto) {
            if (texto == null) {
                return 0;
            }
            double ancho = 0;
            for (int i = 0; i < texto.length(); i++) {
                char c = texto.charAt(i);
                if (c == ' ') {
                    ancho += 0.5;
                } else if (Character.isUpperCase(c)) {
                    ancho += c == 'M' || c == 'W' ? 1.45 : 1.15;
                } else if (Character.isDigit(c)) {
                    ancho += 1.0;
                } else if ("iljtf.,;:'|!".indexOf(c) >= 0) {
                    ancho += 0.5;
                } else if (c == 'm' || c == 'w') {
                    ancho += 1.35;
                } else {
                    ancho += 0.9;
                }
            }
            return ancho;
        }
    }

    /**
     * Estilos del libro, definidos una sola vez: cabecera azul con texto blanco (como los
     * demás Excel de TEC), contenido centrado en vertical, texto a la izquierda, números a la
     * derecha y estados y fechas al centro. Sin wrapText: solo se activa, por celda, en las
     * que no caben en una línea ({@link #ajustado}).
     */
    private static final class Estilos {
        final XSSFWorkbook libro;
        final XSSFCellStyle cabecera;
        final XSSFCellStyle texto;
        final XSSFCellStyle negrita;
        final XSSFCellStyle detalle;
        final XSSFCellStyle centrado;
        final XSSFCellStyle numero;
        final XSSFCellStyle porcentaje;
        final XSSFCellStyle fecha;
        final XSSFCellStyle seccion;
        final XSSFCellStyle nota;
        private final Map<XSSFCellStyle, XSSFCellStyle> ajustados = new HashMap<>();

        Estilos(XSSFWorkbook libro) {
            this.libro = libro;
            XSSFFont blanca = libro.createFont();
            blanca.setBold(true);
            blanca.setColor(IndexedColors.WHITE.index);
            cabecera = celda(HorizontalAlignment.CENTER);
            cabecera.setFont(blanca);
            cabecera.setFillForegroundColor(IndexedColors.DARK_BLUE.index);
            cabecera.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cabecera.setWrapText(true);

            texto = celda(HorizontalAlignment.LEFT);
            negrita = celda(HorizontalAlignment.LEFT);
            XSSFFont fuerte = libro.createFont();
            fuerte.setBold(true);
            negrita.setFont(fuerte);
            XSSFFont gris = libro.createFont();
            gris.setColor(IndexedColors.GREY_50_PERCENT.index);
            detalle = celda(HorizontalAlignment.LEFT);
            detalle.setFont(gris);
            centrado = celda(HorizontalAlignment.CENTER);

            numero = celda(HorizontalAlignment.RIGHT);
            numero.setDataFormat(libro.createDataFormat().getFormat("#,##0"));
            porcentaje = celda(HorizontalAlignment.RIGHT);
            porcentaje.setDataFormat(libro.createDataFormat().getFormat("0\"%\""));
            fecha = celda(HorizontalAlignment.CENTER);
            fecha.setDataFormat(libro.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));

            XSSFFont azul = libro.createFont();
            azul.setBold(true);
            azul.setFontHeightInPoints((short) 12);
            azul.setColor(IndexedColors.DARK_BLUE.index);
            seccion = libro.createCellStyle();
            seccion.setFont(azul);
            seccion.setVerticalAlignment(VerticalAlignment.CENTER);

            nota = libro.createCellStyle();
            nota.setFont(gris);
            nota.setVerticalAlignment(VerticalAlignment.CENTER);
        }

        /** El mismo estilo con wrapText, creado una sola vez por estilo base. */
        XSSFCellStyle ajustado(XSSFCellStyle base) {
            return ajustados.computeIfAbsent(base, b -> {
                XSSFCellStyle copia = libro.createCellStyle();
                copia.cloneStyleFrom(b);
                copia.setWrapText(true);
                return copia;
            });
        }

        private XSSFCellStyle celda(HorizontalAlignment horizontal) {
            XSSFCellStyle estilo = libro.createCellStyle();
            estilo.setAlignment(horizontal);
            estilo.setVerticalAlignment(VerticalAlignment.CENTER);
            estilo.setBorderBottom(BorderStyle.THIN);
            estilo.setBorderTop(BorderStyle.THIN);
            estilo.setBorderLeft(BorderStyle.THIN);
            estilo.setBorderRight(BorderStyle.THIN);
            estilo.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.index);
            estilo.setTopBorderColor(IndexedColors.GREY_25_PERCENT.index);
            estilo.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.index);
            estilo.setRightBorderColor(IndexedColors.GREY_25_PERCENT.index);
            return estilo;
        }
    }
}
