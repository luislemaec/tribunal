package ec.com.antenasur.itext;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

import org.apache.poi.ss.usermodel.Row;

import ec.com.antenasur.dto.AdministradorIglesiaDTO;
import ec.com.antenasur.dto.ConteoAdministradoresDTO;
import ec.com.antenasur.util.Constantes;

/**
 * Excel de la pestaña Administradores de Rep. Registros: solo datos (indicadores, tablas y
 * listados), sin gráficos. Recibe los mismos objetos que ya calculó
 * {@code ReporteAdministradoresController} con los filtros vigentes (cantón y días de
 * inactividad): no consulta la base ni recalcula indicadores. Formato y distribución:
 * {@link ReporteTabularXLSX}.
 */
public final class ReporteAdministradoresXLSX {

    private static final int FILA = ReporteTabularXLSX.FILA_CABECERA;

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
        try (ReporteTabularXLSX libro = new ReporteTabularXLSX(m("titulo"), logo,
                m("filtros", datos.ambito(), datos.diasInactividad()),
                m("generado", datos.generado(), datos.responsable()))) {
            resumen(libro, datos);
            avance(libro, datos);
            List<AdministradorIglesiaDTO> todas = datos.iglesias();
            iglesias(libro, "hoja.iglesias", todas, i -> true);
            iglesias(libro, "hoja.sinAdministrador", todas, i -> !i.isTieneAdministrador());
            iglesias(libro, "hoja.pendientes", todas, i -> i.isTieneAdministrador() && !i.isAdministradorActivado());
            iglesias(libro, "hoja.activados", todas, AdministradorIglesiaDTO::isAdministradorActivado);
            iglesias(libro, "hoja.sinAcceso", todas, AdministradorIglesiaDTO::isSinAccesoReciente);
            return libro.bytes();
        }
    }

    private static void resumen(ReporteTabularXLSX libro, Datos d) {
        ReporteTabularXLSX.Hoja h = libro.hoja(m("hoja.resumen"));
        // Col. 0: indicador o cantón/parroquia; col. 2: valor numérico o detalle del indicador.
        h.limites(0, 16, 40).limites(2, 12, 34);
        ConteoAdministradoresDTO t = d.totales();

        int fila = FILA;
        h.cabecera(fila++, m("col.indicador"), m("col.valor"), m("col.detalle"));
        fila = indicador(h, fila, txt("reporteIglesias.kpi.iglesias"), t.getIglesias(), d.ambito());
        fila = indicador(h, fila, txt("reporteIglesias.serie.conAdministrador"), t.getConAdministrador(),
                m("detalle.porcentaje", t.getPorcentajeConAdministrador(), t.getIglesias()));
        fila = indicador(h, fila, txt("reporteIglesias.serie.sinAdministrador"), t.getSinAdministrador(), d.ambito());
        fila = indicador(h, fila, txt("reporteAdministradores.serie.activados"), t.getActivados(), "");
        fila = indicador(h, fila, txt("reporteAdministradores.serie.pendientes"), t.getPendientesActivar(),
                txt("reporteAdministradores.kpi.pendientes.ayuda"));
        fila = indicador(h, fila, txt("reporteAdministradores.kpi.sinAcceso"), t.getSinAccesoReciente(),
                txt("reporteAdministradores.kpi.sinAcceso.ayuda") + " " + d.diasInactividad() + " "
                        + txt("reporteAdministradores.dias"));

        // Iglesias con y sin administrador por cantón o parroquia (la tabla de la web).
        fila += 1;
        h.seccion(fila++, txt("reporteIglesias.grafico.administrador"));
        h.cabecera(fila++, d.columnaCategoria(), txt("reporteIglesias.kpi.iglesias"),
                txt("reporteIglesias.serie.conAdministrador"), m("col.porcentajeCon"),
                txt("reporteIglesias.serie.sinAdministrador"), txt("reporteAdministradores.serie.activados"),
                txt("reporteAdministradores.serie.pendientes"), txt("reporteAdministradores.kpi.sinAcceso"));
        for (ConteoAdministradoresDTO c : d.categorias()) {
            Row r = h.fila(fila++);
            h.texto(r, 0, c.getEtiqueta());
            h.numero(r, 1, c.getIglesias());
            h.numero(r, 2, c.getConAdministrador());
            h.porcentaje(r, 3, c.getPorcentajeConAdministrador());
            h.numero(r, 4, c.getSinAdministrador());
            h.numero(r, 5, c.getActivados());
            h.numero(r, 6, c.getPendientesActivar());
            h.numero(r, 7, c.getSinAccesoReciente());
        }
        if (d.categorias().isEmpty()) {
            h.nota(fila++, txt("reporteIglesias.sinDatos"));
        }

        // Activación: solo cantones o parroquias con administrador, como en la web.
        List<ConteoAdministradoresDTO> conAdmin = d.categorias().stream().filter(c -> c.getConAdministrador() > 0)
                .toList();
        fila += 1;
        h.seccion(fila++, txt("reporteAdministradores.grafico.activacion"));
        h.cabecera(fila++, d.columnaCategoria(), txt("reporteAdministradores.serie.activados"),
                txt("reporteAdministradores.serie.pendientes"));
        for (ConteoAdministradoresDTO c : conAdmin) {
            Row r = h.fila(fila++);
            h.texto(r, 0, c.getEtiqueta());
            h.numero(r, 1, c.getActivados());
            h.numero(r, 2, c.getPendientesActivar());
        }
        if (conAdmin.isEmpty()) {
            h.nota(fila, txt("reporteAdministradores.sinAdministradores"));
        }
        h.cerrar();
    }

    private static void avance(ReporteTabularXLSX libro, Datos d) {
        ReporteTabularXLSX.Hoja h = libro.hoja(m("hoja.avance"));
        h.limites(0, 16, 30);
        int fila = FILA;
        h.cabecera(fila++, m("col.grupo"), txt("reporteAdministradores.serie.porcentajeHabilitados"),
                txt("reporteAdministradores.serie.porcentajeRevisados"));
        String[] grupos = { txt("reporteIglesias.serie.conAdministrador"), txt("reporteIglesias.serie.sinAdministrador") };
        for (int g = 0; g < grupos.length; g++) {
            Row r = h.fila(fila++);
            h.texto(r, 0, grupos[g]);
            h.porcentaje(r, 1, d.avance()[g][0]);
            h.porcentaje(r, 2, d.avance()[g][1]);
        }
        h.nota(fila + 1, txt("reporteAdministradores.grafico.avance.ayuda"));
        h.cerrar();
    }

    private static void iglesias(ReporteTabularXLSX libro, String claveHoja, List<AdministradorIglesiaDTO> todas,
            Predicate<AdministradorIglesiaDTO> incluir) {
        ReporteTabularXLSX.Hoja h = libro.hoja(m(claveHoja));
        // Mínimo y máximo de cada columna, en caracteres: el máximo deja los nombres en dos líneas
        // como mucho; ubicación, estado y fechas caben en una.
        h.limites(0, 22, 45).limites(1, 16, 32).limites(2, 14, 28).limites(3, 12, 24).limites(4, 20, 40)
                .limites(5, 12, 26).limites(6, 18, 38).limites(7, 14, 24).limites(8, 16, 18).limites(9, 10, 14);
        String[] columnas = { txt("reporteIglesias.col.iglesia"), txt("form.iglesias.label.comunidad"),
                txt("reporteRegistros.col.parroquia"), txt("reporteRegistros.col.canton"),
                txt("reporteIglesias.col.administrador"), m("col.usuario"), m("col.correo"),
                txt("reporteAdministradores.col.estado"), txt("reporteAdministradores.col.ultimoAcceso"),
                txt("reporteAdministradores.kpi.sinAcceso"), txt("reporteRegistros.serie.habilitados"), m("col.registrados") };
        h.cabecera(FILA, columnas);
        int fila = FILA + 1;
        for (AdministradorIglesiaDTO i : todas) {
            if (!incluir.test(i)) {
                continue;
            }
            Row r = h.fila(fila++);
            h.texto(r, 0, i.getIglesia());
            h.texto(r, 1, i.getComunidad());
            h.texto(r, 2, i.getParroquia());
            h.texto(r, 3, i.getCanton());
            boolean tiene = i.isTieneAdministrador();
            h.texto(r, 4, tiene ? i.getAdministradorNombre() : "");
            h.texto(r, 5, tiene ? i.getAdministradorUsuario() : "");
            h.texto(r, 6, !tiene ? "" : (i.getAdministradorCorreo() == null || i.getAdministradorCorreo().isBlank()
                    ? txt("reporteAdministradores.sinCorreo") : i.getAdministradorCorreo()));
            h.centrado(r, 7, estado(i));
            if (tiene && i.getUltimoAcceso() != null) {
                h.fecha(r, 8, i.getUltimoAcceso());
            } else {
                h.centrado(r, 8, tiene ? txt("reporteAdministradores.nunca") : "");
            }
            h.centrado(r, 9, tiene ? m(i.isSinAccesoReciente() ? "si" : "no") : "");
            h.numero(r, 10, i.getHabilitados());
            h.numero(r, 11, i.getRegistrados());
        }
        if (fila == FILA + 1) {
            h.nota(fila, m("sinRegistros"));
        }
        h.autofiltro(FILA, fila - 1, columnas.length);
        h.cerrar();
    }

    private static String estado(AdministradorIglesiaDTO i) {
        if (!i.isTieneAdministrador()) {
            return txt("reporteIglesias.serie.sinAdministrador");
        }
        return txt(i.isAdministradorActivado() ? "reporteAdministradores.estado.activado"
                : "reporteAdministradores.estado.pendiente");
    }

    private static int indicador(ReporteTabularXLSX.Hoja h, int fila, String nombre, long valor, String detalle) {
        Row r = h.fila(fila);
        h.negrita(r, 0, nombre);
        h.numero(r, 1, valor);
        h.detalle(r, 2, detalle);
        return fila + 1;
    }

    /** Texto propio del Excel (reporteAdministradores.excel.*). */
    private static String m(String clave, Object... argumentos) {
        return Constantes.getMensaje("reporteAdministradores.excel." + clave, argumentos);
    }

    /** Texto ya usado por la pestaña en la web. */
    private static String txt(String clave) {
        return Constantes.getMensaje(clave);
    }
}
