package ec.com.antenasur.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FilaPadronDTO implements Serializable {
    private Integer id, iglesiaPersonaId, personaId;
    private String documento, nombres, apellidos, iglesia;
    private Integer iglesiaId;
    private String proceso, provincia, canton, parroquia, recinto;
    private Integer recintoId;
    private String mesa;
    private Integer mesaId;
    private Boolean estado;
}
