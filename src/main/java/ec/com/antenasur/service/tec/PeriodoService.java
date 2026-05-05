package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import ec.com.antenasur.dto.PeriodoDTO;
import ec.com.antenasur.facade.tec.PeriodoFacade;
import ec.com.antenasur.model.tec.Periodo;
import ec.com.antenasur.service.AbstractService;

@Stateless
public class PeriodoService extends AbstractService<Periodo, Integer, PeriodoFacade> {

    @Inject
    private PeriodoFacade periodoFacade;

    @Override
    protected PeriodoFacade getFacade() {
        return periodoFacade;
    }

    public Periodo getPeridoActivo() {
        return periodoFacade.getPeridoActivo();
    }

    public Periodo getPeriodoVigente() {
        return periodoFacade.getPeriodoVigente();
    }

    public PeriodoDTO obtenerDTOPorId(Integer id) {
        if (id == null) return null;
        return PeriodoDTO.fromEntity(periodoFacade.find(id));
    }

    public List<PeriodoDTO> listarDTOs() {
        List<PeriodoDTO> resultado = new ArrayList<>();
        List<Periodo> ps = periodoFacade.findAll();
        if (ps == null) return resultado;
        for (Periodo p : ps) resultado.add(PeriodoDTO.fromEntity(p));
        return resultado;
    }

    public PeriodoDTO obtenerDTOActivo() {
        return PeriodoDTO.fromEntity(periodoFacade.getPeridoActivo());
    }

    public PeriodoDTO obtenerDTOVigente() {
        return PeriodoDTO.fromEntity(periodoFacade.getPeriodoVigente());
    }

    public PeriodoDTO guardarDesdeDTO(PeriodoDTO dto) {
        if (dto == null) return null;
        if (dto.getId() == null) {
            return PeriodoDTO.fromEntity(periodoFacade.create(dto.toEntity()));
        }
        Periodo actual = periodoFacade.find(dto.getId());
        if (actual == null) return null;
        actual.setNombre(dto.getNombre());
        actual.setDescripcion(dto.getDescripcion());
        actual.setFechaInicio(dto.getFechaInicio());
        actual.setFechaFin(dto.getFechaFin());
        return PeriodoDTO.fromEntity(periodoFacade.edit(actual));
    }

    public PeriodoDTO eliminarPorId(Integer id) {
        if (id == null) return null;
        Periodo p = periodoFacade.find(id);
        if (p == null) return null;
        return PeriodoDTO.fromEntity(periodoFacade.delete(p));
    }
}
