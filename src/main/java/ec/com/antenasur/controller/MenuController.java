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

import ec.com.antenasur.bean.LoginBean;
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
    private LoginBean loginBean;

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
            if (listaMenuPadres != null && !listaMenuPadres.isEmpty()) {
                this.root = new CheckboxTreeNode<MenuDTO>(listaMenuPadres.get(0), null);
                crearNodoRecursivo(listaMenuPadres, root);
            }
        } catch (Exception e) {
            log.error("Error al inicializar valores", e);
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
            log.error("ERROR EN CREAR NODO RECURSIVO", e);
        }
    }

    @SuppressWarnings("rawtypes")
    public void onRowEdit(RowEditEvent<TreeNode> event) {
        try {
            this.menuSeleccionado = (MenuDTO) event.getObject().getData();
            if (menuSeleccionado != null) {
                menuSeleccionado = menuService.guardarDesdeDTO(menuSeleccionado);
                JsfUtil.addSuccessMessage("CatÃ¡logo actualizado!");
                init();
            }
        } catch (Exception e) {
            log.error("Error al actualizar", e);
        }
    }

    @SuppressWarnings("rawtypes")
    public void onRowCancel(RowEditEvent<TreeNode> event) {
        this.menuSeleccionado = (MenuDTO) event.getObject().getData();
        JsfUtil.addWarningMessage("Cancelado! " + menuSeleccionado.getNombre());
        menuSeleccionado = null;
        PrimeFaces.current().ajax().update("frmPersonas:trTblCatalogo");
    }

    public void nuevoMenu() {
        this.menuSeleccionado = new MenuDTO();
    }

    public void guardarMenu() {
        try {
            if (menuSeleccionado == null) {
                return;
            }
            boolean esEdicion = menuSeleccionado.getId() != null;
            MenuDTO persistido = menuService.guardarDesdeDTO(menuSeleccionado);
            if (persistido != null) {
                JsfUtil.addSuccessMessage(esEdicion ? "CatÃ¡logo actualizado!" : "CatÃ¡logo creado!");
            }
            menuSeleccionado = null;
            init();
            PrimeFaces.current().executeScript("PF('dlgCatalogo').hide()");
            PrimeFaces.current().ajax().update("frmPersonas:trTblCatalogo");
        } catch (Exception e) {
            log.error("Error al guardar informacion", e);
        }
    }

    public void eliminarMenuSeleccionado() {
        try {
            if (menuSeleccionado != null && menuSeleccionado.getId() != null) {
                menuService.eliminarPorId(menuSeleccionado.getId());
                JsfUtil.addSuccessMessage("CatÃ¡logo eliminado!");
            }
            PrimeFaces.current().ajax().update("frmPersonas:trTblCatalogo", "msgs");
            init();
        } catch (Exception e) {
            log.error("Error al eliminar catalogo", e);
        }
    }
}
