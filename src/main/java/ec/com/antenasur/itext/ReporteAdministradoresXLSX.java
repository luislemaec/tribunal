package ec.com.antenasur.itext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Predicate;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.poi.xddf.usermodel.XDDFShapeProperties;
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties;
import org.apache.poi.xddf.usermodel.chart.AxisCrosses;
import org.apache.poi.xddf.usermodel.chart.AxisOrientation;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.BarDirection;
import org.apache.poi.xddf.usermodel.chart.BarGrouping;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import ec.com.antenasur.dto.AdministradorIglesiaDTO;
import ec.com.antenasur.dto.ConteoAdministradoresDTO;
import ec.com.antenasur.util.Constantes;

/**
 * Excel de la pestaña Administradores de Rep. Registros. Recibe los mismos objetos que ya
 * calculó {@code ReporteAdministradoresController} con los filtros vigentes (cantón y días
 * de inactividad): no consulta la base ni recalcula indicadores. Sin estado estático (a
 * diferencia del modo clásico de {@link ReporteXLSX}); reutiliza su encabezado institucional.
 * Los gráficos son nativos de Excel (XDDF) y replican los de la web: barras horizontales,
 * apiladas cuando hay dos series, y las agrupadas de porcentajes en escala 0-100.
 */
public final class ReporteAdministradoresXLSX {

    /** Fila de cabecera de las tablas: la misma que fija el panel de {@link ReporteXLSX}. */
    private static final int FILA_CABECERA = 5;

    /** Colores de la web (graficos-tema.js): --primary-700 y --tec-warn. */
    private static final byte[] AZUL = { (byte) 0x02, (byte) 0x38, (byte) 0x75 };
    private static final byte[] AMARILLO = { (byte) 0xFB, (byte) 0xC0, (byte) 0x2D };

    /**
     * Datos de la pestaña tal como se muestran.
     *
     * @param ambito           cantón elegido o «Todos los cantones»
     * @param columnaCategoria «Cantón» o «Parroquia», igual que la tabla de la web
     * @param avance           {% habilitados, % revisados} con y sin administrador
     * @param iglesias         todas las iglesias del filtro, en el orden de la lista
     */
    public record Datos(String ambito, int diasInactividad, String columnaCategoria,
            ConteoAdministradoresDTO totales, List<ConteoAdministradoresDTO> categorias, int[][] avance,
            List<AdministradorIglesiaDTO> iglesias, String generado, String responsable) {
    }

    private ReporteAdministradoresXLSX() {
    }

