package ec.com.antenasur.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.PersonaEliminadaDTO;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.facade.IglesiaBajaFacade;
import ec.com.antenasur.facade.IglesiaPersonaFacade;
import ec.com.antenasur.facade.PersonaBajaFacade;
import ec.com.antenasur.facade.PersonaFacade;
import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.Persona;
import ec.com.antenasur.util.Constantes;
import lombok.extern.slf4j.Slf4j;

/**
 * Consulta y restauración de personas dadas de baja.
 *
 * <p>Contraparte de {@link IglesiaBajaService} para la entidad Persona: no
 * borra filas —todo es cambio de {@code estado}, de modo que Envers conserva la
 * trazabilidad— y restaura solo lo que esa misma baja desactivó, identificado
 * por el número de revisión de Envers.
 *
 * <p>La restauración es una sola transacción ({@code REQUIRES_NEW}): si falla
 * cualquier paso se lanza {@link NegocioException} y el contenedor revierte
 * todo, incluidas las revisiones de auditoría.
 *
 * <p>La autorización se declara aquí además de en la vista, con los mismos roles
 * que la gestión de iglesias eliminadas: ocultar un botón no es un control de
 * acceso.
 */
@Stateless
@Slf4j
@DeclareRoles({ "SITEC-Administrador", "SITEC-Tribunal" })
@RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal" })
public class PersonaBajaService {

	@Inject
	private PersonaBajaFacade bajaFacade;

	@Inject
	private PersonaFacade personaFacade;

	@Inject
	private IglesiaPersonaFacade iglesiaPersonaFacade;

	/**
	 * Se reutiliza únicamente por {@code membresiasEnConflicto}, que resuelve en
	 * una consulta la regla «una sola iglesia activa por documento» que impone el
	 * trigger de base; no se duplica esa lógica aquí.
	 */
	@Inject
	private IglesiaBajaFacade iglesiaBajaFacade;

	/** Resultado de una restauración: qué persona y cuántas membresías se reactivaron. */
	public record Resultado(String persona, int membresias, List<String> omitidas) {

		public boolean tieneOmitidas() {
			return !omitidas.isEmpty();
		}
	}

	// =====================================================================
	// Consulta bajo demanda
	// =====================================================================

	/**
	 * Personas dadas de baja, con la fecha y el usuario de la eliminación y el
	 * motivo por el que, si es el caso, no pueden restaurarse. Se invoca solo al
	 * abrir «Ver eliminadas»: la pantalla principal nunca consulta esta
	 * información.
	 */
	public List<PersonaEliminadaDTO> listarEliminadas() {
		List<Persona> personas = bajaFacade.listarInactivas();
		if (personas.isEmpty()) {
			return Collections.emptyList();
		}
		List<Integer> ids = new ArrayList<>(personas.size());
		for (Persona persona : personas) {
			ids.add(persona.getId());
		}
		Map<Integer, Object[]> bajas = bajaFacade.datosDeBaja(ids);
		Map<Integer, String> iglesias = bajaFacade.ultimaIglesiaConocida(ids);
		Map<Integer, String> documentosOcupados = bajaFacade.documentosOcupadosPorOtraPersonaActiva(ids);

		List<PersonaEliminadaDTO> resultado = new ArrayList<>(personas.size());
		for (Persona persona : personas) {
			PersonaEliminadaDTO dto = new PersonaEliminadaDTO();
			dto.setId(persona.getId());
			dto.setNombres(persona.getNombres());
			dto.setApellidos(persona.getApellidos());
			dto.setDocumento(persona.getDocumento());
			dto.setSexo(persona.getSexo());
			dto.setIglesiaNombre(iglesias.get(persona.getId()));
			Object[] baja = bajas.get(persona.getId());
			if (baja != null) {
				dto.setRevisionBaja((Integer) baja[0]);
				dto.setFechaBaja(baja[1] instanceof java.util.Date fecha ? fecha : null);
				dto.setUsuarioBaja((String) baja[2]);
			}
			String ocupadoPor = documentosOcupados.get(persona.getId());
			if (ocupadoPor != null) {
				dto.setMotivoBloqueo(Constantes.getMensaje("form.personas.eliminadas.bloqueo.documento", ocupadoPor));
			}
			resultado.add(dto);
		}
		return resultado;
	}

