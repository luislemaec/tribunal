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

import javax.annotation.PostConstruct;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.hibernate.validator.constraints.Email;
import org.primefaces.model.menu.MenuModel;

import ec.com.antenasur.domain.AccessAuditory;
import ec.com.antenasur.domain.Persona;
import ec.com.antenasur.domain.Usuario;
import ec.com.antenasur.service.AccessFacade;
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
    private Usuario usuario;

    @Setter
    @Getter
    private Persona persona;

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
    private AccessFacade accessFacade;

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
            servidor = JsfUtil.obtieneIpServidor();
            servidor = "D E S A R R O L L O";
            if (servidor.equals("192.168.26.38")) {
                servidor = "P R U E B A S";
            }
            if (servidor.equals("192.168.26.26")) {
                servidor = "P R O D U C C I Ó N";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Usuario getUsuario() {
        if (usuario == null) {
            usuario = new Usuario();
        }
        return usuario;
    }

    private void registerAdditory(String session) {
        try {
            accessAuditory = accessFacade.findBySession(session);
            if (accessAuditory != null) {
                accessAuditory.setLogout(JsfUtil.getTimestamp());
                accessAuditory.setActive(false);
                accessFacade.edit(accessAuditory);

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
        request.logout();

        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        JsfUtil.redirect("/changuepasswordsuccess.jsf?faces-redirect=true");
    }

}
