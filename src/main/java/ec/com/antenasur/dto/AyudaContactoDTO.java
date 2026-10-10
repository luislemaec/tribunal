package ec.com.antenasur.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

/** Canal oficial de contacto, para el chatbot y para su mantenimiento. */
@Getter
@Setter
public class AyudaContactoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String tipo;
    private String valor;
    private String etiqueta;
    private String horario;
    private Integer orden = 100;
    private boolean activo = true;

    /** Enlace del canal: tel:, https://wa.me/ (solo dígitos) o mailto:. */
    public String getHref() {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return switch (tipo == null ? "" : tipo) {
            case "WHATSAPP" -> "https://wa.me/" + limpio.replaceAll("\\D", "");
            case "TELEFONO" -> "tel:" + limpio.replaceAll("[^+\\d]", "");
            case "CORREO" -> "mailto:" + limpio;
            default -> null;
        };
    }

    public String getIcono() {
        return switch (tipo == null ? "" : tipo) {
            case "WHATSAPP" -> "pi pi-whatsapp";
            case "TELEFONO" -> "pi pi-phone";
            case "CORREO" -> "pi pi-envelope";
            default -> "pi pi-info-circle";
        };
    }
}
