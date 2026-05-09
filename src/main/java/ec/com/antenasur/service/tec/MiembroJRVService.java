package ec.com.antenasur.service.tec;

import java.util.Set;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

import ec.com.antenasur.dto.MiembroJRVDTO;
import ec.com.antenasur.facade.tec.MiembroJRVFacade;
import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.model.tec.MiembroJRV;
import ec.com.antenasur.service.AbstractService;

@Stateless
public class MiembroJRVService extends AbstractService<MiembroJRV, Integer, MiembroJRVFacade> {

    @Inject
    private MiembroJRVFacade miembroJRVFacade;

    @Override
    protected MiembroJRVFacade getFacade() {
        return miembroJRVFacade;
    }

    public Set<MiembroJRV> getJRVPorMesa(Mesa mesa) {
        return miembroJRVFacade.getJRVPorMesa(mesa);
    }

    public MiembroJRVDTO obtenerDTOPorId(Integer id) {
        if (id == null) return null;
        return MiembroJRVDTO.fromEntity(miembroJRVFacade.find(id));
    }

    public List<MiembroJRVDTO> listarDTOs() {
        List<MiembroJRVDTO> resultado = new ArrayList<>();
        List<MiembroJRV> ms = miembroJRVFacade.findAll();
        if (ms == null) return resultado;
        for (MiembroJRV m : ms) resultado.add(MiembroJRVDTO.fromEntity(m));
        return resultado;
    }

    public MiembroJRVDTO eliminarPorId(Integer id) {
        if (id == null) return null;
        MiembroJRV m = miembroJRVFacade.find(id);
        if (m == null) return null;
        return MiembroJRVDTO.fromEntity(miembroJRVFacade.delete(m));
    }
}
