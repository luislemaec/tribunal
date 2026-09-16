package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resultado de la revisión previa a asignar un administrador de iglesia.
 * Solo contiene datos de identificación necesarios para que la pantalla
 * explique una decisión; la validación definitiva se repite en el EJB.
 */
@Data
@NoArgsConstructor
public class DiagnosticoAsignacionAdministradorDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String cedula;
    private List<String> iglesiasAsociadas = new ArrayList<>();
    private List<String> personasDuplicadas = new ArrayList<>();

    public boolean isTieneCedulaDuplicada() {
        return personasDuplicadas != null && personasDuplicadas.size() > 1;
    }

    public boolean isRequiereReasignacion() {
        return iglesiasAsociadas != null && iglesiasAsociadas.size() > 1;
    }

    public boolean isPuedeConfirmar() {
        return !isTieneCedulaDuplicada();
    }

    public String getIglesiasAsociadasTexto() {
        return iglesiasAsociadas == null ? "" : String.join(", ", iglesiasAsociadas);
    }
}
