package ec.com.antenasur.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import ec.com.antenasur.dto.FilaPadronImportadaDTO;
import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.model.Persona;

/**
 * Parser del formato Excel del padrón electoral. Convierte filas de un
 * {@link XSSFWorkbook} en una lista de {@link FilaPadronImportadaDTO}, sin
 * tocar BD ni servicios.
 *
 * <h3>Formato esperado</h3>
 * <ul>
 *   <li>Fila 0 = cabecera (se descarta)</li>
 *   <li>Columna 1 = nombres de la persona (máx 100)</li>
 *   <li>Columna 2 = cédula (máx 11, se eliminan guiones)</li>
 *   <li>Columna 3 = nombre de iglesia (máx 255)</li>
 *   <li>Columna 5 = id numérico de ubicación (Geograp)</li>
 *   <li>Columna 6 = nombre de comunidad (máx 255)</li>
 *   <li>Columna 9 = nombre de la mesa</li>
 * </ul>
 *
 * Las celdas de texto se trunca al tamaño máximo de la columna BD y se pasan
 * a mayúsculas. La cédula se normaliza eliminando guiones.
 */
public final class ExcelPadronParser {

    private static final int COL_NOMBRES = 1;
    private static final int COL_CEDULA = 2;
    private static final int COL_IGLESIA = 3;
    private static final int COL_UBICACION = 5;
    private static final int COL_COMUNIDAD = 6;
    private static final int COL_MESA = 9;

    private static final int MAX_NOMBRES = 100;
    private static final int MAX_CEDULA = 11;
    private static final int MAX_TEXTO = 255;

    private ExcelPadronParser() {
    }

    /**
     * Parsea la primera hoja del workbook. La primera fila se descarta (es la
     * cabecera del Excel del cliente).
     */
    public static List<FilaPadronImportadaDTO> parsear(XSSFWorkbook workbook) {
        List<FilaPadronImportadaDTO> resultado = new ArrayList<>();
        if (workbook == null) {
            return resultado;
        }
        XSSFSheet hoja = workbook.getSheetAt(0);
        Iterator<Row> filas = hoja.iterator();
        int indice = 0;
        while (filas.hasNext()) {
            Row fila = filas.next();
            if (indice >= 1) {
                resultado.add(parsearFila(fila));
            }
            indice++;
        }
        return resultado;
    }

    private static FilaPadronImportadaDTO parsearFila(Row fila) {
        Persona persona = new Persona();
        Iglesia iglesia = new Iglesia();
        Integer ubicacionId = null;
        String nombreMesa = null;

        for (int col = 1; col < fila.getLastCellNum(); col++) {
            Cell cell = fila.getCell(col);
            if (cell == null) {
                continue;
            }
            switch (col) {
                case COL_NOMBRES:
                    persona.setNombres(textoCelda(cell, MAX_NOMBRES));
                    break;
                case COL_CEDULA:
                    String cedula = textoCelda(cell, MAX_CEDULA);
                    persona.setDocumento(cedula != null ? cedula.replace("-", "") : null);
                    break;
                case COL_IGLESIA:
                    iglesia.setNombre(textoCelda(cell, MAX_TEXTO));
                    break;
                case COL_UBICACION:
                    ubicacionId = (int) cell.getNumericCellValue();
                    break;
                case COL_COMUNIDAD:
                    iglesia.setComunidad(textoCelda(cell, MAX_TEXTO));
                    break;
                case COL_MESA:
                    nombreMesa = truncar(cell.getStringCellValue().trim(), MAX_TEXTO);
                    break;
                default:
                    break;
            }
        }
        return new FilaPadronImportadaDTO(persona, iglesia, ubicacionId, nombreMesa);
    }

    /** Lee texto de una celda, lo trunca al tamaño dado y lo pasa a UPPERCASE. */
    private static String textoCelda(Cell cell, int tamanioMax) {
        String valor = truncar(cell.getStringCellValue().trim(), tamanioMax);
        return (valor == null || valor.isEmpty()) ? null : valor.toUpperCase();
    }

    private static String truncar(String cadena, int tamanioMax) {
        if (cadena == null) {
            return null;
        }
        return (cadena.length() > tamanioMax) ? cadena.substring(0, tamanioMax - 1) : cadena;
    }
}
