package ec.com.antenasur.tec.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FichaDTO {

    private String representantelegalcargo;
    private String razonSocial;
    private String personaSociedad;
    private String numeroRuc;
    private String nombreFantasiaComercial;
    private String tipoContribuyente;
    private String representantelegalidentificacion;
    private String representantelegalnombre;
    private String itemNobre;

}
