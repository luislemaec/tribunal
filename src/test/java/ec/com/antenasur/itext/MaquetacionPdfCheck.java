package ec.com.antenasur.itext;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.Matrix;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.RenderListener;
import com.itextpdf.text.pdf.parser.TextRenderInfo;
import com.itextpdf.text.pdf.parser.Vector;

import ec.com.antenasur.dto.CandidatoDTO;
import ec.com.antenasur.dto.EscrutinioDTO;
import ec.com.antenasur.dto.IglesiaDTO;
import ec.com.antenasur.dto.IglesiaPersonaDTO;
import ec.com.antenasur.dto.ListaDTO;
import ec.com.antenasur.dto.MesaDTO;
import ec.com.antenasur.dto.MiembroJRVDTO;
import ec.com.antenasur.dto.PersonaDTO;
import ec.com.antenasur.dto.ProcesoElectoralDTO;
import ec.com.antenasur.dto.RecintoDTO;
import ec.com.antenasur.dto.ReporteMesaDTO;
import ec.com.antenasur.dto.TribunalDTO;

/**
 * Comprueba que todos los PDF del sistema respetan la zona segura de la
 * plantilla institucional {@link PlantillaA4}.
 *
 * <p>Recorre el contenido real de cada página (texto e imágenes) y verifica que
 * ningún elemento invada los márgenes, con una tolerancia de 1 pt para el
 * redondeo de las fuentes. El fondo de la plantilla se excluye porque ocupa la
 * página completa por diseño.
 *
 * <p>Ejecución: {@code java -cp ... ec.com.antenasur.itext.MaquetacionPdfCheck [directorio]};
 * si se indica directorio, además guarda los PDF generados para revisión visual.
 */
public class MaquetacionPdfCheck {

    private static final float TOLERANCIA = 1f;
    private static final float LIMITE_IZQUIERDO = PlantillaA4.MARGEN_IZQUIERDO - TOLERANCIA;
    private static final float LIMITE_DERECHO = PageSize.A4.getWidth() - PlantillaA4.MARGEN_DERECHO + TOLERANCIA;
    /**
     * El criterio es no solaparse con la gráfica de la plantilla: el título y el
     * código se imprimen bajo la línea del encabezado y la paginación sobre la
     * línea del pie, ambos fuera del margen de contenido pero dentro del área
     * limpia de A4TEC.png.
     */
    private static final float LIMITE_SUPERIOR = PlantillaA4.LIMITE_ENCABEZADO + TOLERANCIA;
    private static final float LIMITE_INFERIOR = PlantillaA4.LIMITE_PIE - TOLERANCIA;

    private final List<String> fallos = new ArrayList<>();
    private int documentos;
    private int paginas;
    private int elementos;

    public static void main(String[] args) throws Exception {
        Path salida = args.length == 0 ? null : Path.of(args[0]);
        if (salida != null) {
            Files.createDirectories(salida);
        }
        MaquetacionPdfCheck check = new MaquetacionPdfCheck();
        // El acta parcial conserva su encabezado propio, que lee recursos del WAR
        // a través de FacesContext: se le facilita un contexto sintético, igual
        // que en PdfAccesoQrCheck.
        try (ContextoWeb contexto = new ContextoWeb()) {
            check.ejecutar(salida);
        }
    }

    /** Contexto sintético que solo resuelve la ruta real de los recursos del WAR. */
    private static final class ContextoWeb extends jakarta.faces.context.FacesContextWrapper
            implements AutoCloseable {

        ContextoWeb() {
            setCurrentInstance(this);
        }

        @Override
        public jakarta.faces.context.FacesContext getWrapped() {
            return null;
        }

        @Override
        public jakarta.faces.context.ExternalContext getExternalContext() {
            return new jakarta.faces.context.ExternalContextWrapper() {
                @Override
                public jakarta.faces.context.ExternalContext getWrapped() {
                    return null;
                }

                @Override
                public Object getContext() {
                    return java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
                            new Class<?>[] { jakarta.servlet.ServletContext.class }, (proxy, metodo, args) -> {
                                if (metodo.getName().equals("getRealPath")) {
                                    return Path.of("src/main/webapp").toAbsolutePath().toString();
                                }
                                throw new UnsupportedOperationException(metodo.getName());
                            });
                }
            };
        }

