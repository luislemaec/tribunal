package ec.com.antenasur.service.tec;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.ConteoRegistrosDTO;
import ec.com.antenasur.facade.tec.ReporteRegistrosFacade;

/**
 * Rep. Registros: personas registradas, habilitadas y pendientes de revisión
 * por cantón o por parroquia. Datos de todos los cantones: solo gestión central.
 */
@Stateless
@DeclareRoles({ "SITEC-Administrador", "SITEC-Tribunal" })
@RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal" })
public class ReporteRegistrosService {

    private static final Comparator<ConteoRegistrosDTO> MAYOR_A_MENOR = Comparator
            .comparingLong(ConteoRegistrosDTO::getHabilitados).reversed()
            .thenComparing(ConteoRegistrosDTO::getNombre);

    @Inject
    private ReporteRegistrosFacade reporteRegistrosFacade;

    public List<ConteoRegistrosDTO> porCanton() {
        List<ConteoRegistrosDTO> cantones = ordenar(reporteRegistrosFacade.contarPorCanton());
        // Un nombre de cantón puede repetirse en otra provincia: solo entonces se añade la provincia.
        Map<String, Long> repetidos = cantones.stream()
                .collect(Collectors.groupingBy(ConteoRegistrosDTO::getNombre, Collectors.counting()));
        for (ConteoRegistrosDTO canton : cantones) {
            if (repetidos.get(canton.getNombre()) > 1 && canton.getProvincia() != null) {
                canton.setEtiqueta(canton.getNombre() + " (" + canton.getProvincia() + ")");
            }
        }
        return cantones;
    }

    public List<ConteoRegistrosDTO> porParroquia(Integer cantonId) {
        return cantonId == null ? List.of() : ordenar(reporteRegistrosFacade.contarPorParroquia(cantonId));
    }

    public ConteoRegistrosDTO totales(Integer cantonId) {
        return reporteRegistrosFacade.totales(cantonId);
    }

    private static List<ConteoRegistrosDTO> ordenar(List<ConteoRegistrosDTO> filas) {
        return filas.stream().sorted(MAYOR_A_MENOR).collect(Collectors.toList());
    }
}
