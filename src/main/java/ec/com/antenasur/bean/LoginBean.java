/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.bean;

import java.io.IOException;
import java.io.Serializable;
import java.text.Normalizer;
import java.util.Locale;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

import jakarta.validation.constraints.Email;
import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.DefaultMenuModel;
import org.primefaces.model.menu.DefaultSubMenu;
import org.primefaces.model.menu.MenuElement;
import org.primefaces.model.menu.MenuItem;
import org.primefaces.model.menu.MenuModel;
import org.primefaces.model.menu.Submenu;

import ec.com.antenasur.dto.UsuarioDTO;
import ec.com.antenasur.model.AccessAuditory;
import ec.com.antenasur.service.AccessService;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Named(value = "loginBean")
@SessionScoped
@Slf4j
public class LoginBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Setter
    @Getter
    private String userName, password, passwordTemp;

    @Setter
    @Getter
    @Email
    private String email;

    @Setter
    private UsuarioDTO usuario;

    private Map<String, Object> content;

    @Setter
    @Getter
    private boolean loggedIn;

    @Setter
    @Getter
    private boolean internalUsuario;

    @Setter
    @Getter
    private MenuModel menuModel;

    @Setter
    private MenuModel menuModelCompleto;

    @Getter
    @Setter
    private String filtroMenu;

    @Getter
    private boolean filtroMenuActivo;

    @Getter
    private boolean menuFiltradoSinResultados;

    @Getter
    @Setter
    private List<String> roles;

    @Getter
    @Setter
    private int tiempoSession;

    @Inject
    private AccessService accessService;

    @Setter
    private AccessAuditory accessAuditory;

    @Inject
    ProcesoBean procesoBean;

    @Getter
    @Setter
    String servidor, sistema;

    public LoginBean() {

    }

    @PostConstruct
    private void init() {
        try {
            sistema = (String) JsfUtil.getProperty("sistema", true);
            // Etiqueta del ambiente. Se lee de la system property -Dapp.environment;
            // si no está definida, asume DESARROLLO. Antes este bloque hacía dos
            // reverse-DNS lookups via InetAddress.getLocalHost() cuyo resultado se
            // descartaba inmediatamente — eso bloqueaba la primera carga del login
            // hasta 30s en redes con DNS lento. Para definir el ambiente real,
            // arranque WildFly con -Dapp.environment="P R O D U C C I Ó N" (etc.).
            servidor = System.getProperty("app.environment", "D E S A R R O L L O");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public UsuarioDTO getUsuario() {
        if (usuario == null) {
            usuario = new UsuarioDTO();
        }
        return usuario;
    }

    private void registerAdditory(String session) {
        try {
            accessAuditory = accessService.findBySession(session);
            if (accessAuditory != null) {
                accessAuditory.setLogout(JsfUtil.getTimestamp());
                accessAuditory.setActive(false);
                accessService.edit(accessAuditory);
            }
        } catch (Exception e) {
            log.info("Error");
        }
    }

    /**
     * Método para cerrar sesión
     *
     * @return
     * @throws IOException
     * @throws RuntimeException
     * @throws ServletException
     */
    public void logout() throws RuntimeException, IOException, ServletException {
        HttpServletRequest request = JsfUtil.getRequest();
        registerAdditory(request.getSession().getId().toString());
        procesoBean.registraActividad("SALE DEL " + Constantes.SISTEMA);
        request.logout();

        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        JsfUtil.redirect("/");
    }

    public void updateUsuario() throws RuntimeException, IOException, ServletException {
        JsfUtil.redirect("/paginas/administracion/actualizar.jsf");
    }

    public Map<String, Object> getContent() {
        if (content != null) {
            for (Map.Entry<String, Object> entry : content.entrySet()) {
                System.out.println("Key : " + entry.getKey() + "Value : " + entry.getValue());
            }
        }
        return content;
    }

    public void cerrarSessionExpirada() throws RuntimeException, IOException {
        procesoBean.registraActividad("SALE DEL " + Constantes.SISTEMA);
        HttpServletRequest request = JsfUtil.getRequest();
        request.getSession().invalidate();
        JsfUtil.redirect("/errors/viewExpired.jsf");
    }

    public void cerrarSessionRedireccionar(String url) throws RuntimeException, IOException {
        HttpServletRequest request = JsfUtil.getRequest();
        request.getSession().invalidate();
        JsfUtil.redirect(url);
    }

    public void setContent(Map<String, Object> content) {
        this.content = content;
    }

    public void inicializarMenuAutorizado(MenuModel menuAutorizado) {
        this.menuModelCompleto = menuAutorizado;
        this.menuModel = menuAutorizado;
        this.filtroMenu = null;
        this.filtroMenuActivo = false;
        this.menuFiltradoSinResultados = false;
    }

    public void filtrarMenuPrincipal() {
        if (menuModelCompleto == null) {
            menuModelCompleto = menuModel;
        }

        String filtroNormalizado = normalizar(filtroMenu);
        filtroMenuActivo = filtroNormalizado != null && !filtroNormalizado.isBlank();
        menuFiltradoSinResultados = false;

        if (!filtroMenuActivo) {
            menuModel = menuModelCompleto;
            return;
        }

        DefaultMenuModel filtrado = new DefaultMenuModel();
        if (menuModelCompleto != null && menuModelCompleto.getElements() != null) {
            for (MenuElement elemento : menuModelCompleto.getElements()) {
                MenuElement coincidencia = filtrarElemento(elemento, filtroNormalizado);
                if (coincidencia != null) {
                    filtrado.getElements().add(coincidencia);
                }
            }
        }

        menuFiltradoSinResultados = filtrado.getElements().isEmpty();
        menuModel = filtrado;
    }

    public void limpiarFiltroMenu() {
        filtroMenu = null;
        filtrarMenuPrincipal();
    }

    private MenuElement filtrarElemento(MenuElement elemento, String filtroNormalizado) {
        if (elemento instanceof MenuItem menuItem) {
            return coincideMenuItem(menuItem, filtroNormalizado) ? copiarMenuItem(menuItem) : null;
        }
        if (elemento instanceof Submenu submenu) {
            boolean coincidePadre = contiene(normalizar(submenu.getLabel()), filtroNormalizado);
            DefaultSubMenu copia = copiarSubmenu(submenu, filtroMenuActivo);
            for (MenuElement hijo : submenu.getElements()) {
                MenuElement hijoFiltrado = coincidePadre ? copiarElementoCompleto(hijo) : filtrarElemento(hijo, filtroNormalizado);
                if (hijoFiltrado != null) {
                    copia.getElements().add(hijoFiltrado);
                }
            }
            return !copia.getElements().isEmpty() ? copia : null;
        }
        return null;
    }

    private MenuElement copiarElementoCompleto(MenuElement elemento) {
        if (elemento instanceof MenuItem menuItem) {
            return copiarMenuItem(menuItem);
        }
        if (elemento instanceof Submenu submenu) {
            DefaultSubMenu copia = copiarSubmenu(submenu, filtroMenuActivo);
            for (MenuElement hijo : submenu.getElements()) {
                MenuElement hijoCopiado = copiarElementoCompleto(hijo);
                if (hijoCopiado != null) {
                    copia.getElements().add(hijoCopiado);
                }
            }
            return copia;
        }
        return null;
    }

    private DefaultSubMenu copiarSubmenu(Submenu submenu, boolean expandido) {
        DefaultSubMenu copia = new DefaultSubMenu();
        copia.setId(submenu.getId());
        copia.setLabel(submenu.getLabel());
        copia.setIcon(submenu.getIcon());
        copia.setRendered(submenu.isRendered());
        copia.setStyle(submenu.getStyle());
        copia.setStyleClass(construirStyleClass(submenu.getStyleClass(), expandido));
        return copia;
    }

    private DefaultMenuItem copiarMenuItem(MenuItem menuItem) {
        DefaultMenuItem copia = new DefaultMenuItem();
        copia.setId(menuItem.getId());
        copia.setValue(menuItem.getValue());
        copia.setIcon(menuItem.getIcon());
        copia.setOutcome(menuItem.getOutcome());
        copia.setUrl(menuItem.getUrl());
        copia.setCommand(menuItem.getCommand());
        copia.setTarget(menuItem.getTarget());
        copia.setTitle(menuItem.getTitle());
        copia.setDisabled(menuItem.isDisabled());
        copia.setRendered(menuItem.isRendered());
        copia.setStyle(menuItem.getStyle());
        copia.setStyleClass(menuItem.getStyleClass());
        return copia;
    }

    private boolean coincideMenuItem(MenuItem menuItem, String filtroNormalizado) {
        return contiene(normalizar(menuItem.getValue()), filtroNormalizado)
                || contiene(normalizar(menuItem.getTitle()), filtroNormalizado)
                || contiene(normalizar(menuItem.getOutcome()), filtroNormalizado)
                || contiene(normalizar(menuItem.getUrl()), filtroNormalizado);
    }

    private boolean contiene(String valor, String filtroNormalizado) {
        return valor != null && filtroNormalizado != null && valor.contains(filtroNormalizado);
    }

    private String construirStyleClass(String actual, boolean expandido) {
        if (!expandido) {
            return actual;
        }
        String clases = actual == null ? "" : actual.trim();
        return clases.contains("active-menuitem") ? clases : (clases + " active-menuitem").trim();
    }

    private String normalizar(Object valor) {
        if (valor == null) {
            return null;
        }
        String texto = valor.toString().trim().toLowerCase(Locale.ROOT);
        if (texto.isEmpty()) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    /**
     * Metodo cierra sesion, redirije a pagina de notificacion de cambio
     * correcto
     *
     * @throws RuntimeException
     * @throws IOException
     * @throws ServletException
     */
    public void passwordChangued() throws RuntimeException, IOException, ServletException {
        HttpServletRequest request = JsfUtil.getRequest();       
        registerAdditory(request.getSession().getId().toString());
        procesoBean.registraActividad("SALE DEL " + Constantes.SISTEMA);
        request.getSession().invalidate();        
    }

}
