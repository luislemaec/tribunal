package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.primefaces.PrimeFaces;
import org.primefaces.event.RowEditEvent;
import org.primefaces.model.CheckboxTreeNode;
import org.primefaces.model.TreeNode;

import ec.com.antenasur.dto.MenuDTO;
import ec.com.antenasur.service.MenuService;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Named
@ViewScoped
@Slf4j
public class MenuController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private MenuService menuService;

    @Setter
    @Getter
    private MenuDTO menuSeleccionado;

    @Setter
    @Getter
    private List<MenuDTO> listaMenuPadres, listaMenuHijos;

    @Setter
    @Getter
    private List<MenuDTO> listaOpcionesPadre;

    @Setter
    @Getter
    private TreeNode<MenuDTO> root;

    @Setter
    @Getter
    private TreeNode<?> selectedNode;

    @SuppressWarnings("rawtypes")
    @Getter
    @Setter
    private TreeNode[] selectedNodes;

    @PostConstruct
    private void init() {
        try {
            listaMenuPadres = menuService.listarDTOsPadres();
            listaOpcionesPadre = menuService.listarDTOs();
            root = new CheckboxTreeNode<MenuDTO>(new MenuDTO(), null);
            if (listaMenuPadres != null && !listaMenuPadres.isEmpty()) {
                crearNodoRecursivo(listaMenuPadres, root);
            }
        } catch (Exception e) {
            log.error("Error al inicializar valores del menu", e);
            JsfUtil.addErrorMessageFromBundle("menu.mensaje.errorInicializar");
        }
    }

    public void crearNodoRecursivo(List<MenuDTO> objData, TreeNode<MenuDTO> nodoPadre) {
        try {
            for (MenuDTO varnodo : objData) {
                TreeNode<MenuDTO> nodoHijo = new CheckboxTreeNode<MenuDTO>(varnodo, nodoPadre);
                List<MenuDTO> listaHijos = menuService.listarDTOsHijosDe(varnodo.getId());
                if (listaHijos != null && !listaHijos.isEmpty()) {
                    crearNodoRecursivo(listaHijos, nodoHijo);
                }
            }
        } catch (Exception e) {
            log.error("Error al crear el arbol de menu", e);
            JsfUtil.addErrorMessageFromBundle("menu.mensaje.errorInicializar");
        }
    }

    @SuppressWarnings("rawtypes")
    public void onRowEdit(RowEditEvent<TreeNode> event) {
        try {
            menuSeleccionado = (MenuDTO) event.getObject().getData();
            if (menuSeleccionado != null) {
                menuService.guardarDesdeDTO(menuSeleccionado);
                JsfUtil.addSuccessMessageFromBundle("menu.mensaje.actualizado");
                init();
                PrimeFaces.current().ajax().update("frmMenu:trTblMenuo");
            }
        } catch (Exception e) {
            log.error("Error al actualizar menu", e);
            JsfUtil.addErrorMessageFromBundle("menu.mensaje.errorGuardar");
        }
    }

    @SuppressWarnings("rawtypes")
    public void onRowCancel(RowEditEvent<TreeNode> event) {
        menuSeleccionado = (MenuDTO) event.getObject().getData();
        if (menuSeleccionado != null) {
            JsfUtil.addWarningMessageFromBundle("menu.mensaje.cancelado", menuSeleccionado.getNombre());
        }
        menuSeleccionado = null;
        PrimeFaces.current().ajax().update("frmMenu:trTblMenuo");
    }

    public void nuevoMenu() {
        menuSeleccionado = new MenuDTO();
        menuSeleccionado.setNodoFinal(Boolean.FALSE);
    }

    public void guardarMenu() {
        try {
            if (menuSeleccionado == null) {
                return;
            }
            boolean esEdicion = menuSeleccionado.getId() != null;
            MenuDTO persistido = menuService.guardarDesdeDTO(menuSeleccionado);
            if (persistido != null) {
                JsfUtil.addSuccessMessageFromBundle(esEdicion ? "menu.mensaje.actualizado" : "menu.mensaje.creado");
            }
            menuSeleccionado = null;
            init();
            PrimeFaces.current().executeScript("PF('dlgMenuo').hide()");
            PrimeFaces.current().ajax().update("frmMenu:trTblMenuo", "frmMenu:outPnlMenu");
        } catch (Exception e) {
            log.error("Error al guardar menu", e);
            JsfUtil.addErrorMessageFromBundle("menu.mensaje.errorGuardar");
        }
    }

    public void eliminarMenuSeleccionado() {
        try {
            if (menuSeleccionado != null && menuSeleccionado.getId() != null) {
                Integer menuId = menuSeleccionado.getId();
                menuService.eliminarPorId(menuSeleccionado.getId());
                eliminarNodoDelArbol(root, menuId);
                listaOpcionesPadre = menuService.listarDTOs();
                JsfUtil.addSuccessMessageFromBundle("menu.mensaje.eliminado");
            }
            menuSeleccionado = null;
            PrimeFaces.current().ajax().update("frmMenu:trTblMenuo", "frmMenu:outPnlMenu");
        } catch (Exception e) {
            log.error("Error al eliminar menu", e);
            JsfUtil.addErrorMessageFromBundle("menu.mensaje.errorEliminar");
        }
    }

    private boolean eliminarNodoDelArbol(TreeNode<MenuDTO> nodoActual, Integer menuId) {
        if (nodoActual == null || menuId == null) {
            return false;
        }
        List<TreeNode<MenuDTO>> hijos = nodoActual.getChildren();
        for (int i = 0; i < hijos.size(); i++) {
            TreeNode<MenuDTO> hijo = hijos.get(i);
            MenuDTO data = hijo.getData();
            if (data != null && menuId.equals(data.getId())) {
                hijos.remove(i);
                return true;
            }
            if (eliminarNodoDelArbol(hijo, menuId)) {
                return true;
            }
        }
        return false;
    }
}
