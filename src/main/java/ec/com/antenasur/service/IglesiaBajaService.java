package ec.com.antenasur.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.IglesiaEliminadaDTO;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.facade.IglesiaBajaFacade;
import ec.com.antenasur.facade.IglesiaFacade;
import ec.com.antenasur.facade.IglesiaPersonaFacade;
import ec.com.antenasur.facade.PersonaFacade;
import ec.com.antenasur.facade.UsuarioFacade;
import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.Persona;
import ec.com.antenasur.model.Usuario;
import lombok.extern.slf4j.Slf4j;

/**
 * Baja lógica y restauración de iglesias, con sus miembros.
 *
 * <p>Ninguna operación borra filas: todo es cambio de {@code estado}, de modo
 * que Envers y la bitácora funcional conservan la trazabilidad completa.
 *
 * <p>Cada operación es una sola transacción ({@code REQUIRES_NEW}): si falla
 * cualquier paso —validación, miembro, usuario o la propia iglesia— se lanza
 * {@link NegocioException} y el contenedor revierte todo, incluidas las
 * revisiones de auditoría, sin dejar estados intermedios.
 *
 * <p>La autorización se declara aquí además de en la vista: ocultar un botón no
 * es un control de acceso.
 */
@Stateless
@Slf4j
@DeclareRoles({ "SITEC-Administrador", "SITEC-Tribunal" })
@RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal" })
public class IglesiaBajaService {

	@Inject
	private IglesiaBajaFacade bajaFacade;

	@Inject
	private IglesiaFacade iglesiaFacade;

	@Inject
	private IglesiaPersonaFacade iglesiaPersonaFacade;

	@Inject
	private PersonaFacade personaFacade;

	@Inject
	private UsuarioFacade usuarioFacade;

	/**
	 * Resultado de una baja o restauración. {@code omitidos} describe las
	 * membresías que no pudieron reactivarse por la regla de una sola iglesia
	 * activa por persona; está vacío en las bajas.
	 */
	public record Resultado(String iglesia, int membresias, int personas, int usuarios, List<String> omitidos) {

		public Resultado(String iglesia, int membresias, int personas, int usuarios) {
			this(iglesia, membresias, personas, usuarios, List.of());
		}

		public boolean tieneOmitidos() {
			return !omitidos.isEmpty();
		}
	}

	// =====================================================================
	// Eliminación
	// =====================================================================

	/**
	 * Da de baja la iglesia y, de forma coherente, todo lo que depende de ella:
	 * sus membresías activas, las personas que no pertenezcan a ninguna otra
	 * iglesia activa y los usuarios asociados (por ejemplo su IglesiaAdmin).
	 *
	 * <p>Se rechaza si algún miembro participa en el proceso electoral (padrón,
	 * JRV, candidaturas o tribunal): desactivar esas membresías dejaría sin
	 * respaldo registros electorales vigentes.
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public Resultado eliminar(Integer iglesiaId) {
		Iglesia iglesia = bajaFacade.buscarSinFiltro(iglesiaId);
		if (iglesia == null) {
			throw new NegocioException("La iglesia ya no existe; actualice la lista.");
		}
		if (!Boolean.TRUE.equals(iglesia.getEstado())) {
			throw new NegocioException("La iglesia ya está eliminada.");
		}
		validarSinCompromisosElectorales(iglesiaId);
		Resultado resultado = desactivarDependencias(iglesia);
		iglesiaFacade.delete(iglesia);
		log.info("Iglesia id={} dada de baja con {} membresías, {} personas y {} usuarios", iglesiaId,
				resultado.membresias(), resultado.personas(), resultado.usuarios());
		return resultado;
	}

	/**
	 * Regulariza una iglesia ya dada de baja cuyos miembros quedaron activos
	 * (bajas anteriores a la eliminación en cascada lógica). No altera el estado
	 * de la iglesia: solo sincroniza lo que dependía de ella.
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public Resultado regularizar(Integer iglesiaId) {
		Iglesia iglesia = bajaFacade.buscarSinFiltro(iglesiaId);
		if (iglesia == null) {
			throw new NegocioException("La iglesia ya no existe; actualice la lista.");
		}
		if (Boolean.TRUE.equals(iglesia.getEstado())) {
			throw new NegocioException("La iglesia está activa: no hay nada que regularizar.");
		}
		validarSinCompromisosElectorales(iglesiaId);
		Resultado resultado = desactivarDependencias(iglesia);
		log.info("Iglesia id={} regularizada: {} membresías, {} personas y {} usuarios", iglesiaId,
				resultado.membresias(), resultado.personas(), resultado.usuarios());
		return resultado;
	}

	/**
	 * Desactiva membresías, personas sin otra membresía activa y usuarios de la
	 * iglesia. Compartido por la baja y la regularización para que ambas dejen el
	 * mismo estado.
	 */
	private Resultado desactivarDependencias(Iglesia iglesia) {
		List<IglesiaPersona> membresias = bajaFacade.membresiasActivas(iglesia.getId());
		List<Integer> personaIds = new ArrayList<>(membresias.size());
		for (IglesiaPersona membresia : membresias) {
			if (membresia.getPersona() != null && membresia.getPersona().getId() != null) {
				personaIds.add(membresia.getPersona().getId());
			}
			iglesiaPersonaFacade.delete(membresia);
		}

		// Una persona solo se desactiva si esta iglesia era su único vínculo activo.
		Set<Integer> conservanOtraIglesia = new HashSet<>(
				bajaFacade.personasConOtraMembresiaActiva(personaIds, iglesia.getId()));
		int personasBaja = 0;
		Set<Integer> procesadas = new HashSet<>();
		for (IglesiaPersona membresia : membresias) {
			Persona persona = membresia.getPersona();
			if (persona == null || persona.getId() == null || !procesadas.add(persona.getId())
					|| conservanOtraIglesia.contains(persona.getId())
					|| !Boolean.TRUE.equals(persona.getEstado())) {
				continue;
			}
			personaFacade.delete(persona);
			personasBaja++;
		}

		List<Usuario> usuarios = bajaFacade.usuariosActivos(iglesia.getId());
		for (Usuario usuario : usuarios) {
			usuarioFacade.delete(usuario);
		}

		return new Resultado(iglesia.getNombre(), membresias.size(), personasBaja, usuarios.size());
	}

