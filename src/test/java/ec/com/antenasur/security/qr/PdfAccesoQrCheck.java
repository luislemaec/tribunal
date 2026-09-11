package ec.com.antenasur.security.qr;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Date;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import ec.com.antenasur.dto.*;
import ec.com.antenasur.itext.ReportePFD;
import static org.junit.jupiter.api.Assertions.*;

public class PdfAccesoQrCheck {
    public static void main(String[] args) throws Exception {
        try (var contexto = new ContextoPdf()) {
            new PdfAccesoQrCheck().comprobar(args.length == 0 ? null : java.nio.file.Path.of(args[0]));
        }
    }
    void comprobar(java.nio.file.Path salida) throws Exception {
        if (salida != null) java.nio.file.Files.createDirectories(salida);
        for (int listas : new int[]{2, 5, 8, 9}) {
            var reporte = new ReporteMesaDTO();
            var mesa = new MesaDTO(); mesa.setId(1); mesa.setNombre("Mesa de prueba"); reporte.setMesa(mesa);
            var proceso = new ProcesoElectoralDTO(); proceso.setNombre("Proceso de prueba"); reporte.setProceso(proceso);
            var recinto = new RecintoDTO(); recinto.setNombre("Recinto de prueba"); recinto.setProvinciaNombre("Chimborazo");
            recinto.setCantonNombre("Riobamba"); recinto.setUbicacionNombre("Parroquia de prueba"); reporte.setRecinto(recinto);
            reporte.setFechaSufragio(new Date());
            for (String cargo : new String[]{"PRESIDENTE", "SECRETARIO"}) {
                var miembro = new MiembroJRVDTO(); miembro.setCargoNombre(cargo);
                reporte.getMiembrosJrv().add(miembro);
            }
            for (int i = 0; i < listas; i++) {
                var item = new EscrutinioDTO(); item.setCategoriaTipo("LISTA"); item.setCategoriaNombre("LISTA " + (i + 1));
                reporte.getEscrutinios().add(item);
            }
            assertThrows(com.itextpdf.text.DocumentException.class, () -> ReportePFD.generarFormularioActaParcial(
                    reporte, "PRUEBA", "PRUEBA", LocalDateTime.now(), "usuario-prueba", null));
            for (int version = 1; version <= 2; version++) {
            String token = TokenActaQr.generar();
            byte[] pdf = ReportePFD.generarFormularioActaParcial(reporte, "AP-PRUEBA", "ACTA_PARCIAL|PRUEBA",
                    LocalDateTime.now(), "usuario-prueba", TokenActaQr.url(URI.create("https://tribunal.conpociiech.org"), token).toString());
            var reader = new PdfReader(pdf);
            try {
                assertEquals(1, reader.getNumberOfPages(), "Acta con " + listas + " listas");
                String texto = PdfTextExtractor.getTextFromPage(reader, 1);
                assertTrue(texto.contains("Acceso del Presidente"));
                assertFalse(texto.contains(token));
            } finally { reader.close(); }
            if (salida != null) {
                String nombre = "acta-" + listas + "-listas-v" + version;
                java.nio.file.Files.write(salida.resolve(nombre + ".pdf"), pdf);
                java.nio.file.Files.writeString(salida.resolve(nombre + ".sha256-token"), TokenActaQr.hash(token));
            }
            }
        }
        System.out.println("OK: Acta Parcial con QR, 2/5/8/9 listas, una pagina y token sin texto visible");
    }

    /** Contexto sintetico de prueba: solo permite leer los recursos reales del WAR. */
    private static final class ContextoPdf extends jakarta.faces.context.FacesContextWrapper implements AutoCloseable {
        ContextoPdf() { setCurrentInstance(this); }
        @Override public jakarta.faces.context.FacesContext getWrapped() { return null; }
        @Override public jakarta.faces.context.ExternalContext getExternalContext() {
            return new jakarta.faces.context.ExternalContextWrapper() {
                @Override public jakarta.faces.context.ExternalContext getWrapped() { return null; }
                @Override public Object getContext() {
                    return java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
                            new Class<?>[]{jakarta.servlet.ServletContext.class}, (proxy, method, args) -> {
                                if (method.getName().equals("getRealPath"))
                                    return java.nio.file.Path.of("src/main/webapp").toAbsolutePath().toString();
                                throw new UnsupportedOperationException(method.getName());
                            });
                }
            };
        }
        @Override public void close() { setCurrentInstance(null); }
    }
}
