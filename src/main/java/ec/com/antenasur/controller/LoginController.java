package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.DefaultMenuModel;
import org.primefaces.model.menu.DefaultSubMenu;
import org.primefaces.model.menu.MenuModel;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.bean.ProcesoBean;
import ec.com.antenasur.dto.AuthDataDTO;
import ec.com.antenasur.dto.PersonaDTO;
import ec.com.antenasur.dto.RolUsuarioDTO;
import ec.com.antenasur.dto.UsuarioDTO;
import ec.com.antenasur.model.AccessAuditory;
import ec.com.antenasur.service.AccessService;
import ec.com.antenasur.service.MenuService;
import ec.com.antenasur.service.UsuarioService;
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
    private UsuarioService userService;

    @Inject
    private MenuService menuService;

    @Inject
    private AccessService accessService;

    @Inject
    ProcesoBean procesoBean;

    @Setter
    @Getter
    private List<RolUsuarioDTO> listaRolesUsuario = new ArrayList<>();

    @Setter
    @Getter
    private List<String> listRolesUserString = new ArrayList<>();

    @Setter
    @Getter
    private List<String> listRolesTem = new ArrayList<>();

    @Setter
    @Getter
    private UsuarioDTO user = new UsuarioDTO();

    @Setter
    private PersonaDTO people;

    /** Entidad de auditorÃ­a â€” uso interno persistente, no se expone a la vista. */
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

    @PostConstruct
    private void init() {
        try {
            String paramLoginBean = JsfUtil.getRequestParameter("loginBean");
            if (paramLoginBean != null && !paramLoginBean.isEmpty()
                    && !Boolean.parseBoolean(paramLoginBean)) {
                HttpServletRequest request = JsfUtil.getRequest();
                if (request.getUserPrincipal() != null) {
                    request.logout();
                }
            } else if (loginBean != null && loginBean.getUserName() != null && loginBean.isLoggedIn()
                    && loginBean.getUsuario() != null) {
                if (Boolean.TRUE.equals(loginBean.getUsuario().getPermanente())) {
                    JsfUtil.redirect("/dashboard.jsf");
                } else {
                    JsfUtil.redirect("/pages/login/firstLogin.jsf");
                }
            }
        } catch (Exception e) {
            log.error("Error en init() de LoginController", e);
        }
    }

    public void login() throws Throwable {
        log.info("=== LOGIN START === userName='{}'", loginBean != null ? loginBean.getUserName() : "null");
        prefijoRoles = (String) JsfUtil.getProperty("roles.sitec", true);
        log.info("prefijoRoles resuelto: '{}'", prefijoRoles);

        AuthDataDTO authData = userService.resolverDatosAutenticacion(loginBean.getUserName(), prefijoRoles);
        listaRolesUsuario = authData.getRolesUsuario();
        listRolesUserString = authData.getNombresRoles();
        this.user = authData.getUsuario();
        this.people = authData.getPersona();

        log.info("Resultado resolverDatosAutenticacion -> usuario={}, roles={}, isResolved={}",
                user != null ? user.getUsername() : "null",
                listRolesUserString,
                authData.isResolved());

        accessAuditory = new AccessAuditory(loginBean.getUserName(), JsfUtil.getTimestamp(), JsfUtil.getIPAddress());

        if (!authData.isResolved()) {
            // Diagnóstico explícito: antes era fallo silencioso
            String motivo;
            if (user == null) {
                motivo = "Usuario no existe o está inactivo";
            } else if (listaRolesUsuario == null || listaRolesUsuario.isEmpty()) {
                motivo = "El usuario no tiene roles asignados con prefijo '" + prefijoRoles + "' (ni es Superadmin)";
            } else {
                motivo = "Datos de autenticación incompletos";
            }
            log.warn("Login rechazado por isResolved()=false. Motivo: {}", motivo);
            JsfUtil.addErrorMessage("Usuario o contraseña incorrecto");
            procesoBean.registraActividad("ERROR DE INGRESO AL SISTEMA - " + motivo);
            try {
                accessAuditory.setStatus(false);
                accessService.create(accessAuditory);
            } catch (Exception ignored) { }
            return;
        }

        try {
            HttpServletRequest request = JsfUtil.getRequest();
            HttpSession httpSession = request.getSession(false);

            if (request.getUserPrincipal() != null) {
                request.logout();
            }
            log.info("Invocando request.login() para '{}'", loginBean.getUserName());
            request.login(loginBean.getUserName(), loginBean.getPassword());
            log.info("request.login() OK");

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

            if (Boolean.TRUE.equals(user.getPermanente())) {
                fillMenuModel();
                boolean tienePassTemp = Boolean.TRUE.equals(user.getTienePasswordTemporal());
                boolean estadoOk = Boolean.TRUE.equals(user.getEstado());
                String destino;
                if (!tienePassTemp && estadoOk) {
                    if (loginBean.getRoles().contains(prefijoRoles + Constantes.getRolTecnico())) {
                        destino = "/actaE.jsf";
                    } else {
                        destino = "/dashboard.jsf";
                    }
                } else {
                    destino = "/dashboard.jsf";
                }
                log.info("Redireccionando a {}", destino);
                procesoBean.registraActividad("INGRESA AL SISTEMA CORRECTAMENTE");
                JsfUtil.redirect(destino);
            } else {
                cargarPaginasCambioClave();
                log.info("Redireccionando a /cambioClave.jsf");
                JsfUtil.redirect("/cambioClave.jsf");
            }
        } catch (Exception e) {
            log.error("Error durante request.login() o redirect para usuario '{}'", loginBean.getUserName(), e);
            JsfUtil.addErrorMessage("Usuario o contraseña incorrecto");
            procesoBean.registraActividad("ERROR DE INGRESO AL SISTEMA");
            loginBean.setUserName("");
            loginBean.setPassword("");
            accessAuditory.setStatus(false);
        }

        try {
            accessService.create(accessAuditory);
        } catch (Exception e) {
            log.error("Error guardando AccessAuditory", e);
            procesoBean.registraActividad("ERROR DE INGRESO AL SISTEMA");
        }
        log.info("=== LOGIN END ===");
    }

    public void fillMenuModel() throws Throwable {
        String mnemonic = (String) JsfUtil.getProperty("roles.mnemonic", true);
        List<MenuVO> menus = menuService.getMenusByrolsDTO(listaRolesUsuario, mnemonic);

        JsfUtil.cargarObjetoSession("listaPermisos", menuService.extraerPaginasPermitidas(menus));

        if (menus == null) {
            JsfUtil.addErrorMessage("Error al generar el menÃº con los roles de Usuario");
            loginBean.logout();
            return;
        }

        menuModel = new DefaultMenuModel();
        ec.com.antenasur.model.Menu parentMenu = menuService.findByMenuName(mnemonic);

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

    private void cargarPaginasCambioClave() {
        List<String> listaPaginas = new ArrayList<>();
        listaPaginas.add("cambioClave.jsf");
        JsfUtil.cargarObjetoSession("listaPermisos", listaPaginas);
    }

    private DefaultMenuItem fillItems(MenuVO menu_, List<MenuVO> menus, DefaultSubMenu menuParent,
            DefaultMenuItem menuItem) {
        for (MenuVO menu : menus) {
            if (menu_.getIdMenu().equals(menu.getIdMenuParent())) {
                if (menu.getEndNode()) {
                    DefaultMenuItem menuItem_ = new DefaultMenuItem();
                    menuItem_.setValue(menu.getLabelMenu());
                    menuItem_.setUrl(("S/N").equals(menu.getUrlMenu()) ? null : menu.getUrlMenu());
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