	/**
	 * Impide dar de baja una iglesia con miembros comprometidos en el proceso
	 * electoral. El mensaje detalla cuántos y de qué tipo para que el
	 * administrador sepa qué revisar.
	 */
	private void validarSinCompromisosElectorales(Integer iglesiaId) {
		Map<String, Long> compromisos = bajaFacade.contarCompromisosElectorales(iglesiaId);
		if (compromisos.isEmpty()) {
			return;
		}
		StringBuilder detalle = new StringBuilder();
		agregarDetalle(detalle, compromisos.get("padron"), "en el padrón electoral");
		agregarDetalle(detalle, compromisos.get("jrv"), "designados en juntas receptoras del voto");
		agregarDetalle(detalle, compromisos.get("candidato"), "inscritos como candidatos");
		agregarDetalle(detalle, compromisos.get("tribunal"), "designados en el tribunal");
		throw new NegocioException("No se puede eliminar la iglesia porque tiene miembros " + detalle
				+ ". Retire primero esos registros del proceso electoral.");
	}

	private static void agregarDetalle(StringBuilder detalle, Long cantidad, String texto) {
		if (cantidad == null || cantidad == 0) {
			return;
		}
		if (detalle.length() > 0) {
			detalle.append(", ");
		}
		detalle.append(cantidad).append(' ').append(texto);
	}

	// =====================================================================
	// Restauración
	// =====================================================================

