package ec.com.antenasur.service.tec;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import ec.com.antenasur.dto.EstadoJuntaDTO;

import ec.com.antenasur.dto.MiembroJRVDTO;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.facade.IglesiaPersonaFacade;
import ec.com.antenasur.facade.RolFacade;
import ec.com.antenasur.facade.tec.CatalogoGeneralFacade;
import ec.com.antenasur.facade.tec.GestionPadronFacade;
import ec.com.antenasur.facade.tec.MesaFacade;
import ec.com.antenasur.facade.tec.MiembroJRVFacade;
import ec.com.antenasur.facade.tec.PadronFacade;
import ec.com.antenasur.facade.tec.ProcesoElectoralFacade;
import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.Persona;
import ec.com.antenasur.model.Rol;
import ec.com.antenasur.model.Usuario;
import ec.com.antenasur.model.tec.CatalogoGeneral;
import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.model.tec.MiembroJRV;
import ec.com.antenasur.model.tec.Padron;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.service.AbstractService;
import ec.com.antenasur.service.UsuarioService;
import ec.com.antenasur.util.Constantes;

/**
 * Designacion de la JRV. Sin restriccion de clase (@PermitAll mantiene el comportamiento de
 * siempre para JRV, el acta y la App): solo la reapertura de una junta exige Administrador o
 * Tribunal. WildFly niega los metodos sin anotar de un EJB con seguridad, por eso la clase.
 */
@Stateless
@jakarta.annotation.security.PermitAll
@jakarta.annotation.security.DeclareRoles({ "SITEC-Administrador", "SITEC-Tribunal" })
public class MiembroJRVService extends AbstractService<MiembroJRV, Integer, MiembroJRVFacade> {

    /** Accion de bitacora de la reapertura (CatalogoActividades la muestra como "Reabrio junta JRV"). */
    static final String ACTIVIDAD_REABRIR = "REABRE_JUNTA_JRV";

    @Inject
    private GestionPadronFacade gestionPadronFacade;

    @Inject
    private ProcesoService procesoService;

    /**
     * Reabre una junta completada para corregir sus designaciones (p. ej. reemplazar al
     * Presidente). Solo Administrador o Tribunal, con motivo, y solo si el escrutinio de la mesa
     * no ha iniciado. Limpia el responsable de la mesa (el usuario del Presidente), que es lo
     * que marca la junta como completada, y deja la accion en la bitacora.
     */
    @jakarta.annotation.security.RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal" })
    public void reabrirJunta(Integer mesaId, Integer procesoId, String motivo, String ip) {
        if (mesaId == null || procesoId == null) {
            throw new NegocioException("Debe seleccionar una mesa y un proceso electoral activo.");
        }
        String motivoLimpio = motivo == null ? "" : motivo.trim().replaceAll("[\\r\\n\\t]+", " ");
        if (motivoLimpio.isEmpty()) {
            throw new NegocioException(Constantes.getMensaje("mjrv.reabrir.motivo.requerido"));
        }
        if (!juntaCompletadaRegistrada(mesaId, procesoId)) {
            throw new NegocioException(Constantes.getMensaje("mjrv.reabrir.no.completada"));
        }
        if (gestionPadronFacade.escrutinioIniciado(mesaId, procesoId)) {
            throw new NegocioException(Constantes.getMensaje("mjrv.reabrir.escrutinio"));
        }
        Mesa mesa = mesaFacade.find(mesaId);
        String presidenteAnterior = mesa.getResponsable();
        mesa.setResponsable(null);
        mesaFacade.edit(mesa);

        String recinto = mesa.getRecinto() != null ? mesa.getRecinto().getNombre() : "";
        String prefijo = ACTIVIDAD_REABRIR + " | M\u00d3DULO: JRV; RESULTADO: EXITOSO; DETALLE: Mesa: " + mesa.getNombre()
                + "; Recinto: " + recinto + "; Presidente anterior: " + presidenteAnterior + "; Motivo: ";
        // tec.procesos.actividad admite 255 caracteres: se acorta el motivo, nunca el formato.
        int disponible = Math.max(0, 255 - prefijo.length());
        ec.com.antenasur.model.tec.Proceso actividad = new ec.com.antenasur.model.tec.Proceso(ip);
        actividad.setActividad(prefijo + motivoLimpio.substring(0, Math.min(motivoLimpio.length(), disponible)));
        procesoService.create(actividad);
    }

