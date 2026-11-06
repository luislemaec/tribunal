package ec.com.antenasur.controller;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.faces.context.FacesContext;

import ec.com.antenasur.bean.ProcesoBean;
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

    /** Acción de la bitácora para las exportaciones a Excel (CatalogoActividades: «Exportó reporte Excel»). */
    static final String ACCION_EXPORTAR = "EXPORTAR_REPORTE_EXCEL";

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
    static void descargar(String claveArchivo, String clavePestana, String filtro, Generador generador) {
        String archivo = JsfUtil.getMessage(claveArchivo) + "_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
        try (InputStream entrada = FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream(LOGO)) {
            byte[] contenido = generador.generar(entrada == null ? null : entrada.readAllBytes());
            ReporteXLSX.descargarContenido(contenido, archivo);
        } catch (Exception e) {
            // Sin registro de auditoría: la exportación no se completó.
            log.error("Error al exportar Excel de Rep. Registros ({})", claveArchivo, e);
            JsfUtil.addErrorMessage(JsfUtil.getMessage("reporteRegistros.excel.error"));
            return;
        }
        registrarExportacion(clavePestana, filtro, archivo + ".xlsx");
    }

    /**
     * Bitácora (tec.procesos, mismo mecanismo que el resto de TEC: ProcesoBean.okActivityRegister)
     * de una exportación completada: usuario, fecha y hora e IP los pone el registro; el texto
     * sigue el formato estructurado del login y Actividades lo muestra como «Exportó reporte Excel»:
     * <pre>EXPORTAR_REPORTE_EXCEL | MÓDULO: REPORTES; RESULTADO: EXITOSO; DETALLE: Reporte de registros – Iglesias; Filtro: …; Archivo: …</pre>
     * Un fallo al registrar no afecta a la descarga, que ya se entregó.
     */
    private static void registrarExportacion(String clavePestana, String filtro, String archivo) {
        registrar("REPORTES", JsfUtil.getMessage("pagina.reporteRegistros.titulo") + " – "
                + JsfUtil.getMessage(clavePestana) + (filtro == null || filtro.isBlank() ? "" : "; " + filtro), archivo);
    }

    /**
     * Misma entrada EXPORTAR_REPORTE_EXCEL para cualquier exportación de TEC (también fuera
     * de Reportes, p. ej. Personas eliminadas), con el módulo que corresponda.
     */
    static void registrar(String modulo, String descripcion, String archivo) {
        String datos = "MÓDULO: " + modulo + "; RESULTADO: EXITOSO; DETALLE: ";
        String detalle = descripcion + "; Archivo: " + archivo;
        // tec.procesos.actividad admite 255 caracteres: se acorta el detalle, nunca el formato.
        int disponible = LARGO_ACTIVIDAD - ACCION_EXPORTAR.length() - 3 - datos.length();
        if (detalle.length() > disponible) {
            detalle = detalle.substring(0, Math.max(0, disponible));
        }
        escribir(datos + detalle, archivo);
    }

    /**
     * Exportación con la cantidad de registros (exportaciones con datos personales: Personas,
     * Iglesias y Padrón). Ver {@link #componerDetalle}: la cantidad y el archivo nunca se recortan.
     */
    static void registrar(String modulo, String descripcion, long registros, String archivo) {
        escribir(componerDetalle(modulo, descripcion, registros, archivo), archivo);
    }

    /** tec.procesos.actividad admite 255 caracteres. */
    static final int LARGO_ACTIVIDAD = 255;

    /**
     * Texto posterior a «EXPORTAR_REPORTE_EXCEL | » (contrato de la bitácora, ver
     * specs/001-auditoria-exportaciones-excel/contracts/entrada-bitacora.md):
     * <pre>MÓDULO: X; RESULTADO: EXITOSO; DETALLE: &lt;descripción&gt;; Registros: N; Archivo: f.xlsx</pre>
     * Si la actividad completa supera 255 caracteres se acorta solo la descripción (título y
     * filtros): el formato, la cantidad y el nombre del archivo se conservan íntegros.
     */
    static String componerDetalle(String modulo, String descripcion, long registros, String archivo) {
        String prefijo = "MÓDULO: " + modulo + "; RESULTADO: EXITOSO; DETALLE: ";
        String sufijo = "; Registros: " + registros + "; Archivo: " + archivo;
        String texto = descripcion == null ? "" : descripcion.replaceAll("[\\r\\n\\t]+", " ").trim();
        int disponible = LARGO_ACTIVIDAD - ACCION_EXPORTAR.length() - 3 - prefijo.length() - sufijo.length();
        if (texto.length() > disponible) {
            texto = texto.substring(0, Math.max(0, disponible));
        }
        return prefijo + texto + sufijo;
    }

    /** Un fallo al registrar no afecta a la descarga, que ya se entregó (FR-008). */
    private static void escribir(String datos, String archivo) {
        try {
            CDI.current().select(ProcesoBean.class).get().okActivityRegister(ACCION_EXPORTAR, datos);
        } catch (RuntimeException e) {
            log.error("No se pudo registrar la exportación en la bitácora ({})", archivo, e);
        }
    }
}
