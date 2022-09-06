/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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
import ec.com.antenasur.domain.CatalogoGeneral;
import ec.com.antenasur.service.CatalogoGeneralFacade;
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
public class CatalogoController implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private static final String ACTIVO = "ACTIVO";
    private static final String INACTIVO = "INACTIVO";
    
    @Inject
    private LoginBean loginBean;
    
    @Inject
    private CatalogoGeneralFacade catalogoFacade;
    
    @Setter
    @Getter
    private CatalogoGeneral catalogoSeleccionado;
    
    @Setter
    @Getter
    private List<CatalogoGeneral> listaCatalogoPadres, listaCatalogoHijos;
    
    @Setter
    @Getter
    private TreeNode<CatalogoGeneral> root;
    
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
            listaCatalogoPadres = catalogoFacade.findByFather();
            this.root = new CheckboxTreeNode<CatalogoGeneral>(listaCatalogoPadres.get(0), null);
            crearNodoRecursivo(listaCatalogoPadres, root);
            
        } catch (Exception e) {
            log.error("Error al inicializar valores", e);
        }
    }
    
    public void crearNodoRecursivo(List<CatalogoGeneral> ObjData, TreeNode<CatalogoGeneral> nodoPadre) {
        try {
            for (CatalogoGeneral varnodo : ObjData) {//ObjData=Hijos del nodo padre, Lista de segundo nivel
                TreeNode<CatalogoGeneral> nodoHijo = new CheckboxTreeNode<CatalogoGeneral>(varnodo, nodoPadre);
                List<CatalogoGeneral> listaHijos = catalogoFacade.listaCatalogoHijo(varnodo.getId());
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
            this.catalogoSeleccionado = (CatalogoGeneral) event.getObject().getData();
            if (catalogoSeleccionado != null) {
                catalogoSeleccionado = catalogoFacade.edit(catalogoSeleccionado);
                JsfUtil.addSuccessMessage("Catálogo actualizado!");
                this.init();
            }
        } catch (Exception e) {
            log.error("Error al actualizar");
        }
        
    }
    
    public void onRowCancel(@SuppressWarnings("rawtypes") RowEditEvent<TreeNode> event) {
        this.catalogoSeleccionado = (CatalogoGeneral) event.getObject().getData();
        JsfUtil.addWarningMessage("Cancelado! " + catalogoSeleccionado.getNombre());
        catalogoSeleccionado = null;
        PrimeFaces.current().ajax().update("frmPersonas:trTblCatalogo");
    }
    
    public void nuevoCatalogo() {
        this.catalogoSeleccionado = new CatalogoGeneral();
        this.catalogoSeleccionado.setPadre(new CatalogoGeneral());
    }
    
    public void guardarCatalogo() {
        try {
            if (catalogoSeleccionado != null) {
                if (catalogoSeleccionado.getId() != null) {
                    catalogoSeleccionado = catalogoFacade.edit(catalogoSeleccionado);
                    JsfUtil.addSuccessMessage("Catálogo acualizaco!");
                } else {
                    catalogoSeleccionado = catalogoFacade.create(catalogoSeleccionado);
                    JsfUtil.addSuccessMessage("Catálogo creado!");
                }
                catalogoSeleccionado = null;
            }
            this.init();
            PrimeFaces.current().executeScript("PF('dlgCatalogo').hide()");
            PrimeFaces.current().ajax().update("frmPersonas:trTblCatalogo");
        } catch (Exception e) {
            log.error("Error al guardar informacion", e);
        }
    }
    
    public void eliminarCatalogoSeleccionado() {
        try {
            if (catalogoSeleccionado != null) {
                if (catalogoSeleccionado.getId() != null) {                    
                    catalogoSeleccionado = catalogoFacade.delete(catalogoSeleccionado);
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