    public static final List<String> DIGNIDADES_OBLIGATORIAS = List.of("PRESIDENTE", "SECRETARIO", "TESORERO", "VOCAL");

    @Inject
    private MiembroJRVFacade miembroJRVFacade;

    @Inject
    private IglesiaPersonaFacade iglesiaPersonaFacade;

    @Inject
    private MesaFacade mesaFacade;

    @Inject
    private ProcesoElectoralFacade procesoElectoralFacade;

    @Inject
    private CatalogoGeneralFacade catalogoGeneralFacade;

    @Inject
    private PadronFacade padronFacade;

    @Inject
    private UsuarioService usuarioService;

    @Inject
    private RolFacade rolFacade;

    @Override
    protected MiembroJRVFacade getFacade() {
        return miembroJRVFacade;
    }

    public Set<MiembroJRV> getJRVPorMesa(Mesa mesa) {
        return miembroJRVFacade.getJRVPorMesa(mesa);
    }

    public MiembroJRVDTO obtenerDTOPorId(Integer id) {
        if (id == null) return null;
        return MiembroJRVDTO.fromEntity(miembroJRVFacade.find(id));
    }

    public List<MiembroJRVDTO> listarDTOs() {
        List<MiembroJRVDTO> resultado = new ArrayList<>();
        List<MiembroJRV> ms = miembroJRVFacade.findAll();
        if (ms == null) return resultado;
        for (MiembroJRV m : ms) resultado.add(MiembroJRVDTO.fromEntity(m));
        return resultado;
    }

    /** Miembro retirado y, si era el Presidente, el acceso que se retiró a su cuenta. */
    public record ResultadoRetiro(MiembroJRVDTO miembro, ec.com.antenasur.service.UsuarioService.AccesoRetirado acceso,
            String usuario) {
    }

    /**
     * Retira un miembro de una junta abierta. Si era el Presidente y no preside otra mesa del
     * proceso, su cuenta pierde el rol SITEC-Presidente-mesa y, si no le queda otro rol, se
     * desactiva: no tiene sentido un acceso sin responsabilidad. UsuarioService exige
     * Administrador o Tribunal para esa baja.
     */
    public ResultadoRetiro retirarMiembro(Integer id) {
        MiembroJRV m = id == null ? null : miembroJRVFacade.find(id);
        if (m == null) {
            return null;
        }
        boolean eraPresidente = m.getCargo() != null && "PRESIDENTE".equals(normalizarCargo(m.getCargo().getNombre()));
        Persona persona = m.getIglesiaPersona() != null ? m.getIglesiaPersona().getPersona() : null;
        Integer procesoId = m.getProceso() != null ? m.getProceso().getId() : null;
        MiembroJRVDTO retirado = eliminarPorId(id);
        var ninguno = ec.com.antenasur.service.UsuarioService.AccesoRetirado.NINGUNO;
        if (!eraPresidente || persona == null) {
            return new ResultadoRetiro(retirado, ninguno, null);
        }
        if (miembroJRVFacade.buscarPresidentePorPersonaProceso(persona.getId(), procesoId) != null) {
            return new ResultadoRetiro(retirado, ninguno, null);
        }
        Rol rol = obtenerRolPresidenteMesa();
        if (rol == null) {
            return new ResultadoRetiro(retirado, ninguno, null);
        }
        return new ResultadoRetiro(retirado, usuarioService.retirarRolPresidenteMesa(persona, rol), persona.getDocumento());
    }

    public MiembroJRVDTO eliminarPorId(Integer id) {
        if (id == null) return null;
        MiembroJRV m = miembroJRVFacade.find(id);
        if (m == null) return null;
        Integer mesaId = m.getMesa() != null ? m.getMesa().getId() : null;
        Integer procesoId = m.getProceso() != null ? m.getProceso().getId() : null;
        if (juntaCompletadaRegistrada(mesaId, procesoId)) {
            throw new NegocioException("La junta ya fue completada. Para modificarla debe reabrirse mediante una accion autorizada.");
        }
        return MiembroJRVDTO.fromEntity(miembroJRVFacade.delete(m));
    }

    public List<MiembroJRVDTO> listarDTOsPorMesaProceso(Integer mesaId, Integer procesoId) {
        List<MiembroJRVDTO> resultado = new ArrayList<>();
        for (MiembroJRV miembro : miembroJRVFacade.listarPorMesaProceso(mesaId, procesoId)) {
            resultado.add(MiembroJRVDTO.fromEntity(miembro));
        }
        return resultado;
    }

