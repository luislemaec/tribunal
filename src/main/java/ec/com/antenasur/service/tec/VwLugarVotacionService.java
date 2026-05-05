package ec.com.antenasur.service.tec;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import java.util.ArrayList;

import ec.com.antenasur.dto.VwLugarVotacionDTO;
import ec.com.antenasur.facade.tec.VwLugarVotacionFacade;
import ec.com.antenasur.model.tec.VwLugarVotacion;

@Stateless
public class VwLugarVotacionService {

    @Inject
    private VwLugarVotacionFacade vwLugarVotacionFacade;

    public VwLugarVotacion find(Integer id) {
        return vwLugarVotacionFacade.find(id);
    }

    public List<VwLugarVotacion> findAll() {
        return vwLugarVotacionFacade.findAll();
    }

    public int count() {
        return vwLugarVotacionFacade.count();
    }

    public List<VwLugarVotacion> buscaLugarVotacion(String nombreCedula) {
        return vwLugarVotacionFacade.buscaLugarVotacion(nombreCedula);
    }

    public VwLugarVotacionDTO obtenerDTOPorId(Integer id) {
        if (id == null) return null;
        return VwLugarVotacionDTO.fromEntity(vwLugarVotacionFacade.find(id));
    }

    public List<VwLugarVotacionDTO> listarDTOs() {
        return mapearLista(vwLugarVotacionFacade.findAll());
    }

    public List<VwLugarVotacionDTO> buscarDTOsPorNombreOCedula(String nombreCedula) {
        return mapearLista(vwLugarVotacionFacade.buscaLugarVotacion(nombreCedula));
    }

    private List<VwLugarVotacionDTO> mapearLista(List<VwLugarVotacion> entidades) {
        List<VwLugarVotacionDTO> resultado = new ArrayList<>();
        if (entidades == null) return resultado;
        for (VwLugarVotacion v : entidades) resultado.add(VwLugarVotacionDTO.fromEntity(v));
        return resultado;
    }
}