	// =====================================================================
	// Restauración
	// =====================================================================

	/**
	 * Restaura una persona dada de baja y únicamente las membresías que esa baja
	 * desactivó.
	 *
	 * <p>Se rechaza si su cédula ya la usa otra persona activa. Las membresías
	 * cuya iglesia sigue dada de baja, o que chocarían con la regla «una sola
	 * iglesia activa por documento», se dejan como están y se informan: la
	 * persona queda activa pero sin esa membresía, que el operador debe resolver
	 * desde el módulo de miembros.
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public Resultado restaurar(Integer personaId) {
		Persona persona = bajaFacade.buscarSinFiltro(personaId);
		if (persona == null) {
			throw new NegocioException(Constantes.getMensaje("form.personas.eliminadas.error.inexistente"));
		}
		if (Boolean.TRUE.equals(persona.getEstado())) {
			throw new NegocioException(Constantes.getMensaje("form.personas.eliminadas.error.activa"));
		}
		Map<Integer, String> ocupados = bajaFacade
				.documentosOcupadosPorOtraPersonaActiva(List.of(persona.getId()));
		String ocupadoPor = ocupados.get(persona.getId());
		if (ocupadoPor != null) {
			throw new NegocioException(
					Constantes.getMensaje("form.personas.eliminadas.bloqueo.documento", ocupadoPor));
		}
		Integer revision = bajaFacade.revisionDeBaja(personaId);
		if (revision == null) {
			throw new NegocioException(Constantes.getMensaje("form.personas.eliminadas.error.sin.revision"));
		}

		List<Integer> idsDesactivados = bajaFacade.membresiasDesactivadasDesdeRevision(personaId, revision);
		List<IglesiaPersona> membresias = bajaFacade.membresiasPorIds(idsDesactivados);
		// Los conflictos se detectan en bloque ANTES de escribir, de modo que la
		// restauración nunca provoque la excepción del trigger de base ni deje la
		// transacción a medias.
		Map<Integer, String> conflictos = conflictos(idsDesactivados);

		int membresiasRestauradas = 0;
		List<String> omitidas = new ArrayList<>();
		for (IglesiaPersona membresia : membresias) {
			String conflicto = conflictos.get(membresia.getId());
			if (conflicto != null) {
				omitidas.add(conflicto);
				continue;
			}
			if (membresia.getIglesia() == null || !Boolean.TRUE.equals(membresia.getIglesia().getEstado())) {
				omitidas.add(Constantes.getMensaje("form.personas.eliminadas.omitida.iglesia.inactiva",
						membresia.getIglesia() == null ? "" : membresia.getIglesia().getNombre()));
				continue;
			}
			if (!Boolean.TRUE.equals(membresia.getEstado())) {
				membresia.setEstado(true);
				iglesiaPersonaFacade.edit(membresia);
				membresiasRestauradas++;
			}
		}

		persona.setEstado(true);
		personaFacade.edit(persona);
		log.info("Persona id={} restaurada desde la revisión {}: {} membresía(s) reactivada(s), {} omitida(s)",
				personaId, revision, membresiasRestauradas, omitidas.size());
		return new Resultado(persona.getNombres(), membresiasRestauradas, List.copyOf(omitidas));
	}

	/**
	 * Membresías que no pueden reactivarse por la regla «una sola iglesia activa
	 * por documento», con el motivo ya redactado. Una sola consulta para todo el
	 * conjunto: sin N+1 y sin depender de que falle el trigger.
	 */
	private Map<Integer, String> conflictos(List<Integer> igpeIds) {
		Map<Integer, String> conflictos = new LinkedHashMap<>();
		for (Object[] fila : iglesiaBajaFacade.membresiasEnConflicto(igpeIds)) {
			Integer igpeId = ((Number) fila[0]).intValue();
			String documento = (String) fila[1];
			String iglesiaEnConflicto = (String) fila[3];
			conflictos.put(igpeId, iglesiaEnConflicto != null
					? Constantes.getMensaje("form.personas.eliminadas.omitida.otra.iglesia", documento,
							iglesiaEnConflicto)
					: Constantes.getMensaje("form.personas.eliminadas.omitida.documento.duplicado", documento));
		}
		return conflictos;
	}
}
