package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.ListaDTO;
import ec.com.antenasur.facade.tec.ListaFacade;
import ec.com.antenasur.model.tec.Lista;
import ec.com.antenasur.service.AbstractService;

@Stateless
public class ListaService extends AbstractService<Lista, Integer, ListaFacade> {

    @Inject
    private ListaFacade listaFacade;

    @Override
    protected ListaFacade getFacade() {
        return listaFacade;
    }

    public ListaDTO obtenerDTOPorId(Integer id) {
        if (id == null) return null;
        return ListaDTO.fromEntity(listaFacade.find(id));
    }

    public List<ListaDTO> listarDTOs() {
        List<ListaDTO> resultado = new ArrayList<>();
        List<Lista> listas = listaFacade.findAll();
        if (listas == null) return resultado;
        for (Lista l : listas) resultado.add(ListaDTO.fromEntity(l));
        return resultado;
    }

    public ListaDTO guardarDesdeDTO(ListaDTO dto) {
        if (dto == null) return null;
        if (dto.getId() == null) {
            return ListaDTO.fromEntity(listaFacade.create(dto.toEntity()));
        }
        Lista actual = listaFacade.find(dto.getId());
        if (actual == null) return null;
        actual.setNombre(dto.getNombre());
        actual.setSlogan(dto.getSlogan());
        actual.setNumero(dto.getNumero());
        return ListaDTO.fromEntity(listaFacade.edit(actual));
    }

    public ListaDTO eliminarPorId(Integer id) {
        if (id == null) return null;
        Lista l = listaFacade.find(id);
        if (l == null) return null;
        return ListaDTO.fromEntity(listaFacade.delete(l));
    }
}
