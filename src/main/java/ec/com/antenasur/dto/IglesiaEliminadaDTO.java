package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Iglesia dada de baja, tal como la muestra el panel «Ver eliminadas». Se
 * construye únicamente cuando el usuario abre ese panel; la pantalla principal
 * no consulta iglesias eliminadas.
 */
@Data
@NoArgsConstructor
public class IglesiaEliminadaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String nombre;
    private String documento;
    private String comunidad;
    private String provinciaNombre;
    private String cantonNombre;
    private String ubicacionNombre;

    /** Revisión de Envers en que se dio de baja; identifica qué reactivar. */
    private Integer revisionBaja;
    private Date fechaBaja;
    private String usuarioBaja;

    /**
     * Membresías que siguen activas pese a la baja de la iglesia. Solo puede ser
     * mayor que cero en bajas anteriores a la eliminación en cascada lógica;
     * habilita la acción de regularizar.
     */
    private long membresiasColgadas;

    public boolean isRequiereRegularizacion() {
        return membresiasColgadas > 0;
    }

    /** Una baja sin revisión identificable no puede restaurarse de forma selectiva. */
    public boolean isRestaurable() {
        return revisionBaja != null;
    }
}
