package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.CategoriaVotoDTO;
import ec.com.antenasur.facade.tec.CategoriaVotoFacade;
import ec.com.antenasur.model.tec.CategoriaVoto;
import ec.com.antenasur.service.AbstractService;

@Stateless
public class CategoriaVotoService extends AbstractService<CategoriaVoto, Integer, CategoriaVotoFacade> {

    @Inject
    private CategoriaVotoFacade categoriaVotoFacade;

    @Override
    protected CategoriaVotoFacade getFacade() {
        return categoriaVotoFacade;
    }

    public List<CategoriaVoto> getCategoriasOrdenados() {
        return categoriaVotoFacade.getCategoriasOrdenados();
    }

    public CategoriaVotoDTO obtenerDTOPorId(Integer id) {
        if (id == null) return null;
        return CategoriaVotoDTO.fromEntity(categoriaVotoFacade.find(id));
    }

    public List<CategoriaVotoDTO> listarDTOsOrdenados() {
        List<CategoriaVotoDTO> resultado = new ArrayList<>();
        List<CategoriaVoto> cs = categoriaVotoFacade.getCategoriasOrdenados();
        if (cs == null) return resultado;
        for (CategoriaVoto c : cs) resultado.add(CategoriaVotoDTO.fromEntity(c));
        return resultado;
    }

    public CategoriaVotoDTO guardarDesdeDTO(CategoriaVotoDTO dto) {
        if (dto == null) return null;
        if (dto.getId() == null) {
            return CategoriaVotoDTO.fromEntity(categoriaVotoFacade.create(dto.toEntity()));
        }
        CategoriaVoto actual = categoriaVotoFacade.find(dto.getId());
        if (actual == null) return null;
        actual.setNombre(dto.getNombre());
        actual.setCategoriaVoto(dto.getCategoriaVoto());
        actual.setOrden(dto.getOrden());
        return CategoriaVotoDTO.fromEntity(categoriaVotoFacade.edit(actual));
    }

    public CategoriaVotoDTO eliminarPorId(Integer id) {
        if (id == null) return null;
        CategoriaVoto c = categoriaVotoFacade.find(id);
        if (c == null) return null;
        return CategoriaVotoDTO.fromEntity(categoriaVotoFacade.delete(c));
    }
}
