package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.domain.Menu;
import ec.com.antenasur.domain.MenuRol;
import ec.com.antenasur.domain.Rol;
import ec.com.antenasur.service.MenuFacade;
import ec.com.antenasur.service.MenuRolFacade;
import ec.com.antenasur.service.RolFacade;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;
import org.primefaces.model.CheckboxTreeNode;
import org.primefaces.model.TreeNode;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 * @fecha 10-08-2022 15:53
 */
@Named
@ViewScoped
@Slf4j
public class PermisosController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private LoginBean loginBean;

    @Inject
    private MenuRolFacade menuRolFacade;

    @Inject
    private RolFacade rolFacade;

    @Inject
    private MenuFacade menuFacade;

    @Setter
    @Getter
    private MenuRol menuRolSeleccionado;

    @Setter
    @Getter
    private List<MenuRol> listaMenuRoles, listaMenuRolHijos;

    @Getter
    private Rol rolSeleccionado;

    @Setter
    @Getter
    private List<Rol> roles;

    @Setter
    @Getter
    private List<Menu> listaMenuPadres, listaMenuHijos;

    @Setter
    @Getter
    private TreeNode<MenuRol> root;

    @PostConstruct
    private void init() {
        try {
            rolSeleccionado = new Rol();
            this.listaMenuPadres = menuFacade.findByFather();

            roles = rolFacade.findAll();
        } catch (Exception e) {
            log.error("Error al inicializar valores", e);
        }
    }

    public void crearNodoRecursivo(List<MenuRol> ObjData, TreeNode<MenuRol> nodoPadre) {
        try {
            for (MenuRol varnodo : ObjData) {//ObjData=Hijos del nodo padre, Lista de segundo nivel
                TreeNode<MenuRol> nodoHijo = new CheckboxTreeNode<MenuRol>(varnodo, nodoPadre);
                List<Menu> listaHijos = menuFacade.listaCatalogoHijo(varnodo.getMenu().getId());
                listaMenuRolHijos = new ArrayList<>();
                if (listaHijos != null) {
                    for (Menu menu : listaHijos) {
                        MenuRol menuRolBuscado = menuRolFacade.getPorMenuYRol(menu, rolSeleccionado);
                        if (menuRolBuscado != null) {
                            this.listaMenuRolHijos.add(menuRolBuscado);
                        } else {
                            MenuRol tmpMenuRol = new MenuRol(menu, rolSeleccionado);
                            tmpMenuRol.setEstado(false);
                            this.listaMenuRolHijos.add(tmpMenuRol);
                        }
                    }
                    if (listaMenuRolHijos != null) {
                        if (!listaMenuRolHijos.isEmpty()) {
                            this.crearNodoRecursivo(listaMenuRolHijos, nodoHijo);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("ERROR EN CREAR NODO RECURSIVO", e);
        }
    }

    public void setRolSeleccionado(Rol rol) {
        try {
            rolSeleccionado = rolFacade.find(rol.getId());
        } catch (Exception e) {
        }
    }

    public void obtieneMenuPorRol() {
        try {
            if (this.rolSeleccionado != null) {
                setRolSeleccionado(this.rolSeleccionado);
                this.listaMenuRoles = new ArrayList<>();

                for (Menu menu : listaMenuPadres) {
                    MenuRol menuRolBuscado = menuRolFacade.getPorMenuYRol(menu, rolSeleccionado);
                    if (menuRolBuscado != null) {
                        this.listaMenuRoles.add(menuRolBuscado);
                    } else {
                        MenuRol tmpMenuRol = new MenuRol(menu, rolSeleccionado);
                        tmpMenuRol.setEstado(false);
                        this.listaMenuRoles.add(tmpMenuRol);
                    }
                }
                this.root = new CheckboxTreeNode<MenuRol>(listaMenuRoles.get(0), null);
                crearNodoRecursivo(listaMenuRoles, root);
            }
        } catch (Exception e) {
            log.error("ERROR EN CREAR CREAR ARBOL DE OPCIONES", e);
        }
    }

    public void guardarMenuRol(MenuRol menuRol) {
        try {
            if (menuRol != null && rolSeleccionado != null) {
                menuRolSeleccionado = new MenuRol();
                menuRolSeleccionado = menuRol;                
                if (menuRolSeleccionado.getId()!=null && menuRolSeleccionado.getId() > 0) {
                    menuRolSeleccionado = menuRolFacade.edit(menuRolSeleccionado);
                    JsfUtil.addInfoMessage("PERMISOS ACTUALIZADOS");
                } else {
                    menuRolSeleccionado = menuRolFacade.create(menuRol);
                    JsfUtil.addSuccessMessage(menuRolSeleccionado.getMenu().getNombre() + " ASIGNADO A " + menuRolSeleccionado.getRol().getNombre());
                }
            }
            this.init();
            PrimeFaces.current().ajax().update("frmPermisos:trTblCatalogo");
        } catch (Exception e) {
            log.error("ERROR AL GUARDAR ASIGNACION DE PERMISOS", e);
        }
    }

}
