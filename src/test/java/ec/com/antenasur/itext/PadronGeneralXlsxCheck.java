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
            if (detalle.getLastRowNum() != 1206) throw new AssertionError("filas");
            if (!"SN-1201".equals(detalle.getRow(1206).getCell(7).getStringCellValue())) throw new AssertionError("ultimo documento");
            for (int fila : new int[]{0, 2, 4, 6}) {
                if (totales.getRow(fila).getCell(1).getNumericCellValue() != 1201) throw new AssertionError("total " + fila);
            }
            if (!detalle.getCTWorksheet().isSetAutoFilter()) throw new AssertionError("filtro");
        }
        if (args.length > 0) Files.write(Path.of(args[0]), datos);
        System.out.println("OK: Excel de 1201 empadronados, totales general/recinto/mesa/iglesia y filtros.");
    }
}
