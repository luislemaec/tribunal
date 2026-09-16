package ec.com.antenasur.service;

import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.MenuRolDTO;
import ec.com.antenasur.facade.MenuFacade;
import ec.com.antenasur.facade.MenuRolFacade;
import ec.com.antenasur.facade.RolFacade;
import ec.com.antenasur.model.Menu;
import ec.com.antenasur.model.MenuRol;
import ec.com.antenasur.model.Rol;

@Stateless
@jakarta.annotation.security.RolesAllowed("SITEC-Administrador")
public class MenuRolService extends AbstractService<MenuRol, Integer, MenuRolFacade> {

    @Override
    @jakarta.annotation.security.RolesAllowed("SITEC-Administrador")
    public MenuRol create(MenuRol entity) { return getFacade().create(entity); }

    @Override
    @jakarta.annotation.security.RolesAllowed("SITEC-Administrador")
    public MenuRol edit(MenuRol entity) { return getFacade().edit(entity); }

    @Override
    @jakarta.annotation.security.RolesAllowed("SITEC-Administrador")
    public MenuRol delete(MenuRol entity) { return getFacade().delete(entity); }

    @Override
    @jakarta.annotation.security.RolesAllowed("SITEC-Administrador")
    public void remove(MenuRol entity) { getFacade().remove(entity); }

    @Override
    @jakarta.annotation.security.RolesAllowed("SITEC-Administrador")
    public MenuRol find(Integer id) { return getFacade().find(id); }

    @Override
    @jakarta.annotation.security.RolesAllowed("SITEC-Administrador")
    public List<MenuRol> findAll() { return getFacade().findAll(); }

    @Override
    @jakarta.annotation.security.RolesAllowed("SITEC-Administrador")
    public List<MenuRol> findRange(int[] range) { return getFacade().findRange(range); }

    @Override
    @jakarta.annotation.security.RolesAllowed("SITEC-Administrador")
    public int count() { return getFacade().count(); }


    @Inject
    private MenuRolFacade menuRolFacade;

    @Inject
    private MenuFacade menuFacade;

    @Inject
    private RolFacade rolFacade;

    @Override
    protected MenuRolFacade getFacade() {
        return menuRolFacade;
    }

    public MenuRol getPorMenuYRol(Menu menu, Rol rol) {
        return menuRolFacade.getPorMenuYRol(menu, rol);
    }

    public List<MenuRol> getPorRol(Rol rol) {
        return menuRolFacade.getPorRol(rol);
    }

    /**
     * Devuelve la asignación menu-rol existente, o un placeholder transitorio
     * (sin id, {@code estado=false}) si todavía no existe. Útil para
     * construir árboles de permisos donde cada nodo necesita un MenuRol —
     * existente o no — para el binding del checkbox.
     *
     * @return MenuRol nunca null si se reciben menu y rol válidos
     */
    public MenuRol obtenerOPrepararPorMenuYRol(Menu menu, Rol rol) {
        if (menu == null || rol == null) {
            return null;
        }
        MenuRol existente = menuRolFacade.getPorMenuYRol(menu, rol);
        if (existente != null) {
            return existente;
        }
        MenuRol placeholder = new MenuRol(menu, rol);
        placeholder.setEstado(false);
        return placeholder;
    }

    // ----- API basada en DTO -----

    public MenuRolDTO obtenerDTOPorId(Integer id) {
        if (id == null) return null;
        return MenuRolDTO.fromEntity(menuRolFacade.find(id));
    }

    public List<MenuRolDTO> listarDTOsPorRolId(Integer rolId) {
        if (rolId == null) return new ArrayList<>();
        Rol rol = rolFacade.find(rolId);
        if (rol == null) return new ArrayList<>();
        return mapearLista(menuRolFacade.getPorRol(rol));
    }

    /**
     * Versión DTO de {@link #obtenerOPrepararPorMenuYRol(Menu, Rol)}: recibe
     * ids, retorna un MenuRolDTO existente o un placeholder transitorio (sin
     * id, estado=false).
     */
    public MenuRolDTO obtenerOPrepararDTOPorMenuYRolIds(Integer menuId, Integer rolId) {
        if (menuId == null || rolId == null) return null;
        Menu menu = menuFacade.find(menuId);
        Rol rol = rolFacade.find(rolId);
        return MenuRolDTO.fromEntity(obtenerOPrepararPorMenuYRol(menu, rol));
    }

    /**
     * Persiste el toggle de un permiso menu-rol. Si el DTO trae id, hace
     * edit (typical flow al cambiar el estado del checkbox); si no, crea con
     * el estado actual.
     */
    public MenuRolDTO guardarDesdeDTO(MenuRolDTO dto) {
        if (dto == null || dto.getMenu() == null || dto.getRol() == null) return null;
        Menu menu = menuFacade.find(dto.getMenu().getId());
        Rol rol = rolFacade.find(dto.getRol().getId());
        if (menu == null || rol == null) return null;

        if (dto.getId() == null) {
            MenuRol nuevo = new MenuRol(menu, rol);
            nuevo.setEstado(dto.getEstado());
            return MenuRolDTO.fromEntity(menuRolFacade.create(nuevo));
        }
        MenuRol actual = menuRolFacade.find(dto.getId());
        if (actual == null) return null;
        actual.setMenu(menu);
        actual.setRol(rol);
        actual.setEstado(dto.getEstado());
        return MenuRolDTO.fromEntity(menuRolFacade.edit(actual));
    }

    private List<MenuRolDTO> mapearLista(List<MenuRol> entidades) {
        List<MenuRolDTO> resultado = new ArrayList<>();
        if (entidades == null) return resultado;
        for (MenuRol mr : entidades) resultado.add(MenuRolDTO.fromEntity(mr));
        return resultado;
    }
}
