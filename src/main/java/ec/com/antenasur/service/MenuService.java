package ec.com.antenasur.service;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.MenuDTO;
import ec.com.antenasur.dto.RolUsuarioDTO;
import ec.com.antenasur.facade.MenuFacade;
import ec.com.antenasur.model.Menu;
import ec.com.antenasur.model.Rol;
import ec.com.antenasur.model.RolUsuario;
import ec.com.antenasur.model.Usuario;
import ec.com.antenasur.util.MenuVO;

@Stateless
public class MenuService extends AbstractService<Menu, Integer, MenuFacade> {

    @Inject
    private MenuFacade menuFacade;

    @Override
    protected MenuFacade getFacade() {
        return menuFacade;
    }

    public Menu findByMnemonic(String mnemonic) {
        return menuFacade.findByMnemonic(mnemonic);
    }

    public List<MenuVO> getMenusByrols(List<RolUsuario> rolsUsers, String menuMnemonic) {
        return menuFacade.getMenusByrols(rolsUsers, menuMnemonic);
    }

    public Menu findByMenuName(String menuapp) {
        return menuFacade.findByMenuName(menuapp);
    }

    public List<Menu> findByFather() {
        return menuFacade.findByFather();
    }

    public List<Menu> listaCatalogoHijo(Integer padreId) {
        return menuFacade.listaCatalogoHijo(padreId);
    }

    public List<Menu> getMenusFinales() {
        return menuFacade.getMenusFinales();
    }

    /**
     * Devuelve las pÃ¡ginas (Ãºltimo segmento de la URL) permitidas para los
     * menÃºs dados, mÃ¡s las pÃ¡ginas siempre disponibles ({@code inicio.jsf} y
     * {@code cambioClave.jsf}).
     */
    public List<String> extraerPaginasPermitidas(List<MenuVO> menusUsuario) {
        List<String> paginas = new ArrayList<>();
        if (menusUsuario != null) {
            for (MenuVO menu : menusUsuario) {
                paginas.add(extraerUltimoSegmento(menu.getUrlMenu()));
            }
        }
        paginas.add("inicio.jsf");
        paginas.add("cambioClave.jsf");
        return paginas;
    }

    private String extraerUltimoSegmento(String url) {
        StringTokenizer st = new StringTokenizer(url == null ? "" : url, "/");
        String ultimo = null;
        while (st.hasMoreTokens()) {
            ultimo = st.nextToken();
        }
        return ultimo;
    }

    // ----- API basada en DTO -----

    public MenuDTO obtenerDTOPorId(Integer id) {
        if (id == null) return null;
        return MenuDTO.fromEntity(menuFacade.find(id));
    }

    public List<MenuDTO> listarDTOs() {
        return mapearLista(menuFacade.findAll());
    }

    public List<MenuDTO> listarDTOsPadres() {
        return mapearLista(menuFacade.findByFather());
    }

    public List<MenuDTO> listarDTOsHijosDe(Integer padreId) {
        if (padreId == null) return new ArrayList<>();
        return mapearLista(menuFacade.listaCatalogoHijo(padreId));
    }

    public List<MenuDTO> listarDTOsFinales() {
        return mapearLista(menuFacade.getMenusFinales());
    }

    public MenuDTO buscarDTOPorMnemonic(String mnemonic) {
        return MenuDTO.fromEntity(menuFacade.findByMnemonic(mnemonic));
    }

    /**
     * VersiÃ³n DTO de {@link #getMenusByrols(List, String)}: reconstruye stubs
     * de RolUsuario+Rol+Usuario con solo ids (suficiente para la query que
     * usa MenuFacade) y delega.
     */
    public List<MenuVO> getMenusByrolsDTO(List<RolUsuarioDTO> rolesUsuario, String mnemonic) {
        List<RolUsuario> stubs = new ArrayList<>();
        if (rolesUsuario != null) {
            for (RolUsuarioDTO ru : rolesUsuario) {
                RolUsuario stub = new RolUsuario();
                stub.setId(ru.getId());
                if (ru.getRol() != null) {
                    Rol r = new Rol();
                    r.setId(ru.getRol().getId());
                    r.setNombre(ru.getRol().getNombre());
                    stub.setRol(r);
                }
                if (ru.getUsuario() != null) {
                    Usuario u = new Usuario();
                    u.setId(ru.getUsuario().getId());
                    stub.setUsuario(u);
                }
                stubs.add(stub);
            }
        }
        return menuFacade.getMenusByrols(stubs, mnemonic);
    }

    public MenuDTO guardarDesdeDTO(MenuDTO dto) {
        if (dto == null) return null;
        Menu padre = (dto.getPadreId() != null) ? menuFacade.find(dto.getPadreId()) : null;
        if (dto.getId() == null) {
            Menu nuevo = dto.toEntity();
            nuevo.setPadre(padre);
            return MenuDTO.fromEntity(menuFacade.create(nuevo));
        }
        Menu actual = menuFacade.find(dto.getId());
        if (actual == null) return null;
        actual.setNombre(dto.getNombre());
        actual.setAccion(dto.getAccion());
        actual.setNodoFinal(dto.getNodoFinal());
        actual.setIcono(dto.getIcono());
        actual.setImagen(dto.getImagen());
        actual.setOrden(dto.getOrden());
        actual.setUrl(dto.getUrl());
        actual.setComponenteid(dto.getComponenteid());
        actual.setPadre(padre);
        return MenuDTO.fromEntity(menuFacade.edit(actual));
    }

    public MenuDTO eliminarPorId(Integer id) {
        if (id == null) return null;
        Menu m = menuFacade.find(id);
        if (m == null) return null;
        return MenuDTO.fromEntity(menuFacade.delete(m));
    }

    private List<MenuDTO> mapearLista(List<Menu> menus) {
        List<MenuDTO> resultado = new ArrayList<>();
        if (menus == null) return resultado;
        for (Menu m : menus) resultado.add(MenuDTO.fromEntity(m));
        return resultado;
    }
}
