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

    /** Entidad de auditoría — uso interno persistente, no se expone a la vista. */
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
        inicializarAuditoriaAcceso();

        try {
            HttpServletRequest request = JsfUtil.getRequest();
            autenticarEnContenedor(request);
            AuthDataDTO authData = cargarContextoUsuarioAutenticado();
            if (!authData.isResolved()) {
                cerrarAutenticacionIncompleta(request);
                registrarLoginRechazado();
                log.info("=== LOGIN END ===");
                return;
            }
            prepararSesionAutenticada(request);
            redireccionarDespuesDeLogin();
        } catch (Exception e) {
            registrarErrorLogin(e);
        }

        guardarAuditoriaAcceso();
        log.info("=== LOGIN END ===");
    }


    private AuthDataDTO cargarContextoUsuarioAutenticado() {
        prefijoRoles = (String) JsfUtil.getProperty("roles.sitec", true);
        log.info("prefijoRoles resuelto: '{}'", prefijoRoles);

        AuthDataDTO authData = userService.cargarContextoUsuarioAutenticado(loginBean.getUserName(), prefijoRoles);
        listaRolesUsuario = authData.getRolesUsuario();
        listRolesUserString = authData.getNombresRoles();
        this.user = authData.getUsuario();

        log.info("Resultado cargarContextoUsuarioAutenticado -> usuario={}, roles={}, isResolved={}",
                user != null ? user.getUsername() : "null",
                listRolesUserString,
                authData.isResolved());
        return authData;
    }

    private void inicializarAuditoriaAcceso() {
        accessAuditory = new AccessAuditory(loginBean.getUserName(), JsfUtil.getTimestamp(), JsfUtil.getIPAddress());
    }

    private void registrarLoginRechazado() {
        String motivo = obtenerMotivoRechazo();
        log.warn("Login rechazado por isResolved()=false. Motivo: {}", motivo);
        JsfUtil.addErrorMessage("Usuario o contraseÃ±a incorrecto");
        procesoBean.registraActividad("ERROR DE INGRESO AL SISTEMA - " + motivo);
        accessAuditory.setStatus(false);
        guardarAuditoriaAcceso();
    }

    private String obtenerMotivoRechazo() {
        if (user == null) {
            return "Usuario no existe o estÃ¡ inactivo";
        }
        if (listaRolesUsuario == null || listaRolesUsuario.isEmpty()) {
            return "El usuario no tiene roles asignados con prefijo '" + prefijoRoles + "' (ni es Superadmin)";
        }
        return "Datos de autenticaciÃ³n incompletos";
    }

    private void autenticarEnContenedor(HttpServletRequest request) throws Exception {
        if (request.getUserPrincipal() != null) {
            request.logout();
        }
        log.info("Invocando request.login() para '{}'", loginBean.getUserName());
        request.login(loginBean.getUserName(), loginBean.getPassword());
        log.info("request.login() OK");
    }

    private void cerrarAutenticacionIncompleta(HttpServletRequest request) {
        try {
            request.logout();
        } catch (Exception e) {
            log.error("Error cerrando autenticaciÃ³n incompleta para usuario '{}'", loginBean.getUserName(), e);
        }
    }

    private void prepararSesionAutenticada(HttpServletRequest request) {
        HttpSession httpSession = request.getSession();
        loginBean.setRoles(listRolesUserString);
        loginBean.setLoggedIn(true);
        loginBean.setTiempoSession(httpSession.getMaxInactiveInterval());
        loginBean.setUsuario(user);

        accessAuditory.setBrowser(request.getHeader("User-Agent"));
        accessAuditory.setStatus(true);
        accessAuditory.setSession(httpSession.getId());
        accessAuditory.setActive(true);
        httpSession.setAttribute("loginBean", loginBean);
    }

    private void redireccionarDespuesDeLogin() throws Throwable {
        if (Boolean.TRUE.equals(user.getPermanente())) {
            fillMenuModel();
            String destino = resolverDestinoUsuarioPermanente();
            log.info("Redireccionando a {}", destino);
            procesoBean.registraActividad("INGRESA AL SISTEMA CORRECTAMENTE");
            JsfUtil.redirect(destino);
            return;
        }

        cargarPaginasCambioClave();
        log.info("Redireccionando a /cambioClave.jsf");
        JsfUtil.redirect("/cambioClave.jsf");
    }

    private String resolverDestinoUsuarioPermanente() {
        boolean tienePassTemp = Boolean.TRUE.equals(user.getTienePasswordTemporal());
        boolean estadoOk = Boolean.TRUE.equals(user.getEstado());
        if (tienePassTemp || !estadoOk) {
            return "/dashboard.jsf";
        }
        if (loginBean.getRoles().contains(prefijoRoles + Constantes.getRolTecnico())
                || loginBean.getRoles().contains(prefijoRoles + Constantes.getRolPresidenteMesa())) {
            return "/actaE.jsf";
        }
        return "/dashboard.jsf";
    }

    private void registrarErrorLogin(Exception e) {
        log.error("Error durante request.login() o redirect para usuario '{}'", loginBean.getUserName(), e);
        JsfUtil.addErrorMessage("Usuario o contraseÃ±a incorrecto");
        procesoBean.registraActividad("ERROR DE INGRESO AL SISTEMA");
        loginBean.setUserName("");
        loginBean.setPassword("");
        accessAuditory.setStatus(false);
    }

    private void guardarAuditoriaAcceso() {
        try {
            accessService.create(accessAuditory);
        } catch (Exception e) {
            log.error("Error guardando AccessAuditory", e);
            procesoBean.registraActividad("ERROR DE INGRESO AL SISTEMA");
        }
    }

    public void fillMenuModel() throws Throwable {
        String mnemonic = (String) JsfUtil.getProperty("roles.mnemonic", true);
        List<MenuVO> menus = menuService.getMenusByrolsDTO(listaRolesUsuario, mnemonic);

        JsfUtil.cargarObjetoSession("listaPermisos", menuService.extraerPaginasPermitidas(menus));

        if (menus == null) {
            JsfUtil.addErrorMessage("Error al generar el menú con los roles de Usuario");
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
