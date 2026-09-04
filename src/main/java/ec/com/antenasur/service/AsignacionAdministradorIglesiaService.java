package ec.com.antenasur.service;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.ResultadoProvisionUsuarioDTO;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.facade.IglesiaFacade;
import ec.com.antenasur.facade.IglesiaPersonaFacade;
import ec.com.antenasur.facade.RolFacade;
import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.Rol;
import ec.com.antenasur.service.tec.CronogramaService;

/** Mantiene atomica la reasignacion de administrador de una iglesia. */
@Stateless
@DeclareRoles("SITEC-Administrador")
public class AsignacionAdministradorIglesiaService {

    private static final String ROL_IGLESIA_ADMIN = "SITEC-IglesiaAdmin";

    @Inject
    private IglesiaFacade iglesiaFacade;

    @Inject
    private IglesiaPersonaFacade iglesiaPersonaFacade;

    @Inject
    private RolFacade rolFacade;

    @Inject
    private UsuarioService usuarioService;

    @Inject
    private CronogramaService cronogramaService;

    @Resource
    private SessionContext sessionContext;

    @RolesAllowed("SITEC-Administrador")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResultadoProvisionUsuarioDTO asignar(Integer iglesiaId, Integer iglesiaPersonaId, String correo) {
        try {
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
                    || relacion.getPersona() == null || !Boolean.TRUE.equals(relacion.getPersona().getEstado())) {
                throw new NegocioException("iglesias.admin.error.persona.no.pertenece");
            }
            Rol rol = rolFacade.buscaPorNombre(ROL_IGLESIA_ADMIN);
            if (rol == null || !Boolean.TRUE.equals(rol.getEstado())) {
                throw new NegocioException("iglesias.admin.error.rol.no.disponible");
            }

            var adminActual = usuarioService.obtenerAdminDeIglesia(iglesiaId);
            if (adminActual != null && !relacion.getPersona().getId().equals(adminActual.getPersonaId())) {
                usuarioService.removerAdminDeIglesia(iglesiaId);
            }
            return usuarioService.provisionarUsuarioExistenteConRol(
                    relacion.getPersona(), correo, rol, iglesiaId);
        } catch (RuntimeException e) {
            sessionContext.setRollbackOnly();
            throw e;
        }
    }
}
