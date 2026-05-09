package ec.com.antenasur.service;

import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.AccessAuditoryDTO;
import ec.com.antenasur.facade.AccessFacade;
import ec.com.antenasur.model.AccessAuditory;

/**
 * {@link AccessAuditory} no hereda de {@code EntidadBase} (no se aplica el
 * filtro de soft-delete), por lo que este service no extiende
 * {@link AbstractService}. Expone explÃ­citamente las operaciones que los
 * consumidores usan.
 */
@Stateless
public class AccessService {

    @Inject
    private AccessFacade accessFacade;

    public AccessAuditory create(AccessAuditory entity) {
        return accessFacade.create(entity);
    }

    public AccessAuditory edit(AccessAuditory entity) {
        return accessFacade.edit(entity);
    }

    public AccessAuditory find(Integer id) {
        return accessFacade.find(id);
    }

    public List<AccessAuditory> findAll() {
        return accessFacade.findAll();
    }

    public int count() {
        return accessFacade.count();
    }

    public AccessAuditory findBySession(String session) {
        return accessFacade.findBySession(session);
    }

    public List<AccessAuditory> findAllOrderByIdDesc() {
        return accessFacade.findAllOrderByIdDesc();
    }

    public AccessAuditoryDTO obtenerDTOPorId(Integer id) {
        if (id == null) return null;
        return AccessAuditoryDTO.fromEntity(accessFacade.find(id));
    }

    public AccessAuditoryDTO buscarDTOPorSesion(String session) {
        return AccessAuditoryDTO.fromEntity(accessFacade.findBySession(session));
    }

    public List<AccessAuditoryDTO> listarDTOsOrdenadoDesc() {
        List<AccessAuditoryDTO> resultado = new ArrayList<>();
        List<AccessAuditory> entidades = accessFacade.findAllOrderByIdDesc();
        if (entidades == null) return resultado;
        for (AccessAuditory a : entidades) resultado.add(AccessAuditoryDTO.fromEntity(a));
        return resultado;
    }
}
