package ec.com.antenasur.service.tec;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import java.util.ArrayList;

import ec.com.antenasur.dto.VwTotalVotosDTO;
import ec.com.antenasur.facade.tec.VwTotalVotosFacade;
import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.model.tec.Recinto;
import ec.com.antenasur.model.tec.VwTotalVotos;

@Stateless
public class VwTotalVotosService {

    @Inject
    private VwTotalVotosFacade vwTotalVotosFacade;

    public VwTotalVotos find(Integer id) {
        return vwTotalVotosFacade.find(id);
    }

    public List<VwTotalVotos> findAll() {
        return vwTotalVotosFacade.findAll();
    }

    public int count() {
        return vwTotalVotosFacade.count();
    }

    public List<VwTotalVotos> buscaPorMesa(VwTotalVotos mesa) {
        return vwTotalVotosFacade.buscaPorMesa(mesa);
    }

    public List<VwTotalVotos> buscaCanton(Mesa mesa) {
        return vwTotalVotosFacade.buscaCanton(mesa);
    }

    public List<Object[]> sumaGlobal() {
        return vwTotalVotosFacade.sumaGlobal();
    }

    public List<Object[]> votosPorRecinto(Recinto recintoSeleccionado) {
        return vwTotalVotosFacade.votosPorRecinto(recintoSeleccionado);
    }

    public List<Object[]> votosPorParroquias(List<Geograp> parroquias) {
        return vwTotalVotosFacade.votosPorParroquias(parroquias);
    }

    public List<Object[]> votosPorMesa(Mesa mesa) {
        return vwTotalVotosFacade.votosPorMesa(mesa);
    }

    public List<Object[]> votosPorRecintos(List<Recinto> recintos) {
        return vwTotalVotosFacade.votosPorRecintos(recintos);
    }

    public List<Object[]> votosPorMesas(List<Mesa> mesas) {
        return vwTotalVotosFacade.votosPorMesas(mesas);
    }

    public VwTotalVotosDTO obtenerDTOPorId(Integer id) {
        if (id == null) return null;
        return VwTotalVotosDTO.fromEntity(vwTotalVotosFacade.find(id));
    }

    public List<VwTotalVotosDTO> listarDTOs() {
        return mapearLista(vwTotalVotosFacade.findAll());
    }

    private List<VwTotalVotosDTO> mapearLista(List<VwTotalVotos> entidades) {
        List<VwTotalVotosDTO> resultado = new ArrayList<>();
        if (entidades == null) return resultado;
        for (VwTotalVotos v : entidades) resultado.add(VwTotalVotosDTO.fromEntity(v));
        return resultado;
    }
}
