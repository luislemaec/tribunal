package ec.com.antenasur.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;

/** Proyeccion liviana de documentos y designacion JRV por mesa. */
@Data
@AllArgsConstructor
public class MesaDocumentosDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer mesaId;
    private String mesa;
    private Integer recintoId;
    private String recinto;
    private Integer cantonId;
    private String canton;
    private Integer parroquiaId;
    private String parroquia;
    private String estadoMesa;
    private Integer padronDocumentoId;
    private Integer actaDocumentoId;
    private Integer certificadosDocumentoId;
    private Integer actaFisicaDocumentoId;
    private Long miembrosJrv;
    private final java.util.Map<String, EstadoDocumentoMesaDTO> estados = new java.util.HashMap<>();

    public boolean isPadronGenerado() { return padronDocumentoId != null; }
    public boolean isActaGenerada() { return actaDocumentoId != null; }
    public boolean isCertificadosGenerados() { return certificadosDocumentoId != null; }
    public boolean isActaFisicaCargada() { return actaFisicaDocumentoId != null; }
    public boolean isMjrvRegistrado() {
        var estado = estados.get("DESIGNACION_MJRV");
        return estado != null && estado.isPuedeVisualizar();
    }
    /** Los cinco requisitos documentales que se controlan por mesa y proceso. */
    public int getDocumentosGenerados() {
        return (int) estados.values().stream().filter(EstadoDocumentoMesaDTO::isPuedeVisualizar).count();
    }
    public String getResumenDocumental() { return getDocumentosGenerados() + "/5"; }
    public String getEstadoDocumental() {
        int documentosGenerados = getDocumentosGenerados();
        return documentosGenerados == 5 ? "COMPLETO"
                : documentosGenerados == 4 ? "AVANZADO"
                : documentosGenerados == 0 ? "PENDIENTE" : "PARCIAL";
    }

    /** Severidad PrimeFaces derivada del estado documental, no de la vista. */
    public String getSeveridadEstadoDocumental() {
        return switch (getEstadoDocumental()) {
            case "COMPLETO" -> "success";
            case "AVANZADO" -> "info";
            default -> "warning";
        };
    }
}
