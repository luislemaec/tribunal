package ec.com.antenasur.itext;

import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;

import ec.com.antenasur.dto.ActividadAuditoriaDTO;
import ec.com.antenasur.dto.ConteoCoberturaDTO;
import ec.com.antenasur.dto.ConteoIglesiasDTO;
import ec.com.antenasur.dto.ConteoRegistrosDTO;
import ec.com.antenasur.dto.IglesiaAtencionDTO;
import ec.com.antenasur.dto.UsuarioEnLineaDTO;
import ec.com.antenasur.util.Constantes;

/**
 * Excel de las pestañas Registros, Cobertura del padrón, Iglesias y Actividad de Rep.
 * Registros: solo datos (indicadores, tabla por cantón o parroquia y listados), sin gráficos.
 * Cada método recibe los objetos que ya calculó el controlador de la pestaña con su filtro
 * vigente, así que el archivo coincide con lo que se ve y no hay consultas adicionales; las
 * listas paginadas en la web van completas. Formato y distribución: {@link ReporteTabularXLSX}.
 */
public final class ReporteRegistrosXLSX {

    private static final int FILA = ReporteTabularXLSX.FILA_CABECERA;

    /**
     * Datos comunes del encabezado.
     *
     * @param filtro           línea de filtros aplicados (cantón, proceso…)
     * @param columnaCategoria «Cantón» o «Parroquia», igual que la tabla de la web
     */
    public record Encabezado(String filtro, String columnaCategoria, String generado, String responsable) {
    }

    private ReporteRegistrosXLSX() {
    }

    // ------------------------------------------------------------------ Registros

    public static byte[] registros(Encabezado e, ConteoRegistrosDTO t, List<ConteoRegistrosDTO> categorias, byte[] logo)
            throws IOException {
        try (ReporteTabularXLSX libro = libro("titulo.registros", e, logo)) {
            ReporteTabularXLSX.Hoja h = libro.hoja(m("hoja.resumen"));
            h.limites(0, 16, 40).limites(2, 12, 40);
            int fila = cabeceraIndicadores(h);
            fila = indicador(h, fila, txt("reporteRegistros.kpi.registrados"), t.getRegistrados(), "");
            fila = indicador(h, fila, txt("reporteRegistros.kpi.habilitados"), t.getHabilitados(),
                    m("porcentaje", t.getPorcentajeHabilitados(), t.getRegistrados()));
            fila = porcentaje(h, fila, txt("reporteRegistros.kpi.porcentajeHabilitados"), t.getPorcentajeHabilitados());
            fila = indicador(h, fila, txt("reporteRegistros.kpi.noHabilitados"), t.getNoHabilitados(),
                    txt("reporteRegistros.kpi.noHabilitados.ayuda"));
            fila = indicador(h, fila, txt("reporteRegistros.kpi.pendientes"), t.getPendientesRevision(), "");
            fila = indicador(h, fila, txt("reporteRegistros.kpi.revisados"), t.getRevisados(), "");

            fila += 1;
            int cabecera = fila;
            h.cabecera(fila++, e.columnaCategoria(), txt("reporteRegistros.serie.habilitados"),
                    txt("reporteRegistros.serie.noHabilitados"), txt("reporteRegistros.kpi.registrados"),
                    txt("reporteRegistros.kpi.porcentajeHabilitados"), txt("reporteRegistros.serie.pendientes"),
                    txt("reporteRegistros.serie.revisados"));
            for (ConteoRegistrosDTO c : categorias) {
                Row r = h.fila(fila++);
                h.texto(r, 0, c.getEtiqueta());
                h.numero(r, 1, c.getHabilitados());
                h.numero(r, 2, c.getNoHabilitados());
                h.numero(r, 3, c.getRegistrados());
                h.porcentaje(r, 4, c.getPorcentajeHabilitados());
                h.numero(r, 5, c.getPendientesRevision());
                h.numero(r, 6, c.getRevisados());
            }
            sinFilas(h, categorias.isEmpty(), fila, "reporteRegistros.sinDatos");
            h.autofiltro(cabecera, fila - 1, 7);
            h.cerrar();
            return libro.bytes();
        }
    }

    // ------------------------------------------------------------------ Cobertura del padrón

