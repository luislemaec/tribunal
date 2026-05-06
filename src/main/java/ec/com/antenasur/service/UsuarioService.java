package ec.com.antenasur.service;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import ec.com.antenasur.dto.AuthDataDTO;
import ec.com.antenasur.dto.PersonaDTO;
import ec.com.antenasur.dto.RolUsuarioDTO;
import ec.com.antenasur.dto.UsuarioDTO;
import ec.com.antenasur.facade.IglesiaFacade;
import ec.com.antenasur.facade.IglesiaPersonaFacade;
import ec.com.antenasur.facade.PersonaFacade;
import ec.com.antenasur.facade.RolUsuarioFacade;
import ec.com.antenasur.facade.UsuarioFacade;
import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.Persona;
import ec.com.antenasur.model.Rol;
import ec.com.antenasur.model.RolUsuario;
import ec.com.antenasur.model.Usuario;

@Stateless
public class UsuarioService extends AbstractService<Usuario, Integer, UsuarioFacade> {

    @Inject
    private UsuarioFacade usuarioFacade;

    @Inject
    private RolUsuarioFacade rolUsuarioFacade;

    @Inject
    private PersonaFacade personaFacade;

    @Inject
    private IglesiaFacade iglesiaFacade;

    @Inject
    private IglesiaPersonaFacade iglesiaPersonaFacade;

    @Override
    protected UsuarioFacade getFacade() {
        return usuarioFacade;
    }

    public Usuario getUsuarioByRuc(String docuId) {
        return usuarioFacade.getUsuarioByRuc(docuId);
    }

    public Usuario findByUsuarioName(String username, String contrasenia) {
        return usuarioFacade.findByUsuarioName(username, contrasenia);
    }

    public Usuario findByUsuarioName(String username) {
        return usuarioFacade.findByUsuarioName(username);
    }

    public Usuario findUsuarioByRucOrMail(String username, String correo) {
        return usuarioFacade.findUsuarioByRucOrMail(username, correo);
    }

    public Usuario findUsuarioByTemportalPassword(String username, String contraseniaTemp) {
        return usuarioFacade.findUsuarioByTemportalPassword(username, contraseniaTemp);
    }

    public Usuario findUsuarioByPeople(int persona_id) {
        return usuarioFacade.findUsuarioByPeople(persona_id);
    }

    public Usuario findUsuariobyUsuarioName(String username) {
        return usuarioFacade.findUsuariobyUsuarioName(username);
    }

    public List<Usuario> findAllActiveUsuario() {
        return usuarioFacade.findAllActiveUsuario();
    }

    /**
     * Crea un usuario nuevo con su persona asociada y el rol indicado, todo
     * en una transacción atómica EJB. Si cualquier paso falla, la transacción
     * se revierte completamente — no queda persona huérfana sin usuario, ni
     * usuario sin rol.
     *
     * @param usuario datos del usuario (sin id; su {@code personsa} se reemplaza
     *        por la persona persistida)
     * @param persona persona a crear y vincular al usuario
     * @param rol rol a asignar al usuario nuevo
     * @return el {@link RolUsuario} creado, con usuario y persona persistidos;
     *         null si la entrada es inválida
     */
    public RolUsuario crearUsuarioConRol(Usuario usuario, Persona persona, Rol rol) {
        if (usuario == null || persona == null || rol == null) {
            return null;
        }
        // Si la persona ya está persistida (tiene id) la reusamos: el caso
        // típico es cuando el formulario hidrató los datos a partir de una
        // cédula existente en BD. Solo creamos cuando es realmente nueva.
        Persona personaPersistida = (persona.getId() != null)
                ? persona
                : personaFacade.create(persona);
        if (personaPersistida == null) {
            return null;
        }
        usuario.setPersonsa(personaPersistida);
        usuario.setEstado(true);
        // Si el usuario no trae clave seteada, la inicializamos con la cédula
        // hasheada en SHA-1. El usuario podrá cambiarla después.
        if (usuario.getContrasenia() == null || usuario.getContrasenia().isEmpty()) {
            String cedula = personaPersistida.getDocumento();
            if (cedula != null && !cedula.isEmpty()) {
                usuario.setContrasenia(ec.com.antenasur.util.JsfUtil.claveEncriptadaSHA1(cedula));
            }
        }
        Usuario usuarioPersistido = usuarioFacade.create(usuario);
        if (usuarioPersistido == null) {
            return null;
        }
        RolUsuario rolUsuario = new RolUsuario();
        rolUsuario.setUsuario(usuarioPersistido);
        rolUsuario.setRol(rol);
        return rolUsuarioFacade.create(rolUsuario);
    }

