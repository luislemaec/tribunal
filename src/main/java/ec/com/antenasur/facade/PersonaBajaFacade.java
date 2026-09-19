package ec.com.antenasur.facade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.Persona;
import ec.com.antenasur.model.generic.EntidadBase;
import org.hibernate.Session;

/**
 * Consultas de apoyo para la consulta y restauración de personas dadas de baja.
 *
 * <p>Mismo enfoque que {@link IglesiaBajaFacade}: {@link EntityManager} propio
 * sin el filtro {@code filterActive}, porque hay que leer justamente los
 * registros inactivos, y la restauración se apoya en el número de revisión de
 * Envers —la baja de una persona y la de sus membresías ocurren en la misma
 * transacción y comparten revisión— para no reactivar lo que ya estaba
 * inactivo antes.
 */
@Stateless
public class PersonaBajaFacade {

	/** Tope de filas del listado de eliminados: es una consulta de revisión, no un reporte. */
	public static final int MAX_ELIMINADAS = 500;

	@PersistenceContext(unitName = "tribunalPU")
	private EntityManager em;

	private EntityManager emSinFiltro() {
		Session session = em.unwrap(Session.class);
		if (session.getEnabledFilter(EntidadBase.FILTER_ACTIVE) != null) {
			session.disableFilter(EntidadBase.FILTER_ACTIVE);
		}
		return em;
	}

	/** Persona por id sin filtro de estado (una persona dada de baja también). */
	public Persona buscarSinFiltro(Integer id) {
		return id == null ? null : emSinFiltro().find(Persona.class, id);
	}

	/**
	 * Personas inactivas, acotadas a {@link #MAX_ELIMINADAS}: se consulta solo al
	 * abrir «Ver eliminadas».
	 */
	public List<Persona> listarInactivas() {
		return emSinFiltro()
				.createQuery("SELECT p FROM Persona p WHERE p.estado = FALSE"
						+ " ORDER BY p.fechaActualiza DESC NULLS LAST, p.id DESC", Persona.class)
				.setMaxResults(MAX_ELIMINADAS).getResultList();
	}

	/**
	 * Fecha, usuario y número de la revisión en que cada persona fue dada de baja
	 * (última revisión con {@code estado = false}), en una sola consulta.
	 *
	 * @return mapa personaId → [revisión, fecha, usuario]
	 */
	public Map<Integer, Object[]> datosDeBaja(List<Integer> personaIds) {
		Map<Integer, Object[]> resultado = new LinkedHashMap<>();
		if (personaIds == null || personaIds.isEmpty()) {
			return resultado;
		}
		String sql = "SELECT a.pers_id, a.rev, COALESCE(r.audit_date, r.create_date),"
				+ " COALESCE(r.update_user, r.create_user)"
				+ " FROM public.tb_persona_aud a"
				+ " LEFT JOIN tec.tec_auditoria r ON r.aud_id = a.rev"
				+ " WHERE a.pers_id IN (:ids) AND a.estado = FALSE"
				+ "   AND a.rev = (SELECT MAX(b.rev) FROM public.tb_persona_aud b"
				+ "                WHERE b.pers_id = a.pers_id AND b.estado = FALSE)";
		@SuppressWarnings("unchecked")
		List<Object[]> filas = emSinFiltro().createNativeQuery(sql).setParameter("ids", personaIds).getResultList();
		for (Object[] fila : filas) {
			resultado.put(((Number) fila[0]).intValue(),
					new Object[] { ((Number) fila[1]).intValue(), fila[2], fila[3] });
		}
		return resultado;
	}

	/**
	 * Última iglesia conocida de cada persona, como referencia para el operador:
	 * la membresía de mayor id, activa o no. Una sola consulta para todo el
	 * conjunto.
	 *
	 * @return mapa personaId → nombre de la iglesia
	 */
	public Map<Integer, String> ultimaIglesiaConocida(List<Integer> personaIds) {
		Map<Integer, String> resultado = new LinkedHashMap<>();
		if (personaIds == null || personaIds.isEmpty()) {
			return resultado;
		}
		String sql = "SELECT DISTINCT ON (ip.pers_id) ip.pers_id, i.igl_nombre"
				+ " FROM public.tb_iglesia_persona ip"
				+ " LEFT JOIN public.tb_iglesia i ON i.igl_id = ip.igl_id"
				+ " WHERE ip.pers_id IN (:ids)"
				+ " ORDER BY ip.pers_id, ip.igpe_id DESC";
		@SuppressWarnings("unchecked")
		List<Object[]> filas = emSinFiltro().createNativeQuery(sql).setParameter("ids", personaIds).getResultList();
		for (Object[] fila : filas) {
			resultado.put(((Number) fila[0]).intValue(), (String) fila[1]);
		}
		return resultado;
	}