    public static byte[] generar(Datos datos, byte[] logo) throws IOException {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Estilos estilos = new Estilos(libro);
            resumen(libro, estilos, datos, logo);
            avance(libro, estilos, datos, logo);
            List<AdministradorIglesiaDTO> todas = datos.iglesias();
            iglesias(libro, estilos, datos, logo, "hoja.iglesias", todas, i -> true);
            iglesias(libro, estilos, datos, logo, "hoja.sinAdministrador", todas, i -> !i.isTieneAdministrador());
            iglesias(libro, estilos, datos, logo, "hoja.pendientes", todas,
                    i -> i.isTieneAdministrador() && !i.isAdministradorActivado());
            iglesias(libro, estilos, datos, logo, "hoja.activados", todas, AdministradorIglesiaDTO::isAdministradorActivado);
            iglesias(libro, estilos, datos, logo, "hoja.sinAcceso", todas, AdministradorIglesiaDTO::isSinAccesoReciente);
            libro.setActiveSheet(0);
            libro.write(salida);
            return salida.toByteArray();
        }
    }

    // ------------------------------------------------------------------ Hojas

    private static void resumen(XSSFWorkbook libro, Estilos e, Datos d, byte[] logo) {
        XSSFSheet hoja = nuevaHoja(libro, e, d, logo, "hoja.resumen");
        anchos(hoja, 9000, 4200, 4200, 4200, 4200, 4200, 4200, 4200);
        ConteoAdministradoresDTO t = d.totales();

        int fila = FILA_CABECERA;
        cabecera(hoja, e, fila++, m("col.indicador"), m("col.valor"), m("col.detalle"));
        fila = indicador(hoja, e, fila, txt("reporteIglesias.kpi.iglesias"), t.getIglesias(), d.ambito());
        fila = indicador(hoja, e, fila, txt("reporteIglesias.serie.conAdministrador"), t.getConAdministrador(),
                m("detalle.porcentaje", t.getPorcentajeConAdministrador(), t.getIglesias()));
        fila = indicador(hoja, e, fila, txt("reporteIglesias.serie.sinAdministrador"), t.getSinAdministrador(), d.ambito());
        fila = indicador(hoja, e, fila, txt("reporteAdministradores.serie.activados"), t.getActivados(), "");
        fila = indicador(hoja, e, fila, txt("reporteAdministradores.serie.pendientes"), t.getPendientesActivar(),
                txt("reporteAdministradores.kpi.pendientes.ayuda"));
        fila = indicador(hoja, e, fila, txt("reporteAdministradores.kpi.sinAcceso"), t.getSinAccesoReciente(),
                txt("reporteAdministradores.kpi.sinAcceso.ayuda") + " " + d.diasInactividad() + " "
                        + txt("reporteAdministradores.dias"));

        // Tabla por cantón o parroquia (la de la web) y gráfico de iglesias con y sin administrador.
        fila += 1;
        titulo(hoja, e, fila++, txt("reporteIglesias.grafico.administrador"));
        int cabeceraCategorias = fila;
        cabecera(hoja, e, fila++, d.columnaCategoria(), txt("reporteIglesias.kpi.iglesias"),
                txt("reporteIglesias.serie.conAdministrador"), m("col.porcentajeCon"),
                txt("reporteIglesias.serie.sinAdministrador"), txt("reporteAdministradores.serie.activados"),
                txt("reporteAdministradores.serie.pendientes"), txt("reporteAdministradores.kpi.sinAcceso"));
        for (ConteoAdministradoresDTO c : d.categorias()) {
            Row r = hoja.createRow(fila++);
            texto(r, 0, c.getEtiqueta(), e.celda);
            numero(r, 1, c.getIglesias(), e.numero);
            numero(r, 2, c.getConAdministrador(), e.numero);
            numero(r, 3, c.getPorcentajeConAdministrador(), e.porcentaje);
            numero(r, 4, c.getSinAdministrador(), e.numero);
            numero(r, 5, c.getActivados(), e.numero);
            numero(r, 6, c.getPendientesActivar(), e.numero);
            numero(r, 7, c.getSinAccesoReciente(), e.numero);
        }
        if (d.categorias().isEmpty()) {
            sinDatos(hoja, e, fila++, txt("reporteIglesias.sinDatos"));
        }
        int primeraCategoria = cabeceraCategorias + 1;
        int ultimaCategoria = fila - 1;

        // Activación: solo cantones o parroquias con administrador, como el gráfico de la web.
        List<ConteoAdministradoresDTO> conAdmin = d.categorias().stream().filter(c -> c.getConAdministrador() > 0)
                .toList();
        fila += 1;
        titulo(hoja, e, fila++, txt("reporteAdministradores.grafico.activacion"));
        int cabeceraActivacion = fila;
        cabecera(hoja, e, fila++, d.columnaCategoria(), txt("reporteAdministradores.serie.activados"),
                txt("reporteAdministradores.serie.pendientes"));
        for (ConteoAdministradoresDTO c : conAdmin) {
            Row r = hoja.createRow(fila++);
            texto(r, 0, c.getEtiqueta(), e.celda);
            numero(r, 1, c.getActivados(), e.numero);
            numero(r, 2, c.getPendientesActivar(), e.numero);
        }
        if (conAdmin.isEmpty()) {
            sinDatos(hoja, e, fila++, txt("reporteAdministradores.sinAdministradores"));
        }

        // Gráficos a la derecha de las tablas.
        int col = 9;
        int alto = Math.max(16, d.categorias().size() + 6);
        if (!d.categorias().isEmpty()) {
            barras(hoja, txt("reporteIglesias.grafico.administrador"), col, FILA_CABECERA, col + 9, FILA_CABECERA + alto,
                    primeraCategoria, ultimaCategoria, 0, true, false,
                    new Serie(txt("reporteIglesias.serie.conAdministrador"), 2, AZUL),
                    new Serie(txt("reporteIglesias.serie.sinAdministrador"), 4, AMARILLO));
        }
        if (!conAdmin.isEmpty()) {
            int desde = FILA_CABECERA + alto + 1;
            barras(hoja, txt("reporteAdministradores.grafico.activacion"), col, desde, col + 9,
                    desde + Math.max(16, conAdmin.size() + 6), cabeceraActivacion + 1, cabeceraActivacion + conAdmin.size(),
                    0, true, false,
                    new Serie(txt("reporteAdministradores.serie.activados"), 1, AZUL),
                    new Serie(txt("reporteAdministradores.serie.pendientes"), 2, AMARILLO));
        }
    }

    private static void avance(XSSFWorkbook libro, Estilos e, Datos d, byte[] logo) {
        XSSFSheet hoja = nuevaHoja(libro, e, d, logo, "hoja.avance");
        anchos(hoja, 9000, 5000, 5000);
        int fila = FILA_CABECERA;
        cabecera(hoja, e, fila++, m("col.grupo"), txt("reporteAdministradores.serie.porcentajeHabilitados"),
                txt("reporteAdministradores.serie.porcentajeRevisados"));
        String[] grupos = { txt("reporteIglesias.serie.conAdministrador"), txt("reporteIglesias.serie.sinAdministrador") };
        for (int g = 0; g < grupos.length; g++) {
            Row r = hoja.createRow(fila++);
            texto(r, 0, grupos[g], e.celda);
            numero(r, 1, d.avance()[g][0], e.porcentaje);
            numero(r, 2, d.avance()[g][1], e.porcentaje);
        }
        Row ayuda = hoja.createRow(fila + 1);
        texto(ayuda, 0, txt("reporteAdministradores.grafico.avance.ayuda"), e.nota);
        if (!d.iglesias().isEmpty()) {
            barras(hoja, txt("reporteAdministradores.grafico.avance"), 4, FILA_CABECERA, 13, FILA_CABECERA + 14,
                    FILA_CABECERA + 1, FILA_CABECERA + 2, 0, false, true,
                    new Serie(txt("reporteAdministradores.serie.porcentajeHabilitados"), 1, AZUL),
                    new Serie(txt("reporteAdministradores.serie.porcentajeRevisados"), 2, AMARILLO));
        }
    }

    private static void iglesias(XSSFWorkbook libro, Estilos e, Datos d, byte[] logo, String claveHoja,
            List<AdministradorIglesiaDTO> todas, Predicate<AdministradorIglesiaDTO> incluir) {
        XSSFSheet hoja = nuevaHoja(libro, e, d, logo, claveHoja);
        anchos(hoja, 11000, 6500, 5500, 9000, 5000, 8000, 5500, 5000, 4500, 4200, 4200);
        String[] columnas = { txt("reporteIglesias.col.iglesia"), txt("reporteRegistros.col.parroquia"),
                txt("reporteRegistros.col.canton"), txt("reporteIglesias.col.administrador"), m("col.usuario"),
                m("col.correo"), txt("reporteAdministradores.col.estado"), txt("reporteAdministradores.col.ultimoAcceso"),
                txt("reporteAdministradores.kpi.sinAcceso"), txt("reporteRegistros.serie.habilitados"), m("col.registrados") };
        cabecera(hoja, e, FILA_CABECERA, columnas);
        int fila = FILA_CABECERA + 1;
        for (AdministradorIglesiaDTO i : todas) {
            if (!incluir.test(i)) {
                continue;
            }
            Row r = hoja.createRow(fila++);
            texto(r, 0, i.getIglesia(), e.celda);
            texto(r, 1, i.getParroquia(), e.celda);
            texto(r, 2, i.getCanton(), e.celda);
            boolean tiene = i.isTieneAdministrador();
            texto(r, 3, tiene ? i.getAdministradorNombre() : "", e.celda);
            texto(r, 4, tiene ? i.getAdministradorUsuario() : "", e.celda);
            texto(r, 5, !tiene ? "" : (i.getAdministradorCorreo() == null || i.getAdministradorCorreo().isBlank()
                    ? txt("reporteAdministradores.sinCorreo") : i.getAdministradorCorreo()), e.celda);
            texto(r, 6, estado(i), e.celda);
            if (tiene && i.getUltimoAcceso() != null) {
                Cell c = r.createCell(7);
                c.setCellValue(i.getUltimoAcceso());
                c.setCellStyle(e.fecha);
            } else {
                texto(r, 7, tiene ? txt("reporteAdministradores.nunca") : "", e.celda);
            }
            texto(r, 8, tiene ? m(i.isSinAccesoReciente() ? "si" : "no") : "", e.celda);
            numero(r, 9, i.getHabilitados(), e.numero);
            numero(r, 10, i.getRegistrados(), e.numero);
        }
        if (fila == FILA_CABECERA + 1) {
            sinDatos(hoja, e, fila, m("sinRegistros"));
        } else {
            hoja.setAutoFilter(new CellRangeAddress(FILA_CABECERA, fila - 1, 0, columnas.length - 1));
        }
    }

    private static String estado(AdministradorIglesiaDTO i) {
        if (!i.isTieneAdministrador()) {
            return txt("reporteIglesias.serie.sinAdministrador");
        }
        return txt(i.isAdministradorActivado() ? "reporteAdministradores.estado.activado"
                : "reporteAdministradores.estado.pendiente");
    }

    // ------------------------------------------------------------------ Estructura

    /** Hoja con el encabezado institucional de los Excel de TEC y la línea de filtros. */
    private static XSSFSheet nuevaHoja(XSSFWorkbook libro, Estilos e, Datos d, byte[] logo, String claveHoja) {
        XSSFSheet hoja = libro.createSheet(m(claveHoja));
        ReporteXLSX.crearEncabezadoInstitucional(libro, hoja, m("titulo"),
                logo == null ? InputStream.nullInputStream() : new java.io.ByteArrayInputStream(logo));
        Row filtros = hoja.createRow(2);
        texto(filtros, 2, m("filtros", d.ambito(), d.diasInactividad()), e.nota);
        Row generado = hoja.createRow(3);
        texto(generado, 2, m("generado", d.generado(), d.responsable()), e.nota);
        return hoja;
    }

    private record Serie(String nombre, int columna, byte[] color) {
    }

    /**
     * Barras horizontales nativas, con las categorías de la primera columna. Dos series se
     * apilan (como la web) salvo en porcentajes, que van agrupadas en escala 0-100.
     */
    private static void barras(XSSFSheet hoja, String titulo, int col1, int fila1, int col2, int fila2,
            int primeraFila, int ultimaFila, int columnaCategorias, boolean apilado, boolean porcentaje,
            Serie... series) {
        XSSFDrawing dibujo = hoja.createDrawingPatriarch();
        XSSFClientAnchor ancla = dibujo.createAnchor(0, 0, 0, 0, col1, fila1, col2, fila2);
        XSSFChart grafico = dibujo.createChart(ancla);
        grafico.setTitleText(titulo);
        grafico.setTitleOverlay(false);
        grafico.getOrAddLegend().setPosition(LegendPosition.BOTTOM);

        XDDFCategoryAxis eje = grafico.createCategoryAxis(AxisPosition.LEFT);
        // Primera categoría arriba, igual que la web.
        eje.setOrientation(AxisOrientation.MAX_MIN);
        XDDFValueAxis valores = grafico.createValueAxis(AxisPosition.BOTTOM);
        valores.setCrosses(AxisCrosses.AUTO_ZERO);
        valores.setMinimum(0);
        if (porcentaje) {
            valores.setMaximum(100);
        }
        XDDFDataSource<String> categorias = XDDFDataSourcesFactory.fromStringCellRange(hoja,
                new CellRangeAddress(primeraFila, ultimaFila, columnaCategorias, columnaCategorias));
        XDDFBarChartData datos = (XDDFBarChartData) grafico.createData(ChartTypes.BAR, eje, valores);
        datos.setBarDirection(BarDirection.BAR);
        boolean apilar = apilado && series.length > 1;
        datos.setBarGrouping(apilar ? BarGrouping.STACKED : BarGrouping.CLUSTERED);
        datos.setVaryColors(false);
        for (Serie s : series) {
            XDDFNumericalDataSource<Double> numeros = XDDFDataSourcesFactory.fromNumericCellRange(hoja,
                    new CellRangeAddress(primeraFila, ultimaFila, s.columna(), s.columna()));
            XDDFChartData.Series serie = datos.addSeries(categorias, numeros);
            serie.setTitle(s.nombre(), null);
            XDDFShapeProperties forma = new XDDFShapeProperties();
            forma.setFillProperties(new XDDFSolidFillProperties(XDDFColor.from(s.color())));
            serie.setShapeProperties(forma);
        }
        grafico.plot(datos);
        if (apilar) {
            superponerSeries(grafico);
        }
    }

    /**
     * Barras apiladas: Excel necesita {@code <c:overlap val="100"/>} para dibujar las series
     * una sobre otra. {@code XDDFBarChartData.setOverlap} no sirve con poi-ooxml-lite (el
     * jar del WAR no trae el tipo STOverlapPercent), así que el elemento se escribe con un
     * cursor XML, en su lugar del esquema: justo antes del primer {@code c:axId}.
     */
    private static void superponerSeries(XSSFChart grafico) {
        String ns = "http://schemas.openxmlformats.org/drawingml/2006/chart";
        try (org.apache.xmlbeans.XmlCursor cursor = grafico.getCTChart().getPlotArea().getBarChartArray(0).newCursor()) {
            if (cursor.toChild(new javax.xml.namespace.QName(ns, "axId"))) {
                cursor.beginElement(new javax.xml.namespace.QName(ns, "overlap"));
                cursor.insertAttributeWithValue("val", "100");
            }
        }
    }

    // ------------------------------------------------------------------ Celdas

    private static int indicador(XSSFSheet hoja, Estilos e, int fila, String nombre, long valor, String detalle) {
        Row r = hoja.createRow(fila);
        texto(r, 0, nombre, e.celdaNegrita);
        numero(r, 1, valor, e.numero);
        texto(r, 2, detalle, e.nota);
        return fila + 1;
    }

    private static void cabecera(XSSFSheet hoja, Estilos e, int fila, String... titulos) {
        Row r = hoja.getRow(fila) != null ? hoja.getRow(fila) : hoja.createRow(fila);
        r.setHeightInPoints(30);
        for (int i = 0; i < titulos.length; i++) {
            texto(r, i, titulos[i], e.cabecera);
        }
    }

    private static void titulo(XSSFSheet hoja, Estilos e, int fila, String texto) {
        texto(hoja.createRow(fila), 0, texto, e.seccion);
    }

    private static void sinDatos(XSSFSheet hoja, Estilos e, int fila, String texto) {
        texto(hoja.createRow(fila), 0, texto, e.nota);
    }

    private static void texto(Row fila, int columna, String valor, XSSFCellStyle estilo) {
        Cell c = fila.createCell(columna);
        c.setCellValue(valor == null ? "" : valor);
        c.setCellStyle(estilo);
    }

    private static void numero(Row fila, int columna, long valor, XSSFCellStyle estilo) {
        Cell c = fila.createCell(columna);
        c.setCellValue(valor);
        c.setCellStyle(estilo);
    }

    private static void anchos(XSSFSheet hoja, int... anchos) {
        for (int i = 0; i < anchos.length; i++) {
            hoja.setColumnWidth(i, anchos[i]);
        }
    }

    // ------------------------------------------------------------------ Textos

    /** Texto propio del Excel (reporteAdministradores.excel.*). */
    private static String m(String clave, Object... argumentos) {
        return Constantes.getMensaje("reporteAdministradores.excel." + clave, argumentos);
    }

    /** Texto ya usado por la pestaña en la web. */
    private static String txt(String clave) {
        return Constantes.getMensaje(clave);
    }

    // ------------------------------------------------------------------ Estilos

    /** Estilos del libro: cabecera azul con texto blanco, como los demás Excel de TEC. */
    private static final class Estilos {
        final XSSFCellStyle cabecera;
        final XSSFCellStyle celda;
        final XSSFCellStyle celdaNegrita;
        final XSSFCellStyle numero;
        final XSSFCellStyle porcentaje;
        final XSSFCellStyle fecha;
        final XSSFCellStyle seccion;
        final XSSFCellStyle nota;

        Estilos(XSSFWorkbook libro) {
            XSSFFont blanca = libro.createFont();
            blanca.setBold(true);
            blanca.setColor(IndexedColors.WHITE.index);
            cabecera = libro.createCellStyle();
            cabecera.setFont(blanca);
            cabecera.setFillForegroundColor(IndexedColors.DARK_BLUE.index);
            cabecera.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cabecera.setAlignment(HorizontalAlignment.CENTER);
            cabecera.setWrapText(true);
            bordes(cabecera);

            celda = libro.createCellStyle();
            bordes(celda);
            celdaNegrita = libro.createCellStyle();
            bordes(celdaNegrita);
            XSSFFont negrita = libro.createFont();
            negrita.setBold(true);
            celdaNegrita.setFont(negrita);

            numero = libro.createCellStyle();
            bordes(numero);
            numero.setDataFormat(libro.createDataFormat().getFormat("#,##0"));
            porcentaje = libro.createCellStyle();
            bordes(porcentaje);
            porcentaje.setDataFormat(libro.createDataFormat().getFormat("0\"%\""));
            fecha = libro.createCellStyle();
            bordes(fecha);
            fecha.setDataFormat(libro.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));

            XSSFFont azul = libro.createFont();
            azul.setBold(true);
            azul.setFontHeightInPoints((short) 12);
            azul.setColor(IndexedColors.DARK_BLUE.index);
            seccion = libro.createCellStyle();
            seccion.setFont(azul);

            XSSFFont gris = libro.createFont();
            gris.setColor(IndexedColors.GREY_50_PERCENT.index);
            nota = libro.createCellStyle();
            nota.setFont(gris);
        }

        private static void bordes(XSSFCellStyle estilo) {
            estilo.setBorderBottom(BorderStyle.THIN);
            estilo.setBorderTop(BorderStyle.THIN);
            estilo.setBorderLeft(BorderStyle.THIN);
            estilo.setBorderRight(BorderStyle.THIN);
            estilo.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.index);
            estilo.setTopBorderColor(IndexedColors.GREY_25_PERCENT.index);
            estilo.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.index);
            estilo.setRightBorderColor(IndexedColors.GREY_25_PERCENT.index);
        }
    }
}
