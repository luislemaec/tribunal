package ec.com.antenasur.itext;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.JsfUtil;

public class ReporteXLSX {

    private static final Logger LOG = LoggerFactory.getLogger(ReporteXLSX.class);

    private static final String MIME_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static String PATH_LOGO;

    private static XSSFWorkbook LIBRO;// Libro

    private static XSSFSheet HOJA;// Hoja

    private static FileInputStream stream;

    private static int FILA_CABECERA_TABLA = 5;

    private static final int HEADER_LOGO_MAX_WIDTH_PX = 300;

    private static final int HEADER_LOGO_MAX_HEIGHT_PX = 108;

    public static String getNombreUsuarioAutenticado() {
        String userName = JsfUtil.getNombreUsuarioAutenticado();
        return tieneTexto(userName) ? userName : "<desconocido>";
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }

    private static void inicializa() {
        try {
            /* Agrega logo institucional a la cabecera del documento. */
            PATH_LOGO = Constantes.getPathLogo();
            LIBRO = new XSSFWorkbook();
            FILA_CABECERA_TABLA = 5;
        } catch (Exception e) {
            LOG.error("ERROR AL INICIALIZAR VALORES" + e);
        }
    }

    public static void nuevoExcel(String nombreReporte) {
        inicializa();
        HOJA = LIBRO.createSheet(nombreReporte);
        // Aqui Inserta Imagen
        try {
            stream = new FileInputStream(PATH_LOGO);
            crearEncabezadoInstitucional(LIBRO, HOJA, nombreReporte, stream);

        } catch (FileNotFoundException ex) {
            LOG.error("ERROR AL CREAR NUEVO EXCEL" + ex);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ex) {
                    LOG.error("ERROR AL CERRAR LOGO DEL EXCEL" + ex);
                }
            }
        }
    }

    public static void creaCabeceraTabla(String[] listColumnNames, int[] columnWidth) {
        try {
            XSSFFont fuenteTituloTabla = LIBRO.createFont();
            fuenteTituloTabla.setColor(IndexedColors.WHITE.index);
            fuenteTituloTabla.setBold(true);

            XSSFCellStyle celdaTituloTabla = LIBRO.createCellStyle();
            celdaTituloTabla.setAlignment(HorizontalAlignment.CENTER);
            celdaTituloTabla.setWrapText(true);
            celdaTituloTabla.setFont(fuenteTituloTabla);
            celdaTituloTabla.setFillForegroundColor(IndexedColors.DARK_BLUE.index);
            celdaTituloTabla.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            Row encabezado = HOJA.createRow(FILA_CABECERA_TABLA);
            encabezado.setHeightInPoints(24);
            for (int i = 0; i < listColumnNames.length; i++) {
                HOJA.setColumnWidth(i, columnWidth[i]);
                Cell celda = encabezado.createCell(i);
                celda.setCellValue(listColumnNames[i].toUpperCase());
                celda.setCellStyle(celdaTituloTabla);
            }
        } catch (Exception e) {
            LOG.error("ERROR AL CREAR CABECERA DE TABLA" + e);
        }
    }

    public static void creaEspacioInformativo(String fecha, String hora, String responsable) {
        try {
            Row rfecha = HOJA.createRow(3);//FILA DONDE INICIA EL ESPACIO INFORMATIVO

            Cell cfecha = rfecha.createCell(1); //COLUMNA DONDE INICIA EL ESPACIO INFORMATIVO
            cfecha.setCellValue("FECHA Y HORA:"); //ETIQUETA DEL ESPACIO INFORMATIVO

            Cell cfecha_v = rfecha.createCell(2);
            cfecha_v.setCellValue(fecha + " " + hora);
    
            Cell cResponsable = rfecha.createCell(3);//COLUMNA DONDE INICIA EL ESPACIO INFORMATIVO
            cResponsable.setCellValue("RESPONSABLE:");//ETIQUETA DEL ESPACIO INFORMATIVO

            Cell cResponsable_v = rfecha.createCell(4);//COLUMNA DONDE INICIA EL ESPACIO INFORMATIVO
            cResponsable_v.setCellValue(responsable);//VALOR DEL ESPACIO INFORMATIVO

        } catch (Exception e) {
            LOG.error("ERROR AL CREAR ESPACIO INFORMATIVO" + e);
        }

    }

    public static void creaEspacioInformativoPadron(String fecha, String hora, String responsable,
            String proceso, String provincia, String canton, String parroquia, String recinto, String mesa) {
        try {
            //creaEspacioInformativo(fecha, hora, responsable);
            //(3, "FECHA Y HORA:", fecha + " " + hora, "RESPONSABLE:", responsable);
            crearFilaInformativa(3, "PROCESO:", proceso, "RECINTO:", recinto);
            crearFilaInformativa(4, "PROVINCIA:", provincia, "CANTON:", canton);
            crearFilaInformativa(5, "PARROQUIA:", parroquia, "MESA:", mesa);
            FILA_CABECERA_TABLA = 7;
        } catch (Exception e) {
            LOG.error("ERROR AL CREAR ESPACIO INFORMATIVO DEL PADRON" + e);
        }
    }

    private static void crearFilaInformativa(int fila, String etiquetaA, String valorA, String etiquetaB, String valorB) {
        Row row = HOJA.createRow(fila);

        Cell labelA = row.createCell(1);
        labelA.setCellValue(etiquetaA);

        Cell valueA = row.createCell(2);
        valueA.setCellValue(valorA != null ? valorA : "");

        Cell labelB = row.createCell(3);
        labelB.setCellValue(etiquetaB != null ? etiquetaB : "");

        Cell valueB = row.createCell(4);
        valueB.setCellValue(valorB != null ? valorB : "");
    }

    public static void setFinalParagraph(int tamanioLista) {
        try {
            String date = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
            String pieDePagina = " Documento generado por: " + getNombreUsuarioAutenticado() + " Fecha: " + date.substring(0, 10) + " Hora: "
                    + date.substring(11, 19);
            Row pie = HOJA.createRow(tamanioLista + FILA_CABECERA_TABLA + 2);
            Cell celda = pie.createCell(3);
            celda.setCellValue(pieDePagina);
        } catch (Exception e) {
            LOG.error("ERROR AL AGREGAR PARRAFO FINAL" + e);
        }
    }

    public static void creaContenidoTabla(String[][] listaDatos, String[] listColumnNames) {
        try {
            int posRow = 1;
            for (String[] medio : listaDatos) {
                Row fila = HOJA.createRow(posRow + FILA_CABECERA_TABLA);
                for (int i = 0; i < medio.length; i++) {
                    Cell celda = fila.createCell(i);
                    celda.setCellValue(medio[i]);
                }
                posRow++;
            }
        } catch (Exception e) {
            LOG.error("ERROR AL CREAR CONTENIDO DE TABLA" + e);
        }
    }

    public static void descargarExcel(String nombreReporte) throws IOException {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null || LIBRO == null) {
            throw new IOException("No existe un contexto o libro Excel disponible para descargar.");
        }
        ExternalContext contexto = facesContext.getExternalContext();
        String nombreSeguro = tieneTexto(nombreReporte)
                ? nombreReporte.replaceAll("[\\r\\n\\\"/\\\\]", "_") : "reporte";
        contexto.responseReset();
        HttpServletResponse response = (HttpServletResponse) contexto.getResponse();
        response.setContentType(MIME_XLSX);
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + nombreSeguro + ".xlsx\"");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setDateHeader("Expires", 0);
        OutputStream out = response.getOutputStream();
        try {
            LIBRO.write(out);
            out.flush();
            facesContext.responseComplete();
        } finally {
            try {
                LIBRO.close();
            } finally {
                LIBRO = null;
                HOJA = null;
            }
        }
    }

    /** Obtiene el libro actual para almacenamiento institucional sin escribir la respuesta HTTP. */
    public static byte[] obtenerContenidoExcel() throws IOException {
        if (LIBRO == null) {
            throw new IOException("No existe un libro Excel inicializado.");
        }
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            LIBRO.write(salida);
            return salida.toByteArray();
        }
    }

    public static void crearEncabezadoInstitucional(XSSFWorkbook workbook, Sheet sheet, String nombreReporte, InputStream file) {
        try {
            XSSFSheet xssfSheet = (XSSFSheet) sheet;
            configurarHojaReporte(xssfSheet);
            crearTituloInstitucional(workbook, sheet);
            crearTituloReporte(workbook, sheet, nombreReporte);
            insertarLogo(workbook, sheet, file);
            configurarPanelFijo(sheet);
        } catch (Exception ex) {
            LOG.error("ERROR AL CREAR ENCABEZADO INSTITUCIONAL DEL EXCEL" + ex);
        }

    }

    private static void configurarHojaReporte(XSSFSheet sheet) {
        sheet.setDisplayGridlines(false);
        sheet.setColumnWidth(0, 5000);
        sheet.setColumnWidth(1, 5000);
        sheet.setColumnWidth(2, 4200);
    }

    private static void crearTituloInstitucional(XSSFWorkbook workbook, Sheet sheet) {
        XSSFCellStyle estilo = crearEstiloTitulo(workbook, IndexedColors.DARK_BLUE, 16);
        
        Row row = sheet.createRow(0);
        row.setHeightInPoints(34);
        Cell cell = row.createCell(2);
        sheet.addMergedRegion(CellRangeAddress.valueOf("$C$1:$F$1"));
        cell.setCellValue(new XSSFRichTextString(Constantes.INSTITUCION));
        cell.setCellStyle(estilo);
    }

    private static void crearTituloReporte(XSSFWorkbook workbook, Sheet sheet, String nombreReporte) {
        XSSFCellStyle estilo = crearEstiloTitulo(workbook, IndexedColors.GREY_50_PERCENT, 14);
        Row row = sheet.createRow(1);
        row.setHeightInPoints(28);
        Cell cell = row.createCell(2);
        sheet.addMergedRegion(CellRangeAddress.valueOf("$C$2:$F$2"));
        cell.setCellValue(new XSSFRichTextString(nombreReporte));
        cell.setCellStyle(estilo);
    }

    private static XSSFCellStyle crearEstiloTitulo(XSSFWorkbook workbook, IndexedColors color, int tamanioFuente) {
        XSSFFont font = workbook.createFont();
        font.setColor(color.index);
        font.setFontHeightInPoints((short) tamanioFuente);
        font.setBold(true);

        XSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setWrapText(true);
        style.setFont(font);
        return style;
    }

    private static void insertarLogo(XSSFWorkbook workbook, Sheet sheet, InputStream file) throws IOException {
        byte[] bytes = IOUtils.toByteArray(file);
        double escala = calcularEscalaLogo(bytes);
        int pictureIdx = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);
        Drawing drawing = sheet.createDrawingPatriarch();
        Picture pict = drawing.createPicture(crearAnclaLogo(workbook), pictureIdx);
        pict.resize(escala);
    }

    private static double calcularEscalaLogo(byte[] bytes) throws IOException {
        java.awt.image.BufferedImage logo = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        int logoWidth = logo != null ? logo.getWidth() : HEADER_LOGO_MAX_WIDTH_PX;
        int logoHeight = logo != null ? logo.getHeight() : HEADER_LOGO_MAX_HEIGHT_PX;
        double escala = Math.min(
                (double) HEADER_LOGO_MAX_WIDTH_PX / logoWidth,
                (double) HEADER_LOGO_MAX_HEIGHT_PX / logoHeight);
        return Math.min(escala, 1.0d);
    }

    private static ClientAnchor crearAnclaLogo(XSSFWorkbook workbook) {
        CreationHelper helper = workbook.getCreationHelper();
        ClientAnchor anchor = helper.createClientAnchor();
        anchor.setDx1(Units.pixelToEMU(8));
        anchor.setDy1(Units.pixelToEMU(6));
        anchor.setCol1(0);
        anchor.setRow1(0);
        anchor.setCol2(3);
        anchor.setRow2(4);
        if (anchor instanceof XSSFClientAnchor) {
            ((XSSFClientAnchor) anchor).setAnchorType(ClientAnchor.AnchorType.DONT_MOVE_AND_RESIZE);
        }
        return anchor;
    }

    private static void configurarPanelFijo(Sheet sheet) {
        sheet.createFreezePane(0, FILA_CABECERA_TABLA + 1, 0, FILA_CABECERA_TABLA + 1);
    }

}