	/**
	 * De un conjunto de personas dadas de baja, las cuyo documento ya lo usa otra
	 * persona activa: reactivarlas violaría la unicidad funcional de la cédula.
	 *
	 * @return mapa personaId → nombre de la persona activa que tiene el documento
	 */
	public Map<Integer, String> documentosOcupadosPorOtraPersonaActiva(List<Integer> personaIds) {
		Map<Integer, String> resultado = new LinkedHashMap<>();
		if (personaIds == null || personaIds.isEmpty()) {
			return resultado;
		}
		String sql = "SELECT DISTINCT ON (p.pers_id) p.pers_id, o.pers_nombre"
				+ " FROM public.tb_persona p"
				+ " JOIN public.tb_persona o ON o.estado = TRUE AND o.pers_id <> p.pers_id"
				+ "   AND BTRIM(o.pers_documento) = BTRIM(p.pers_documento)"
				+ " WHERE p.pers_id IN (:ids) AND NULLIF(BTRIM(p.pers_documento), '') IS NOT NULL"
				+ " ORDER BY p.pers_id, o.pers_id";
		@SuppressWarnings("unchecked")
		List<Object[]> filas = emSinFiltro().createNativeQuery(sql).setParameter("ids", personaIds).getResultList();
		for (Object[] fila : filas) {
			resultado.put(((Number) fila[0]).intValue(), (String) fila[1]);
		}
		return resultado;
	}

	/** Número de la revisión Envers en que se dio de baja la persona, o null. */
	public Integer revisionDeBaja(Integer personaId) {
		if (personaId == null) {
			return null;
		}
		List<?> filas = emSinFiltro()
				.createNativeQuery("SELECT MAX(rev) FROM public.tb_persona_aud"
						+ " WHERE pers_id = :id AND estado = FALSE")
				.setParameter("id", personaId).getResultList();
		Object valor = filas.isEmpty() ? null : filas.get(0);
		return valor == null ? null : ((Number) valor).intValue();
	}

	/**
	 * Membresías de la persona desactivadas en la revisión de su baja o después.
	 * Delimitar por revisión evita reactivar membresías que ya estaban inactivas
	 * por otro motivo (un traslado anterior, por ejemplo).
	 */
	public List<Integer> membresiasDesactivadasDesdeRevision(Integer personaId, Integer revision) {
		if (personaId == null || revision == null) {
			return Collections.emptyList();
		}
		String sql = "SELECT DISTINCT igpe_id FROM public.tb_iglesia_persona_aud"
				+ " WHERE pers_id = :id AND rev >= :rev AND revtype = 1 AND estado = FALSE";
		@SuppressWarnings("unchecked")
		List<Object> filas = emSinFiltro().createNativeQuery(sql).setParameter("id", personaId)
				.setParameter("rev", revision).getResultList();
		List<Integer> ids = new ArrayList<>(filas.size());
		for (Object fila : filas) {
			ids.add(((Number) fila).intValue());
		}
		return ids;
	}

	/**
	 * Membresías por id, sin filtro de estado y con la iglesia ya cargada: la
	 * restauración necesita saber si esa iglesia sigue activa.
	 */
	public List<IglesiaPersona> membresiasPorIds(List<Integer> ids) {
		if (ids == null || ids.isEmpty()) {
			return Collections.emptyList();
		}
		return emSinFiltro().createQuery("SELECT ip FROM IglesiaPersona ip"
				+ " LEFT JOIN FETCH ip.iglesia LEFT JOIN FETCH ip.persona"
				+ " WHERE ip.id IN :ids", IglesiaPersona.class).setParameter("ids", ids).getResultList();
	}
}
