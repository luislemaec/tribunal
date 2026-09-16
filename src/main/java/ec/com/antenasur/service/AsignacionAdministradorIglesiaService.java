package ec.com.antenasur.service;

import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.ResultadoProvisionUsuarioDTO;
import ec.com.antenasur.dto.DiagnosticoAsignacionAdministradorDTO;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.facade.IglesiaFacade;
import ec.com.antenasur.facade.IglesiaPersonaFacade;
import ec.com.antenasur.facade.PersonaFacade;
import ec.com.antenasur.facade.RolFacade;
import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.Persona;
import ec.com.antenasur.model.Rol;
import ec.com.antenasur.service.tec.CronogramaService;
import lombok.extern.slf4j.Slf4j;

/** Mantiene atomica la reasignacion de administrador de una iglesia. */
@Stateless
@DeclareRoles({"SITEC-Administrador", "SITEC-Tribunal"})
@Slf4j
public class AsignacionAdministradorIglesiaService {

    private static final String ROL_IGLESIA_ADMIN = "SITEC-IglesiaAdmin";

    @Inject
    private IglesiaFacade iglesiaFacade;

    @Inject
    private IglesiaPersonaFacade iglesiaPersonaFacade;

    @Inject
    private PersonaFacade personaFacade;

    @Inject
    private RolFacade rolFacade;

    @Inject
    private UsuarioService usuarioService;

    @Inject
    private CronogramaService cronogramaService;

    @Resource
    private SessionContext sessionContext;

    @RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal"})
    public DiagnosticoAsignacionAdministradorDTO previsualizarAsignacion(
            Integer iglesiaId, Integer iglesiaPersonaId) {
        IglesiaPersona relacion = validarAsignacionBase(iglesiaId, iglesiaPersonaId);
        return construirDiagnostico(relacion, false);
    }

    @RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResultadoProvisionUsuarioDTO asignar(Integer iglesiaId, Integer iglesiaPersonaId, String correo) {
        try {
            log.info("Asignacion IglesiaAdmin iniciada. iglesiaId={}, iglesiaPersonaId={}",
                    iglesiaId, iglesiaPersonaId);
            IglesiaPersona relacionSolicitada = validarAsignacionBase(iglesiaId, iglesiaPersonaId);
            Iglesia iglesia = iglesiaFacade.findForAdminAssignment(iglesiaId);
            DiagnosticoAsignacionAdministradorDTO diagnostico = construirDiagnostico(relacionSolicitada, true);
            if (diagnostico.isTieneCedulaDuplicada()) {
                throw new NegocioException("iglesias.admin.error.cedula.duplicada");
            }
            List<IglesiaPersona> relaciones = iglesiaPersonaFacade.listarActivasPorDocumento(
                    diagnostico.getCedula(), true);
            IglesiaPersona relacion = relaciones.stream()
                    .filter(item -> iglesiaPersonaId.equals(item.getId())
                    && item.getPersona() != null
                    && relacionSolicitada.getPersona().getId().equals(item.getPersona().getId())
                    && item.getIglesia() != null && iglesiaId.equals(item.getIglesia().getId()))
                    .findFirst()
                    .orElseThrow(() -> new NegocioException("iglesias.admin.error.persona.no.pertenece"));

            Rol rol = rolFacade.buscaPorNombre(ROL_IGLESIA_ADMIN);
            if (rol == null || !Boolean.TRUE.equals(rol.getEstado())) {
                throw new NegocioException("iglesias.admin.error.rol.no.disponible");
            }

            desactivarVinculosAlternos(relaciones, relacion.getId());

            var adminActual = usuarioService.obtenerAdminDeIglesia(iglesiaId);
            if (adminActual != null && !relacion.getPersona().getId().equals(adminActual.getPersonaId())) {
                log.info("Reasignando IglesiaAdmin. iglesiaId={}, usuarioAnteriorId={}, personaNuevaId={}",
                        iglesiaId, adminActual.getId(), relacion.getPersona().getId());
                usuarioService.removerAdminDeIglesia(iglesiaId);
            }
            ResultadoProvisionUsuarioDTO resultado = usuarioService.provisionarAdministradorIglesiaExclusivo(
                    relacion.getPersona(), correo, rol, iglesiaId);
            log.info("Asignacion IglesiaAdmin completada. iglesiaId={}, personaId={}, usuarioId={}, rol={}, reutilizado={}, reactivado={}",
                    iglesiaId, relacion.getPersona().getId(), resultado.getUsuario().getId(),
                    rol.getNombre(), resultado.isReutilizado(), resultado.isReactivado());
            return resultado;
        } catch (RuntimeException e) {
            sessionContext.setRollbackOnly();
            log.warn("Asignacion IglesiaAdmin revertida. iglesiaId={}, iglesiaPersonaId={}, motivo={}",
                    iglesiaId, iglesiaPersonaId, e.getMessage());
            throw e;
        }
    }