    public MiembroJRVDTO designarMiembro(Integer iglesiaPersonaId, Integer mesaId, Integer procesoId, Integer cargoId) {
        validarDesignacion(iglesiaPersonaId, mesaId, procesoId, cargoId);
        if (!perteneceAPadronDeMesa(iglesiaPersonaId, mesaId, procesoId)) {
            throw new NegocioException("La persona seleccionada no pertenece a una iglesia asignada a esta mesa.");
        }
        return guardarDesignacion(iglesiaPersonaId, mesaId, procesoId, cargoId, null);
    }

    /**
     * Busca por cédula a una persona habilitada en el padrón del proceso (en cualquier mesa) para
     * designarla en la JRV de la mesa indicada. Solo Administrador o Tribunal.
     */
    @jakarta.annotation.security.RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal" })
    public ec.com.antenasur.dto.CandidatoJrvDTO buscarCandidatoPorCedula(String cedula, Integer mesaId,
            Integer procesoId) {
        if (mesaId == null || procesoId == null) {
            throw new NegocioException("Debe seleccionar una mesa y un proceso electoral activo.");
        }
        String limpia = cedula == null ? "" : cedula.trim();
        if (!limpia.matches("\\d{10}")) {
            throw new NegocioException(Constantes.getMensaje("mjrv.otra.cedula.invalida"));
        }
        List<Padron> registros = padronFacade.buscarHabilitadoPorCedulaProceso(limpia, procesoId);
        if (registros.isEmpty()) {
            throw new NegocioException(Constantes.getMensaje("mjrv.otra.no.empadronado"));
        }
        if (registros.size() > 1) {
            // Una persona en dos registros del padrón del mismo proceso: no se elige por el usuario.
            throw new NegocioException(Constantes.getMensaje("mjrv.otra.padron.duplicado"));
        }
        Padron padron = registros.get(0);
        IglesiaPersona ip = padron.getIglesiaPersona();
        MiembroJRV existente = miembroJRVFacade.buscarPorIglesiaPersonaProceso(ip.getId(), procesoId);
        if (existente != null) {
            String mesaExistente = existente.getMesa() != null ? existente.getMesa().getNombre() : "";
            throw new NegocioException(Constantes.getMensaje("mjrv.otra.ya.designado", mesaExistente));
        }
        Persona persona = ip.getPersona();
        Mesa mesaVoto = padron.getMesa();
        return new ec.com.antenasur.dto.CandidatoJrvDTO(ip.getId(), persona.getDocumento(),
                (texto(persona.getNombres()) + " " + texto(persona.getApellidos())).trim(),
                ip.getIglesia() != null ? texto(ip.getIglesia().getNombre()) : "",
                texto(mesaVoto.getNombre()),
                mesaVoto.getRecinto() != null ? texto(mesaVoto.getRecinto().getNombre()) : "",
                mesaId.equals(mesaVoto.getId()));
    }

    /**
     * Designa en la JRV de la mesa a una persona empadronada en otra mesa del mismo proceso. Exige
     * la aceptación expresa de la condición; la persona sigue votando en su mesa (no se toca el
     * padrón). Se aplican las demás reglas de la designación. Solo Administrador o Tribunal.
     */
    @jakarta.annotation.security.RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal" })
    public MiembroJRVDTO designarMiembroOtraMesa(Integer iglesiaPersonaId, Integer mesaId, Integer procesoId,
            Integer cargoId, boolean condicionAceptada, String ip) {
        validarDesignacion(iglesiaPersonaId, mesaId, procesoId, cargoId);
        Padron padron = padronFacade.buscaPorPersonaProcesoIglesia(iglesiaPersonaId, procesoId);
        IglesiaPersona persona = iglesiaPersonaFacade.find(iglesiaPersonaId);
        if (padron == null || persona == null || !Boolean.TRUE.equals(persona.getEstado())
                || !Boolean.TRUE.equals(persona.getHabilitadoPadron())) {
            throw new NegocioException(Constantes.getMensaje("mjrv.otra.no.empadronado"));
        }
        if (perteneceAPadronDeMesa(iglesiaPersonaId, mesaId, procesoId)) {
            // Es de esta mesa: designación normal, sin marca ni advertencia.
            return guardarDesignacion(iglesiaPersonaId, mesaId, procesoId, cargoId, null);
        }
        if (!condicionAceptada) {
            throw new NegocioException(Constantes.getMensaje("mjrv.otra.condicion.requerida"));
        }
        String usuario = usuarioActual();
        MiembroJRVDTO designado = guardarDesignacion(iglesiaPersonaId, mesaId, procesoId, cargoId,
                usuario == null ? "" : usuario);
        registrarDesignacionOtraMesa(designado, padron, ip);
        return designado;
    }