    /**
     * Actualiza el correo de un usuario y/o su rol asignado, solo si alguno
     * cambió respecto al estado persistido. Si correo y rol son iguales a los
     * actuales, no toca la BD y devuelve {@code false}.
     *
     * @param usuarioActualizado usuario con los nuevos valores (debe tener id)
     * @param rolUsuarioActual relación rol-usuario vigente
     * @param nuevoRol rol seleccionado en el formulario
     * @return {@code true} si hubo persistencia, {@code false} si no hubo
     *         cambios o la entrada es inválida
     */
    /**
     * Crea usuario+persona+rolUsuario a partir de un DTO. La persona se
     * construye con id (si lo trae) o se crea nueva. Devuelve el DTO del
     * usuario persistido.
     *
     * @param dto datos del usuario nuevo (campos username, correo,
     *        personaDocumento, personaNombres)
     * @param rol rol a asignar
     * @return UsuarioDTO persistido, o null si hubo error
     */
    public UsuarioDTO crearUsuarioDesdeDTO(UsuarioDTO dto, Rol rol) {
        if (dto == null || rol == null) {
            return null;
        }
        // Reusa la persona si ya existe en BD (el blurEvent del controller
        // pre-carga personaId cuando la cédula coincide con un registro
        // existente en tb_persona). Si no, crea una persona nueva.
        Persona persona = null;
        boolean personaEsNueva = false;
        if (dto.getPersonaId() != null) {
            persona = personaFacade.find(dto.getPersonaId());
        }
        if (persona == null) {
            persona = new Persona();
            persona.setDocumento(dto.getPersonaDocumento());
            persona.setNombres(dto.getPersonaNombres());
            persona.setApellidos(dto.getPersonaApellidos());
            personaEsNueva = true;
        }

        Usuario nuevo = dto.toEntity();
        // Resuelve iglesia desde el id del DTO (solo aplica si se eligió rol IglesiaAdmin).
        Iglesia iglesia = null;
        if (dto.getIglesiaId() != null) {
            iglesia = iglesiaFacade.find(dto.getIglesiaId());
            nuevo.setIglesia(iglesia);
        }
        RolUsuario creado = crearUsuarioConRol(nuevo, persona, rol);
        if (creado == null) {
            return null;
        }

        // Si la persona se creó en este flujo y se asignó una iglesia,
        // registramos el vínculo en tb_iglesia_persona para que el usuario
        // figure como miembro/admin de esa iglesia desde el inicio.
        // Para personas EXISTENTES se confía en que el caller (controller)
        // gestione el vínculo via IglesiaPersonaService.crearVinculoSiNoExiste,
        // que es idempotente y aplica las reglas de negocio (bloqueo si la
        // persona ya pertenece a otra iglesia, etc.).
        if (personaEsNueva && iglesia != null) {
            IglesiaPersona vinculo = new IglesiaPersona();
            vinculo.setPersona(creado.getUsuario().getPersonsa());
            vinculo.setIglesia(iglesia);
            vinculo.setDesde(new java.sql.Timestamp(System.currentTimeMillis()));
            iglesiaPersonaFacade.create(vinculo);
        }
        return UsuarioDTO.fromEntity(creado.getUsuario());
    }

    /**
     * Actualiza correo y/o rol del usuario identificado por
     * {@code dto.getId()}. Reusa
     * {@link #actualizarUsuarioConRol(Usuario, RolUsuario, Rol)} pero recibe
     * un DTO. Si los datos no cambiaron, retorna el DTO actual sin tocar BD.
     *
     * @return UsuarioDTO con los datos posteriores a la operación, o null si
     *         el id es inválido
     */
    public UsuarioDTO actualizarUsuarioDesdeDTO(UsuarioDTO dto, RolUsuario rolUsuarioActual, Rol nuevoRol) {
        if (dto == null || dto.getId() == null) {
            return null;
        }
        Usuario actual = usuarioFacade.find(dto.getId());
        if (actual == null) {
            return null;
        }
        actual.setCorreo(dto.getCorreo());
        // Sincroniza iglesia: si el id viene null limpia el vínculo (cuando el
        // usuario deja de ser IglesiaAdmin); si trae id, resuelve y asigna.
        if (dto.getIglesiaId() != null) {
            actual.setIglesia(iglesiaFacade.find(dto.getIglesiaId()));
        } else {
            actual.setIglesia(null);
        }
        actualizarUsuarioConRol(actual, rolUsuarioActual, nuevoRol);
        return UsuarioDTO.fromEntity(actual);
    }

