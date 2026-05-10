package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.ProcesoElectoralDTO;
import ec.com.antenasur.facade.tec.ProcesoElectoralFacade;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.service.AbstractService;

@Stateless
public class ProcesoElectoralService extends AbstractService<ProcesoElectoral, Integer, ProcesoElectoralFacade> {

    @Inject
    private ProcesoElectoralFacade procesoElectoralFacade;

    @Override
    protected ProcesoElectoralFacade getFacade() {
        return procesoElectoralFacade;
    }

    public ProcesoElectoral getActivo() {
        return procesoElectoralFacade.getActivo();
    }

    public ProcesoElectoralDTO getActivoDTO() {
        return ProcesoElectoralDTO.fromEntity(procesoElectoralFacade.getActivo());
    }

    public List<ProcesoElectoralDTO> listarDTOs() {
        List<ProcesoElectoralDTO> r = new ArrayList<>();
        List<ProcesoElectoral> all = procesoElectoralFacade.findAll();
        if (all == null) return r;
        for (ProcesoElectoral p : all) {
            r.add(ProcesoElectoralDTO.fromEntity(p));
        }
        return r;
    }

    /**
     * Persiste el proceso desde DTO. Si {@code activo=true}, desmarca todos
     * los demás antes de persistir éste — solo uno puede estar activo a la vez.
     */
    public ProcesoElectoralDTO guardarDesdeDTO(ProcesoElectoralDTO dto) {
        if (dto == null) return null;
        if (Boolean.TRUE.equals(dto.getActivo())) {
            for (ProcesoElectoral p : procesoElectoralFacade.findAll()) {
                if (Boolean.TRUE.equals(p.getActivo())
                        && (dto.getId() == null || !dto.getId().equals(p.getId()))) {
                    p.setActivo(false);
                    procesoElectoralFacade.edit(p);
                }
            }
        }
        if (dto.getId() == null) {
            return ProcesoElectoralDTO.fromEntity(procesoElectoralFacade.create(dto.toEntity()));
        }
        ProcesoElectoral actual = procesoElectoralFacade.find(dto.getId());
        if (actual == null) return null;
        actual.setNombre(dto.getNombre());
        actual.setDescripcion(dto.getDescripcion());
        actual.setFechaInicio(dto.getFechaInicio());
        actual.setFechaFin(dto.getFechaFin());
        actual.setActivo(dto.getActivo());
        return ProcesoElectoralDTO.fromEntity(procesoElectoralFacade.edit(actual));
    }
}