	/**
	 * Restaura una iglesia dada de baja y únicamente los registros que esa baja
	 * desactivó.
	 *
	 * <p>Se identifican por el número de revisión de Envers: la baja y todas sus
	 * desactivaciones ocurrieron en la misma transacción y comparten revisión,
	 * de modo que los registros que ya estaban inactivos antes —desactivados en
	 * otra revisión— no se reactivan.
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public Resultado restaurar(Integer iglesiaId) {
		Iglesia iglesia = bajaFacade.buscarSinFiltro(iglesiaId);
		if (iglesia == null) {
			throw new NegocioException("La iglesia ya no existe; actualice la lista.");
		}
		if (Boolean.TRUE.equals(iglesia.getEstado())) {
			throw new NegocioException("La iglesia ya está activa.");
		}
		Integer revision = bajaFacade.revisionDeBaja(iglesiaId);
		if (revision == null) {
			throw new NegocioException("No se encontró la revisión de auditoría de la eliminación,"
					+ " por lo que no es posible restaurar las relaciones de forma selectiva.");
		}

		List<Integer> idsDesactivados = bajaFacade.idsDesactivadosDesdeRevision("public.tb_iglesia_persona_aud",
				"igpe_id", iglesiaId, revision);
		List<IglesiaPersona> membresias = bajaFacade.membresiasPorIds(idsDesactivados);

		// La base impone «una sola iglesia activa por persona» (trigger
		// fn_validar_iglesia_activa_persona, por documento). Los conflictos se
		// detectan en bloque ANTES de escribir: así la restauración nunca provoca
		// la excepción del trigger ni deja la transacción a medias, y la iglesia
		// actual de una persona trasladada no se toca.
		Map<Integer, String> conflictos = detectarConflictos(idsDesactivados);

		int membresiasRestauradas = 0;
		List<Integer> personaIds = new ArrayList<>(membresias.size());
		for (IglesiaPersona membresia : membresias) {
			if (conflictos.containsKey(membresia.getId())) {
				continue;
			}
			if (membresia.getPersona() != null && membresia.getPersona().getId() != null) {
				personaIds.add(membresia.getPersona().getId());
			}
			if (!Boolean.TRUE.equals(membresia.getEstado())) {
				membresia.setEstado(true);
				iglesiaPersonaFacade.edit(membresia);
				membresiasRestauradas++;
			}
		}

		// Solo las personas de esas membresías, y solo si fue esta eliminación la
		// que las desactivó.
		List<Persona> personas = bajaFacade
				.personasPorIds(bajaFacade.personasDesactivadasDesdeRevision(personaIds, revision));
		int personasRestauradas = 0;
		for (Persona persona : personas) {
			if (!Boolean.TRUE.equals(persona.getEstado())) {
				persona.setEstado(true);
				personaFacade.edit(persona);
				personasRestauradas++;
			}
		}

		List<Usuario> usuarios = bajaFacade.usuariosPorIds(
				bajaFacade.idsDesactivadosDesdeRevision("public.tb_usuario_aud", "usu_id", iglesiaId, revision));
		int usuariosRestaurados = 0;
		for (Usuario usuario : usuarios) {
			if (!Boolean.TRUE.equals(usuario.getEstado())) {
				usuario.setEstado(true);
				usuarioFacade.edit(usuario);
				usuariosRestaurados++;
			}
		}

		iglesia.setEstado(true);
		iglesiaFacade.edit(iglesia);
		log.info("Iglesia id={} restaurada desde la revisión {}: {} membresías, {} personas y {} usuarios;"
				+ " {} membresía(s) omitida(s) por pertenencia activa a otra iglesia", iglesiaId, revision,
				membresiasRestauradas, personasRestauradas, usuariosRestaurados, conflictos.size());
		return new Resultado(iglesia.getNombre(), membresiasRestauradas, personasRestauradas, usuariosRestaurados,
				List.copyOf(conflictos.values()));
	}

	/**
	 * Membresías que no pueden reactivarse, con el motivo ya redactado para el
	 * usuario. Una sola consulta para todo el conjunto: sin N+1 y sin depender de
	 * que falle el trigger.
	 */
	private Map<Integer, String> detectarConflictos(List<Integer> igpeIds) {
		Map<Integer, String> conflictos = new LinkedHashMap<>();
		for (Object[] fila : bajaFacade.membresiasEnConflicto(igpeIds)) {
			Integer igpeId = ((Number) fila[0]).intValue();
			String documento = (String) fila[1];
			String nombre = (String) fila[2];
			String iglesiaEnConflicto = (String) fila[3];
			String motivo = iglesiaEnConflicto != null
					? nombre + " (" + documento + "): ya pertenece a " + iglesiaEnConflicto
					: nombre + " (" + documento + "): el documento está duplicado en otro miembro de esta iglesia";
			conflictos.put(igpeId, motivo);
		}
		return conflictos;
	}

	// =====================================================================
	// Consulta bajo demanda
	// =====================================================================

	/**
	 * Iglesias dadas de baja, con la fecha y el usuario de la eliminación. Se
	 * invoca solo al abrir «Ver eliminadas»: la pantalla principal nunca consulta
	 * esta información.
	 */
	public List<IglesiaEliminadaDTO> listarEliminadas() {
		List<Iglesia> iglesias = bajaFacade.listarInactivas();
		if (iglesias.isEmpty()) {
			return Collections.emptyList();
		}
		List<Integer> ids = new ArrayList<>(iglesias.size());
		for (Iglesia iglesia : iglesias) {
			ids.add(iglesia.getId());
		}
		Map<Integer, Object[]> bajas = bajaFacade.datosDeBaja(ids);
		Map<Integer, Long> colgadas = bajaFacade.contarMembresiasColgadas(ids);

		List<IglesiaEliminadaDTO> resultado = new ArrayList<>(iglesias.size());
		for (Iglesia iglesia : iglesias) {
			IglesiaEliminadaDTO dto = new IglesiaEliminadaDTO();
			dto.setId(iglesia.getId());
			dto.setNombre(iglesia.getNombre());
			dto.setDocumento(iglesia.getDocumento());
			dto.setComunidad(iglesia.getComunidad());
			if (iglesia.getUbicacion() != null) {
				dto.setUbicacionNombre(iglesia.getUbicacion().getName());
				if (iglesia.getUbicacion().getGeograp() != null) {
					dto.setCantonNombre(iglesia.getUbicacion().getGeograp().getName());
					if (iglesia.getUbicacion().getGeograp().getGeograp() != null) {
						dto.setProvinciaNombre(iglesia.getUbicacion().getGeograp().getGeograp().getName());
					}
				}
			}
			Object[] baja = bajas.get(iglesia.getId());
			if (baja != null) {
				dto.setRevisionBaja((Integer) baja[0]);
				dto.setFechaBaja(baja[1] instanceof java.util.Date fecha ? fecha : null);
				dto.setUsuarioBaja((String) baja[2]);
			}
			Long pendientes = colgadas.get(iglesia.getId());
			dto.setMembresiasColgadas(pendientes == null ? 0L : pendientes);
			resultado.add(dto);
		}
		return resultado;
	}
}