    /**
     * Devuelve el {@link UsuarioDTO} del IglesiaAdmin asignado a la iglesia
     * indicada, o {@code null} si la iglesia aún no tiene admin.
     */
    public UsuarioDTO obtenerAdminDeIglesia(Integer iglesiaId) {
        if (iglesiaId == null) {
            return null;
        }
        Usuario admin = usuarioFacade.findAdminByIglesiaId(iglesiaId);
        return UsuarioDTO.fromEntity(admin);
    }

    /**
     * Quita el rol IglesiaAdmin al usuario que actualmente administra la
     * iglesia indicada: limpia el vínculo {@code u.iglesia} y soft-deletea
     * la(s) relación(es) {@link RolUsuario} con rol IglesiaAdmin. El usuario
     * permanece activo para poder ser reasignado a otra iglesia o desempeñar
     * otros roles.
     *
     * @return DTO del admin previo (en su nuevo estado, sin iglesia), o
     *         {@code null} si la iglesia no tenía admin.
     */
    public UsuarioDTO removerAdminDeIglesia(Integer iglesiaId) {
        if (iglesiaId == null) {
            return null;
        }
        Usuario admin = usuarioFacade.findAdminByIglesiaId(iglesiaId);
        if (admin == null) {
            return null;
        }
        admin.setIglesia(null);
        usuarioFacade.edit(admin);
        // Soft-delete de los RolUsuario IglesiaAdmin del usuario. Otros roles
        // (Superadmin, Iglesia, etc.) se conservan: la persona puede seguir
        // operando en el sistema con las atribuciones que le queden.
        List<RolUsuario> rus = rolUsuarioFacade.findByUserNameAndRoleName2(
                admin.getUsername(), "%IglesiaAdmin");
        if (rus != null) {
            for (RolUsuario ru : rus) {
                if (ru.getRol() != null && ru.getRol().getNombre() != null
                        && ru.getRol().getNombre().endsWith("IglesiaAdmin")) {
                    rolUsuarioFacade.delete(ru);
                }
            }
        }
        return UsuarioDTO.fromEntity(admin);
    }

    /** Devuelve el DTO de un usuario por id, o null si no existe. */
    public UsuarioDTO obtenerDTOPorId(Integer id) {
        if (id == null) {
            return null;
        }
        return UsuarioDTO.fromEntity(usuarioFacade.find(id));
    }

    /**
     * Borra (soft-delete) un usuario por id. Devuelve el DTO en su estado
     * post-borrado, o null si no existía.
     */
    public UsuarioDTO eliminarPorId(Integer id) {
        if (id == null) {
            return null;
        }
        Usuario u = usuarioFacade.find(id);
        if (u == null) {
            return null;
        }
        return UsuarioDTO.fromEntity(usuarioFacade.delete(u));
    }

    /**
     * Lista usuarios distintos vinculados a cualquiera de los roles dados,
     * mapeados a DTO. Se apoya en {@code RolUsuarioService} pero retorna
     * directamente DTOs para que el controller no toque entidades.
     */
    public List<UsuarioDTO> listarDTOPorRoles(List<Rol> roles) {
        List<UsuarioDTO> resultado = new ArrayList<>();
        if (roles == null || roles.isEmpty()) {
            return resultado;
        }
        List<RolUsuario> rolesUsuarios = rolUsuarioFacade.getRolesUsuariosActivos(roles);
        if (rolesUsuarios == null) {
            return resultado;
        }
        for (RolUsuario ru : rolesUsuarios) {
            Usuario u = ru.getUsuario();
            if (u != null) {
                UsuarioDTO dto = UsuarioDTO.fromEntity(u);
                if (!resultado.contains(dto)) {
                    resultado.add(dto);
                }
            }
        }
        return resultado;
    }

    public boolean actualizarUsuarioConRol(Usuario usuarioActualizado,
            RolUsuario rolUsuarioActual, Rol nuevoRol) {
        if (usuarioActualizado == null || usuarioActualizado.getId() == null
                || rolUsuarioActual == null || nuevoRol == null) {
            return false;
        }
        Usuario actual = usuarioFacade.find(usuarioActualizado.getId());
        boolean correoCambio = actual != null
                && !actual.getCorreo().equals(usuarioActualizado.getCorreo());
        boolean rolCambio = rolUsuarioActual.getRol() == null
                || !rolUsuarioActual.getRol().getId().equals(nuevoRol.getId());
        if (!correoCambio && !rolCambio) {
            return false;
        }
        usuarioFacade.edit(usuarioActualizado);
        rolUsuarioActual.getUsuario().setId(usuarioActualizado.getId());
        rolUsuarioActual.setRol(nuevoRol);
        rolUsuarioFacade.edit(rolUsuarioActual);
        return true;
    }

