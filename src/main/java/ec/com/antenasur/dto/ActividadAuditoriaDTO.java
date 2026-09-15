package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ec.com.antenasur.model.tec.Proceso;
import lombok.Data;

/** Proyección segura y legible de una actividad funcional. */
@Data
public class ActividadAuditoriaDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Pattern MODULO_ESTRUCTURADO = Pattern.compile("MÓDULO\\s*:\\s*([^;|]+)", Pattern.CASE_INSENSITIVE);

    private Integer id;
    private Date fecha;
    private String usuario;
    private String ip;
    private String modulo;
    private String accion;
    private String resultado;
    private String detalle;

    public static ActividadAuditoriaDTO fromEntity(Proceso proceso) {
        if (proceso == null) return null;
        ActividadAuditoriaDTO dto = new ActividadAuditoriaDTO();
        dto.setId(proceso.getId());
        dto.setFecha(proceso.getFechaCrea());
        dto.setUsuario(proceso.getUsuarioCrea());
        dto.setIp(proceso.getIp());

        String actividad = proceso.getActividad() == null ? "" : proceso.getActividad().trim();
        String[] partes = actividad.split("\\|", 2);
        String resumen = partes[0].trim();
        dto.setDetalle(partes.length > 1 ? partes[1].trim() : resumen);
        dto.setAccion(resumen.isBlank() ? "SIN ACCIÓN" : resumen.split("\\s+", 2)[0]);
        dto.setModulo(resolverModulo(actividad));
        dto.setResultado(resolverResultado(actividad));
        return dto;
    }

    private static String resolverModulo(String actividad) {
        Matcher coincidencia = MODULO_ESTRUCTURADO.matcher(actividad);
        if (coincidencia.find()) return coincidencia.group(1).trim();
        String texto = actividad.toUpperCase(Locale.ROOT);
        if (texto.contains("USUARIO") || texto.contains("CONTRASEÑA") || texto.contains("CLAVE")) return "SEGURIDAD";
        if (texto.contains("JRV") || texto.contains("JUNTA")) return "JRV";
        if (texto.contains("PADRÓN") || texto.contains("PADRON")) return "PADRÓN";
        if (texto.contains("ACTA") || texto.contains("ESCRUTINIO")) return "ESCRUTINIO";
        if (texto.contains("MESA") || texto.contains("RECINTO")) return "MESAS";
        if (texto.contains("IGLESIA")) return "IGLESIAS";
        if (texto.contains("DOCUMENTO") || texto.contains("REPORTE") || texto.contains("PDF")) return "DOCUMENTOS";
        if (texto.contains("INGRESA") || texto.contains("SALE") || texto.contains("LOGIN")) return "ACCESO";
        return "SISTEMA";
    }

    private static String resolverResultado(String actividad) {
        Matcher coincidencia = Pattern.compile("RESULTADO\\s*:\\s*([^;|]+)", Pattern.CASE_INSENSITIVE)
                .matcher(actividad);
        return coincidencia.find() ? coincidencia.group(1).trim() : "N/A";
    }
}