    public static byte[] cobertura(Encabezado e, ConteoCoberturaDTO t, List<ConteoCoberturaDTO> categorias, byte[] logo)
            throws IOException {
        try (ReporteTabularXLSX libro = libro("titulo.cobertura", e, logo)) {
            ReporteTabularXLSX.Hoja h = libro.hoja(m("hoja.resumen"));
            h.limites(0, 16, 40).limites(2, 12, 40);
            int fila = cabeceraIndicadores(h);
            fila = indicador(h, fila, txt("reporteRegistros.kpi.habilitados"), t.getHabilitados(), "");
            fila = indicador(h, fila, txt("reporteCobertura.kpi.empadronados"), t.getEmpadronados(),
                    txt("reporteCobertura.kpi.empadronados.ayuda"));
            fila = indicador(h, fila, txt("reporteCobertura.kpi.sinPadron"), t.getSinPadron(),
                    txt("reporteCobertura.kpi.sinPadron.ayuda"));
            fila = porcentaje(h, fila, txt("reporteCobertura.kpi.cobertura"), t.getPorcentajeCobertura(),
                    txt("reporteCobertura.kpi.cobertura.ayuda"));

            fila += 1;
            int cabecera = fila;
            h.cabecera(fila++, e.columnaCategoria(), txt("reporteRegistros.serie.habilitados"),
                    txt("reporteCobertura.serie.empadronados"), txt("reporteCobertura.serie.sinPadron"),
                    txt("reporteCobertura.kpi.cobertura"));
            for (ConteoCoberturaDTO c : categorias) {
                Row r = h.fila(fila++);
                h.texto(r, 0, c.getEtiqueta());
                h.numero(r, 1, c.getHabilitados());
                h.numero(r, 2, c.getEmpadronados());
                h.numero(r, 3, c.getSinPadron());
                h.porcentaje(r, 4, c.getPorcentajeCobertura());
            }
            sinFilas(h, categorias.isEmpty(), fila, "reporteCobertura.sinDatos");
            h.autofiltro(cabecera, fila - 1, 5);
            h.cerrar();
            return libro.bytes();
        }
    }

    // ------------------------------------------------------------------ Iglesias

    public static byte[] iglesias(Encabezado e, ConteoIglesiasDTO t, List<ConteoIglesiasDTO> categorias,
            List<IglesiaAtencionDTO> atencion, byte[] logo) throws IOException {
        try (ReporteTabularXLSX libro = libro("titulo.iglesias", e, logo)) {
            ReporteTabularXLSX.Hoja h = libro.hoja(m("hoja.resumen"));
            h.limites(0, 16, 40).limites(2, 12, 40);
            int fila = cabeceraIndicadores(h);
            fila = indicador(h, fila, txt("reporteIglesias.kpi.iglesias"), t.getIglesias(), "");
            fila = indicador(h, fila, txt("reporteIglesias.serie.sinAdministrador"), t.getSinAdministrador(), "");
            fila = indicador(h, fila, txt("reporteIglesias.serie.conAdministrador"), t.getConAdministrador(), "");
            fila = indicador(h, fila, txt("reporteIglesias.serie.sinLista"), t.getSinListaMiembros(), "");
            fila = indicador(h, fila, txt("reporteIglesias.serie.conLista"), t.getConListaMiembros(), "");
            fila = indicador(h, fila, txt("reporteIglesias.kpi.sinHabilitados"), t.getSinHabilitados(),
                    txt("reporteIglesias.kpi.sinHabilitados.ayuda"));

            fila += 1;
            int cabecera = fila;
            h.cabecera(fila++, e.columnaCategoria(), txt("reporteIglesias.kpi.iglesias"),
                    txt("reporteIglesias.serie.sinAdministrador"), txt("reporteIglesias.serie.sinLista"),
                    txt("reporteIglesias.kpi.sinHabilitados"));
            for (ConteoIglesiasDTO c : categorias) {
                Row r = h.fila(fila++);
                h.texto(r, 0, c.getEtiqueta());
                h.numero(r, 1, c.getIglesias());
                h.numero(r, 2, c.getSinAdministrador());
                h.numero(r, 3, c.getSinListaMiembros());
                h.numero(r, 4, c.getSinHabilitados());
            }
            sinFilas(h, categorias.isEmpty(), fila, "reporteIglesias.sinDatos");
            h.autofiltro(cabecera, fila - 1, 5);
            h.cerrar();

            // Lista de acción completa (en la web va paginada).
            ReporteTabularXLSX.Hoja a = libro.hoja(m("hoja.atencion"));
            a.limites(0, 22, 45).limites(1, 16, 32).limites(2, 14, 28).limites(3, 12, 24);
            a.cabecera(FILA, txt("reporteIglesias.col.iglesia"), txt("form.iglesias.label.comunidad"),
                    txt("reporteRegistros.col.parroquia"), txt("reporteRegistros.col.canton"),
                    txt("reporteRegistros.serie.noHabilitados"), txt("reporteRegistros.serie.pendientes"),
                    txt("reporteRegistros.serie.habilitados"), txt("reporteIglesias.col.administrador"),
                    txt("reporteIglesias.col.lista"));
            int f = FILA + 1;
            for (IglesiaAtencionDTO i : atencion) {
                Row r = a.fila(f++);
                a.texto(r, 0, i.getNombre());
                a.texto(r, 1, i.getComunidad());
                a.texto(r, 2, i.getParroquia());
                a.texto(r, 3, i.getCanton());
                a.numero(r, 4, i.getNoHabilitados());
                a.numero(r, 5, i.getPendientesRevision());
                a.numero(r, 6, i.getHabilitados());
                a.centrado(r, 7, txt(i.isTieneAdministrador() ? "reporteIglesias.si" : "reporteIglesias.no"));
                a.centrado(r, 8, txt(i.isTieneListaMiembros() ? "reporteIglesias.si" : "reporteIglesias.no"));
            }
            sinFilas(a, atencion.isEmpty(), f, "reporteIglesias.atencion.vacio");
            a.autofiltro(FILA, f - 1, 9);
            a.cerrar();
            return libro.bytes();
        }
    }

