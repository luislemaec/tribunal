package ec.com.antenasur.itext;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

    private static ExternalContext externalContext;

    private static String PATH_LOGO;

    private static XSSFWorkbook LIBRO;// Libro

    private static XSSFSheet HOJA;// Hoja

    private static FileInputStream stream;

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
            externalContext = FacesContext.getCurrentInstance().getExternalContext();
            /* Agrega logo institucional a la cabecera del documento. */
            PATH_LOGO = Constantes.getPathLogo();
            LIBRO = new XSSFWorkbook();
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
            setimagen(LIBRO, HOJA, nombreReporte, stream);

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

            Row encabezado = HOJA.createRow(5);
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
            Row rfecha = HOJA.createRow(3);
            Cell cfecha = rfecha.createCell(3);
            cfecha.setCellValue("FECHA Y HORA:");

            Cell cfecha_v = rfecha.createCell(4);
            cfecha_v.setCellValue(fecha + " " + hora);

            Row rResponsable = HOJA.createRow(4);
            Cell cResponsable = rResponsable.createCell(3);
            cResponsable.setCellValue("RESPONSABLE:");

            Cell cResponsable_v = rResponsable.createCell(4);
            cResponsable_v.setCellValue(responsable);
        } catch (Exception e) {
            LOG.error("ERROR AL CREAR ESPACIO INFORMATIVO" + e);
        }

    }

    public static void setFinalParagraph(int tamanioLista) {
        try {
            String date = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
            String pieDePagina = " Documento generado por: " + getNombreUsuarioAutenticado() + " Fecha: " + date.substring(0, 10) + " Hora: "
                    + date.substring(11, 19);
            Row pie = HOJA.createRow(tamanioLista + 7);
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
                Row fila = HOJA.createRow(posRow + 5);
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

    public static void descargarExcel(String nombreReporte) {
        try {

            HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();
            OutputStream out = response.getOutputStream();
            response.setContentType("application/xlsx");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + nombreReporte + ".xlsx\"");
            response.setDateHeader("Expires", 0);
            try {
                LIBRO.write(out);
                out.flush();
            } catch (IOException ex) {
                LOG.error("ERROR AL DESCARGAR ARCHIVO EXCEL" + ex);
            }
            out.flush();
            FacesContext.getCurrentInstance().responseComplete();
        } catch (IOException ex) {
            LOG.error("ERROR AL DESCARGAR ARCHIVO EXCEL" + ex);
        }
    }

    public static void setimagen(XSSFWorkbook workbook, Sheet sheet, String name, InputStream file) {
        try {
            XSSFSheet xssfSheet = (XSSFSheet) sheet;
            xssfSheet.setDisplayGridlines(false);
            xssfSheet.setColumnWidth(0, 5000);
            xssfSheet.setColumnWidth(1, 5000);
            xssfSheet.setColumnWidth(2, 4200);

            // TITULO DEL PROYECTO
            XSSFFont whiteFont = workbook.createFont();
            whiteFont.setColor(IndexedColors.DARK_BLUE.index);
            whiteFont.setFontHeightInPoints((short) 16.00);
            whiteFont.setBold(true);

            XSSFCellStyle cellheader = workbook.createCellStyle();
            cellheader.setAlignment(HorizontalAlignment.CENTER);
            cellheader.setWrapText(true);
            cellheader.setFont(whiteFont);

            Row row0 = sheet.createRow((short) 0);
            row0.setHeightInPoints(34);
            Row row = sheet.createRow((short) 1);// Fila que salta
            row.setHeightInPoints((float) 34);
            Cell cell = row.createCell((short) 3);// Columna que inicia el titulo
            sheet.addMergedRegion(CellRangeAddress.valueOf("$D$2:$H$2"));// Celdas combinadas
            cell.setCellValue(new XSSFRichTextString(Constantes.INSTITUCION));
            cell.setCellStyle(cellheader);

            // NOMBRE DEL REPORTE
            XSSFFont whiteFont1 = workbook.createFont();
            whiteFont1.setColor(IndexedColors.GREY_50_PERCENT.index);
            whiteFont1.setFontHeightInPoints((short) 14.00);
            whiteFont1.setBold(true);

            XSSFCellStyle cellheader1 = workbook.createCellStyle();
            cellheader1.setAlignment(HorizontalAlignment.CENTER);
            cellheader1.setWrapText(true);
            cellheader1.setFont(whiteFont1);

            Row row1 = sheet.createRow((short) 2);
            row1.setHeightInPoints((float) 28);
            Cell cell1 = row1.createCell((short) 3);
            sheet.addMergedRegion(CellRangeAddress.valueOf("$D$3:$H$3"));
            cell1.setCellValue(new XSSFRichTextString(name));
            cell1.setCellStyle(cellheader1);

            // Get the contents of an InputStream as a byte[].
            byte[] bytes = IOUtils.toByteArray(file);
            java.awt.image.BufferedImage logo = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            int logoWidth = logo != null ? logo.getWidth() : HEADER_LOGO_MAX_WIDTH_PX;
            int logoHeight = logo != null ? logo.getHeight() : HEADER_LOGO_MAX_HEIGHT_PX;
            double scale = Math.min(
                    (double) HEADER_LOGO_MAX_WIDTH_PX / logoWidth,
                    (double) HEADER_LOGO_MAX_HEIGHT_PX / logoHeight);
            scale = Math.min(scale, 1.0d);

            // Adds a picture to the workbook
            int pictureIdx = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);
            // close the input stream
            // Returns an object that handles instantiating concrete classes
            CreationHelper helper = workbook.getCreationHelper();
            // Creates the top-level drawing patriarch.
            Drawing drawing = sheet.createDrawingPatriarch();
            // Create an anchor that is attached to the worksheet
            ClientAnchor anchor = helper.createClientAnchor();
            // set top-left corner for the image
            anchor.setDx1(Units.pixelToEMU(8));
            anchor.setDy1(Units.pixelToEMU(6));

            anchor.setCol1(0);// Columna donde inicia el logo
            anchor.setRow1(0);
            anchor.setCol2(3);
            anchor.setRow2(4);
            if (anchor instanceof XSSFClientAnchor) {
                ((XSSFClientAnchor) anchor).setAnchorType(ClientAnchor.AnchorType.DONT_MOVE_AND_RESIZE);
            }
            // Creates a picture
            Picture pict = drawing.createPicture(anchor, pictureIdx);
            pict.resize(scale);
            sheet.createFreezePane(0, 6, 0, 6);// 0, 7, 0, 7
        } catch (Exception ex) {
            LOG.error("ERROR AL ASIGNAR IMAGEN AL ARCHIVO EXCEL" + ex);
        }

    }

}
