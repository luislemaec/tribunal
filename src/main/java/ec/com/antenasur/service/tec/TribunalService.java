package ec.com.antenasur.service.tec;

import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

import java.util.ArrayList;

import ec.com.antenasur.dto.TribunalDTO;
import ec.com.antenasur.dto.ResultadoProvisionUsuarioDTO;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.facade.IglesiaPersonaFacade;
import ec.com.antenasur.facade.tec.CatalogoGeneralFacade;
import ec.com.antenasur.facade.tec.ProcesoElectoralFacade;
import ec.com.antenasur.facade.tec.TribunalFacade;
import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.Persona;
import ec.com.antenasur.model.Rol;
import ec.com.antenasur.model.Usuario;
import ec.com.antenasur.model.tec.CatalogoGeneral;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.model.tec.Tribunal;
import ec.com.antenasur.service.AbstractService;
import ec.com.antenasur.service.UsuarioService;
import ec.com.antenasur.facade.RolFacade;
import ec.com.antenasur.util.Constantes;

@Stateless
@DeclareRoles({"SITEC-Administrador", "SITEC-Tecnico", "SITEC-Tribunal", "SITEC-IglesiaAdmin"})
public class TribunalService extends AbstractService<Tribunal, Integer, TribunalFacade> {

    private static final Integer CARGO_PADRE_AUTORIDADES_TRIBUNAL = 2;

    @Inject
    private TribunalFacade tribunalFacade;

    @Inject
    private ProcesoElectoralFacade procesoElectoralFacade;

    @Inject
    private CatalogoGeneralFacade catalogoFacade;

    @Inject
    private IglesiaPersonaFacade iglesiaPersonaFacade;

    @Inject
    private UsuarioService usuarioService;

    @Inject
    private RolFacade rolFacade;

    @Resource
    private SessionContext sessionContext;

    @Override
    protected TribunalFacade getFacade() {
        return tribunalFacade;
    }

    @RolesAllowed({"SITEC-Administrador", "SITEC-Tecnico", "SITEC-Tribunal", "SITEC-IglesiaAdmin"})
    public List<Tribunal> getRegistrosActivos() {
        return tribunalFacade.getRegistrosActivos();
    }

    @RolesAllowed({"SITEC-Administrador", "SITEC-Tecnico", "SITEC-Tribunal"})
    public TribunalDTO obtenerDTOPorId(Integer id) {
        if (id == null) return null;
        return TribunalDTO.fromEntity(tribunalFacade.find(id));
    }

    @RolesAllowed({"SITEC-Administrador", "SITEC-Tecnico", "SITEC-Tribunal", "SITEC-IglesiaAdmin"})
    public List<TribunalDTO> listarDTOsActivos() {
        return mapearLista(tribunalFacade.getRegistrosActivos());
    }

    @RolesAllowed({"SITEC-Administrador", "SITEC-Tecnico", "SITEC-Tribunal", "SITEC-IglesiaAdmin"})
    public List<TribunalDTO> listarDTOsActivosPorProceso(Integer procesoId) {
        ProcesoElectoral proceso = procesoId != null ? procesoElectoralFacade.find(procesoId) : null;
        if (proceso == null) {
            return new ArrayList<>();
        }
        return mapearLista(tribunalFacade.getRegistrosActivosPorProceso(proceso));
    }