    @jakarta.annotation.Resource
    private jakarta.ejb.SessionContext sessionContext;

    private String usuarioActual() {
        try {
            return sessionContext != null && sessionContext.getCallerPrincipal() != null
                    ? sessionContext.getCallerPrincipal().getName()
                    : null;
        } catch (IllegalStateException e) {
            return null;
        }
    }

    /** Bitácora de la designación desde otra mesa (CatalogoActividades: "Designó miembro JRV de otra mesa"). */
    private void registrarDesignacionOtraMesa(MiembroJRVDTO designado, Padron padron, String ip) {
        Mesa mesa = designado.getMesa() != null ? mesaFacade.find(designado.getMesa().getId()) : null;
        Mesa mesaVoto = padron.getMesa();
        String cedula = designado.getIglesiaPersona() != null && designado.getIglesiaPersona().getPersona() != null
                ? designado.getIglesiaPersona().getPersona().getDocumento()
                : "";
        String detalle = "DESIGNA_JRV_OTRA_MESA | MÓDULO: JRV; RESULTADO: EXITOSO; DETALLE: Mesa: "
                + (mesa != null ? mesa.getNombre() : "") + "; Recinto: "
                + (mesa != null && mesa.getRecinto() != null ? mesa.getRecinto().getNombre() : "")
                + "; Dignidad: " + texto(designado.getCargoNombre()) + "; Miembro: " + cedula
                + "; Vota en: " + (mesaVoto != null ? mesaVoto.getNombre() : "") + " ("
                + (mesaVoto != null && mesaVoto.getRecinto() != null ? mesaVoto.getRecinto().getNombre() : "") + ")";
        ec.com.antenasur.model.tec.Proceso actividad = new ec.com.antenasur.model.tec.Proceso(ip);
        actividad.setActividad(detalle.length() > 255 ? detalle.substring(0, 255) : detalle);
        procesoService.create(actividad);
    }

