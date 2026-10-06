package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.DefaultMenuModel;
import org.primefaces.model.menu.DefaultSubMenu;
import org.primefaces.model.menu.MenuElement;
import org.primefaces.model.menu.MenuGroup;
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
    private ec.com.antenasur.security.menu.AutorizacionMenuService autorizacionMenu;

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
            } else if (!FacesContext.getCurrentInstance().isPostback()
                    && loginBean != null && loginBean.getUserName() != null && loginBean.isLoggedIn()
                    && loginBean.getUsuario() != null) {
                if (Boolean.TRUE.equals(loginBean.getUsuario().getPermanente())) {
                    JsfUtil.redirect("/dashboard.jsf");
                } else {
                    JsfUtil.redirect("/cambioClave.jsf");
                }
                return;
            }
            // La sesión ya fue invalidada por LoginBean.cerrarSessionExpirada()
            // antes de esta redirección; aquí solo se informa al usuario en la
            // misma carga del formulario de login, sin depender de flash scope
            // (que vive en la sesión que acabamos de destruir).
            if (!FacesContext.getCurrentInstance().isPostback()
                    && "1".equals(JsfUtil.getRequestParameter("expirado"))) {
                JsfUtil.addWarningMessageFromBundle("msg.session.expired");
            }
        } catch (Exception e) {
            log.error("Error en init() de LoginController", e);
        }
    }

    public void login() {
        if (loginBean.getUserName() != null && loginBean.getUserName().startsWith(
                ec.com.antenasur.security.qr.ConfiguracionQr.PREFIJO)) {
            loginBean.setPassword(null);
            JsfUtil.addErrorMessageFromBundle("actaQr.error.acceso");
            return;
        }
        HttpServletRequest request = JsfUtil.getRequest();
        if (!iniciarAutenticacion(request)) {
            return;
        }

        try {
            inicializarAuditoriaAcceso();
            try {
                autenticarEnContenedor(request);
            } catch (Exception e) {
                // Solo request.login() puede convertir el error en credenciales inválidas.
                registrarErrorLogin(e);
                guardarAuditoriaAcceso();
                return;
            }

            try {
                AuthDataDTO authData = cargarContextoUsuarioAutenticado();
                if (!authData.isResolved()) {
                    cerrarAutenticacionIncompleta(request);
                    registrarLoginRechazado();
                    return;
                }
                prepararSesionAutenticada(request);
                guardarAuditoriaAcceso();
            } catch (Throwable e) {
                registrarErrorPosteriorAutenticacion(e);
                return;
            }

            try {
                redireccionarDespuesDeLogin();
            } catch (Throwable e) {
                registrarErrorRedireccion(e);
            }
        } finally {
            loginBean.setAutenticacionEnCurso(false);
            log.info("=== LOGIN END ===");
        }
    }

    private boolean iniciarAutenticacion(HttpServletRequest request) {
        synchronized (loginBean) {
            if (loginBean.isAutenticacionEnCurso()) {
                log.warn("Se ignoró un segundo envío de login para '{}': autenticación en curso", loginBean.getUserName());
                return false;
            }
            if (loginBean.isLoggedIn() && request.getUserPrincipal() != null) {
                log.warn("Se ignoró un segundo envío de login para '{}' ya autenticado", loginBean.getUserName());
                return false;
            }
            loginBean.setAutenticacionEnCurso(true);
            return true;
        }
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
        JsfUtil.addErrorMessageFromBundle("msg.invalid.credentials");
        procesoBean.registraLoginFallido(loginBean.getUserName());
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
        try {
            request.login(loginBean.getUserName(), loginBean.getPassword());
            log.info("request.login() OK");
        } finally {
            loginBean.setPassword(null);
        }
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
        registrarActividadInicioSesionSinInterrumpir();
        if (Boolean.TRUE.equals(user.getPermanente())) {
            fillMenuModel();
            // fillMenuModel puede cerrar la sesión y redirigir cuando no hay
            // menú disponible. No se debe emitir una segunda redirección.
            if (FacesContext.getCurrentInstance().getResponseComplete()) {
                return;
            }
            String destino = resolverDestinoUsuarioPermanente();
            log.info("Redireccionando a {}", destino);
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
        log.warn("Autenticación rechazada para usuario '{}'", loginBean.getUserName());
        JsfUtil.addErrorMessageFromBundle("msg.invalid.credentials");
        procesoBean.registraLoginFallido(loginBean.getUserName());
        loginBean.setUserName("");
        loginBean.setPassword("");
        accessAuditory.setStatus(false);
    }

    private void registrarErrorPosteriorAutenticacion(Throwable e) {
        log.error("Autenticación correcta, pero falló una operación posterior para usuario '{}'",
                loginBean.getUserName(), e);
        JsfUtil.addErrorMessageFromBundle("msg.login.postauth.error");
    }

    private void registrarErrorRedireccion(Throwable e) {
        log.error("La autenticación fue correcta, pero no se pudo completar la redirección para usuario '{}'",
                loginBean.getUserName(), e);
        if (!FacesContext.getCurrentInstance().getResponseComplete()) {
            JsfUtil.addErrorMessageFromBundle("msg.login.postauth.error");
        }
    }

    private void registrarActividadInicioSesionSinInterrumpir() {
        try {
            procesoBean.registraActividad("LOGIN | MÓDULO: ACCESO; RESULTADO: EXITOSO; DETALLE: Inicio de sesión");
        } catch (Exception e) {
            log.error("No se pudo registrar la auditoría posterior a request.login() OK", e);
        }
    }

    private void guardarAuditoriaAcceso() {
        try {
            accessService.create(accessAuditory);
        } catch (Exception e) {
            log.error("Error guardando AccessAuditory", e);
        }
    }

    public void fillMenuModel() throws Throwable {
        String mnemonic = (String) JsfUtil.getProperty("roles.mnemonic", true);
        List<MenuVO> menus = autorizacionMenu.menusActuales();

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
                    subMenu.setId(idEstable(menu));
                    subMenu.setLabel(menu.getLabelMenu());
                    subMenu.setIcon(menu.getIcon());
                    fillItems(menu, menus, subMenu, null);
                    menuModel.getElements().add(subMenu);
                } else {
                    DefaultMenuItem menuItem_ = new DefaultMenuItem();
                    menuItem_.setId(idEstable(menu));
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
        completarIdsEstables(menuModel.getElements(), null);
        loginBean.inicializarMenuAutorizado(menuModel);
    }

    /** Prefijo de los ids de menú derivados del id de la opción en BD. */
    private static final String PREFIJO_ID_MENU = "tec";

    /**
     * Id de la opción de menú: el componente configurado en BD o, si no tiene, uno
     * derivado del id de la opción. Sin id, PrimeFaces genera uno con un UUID aleatorio
     * cada vez que se arma el menú (en cada inicio de sesión); el menú lateral guarda en
     * cookie los ids de los ítems abiertos y, tras otro login, apuntaba a ítems que ya no
     * existían ("elem.position() is undefined" en layout.js).
     */
    private static String idEstable(MenuVO menu) {
        String componente = menu.getComponenteId();
        if (componente != null && !componente.isBlank()) {
            return componente;
        }
        return menu.getIdMenu() != null ? PREFIJO_ID_MENU + menu.getIdMenu() : null;
    }

    /**
     * Completa los ids derivados con las coordenadas del elemento, en el mismo formato
     * que BaseMenuModel.generateUniqueIds de PrimeFaces ("[_]id|coordenadas"; el "_"
     * solo en el primer nivel): PrimeFaces localiza el ítem por la parte anterior a "|"
     * o por sus coordenadas, así que se comporta igual que con sus ids, pero el valor no
     * cambia entre sesiones. Los ids configurados en BD (componente) se conservan tal cual
     * y los elementos sin id los sigue generando PrimeFaces.
     */
    private static void completarIdsEstables(List<MenuElement> elementos, String coordenadasPadre) {
        for (int i = 0; i < elementos.size(); i++) {
            MenuElement elemento = elementos.get(i);
            String coordenadas = coordenadasPadre == null ? String.valueOf(i) : coordenadasPadre + "_" + i;
            String id = elemento.getId();
            if (id != null && id.startsWith(PREFIJO_ID_MENU) && !id.contains("|")) {
                elemento.setId((coordenadasPadre == null ? "_" : "") + id + "|" + coordenadas);
            }
            if (elemento instanceof MenuGroup grupo) {
                completarIdsEstables(grupo.getElements(), coordenadas);
            }
        }
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
                    menuItem_.setId(idEstable(menu));
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
        submenuChild.setId(idEstable(menu_));
        submenuChild.setLabel(menu_.getLabelMenu());
        menuParent.getElements().add(submenuChild);
        submenuChild.setIcon(menu_.getIcon());
        DefaultMenuItem menus_ = fillItems(menu_, menus, submenuChild, menuItem);
        if (menus_ != null) {
            submenuChild.getElements().add(menus_);
        }
    }
}
