package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.CatalogoGeneralDTO;
import ec.com.antenasur.exception.NegocioException;
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
        validarCatalogo(dto);
        CatalogoGeneral padre = (dto.getPadreId() != null) ? catalogoFacade.find(dto.getPadreId()) : null;
        if (dto.getPadreId() != null && padre == null) {
            throw new NegocioException("catalogo.mensaje.padre.invalido");
        }
        if (dto.getId() != null && dto.getId().equals(dto.getPadreId())) {
            throw new NegocioException("catalogo.mensaje.padre.mismo");
        }
        if (catalogoFacade.buscarActivoPorPadreYNombre(dto.getPadreId(), dto.getNombre(), dto.getId()) != null) {
            throw new NegocioException("catalogo.mensaje.duplicado");
        }
        if (dto.getId() == null) {
            CatalogoGeneral nuevo = dto.toEntity();
            aplicarValoresNormalizados(nuevo, dto);
            nuevo.setPadre(padre);
            return CatalogoGeneralDTO.fromEntity(catalogoFacade.create(nuevo));
        }
        CatalogoGeneral actual = catalogoFacade.find(dto.getId());
        if (actual == null) {
            throw new NegocioException("catalogo.mensaje.no.encontrado");
        }
        aplicarValoresNormalizados(actual, dto);
        actual.setPadre(padre);
        return CatalogoGeneralDTO.fromEntity(catalogoFacade.edit(actual));
    }

    public CatalogoGeneralDTO deshabilitarPorId(Integer id) {
        if (id == null) {
            throw new NegocioException("catalogo.mensaje.no.seleccionado");
        }
        CatalogoGeneral c = catalogoFacade.find(id);
        if (c == null) {
            throw new NegocioException("catalogo.mensaje.no.encontrado");
        }
        if (catalogoFacade.contarHijosActivos(id) > 0) {
            throw new NegocioException("catalogo.mensaje.usado");
        }
        if (catalogoFacade.contarReferenciasOperativas(id) > 0) {
            throw new NegocioException("catalogo.mensaje.usado");
        }
        return CatalogoGeneralDTO.fromEntity(catalogoFacade.delete(c));
    }

    public CatalogoGeneralDTO eliminarPorId(Integer id) {
        return deshabilitarPorId(id);
    }

    private void validarCatalogo(CatalogoGeneralDTO dto) {
        if (dto == null) {
            throw new NegocioException("catalogo.mensaje.no.seleccionado");
        }
        if (!tieneTexto(dto.getNombre()) || !tieneTexto(dto.getDescripcion()) || dto.getOrden() == null) {
            throw new NegocioException("catalogo.mensaje.campos.obligatorios");
        }
    }

    private void aplicarValoresNormalizados(CatalogoGeneral catalogo, CatalogoGeneralDTO dto) {
        catalogo.setNombre(normalizar(dto.getNombre()));
        catalogo.setDescripcion(normalizar(dto.getDescripcion()));
        catalogo.setHistorial(dto.getHistorial());
        catalogo.setOrden(dto.getOrden());
        catalogo.setInfo(tieneTexto(dto.getInfo()) ? dto.getInfo().trim() : null);
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim().toUpperCase();
    }

    private List<CatalogoGeneralDTO> mapearLista(List<CatalogoGeneral> entidades) {
        List<CatalogoGeneralDTO> resultado = new ArrayList<>();
        if (entidades == null) return resultado;
        for (CatalogoGeneral c : entidades) resultado.add(CatalogoGeneralDTO.fromEntity(c));
        return resultado;
    }
}
