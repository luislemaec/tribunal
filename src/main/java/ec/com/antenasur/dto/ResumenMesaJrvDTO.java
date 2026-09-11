package ec.com.antenasur.dto;

import java.io.Serializable;
import lombok.Value;

@Value
public class ResumenMesaJrvDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    int miembrosAsignados;
    int miembrosRequeridos;
    long iglesiasAsignadas;
    boolean completa;

    public String getEstadoClave() {
        return completa ? "mjrv.mesas.estado.completado"
                : miembrosAsignados == 0 ? "mjrv.mesas.estado.pendiente" : "mjrv.mesas.estado.parcial";
    }

    public String getSeveridad() {
        return completa ? "success" : miembrosAsignados == 0 ? "secondary" : "warning";
    }

    public String getProgreso() { return miembrosAsignados + "/" + miembrosRequeridos; }
    public boolean isSinIglesias() { return iglesiasAsignadas == 0; }
}
