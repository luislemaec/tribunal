package ec.com.antenasur.itext;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ec.com.antenasur.dto.FilaPadronDTO;

/** Verifica contenido y totales de un reporte que supera varias ventanas SXSSF. */
public class PadronGeneralXlsxCheck {
    public static void main(String[] args) throws Exception {
        byte[] datos;
        try (var logo = Files.newInputStream(Path.of("src/main/webapp/resources/img/logo_consejo_417x150.png"))) {
            datos = ReporteXLSX.generarPadronGeneral(consumir -> {
                for (int i = 1; i <= 1201; i++) consumir.accept(new FilaPadronDTO(i, i, i, "SN-" + i,
                        "Nombres " + i, "Apellidos", "Iglesia de prueba", 3, "Proceso prueba", "Provincia",
                        "Canton", "Parroquia", "Recinto prueba", 1, "Mesa prueba", 2, true));
            }, logo);
        }
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(datos))) {
            var detalle = libro.getSheetAt(0); var totales = libro.getSheetAt(1);
            // Encabezado compacto: fila 1 logo y título, filas 2-3 datos, títulos en la fila 4.
            if (detalle.getLastRowNum() != 1204) throw new AssertionError("filas");
            if (!"SN-1201".equals(detalle.getRow(1204).getCell(7).getStringCellValue())) throw new AssertionError("ultimo documento");
            if (detalle.getRow(ReporteXLSX.FILA_TABLA_ELECTORAL).getCell(0).getStringCellValue().isBlank())
                throw new AssertionError("titulos de la tabla");
            if (detalle.getNumMergedRegions() != 0) throw new AssertionError("sin celdas combinadas");
            for (int fila : new int[]{0, 2, 4, 6}) {
                if (totales.getRow(fila).getCell(1).getNumericCellValue() != 1201) throw new AssertionError("total " + fila);
            }
            if (!detalle.getCTWorksheet().isSetAutoFilter()) throw new AssertionError("filtro");
        }
        if (args.length > 0) Files.write(Path.of(args[0]), datos);

        // Con ámbito: los seis datos electorales en dos filas desde la columna A.
        var ambito = new java.util.LinkedHashMap<String, String>();
        ambito.put("proceso", "Elecciones 2026"); ambito.put("provincia", "CHIMBORAZO"); ambito.put("canton", "ALAUSÍ");
        ambito.put("parroquia", "TIXÁN"); ambito.put("recinto", "UNIDAD EDUCATIVA CEDEÑO DE LA COMUNIDAD"); ambito.put("mesa", "1");
        byte[] conAmbito;
        try (var logo = Files.newInputStream(Path.of("src/main/webapp/resources/img/logo_consejo_417x150.png"))) {
            conAmbito = ReporteXLSX.generarPadronGeneral(consumir -> consumir.accept(new FilaPadronDTO(1, 1, 1, "SN-1",
                    "Nombres", "Apellidos", "Iglesia de prueba", 3, "Elecciones 2026", "CHIMBORAZO", "ALAUSÍ", "TIXÁN",
                    "UNIDAD EDUCATIVA CEDEÑO DE LA COMUNIDAD", 1, "1", 2, true)), ambito, logo);
        }
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(conAmbito))) {
            var detalle = libro.getSheetAt(0);
            String fila2 = detalle.getRow(1).getCell(0).getStringCellValue();
            String fila3 = detalle.getRow(2).getCell(0).getStringCellValue();
            for (String valor : ambito.values()) {
                if (!(fila2 + fila3).contains(valor)) throw new AssertionError("dato " + valor);
            }
            if (!fila2.contains("Elecciones 2026") || !fila3.contains("TIXÁN")) throw new AssertionError("orden");
            if (detalle.getRow(3).getCell(0) == null) throw new AssertionError("tabla en la fila 4");
            if (args.length > 1) Files.write(Path.of(args[1]), conAmbito);
        }
        System.out.println("OK: Excel de 1201 empadronados, totales general/recinto/mesa/iglesia, filtros y encabezado compacto.");
    }
}
