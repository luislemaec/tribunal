package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import ec.com.antenasur.dto.RecintoDTO;
import ec.com.antenasur.facade.GeograpFacade;
import ec.com.antenasur.facade.tec.RecintoFacade;
import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.tec.Recinto;
import ec.com.antenasur.service.AbstractService;

@Stateless
public class RecintoService extends AbstractService<Recinto, Integer, RecintoFacade> {

    @Inject
    private RecintoFacade recintoFacade;

    @Inject
    private GeograpFacade geograpFacade;

    @Override
    protected RecintoFacade getFacade() {
        return recintoFacade;
    }

    public Recinto buscaRecintoPorNombre(String nombreRecinto) {
        return recintoFacade.buscaRecintoPorNombre(nombreRecinto);
    }

    public List<Recinto> getRecintosPorParroquias(List<Geograp> parroquias) {
        return recintoFacade.getRecintosPorParroquias(parroquias);
    }

    // ----- API basada en DTO -----

    public RecintoDTO obtenerDTOPorId(Integer id) {
        if (id == null) {
            return null;
        }
        return RecintoDTO.fromEntity(recintoFacade.find(id));
    }

    public List<RecintoDTO> listarDTOs() {
        return mapearLista(recintoFacade.findAll());
    }

    public List<RecintoDTO> listarDTOsPorParroquias(List<Geograp> parroquias) {
        return mapearLista(recintoFacade.getRecintosPorParroquias(parroquias));
    }

    public RecintoDTO buscarDTOPorNombre(String nombre) {
        if (nombre == null || nombre.isEmpty()) {
            return null;
        }
        return RecintoDTO.fromEntity(recintoFacade.buscaRecintoPorNombre(nombre));
    }

    /**
     * Persiste el recinto descrito por el DTO. Resuelve {@code ubicacionId}
     * contra {@link GeograpFacade}. Si el id es null crea, si no, hidrata la
     * entidad existente con los campos del DTO (preservando auditoría).
     */
    public RecintoDTO guardarDesdeDTO(RecintoDTO dto) {
        if (dto == null) {
            return null;
        }
        Geograp ubicacion = (dto.getUbicacionId() != null)
                ? geograpFacade.find(dto.getUbicacionId()) : null;

        if (dto.getId() == null) {
            Recinto nuevo = dto.toEntity();
            nuevo.setUbicacion(ubicacion);
            return RecintoDTO.fromEntity(recintoFacade.create(nuevo));
        }
        Recinto actual = recintoFacade.find(dto.getId());
        if (actual == null) {
            return null;
        }
        actual.setNombre(dto.getNombre());
        actual.setUbicacion(ubicacion);
        actual.setEstadoTarea(dto.getEstadoTarea());
        return RecintoDTO.fromEntity(recintoFacade.edit(actual));
    }

    public RecintoDTO eliminarPorId(Integer id) {
        if (id == null) {
            return null;
        }
        Recinto r = recintoFacade.find(id);
        if (r == null) {
            return null;
        }
        return RecintoDTO.fromEntity(recintoFacade.delete(r));
    }

    private List<RecintoDTO> mapearLista(List<Recinto> recintos) {
        List<RecintoDTO> resultado = new ArrayList<>();
        if (recintos == null) {
            return resultado;
        }
        for (Recinto r : recintos) {
            resultado.add(RecintoDTO.fromEntity(r));
        }
        return resultado;
    }
}
