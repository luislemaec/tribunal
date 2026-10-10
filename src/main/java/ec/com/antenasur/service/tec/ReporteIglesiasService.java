package ec.com.antenasur.service.tec;

import java.util.Comparator;
import java.util.List;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.CategoriaGeograficaDTO;
import ec.com.antenasur.dto.ConteoIglesiasDTO;
import ec.com.antenasur.dto.IglesiaAtencionDTO;
import ec.com.antenasur.facade.tec.ReporteIglesiasFacade;
import ec.com.antenasur.util.Constantes;

/**
 * Pestaña Iglesias de Rep. Registros: estado del registro de las iglesias por
 * cantón o parroquia y lista de iglesias que requieren atención. Mismos roles
 * que la página.
 */
@Stateless
@DeclareRoles({ "SITEC-Administrador", "SITEC-Tribunal" })
@RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal" })
public class ReporteIglesiasService {

    private static final Comparator<ConteoIglesiasDTO> MAS_IGLESIAS = Comparator
            .comparingLong(ConteoIglesiasDTO::getIglesias).reversed()
            .thenComparing(ConteoIglesiasDTO::getNombre);

    /** Primero las que más miembros tienen por habilitar y por revisar. */
    private static final Comparator<IglesiaAtencionDTO> MAS_PENDIENTES = Comparator
            .comparingLong(IglesiaAtencionDTO::getNoHabilitados).reversed()
            .thenComparing(Comparator.comparingLong(IglesiaAtencionDTO::getPendientesRevision).reversed())
            .thenComparing(IglesiaAtencionDTO::getNombre);

    @Inject
    private ReporteIglesiasFacade reporteIglesiasFacade;

    public List<ConteoIglesiasDTO> porCanton() {
        List<ConteoIglesiasDTO> cantones = reporteIglesiasFacade.contarPorCanton(Constantes.LISTA_MIEMBROS)
                .stream().sorted(MAS_IGLESIAS).toList();
        CategoriaGeograficaDTO.desambiguar(cantones);
        return cantones;
    }

    public List<ConteoIglesiasDTO> porParroquia(Integer cantonId) {
        return reporteIglesiasFacade.contarPorParroquia(Constantes.LISTA_MIEMBROS, cantonId).stream()
                .sorted(MAS_IGLESIAS).toList();
    }

    public ConteoIglesiasDTO totales(Integer cantonId) {
        return reporteIglesiasFacade.totales(Constantes.LISTA_MIEMBROS, cantonId);
    }

    public List<IglesiaAtencionDTO> iglesiasQueRequierenAtencion(Integer cantonId) {
        return reporteIglesiasFacade.listarIglesias(Constantes.LISTA_MIEMBROS, cantonId).stream()
                .filter(IglesiaAtencionDTO::isRequiereAtencion).sorted(MAS_PENDIENTES).toList();
    }
}
