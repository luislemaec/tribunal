package ec.com.antenasur.service;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import ec.com.antenasur.dto.IglesiaDTO;
import ec.com.antenasur.facade.GeograpFacade;
import ec.com.antenasur.facade.IglesiaFacade;
import ec.com.antenasur.facade.tec.DocumentoFacade;
import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.Iglesia;

@Stateless
public class IglesiaService extends AbstractService<Iglesia, Integer, IglesiaFacade> {

    @Inject
    private IglesiaFacade iglesiaFacade;

    @Inject
    private DocumentoFacade documentoFacade;

    @Inject
    private GeograpFacade geograpFacade;

    @Override
    protected IglesiaFacade getFacade() {
        return iglesiaFacade;
    }

    public List<Iglesia> getIglesiasPorParroquia(Geograp parroquia) {
        return iglesiaFacade.getIglesiasPorParroquia(parroquia);
    }

    public Iglesia getIglesiaPorDocumento(String documento) {
        return iglesiaFacade.getIglesiaPorDocumento(documento);
    }

    public List<Iglesia> obtieneIglesiasAsignadasPorIds(List<Integer> listaIdIglesias) {
        return iglesiaFacade.obtieneIglesiasAsignadasPorIds(listaIdIglesias);
    }

    public List<Iglesia> obtieneIglesiasPorAsignarPorIds(List<Integer> listaIdIglesias, List<Integer> listaIdParroquias) {
        return iglesiaFacade.obtieneIglesiasPorAsignarPorIds(listaIdIglesias, listaIdParroquias);
    }

    public List<Iglesia> getIglesiasPorParroquias(List<Geograp> parroquias) {
        return iglesiaFacade.getIglesiasPorParroquias(parroquias);
    }

    public Iglesia getIglesiaPorNombreNombreComunidadYUbicacion(Iglesia iglesiaTmp) {
        return iglesiaFacade.getIglesiaPorNombreNombreComunidadYUbicacion(iglesiaTmp);
    }

    /**
     * Marca cada iglesia con su flag {@code tieneDocumentos} consultando si
     * existe al menos un documento del tipo dado vinculado a su id. Modifica
     * la lista in-place; no persiste cambios (es un flag transitorio para la
     * vista). No-op si la lista es null o vacía.
     */
    /**
     * Marca el flag {@code tieneDocumentos} en cada iglesia de la lista. Usa
     * una sola query agregada
     * ({@link DocumentoFacade#getEntidadesIdsConDocumentos}) en lugar de
     * 1 query por iglesia. Antes: O(N) round-trips a BD; ahora: O(1).
     */
    public void marcarConTieneDocumentos(List<Iglesia> iglesias, Integer tipoDocumentoId) {
        if (iglesias == null || iglesias.isEmpty() || tipoDocumentoId == null) {
            return;
        }
        java.util.Set<Integer> idsConDocs = documentoFacade.getEntidadesIdsConDocumentos(tipoDocumentoId);
        for (Iglesia iglesia : iglesias) {
            iglesia.setTieneDocumentos(idsConDocs.contains(iglesia.getId()));
        }
    }

    // ----- API basada en DTO -----

    public IglesiaDTO obtenerDTOPorId(Integer id) {
        if (id == null) {
            return null;
        }
        return IglesiaDTO.fromEntity(iglesiaFacade.find(id));
    }

    public List<IglesiaDTO> listarDTOs() {
        return mapearLista(iglesiaFacade.findAll());
    }

    public List<IglesiaDTO> listarDTOsPorParroquias(List<Geograp> parroquias) {
        return mapearLista(iglesiaFacade.getIglesiasPorParroquias(parroquias));
    }

    public List<IglesiaDTO> listarDTOsPorParroquia(Geograp parroquia) {
        return mapearLista(iglesiaFacade.getIglesiasPorParroquia(parroquia));
    }

    /**
     * Variante de {@link #listarDTOs()} que rellena el flag
     * {@code tieneDocumentos} en cada DTO antes de retornar.
     */
    public List<IglesiaDTO> listarDTOsConFlagDocumentos(Integer tipoDocumentoId) {
        List<Iglesia> iglesias = iglesiaFacade.findAll();
        marcarConTieneDocumentos(iglesias, tipoDocumentoId);
        return mapearLista(iglesias);
    }

    /**
     * Persiste la iglesia descrita por el DTO. Si {@code id} es null, crea;
     * si tiene id, hidrata la entidad existente con los campos del DTO. La
     * ubicación se resuelve por {@link IglesiaDTO#getUbicacionId()} contra
     * {@link GeograpFacade}; si el id es null la iglesia se persiste sin
     * ubicación (caso poco frecuente).
     */
    public IglesiaDTO guardarDesdeDTO(IglesiaDTO dto) {
        if (dto == null) {
            return null;
        }
        Geograp ubicacion = (dto.getUbicacionId() != null)
                ? geograpFacade.find(dto.getUbicacionId()) : null;

        if (dto.getId() == null) {
            Iglesia nueva = dto.toEntity();
            nueva.setUbicacion(ubicacion);
            return IglesiaDTO.fromEntity(iglesiaFacade.create(nueva));
        }
        Iglesia actual = iglesiaFacade.find(dto.getId());
        if (actual == null) {
            return null;
        }
        actual.setNombre(dto.getNombre());
        actual.setComunidad(dto.getComunidad());
        actual.setTotalMiembros(dto.getTotalMiembros());
        actual.setDocumento(dto.getDocumento());
        actual.setUbicacion(ubicacion);
        return IglesiaDTO.fromEntity(iglesiaFacade.edit(actual));
    }

    public IglesiaDTO eliminarPorId(Integer id) {
        if (id == null) {
            return null;
        }
        Iglesia i = iglesiaFacade.find(id);
        if (i == null) {
            return null;
        }
        return IglesiaDTO.fromEntity(iglesiaFacade.delete(i));
    }

    public IglesiaDTO buscarDTOPorDocumento(String documento) {
        if (documento == null || documento.isEmpty()) {
            return null;
        }
        return IglesiaDTO.fromEntity(iglesiaFacade.getIglesiaPorDocumento(documento));
    }

    public List<IglesiaDTO> listarDTOsPorAsignarPorIds(List<Integer> idsExcluir, List<Integer> idsParroquias) {
        return mapearLista(iglesiaFacade.obtieneIglesiasPorAsignarPorIds(idsExcluir, idsParroquias));
    }

    private List<IglesiaDTO> mapearLista(List<Iglesia> iglesias) {
        List<IglesiaDTO> resultado = new ArrayList<>();
        if (iglesias == null) {
            return resultado;
        }
        for (Iglesia i : iglesias) {
            resultado.add(IglesiaDTO.fromEntity(i));
        }
        return resultado;
    }
}
