package ec.com.antenasur.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OpcionPadronDTO implements Serializable {
    private Integer id;
    private String nombre;
}
