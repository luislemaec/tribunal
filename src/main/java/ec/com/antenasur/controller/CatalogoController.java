package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.primefaces.PrimeFaces;
import org.primefaces.event.RowEditEvent;
import org.primefaces.model.CheckboxTreeNode;
import org.primefaces.model.TreeNode;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.dto.CatalogoGeneralDTO;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.service.tec.CatalogoGeneralService;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Named
@ViewScoped
@Slf4j
public class CatalogoController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String FORMULARIO = "frmCatalogos";
    private static final String TABLA = FORMULARIO + ":trTblCatalogo";
    private static final String PANEL_DIALOGO = FORMULARIO + ":outPnlCatalogo";
    private static final String GROWL_GLOBAL = JsfUtil.GROWL_MESSAGES;

    @Inject
    private LoginBean loginBean;

    @Inject
    private CatalogoGeneralService catalogoService;

    @Setter
    @Getter
    private CatalogoGeneralDTO catalogoSeleccionado;

    @Setter
    @Getter
    private List<CatalogoGeneralDTO> listaCatalogoPadres;

    @Setter
    @Getter
    private List<CatalogoGeneralDTO> listaCatalogoHijos;

    @Setter
    private List<CatalogoGeneralDTO> catalogosPadreDisponibles;

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
        cargarArbolCatalogos();
    }

    public void crearNodoRecursivo(List<CatalogoGeneralDTO> objData, TreeNode<CatalogoGeneralDTO> nodoPadre) {
        if (objData == null || objData.isEmpty() || nodoPadre == null) {
            return;
        }
        for (CatalogoGeneralDTO varnodo : objData) {
            TreeNode<CatalogoGeneralDTO> nodoHijo = new CheckboxTreeNode<>(varnodo, nodoPadre);
            List<CatalogoGeneralDTO> listaHijos = catalogoService.listarDTOsHijosDe(varnodo.getId());
            if (listaHijos != null && !listaHijos.isEmpty()) {
                crearNodoRecursivo(listaHijos, nodoHijo);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    public void onRowEdit(RowEditEvent<TreeNode> event) {
        try {
            validarPermisoAdministracion();
            this.catalogoSeleccionado = (CatalogoGeneralDTO) event.getObject().getData();
            if (catalogoSeleccionado == null) {
                throw new NegocioException("catalogo.mensaje.no.seleccionado");
            }
            catalogoService.guardarDesdeDTO(catalogoSeleccionado);
            JsfUtil.addSuccessMessageFromBundle("catalogo.mensaje.actualizado");
        } catch (NegocioException e) {
            manejarErrorNegocio(e);
        } catch (Exception e) {
            log.error("Error al actualizar catalogo", e);
            marcarValidacionFallida();
            JsfUtil.addErrorMessageFromBundle("catalogo.mensaje.error");
        } finally {
            catalogoSeleccionado = null;
            cargarArbolCatalogos();
            PrimeFaces.current().ajax().update(TABLA, GROWL_GLOBAL);
        }
    }

    @SuppressWarnings("rawtypes")
    public void onRowCancel(RowEditEvent<TreeNode> event) {
        this.catalogoSeleccionado = (CatalogoGeneralDTO) event.getObject().getData();
        JsfUtil.addWarningMessageFromBundle("catalogo.mensaje.edicion.cancelada",
                catalogoSeleccionado != null ? catalogoSeleccionado.getNombre() : "");
        catalogoSeleccionado = null;
        PrimeFaces.current().ajax().update(GROWL_GLOBAL);
    }

    public void nuevoCatalogo() {
        try {
            validarPermisoAdministracion();
            this.catalogoSeleccionado = new CatalogoGeneralDTO();
        } catch (NegocioException e) {
            manejarErrorNegocio(e);
        }
    }

    public void guardarCatalogo() {
        try {
            validarPermisoAdministracion();
            if (catalogoSeleccionado == null) {
                throw new NegocioException("catalogo.mensaje.no.seleccionado");
            }
            boolean esEdicion = catalogoSeleccionado.getId() != null;
            catalogoService.guardarDesdeDTO(catalogoSeleccionado);
            JsfUtil.addSuccessMessageFromBundle(esEdicion
                    ? "catalogo.mensaje.actualizado"
                    : "catalogo.mensaje.creado");
            catalogoSeleccionado = null;
            cargarArbolCatalogos();
        } catch (NegocioException e) {
            manejarErrorNegocio(e);
        } catch (Exception e) {
            log.error("Error al guardar catalogo", e);
            marcarValidacionFallida();
            JsfUtil.addErrorMessageFromBundle("catalogo.mensaje.error");
        } finally {
            PrimeFaces.current().ajax().update(PANEL_DIALOGO, TABLA, GROWL_GLOBAL);
        }
    }

    public void eliminarCatalogoSeleccionado() {
        try {
            validarPermisoAdministracion();
            if (catalogoSeleccionado == null || catalogoSeleccionado.getId() == null) {
                throw new NegocioException("catalogo.mensaje.no.seleccionado");
            }
            catalogoService.deshabilitarPorId(catalogoSeleccionado.getId());
            JsfUtil.addSuccessMessageFromBundle("catalogo.mensaje.deshabilitado");
            catalogoSeleccionado = null;
            cargarArbolCatalogos();
        } catch (NegocioException e) {
            manejarErrorNegocio(e);
        } catch (Exception e) {
            log.error("Error al deshabilitar catalogo", e);
            marcarValidacionFallida();
            JsfUtil.addErrorMessageFromBundle("catalogo.mensaje.error");
        } finally {
            PrimeFaces.current().ajax().update(TABLA, GROWL_GLOBAL);
        }
    }

    public void cancelarDialogo() {
        catalogoSeleccionado = null;
        PrimeFaces.current().ajax().update(PANEL_DIALOGO);
    }

    public boolean puedeAdministrarCatalogos() {
        return tieneRol(Constantes.getRolAdministrador()) || tieneRol(Constantes.getRolTribunal());
    }

    public List<CatalogoGeneralDTO> getCatalogosPadreDisponibles() {
        if (catalogosPadreDisponibles == null) {
            return Collections.emptyList();
        }
        if (catalogoSeleccionado == null || catalogoSeleccionado.getId() == null) {
            return catalogosPadreDisponibles;
        }
        List<CatalogoGeneralDTO> disponibles = new ArrayList<>();
        for (CatalogoGeneralDTO catalogo : catalogosPadreDisponibles) {
            if (catalogo != null && !catalogoSeleccionado.getId().equals(catalogo.getId())) {
                disponibles.add(catalogo);
            }
        }
        return disponibles;
    }

    private void cargarArbolCatalogos() {
        try {
            listaCatalogoPadres = catalogoService.listarDTOsPorPadre();
            catalogosPadreDisponibles = catalogoService.listarDTOs();
            root = new CheckboxTreeNode<>(new CatalogoGeneralDTO(), null);
            crearNodoRecursivo(listaCatalogoPadres, root);
        } catch (Exception e) {
            log.error("Error al inicializar catalogos", e);
            root = new CheckboxTreeNode<>(new CatalogoGeneralDTO(), null);
            catalogosPadreDisponibles = Collections.emptyList();
            JsfUtil.addErrorMessageFromBundle("catalogo.mensaje.error");
        }
    }

    private void validarPermisoAdministracion() {
        if (!puedeAdministrarCatalogos()) {
            throw new NegocioException("catalogo.mensaje.sin.permiso");
        }
    }

    private boolean tieneRol(String rolCorto) {
        if (loginBean == null || loginBean.getRoles() == null || rolCorto == null) {
            return false;
        }
        String prefijo = JsfUtil.getProperty("roles.sitec", true);
        return loginBean.getRoles().contains((prefijo == null ? "" : prefijo) + rolCorto);
    }

    private void manejarErrorNegocio(NegocioException e) {
        marcarValidacionFallida();
        JsfUtil.addWarningMessageFromBundle(e.getMessage());
    }

    private void marcarValidacionFallida() {
        FacesContext.getCurrentInstance().validationFailed();
        PrimeFaces.current().ajax().addCallbackParam("validationFailed", true);
    }
}
