
package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.event.RowEditEvent;
import org.primefaces.model.CheckboxTreeNode;
import org.primefaces.model.TreeNode;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.domain.Menu;
import ec.com.antenasur.service.MenuFacade;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 * @fecha 10-08-2022 15:53
 */
@Named
@ViewScoped
@Slf4j
public class MenuController implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    
    @Inject
    private LoginBean loginBean;
    
    @Inject
    private MenuFacade menuFacade;
    
    @Setter
    @Getter
    private Menu menuSeleccionado;
    
    @Setter
    @Getter
    private List<Menu> listaMenuPadres, listaMenuHijos;
    
    @Setter
    @Getter
    private TreeNode<Menu> root;
    
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
            listaMenuPadres = menuFacade.findByFather();
            this.root = new CheckboxTreeNode<Menu>(listaMenuPadres.get(0), null);
            crearNodoRecursivo(listaMenuPadres, root);
            
        } catch (Exception e) {
            log.error("Error al inicializar valores", e);
        }
    }
    
    public void crearNodoRecursivo(List<Menu> ObjData, TreeNode<Menu> nodoPadre) {
        try {
            for (Menu varnodo : ObjData) {//ObjData=Hijos del nodo padre, Lista de segundo nivel
                TreeNode<Menu> nodoHijo = new CheckboxTreeNode<Menu>(varnodo, nodoPadre);
                List<Menu> listaHijos = menuFacade.listaCatalogoHijo(varnodo.getId());
                if (listaHijos != null) {
                    if (!listaHijos.isEmpty()) {
                        this.crearNodoRecursivo(listaHijos, nodoHijo);
                    }
                }
            }
        } catch (Exception e) {
            log.error("ERROR EN CREAR NODO RECURSIVO", e);
        }
    }
    
    public void onRowEdit(@SuppressWarnings("rawtypes") RowEditEvent<TreeNode> event) {
        try {
            this.menuSeleccionado = (Menu) event.getObject().getData();
            if (menuSeleccionado != null) {
                menuSeleccionado = menuFacade.edit(menuSeleccionado);
                JsfUtil.addSuccessMessage("Catálogo actualizado!");
                this.init();
            }
        } catch (Exception e) {
            log.error("Error al actualizar");
        }
        
    }
    
    public void onRowCancel(@SuppressWarnings("rawtypes") RowEditEvent<TreeNode> event) {
        this.menuSeleccionado = (Menu) event.getObject().getData();
        JsfUtil.addWarningMessage("Cancelado! " + menuSeleccionado.getNombre());
        menuSeleccionado = null;
        PrimeFaces.current().ajax().update("frmPersonas:trTblCatalogo");
    }
    
    public void nuevoMenu() {
        this.menuSeleccionado = new Menu();
        this.menuSeleccionado.setPadre(new Menu());
    }
    
    public void guardarMenu() {
        try {
            if (menuSeleccionado != null) {
                if (menuSeleccionado.getId() != null) {
                    menuSeleccionado = menuFacade.edit(menuSeleccionado);
                    JsfUtil.addSuccessMessage("Catálogo acualizaco!");
                } else {
                    menuSeleccionado = menuFacade.create(menuSeleccionado);
                    JsfUtil.addSuccessMessage("Catálogo creado!");
                }
                menuSeleccionado = null;
            }
            this.init();
            PrimeFaces.current().executeScript("PF('dlgCatalogo').hide()");
            PrimeFaces.current().ajax().update("frmPersonas:trTblCatalogo");
        } catch (Exception e) {
            log.error("Error al guardar informacion", e);
        }
    }
    
    public void eliminarMenuSeleccionado() {
        try {
            if (menuSeleccionado != null) {
                if (menuSeleccionado.getId() != null) {                    
                    menuSeleccionado = menuFacade.delete(menuSeleccionado);
                    JsfUtil.addSuccessMessage("Catálogo eliminado!");
                }
            }
            PrimeFaces.current().ajax().update("frmPersonas:trTblCatalogo", "msgs");
            this.init();            
        } catch (Exception e) {
            log.error("Error al eliminar catalogo", e);
        }
    }
}
