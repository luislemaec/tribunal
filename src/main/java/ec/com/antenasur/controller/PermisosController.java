package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.primefaces.model.CheckboxTreeNode;
import org.primefaces.model.TreeNode;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.dto.MenuDTO;
import ec.com.antenasur.dto.MenuRolDTO;
import ec.com.antenasur.dto.RolDTO;
import ec.com.antenasur.service.MenuRolService;
import ec.com.antenasur.service.MenuService;
import ec.com.antenasur.service.RolService;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Named
@ViewScoped
@Slf4j
public class PermisosController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private LoginBean loginBean;

    @Inject
    private MenuRolService menuRolService;

    @Inject
    private RolService rolService;

    @Inject
    private MenuService menuService;

    @Setter
    @Getter
    private MenuRolDTO menuRolSeleccionado;

    @Setter
    @Getter
    private List<MenuRolDTO> listaMenuRoles, listaMenuRolHijos;

    @Getter
    private RolDTO rolSeleccionado;

    @Setter
    @Getter
    private List<RolDTO> roles;

    @Setter
    @Getter
    private List<MenuDTO> listaMenuPadres, listaMenuHijos;

    @Setter
    @Getter
    private TreeNode<MenuRolDTO> root;

    @PostConstruct
    private void init() {
        try {
            rolSeleccionado = new RolDTO();
            this.listaMenuPadres = menuService.listarDTOsPadres();
            roles = rolService.listarDTOs();
        } catch (Exception e) {
            log.error("Error al inicializar valores", e);
        }
    }

    public void crearNodoRecursivo(List<MenuRolDTO> objData, TreeNode<MenuRolDTO> nodoPadre) {
        try {
            for (MenuRolDTO varnodo : objData) {
                TreeNode<MenuRolDTO> nodoHijo = new CheckboxTreeNode<MenuRolDTO>(varnodo, nodoPadre);
                Integer menuPadreId = (varnodo.getMenu() != null) ? varnodo.getMenu().getId() : null;
                List<MenuDTO> listaHijos = menuService.listarDTOsHijosDe(menuPadreId);
                if (listaHijos == null || listaHijos.isEmpty()) {
                    continue;
                }
                listaMenuRolHijos = new ArrayList<>();
                for (MenuDTO menu : listaHijos) {
                    listaMenuRolHijos.add(
                            menuRolService.obtenerOPrepararDTOPorMenuYRolIds(menu.getId(), rolSeleccionado.getId()));
                }
                if (!listaMenuRolHijos.isEmpty()) {
                    crearNodoRecursivo(listaMenuRolHijos, nodoHijo);
                }
            }
        } catch (Exception e) {
            log.error("ERROR EN CREAR NODO RECURSIVO", e);
        }
    }

    public void setRolSeleccionado(RolDTO rol) {
        try {
            if (rol != null && rol.getId() != null) {
                rolSeleccionado = rolService.obtenerDTOPorId(rol.getId());
            }
        } catch (Exception e) {
        }
    }

    public void obtieneMenuPorRol() {
        try {
            if (this.rolSeleccionado == null || this.rolSeleccionado.getId() == null) {
                return;
            }
            setRolSeleccionado(this.rolSeleccionado);
            this.listaMenuRoles = new ArrayList<>();
            for (MenuDTO menu : listaMenuPadres) {
                listaMenuRoles.add(
                        menuRolService.obtenerOPrepararDTOPorMenuYRolIds(menu.getId(), rolSeleccionado.getId()));
            }
            if (!listaMenuRoles.isEmpty()) {
                this.root = new CheckboxTreeNode<MenuRolDTO>(listaMenuRoles.get(0), null);
                crearNodoRecursivo(listaMenuRoles, root);
            }
        } catch (Exception e) {
            log.error("ERROR EN CREAR CREAR ARBOL DE OPCIONES", e);
        }
    }

    public void guardarMenuRol(MenuRolDTO menuRol) {
        try {
            if (menuRol != null && rolSeleccionado != null) {
                MenuRolDTO persistido = menuRolService.guardarDesdeDTO(menuRol);
                if (persistido != null) {
                    menuRolSeleccionado = persistido;
                    if (menuRol.getId() != null && menuRol.getId() > 0) {
                        JsfUtil.addInfoMessage("PERMISOS ACTUALIZADOS");
                    } else {
                        JsfUtil.addSuccessMessage(persistido.getMenu().getNombre()
                                + " ASIGNADO A " + persistido.getRol().getNombre());
                    }
                }
            }
            init();
            PrimeFaces.current().ajax().update("frmPermisos:trTblCatalogo");
        } catch (Exception e) {
            log.error("ERROR AL GUARDAR ASIGNACION DE PERMISOS", e);
        }
    }
}
