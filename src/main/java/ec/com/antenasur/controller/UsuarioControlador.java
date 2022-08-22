/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.controller;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Model;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import ec.com.antenasur.tec.dto.PersonaDTO;
import ec.com.antenasur.domain.Persona;
import ec.com.antenasur.domain.Rol;
import ec.com.antenasur.domain.RolUsuario;
import ec.com.antenasur.domain.Usuario;
import ec.com.antenasur.service.PersonaFacade;
import ec.com.antenasur.service.RolFacade;
import ec.com.antenasur.service.RolUsuarioFacade;
import ec.com.antenasur.service.UsuarioFacade;
import ec.com.antenasur.bean.ProcesoBean;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.JsfUtil;
import ec.com.antenasur.util.SendEmail;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Usuario
 */
@Named(value = "usuarioControlador")
@ViewScoped
@Model
public class UsuarioControlador implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    @Setter
    @Getter
    private ProcesoBean procesoBean;

    @Inject
    UsuarioFacade usuarioFacade;

    @Inject
    RolUsuarioFacade rolUsuarioFacade;

    @Inject
    PersonaFacade personaFacade;

    @Inject
    RolFacade rolFacade;

    @Setter
    @Getter
    private List<Usuario> listaUsuarios, listaUsuariosSeleccionado;

    @Setter
    @Getter
    private List<RolUsuario> listaRolUsuarios, listaRUsSeleccionado;

    @Setter
    @Getter
    private List<Rol> listaRoles, listaRolesSeleccionado;

    @Setter
    @Getter
    private Rol rol, rolSeleccionado;

    @Setter
    @Getter
    private Usuario usuario, usuarioSeleccionado;

    @Setter
    @Getter
    private Persona persona, personaSeleccionado;

    @Setter
    @Getter
    private RolUsuario rolUsuario, rolUsuarioSeleccionado;

    @Setter
    @Getter
    private PersonaDTO personaRegistroCivil;

    @Setter
    @Getter
    private String textHtml;

    @Setter
    @Getter
    private Boolean esUsuarioNuevo = false;

    public UsuarioControlador() {

    }

    @PostConstruct
    private void init() {
        listaRoles = rolFacade.getRolesAplicativoSeleccion();
        listaUsuarios = new ArrayList<>();
        rolSeleccionado = new Rol();
        listaRolUsuarios = rolUsuarioFacade.getRolesUsuariosActivos(listaRoles);
        if (listaRolUsuarios != null) {
            for (RolUsuario ru : listaRolUsuarios) {
                listaUsuarios.add(ru.getUsuario());
            }
        }
        // listaUsuarios=usuarioFacade.findAllActiveUsuario();
    }

    public void eliminarUsuario() throws RuntimeException, IOException {
        if (usuarioSeleccionado != null) {            
            usuarioSeleccionado = usuarioFacade.delete(usuarioSeleccionado);
            if (usuarioSeleccionado != null) {
                init();
                JsfUtil.addInfoMessage(usuarioSeleccionado.getUsername() + ", ELIMINADO");
                procesoBean.registraActividad("ELIMINA USUARIO: " + usuarioSeleccionado.getUsername());
            } else {
                JsfUtil.addWarningMessage("Problemas en eliminar");
                procesoBean.registraActividad("ERROR  AL ELIMINAR USUARIO");
            }
        }

    }

    public void actualizarUsuario() throws RuntimeException, IOException {

        if (esUsuarioNuevo) {
            this.personaSeleccionado = personaFacade.create(personaSeleccionado);
            procesoBean.registraActividad("CREA PERSONA: " + personaSeleccionado.getDocumento());
            if (personaSeleccionado != null) {
                usuarioSeleccionado.setPersonsa(personaSeleccionado);
                this.usuarioSeleccionado = usuarioFacade.create(usuarioSeleccionado);
                procesoBean.registraActividad("CREA USUARIO: " + usuarioSeleccionado.getUsername());
                if (usuarioSeleccionado != null) {
                    rolUsuario = new RolUsuario();
                    rolUsuario.setUsuario(usuarioSeleccionado);
                    rolUsuario.setRol(rolSeleccionado);
                    rolUsuario = rolUsuarioFacade.create(rolUsuario);
                    procesoBean.registraActividad("CREA USUARIO: " + usuarioSeleccionado.getUsername() + " ROL: "
                            + rolSeleccionado.getNombre());
                    JsfUtil.addSuccessMessage("Usuario registrado correctamente");
                    enviarCorreoCreacionUser();
                }
            }

        } else {
            Usuario usuBusca = usuarioFacade.find(usuarioSeleccionado.getId());
            if (!usuBusca.getCorreo().equals(usuarioSeleccionado.getCorreo()) || rolUsuarioSeleccionado.getRol().getId() != rolSeleccionado.getId()) {
                usuarioSeleccionado = usuarioFacade.edit(usuarioSeleccionado);

                rolSeleccionado = rolFacade.find(rolSeleccionado.getId());
                rolUsuarioSeleccionado.getUsuario().setId(usuarioSeleccionado.getId());
                rolUsuarioSeleccionado.getRol().setId(rolSeleccionado.getId());
                rolUsuarioFacade.edit(rolUsuarioSeleccionado);
                procesoBean.registraActividad("ACTUALIZA CORREO: " + usuarioSeleccionado.getUsername(),
                        usuBusca.getCorreo(), usuarioSeleccionado.getCorreo());
                if (usuarioSeleccionado != null) {
                    init();
                    JsfUtil.addInfoMessage(usuarioSeleccionado.getUsername() + ", ACTUALIZADO");
                } else {
                    JsfUtil.addWarningMessage("Problemas en eliminar");
                }
            }

        }
    }

    public void crearNuevoUsuario() {
        esUsuarioNuevo = true;
        this.usuarioSeleccionado = new Usuario();
        usuarioSeleccionado.setPersonsa(new Persona());
    }

    public void blurEvent() {
        if (usuarioSeleccionado.getPersonsa() != null) {
            personaSeleccionado = personaFacade.finByPersonaDocument(usuarioSeleccionado.getPersonsa().getDocumento());

            if (personaSeleccionado != null) {
                JsfUtil.addInfoMessage("El usuario ya ha sido registrado");
            } else {
                if (getDatos_registro_civil(usuarioSeleccionado.getPersonsa().getDocumento())) {
                    personaSeleccionado = new Persona();
                    personaSeleccionado.setNombres(personaRegistroCivil.getNombre());
                    personaSeleccionado.setDocumento(personaRegistroCivil.getCedula());

                    usuarioSeleccionado.setPersonsa(personaSeleccionado);

                    usuarioSeleccionado.setUsername(personaRegistroCivil.getCedula());
                    usuarioSeleccionado.setPermanente(true);
                    usuarioSeleccionado.setContrasenia(JsfUtil.claveEncriptadaSHA1(personaRegistroCivil.getCedula()));
                    usuarioSeleccionado.setCorreo(personaRegistroCivil.getCedula() + "@consejodecomunicacion.gob.ec");

                    JsfUtil.addInfoMessage("Usuario encontrado");
                }
            }
        }

    }

    public void obtenerRolSeleccionado() {
        if (rolSeleccionado != null) {
            this.rolSeleccionado = rolFacade.find(rolSeleccionado.getId());
        }
    }

    public void obtenerDatosUsuarioSeleccionado() {
        if (usuarioSeleccionado != null && rolSeleccionado != null) {
            listaRUsSeleccionado = rolUsuarioFacade.findByUserName(usuarioSeleccionado.getUsername());
            if (listaRUsSeleccionado != null) {
                rolSeleccionado = listaRUsSeleccionado.get(0).getRol();
                listaRoles = rolFacade.getRolesAplicativoSeleccion();
            }
        }
    }

    private Boolean getDatos_registro_civil(String identificacion) {
        Client clienteRC = ClientBuilder.newClient();
        WebTarget targetRC = clienteRC.target("http://192.168.26.32:8090/WS_REST/datos_regitrocivil/");
        Response respuestaRegistroCivil = targetRC.path(identificacion).request().get();
        personaRegistroCivil = new PersonaDTO();
        boolean operationStus = false;
        try {
            if (respuestaRegistroCivil.getStatus() == 200) {
                personaRegistroCivil = respuestaRegistroCivil.readEntity(PersonaDTO.class);
                operationStus = true;
            }
            if (respuestaRegistroCivil.getStatus() == 404) {
                JsfUtil.addInfoMessage("Problemas de interconección, contactese con el administrador");
            }
        } catch (Exception e) {
            personaRegistroCivil = null;
        } finally {
            respuestaRegistroCivil.close();
        }
        return operationStus;
    }

    /**
     * envia correo de aprobado
     */
    public void enviarCorreoCreacionUser() {
        if (personaSeleccionado != null) {

            textHtml = "<p><strong>Estimado/a " + personaSeleccionado.getNombres() + "</strong></p>"
                    + "<p>Se ha creado el usuario en el Sistema de Seleccion de Medios, puede iniciar su sesi&oacute;n con su usuario </p>"
                    + "<p>Datos de acceso:&nbsp;</p>" + "<p><strong>Usuario:</strong> "
                    + usuarioSeleccionado.getUsername() + "</p>" + "<p><strong>Contrase&ntilde;a:</strong> "
                    + personaSeleccionado.getDocumento() + "</p>";
        }
        textHtml = textHtml + Constantes.FIRMA_CORREO;
        List<String> emailsDestino = new ArrayList<>();

        String pathAdjunto = Constantes.getUrlAdjunto();
        emailsDestino.add(usuarioSeleccionado.getCorreo());

        String asunto = "Creación de usuarios";

        try {
            SendEmail.correoAdjunto(emailsDestino, asunto, textHtml, pathAdjunto);
            procesoBean.registraActividad("ENVIA CORREO REGISTRO USUARIO: " + usuarioSeleccionado.getUsername());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
