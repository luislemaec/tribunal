package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.domain.Rol;
import ec.com.antenasur.service.RolFacade;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Named
@ViewScoped
@Slf4j
public class RolController implements Serializable {

    private static final String DESTINATION = System.getProperty("java.io.tmpdir");

    private static final long serialVersionUID = 1L;

    private static final String FORMULARIO = "frmRol";
    private static final String TABLA = "tblRol";
    private static final String MENSAJE_REGISTRA_OK = "Rol registrado";
    private static final String MENSAJE_ACTUALIZA_OK = "Rol actualizado";
    private static final String MENSAJE_ELIMINA_OK = "Rol eliminado";
    public static final String MENSAJE_CONFORMACION_ELIMINAR = "¿Esta seguro de eliminar?";

    @Inject
    private LoginBean loginBean;

    @Inject
    private RolFacade rolFacade;

    @Getter
    @Setter
    private Rol rolSeleccionado;

    @Setter
    @Getter
    private List<Rol> roles, rolesSeleccionados;

    @PostConstruct
    private void init() {
        try {
            roles = rolFacade.findAll();
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    /**
     * Inicializa medio seleccionado
     */
    public void inicializaRolSeleccionado() {
        if (roles != null) {
            roles.clear();
        }
        rolSeleccionado = new Rol();

    }

    public void nuevoRol() {
        inicializaRolSeleccionado();
    }

    public boolean existeRolesSeleccionados() {
        return this.rolesSeleccionados != null && !this.rolesSeleccionados.isEmpty();
    }

    public String getMensajeBotonEliminar() {
        if (existeRolesSeleccionados()) {
            int size = this.rolesSeleccionados.size();
            return size > 1 ? size + " Roles seleccionadas" : "1 rol seleccionado";
        }
        return "Eliminar";
    }

    public void eliminarRolesSeleccionados() {
        if (rolesSeleccionados != null) {
            for (Rol item : rolesSeleccionados) {
                rolFacade.delete(item);
            }
        }
        roles = rolFacade.findAll();
        JsfUtil.addInfoMessage(+rolesSeleccionados.size() + " Roles eliminados");
        this.rolesSeleccionados = null;
        PrimeFaces.current().ajax().update(FORMULARIO, "msgs");

    }

    public void buscaRolPorNombre() {
        if (rolSeleccionado != null) {
            Rol rolBuscado = rolFacade.buscaPorNombre(rolSeleccionado.getNombre());
            if (rolBuscado != null) {
                rolSeleccionado = rolBuscado;
                JsfUtil.addInfoMessage("Rol " + rolBuscado.getNombre() + " ya se encuentra registrado ");
            }
        }
    }

    public void actualizarRegistro() {
        try {
            if (rolSeleccionado != null) {
                if (this.rolSeleccionado.getId() != null) {
                    Rol rolActualiza = rolFacade.edit(rolSeleccionado);
                    if (rolActualiza != null) {
                        JsfUtil.addSuccessMessage(MENSAJE_ACTUALIZA_OK);
                        roles = rolFacade.findAll();
                        PrimeFaces.current().ajax().update("msgs", FORMULARIO);
                    }
                } else {
                    Rol rolNuevo = rolFacade.create(rolSeleccionado);
                    if (rolNuevo != null) {
                        JsfUtil.addSuccessMessage(MENSAJE_REGISTRA_OK);
                        PrimeFaces.current().ajax().update("msgs", FORMULARIO);
                    }
                }
                rolSeleccionado = null;
            }
        } catch (Exception e) {
            log.error("ERROR EN AGUARDAR ROL");
        }
        PrimeFaces.current().executeScript("PF('dlgRol').hide()");
        PrimeFaces.current().ajax().update(FORMULARIO, FORMULARIO + ":" + TABLA);
    }
}
