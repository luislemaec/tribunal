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
public class MenuRolService extends AbstractService<MenuRol, Integer, MenuRolFacade> {

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
     * Devuelve la asignaciÃ³n menu-rol existente, o un placeholder transitorio
     * (sin id, {@code estado=false}) si todavÃ­a no existe. Ãštil para
     * construir Ã¡rboles de permisos donde cada nodo necesita un MenuRol â€”
     * existente o no â€” para el binding del checkbox.
     *
     * @return MenuRol nunca null si se reciben menu y rol vÃ¡lidos
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
     * VersiÃ³n DTO de {@link #obtenerOPrepararPorMenuYRol(Menu, Rol)}: recibe
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
