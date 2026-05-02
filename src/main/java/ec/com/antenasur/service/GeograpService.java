package ec.com.antenasur.service;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import ec.com.antenasur.dto.GeograpDTO;
import ec.com.antenasur.facade.GeograpFacade;
import ec.com.antenasur.model.Geograp;

/**
 * {@link Geograp} no hereda de {@code EntidadBase} (entidad geográfica
 * estática, sin soft-delete), por lo que este service no extiende
 * {@link AbstractService}. Expone explícitamente las operaciones que los
 * consumidores usan.
 */
@Stateless
public class GeograpService {

    @Inject
    private GeograpFacade geograpFacade;

    public Geograp create(Geograp entity) {
        return geograpFacade.create(entity);
    }

    public Geograp edit(Geograp entity) {
        return geograpFacade.edit(entity);
    }

    public Geograp find(Integer id) {
        return geograpFacade.find(id);
    }

    public List<Geograp> findAll() {
        return geograpFacade.findAll();
    }

    public int count() {
        return geograpFacade.count();
    }

    public List<Geograp> findByFatherId(Integer idFather) {
        return geograpFacade.findByFatherId(idFather);
    }

    public Geograp findByFather_Id(Integer idFather) {
        return geograpFacade.findByFather_Id(idFather);
    }

    public Geograp findByGeograpName(String nameGeograp) {
        return geograpFacade.findByGeograpName(nameGeograp);
    }

    public Geograp findByFatherIdAndGeographName(Integer idFather, String nameGeograp) {
        return geograpFacade.findByFatherIdAndGeographName(idFather, nameGeograp);
    }

    public List<Geograp> findByFatherGeograp(Geograp geograp) {
        return geograpFacade.findByFatherGeograp(geograp);
    }

    /**
     * Devuelve la unión de todas las parroquias hijas de los cantones dados.
     * Útil para reportes a nivel provincial donde se necesita iterar sobre
     * todas las parroquias agregadas.
     */
    public GeograpDTO obtenerDTOPorId(Integer id) {
        if (id == null) return null;
        return GeograpDTO.fromEntity(geograpFacade.find(id));
    }

    public List<GeograpDTO> listarDTOsPorPadreId(Integer idPadre) {
        return mapearLista(geograpFacade.findByFatherId(idPadre));
    }

    public GeograpDTO buscarDTOPorNombre(String nombre) {
        return GeograpDTO.fromEntity(geograpFacade.findByGeograpName(nombre));
    }

    public List<GeograpDTO> listarDTOsHijos(Integer padreId) {
        if (padreId == null) return new ArrayList<>();
        Geograp padre = geograpFacade.find(padreId);
        if (padre == null) return new ArrayList<>();
        return mapearLista(geograpFacade.findByFatherGeograp(padre));
    }

    public List<GeograpDTO> listarDTOsParroquiasDeCantones(List<Integer> cantonIds) {
        List<Geograp> cantones = new ArrayList<>();
        if (cantonIds != null) {
            for (Integer id : cantonIds) {
                Geograp g = geograpFacade.find(id);
                if (g != null) cantones.add(g);
            }
        }
        return mapearLista(obtenerParroquiasDeCantones(cantones));
    }

    private List<GeograpDTO> mapearLista(List<Geograp> entidades) {
        List<GeograpDTO> resultado = new ArrayList<>();
        if (entidades == null) return resultado;
        for (Geograp g : entidades) resultado.add(GeograpDTO.fromEntity(g));
        return resultado;
    }

    public List<Geograp> obtenerParroquiasDeCantones(List<Geograp> cantones) {
        List<Geograp> parroquias = new ArrayList<>();
        if (cantones == null) {
            return parroquias;
        }
        for (Geograp canton : cantones) {
            List<Geograp> hijas = geograpFacade.findByFatherGeograp(canton);
            if (hijas != null) {
                parroquias.addAll(hijas);
            }
        }
        return parroquias;
    }
}