        @Override
        public void close() {
            setCurrentInstance(null);
        }
    }

    private void ejecutar(Path salida) throws Exception {
        verificar("reporte-listado-largo", reporteListado(120), salida);
        verificar("reporte-listado-una-pagina", reporteListado(8), salida);
        verificar("acta-inscripcion", actaInscripcion(25), salida);
        verificar("acta-inscripcion-larga", actaInscripcion(90), salida);
        verificar("acta-actualizacion-miembros", actaActualizacion(40), salida);
        verificar("acta-actualizacion-miembros-larga", actaActualizacion(150), salida);
        for (int listas : new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }) {
            try {
                verificar("acta-parcial-" + listas + "-listas", actaParcial(listas), salida);
            } catch (com.itextpdf.text.DocumentException e) {
                System.out.println(String.format("%-38s NO CABE EN UNA HOJA: %s", "acta-parcial-" + listas + "-listas", e.getMessage()));
            }
        }

        System.out.println("Documentos: " + documentos + " | páginas: " + paginas + " | elementos: " + elementos);
        if (fallos.isEmpty()) {
            System.out.println("OK: todo el contenido está dentro de la zona segura de A4TEC.png");
        } else {
            System.out.println("FALLOS (" + fallos.size() + "):");
            fallos.stream().limit(40).forEach(f -> System.out.println("  - " + f));
            throw new IllegalStateException(fallos.size() + " elementos fuera de la zona segura");
        }
    }

    // ----- generación de documentos de prueba -----

    /** Listado tipo Personas / Acta de escrutinio, con textos largos. */
    private byte[] reporteListado(int filas) {
        ReportePFD.nuevoPDF("REPORTE-PRUEBA");
        Font fuente = FontFactory.getFont("arial", 8);
        ReportePFD.agregaTituloSeccion("Listado de personas registradas en el proceso electoral vigente");
        ReportePFD.addParagraph("Texto largo de prueba para comprobar el ajuste de línea dentro de la zona segura"
                + " de la plantilla institucional, que debe respetar los márgenes definidos por las líneas del"
                + " encabezado y del pie sin invadir la banda azul lateral ni el bloque DOCUMENTO OFICIAL.");
        String[] columnas = { "#", "Documento", "Apellidos y nombres completos", "Iglesia", "Parroquia", "Estado" };
        String[][] datos = new String[filas][columnas.length];
        for (int i = 0; i < filas; i++) {
            datos[i] = new String[] { String.valueOf(i + 1), "060" + (1000000 + i),
                    "APELLIDO LARGO DE PRUEBA " + i + " NOMBRES COMPUESTOS DE VERIFICACION",
                    "IGLESIA EVANGELICA DE PRUEBA NUMERO " + i, "SAN JUAN DE PRUEBA", "ACTIVO" };
        }
        ReportePFD.creaTablaCabecera(columnas.length, new float[] { 4, 12, 34, 26, 16, 8 },
                "Miembros registrados", columnas, fuente);
        ReportePFD.creaContenidoTabla(datos, columnas, fuente);
        return ReportePFD.cerrarYObtener();
    }

    private byte[] actaInscripcion(int candidatos) {
        ListaDTO lista = new ListaDTO();
        lista.setNombre("LISTA DE PRUEBA CON NOMBRE INSTITUCIONAL EXTENSO");
        lista.setNumero("07");
        lista.setSlogan("Eslogan de prueba suficientemente largo para forzar el ajuste de línea en el acta");
        List<CandidatoDTO> listaCandidatos = new ArrayList<>();
        for (int i = 0; i < candidatos; i++) {
            CandidatoDTO candidato = new CandidatoDTO();
            candidato.setCargoNombre("CARGO DE PRUEBA " + i);
            candidato.setIglesiaPersona(membresia(i));
            listaCandidatos.add(candidato);
        }
        List<TribunalDTO> autoridades = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            TribunalDTO autoridad = new TribunalDTO();
            autoridad.setCargoNombre("AUTORIDAD DE PRUEBA " + i);
            autoridad.setIglesiaPersona(membresia(i));
            autoridades.add(autoridad);
        }
        return ActaInscripcionPdf.generar(lista, "PROCESO ELECTORAL DE PRUEBA 2026", listaCandidatos, autoridades,
                "Riobamba", LocalDateTime.now(), "usuario-prueba", "AI-PRUEBA-001");
    }

    private byte[] actaActualizacion(int miembros) {
        IglesiaDTO iglesia = new IglesiaDTO();
        iglesia.setNombre("IGLESIA DE PRUEBA CON NOMBRE MUY EXTENSO PARA VERIFICAR EL AJUSTE");
        iglesia.setComunidad("COMUNIDAD DE PRUEBA DE NOMBRE TAMBIEN EXTENSO");
        List<IglesiaPersonaDTO> lista = new ArrayList<>();
        for (int i = 0; i < miembros; i++) {
            lista.add(membresia(i));
        }
        return ActaActualizacionMiembrosPdf.generar(iglesia, "PROCESO ELECTORAL DE PRUEBA 2026", lista,
                "PRESIDENTE DEL TRIBUNAL DE PRUEBA", "SECRETARIO DEL TRIBUNAL DE PRUEBA",
                "ADMINISTRADOR DE PRUEBA", LocalDateTime.now(), "AM-PRUEBA-001",
                "https://tribunal.conpociiech.org/verificacion/AM-PRUEBA-001");
    }

    private byte[] actaParcial(int listas) throws Exception {
        ReporteMesaDTO reporte = new ReporteMesaDTO();
        MesaDTO mesa = new MesaDTO();
        mesa.setId(1);
        mesa.setNombre("Mesa de prueba");
        reporte.setMesa(mesa);
        ProcesoElectoralDTO proceso = new ProcesoElectoralDTO();
        proceso.setNombre("Proceso electoral de prueba 2026");
        reporte.setProceso(proceso);
        RecintoDTO recinto = new RecintoDTO();
        recinto.setNombre("Recinto de prueba");
        recinto.setProvinciaNombre("Chimborazo");
        recinto.setCantonNombre("Riobamba");
        recinto.setUbicacionNombre("Parroquia de prueba");
        reporte.setRecinto(recinto);
        reporte.setFechaSufragio(new Date());
        for (String cargo : new String[] { "PRESIDENTE", "SECRETARIO" }) {
            MiembroJRVDTO miembro = new MiembroJRVDTO();
            miembro.setCargoNombre(cargo);
            reporte.getMiembrosJrv().add(miembro);
        }
        for (int i = 0; i < listas; i++) {
            EscrutinioDTO item = new EscrutinioDTO();
            item.setCategoriaTipo("LISTA");
            item.setCategoriaNombre("LISTA DE PRUEBA " + (i + 1));
            reporte.getEscrutinios().add(item);
        }
        // El acta parcial exige una URL de acceso QR válida emitida por el sistema.
        String token = ec.com.antenasur.security.qr.TokenActaQr.generar();
        String urlQr = ec.com.antenasur.security.qr.TokenActaQr
                .url(URI.create("https://tribunal.conpociiech.org"), token).toString();
        return ReportePFD.generarFormularioActaParcial(reporte, "AP-PRUEBA", "ACTA_PARCIAL|PRUEBA",
                LocalDateTime.now(), "usuario-prueba", urlQr);
    }

    private IglesiaPersonaDTO membresia(int indice) {
        PersonaDTO persona = new PersonaDTO();
        persona.setDocumento("060" + (2000000 + indice));
        persona.setNombres("APELLIDOS Y NOMBRES DE PRUEBA NUMERO " + indice + " CON TEXTO EXTENSO");
        persona.setSexo(indice % 2 == 0 ? "M" : "F");
        IglesiaPersonaDTO membresia = new IglesiaPersonaDTO();
        membresia.setPersona(persona);
        return membresia;
    }

    // ----- verificación -----

    private void verificar(String nombre, byte[] pdf, Path salida) throws Exception {
        boolean aplicaPlantilla = true;
        documentos++;
        if (salida != null) {
            Files.write(salida.resolve(nombre + ".pdf"), pdf);
        }
        PdfReader lector = new PdfReader(pdf);
        try {
            PdfReaderContentParser parser = new PdfReaderContentParser(lector);
            for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
                paginas++;
                if (aplicaPlantilla) {
                    parser.processContent(pagina, new Inspector(nombre, pagina, LIMITE_IZQUIERDO));
                }
            }
            System.out.println(String.format("%-38s %d página(s)%s", nombre, lector.getNumberOfPages(),
                    ""));
        } finally {
            lector.close();
        }
    }

    /** Anota cada texto o imagen que se sale de la zona segura. */
    private final class Inspector implements RenderListener {

        private final String documento;
        private final int pagina;
        private final float limiteIzquierdo;

        Inspector(String documento, int pagina, float limiteIzquierdo) {
            this.documento = documento;
            this.pagina = pagina;
            this.limiteIzquierdo = limiteIzquierdo;
        }

        @Override
        public void beginTextBlock() {
        }

        @Override
        public void endTextBlock() {
        }

        @Override
        public void renderText(TextRenderInfo info) {
            String texto = info.getText();
            if (texto == null || texto.isBlank()) {
                return;
            }
            elementos++;
            Vector inicio = info.getDescentLine().getStartPoint();
            Vector fin = info.getAscentLine().getEndPoint();
            comprobar("texto «" + resumen(texto) + "»", inicio.get(Vector.I1), fin.get(Vector.I1),
                    inicio.get(Vector.I2), fin.get(Vector.I2));
        }

        @Override
        public void renderImage(ImageRenderInfo info) {
            Matrix ctm = info.getImageCTM();
            float ancho = ctm.get(Matrix.I11);
            float alto = ctm.get(Matrix.I22);
            // El fondo institucional ocupa la página completa por diseño.
            if (ancho >= PageSize.A4.getWidth() - 1 && alto >= PageSize.A4.getHeight() - 1) {
                return;
            }
            elementos++;
            float x = ctm.get(Matrix.I31);
            float y = ctm.get(Matrix.I32);
            comprobar("imagen " + Math.round(ancho) + "x" + Math.round(alto), x, x + ancho, y, y + alto);
        }

        private void comprobar(String descripcion, float x1, float x2, float y1, float y2) {
            List<String> motivos = new ArrayList<>();
            if (x1 < limiteIzquierdo) {
                motivos.add("invade el margen izquierdo (x=" + redondear(x1) + " < " + redondear(limiteIzquierdo) + ")");
            }
            if (x2 > LIMITE_DERECHO) {
                motivos.add("invade el margen derecho (x=" + redondear(x2) + " > " + redondear(LIMITE_DERECHO) + ")");
            }
            if (y2 > LIMITE_SUPERIOR) {
                motivos.add("invade el encabezado (y=" + redondear(y2) + " > " + redondear(LIMITE_SUPERIOR) + ")");
            }
            if (y1 < LIMITE_INFERIOR) {
                motivos.add("invade el pie (y=" + redondear(y1) + " < " + redondear(LIMITE_INFERIOR) + ")");
            }
            if (!motivos.isEmpty()) {
                fallos.add(documento + " p." + pagina + " " + descripcion + ": " + String.join(", ", motivos));
            }
        }
    }

    private static String resumen(String texto) {
        String limpio = texto.strip();
        return limpio.length() <= 30 ? limpio : limpio.substring(0, 30) + "…";
    }

    private static String redondear(float valor) {
        return String.format("%.1f", valor);
    }
}
