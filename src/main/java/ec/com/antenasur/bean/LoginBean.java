/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.bean;

import java.io.IOException;
import java.io.Serializable;
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
import org.primefaces.model.menu.MenuModel;

import ec.com.antenasur.dto.PersonaDTO;
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

    @Setter
    @Getter
    private PersonaDTO persona;

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
