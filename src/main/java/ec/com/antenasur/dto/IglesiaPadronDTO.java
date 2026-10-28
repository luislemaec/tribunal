package ec.com.antenasur.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IglesiaPadronDTO implements Serializable {
    private Integer id;
    private String nombre;
    /** Comunidad / barrio de la iglesia (Iglesia.comunidad, igl_comunidad_barrio), como en Iglesias. */
    private String comunidad;
    private Long total;
}
