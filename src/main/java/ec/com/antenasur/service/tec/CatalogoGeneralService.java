package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.CatalogoGeneralDTO;
import ec.com.antenasur.facade.tec.CatalogoGeneralFacade;
import ec.com.antenasur.model.tec.CatalogoGeneral;
import ec.com.antenasur.service.AbstractService;

@Stateless
public class CatalogoGeneralService extends AbstractService<CatalogoGeneral, Integer, CatalogoGeneralFacade> {

    @Inject
    private CatalogoGeneralFacade catalogoFacade;

    @Override
    protected CatalogoGeneralFacade getFacade() {
        return catalogoFacade;
    }

    public List<CatalogoGeneral> findByFatherCatalogue(CatalogoGeneral generalCatalogue) {
        return catalogoFacade.findByFatherCatalogue(generalCatalogue);
    }

    public CatalogoGeneral findByName(String catalogoNombre) {
        return catalogoFacade.findByName(catalogoNombre);
    }

    public List<CatalogoGeneral> findByFatherName(String catalogoNombre) {
        return catalogoFacade.findByFatherName(catalogoNombre);
    }

    public List<CatalogoGeneral> listaCatalogoHijo(Integer padreId) {
        return catalogoFacade.listaCatalogoHijo(padreId);
    }

    public List<CatalogoGeneral> listaCatalogoHijo(Integer padreId, List<Integer> listaIdCargos) {
        return catalogoFacade.listaCatalogoHijo(padreId, listaIdCargos);
    }

    public List<CatalogoGeneral> findByFather() {
        return catalogoFacade.findByFather();
    }

    public CatalogoGeneral findByFatherIdAndCatalogueName(Integer idPadre, String nombreCatalogo) {
        return catalogoFacade.findByFatherIdAndCatalogueName(idPadre, nombreCatalogo);
    }

    public List<CatalogoGeneral> findByFatherName2(String nombreCatalogo) {
        return catalogoFacade.findByFatherName2(nombreCatalogo);
    }

    public List<CatalogoGeneral> findByFatherNameInactive(String nombreCatalogo) {
        return catalogoFacade.findByFatherNameInactive(nombreCatalogo);
    }

    public CatalogoGeneral findByNameAll(String nombreCatalogo) {
        return catalogoFacade.findByNameAll(nombreCatalogo);
    }

    public CatalogoGeneral findByNamelike(String nombreCatalogo, int tamanioCadena) {
        return catalogoFacade.findByNamelike(nombreCatalogo, tamanioCadena);
    }

    // ----- API basada en DTO -----

    public CatalogoGeneralDTO obtenerDTOPorId(Integer id) {
        if (id == null) return null;
        return CatalogoGeneralDTO.fromEntity(catalogoFacade.find(id));
    }

    public List<CatalogoGeneralDTO> listarDTOs() {
        return mapearLista(catalogoFacade.findAll());
    }

    public List<CatalogoGeneralDTO> listarDTOsPorPadre() {
        return mapearLista(catalogoFacade.findByFather());
    }

    public List<CatalogoGeneralDTO> listarDTOsHijosDe(Integer padreId) {
        if (padreId == null) return new ArrayList<>();
        return mapearLista(catalogoFacade.listaCatalogoHijo(padreId));
    }

    public List<CatalogoGeneralDTO> listarDTOsPorNombrePadre(String nombrePadre) {
        return mapearLista(catalogoFacade.findByFatherName(nombrePadre));
    }

    public CatalogoGeneralDTO buscarDTOPorNombre(String nombre) {
        return CatalogoGeneralDTO.fromEntity(catalogoFacade.findByName(nombre));
    }

    public CatalogoGeneralDTO guardarDesdeDTO(CatalogoGeneralDTO dto) {
        if (dto == null) return null;
        CatalogoGeneral padre = (dto.getPadreId() != null) ? catalogoFacade.find(dto.getPadreId()) : null;
        if (dto.getId() == null) {
            CatalogoGeneral nuevo = dto.toEntity();
            nuevo.setPadre(padre);
            return CatalogoGeneralDTO.fromEntity(catalogoFacade.create(nuevo));
        }
        CatalogoGeneral actual = catalogoFacade.find(dto.getId());
        if (actual == null) return null;
        actual.setNombre(dto.getNombre());
        actual.setDescripcion(dto.getDescripcion());
        actual.setHistorial(dto.getHistorial());
        actual.setOrden(dto.getOrden());
        actual.setInfo(dto.getInfo());
        actual.setPadre(padre);
        return CatalogoGeneralDTO.fromEntity(catalogoFacade.edit(actual));
    }

    public CatalogoGeneralDTO eliminarPorId(Integer id) {
        if (id == null) return null;
        CatalogoGeneral c = catalogoFacade.find(id);
        if (c == null) return null;
        return CatalogoGeneralDTO.fromEntity(catalogoFacade.delete(c));
    }

    private List<CatalogoGeneralDTO> mapearLista(List<CatalogoGeneral> entidades) {
        List<CatalogoGeneralDTO> resultado = new ArrayList<>();
        if (entidades == null) return resultado;
        for (CatalogoGeneral c : entidades) resultado.add(CatalogoGeneralDTO.fromEntity(c));
        return resultado;
    }
}
