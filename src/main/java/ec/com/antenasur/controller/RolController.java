package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.dto.RolDTO;
import ec.com.antenasur.service.RolService;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Named
@ViewScoped
@Slf4j
public class RolController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String FORMULARIO = "frmRol";
    private static final String TABLA = "tblRol";
    private static final String MENSAJE_REGISTRA_OK = "Rol registrado";
    private static final String MENSAJE_ACTUALIZA_OK = "Rol actualizado";

    @Inject
    private LoginBean loginBean;

    @Inject
    private RolService rolService;

    @Getter
    @Setter
    private RolDTO rolSeleccionado;

    @Setter
    @Getter
    private List<RolDTO> roles, rolesSeleccionados;

    @PostConstruct
    private void init() {
        try {
            roles = rolService.listarDTOs();
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    public void inicializaRolSeleccionado() {
        if (roles != null) {
            roles.clear();
        }
        rolSeleccionado = new RolDTO();
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
        int eliminados = 0;
        if (rolesSeleccionados != null) {
            for (RolDTO item : rolesSeleccionados) {
                if (item.getId() != null && rolService.eliminarPorId(item.getId()) != null) {
                    eliminados++;
                }
            }
        }
        roles = rolService.listarDTOs();
        JsfUtil.addInfoMessage(eliminados + " Roles eliminados");
        this.rolesSeleccionados = null;
        PrimeFaces.current().ajax().update(FORMULARIO, "msgs");
    }

    public void buscaRolPorNombre() {
        if (rolSeleccionado != null && rolSeleccionado.getNombre() != null) {
            RolDTO encontrado = rolService.buscarDTOPorNombre(rolSeleccionado.getNombre());
            if (encontrado != null) {
                rolSeleccionado = encontrado;
                JsfUtil.addInfoMessage("Rol " + encontrado.getNombre() + " ya se encuentra registrado ");
            }
        }
    }

    public void actualizarRegistro() {
        try {
            if (rolSeleccionado == null) {
                return;
            }
            boolean esEdicion = rolSeleccionado.getId() != null;
            RolDTO persistido = rolService.guardarDesdeDTO(rolSeleccionado);
            if (persistido != null) {
                JsfUtil.addSuccessMessage(esEdicion ? MENSAJE_ACTUALIZA_OK : MENSAJE_REGISTRA_OK);
                roles = rolService.listarDTOs();
                PrimeFaces.current().ajax().update("msgs", FORMULARIO);
            }
            rolSeleccionado = null;
        } catch (Exception e) {
            log.error("ERROR EN GUARDAR ROL", e);
        }
        PrimeFaces.current().executeScript("PF('dlgRol').hide()");
        PrimeFaces.current().ajax().update(FORMULARIO, FORMULARIO + ":" + TABLA);
    }
}