    /** Reglas comunes de toda designación, salvo la pertenencia al padrón de la mesa. */
    private void validarDesignacion(Integer iglesiaPersonaId, Integer mesaId, Integer procesoId, Integer cargoId) {
        validarParametros(iglesiaPersonaId, mesaId, procesoId, cargoId);
        if (padronFacade.contarIglesiasPorMesas(procesoId, List.of(mesaId)).getOrDefault(mesaId, 0L) == 0) {
            throw new NegocioException(Constantes.getMensaje("mjrv.mesas.sin.iglesias"));
        }
        if (juntaCompletadaRegistrada(mesaId, procesoId)) {
            throw new NegocioException("La junta ya fue completada. Para modificarla debe reabrirse mediante una accion autorizada.");
        }
        if (miembroJRVFacade.buscarPorIglesiaPersonaProceso(iglesiaPersonaId, procesoId) != null) {
            throw new NegocioException("La persona seleccionada ya pertenece a una Junta Receptora del Voto.");
        }
        if (miembroJRVFacade.buscarPorMesaCargoProceso(mesaId, cargoId, procesoId) != null) {
            throw new NegocioException("La dignidad seleccionada ya se encuentra asignada.");
        }
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    /**
     * Guarda la designación ya validada. {@code aceptaUsuario} distinto de null marca la designación
     * desde otra mesa (con quién y cuándo la aceptó); null la deja como designación normal.
     */
    private MiembroJRVDTO guardarDesignacion(Integer iglesiaPersonaId, Integer mesaId, Integer procesoId,
            Integer cargoId, String aceptaUsuario) {
        IglesiaPersona iglesiaPersona = iglesiaPersonaFacade.find(iglesiaPersonaId);
        Mesa mesa = mesaFacade.find(mesaId);
        ProcesoElectoral proceso = procesoElectoralFacade.find(procesoId);
        CatalogoGeneral cargo = catalogoGeneralFacade.find(cargoId);
        if (iglesiaPersona == null || mesa == null || proceso == null || cargo == null) {
            throw new NegocioException("No se pudo resolver la informacion requerida para designar el miembro.");
        }
        MiembroJRV registroLogico = miembroJRVFacade.buscarPorMesaCargoProcesoIncluyeInactivos(
                mesaId, cargoId, procesoId);
        boolean otraMesa = aceptaUsuario != null;
        if (registroLogico != null && !Boolean.TRUE.equals(registroLogico.getEstado())) {
            registroLogico.setIglesiaPersona(iglesiaPersona);
            registroLogico.setMesa(mesa);
            registroLogico.setProceso(proceso);
            registroLogico.setCargo(cargo);
            registroLogico.setEstado(true);
            marcarOtraMesa(registroLogico, otraMesa, aceptaUsuario);
            return MiembroJRVDTO.fromEntity(miembroJRVFacade.edit(registroLogico));
        }
        MiembroJRV nuevo = new MiembroJRV();
        nuevo.setIglesiaPersona(iglesiaPersona);
        nuevo.setMesa(mesa);
        nuevo.setProceso(proceso);
        nuevo.setCargo(cargo);
        marcarOtraMesa(nuevo, otraMesa, aceptaUsuario);
        return MiembroJRVDTO.fromEntity(miembroJRVFacade.create(nuevo));
    }

    /** Un registro reutilizado no conserva la marca de una designación anterior. */
    private static void marcarOtraMesa(MiembroJRV miembro, boolean otraMesa, String aceptaUsuario) {
        miembro.setOtraMesa(otraMesa);
        miembro.setAceptaUsuario(otraMesa ? aceptaUsuario : null);
        miembro.setAceptaFecha(otraMesa ? new java.util.Date() : null);
    }

    /** Resultado de completar la junta: datos para el correo al Presidente. */
    public record ResultadoCompletar(MiembroJRVDTO presidente, String usuario, String correo, String nombres,
            String mesa, String recinto, String proceso, boolean cuentaNueva) implements java.io.Serializable {
    }

    /** Correo actual de la cuenta del Presidente designado (para precargar el diálogo), o null. */
    public String obtenerCorreoPresidente(Integer mesaId, Integer procesoId) {
        if (mesaId == null || procesoId == null) {
            return null;
        }
        MiembroJRV presidente = obtenerMiembroPorDignidad(miembroJRVFacade.listarPorMesaProceso(mesaId, procesoId),
                "PRESIDENTE");
        if (presidente == null || presidente.getIglesiaPersona() == null) {
            return null;
        }
        return usuarioService.obtenerCorreoDePersona(presidente.getIglesiaPersona().getPersona());
    }

    /**
     * Completa la junta: crea o reutiliza la cuenta del Presidente con el rol SITEC-Presidente-mesa
     * y registra su correo (obligatorio). El correo de aviso lo envía el controlador después.
     */
    public ResultadoCompletar completarJunta(Integer mesaId, Integer procesoId, String correo) {
        if (mesaId == null || procesoId == null) {
            throw new NegocioException("Debe seleccionar una mesa y un proceso electoral activo.");
        }
        Mesa mesa = mesaFacade.find(mesaId);
        if (mesa == null) {
            throw new NegocioException("No se pudo resolver la mesa seleccionada.");
        }
        List<MiembroJRV> miembros = miembroJRVFacade.listarPorMesaProceso(mesaId, procesoId);
        validarJuntaCompleta(mesaId, procesoId);

        MiembroJRV presidente = obtenerMiembroPorDignidad(miembros, "PRESIDENTE");
        if (presidente == null || presidente.getIglesiaPersona() == null
                || presidente.getIglesiaPersona().getPersona() == null) {
            throw new NegocioException("No se pudo resolver la persona designada como Presidente de Mesa.");
        }
        Rol rolPresidenteMesa = obtenerRolPresidenteMesa();
        if (rolPresidenteMesa == null) {
            throw new NegocioException("El rol SITEC-Presidente-mesa no esta configurado en el modulo de roles.");
        }
        Persona personaPresidente = presidente.getIglesiaPersona().getPersona();
        ec.com.antenasur.dto.ResultadoProvisionUsuarioDTO cuenta = usuarioService.asegurarUsuarioConRol(
                personaPresidente, rolPresidenteMesa, correo);
        Usuario usuario = cuenta.getUsuario();
        mesa.setResponsable(usuario.getUsername());
        mesaFacade.edit(mesa);
        ProcesoElectoral proceso = procesoElectoralFacade.find(procesoId);
        return new ResultadoCompletar(MiembroJRVDTO.fromEntity(presidente), usuario.getUsername(), usuario.getCorreo(),
                (texto(personaPresidente.getNombres()) + " " + texto(personaPresidente.getApellidos())).trim(),
                texto(mesa.getNombre()), mesa.getRecinto() != null ? texto(mesa.getRecinto().getNombre()) : "",
                proceso != null ? texto(proceso.getNombre()) : "", !cuenta.isReutilizado());
    }

    /** Bitácora del correo al Presidente (CatalogoActividades: "Notificó al Presidente de Mesa"). */
    public void registrarNotificacionPresidente(ResultadoCompletar resultado, boolean enviado, String ip) {
        if (resultado == null) {
            return;
        }
        String detalle = "NOTIFICA_PRESIDENTE_MESA | MÓDULO: JRV; RESULTADO: " + (enviado ? "EXITOSO" : "FALLIDO")
                + "; DETALLE: Usuario: " + resultado.usuario() + "; Mesa: " + resultado.mesa() + "; Recinto: "
                + resultado.recinto() + (enviado ? "" : "; Correo no enviado");
        ec.com.antenasur.model.tec.Proceso actividad = new ec.com.antenasur.model.tec.Proceso(ip);
        actividad.setActividad(detalle.length() > 255 ? detalle.substring(0, 255) : detalle);
        procesoService.create(actividad);
    }

    public MiembroJRVDTO obtenerDesignacionPorPersonaProceso(Integer personaId, Integer procesoId) {
        return MiembroJRVDTO.fromEntity(miembroJRVFacade.buscarPorPersonaProceso(personaId, procesoId));
    }

    public MiembroJRVDTO obtenerDesignacionPresidentePorPersonaProceso(Integer personaId, Integer procesoId) {
        return MiembroJRVDTO.fromEntity(miembroJRVFacade.buscarPresidentePorPersonaProceso(personaId, procesoId));
    }

    public Set<Integer> obtenerIglesiaPersonaIdsDesignadas(Integer procesoId) {
        if (procesoId == null) {
            return new HashSet<>();
        }
        return miembroJRVFacade.listarIglesiaPersonaIdsDesignadas(procesoId);
    }

    public boolean juntaCompletadaRegistrada(Integer mesaId, Integer procesoId) {
        if (mesaId == null || procesoId == null) {
            return false;
        }
        Mesa mesa = mesaFacade.find(mesaId);
        if (mesa == null || mesa.getResponsable() == null || mesa.getResponsable().isBlank()) {
            return false;
        }
        // El bloqueo de una junta completada no se levanta por cambios posteriores en el padron.
        Set<String> cargos = new HashSet<>();
        for (MiembroJRV miembro : miembroJRVFacade.listarPorMesaProceso(mesaId, procesoId)) {
            if (miembro.getCargo() != null) cargos.add(normalizarCargo(miembro.getCargo().getNombre()));
        }
        return cargos.containsAll(DIGNIDADES_OBLIGATORIAS);
    }

    private void validarParametros(Integer iglesiaPersonaId, Integer mesaId, Integer procesoId, Integer cargoId) {
        if (mesaId == null) {
            throw new NegocioException("Debe seleccionar una mesa antes de designar miembros.");
        }
        if (iglesiaPersonaId == null) {
            throw new NegocioException("Debe seleccionar una persona para designar.");
        }
        if (cargoId == null) {
            throw new NegocioException("Debe seleccionar una dignidad.");
        }
        if (procesoId == null) {
            throw new NegocioException("Debe seleccionar un proceso electoral.");
        }
    }

    private boolean perteneceAPadronDeMesa(Integer iglesiaPersonaId, Integer mesaId, Integer procesoId) {
        List<Integer> mesaIds = new ArrayList<>();
        mesaIds.add(mesaId);
        List<Padron> padrones = padronFacade.getPadronPorMesaIdsYProceso(mesaIds, procesoId);
        if (padrones == null) {
            return false;
        }
        for (Padron padron : padrones) {
            if (padron.getIglesiaPersona() != null
                    && iglesiaPersonaId.equals(padron.getIglesiaPersona().getId())) {
                return true;
            }
        }
        return false;
    }

    private void validarJuntaCompleta(Integer mesa, Integer proceso) {
        EstadoJuntaDTO estado = consultarEstadoJunta(mesa, proceso);
        if (!estado.isCompleta()) throw new NegocioException(Constantes.getMensaje("mjrv.validacion.conformacion")
                + (estado.getCargosFaltantes().isEmpty() ? "" : " " + Constantes.getMensaje(
                        "reportesMesa.regla.cargos", String.join(", ", estado.getCargosFaltantes()))));
    }

    public EstadoJuntaDTO consultarEstadoJunta(Integer mesa, Integer proceso) {
        if (mesa == null || proceso == null) return evaluarConformacion(List.of());
        return consultarEstadosJuntas(proceso, List.of(mesa)).get(mesa);
    }

    public Map<Integer, EstadoJuntaDTO> consultarEstadosJuntas(Integer proceso, List<Integer> mesas) {
        Map<Integer, List<Object[]>> porMesa = new HashMap<>();
        mesas.forEach(mesa -> porMesa.put(mesa, new ArrayList<>()));
        for (Object[] fila : miembroJRVFacade.consultarConformacion(proceso, mesas)) {
            porMesa.get((Integer) fila[0]).add(fila);
        }
        Map<Integer, EstadoJuntaDTO> estados = new HashMap<>();
        porMesa.forEach((mesa, miembros) -> estados.put(mesa, evaluarConformacion(miembros)));
        return estados;
    }

    static EstadoJuntaDTO evaluarConformacion(List<Object[]> miembros) {
        Map<String, Integer> cargos = new HashMap<>();
        Set<Integer> personas = new HashSet<>();
        boolean valida = true;
        for (Object[] miembro : miembros) {
            String cargo = normalizarCargo((String) miembro[1]);
            if (miembro[2] == null
                    || !personas.add((Integer) miembro[2]) || !DIGNIDADES_OBLIGATORIAS.contains(cargo)) valida = false;
            if (miembro[2] != null) cargos.merge(cargo, 1, Integer::sum);
        }
        List<String> faltantes = DIGNIDADES_OBLIGATORIAS.stream()
                .filter(cargo -> cargos.getOrDefault(cargo, 0) != 1).toList();
        return new EstadoJuntaDTO(valida && faltantes.isEmpty(), DIGNIDADES_OBLIGATORIAS.size() - faltantes.size(), faltantes);
    }

    public Map<Integer, ec.com.antenasur.dto.ResumenMesaJrvDTO> consultarResumenMesas(
            Integer proceso, List<Integer> mesas) {
        Map<Integer, ec.com.antenasur.dto.ResumenMesaJrvDTO> resultado = new HashMap<>();
        if (proceso == null || mesas == null || mesas.isEmpty()) return resultado;
        Map<Integer, List<Object[]>> miembros = new HashMap<>();
        mesas.forEach(id -> miembros.put(id, new ArrayList<>()));
        for (Object[] fila : miembroJRVFacade.consultarConformacion(proceso, mesas)) {
            miembros.get((Integer) fila[0]).add(fila);
        }
        Map<Integer, Long> iglesias = padronFacade.contarIglesiasPorMesas(proceso, mesas);
        miembros.forEach((id, filas) -> resultado.put(id, new ec.com.antenasur.dto.ResumenMesaJrvDTO(
                filas.size(), DIGNIDADES_OBLIGATORIAS.size(), iglesias.getOrDefault(id, 0L),
                evaluarConformacion(filas).isCompleta())));
        return resultado;
    }

    public static String normalizarCargo(String nombre) {
        return nombre == null ? "" : nombre.trim().toUpperCase(java.util.Locale.ROOT).replaceFirst(" DE MESA$", "");
    }

    private MiembroJRV obtenerMiembroPorDignidad(List<MiembroJRV> miembros, String dignidad) {
        if (miembros == null) {
            return null;
        }
        for (MiembroJRV miembro : miembros) {
            if (miembro != null && miembro.getCargo() != null
                    && contieneCargo(normalizar(miembro.getCargo().getNombre()), dignidad)) {
                return miembro;
            }
        }
        return null;
    }

    private Rol obtenerRolPresidenteMesa() {
        String nombreRol = Constantes.getRolPresidenteMesa();
        Rol rol = rolFacade.buscaPorNombre("SITEC-" + nombreRol);
        return rol != null ? rol : rolFacade.buscaPorNombre(nombreRol);
    }

    private static boolean contieneCargo(String cargo, String dignidad) {
        return dignidad.equals(normalizarCargo(cargo));
    }

    private static String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase();
    }
}