    private IglesiaPersona validarAsignacionBase(Integer iglesiaId, Integer iglesiaPersonaId) {
        if (!cronogramaService.permiteAsignacionUsuarios()) {
            throw new NegocioException("iglesias.admin.error.fase.no.permite");
        }
        Iglesia iglesia = iglesiaFacade.findForAdminAssignment(iglesiaId);
        if (iglesia == null || !Boolean.TRUE.equals(iglesia.getEstado())) {
            throw new NegocioException("iglesias.admin.error.iglesia.no.disponible");
        }
        IglesiaPersona relacion = iglesiaPersonaFacade.find(iglesiaPersonaId);
        if (relacion == null || !Boolean.TRUE.equals(relacion.getEstado())
                || relacion.getIglesia() == null || !iglesiaId.equals(relacion.getIglesia().getId())
                || relacion.getPersona() == null || !Boolean.TRUE.equals(relacion.getPersona().getEstado())
                || relacion.getPersona().getDocumento() == null
                || relacion.getPersona().getDocumento().trim().isEmpty()) {
            throw new NegocioException("iglesias.admin.error.persona.no.pertenece");
        }
        return relacion;
    }

    private DiagnosticoAsignacionAdministradorDTO construirDiagnostico(
            IglesiaPersona relacion, boolean bloquear) {
        String cedula = relacion.getPersona().getDocumento().trim();
        List<Persona> personas = personaFacade.listarActivasPorDocumento(cedula, bloquear);
        List<IglesiaPersona> relaciones = iglesiaPersonaFacade.listarActivasPorDocumento(cedula, bloquear);

        DiagnosticoAsignacionAdministradorDTO diagnostico = new DiagnosticoAsignacionAdministradorDTO();
        diagnostico.setCedula(cedula);
        Set<String> iglesias = new LinkedHashSet<>();
        for (IglesiaPersona item : relaciones) {
            if (item.getIglesia() != null && item.getIglesia().getNombre() != null) {
                iglesias.add(item.getIglesia().getNombre());
            }
        }
        var usuario = usuarioService.obtenerUsuarioPorPersonaIncluyendoInactivos(
                relacion.getPersona().getId());
        if (usuario != null && Boolean.TRUE.equals(usuario.getEstado())
                && usuario.getIglesiaNombre() != null && !usuario.getIglesiaNombre().isBlank()) {
            iglesias.add(usuario.getIglesiaNombre());
        }
        diagnostico.setIglesiasAsociadas(List.copyOf(iglesias));
        diagnostico.setPersonasDuplicadas(personas.stream()
                .map(this::identificarPersona)
                .toList());
        return diagnostico;
    }

    private String identificarPersona(Persona persona) {
        String nombres = persona.getNombres() == null || persona.getNombres().isBlank()
                ? "Sin nombres registrados" : persona.getNombres().trim();
        return "ID " + persona.getId() + " — " + nombres;
    }

    /** Mantiene auditoría e historial mediante bajas lógicas de los vínculos alternos. */
    private void desactivarVinculosAlternos(List<IglesiaPersona> relaciones, Integer vinculoDestinoId) {
        Date ahora = new Date();
        boolean huboCambios = false;
        for (IglesiaPersona relacion : relaciones) {
            if (!vinculoDestinoId.equals(relacion.getId())) {
                relacion.setEstado(Boolean.FALSE);
                relacion.setHasta(new Timestamp(ahora.getTime()));
                relacion.setHabilitadoPadron(Boolean.FALSE);
                iglesiaPersonaFacade.edit(relacion);
                huboCambios = true;
            }
        }
        if (huboCambios) {
            iglesiaPersonaFacade.flushCambios();
        }
    }
}
