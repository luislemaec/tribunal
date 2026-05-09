package ec.com.antenasur.service;

import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.RolDTO;
import ec.com.antenasur.facade.RolFacade;
import ec.com.antenasur.model.Rol;

@Stateless
public class RolService extends AbstractService<Rol, Integer, RolFacade> {

    @Inject
    private RolFacade rolFacade;

    @Override
    protected RolFacade getFacade() {
        return rolFacade;
    }

    public Rol buscaPorNombre(String nombre) {
        return rolFacade.buscaPorNombre(nombre);
    }

    public List<Rol> getRolesAplicativoSeleccion() {
        return rolFacade.getRolesAplicativoSeleccion();
    }

    public RolDTO obtenerDTOPorId(Integer id) {
        if (id == null) return null;
        return RolDTO.fromEntity(rolFacade.find(id));
    }

    public List<RolDTO> listarDTOs() {
        return mapearLista(rolFacade.findAll());
    }

    public List<RolDTO> listarDTOsAplicativoSeleccion() {
        return mapearLista(rolFacade.getRolesAplicativoSeleccion());
    }

    public RolDTO buscarDTOPorNombre(String nombre) {
        return RolDTO.fromEntity(rolFacade.buscaPorNombre(nombre));
    }

    public RolDTO guardarDesdeDTO(RolDTO dto) {
        if (dto == null) return null;
        if (dto.getId() == null) {
            return RolDTO.fromEntity(rolFacade.create(dto.toEntity()));
        }
        Rol actual = rolFacade.find(dto.getId());
        if (actual == null) return null;
        actual.setNombre(dto.getNombre());
        actual.setDescripcion(dto.getDescripcion());
        return RolDTO.fromEntity(rolFacade.edit(actual));
    }

    public RolDTO eliminarPorId(Integer id) {
        if (id == null) return null;
        Rol r = rolFacade.find(id);
        if (r == null) return null;
        return RolDTO.fromEntity(rolFacade.delete(r));
    }

    private List<RolDTO> mapearLista(List<Rol> roles) {
        List<RolDTO> resultado = new ArrayList<>();
        if (roles == null) return resultado;
        for (Rol r : roles) resultado.add(RolDTO.fromEntity(r));
        return resultado;
    }
}
