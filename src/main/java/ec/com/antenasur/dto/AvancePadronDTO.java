package ec.com.antenasur.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resumen ejecutivo del avance del padron de un proceso electoral. No guarda detalle
 * por iglesia: solo los agregados que alimentan las tarjetas y la barra de progreso.
 *
 * <p>Criterios, acordados con el usuario y alineados con las consultas existentes:</p>
 * <ul>
 *   <li><b>totalIglesias</b>: iglesias activas con al menos un miembro habilitado para
 *       padron, es decir las que pueden incorporarse; el 100 % es alcanzable.</li>
 *   <li><b>iglesiasAsignadas</b>: iglesias con al menos un empadronado activo en el
 *       proceso, en cualquier mesa. Es la misma regla con la que la pantalla marca una
 *       iglesia como asignada.</li>
 *   <li><b>personas</b>: empadronados activos del proceso.</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvancePadronDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private long totalIglesias;
    private long iglesiasAsignadas;
    private long personas;

    /** Iglesias que aun no tienen ningun empadronado en el proceso. */
    public long getIglesiasPendientes() {
        return Math.max(0L, totalIglesias - iglesiasAsignadas);
    }

    /** Avance en porcentaje, 0 cuando todavia no hay iglesias habilitadas. */
    public int getPorcentaje() {
        return totalIglesias <= 0 ? 0
                : (int) Math.round(iglesiasAsignadas * 100.0d / totalIglesias);
    }

    public boolean isCompleto() {
        return totalIglesias > 0 && iglesiasAsignadas >= totalIglesias;
    }
}