    // ------------------------------------------------------------------ Actividad

    /** Foto del momento: usuarios en línea y últimos eventos, con el alcance de la pestaña. */
    public static byte[] actividad(Encabezado e, long web, long app, List<UsuarioEnLineaDTO> enLinea,
            List<ActividadAuditoriaDTO> eventos, byte[] logo) throws IOException {
        try (ReporteTabularXLSX libro = libro("titulo.actividad", e, logo)) {
            ReporteTabularXLSX.Hoja h = libro.hoja(m("hoja.resumen"));
            h.limites(0, 16, 40).limites(2, 12, 40);
            int fila = cabeceraIndicadores(h);
            fila = indicador(h, fila, txt("reporteActividad.kpi.enLinea"), enLinea.size(),
                    txt("reporteActividad.enLinea.ayuda"));
            fila = indicador(h, fila, txt("reporteActividad.web"), web, "");
            indicador(h, fila, txt("reporteActividad.app"), app, "");
            h.cerrar();

            ReporteTabularXLSX.Hoja u = libro.hoja(m("hoja.enLinea"));
            u.limites(0, 20, 40).limites(1, 12, 26).limites(2, 14, 40);
            u.cabecera(FILA, m("col.nombre"), txt("reporteActividad.col.usuario"), m("col.roles"),
                    txt("reporteActividad.col.canal"), txt("reporteActividad.col.desde"));
            int f = FILA + 1;
            for (UsuarioEnLineaDTO usuario : enLinea) {
                Row r = u.fila(f++);
                u.texto(r, 0, usuario.getNombre() == null || usuario.getNombre().isBlank() ? usuario.getUsuario()
                        : usuario.getNombre());
                u.texto(r, 1, usuario.getUsuario());
                u.texto(r, 2, usuario.getRoles());
                u.centrado(r, 3, usuario.getCanalesTexto());
                u.fecha(r, 4, usuario.getDesdeFecha());
            }
            sinFilas(u, enLinea.isEmpty(), f, "reporteActividad.enLinea.vacio");
            u.autofiltro(FILA, f - 1, 5);
            u.cerrar();

            ReporteTabularXLSX.Hoja ev = libro.hoja(m("hoja.eventos"));
            ev.limites(1, 12, 24).limites(2, 20, 45).limites(3, 16, 36).limites(4, 10, 20);
            ev.cabecera(FILA, m("col.fecha"), m("col.modulo"), m("col.accion"), txt("reporteActividad.col.usuario"),
                    m("col.resultado"));
            f = FILA + 1;
            for (ActividadAuditoriaDTO evento : eventos) {
                Row r = ev.fila(f++);
                ev.fecha(r, 0, evento.getFecha());
                ev.texto(r, 1, evento.getModulo());
                ev.texto(r, 2, evento.getAccion());
                ev.texto(r, 3, evento.getNombreUsuario() == null || evento.getNombreUsuario().isBlank()
                        ? evento.getUsuario() : evento.getNombreUsuario());
                ev.centrado(r, 4, evento.getResultado());
            }
            sinFilas(ev, eventos.isEmpty(), f, "reporteActividad.sinDatos");
            ev.autofiltro(FILA, f - 1, 5);
            ev.cerrar();
            return libro.bytes();
        }
    }

    // ------------------------------------------------------------------ Comunes

    private static ReporteTabularXLSX libro(String claveTitulo, Encabezado e, byte[] logo) {
        return new ReporteTabularXLSX(m(claveTitulo), logo, e.filtro(), m("generado", e.generado(), e.responsable()));
    }

    private static int cabeceraIndicadores(ReporteTabularXLSX.Hoja h) {
        h.cabecera(FILA, m("col.indicador"), m("col.valor"), m("col.detalle"));
        return FILA + 1;
    }

    private static int indicador(ReporteTabularXLSX.Hoja h, int fila, String nombre, long valor, String detalle) {
        Row r = h.fila(fila);
        h.negrita(r, 0, nombre);
        h.numero(r, 1, valor);
        h.detalle(r, 2, detalle);
        return fila + 1;
    }

    private static int porcentaje(ReporteTabularXLSX.Hoja h, int fila, String nombre, long valor, String... detalle) {
        Row r = h.fila(fila);
        h.negrita(r, 0, nombre);
        h.porcentaje(r, 1, valor);
        h.detalle(r, 2, detalle.length == 0 ? "" : detalle[0]);
        return fila + 1;
    }

    private static void sinFilas(ReporteTabularXLSX.Hoja h, boolean vacia, int fila, String claveMensaje) {
        if (vacia) {
            h.nota(fila, txt(claveMensaje));
        }
    }

    /** Texto propio del Excel (reporteRegistros.excel.*). */
    private static String m(String clave, Object... argumentos) {
        return Constantes.getMensaje("reporteRegistros.excel." + clave, argumentos);
    }

    /** Texto ya usado por la pestaña en la web. */
    private static String txt(String clave) {
        return Constantes.getMensaje(clave);
    }
}
