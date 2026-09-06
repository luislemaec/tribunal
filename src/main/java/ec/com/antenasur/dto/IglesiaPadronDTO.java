package ec.com.antenasur.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IglesiaPadronDTO implements Serializable {
    private Integer id;
    private String nombre;
    private Long total;
}
