package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import ec.com.antenasur.dto.PlantillaCorreoDTO;
import ec.com.antenasur.facade.tec.PlantillaCorreoFacade;
import ec.com.antenasur.model.tec.PlantillaCorreo;
import ec.com.antenasur.service.AbstractService;

@Stateless
public class PlantillaCorreoService extends AbstractService<PlantillaCorreo, Integer, PlantillaCorreoFacade> {

    @Inject
    private PlantillaCorreoFacade plantillaCorreoFacade;

    @Override
    protected PlantillaCorreoFacade getFacade() {
        return plantillaCorreoFacade;
    }

    public PlantillaCorreo buscarPorAsunto(String asunto) {
        return plantillaCorreoFacade.buscarPorAsunto(asunto);
    }

    public PlantillaCorreoDTO obtenerDTOPorId(Integer id) {
        if (id == null) return null;
        return PlantillaCorreoDTO.fromEntity(plantillaCorreoFacade.find(id));
    }

    public List<PlantillaCorreoDTO> listarDTOs() {
        List<PlantillaCorreoDTO> resultado = new ArrayList<>();
        List<PlantillaCorreo> ps = plantillaCorreoFacade.findAll();
        if (ps == null) return resultado;
        for (PlantillaCorreo p : ps) resultado.add(PlantillaCorreoDTO.fromEntity(p));
        return resultado;
    }

    public PlantillaCorreoDTO buscarDTOPorAsunto(String asunto) {
        return PlantillaCorreoDTO.fromEntity(plantillaCorreoFacade.buscarPorAsunto(asunto));
    }

    public PlantillaCorreoDTO guardarDesdeDTO(PlantillaCorreoDTO dto) {
        if (dto == null) return null;
        if (dto.getId() == null) {
            return PlantillaCorreoDTO.fromEntity(plantillaCorreoFacade.create(dto.toEntity()));
        }
        PlantillaCorreo actual = plantillaCorreoFacade.find(dto.getId());
        if (actual == null) return null;
        actual.setAsunto(dto.getAsunto());
        actual.setMensaje(dto.getMensaje());
        actual.setDescripcion(dto.getDescripcion());
        return PlantillaCorreoDTO.fromEntity(plantillaCorreoFacade.edit(actual));
    }

    public PlantillaCorreoDTO eliminarPorId(Integer id) {
        if (id == null) return null;
        PlantillaCorreo p = plantillaCorreoFacade.find(id);
        if (p == null) return null;
        return PlantillaCorreoDTO.fromEntity(plantillaCorreoFacade.delete(p));
    }
}
