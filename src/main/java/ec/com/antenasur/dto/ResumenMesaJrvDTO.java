package ec.com.antenasur.dto;

import java.io.Serializable;
import lombok.Value;

@Value
public class ResumenMesaJrvDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    int miembrosAsignados;
    int miembrosRequeridos;
    long iglesiasAsignadas;
    /**
     * Las cuatro dignidades están asignadas y son válidas. No basta para mostrar «Completado»: el
     * estado visible (con «Lista para completar» y la junta registrada) lo decide
     * JrvController.estadoJrv, único punto para la tabla, el filtro y la mesa seleccionada.
     */
    boolean completa;

    public String getProgreso() { return miembrosAsignados + "/" + miembrosRequeridos; }
    public boolean isSinIglesias() { return iglesiasAsignadas == 0; }
}
