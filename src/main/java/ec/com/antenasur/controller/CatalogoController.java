package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.primefaces.event.RowEditEvent;
import org.primefaces.model.CheckboxTreeNode;
import org.primefaces.model.TreeNode;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.dto.CatalogoGeneralDTO;
import ec.com.antenasur.service.tec.CatalogoGeneralService;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Named
@ViewScoped
@Slf4j
public class CatalogoController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private LoginBean loginBean;

    @Inject
    private CatalogoGeneralService catalogoService;

    @Setter
    @Getter
    private CatalogoGeneralDTO catalogoSeleccionado;

    @Setter
    @Getter
    private List<CatalogoGeneralDTO> listaCatalogoPadres, listaCatalogoHijos;

    @Setter
    @Getter
    private TreeNode<CatalogoGeneralDTO> root;

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
            listaCatalogoPadres = catalogoService.listarDTOsPorPadre();
            if (listaCatalogoPadres != null && !listaCatalogoPadres.isEmpty()) {
                this.root = new CheckboxTreeNode<CatalogoGeneralDTO>(listaCatalogoPadres.get(0), null);
                crearNodoRecursivo(listaCatalogoPadres, root);
            }
        } catch (Exception e) {
            log.error("Error al inicializar valores", e);
        }
    }

    public void crearNodoRecursivo(List<CatalogoGeneralDTO> objData, TreeNode<CatalogoGeneralDTO> nodoPadre) {
        try {
            for (CatalogoGeneralDTO varnodo : objData) {
                TreeNode<CatalogoGeneralDTO> nodoHijo = new CheckboxTreeNode<CatalogoGeneralDTO>(varnodo, nodoPadre);
                List<CatalogoGeneralDTO> listaHijos = catalogoService.listarDTOsHijosDe(varnodo.getId());
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
            this.catalogoSeleccionado = (CatalogoGeneralDTO) event.getObject().getData();
            if (catalogoSeleccionado != null) {
                catalogoSeleccionado = catalogoService.guardarDesdeDTO(catalogoSeleccionado);
                JsfUtil.addSuccessMessage("Catálogo actualizado!");
                init();
            }
        } catch (Exception e) {
            log.error("Error al actualizar", e);
        }
    }

    @SuppressWarnings("rawtypes")
    public void onRowCancel(RowEditEvent<TreeNode> event) {
        this.catalogoSeleccionado = (CatalogoGeneralDTO) event.getObject().getData();
        JsfUtil.addWarningMessage("Cancelado! " + catalogoSeleccionado.getNombre());
        catalogoSeleccionado = null;
        PrimeFaces.current().ajax().update("frmPersonas:trTblCatalogo");
    }

    public void nuevoCatalogo() {
        this.catalogoSeleccionado = new CatalogoGeneralDTO();
    }

    public void guardarCatalogo() {
        try {
            if (catalogoSeleccionado == null) {
                return;
            }
            boolean esEdicion = catalogoSeleccionado.getId() != null;
            CatalogoGeneralDTO persistido = catalogoService.guardarDesdeDTO(catalogoSeleccionado);
            if (persistido != null) {
                JsfUtil.addSuccessMessage(esEdicion ? "Catálogo actualizado!" : "Catálogo creado!");
            }
            catalogoSeleccionado = null;
            init();
            PrimeFaces.current().executeScript("PF('dlgCatalogo').hide()");
            PrimeFaces.current().ajax().update("frmPersonas:trTblCatalogo");
        } catch (Exception e) {
            log.error("Error al guardar informacion", e);
        }
    }

    public void eliminarCatalogoSeleccionado() {
        try {
            if (catalogoSeleccionado != null && catalogoSeleccionado.getId() != null) {
                catalogoService.eliminarPorId(catalogoSeleccionado.getId());
                JsfUtil.addSuccessMessage("Catálogo eliminado!");
            }
            PrimeFaces.current().ajax().update("frmPersonas:trTblCatalogo", "msgs");
            init();
        } catch (Exception e) {
            log.error("Error al eliminar catalogo", e);
        }
    }
}
