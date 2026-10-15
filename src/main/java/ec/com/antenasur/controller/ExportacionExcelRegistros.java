package ec.com.antenasur.controller;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.faces.context.FacesContext;

import ec.com.antenasur.itext.ReporteRegistrosXLSX;
import ec.com.antenasur.itext.ReporteXLSX;
import ec.com.antenasur.util.JsfUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Descarga de los Excel de las pestañas de Rep. Registros: logo institucional, fecha y usuario
 * del encabezado, nombre del archivo y el mensaje de error común. Cada controlador solo arma
 * su contenido con los datos que ya tiene cargados.
 */
@Slf4j
final class ExportacionExcelRegistros {

    private static final String LOGO = "/resources/img/logo_consejo_417x150.png";

    /** Genera el contenido del Excel con el logo institucional. */
    @FunctionalInterface
    interface Generador {
        byte[] generar(byte[] logo) throws Exception;
    }

    private ExportacionExcelRegistros() {
    }

    /** Encabezado con el filtro de cantón de la pestaña. */
    static ReporteRegistrosXLSX.Encabezado encabezado(String filtro, boolean todosLosCantones) {
        return new ReporteRegistrosXLSX.Encabezado(filtro,
                JsfUtil.getMessage(todosLosCantones ? "reporteRegistros.col.canton" : "reporteRegistros.col.parroquia"),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                ReporteXLSX.getNombreUsuarioAutenticado());
    }

    /** «Filtro: Todos los cantones» o el cantón elegido. */
    static String filtroCanton(boolean todosLosCantones, String canton) {
        return JsfUtil.getMessage("reporteRegistros.excel.filtro",
                todosLosCantones ? JsfUtil.getMessage("reporteRegistros.canton.todos") : canton);
    }

    /**
     * Genera y descarga el archivo «{prefijo}_aaaammdd_hhmm.xlsx». Si falla, informa con el
     * mensaje común sin interrumpir la vista.
     */
    static void descargar(String claveArchivo, Generador generador) {
        try (InputStream entrada = FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream(LOGO)) {
            byte[] contenido = generador.generar(entrada == null ? null : entrada.readAllBytes());
            ReporteXLSX.descargarContenido(contenido, JsfUtil.getMessage(claveArchivo) + "_"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")));
        } catch (Exception e) {
            log.error("Error al exportar Excel de Rep. Registros ({})", claveArchivo, e);
            JsfUtil.addErrorMessage(JsfUtil.getMessage("reporteRegistros.excel.error"));
        }
    }
}
