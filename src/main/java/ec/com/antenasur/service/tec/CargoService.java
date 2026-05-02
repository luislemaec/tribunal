package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import ec.com.antenasur.dto.CargoDTO;
import ec.com.antenasur.facade.tec.CargoFacade;
import ec.com.antenasur.model.tec.Cargo;
import ec.com.antenasur.service.AbstractService;

@Stateless
public class CargoService extends AbstractService<Cargo, Integer, CargoFacade> {

    @Inject
    private CargoFacade cargoFacade;

    @Override
    protected CargoFacade getFacade() {
        return cargoFacade;
    }

    public CargoDTO obtenerDTOPorId(Integer id) {
        if (id == null) return null;
        return CargoDTO.fromEntity(cargoFacade.find(id));
    }

    public List<CargoDTO> listarDTOs() {
        List<CargoDTO> resultado = new ArrayList<>();
        List<Cargo> cs = cargoFacade.findAll();
        if (cs == null) return resultado;
        for (Cargo c : cs) resultado.add(CargoDTO.fromEntity(c));
        return resultado;
    }

    public CargoDTO guardarDesdeDTO(CargoDTO dto) {
        if (dto == null) return null;
        if (dto.getId() == null) {
            return CargoDTO.fromEntity(cargoFacade.create(dto.toEntity()));
        }
        Cargo actual = cargoFacade.find(dto.getId());
        if (actual == null) return null;
        actual.setNombre(dto.getNombre());
        return CargoDTO.fromEntity(cargoFacade.edit(actual));
    }

    public CargoDTO eliminarPorId(Integer id) {
        if (id == null) return null;
        Cargo c = cargoFacade.find(id);
        if (c == null) return null;
        return CargoDTO.fromEntity(cargoFacade.delete(c));
    }
}
