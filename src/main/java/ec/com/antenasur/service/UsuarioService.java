package ec.com.antenasur.service;

import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.AuthDataDTO;
import ec.com.antenasur.dto.RolUsuarioDTO;
import ec.com.antenasur.dto.ResultadoProvisionUsuarioDTO;
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
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.util.Constantes;

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

    @Inject
    private PasswordService passwordService;

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

    public Usuario findUsuarioPorPersonaIncluyendoInactivos(Integer personaId) {
        return usuarioFacade.findByPersonaIdIncluyendoInactivos(personaId);
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
        // hasheada en BCrypt. El usuario podrá cambiarla después.
        if (usuario.getContrasenia() == null || usuario.getContrasenia().isEmpty()) {
            String cedula = personaPersistida.getDocumento();
            if (cedula != null && !cedula.isEmpty()) {
                usuario.setContrasenia(passwordService.hashBcrypt(cedula));
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
        validarDatosCreacion(dto, rol);
        dto.setUsername(dto.getUsername().trim());
        dto.setPersonaDocumento(dto.getPersonaDocumento().trim());

        Persona persona = resolverPersona(dto);
        boolean personaEsNueva = persona.getId() == null;
        Iglesia iglesia = resolverIglesiaParaRol(dto.getIglesiaId(), rol, null);

        // usu_nombre tiene una restricción única que incluye los registros
        // inactivos. Por eso una cuenta dada de baja se restaura en vez de
        // insertar una segunda fila con la misma identidad de acceso.
        Usuario existente = usuarioFacade.findByUsuarioNameIncluyendoInactivos(dto.getUsername());
        if (existente != null) {
            if (Boolean.TRUE.equals(existente.getEstado())) {
                throw new NegocioException("Ya existe un usuario activo con el nombre " + dto.getUsername() + ".");
            }
            validarPersonaDeCuentaExistente(existente, dto.getPersonaDocumento());
            if (existente.getPersonsa() == null) {
                existente.setPersonsa(persistirPersonaSiNecesario(persona));
            }
            existente.setCorreo(dto.getCorreo());
            existente.setIglesia(iglesia);
            existente.setEstado(true);
            usuarioFacade.edit(existente);
            activarSoloRolSeleccionado(existente, rol);

            UsuarioDTO reactivado = UsuarioDTO.fromEntity(existente);
            reactivado.setReactivado(true);
            return reactivado;
        }

        if (persona.getId() != null) {
            Usuario cuentaDeLaPersona = usuarioFacade.findByPersonaIdIncluyendoInactivos(persona.getId());
            if (cuentaDeLaPersona != null) {
                String estado = Boolean.TRUE.equals(cuentaDeLaPersona.getEstado()) ? "activo" : "eliminado";
                throw new NegocioException("La persona ya tiene un usuario " + estado + " ("
                        + cuentaDeLaPersona.getUsername() + ").");
            }
        }

        Persona personaPersistida = persistirPersonaSiNecesario(persona);
        Usuario nuevo = dto.toEntity();
        nuevo.setPersonsa(personaPersistida);
        nuevo.setIglesia(iglesia);
        nuevo.setEstado(true);
        nuevo.setPermanente(false);
        nuevo.setContrasenia(passwordService.hashBcrypt(personaPersistida.getDocumento()));
        Usuario usuarioPersistido = usuarioFacade.create(nuevo);

        RolUsuario relacion = new RolUsuario();
        relacion.setUsuario(usuarioPersistido);
        relacion.setRol(rol);
        rolUsuarioFacade.create(relacion);

        if (personaEsNueva && iglesia != null) {
            IglesiaPersona vinculo = new IglesiaPersona();
            vinculo.setPersona(personaPersistida);
            vinculo.setIglesia(iglesia);
            vinculo.setDesde(new java.sql.Timestamp(System.currentTimeMillis()));
            iglesiaPersonaFacade.create(vinculo);
        }
        return UsuarioDTO.fromEntity(usuarioPersistido);
    }

    public Usuario asegurarUsuarioConRol(Persona persona, Rol rol) {
        if (persona == null || persona.getDocumento() == null || persona.getDocumento().isBlank() || rol == null) {
            throw new NegocioException("No se pudo crear el usuario porque faltan datos de la persona o del rol.");
        }
        Usuario usuario = usuarioFacade.findByUsuarioNameIncluyendoInactivos(persona.getDocumento());
        if (usuario == null) {
            usuario = new Usuario();
            usuario.setUsername(persona.getDocumento());
            usuario.setPersonsa(persona);
            usuario.setEstado(true);
            usuario.setPermanente(false);
            usuario.setContraseniaTemp(persona.getDocumento());
            usuario.setContrasenia(passwordService.hashBcrypt(persona.getDocumento()));
            usuario = usuarioFacade.create(usuario);
        } else {
            boolean reactivado = !Boolean.TRUE.equals(usuario.getEstado());
            usuario.setEstado(true);
            if (usuario.getPersonsa() == null) {
                usuario.setPersonsa(persona);
            }
            usuario = usuarioFacade.edit(usuario);
            if (reactivado) {
                desactivarRoles(usuario);
            }
        }
        asegurarRolActivo(usuario, rol);
        return usuario;
    }

    /**
     * Asegura una cuenta para una persona ya registrada y habilita el rol
     * solicitado. Conserva los demas roles legitimos de la cuenta y solo
     * reactiva la relacion requerida cuando estaba dada de baja.
     */
    public ResultadoProvisionUsuarioDTO provisionarUsuarioExistenteConRol(
            Persona persona, String correo, Rol rol) {
        return provisionarUsuarioExistenteConRol(persona, correo, rol, null);
    }

    /**
     * Variante para roles que requieren una iglesia concreta, como
     * SITEC-IglesiaAdmin. La validacion de disponibilidad se mantiene en este
     * servicio para que no dependa de la vista.
     */
    public ResultadoProvisionUsuarioDTO provisionarUsuarioExistenteConRol(
            Persona persona, String correo, Rol rol, Integer iglesiaId) {
        if (persona == null || persona.getId() == null || persona.getDocumento() == null
                || persona.getDocumento().isBlank() || rol == null || rol.getId() == null) {
            throw new NegocioException("usuarios.mensaje.datos.incompletos");
        }
        String correoNormalizado = normalizarCorreoObligatorio(correo);
        Usuario porDocumento = usuarioFacade.findByUsuarioNameIncluyendoInactivos(persona.getDocumento());
        Usuario porPersona = usuarioFacade.findByPersonaIdIncluyendoInactivos(persona.getId());
        if (porDocumento != null && porPersona != null && !porDocumento.getId().equals(porPersona.getId())) {
            throw new NegocioException("usuarios.mensaje.identidad.conflicto");
        }
        Usuario usuario = porPersona != null ? porPersona : porDocumento;
        Usuario porCorreo = usuarioFacade.findByCorreoIncluyendoInactivos(correoNormalizado);
        if (porCorreo != null && (usuario == null || !porCorreo.getId().equals(usuario.getId()))) {
            throw new NegocioException("usuarios.mensaje.correo.registrado");
        }

        boolean reutilizado = usuario != null;
        boolean reactivado = reutilizado && !Boolean.TRUE.equals(usuario.getEstado());
        if (!reutilizado) {
            usuario = new Usuario();
            usuario.setUsername(persona.getDocumento().trim());
            usuario.setPersonsa(persona);
            usuario.setCorreo(correoNormalizado);
            usuario.setEstado(true);
            usuario.setPermanente(false);
            usuario.setContrasenia(passwordService.hashBcrypt(persona.getDocumento().trim()));
            usuario = usuarioFacade.create(usuario);
        } else {
            validarPersonaDeCuentaExistente(usuario, persona.getDocumento().trim());
            if (usuario.getPersonsa() == null) {
                usuario.setPersonsa(persona);
            }
            usuario.setCorreo(correoNormalizado);
            usuario.setEstado(true);
            usuario = usuarioFacade.edit(usuario);
        }
        asegurarRolActivo(usuario, rol);
        Iglesia iglesia = resolverIglesiaParaRol(iglesiaId, rol, usuario.getId());
        if (iglesia != null) {
            Integer iglesiaActualId = usuario.getIglesia() != null ? usuario.getIglesia().getId() : null;
            if (iglesiaActualId != null && !iglesiaActualId.equals(iglesia.getId())) {
                throw new NegocioException("iglesias.admin.error.usuario.otra.iglesia");
            }
            usuario.setIglesia(iglesia);
            usuario = usuarioFacade.edit(usuario);
        }
        return new ResultadoProvisionUsuarioDTO(usuario, reutilizado, reactivado);
    }

    /**
     * Retira exclusivamente el rol indicado. La cuenta solo se da de baja si
     * ya no conserva ningun otro rol activo.
     */
    public boolean retirarRolDePersonaSiNoTieneOtrosRoles(Persona persona, Rol rol) {
        if (persona == null || persona.getId() == null || rol == null || rol.getId() == null) {
            return false;
        }
        Usuario usuario = usuarioFacade.findByPersonaIdIncluyendoInactivos(persona.getId());
        if (usuario == null) {
            return false;
        }
        RolUsuario relacion = buscarRelacionPorRol(usuario.getId(), rol.getId());
        if (relacion != null && Boolean.TRUE.equals(relacion.getEstado())) {
            rolUsuarioFacade.delete(relacion);
        }
        boolean tieneOtroRolActivo = rolUsuarioFacade.findByUsuarioIdIncluyendoInactivos(usuario.getId()).stream()
                .anyMatch(item -> Boolean.TRUE.equals(item.getEstado()) && item.getRol() != null
                        && Boolean.TRUE.equals(item.getRol().getEstado()));
        if (!tieneOtroRolActivo && Boolean.TRUE.equals(usuario.getEstado())) {
            usuario.setEstado(false);
            usuarioFacade.edit(usuario);
        }
        return true;
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
        if (dto == null || dto.getId() == null || rolUsuarioActual == null || nuevoRol == null) {
            throw new NegocioException("No se pudo determinar el usuario y rol a actualizar.");
        }
        Usuario actual = usuarioFacade.find(dto.getId());
        if (actual == null) {
            throw new NegocioException("El usuario ya no está activo o no existe.");
        }
        RolUsuario relacionActual = rolUsuarioFacade.find(rolUsuarioActual.getId());
        if (relacionActual == null || relacionActual.getUsuario() == null
                || !actual.getId().equals(relacionActual.getUsuario().getId())) {
            throw new NegocioException("La relación de rol del usuario no es válida.");
        }

        Iglesia iglesia = resolverIglesiaParaRol(dto.getIglesiaId(), nuevoRol, actual.getId());
        boolean rolCambio = !relacionActual.getRol().getId().equals(nuevoRol.getId());
        RolUsuario relacionDestino = rolCambio
                ? buscarRelacionPorRol(actual.getId(), nuevoRol.getId()) : null;
        if (relacionDestino != null && !relacionDestino.getId().equals(relacionActual.getId())
                && Boolean.TRUE.equals(relacionDestino.getEstado())) {
            throw new NegocioException("El usuario ya tiene asignado el rol seleccionado.");
        }
        boolean correoCambio = !java.util.Objects.equals(actual.getCorreo(), dto.getCorreo());
        boolean iglesiaCambio = !java.util.Objects.equals(
                actual.getIglesia() != null ? actual.getIglesia().getId() : null,
                iglesia != null ? iglesia.getId() : null);
        actual.setCorreo(dto.getCorreo());
        actual.setIglesia(iglesia);
        if (correoCambio || iglesiaCambio) {
            usuarioFacade.edit(actual);
        }

        if (rolCambio) {
            if (relacionDestino != null && !relacionDestino.getId().equals(relacionActual.getId())) {
                rolUsuarioFacade.delete(relacionActual);
                relacionDestino.setEstado(true);
                rolUsuarioFacade.edit(relacionDestino);
            } else {
                relacionActual.setRol(nuevoRol);
                rolUsuarioFacade.edit(relacionActual);
            }
        }
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

    /** Consulta puntual usada al preparar asignaciones; incluye cuentas dadas de baja. */
    public UsuarioDTO obtenerUsuarioPorPersonaIncluyendoInactivos(Integer personaId) {
        if (personaId == null) {
            return null;
        }
        return UsuarioDTO.fromEntity(usuarioFacade.findByPersonaIdIncluyendoInactivos(personaId));
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
        // La eliminación es lógica. Se libera la iglesia en esta misma
        // transacción y se inactivan los roles para no conservar permisos ni
        // bloquear una futura asignación de otro IglesiaAdmin.
        u.setIglesia(null);
        desactivarRoles(u);
        return UsuarioDTO.fromEntity(usuarioFacade.delete(u));
    }

    /**
     * Reactiva una cuenta eliminada lógicamente y el rol elegido por el
     * administrador. No se inserta ningún registro ni se restauran roles
     * distintos al seleccionado.
     */
    public UsuarioDTO reactivarPorId(Integer usuarioId, Integer rolUsuarioId, Integer iglesiaId) {
        if (usuarioId == null || rolUsuarioId == null) {
            throw new NegocioException("No se pudo determinar el usuario y rol a reactivar.");
        }
        Usuario usuario = usuarioFacade.findByIdIncluyendoInactivos(usuarioId);
        if (usuario == null || Boolean.TRUE.equals(usuario.getEstado())) {
            throw new NegocioException("El usuario no está dado de baja o ya fue reactivado.");
        }

        RolUsuario relacionSeleccionada = null;
        for (RolUsuario relacion : rolUsuarioFacade.findByUsuarioIdIncluyendoInactivos(usuarioId)) {
            if (rolUsuarioId.equals(relacion.getId())) {
                relacionSeleccionada = relacion;
                break;
            }
        }
        if (relacionSeleccionada == null || relacionSeleccionada.getRol() == null
                || !Boolean.TRUE.equals(relacionSeleccionada.getRol().getEstado())) {
            throw new NegocioException("El rol seleccionado ya no está disponible para reactivar el usuario.");
        }

        // Un IglesiaAdmin dado de baja no conserva iglesia. Al reactivarlo debe
        // tomar una iglesia disponible, validada antes de habilitar cuenta/rol.
        Iglesia iglesia = resolverIglesiaParaRol(iglesiaId, relacionSeleccionada.getRol(), usuarioId);
        usuario.setIglesia(iglesia);
        usuario.setEstado(true);
        usuarioFacade.edit(usuario);
        relacionSeleccionada.setEstado(true);
        rolUsuarioFacade.edit(relacionSeleccionada);

        UsuarioDTO reactivado = UsuarioDTO.fromEntity(usuario);
        reactivado.setReactivado(true);
        return reactivado;
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
                && !java.util.Objects.equals(actual.getCorreo(), usuarioActualizado.getCorreo());
        boolean rolCambio = rolUsuarioActual.getRol() == null
                || !rolUsuarioActual.getRol().getId().equals(nuevoRol.getId());
        boolean iglesiaCambio = actual != null && !java.util.Objects.equals(
                actual.getIglesia() != null ? actual.getIglesia().getId() : null,
                usuarioActualizado.getIglesia() != null ? usuarioActualizado.getIglesia().getId() : null);
        if (!correoCambio && !rolCambio && !iglesiaCambio) {
            return false;
        }
        UsuarioDTO dto = UsuarioDTO.fromEntity(usuarioActualizado);
        actualizarUsuarioDesdeDTO(dto, rolUsuarioActual, nuevoRol);
        return true;
    }

    private void validarDatosCreacion(UsuarioDTO dto, Rol rol) {
        if (dto == null || rol == null || rol.getId() == null) {
            throw new NegocioException("Debe seleccionar un rol para el usuario.");
        }
        if (dto.getUsername() == null || dto.getUsername().isBlank()
                || dto.getPersonaDocumento() == null || dto.getPersonaDocumento().isBlank()
                || dto.getPersonaNombres() == null || dto.getPersonaNombres().isBlank()) {
            throw new NegocioException("Cédula, nombres y usuario son obligatorios.");
        }
    }

    private Persona resolverPersona(UsuarioDTO dto) {
        Persona persona = dto.getPersonaId() != null ? personaFacade.find(dto.getPersonaId()) : null;
        if (persona != null && !dto.getPersonaDocumento().equals(persona.getDocumento())) {
            throw new NegocioException("La cédula no coincide con la persona seleccionada.");
        }
        if (persona == null) {
            persona = personaFacade.finByPersonaDocument(dto.getPersonaDocumento());
        }
        if (persona == null) {
            persona = new Persona();
            persona.setDocumento(dto.getPersonaDocumento());
            persona.setNombres(dto.getPersonaNombres());
            persona.setApellidos(dto.getPersonaApellidos());
            persona.setEstado(true);
        }
        return persona;
    }

    private Persona persistirPersonaSiNecesario(Persona persona) {
        return persona.getId() == null ? personaFacade.create(persona) : persona;
    }

    private String normalizarCorreoObligatorio(String correo) {
        if (correo == null || correo.isBlank()) {
            throw new NegocioException("usuarios.mensaje.correo.requerido");
        }
        String normalizado = correo.trim().toLowerCase(java.util.Locale.ROOT);
        if (!normalizado.matches("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$")) {
            throw new NegocioException("usuarios.mensaje.correo.invalido");
        }
        return normalizado;
    }

    private Iglesia resolverIglesiaParaRol(Integer iglesiaId, Rol rol, Integer usuarioId) {
        boolean requiereIglesia = rol != null && rol.getNombre() != null
                && rol.getNombre().endsWith(Constantes.getRolIglesiaAdmin());
        if (!requiereIglesia) {
            return null;
        }
        if (iglesiaId == null) {
            throw new NegocioException("Debe seleccionar una iglesia para el rol IglesiaAdmin.");
        }
        // El bloqueo de la iglesia serializa la comprobación y la asignación
        // frente a otras altas, cambios de rol o reactivaciones concurrentes.
        Iglesia iglesia = iglesiaFacade.findForAdminAssignment(iglesiaId);
        if (iglesia == null) {
            throw new NegocioException("La iglesia seleccionada no está disponible.");
        }
        Usuario adminActual = usuarioFacade.findAdminByIglesiaId(iglesiaId);
        if (adminActual != null && (usuarioId == null || !adminActual.getId().equals(usuarioId))) {
            throw new NegocioException("La iglesia seleccionada ya tiene un usuario administrador activo.");
        }
        return iglesia;
    }

    private void validarPersonaDeCuentaExistente(Usuario existente, String documento) {
        if (existente.getPersonsa() != null && existente.getPersonsa().getDocumento() != null
                && !existente.getPersonsa().getDocumento().equals(documento)) {
            throw new NegocioException("El usuario eliminado pertenece a otra persona y no puede reutilizarse.");
        }
    }

    private void desactivarRoles(Usuario usuario) {
        for (RolUsuario relacion : rolUsuarioFacade.findByUsuarioIdIncluyendoInactivos(usuario.getId())) {
            if (Boolean.TRUE.equals(relacion.getEstado())) {
                rolUsuarioFacade.delete(relacion);
            }
        }
    }

    private void activarSoloRolSeleccionado(Usuario usuario, Rol rol) {
        desactivarRoles(usuario);
        asegurarRolActivo(usuario, rol);
    }

    private void asegurarRolActivo(Usuario usuario, Rol rol) {
        RolUsuario relacion = buscarRelacionPorRol(usuario.getId(), rol.getId());
        if (relacion == null) {
            relacion = new RolUsuario();
            relacion.setUsuario(usuario);
            relacion.setRol(rol);
            relacion.setEstado(true);
            rolUsuarioFacade.create(relacion);
        } else if (!Boolean.TRUE.equals(relacion.getEstado())) {
            relacion.setEstado(true);
            rolUsuarioFacade.edit(relacion);
        }
    }

    private RolUsuario buscarRelacionPorRol(Integer usuarioId, Integer rolId) {
        for (RolUsuario relacion : rolUsuarioFacade.findByUsuarioIdIncluyendoInactivos(usuarioId)) {
            if (relacion.getRol() != null && rolId.equals(relacion.getRol().getId())) {
                return relacion;
            }
        }
        return null;
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
     * @return AuthDataDTO con usuario y roles; nunca null
     */
    /**
     * Aplica el cambio de contraseña: persiste el hash recibido, marca al
     * usuario como permanente y limpia la contraseña temporal. La validación
     * de complejidad de la clave y el hashing son responsabilidad del caller
     * (la capa UI usa {@code JsfUtil.validarContrasenia}). El service solo
     * asegura que el usuario y el hash no son null/vacíos.
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
     * @param hashClaveTemporal hash BCrypt de la clave temporal
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

    public AuthDataDTO cargarContextoUsuarioAutenticado(String userName, String prefijoRoles) {
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
        return data;
    }
}
