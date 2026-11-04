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
    /** Cantón y parroquia de la iglesia (Iglesia.ubicacion), los mismos datos que muestra Iglesias. */
    private String canton;
    private String parroquia;
    /** La iglesia está en la parroquia del recinto de la mesa; false = iglesia de otra parroquia del cantón. */
    private Boolean mismaParroquia;
    private Long total;

    public boolean isOtraParroquia() {
        return Boolean.FALSE.equals(mismaParroquia);
    }
}
