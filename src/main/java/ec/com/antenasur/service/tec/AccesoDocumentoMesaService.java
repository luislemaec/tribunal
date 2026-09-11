package ec.com.antenasur.service.tec;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.facade.UsuarioFacade;
import ec.com.antenasur.util.Constantes;

/**
 * La identidad y el alcance se resuelven desde Elytron, nunca desde parametros
 * JSF.
 */
@Stateless
@DeclareRoles({ "SITEC-Administrador", "SITEC-Tribunal", "SITEC-Presidente-mesa" })
@RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal", "SITEC-Presidente-mesa" })
public class AccesoDocumentoMesaService {
	@Inject
	private AlcanceSesionQrService alcanceQr;
	@Resource
	private SessionContext contexto;
	@Inject
	private UsuarioFacade usuarioFacade;
	@Inject
	private MiembroJRVService miembroService;
	@Inject
	private ProcesoElectoralService procesoService;

	public boolean esRevisor() {
		if (alcanceQr != null && alcanceQr.contexto() != null)
			return false;
		return contexto.isCallerInRole("SITEC-Administrador") || contexto.isCallerInRole("SITEC-Tribunal");
	}

	public String usuarioActual() {
		var qr = alcanceQr == null ? null : alcanceQr.contexto();
		return qr != null ? qr.username() : contexto.getCallerPrincipal().getName();
	}

	/** null significa alcance de todas las mesas para un revisor autorizado. */
	public Integer mesaPermitida(Integer procesoId) {
		var qr = alcanceQr == null ? null : alcanceQr.contexto();
		if (qr != null) {
			alcanceQr.validar(qr.mesaId(), procesoId);
			return qr.mesaId();
		}
		var activo = procesoService.getActivo();
		if (activo == null || !Boolean.TRUE.equals(activo.getEstado()) || !activo.getId().equals(procesoId)) {
			throw new NegocioException(Constantes.getMensaje("reportesMesa.error.proceso.activo"));
		}
		// El rol de presidente mantiene su restriccion incluso si tiene otros roles.
		if (contexto.isCallerInRole("SITEC-Presidente-mesa")) {
			var usuario = usuarioFacade.findByUsuarioName(usuarioActual());
			var designacion = usuario == null || usuario.getPersonsa() == null ? null
					: miembroService.obtenerDesignacionPresidentePorPersonaProceso(usuario.getPersonsa().getId(),
							procesoId);
			if (designacion != null && designacion.getMesa() != null)
				return designacion.getMesa().getId();
		} else if (esRevisor()) {
			return null;
		}
		throw new NegocioException(Constantes.getMensaje("reportesMesa.error.mesa.no.autorizada"));
	}

	public void validar(Integer mesaId, Integer procesoId) {
		Integer permitida = mesaPermitida(procesoId);
		if (mesaId == null || (permitida != null && !permitida.equals(mesaId))) {
			throw new NegocioException(Constantes.getMensaje("reportesMesa.error.mesa.no.autorizada"));
		}
	}

	public void exigirRevisor(Integer mesaId, Integer procesoId) {
		validar(mesaId, procesoId);
		if (!esRevisor())
			throw new NegocioException(Constantes.getMensaje("reportesMesa.certificados.no.autorizado"));
	}
}
