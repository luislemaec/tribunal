package ec.com.antenasur.service.tec;

import java.util.Comparator;
import java.util.List;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.CategoriaGeograficaDTO;
import ec.com.antenasur.dto.ConteoCoberturaDTO;
import ec.com.antenasur.dto.ProcesoElectoralDTO;
import ec.com.antenasur.facade.tec.ReporteCoberturaPadronFacade;

/**
 * Pestaña Cobertura del padrón de Rep. Registros: habilitados vs. empadronados
 * en el proceso activo. Mismos roles que la página.
 */
@Stateless
@DeclareRoles({ "SITEC-Administrador", "SITEC-Tribunal" })
@RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal" })
public class ReporteCoberturaPadronService {

    /** Primero donde más habilitados faltan por empadronar. */
    private static final Comparator<ConteoCoberturaDTO> MAS_PENDIENTES = Comparator
            .comparingLong(ConteoCoberturaDTO::getSinPadron).reversed()
            .thenComparing(Comparator.comparingLong(ConteoCoberturaDTO::getHabilitados).reversed())
            .thenComparing(ConteoCoberturaDTO::getNombre);

    @Inject
    private ReporteCoberturaPadronFacade reporteCoberturaPadronFacade;

    @Inject
    private ProcesoElectoralService procesoElectoralService;

    public ProcesoElectoralDTO procesoActivo() {
        return procesoElectoralService.getActivoDTO();
    }

    public List<ConteoCoberturaDTO> porCanton(Integer procesoId) {
        List<ConteoCoberturaDTO> cantones = reporteCoberturaPadronFacade.contarPorCanton(procesoId).stream()
                .sorted(MAS_PENDIENTES).toList();
        CategoriaGeograficaDTO.desambiguar(cantones);
        return cantones;
    }

    public List<ConteoCoberturaDTO> porParroquia(Integer procesoId, Integer cantonId) {
        return reporteCoberturaPadronFacade.contarPorParroquia(procesoId, cantonId).stream()
                .sorted(MAS_PENDIENTES).toList();
    }

    public ConteoCoberturaDTO totales(Integer procesoId, Integer cantonId) {
        return reporteCoberturaPadronFacade.totales(procesoId, cantonId);
    }
}