    /**
     * Resuelve un usuario por nombre + prefijo de roles, cargando además su
     * persona asociada y los nombres de rol. No autentica contra credenciales —
     * solo construye el contexto de identidad. La autenticación contra
     * credenciales sigue siendo responsabilidad del contenedor (request.login).
     *
     * @param userName nombre de usuario
     * @param prefijoRoles prefijo para filtrar roles del aplicativo (ej.
     * "SITEC_"); si es null, devuelve un AuthDataDTO sin roles
     * @return AuthDataDTO con usuario, persona y roles; nunca null
     */
    /**
     * Aplica el cambio de contraseña: persiste el hash recibido, marca al
     * usuario como permanente y limpia la contraseña temporal. La validación
     * de complejidad de la clave y el hashing son responsabilidad del caller
     * (la capa UI usa {@code JsfUtil.validarContrasenia} y
     * {@code JsfUtil.claveEncriptadaSHA1}). El service solo asegura que el
     * usuario y el hash no son null/vacíos.
     *
     * @return el {@code Usuario} persistido, o {@code null} si la entrada es
     *         inválida
     */
    /**
     * Inicia el flujo de recuperación: busca al usuario por username + correo,
     * y si lo encuentra, establece la clave temporal en texto plano (campo
     * {@code contraseniaTemp}, para que el operador la copie en el primer
     * login) y persiste el hash como contraseña efectiva. Marca al usuario
     * como NO permanente — el siguiente login lo forzará a cambiar la clave.
     *
     * @param username RUC o documento de identidad
     * @param correo email registrado del usuario
     * @param claveTemporalPlana clave generada en el caller (texto plano para
     *        envío por correo)
     * @param hashClaveTemporal hash SHA-1 de la clave temporal
     * @return usuario actualizado, o null si no existe usuario con esa
     *         combinación o si los argumentos son inválidos
     */
    public Usuario iniciarRecuperacionClave(String username, String correo,
            String claveTemporalPlana, String hashClaveTemporal) {
        if (username == null || username.isEmpty() || correo == null || correo.isEmpty()
                || claveTemporalPlana == null || hashClaveTemporal == null) {
            return null;
        }
        Usuario usuario = usuarioFacade.findUsuarioByRucOrMail(username, correo);
        if (usuario == null) {
            return null;
        }
        usuario.setContraseniaTemp(claveTemporalPlana);
        usuario.setContrasenia(hashClaveTemporal);
        usuario.setPermanente(false);
        return usuarioFacade.edit(usuario);
    }

    public Usuario cambiarContrasenia(Usuario usuario, String hashClaveNueva) {
        if (usuario == null || hashClaveNueva == null || hashClaveNueva.isEmpty()) {
            return null;
        }
        usuario.setContraseniaTemp(null);
        usuario.setContrasenia(hashClaveNueva);
        usuario.setPermanente(true);
        return usuarioFacade.edit(usuario);
    }

    /**
     * Versión por id: hidrata la entidad desde la BD y aplica el cambio.
     * Retorna el {@link UsuarioDTO} actualizado, o null si no existe.
     */
    public UsuarioDTO cambiarContraseniaPorId(Integer usuarioId, String hashClaveNueva) {
        if (usuarioId == null) return null;
        Usuario u = usuarioFacade.find(usuarioId);
        if (u == null) return null;
        return UsuarioDTO.fromEntity(cambiarContrasenia(u, hashClaveNueva));
    }

    public AuthDataDTO resolverDatosAutenticacion(String userName, String prefijoRoles) {
        AuthDataDTO data = new AuthDataDTO();
        if (userName == null || userName.isEmpty()) {
            return data;
        }

        if (prefijoRoles != null) {
            List<RolUsuario> roles = rolUsuarioFacade.findByUserNameAndRoleName2(userName, prefijoRoles + "%");
            if (roles != null) {
                List<RolUsuarioDTO> rolesDTO = new ArrayList<>();
                List<String> nombres = new ArrayList<>();
                for (RolUsuario ru : roles) {
                    rolesDTO.add(RolUsuarioDTO.fromEntity(ru));
                    if (ru.getRol() != null) {
                        nombres.add(ru.getRol().getNombre());
                    }
                }
                data.setRolesUsuario(rolesDTO);
                data.setNombresRoles(nombres);
            }
        }

        // findByUsuarioName ahora trae u.personsa via JOIN FETCH, así que la
        // persona se mapea en una sola query (antes se hacían 2 round-trips).
        Usuario usuario = usuarioFacade.findByUsuarioName(userName);
        data.setUsuario(UsuarioDTO.fromEntity(usuario));
        if (usuario != null && usuario.getPersonsa() != null) {
            data.setPersona(PersonaDTO.fromEntity(usuario.getPersonsa()));
        }
        return data;
    }
}
