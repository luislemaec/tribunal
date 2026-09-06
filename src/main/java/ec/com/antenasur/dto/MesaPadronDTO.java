package ec.com.antenasur.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MesaPadronDTO implements Serializable {
    private Integer id;
    private String nombre;
    private Integer recintoId;
    private String recinto;
    private Long total;
}
