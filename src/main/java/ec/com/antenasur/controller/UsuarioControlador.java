package ec.com.antenasur.controller;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Model;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import ec.com.antenasur.bean.ProcesoBean;
import ec.com.antenasur.dto.IglesiaDTO;
import ec.com.antenasur.dto.FiltroUsuarioDTO;
import ec.com.antenasur.dto.RegistroCivilDTO;
import ec.com.antenasur.dto.RolUsuarioDTO;
import ec.com.antenasur.dto.UsuarioDTO;
import ec.com.antenasur.model.Persona;
import ec.com.antenasur.model.Rol;
import ec.com.antenasur.model.RolUsuario;
import ec.com.antenasur.service.IglesiaPersonaService;
import ec.com.antenasur.service.IglesiaService;
import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.service.PersonaService;
import ec.com.antenasur.service.RolService;
import ec.com.antenasur.service.RolUsuarioService;
import ec.com.antenasur.service.UsuarioService;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.JsfUtil;
import ec.com.antenasur.util.SendEmail;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;

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
    UsuarioService usuarioService;

    @Inject
    RolUsuarioService rolUsuarioService;

    @Inject
    PersonaService personaService;

    @Inject
    RolService rolService;

    @Inject
    IglesiaService iglesiaService;

    @Inject
    IglesiaPersonaService iglesiaPersonaService;

    @Setter
    @Getter
    private List<UsuarioDTO> listaUsuarios, listaUsuariosSeleccionado;

    @Setter
    @Getter
    private List<RolUsuarioDTO> listaRolUsuarios, listaRUsSeleccionado;

    @Setter
    @Getter
    private List<Rol> listaRoles, listaRolesSeleccionado;

    @Setter
    @Getter
    private List<IglesiaDTO> listaIglesias;

    @Setter
    @Getter
    private Rol rolSeleccionado;

    @Setter
    @Getter
    private UsuarioDTO usuarioSeleccionado;

    @Setter
    @Getter
    private RolUsuarioDTO rolUsuarioSeleccionado;

    @Setter
    @Getter
    private RolUsuarioDTO rolUsuarioParaReactivacion;

    @Setter
    @Getter
    private Integer iglesiaReactivacionId;

    @Setter
    @Getter
    private RegistroCivilDTO personaRegistroCivil;

    @Setter
    @Getter
    private String textHtml;

    @Setter
    @Getter
    private Boolean esUsuarioNuevo = false;

    @Getter
    private FiltroUsuarioDTO filtroUsuarios;

    @Getter
    private LazyDataModel<RolUsuarioDTO> usuariosLazy;

    @Getter
    private boolean busquedaUsuariosEjecutada;

    @Setter
    @Getter
    private int primerRegistroUsuarios;

    public UsuarioControlador() {
    }

    @PostConstruct
    private void init() {
        filtroUsuarios = new FiltroUsuarioDTO();
        inicializarModeloUsuarios();
        cargarCatalogos();
    }

    private void cargarCatalogos() {
        listaRoles = rolService.getRolesAplicativoSeleccion();
        if (listaRoles == null) {
            listaRoles = Collections.emptyList();
        }
        rolSeleccionado = new Rol();
        listaIglesias = iglesiaService.listarDTOs();
    }

    private void inicializarModeloUsuarios() {
        usuariosLazy = new LazyDataModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                return busquedaUsuariosEjecutada ? rolUsuarioService.contarUsuarios(filtroUsuarios) : 0;
            }

            @Override
            public List<RolUsuarioDTO> load(int first, int pageSize, Map<String, SortMeta> sortBy,
                    Map<String, FilterMeta> filterBy) {
                if (!busquedaUsuariosEjecutada) {
                    return Collections.emptyList();
                }
                return rolUsuarioService.buscarUsuarios(filtroUsuarios, first, pageSize);
            }
        };
    }

    /** Ejecuta una consulta paginada; no se carga ningún usuario antes de esta acción. */
    public void buscarUsuarios() {
        primerRegistroUsuarios = 0;
        busquedaUsuariosEjecutada = true;
    }

    /** Limpia los criterios y deja la tabla sin resultados hasta una nueva búsqueda. */
    public void limpiarBusquedaUsuarios() {
        filtroUsuarios = new FiltroUsuarioDTO();
        primerRegistroUsuarios = 0;
        busquedaUsuariosEjecutada = false;
    }

    /**
     * Indica si el rol actualmente seleccionado en el formulario corresponde
     * al perfil "IglesiaAdmin", para que la vista muestre/exija el dropdown
     * de iglesia. Compara contra el nombre del rol (con prefijo SITEC-).
     */
    /**
     * Listener del dropdown de rol: resuelve la entity completa a partir del
     * id seleccionado para que {@link #isRolRequiereIglesia()} pueda consultar
     * el nombre. Sin esto, value="#{...rolSeleccionado.id}" solo asignaría
     * el id y el nombre quedaría vacío.
     */
    public void cargarRolSeleccionado() {
        if (rolSeleccionado != null && rolSeleccionado.getId() != null) {
            this.rolSeleccionado = rolService.find(rolSeleccionado.getId());
        }
    }

    public boolean isRolRequiereIglesia() {
        if (rolSeleccionado == null || rolSeleccionado.getNombre() == null) {
            return false;
        }
        return rolSeleccionado.getNombre().endsWith(Constantes.getRolIglesiaAdmin());
    }

    public void eliminarUsuario(UsuarioDTO usuario) throws RuntimeException, IOException {
        if (usuario == null || usuario.getId() == null) {
            return;
        }
        try {
            UsuarioDTO eliminado = usuarioService.eliminarPorId(usuario.getId());
            if (eliminado != null) {
                cargarCatalogos();
                JsfUtil.addInfoMessage(eliminado.getUsername() + ", DESACTIVADO");
                procesoBean.registraActividad("DESACTIVA USUARIO: " + eliminado.getUsername());
            } else {
                JsfUtil.addWarningMessage("No fue posible desactivar el usuario.");
            }
        } catch (Exception e) {
            JsfUtil.addErrorMessage("No fue posible desactivar el usuario.");
        }
    }

    /** Prepara la reactivación sin habilitar la cuenta todavía. */
    public void prepararReactivacion(RolUsuarioDTO rolUsuario) {
        if (rolUsuario == null || rolUsuario.getId() == null || rolUsuario.getUsuario() == null) {
            return;
        }
        rolUsuarioParaReactivacion = rolUsuario;
        iglesiaReactivacionId = null;
    }

    public boolean isRolReactivacionRequiereIglesia() {
        return rolUsuarioParaReactivacion != null
                && rolUsuarioParaReactivacion.getRol() != null
                && rolUsuarioParaReactivacion.getRol().getNombre() != null
                && rolUsuarioParaReactivacion.getRol().getNombre()
                        .endsWith(Constantes.getRolIglesiaAdmin());
    }

    public void confirmarReactivacion() {
        if (rolUsuarioParaReactivacion == null || rolUsuarioParaReactivacion.getId() == null
                || rolUsuarioParaReactivacion.getUsuario() == null) {
            JsfUtil.addErrorMessage("No fue posible determinar el usuario a reactivar.");
            FacesContext.getCurrentInstance().validationFailed();
            return;
        }
        try {
            UsuarioDTO reactivado = usuarioService.reactivarPorId(
                    rolUsuarioParaReactivacion.getUsuario().getId(),
                    rolUsuarioParaReactivacion.getId(), iglesiaReactivacionId);
            cargarCatalogos();
            JsfUtil.addSuccessMessage(reactivado.getUsername() + ", REACTIVADO");
            procesoBean.registraActividad("REACTIVA USUARIO: " + reactivado.getUsername()
                    + " ROL: " + (rolUsuarioParaReactivacion.getRol() != null
                    ? rolUsuarioParaReactivacion.getRol().getNombre() : "(sin rol)"));
            rolUsuarioParaReactivacion = null;
            iglesiaReactivacionId = null;
        } catch (ec.com.antenasur.exception.NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
            FacesContext.getCurrentInstance().validationFailed();
        } catch (Exception e) {
            JsfUtil.addErrorMessage("No fue posible reactivar el usuario.");
            FacesContext.getCurrentInstance().validationFailed();
        }
    }

    public void actualizarUsuario() throws RuntimeException, IOException {
        if (usuarioSeleccionado == null || rolSeleccionado == null || rolSeleccionado.getId() == null) {
            JsfUtil.addErrorMessage("Debe seleccionar un rol para el usuario.");
            FacesContext.getCurrentInstance().validationFailed();
            return;
        }
        if (isRolRequiereIglesia()
                && usuarioSeleccionado.getIglesiaId() == null) {
            JsfUtil.addErrorMessage("Debe seleccionar una iglesia para el rol IglesiaAdmin");
            FacesContext.getCurrentInstance().validationFailed();
            return;
        }
        if (!isRolRequiereIglesia()) {
            // Otros roles no llevan iglesia: limpiamos por si quedó del cambio.
            usuarioSeleccionado.setIglesiaId(null);
        }
        try {
            if (esUsuarioNuevo) {
                UsuarioDTO creado = usuarioService.crearUsuarioDesdeDTO(usuarioSeleccionado, rolSeleccionado);
                if (creado == null) {
                    JsfUtil.addErrorMessage("No fue posible registrar el usuario.");
                    FacesContext.getCurrentInstance().validationFailed();
                    return;
                }
                this.usuarioSeleccionado = creado;
                boolean reactivado = Boolean.TRUE.equals(creado.getReactivado());
                JsfUtil.addSuccessMessage(reactivado
                        ? "Usuario reactivado correctamente."
                        : "Usuario registrado correctamente.");
                try {
                    procesoBean.registraActividad((reactivado ? "REACTIVA" : "CREA") + " USUARIO: "
                            + creado.getUsername() + " ROL: " + rolSeleccionado.getNombre());
                } catch (Exception logEx) {
                    logEx.printStackTrace();
                }
                if (!reactivado) {
                    try {
                        enviarCorreoCreacionUser();
                    } catch (Exception mailEx) {
                        mailEx.printStackTrace();
                        JsfUtil.addWarningMessage("Usuario creado correctamente; el correo de bienvenida no pudo enviarse.");
                    }
                }
                cargarCatalogos();
                esUsuarioNuevo = false;
            } else {
                UsuarioDTO actual = usuarioService.obtenerDTOPorId(usuarioSeleccionado.getId());
                String correoAnterior = actual != null ? actual.getCorreo() : null;
                Rol rolPersistido = rolService.find(rolSeleccionado.getId());
                RolUsuario rolUsuarioActual = rolUsuarioSeleccionado != null && rolUsuarioSeleccionado.getId() != null
                        ? rolUsuarioService.find(rolUsuarioSeleccionado.getId()) : null;
                UsuarioDTO actualizado = usuarioService.actualizarUsuarioDesdeDTO(
                        usuarioSeleccionado, rolUsuarioActual, rolPersistido);
                this.usuarioSeleccionado = actualizado;
                procesoBean.registraActividad("ACTUALIZA USUARIO: " + actualizado.getUsername(),
                        correoAnterior, actualizado.getCorreo());
                cargarCatalogos();
                JsfUtil.addInfoMessage(actualizado.getUsername() + ", ACTUALIZADO");
            }
        } catch (ec.com.antenasur.exception.NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
            FacesContext.getCurrentInstance().validationFailed();
        } catch (Exception e) {
            JsfUtil.addErrorMessage("No fue posible guardar el usuario.");
            FacesContext.getCurrentInstance().validationFailed();
        }
    }

    public void crearNuevoUsuario() {
        esUsuarioNuevo = true;
        this.usuarioSeleccionado = new UsuarioDTO();
        this.rolSeleccionado = new Rol();
        this.rolUsuarioSeleccionado = null;
        this.personaRegistroCivil = null;
    }

    public void blurEvent() {
        if (usuarioSeleccionado == null || usuarioSeleccionado.getPersonaDocumento() == null
                || usuarioSeleccionado.getPersonaDocumento().isEmpty()) {
            return;
        }
        String cedula = usuarioSeleccionado.getPersonaDocumento();

        // 1) Buscar primero en la BD interna (tb_persona). Si existe, hidratamos
        //    el formulario con sus datos y la iglesia a la que pertenece (si la
        //    tiene en tb_iglesia_persona). El username queda igual a la cédula.
        Persona personaExistente = personaService.finByPersonaDocument(cedula);
        if (personaExistente != null) {
            usuarioSeleccionado.setPersonaId(personaExistente.getId());
            usuarioSeleccionado.setPersonaNombres(personaExistente.getNombres());
            usuarioSeleccionado.setPersonaApellidos(personaExistente.getApellidos());
            usuarioSeleccionado.setPersonaDocumento(personaExistente.getDocumento());
            if (usuarioSeleccionado.getUsername() == null || usuarioSeleccionado.getUsername().isEmpty()) {
                usuarioSeleccionado.setUsername(personaExistente.getDocumento());
            }
            // Autogenerar correo si la persona no lo tiene (consistencia con
            // flujo Registro Civil). Si Persona expone correo y existe se
            // prefiere el real.
            if (usuarioSeleccionado.getCorreo() == null || usuarioSeleccionado.getCorreo().isEmpty()) {
                usuarioSeleccionado.setCorreo(personaExistente.getDocumento() + "@gmail.com");
            }
            usuarioSeleccionado.setPermanente(false);
            // Iglesia asociada vía tb_iglesia_persona (vínculo activo más reciente)
            Iglesia iglesia = iglesiaPersonaService.obtenerIglesiaDePersona(personaExistente.getId());
            if (iglesia != null) {
                usuarioSeleccionado.setIglesiaId(iglesia.getId());
                usuarioSeleccionado.setIglesiaNombre(iglesia.getNombre());
                JsfUtil.addInfoMessage("Persona encontrada — iglesia: " + iglesia.getNombre());
            } else {
                JsfUtil.addInfoMessage("Persona encontrada (sin iglesia asignada)");
            }
            return;
        }

        // 2) No existe internamente: caer al fallback del Registro Civil.
        if (getDatos_registro_civil(cedula)) {
            usuarioSeleccionado.setPersonaNombres(personaRegistroCivil.getNombre());
            usuarioSeleccionado.setPersonaDocumento(personaRegistroCivil.getCedula());
            usuarioSeleccionado.setUsername(personaRegistroCivil.getCedula());
            usuarioSeleccionado.setPermanente(false);
            usuarioSeleccionado.setCorreo(personaRegistroCivil.getCedula() + "@gmail.com");
            JsfUtil.addInfoMessage("Datos obtenidos del Registro Civil");
        }
    }

    public void obtenerRolSeleccionado() {
        if (rolSeleccionado != null) {
            this.rolSeleccionado = rolService.find(rolSeleccionado.getId());
        }
    }

    /**
     * Prepara el formulario de edición con los datos del rolUsuario
     * seleccionado. Resuelve la entidad {@code Rol} a partir del id del DTO
     * para mantener compatibilidad con el service que requiere entity.
     */
    public void cargarParaEdicion(RolUsuarioDTO rolUs) {
        if (rolUs == null) {
            return;
        }
        this.rolUsuarioSeleccionado = rolUs;
        this.usuarioSeleccionado = usuarioService.obtenerDTOPorId(rolUs.getUsuario().getId());
        if (rolUs.getRol() != null && rolUs.getRol().getId() != null) {
            this.rolSeleccionado = rolService.find(rolUs.getRol().getId());
        }
        this.listaRoles = rolService.getRolesAplicativoSeleccion();
        this.esUsuarioNuevo = false;
    }

    public void obtenerDatosUsuarioSeleccionado() {
        if (usuarioSeleccionado != null && rolSeleccionado != null) {
            listaRUsSeleccionado = rolUsuarioService.listarDTOsPorUsername(usuarioSeleccionado.getUsername());
            if (listaRUsSeleccionado != null && !listaRUsSeleccionado.isEmpty()) {
                Integer rolId = listaRUsSeleccionado.get(0).getRol() != null
                        ? listaRUsSeleccionado.get(0).getRol().getId() : null;
                if (rolId != null) {
                    rolSeleccionado = rolService.find(rolId);
                }
                listaRoles = rolService.getRolesAplicativoSeleccion();
            }
        }
    }

    private Boolean getDatos_registro_civil(String identificacion) {
        Client clienteRC = ClientBuilder.newClient();
        WebTarget targetRC = clienteRC.target("http://192.168.26.32:8090/WS_REST/datos_regitrocivil/");
        Response respuestaRegistroCivil = targetRC.path(identificacion).request().get();
        personaRegistroCivil = null;
        boolean operationStus = false;
        try {
            if (respuestaRegistroCivil.getStatus() == 200) {
                personaRegistroCivil = respuestaRegistroCivil.readEntity(RegistroCivilDTO.class);
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
     * Best-effort: nunca debe propagar excepciones al caller. Si el envío del
     * correo falla por cualquier razón (SMTP caído, certificado inválido,
     * destinatario inválido, NPE, etc.), el usuario ya quedó registrado en BD
     * y el flujo debe continuar sin error.
     */
    public void enviarCorreoCreacionUser() {
        try {
            if (usuarioSeleccionado == null) {
                return;
            }
            String destino = usuarioSeleccionado.getCorreo();
            if (destino == null || destino.trim().isEmpty()) {
                JsfUtil.addWarningMessage("El usuario fue creado, pero no tiene correo: no se envió notificación.");
                try {
                    procesoBean.registraActividad("CREA USUARIO SIN CORREO: " + usuarioSeleccionado.getUsername());
                } catch (Exception ignored) { /* nunca interrumpir */ }
                return;
            }
            textHtml = "<p><strong>Estimado/a " + usuarioSeleccionado.getPersonaNombres() + "</strong></p>"
                    + "<p>Se ha creado el usuario en el Sistema de Seleccion de Medios, puede iniciar su sesi&oacute;n con su usuario </p>"
                    + "<p>Datos de acceso:&nbsp;</p>" + "<p><strong>Usuario:</strong> "
                    + usuarioSeleccionado.getUsername() + "</p>" + "<p><strong>Contrase&ntilde;a:</strong> "
                    + usuarioSeleccionado.getPersonaDocumento() + "</p>"
                    + Constantes.FIRMA_CORREO;
            List<String> emailsDestino = new ArrayList<>();
            emailsDestino.add(destino);
            try {
                SendEmail.correoAdjunto(emailsDestino, "Creación de usuarios", textHtml, Constantes.getPathLogo());
                try {
                    procesoBean.registraActividad("ENVIA CORREO REGISTRO USUARIO: " + usuarioSeleccionado.getUsername());
                } catch (Exception ignored) { /* nunca interrumpir */ }
            } catch (Exception e) {
                e.printStackTrace();
                JsfUtil.addWarningMessage("Usuario creado, pero hubo un problema al enviar el correo de bienvenida.");
            }
        } catch (Throwable t) {
            // Captura final defensiva: un fallo de mailing JAMÃS debe romper
            // el flujo de creación del usuario.
            t.printStackTrace();
        }
    }
}
