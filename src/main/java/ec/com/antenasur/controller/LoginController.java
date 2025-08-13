package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.DefaultMenuModel;
import org.primefaces.model.menu.DefaultSubMenu;
import org.primefaces.model.menu.MenuModel;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.bean.ProcesoBean;
import ec.com.antenasur.service.AccessFacade;
import ec.com.antenasur.service.MenuFacade;
import ec.com.antenasur.service.PersonaFacade;
import ec.com.antenasur.service.RolFacade;
import ec.com.antenasur.service.RolUsuarioFacade;
import ec.com.antenasur.service.UsuarioFacade;
import ec.com.antenasur.domain.AccessAuditory;
import ec.com.antenasur.domain.Menu;
import ec.com.antenasur.domain.Persona;
import ec.com.antenasur.domain.RolUsuario;
import ec.com.antenasur.domain.Usuario;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.JsfUtil;
import ec.com.antenasur.util.MenuVO;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Named
@RequestScoped
@Slf4j
public class LoginController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private LoginBean loginBean;

    @Inject
    private RolUsuarioFacade roleUserFacade;

    @Inject
    private RolFacade rolFacade;

    @Inject
    private UsuarioFacade userFacade;

    @Inject
    private PersonaFacade peopleFacade;

    @Inject
    private MenuFacade menuFacade;

    @Inject
    private AccessFacade accessFacade;

    @Setter
    @Getter
    private List<RolUsuario> listaRolesUsuario = new ArrayList<RolUsuario>();

    @Setter
    @Getter
    private List<String> listRolesUserString = new ArrayList<>(), listRolesTem = new ArrayList<>();

    @Setter
    @Getter
    private List<Menu> menus = new ArrayList<Menu>();

    @Setter
    @Getter
    private Usuario user = new Usuario();

    @Setter
    private Persona people;

    @Setter
    private AccessAuditory accessAuditory = new AccessAuditory();

    private MenuModel menuModel;

    @Setter
    @Getter
    private String email;

    @Setter
    @Getter
    private String typeElement = "password";

    @Setter
    @Getter
    private String prefijoRoles;

    @Inject
    ProcesoBean procesoBean;

    @PostConstruct
    private void init() {
        try {
            if (JsfUtil.getRequestParameter("loginBean") != null && JsfUtil.getRequestParameter("loginBean") != ""
                    && !Boolean.parseBoolean(JsfUtil.getRequestParameter("loginBean"))) {
                HttpServletRequest request = JsfUtil.getRequest();
                if (request.getUserPrincipal() != null) {
                    request.logout();
                }
            } else if (loginBean.getUserName() != null && loginBean.isLoggedIn()) {
                if (loginBean.getUsuario().getPermanente()) {
                    JsfUtil.redirect("/dashboard.jsf");
                } else {
                    JsfUtil.redirect("/pages/login/firstLogin.jsf");
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void login() throws Throwable {
        listaRolesUsuario = new ArrayList<RolUsuario>();
        prefijoRoles = (String) JsfUtil.getProperty("roles.sitec", true);
        if (prefijoRoles != null) {
            listaRolesUsuario = roleUserFacade.findByUserNameAndRoleName2(loginBean.getUserName(), prefijoRoles + "%");
        }

        accessAuditory = new AccessAuditory(loginBean.getUserName(), JsfUtil.getTimestamp(), JsfUtil.getIPAddress());

        if (listaRolesUsuario != null) {

            try {
                for (RolUsuario ur : listaRolesUsuario) {
                    listRolesUserString.add(ur.getRol().getNombre());
                }
                getUsuario();
                getPersona();
                HttpServletRequest request = JsfUtil.getRequest();
                HttpSession httpSession = request.getSession(false);

                if (request.getUserPrincipal() != null) {
                    request.logout();
                }
                request.login(loginBean.getUserName(), loginBean.getPassword());

                loginBean.setRoles(listRolesUserString);
                loginBean.setLoggedIn(true);
                loginBean.setTiempoSession(request.getSession().getMaxInactiveInterval());

                loginBean.setUsuario(user);
                loginBean.setPersona(people);

                accessAuditory.setBrowser(request.getHeader("User-Agent"));
                accessAuditory.setStatus(true);
                accessAuditory.setSession(httpSession.getId());
                accessAuditory.setActive(true);
                httpSession.setAttribute("loginBean", loginBean);
                if (user.getPermanente()) {
                    fillMenuModel();

                    if (user.getContraseniaTemp() == null && user.getPermanente() == true && user.getEstado() == true) {
                    	if(loginBean.getRoles().contains(prefijoRoles+Constantes.getRolTecnico())) {
                    		JsfUtil.redirect("/actaE.jsf");
                            procesoBean.registraActividad("INGRESA AL SISTEMA CORRECTAMENTE");
                    	}else {
                    		JsfUtil.redirect("/dashboard.jsf");
                            procesoBean.registraActividad("INGRESA AL SISTEMA CORRECTAMENTE");	
                    	}
                        
                    } else {
                        JsfUtil.redirect("/dashboard.jsf");
                        procesoBean.registraActividad("INGRESA AL SISTEMA CORRECTAMENTE");

                    }
                } else {
                    cargarPaginasCambioClave();
                    JsfUtil.redirect("/cambioClave.jsf");
                }
            } catch (Exception e) {
                JsfUtil.addErrorMessage("Usuario o contraseña incorrecto");
                procesoBean.registraActividad("ERROR DE INGRESO AL SISTEMA");
                loginBean.setUserName("");
                loginBean.setPassword("");

            }
        }
        try {
            accessFacade.create(accessAuditory);
        } catch (Exception e) {
            procesoBean.registraActividad("ERROR DE INGRESO AL SISTEMA");
        }

    }

    private void getUsuario() {
        try {
            if (loginBean.getUserName() != null) {
                this.user = userFacade.findByUsuarioName(loginBean.getUserName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getPersona() {
        try {
            if (user != null) {
                this.people = peopleFacade.finByPersonaDocument(user.getPersonsa().getDocumento());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fillMenuModel() throws Throwable { // String mnemonic = (String)
        String mnemonic = (String) JsfUtil.getProperty("roles.mnemonic", true);
        /*
        for (RolUsuario rolUsuario : listaRolesUsuario) {
            if (!rolUsuario.getRol().getNombre().startsWith(prefijoRoles)) {
                Rol r = rolFacade.findByName(prefijoRoles + rolUsuario.getRol().getNombre());
                if (r != null) {
                    rolUsuario.setRol(r);
                } else {
                    System.out.println("No se encontró el rol " + prefijoRoles + rolUsuario.getRol().getNombre()
                            + " para generar el menú");
                    loginBean.logout();
                    return;
                }
            }
        }
         */
        List<MenuVO> menus = menuFacade.getMenusByrols(listaRolesUsuario, mnemonic);

        cargarPaginasSession(menus);

        if (menus == null) {
            System.out.println("Error al generar el menú con los roles de Usuario");
            JsfUtil.addErrorMessage("Error al generar el menú con los roles de Usuario");
            loginBean.logout();
            return;
        }

        menuModel = new DefaultMenuModel();

        Menu parentMenu = menuFacade.findByMenuName(mnemonic);

        for (MenuVO menu : menus) {
            if (menu.getIdMenuParent() != null && menu.getIdMenuParent().equals(parentMenu.getId())) {
                if (!menu.getEndNode()) {
                    DefaultSubMenu subMenu = new DefaultSubMenu();
                    subMenu.setId(menu.getComponenteId());
                    subMenu.setLabel(menu.getLabelMenu());
                    subMenu.setIcon(menu.getIcon());
                    fillItems(menu, menus, subMenu, null);
                    menuModel.getElements().add(subMenu);

                } else {
                    DefaultMenuItem menuItem_ = new DefaultMenuItem();
                    menuItem_.setId(menu.getComponenteId());
                    menuItem_.setValue(menu.getLabelMenu());
                    menuItem_.setIcon(menu.getIcon());
                    menuItem_.setOutcome(menu.getActionMenu());
                    menuItem_.setUrl(("S/N").equals(menu.getUrlMenu()) ? null : menu.getUrlMenu());
                    menuItem_.setCommand(menu.getActionMenu() == null || menu.getActionMenu().isEmpty() ? null
                            : menu.getActionMenu());

                    menuModel.getElements().add(menuItem_);
                }
            }
        }
        loginBean.setMenuModel(menuModel);
    }

    private void cargarPaginasSession(final List<MenuVO> listaPermisos) {
        List<String> listaPaginas = new ArrayList<String>();
        for (MenuVO en : listaPermisos) {
            listaPaginas.add(extraerPagina(en.getUrlMenu()));
        }
        listaPaginas.add("inicio.jsf");
        listaPaginas.add("cambioClave.jsf");
        JsfUtil.cargarObjetoSession("listaPermisos", listaPaginas);
    }

    private void cargarPaginasCambioClave() {
        List<String> listaPaginas = new ArrayList<String>();
        listaPaginas.add("cambioClave.jsf");
        JsfUtil.cargarObjetoSession("listaPermisos", listaPaginas);
    }

    private String extraerPagina(String url) {
        StringTokenizer str = new StringTokenizer(url == null ? "" : url, "/");
        String retorno = null;
        while (str.hasMoreTokens()) {
            retorno = str.nextToken();
        }
        return retorno;
    }

    private DefaultMenuItem fillItems(MenuVO menu_, List<MenuVO> menus, DefaultSubMenu menuParent,
            DefaultMenuItem menuItem) {
        for (MenuVO menu : menus) {
            if (menu_.getIdMenu().equals(menu.getIdMenuParent())) {
                if (menu.getEndNode()) {
                    DefaultMenuItem menuItem_ = new DefaultMenuItem();
                    menuItem_.setValue(menu.getLabelMenu());
                    boolean result = false;//
                    if (!result) {
                        menuItem_.setUrl(("S/N").equals(menu.getUrlMenu()) ? null : menu.getUrlMenu());
                    } else {
                        menuItem_.setUrl("/pages/application/pendingReport.xhtml");
                    }
                    menuItem_.setCommand(menu.getActionMenu() == null || menu.getActionMenu().isEmpty() ? null
                            : menu.getActionMenu());
                    menuItem_.setIcon(menu.getIcon());
                    menuParent.getElements().add(menuItem_);
                } else {
                    addChildElement(menuParent, menu, menus, menuItem);
                }
            }
        }
        return menuItem;
    }

    private void addChildElement(DefaultSubMenu menuParent, MenuVO menu_, List<MenuVO> menus,
            DefaultMenuItem menuItem) {
        DefaultSubMenu submenuChild = new DefaultSubMenu();
        submenuChild.setLabel(menu_.getLabelMenu());
        menuParent.getElements().add(submenuChild);
        submenuChild.setIcon(menu_.getIcon());
        DefaultMenuItem menus_ = fillItems(menu_, menus, submenuChild, menuItem);
        if (menus_ != null) {
            submenuChild.getElements().add(menus_);
        }
    }

}