    @RolesAllowed({"SITEC-Administrador", "SITEC-Tecnico", "SITEC-Tribunal"})
    public List<TribunalDTO> listarDTOs() {
        return mapearLista(tribunalFacade.findAll());
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal"})
    public TribunalDTO guardarDesdeDTO(TribunalDTO dto) {
        try {
            return guardarDesdeDTOTransaccional(dto);
        } catch (RuntimeException e) {
            sessionContext.setRollbackOnly();
            throw e;
        }
    }

    private TribunalDTO guardarDesdeDTOTransaccional(TribunalDTO dto) {
        if (dto == null) {
            throw new NegocioException("autoridades.mensaje.no.determinada");
        }
        Integer procesoId = dto.getProcesoId() != null ? dto.getProcesoId() : dto.getPeriodoId();
        ProcesoElectoral proceso = (procesoId != null) ? procesoElectoralFacade.find(procesoId) : null;
        CatalogoGeneral cargo = (dto.getCargoId() != null) ? catalogoFacade.find(dto.getCargoId()) : null;
        IglesiaPersona ip = (dto.getIglesiaPersona() != null && dto.getIglesiaPersona().getId() != null)
                ? iglesiaPersonaFacade.find(dto.getIglesiaPersona().getId()) : null;

        validarAsignacion(proceso, cargo, ip);
        Rol rolTribunal = rolFacade.buscaPorNombre("SITEC-" + Constantes.getRolTribunal());
        if (rolTribunal == null || !Boolean.TRUE.equals(rolTribunal.getEstado())) {
            throw new NegocioException("autoridades.mensaje.rol.no.configurado");
        }
        Tribunal registroLogico = tribunalFacade.buscarPorProcesoCargoIncluyendoInactivos(
                proceso.getId(), cargo.getId());
        Tribunal actual = dto.getId() != null ? tribunalFacade.find(dto.getId()) : null;
        if (dto.getId() != null && actual == null) {
            throw new NegocioException("autoridades.mensaje.no.disponible");
        }
        if (registroLogico != null && (actual == null || !actual.getId().equals(registroLogico.getId()))
                && Boolean.TRUE.equals(registroLogico.getEstado())) {
            throw new NegocioException("autoridades.mensaje.cargo.duplicado");
        }

        ResultadoProvisionUsuarioDTO provision = usuarioService.provisionarUsuarioExistenteConRol(
                ip.getPersona(), dto.getCorreoAutoridad(), rolTribunal);
        Tribunal persistido;

        if (dto.getId() == null) {
            if (registroLogico != null) {
                registroLogico.setIglesiaPersona(ip);
                registroLogico.setProceso(proceso);
                registroLogico.setCargo(cargo);
                registroLogico.setEstado(true);
                persistido = tribunalFacade.edit(registroLogico);
            } else {
                Tribunal nuevo = new Tribunal();
                nuevo.setProceso(proceso);
                nuevo.setCargo(cargo);
                nuevo.setIglesiaPersona(ip);
                persistido = tribunalFacade.create(nuevo);
            }
        } else {
            Persona personaAnterior = actual.getIglesiaPersona() != null ? actual.getIglesiaPersona().getPersona() : null;
            Integer personaAnteriorId = personaAnterior != null ? personaAnterior.getId() : null;
            actual.setProceso(proceso);
            actual.setCargo(cargo);
            actual.setIglesiaPersona(ip);
            persistido = tribunalFacade.edit(actual);
            if (personaAnteriorId != null && !personaAnteriorId.equals(ip.getPersona().getId())
                    && !tribunalFacade.existeAutoridadActivaPorPersona(personaAnteriorId)) {
                usuarioService.retirarRolDePersonaSiNoTieneOtrosRoles(personaAnterior, rolTribunal);
            }
        }
        TribunalDTO resultado = TribunalDTO.fromEntity(persistido);
        resultado.setCorreoAutoridad(provision.getUsuario().getCorreo());
        resultado.setUsuarioReutilizado(provision.isReutilizado());
        resultado.setUsuarioReactivado(provision.isReactivado());
        return resultado;
    }

    /**
     * Asigna a un DTO una IglesiaPersona resuelta por cédula. NO persiste.
     */
    @RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal"})
    public TribunalDTO asignarPersonaPorCedula(TribunalDTO dto, String cedula) {
        if (dto == null) {
            throw new NegocioException("autoridades.mensaje.cargo.requerido");
        }
        if (cedula == null || !cedula.trim().matches("\\d{10}")) {
            throw new NegocioException("autoridades.mensaje.cedula.invalida");
        }
        IglesiaPersona ip = iglesiaPersonaFacade.buscarPorCedulaPersona(cedula.trim());
        if (ip == null || ip.getPersona() == null || ip.getIglesia() == null) {
            throw new NegocioException("autoridades.mensaje.persona.no.encontrada");
        }
        dto.setIglesiaPersona(ec.com.antenasur.dto.IglesiaPersonaDTO.fromEntity(ip));
        dto.setCorreoAutoridad(obtenerCorreoUsuario(ip.getPersona()));
        return dto;
    }

    @RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal"})
    public String obtenerCorreoUsuario(IglesiaPersona iglesiaPersona) {
        return iglesiaPersona != null ? obtenerCorreoUsuario(iglesiaPersona.getPersona()) : null;
    }

    @RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal"})
    public String obtenerCorreoUsuarioPorPersonaId(Integer personaId) {
        Usuario usuario = usuarioService.findUsuarioPorPersonaIncluyendoInactivos(personaId);
        return usuario != null ? usuario.getCorreo() : null;
    }

    private String obtenerCorreoUsuario(Persona persona) {
        return persona != null ? obtenerCorreoUsuarioPorPersonaId(persona.getId()) : null;
    }

    /**
     * Devuelve la lista de autoridades vigentes; si faltan cargos, agrega
     * placeholders (TribunalDTO sin id) por cada cargo que no esté asignado.
     */
    @RolesAllowed({"SITEC-Administrador", "SITEC-Tecnico", "SITEC-Tribunal"})
    public List<TribunalDTO> listarAutoridadesConPlaceholders(Integer procesoId, Integer cargoPadreId) {
        List<TribunalDTO> resultado = new ArrayList<>();
        ProcesoElectoral proceso = (procesoId != null) ? procesoElectoralFacade.find(procesoId) : null;
        List<Tribunal> activos = tribunalFacade.getRegistrosActivosPorProceso(proceso);

        if (activos == null || activos.isEmpty()) {
            List<CatalogoGeneral> cargos = catalogoFacade.listaCatalogoHijo(cargoPadreId);
            if (cargos != null) {
                for (CatalogoGeneral cargo : cargos) {
                    Tribunal placeholder = new Tribunal();
                    placeholder.setCargo(cargo);
                    placeholder.setProceso(proceso);
                    resultado.add(TribunalDTO.fromEntity(placeholder));
                }
            }
            return resultado;
        }

        List<Integer> idsCargosAsignados = new ArrayList<>();
        for (Tribunal t : activos) {
            resultado.add(TribunalDTO.fromEntity(t));
            if (t.getCargo() != null) idsCargosAsignados.add(t.getCargo().getId());
        }
        List<CatalogoGeneral> cargosFaltantes = catalogoFacade.listaCatalogoHijo(cargoPadreId, idsCargosAsignados);
        if (cargosFaltantes != null) {
            for (CatalogoGeneral cargo : cargosFaltantes) {
                Tribunal placeholder = new Tribunal();
                placeholder.setCargo(cargo);
                placeholder.setProceso(proceso);
                resultado.add(TribunalDTO.fromEntity(placeholder));
            }
        }
        return resultado;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal"})
    public TribunalDTO eliminarPorId(Integer id) {
        try {
            if (id == null) return null;
            Tribunal t = tribunalFacade.find(id);
            if (t == null) return null;
            Persona persona = t.getIglesiaPersona() != null ? t.getIglesiaPersona().getPersona() : null;
            TribunalDTO eliminado = TribunalDTO.fromEntity(tribunalFacade.delete(t));
            Rol rolTribunal = rolFacade.buscaPorNombre("SITEC-" + Constantes.getRolTribunal());
            if (persona != null && rolTribunal != null
                    && !tribunalFacade.existeAutoridadActivaPorPersona(persona.getId())) {
                usuarioService.retirarRolDePersonaSiNoTieneOtrosRoles(persona, rolTribunal);
            }
            return eliminado;
        } catch (RuntimeException e) {
            sessionContext.setRollbackOnly();
            throw e;
        }
    }

    private List<TribunalDTO> mapearLista(List<Tribunal> entidades) {
        List<TribunalDTO> resultado = new ArrayList<>();
        if (entidades == null) return resultado;
        for (Tribunal t : entidades) resultado.add(TribunalDTO.fromEntity(t));
        return resultado;
    }

    private void validarAsignacion(ProcesoElectoral proceso, CatalogoGeneral cargo, IglesiaPersona iglesiaPersona) {
        if (proceso == null) {
            throw new NegocioException("autoridades.mensaje.proceso.invalido");
        }
        if (cargo == null || cargo.getPadre() == null
                || !CARGO_PADRE_AUTORIDADES_TRIBUNAL.equals(cargo.getPadre().getId())) {
            throw new NegocioException("autoridades.mensaje.cargo.invalido");
        }
        if (iglesiaPersona == null) {
            throw new NegocioException("autoridades.mensaje.miembro.requerido");
        }
    }
}
