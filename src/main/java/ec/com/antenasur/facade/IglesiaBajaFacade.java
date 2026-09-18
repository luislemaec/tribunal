package ec.com.antenasur.facade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.Persona;
import ec.com.antenasur.model.Usuario;
import ec.com.antenasur.model.generic.EntidadBase;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;

/**
 * Consultas de apoyo para la baja lógica y la restauración de iglesias.
 *
 * <p>Usa un {@link EntityManager} propio, sin el filtro
 * {@code filterActive} que aplica {@code AbstractFacade}: para restaurar hay
 * que leer precisamente los registros inactivos.
 *
 * <p>La restauración se apoya en Envers: todas las desactivaciones de una
 * eliminación ocurren en la misma transacción y, por tanto, comparten número de
 * revisión. Buscando esa revisión se sabe exactamente qué registros desactivó
 * esa eliminación, sin columnas ni tablas adicionales, y sin reactivar lo que
 * ya estaba inactivo antes.
 */
@Stateless
@Slf4j
public class IglesiaBajaFacade {

	@PersistenceContext(unitName = "tribunalPU")
	private EntityManager em;

	/**
	 * EntityManager sin el filtro de activos: necesario para leer iglesias,
	 * membresías y personas dadas de baja.
	 */
	private EntityManager emSinFiltro() {
		Session session = em.unwrap(Session.class);
		if (session.getEnabledFilter(EntidadBase.FILTER_ACTIVE) != null) {
			session.disableFilter(EntidadBase.FILTER_ACTIVE);
		}
		return em;
	}

	// ----- Compromisos con el proceso electoral -----

	/**
	 * Cuenta, por tipo de registro, los compromisos electorales vigentes de los
	 * miembros de una iglesia. Padrón, JRV, candidaturas y tribunal no cuelgan de
	 * la iglesia sino de la membresía ({@code igpe_id}), por lo que dar de baja
	 * las membresías los dejaría sin respaldo activo.
	 *
	 * @return mapa tipo → cantidad, solo con los tipos que tienen registros.
	 */
	public Map<String, Long> contarCompromisosElectorales(Integer iglesiaId) {
		Map<String, Long> resultado = new LinkedHashMap<>();
		if (iglesiaId == null) {
			return resultado;
		}
		String sql = "SELECT tipo, total FROM ("
				+ " SELECT 'padron' AS tipo, COUNT(*) AS total FROM tec.padron t"
				+ "   JOIN public.tb_iglesia_persona ip ON ip.igpe_id = t.igpe_id"
				+ "   WHERE ip.igl_id = :id AND t.estado = TRUE"
				+ " UNION ALL"
				+ " SELECT 'jrv', COUNT(*) FROM tec.miembros_jrv t"
				+ "   JOIN public.tb_iglesia_persona ip ON ip.igpe_id = t.igpe_id"
				+ "   WHERE ip.igl_id = :id AND t.estado = TRUE"
				+ " UNION ALL"
				+ " SELECT 'candidato', COUNT(*) FROM tec.candidatos t"
				+ "   JOIN public.tb_iglesia_persona ip ON ip.igpe_id = t.igpe_id"
				+ "   WHERE ip.igl_id = :id AND t.estado = TRUE"
				+ " UNION ALL"
				+ " SELECT 'tribunal', COUNT(*) FROM tec.tribunal t"
				+ "   JOIN public.tb_iglesia_persona ip ON ip.igpe_id = t.igpe_id"
				+ "   WHERE ip.igl_id = :id AND t.estado = TRUE"
				+ ") c WHERE total > 0";
		@SuppressWarnings("unchecked")
		List<Object[]> filas = emSinFiltro().createNativeQuery(sql).setParameter("id", iglesiaId).getResultList();
		for (Object[] fila : filas) {
			resultado.put((String) fila[0], ((Number) fila[1]).longValue());
		}
		return resultado;
	}

	// ----- Registros que dependen de la iglesia -----

	/** Membresías activas de una iglesia, con la persona ya cargada. */
	public List<IglesiaPersona> membresiasActivas(Integer iglesiaId) {
		if (iglesiaId == null) {
			return Collections.emptyList();
		}
		return emSinFiltro()
				.createQuery("SELECT ip FROM IglesiaPersona ip JOIN FETCH ip.persona p"
						+ " WHERE ip.iglesia.id = :id AND ip.estado = TRUE", IglesiaPersona.class)
				.setParameter("id", iglesiaId).getResultList();
	}

	/** Usuarios activos asociados a la iglesia (por ejemplo, su IglesiaAdmin). */
	public List<Usuario> usuariosActivos(Integer iglesiaId) {
		if (iglesiaId == null) {
			return Collections.emptyList();
		}
		return emSinFiltro().createQuery(
				"SELECT u FROM Usuario u WHERE u.iglesia.id = :id AND u.estado = TRUE", Usuario.class)
				.setParameter("id", iglesiaId).getResultList();
	}

	/**
	 * De un conjunto de personas, las que aún pertenecen a alguna otra iglesia
	 * activa. Se resuelve en una sola consulta para no repetirla por persona.
	 */
	public List<Integer> personasConOtraMembresiaActiva(List<Integer> personaIds, Integer iglesiaIdExcluida) {
		if (personaIds == null || personaIds.isEmpty()) {
			return Collections.emptyList();
		}
		return emSinFiltro().createQuery("SELECT DISTINCT p.id FROM IglesiaPersona ip"
				+ " JOIN ip.persona p JOIN ip.iglesia i"
				+ " WHERE p.id IN :personaIds AND i.id <> :iglesiaId"
				+ "   AND ip.estado = TRUE AND i.estado = TRUE", Integer.class)
				.setParameter("personaIds", personaIds).setParameter("iglesiaId", iglesiaIdExcluida).getResultList();
	}

	// ----- Iglesias dadas de baja -----

	/** Iglesias inactivas; se consulta solo cuando el usuario abre «Ver eliminadas». */
	public List<Iglesia> listarInactivas() {
		return emSinFiltro().createQuery("SELECT ig FROM Iglesia ig"
				+ " LEFT JOIN FETCH ig.ubicacion ub LEFT JOIN FETCH ub.geograp canton"
				+ " LEFT JOIN FETCH canton.geograp provincia"
				+ " WHERE ig.estado = FALSE ORDER BY ig.nombre", Iglesia.class).getResultList();
	}

	/**
	 * Fecha, usuario y número de la revisión en que cada iglesia fue dada de baja
	 * (última revisión con {@code estado = false}), en una sola consulta.
	 *
	 * @return mapa iglesiaId → [revisión, fecha, usuario]
	 */
	public Map<Integer, Object[]> datosDeBaja(List<Integer> iglesiaIds) {
		Map<Integer, Object[]> resultado = new LinkedHashMap<>();
		if (iglesiaIds == null || iglesiaIds.isEmpty()) {
			return resultado;
		}
		String sql = "SELECT a.igl_id, a.rev, COALESCE(r.audit_date, r.create_date),"
				+ " COALESCE(r.update_user, r.create_user)"
				+ " FROM public.tb_iglesia_aud a"
				+ " LEFT JOIN tec.tec_auditoria r ON r.aud_id = a.rev"
				+ " WHERE a.igl_id IN (:ids) AND a.estado = FALSE"
				+ "   AND a.rev = (SELECT MAX(b.rev) FROM public.tb_iglesia_aud b"
				+ "                WHERE b.igl_id = a.igl_id AND b.estado = FALSE)";
		@SuppressWarnings("unchecked")
		List<Object[]> filas = emSinFiltro().createNativeQuery(sql).setParameter("ids", iglesiaIds).getResultList();
		for (Object[] fila : filas) {
			resultado.put(((Number) fila[0]).intValue(),
					new Object[] { ((Number) fila[1]).intValue(), fila[2], fila[3] });
		}
		return resultado;
	}

	/** Membresías que siguen activas pese a que su iglesia está dada de baja. */
	public Map<Integer, Long> contarMembresiasColgadas(List<Integer> iglesiaIds) {
		Map<Integer, Long> resultado = new LinkedHashMap<>();
		if (iglesiaIds == null || iglesiaIds.isEmpty()) {
			return resultado;
		}
		List<Object[]> filas = emSinFiltro()
				.createQuery("SELECT i.id, COUNT(ip.id) FROM IglesiaPersona ip JOIN ip.iglesia i"
						+ " WHERE i.id IN :ids AND ip.estado = TRUE GROUP BY i.id", Object[].class)
				.setParameter("ids", iglesiaIds).getResultList();
		for (Object[] fila : filas) {
			resultado.put((Integer) fila[0], ((Number) fila[1]).longValue());
		}
		return resultado;
	}

	// ----- Restauración a partir de la revisión de la baja -----

	/** Número de la revisión Envers en que se dio de baja la iglesia, o null. */
	public Integer revisionDeBaja(Integer iglesiaId) {
		if (iglesiaId == null) {
			return null;
		}
		List<?> filas = emSinFiltro()
				.createNativeQuery("SELECT MAX(rev) FROM public.tb_iglesia_aud"
						+ " WHERE igl_id = :id AND estado = FALSE")
				.setParameter("id", iglesiaId).getResultList();
		Object valor = filas.isEmpty() ? null : filas.get(0);
		return valor == null ? null : ((Number) valor).intValue();
	}

	/**
	 * Registros de la iglesia desactivados desde la revisión de la baja en
	 * adelante, en las tablas de auditoría que llevan {@code igl_id}
	 * (membresías y usuarios).
	 *
	 * <p>Se toma «desde esa revisión» y no «en esa revisión exacta» porque una
	 * iglesia dada de baja antes de esta funcionalidad puede regularizarse
	 * después, en otra transacción: esas desactivaciones también forman parte de
	 * la eliminación y deben revertirse al restaurar. Lo desactivado antes de la
	 * baja queda fuera por tener una revisión anterior.
	 */
	public List<Integer> idsDesactivadosDesdeRevision(String tablaAud, String columnaId, Integer iglesiaId,
			Integer revision) {
		if (revision == null || iglesiaId == null) {
			return Collections.emptyList();
		}
		String sql = "SELECT DISTINCT " + columnaId + " FROM " + tablaAud
				+ " WHERE igl_id = :igl AND rev >= :rev AND revtype = 1 AND estado = FALSE";
		@SuppressWarnings("unchecked")
		List<Object> filas = emSinFiltro().createNativeQuery(sql).setParameter("igl", iglesiaId)
				.setParameter("rev", revision).getResultList();
		return aEnteros(filas);
	}

	/**
	 * De un conjunto de personas, las desactivadas en la revisión de la baja o
	 * después. {@code tb_persona_aud} no tiene iglesia, así que las candidatas se
	 * acotan con las personas de las membresías restauradas.
	 */
	public List<Integer> personasDesactivadasDesdeRevision(List<Integer> personaIds, Integer revision) {
		if (personaIds == null || personaIds.isEmpty() || revision == null) {
			return Collections.emptyList();
		}
		String sql = "SELECT DISTINCT pers_id FROM public.tb_persona_aud"
				+ " WHERE pers_id IN (:ids) AND rev >= :rev AND revtype = 1 AND estado = FALSE";
		@SuppressWarnings("unchecked")
		List<Object> filas = emSinFiltro().createNativeQuery(sql).setParameter("ids", personaIds)
				.setParameter("rev", revision).getResultList();
		return aEnteros(filas);
	}

	private static List<Integer> aEnteros(List<Object> filas) {
		List<Integer> ids = new ArrayList<>(filas.size());
		for (Object fila : filas) {
			ids.add(((Number) fila).intValue());
		}
		return ids;
	}

	/**
	 * De un conjunto de membresías a reactivar, las que chocarían con la regla
	 * «una sola iglesia activa por persona» que aplica el trigger
	 * {@code fn_validar_iglesia_activa_persona}.
	 *
	 * <p>La regla se evalúa por documento (no por {@code pers_id}), igual que el
	 * trigger, y considera dos fuentes de conflicto:
	 * <ul>
	 * <li><b>externo</b>: el documento ya tiene una membresía activa en otra
	 * iglesia (por ejemplo, la persona fue trasladada después de la baja);</li>
	 * <li><b>interno</b>: dos membresías del propio conjunto comparten documento
	 * —dato histórico duplicado—, de las que solo una puede quedar activa.</li>
	 * </ul>
	 *
	 * <p>Se resuelve en una consulta previa a cualquier escritura, de modo que la
	 * restauración nunca provoque la excepción del trigger.
	 *
	 * @return filas [igpe_id, documento, nombre de la persona, iglesia en
	 *         conflicto o null si el conflicto es interno]
	 */
	public List<Object[]> membresiasEnConflicto(List<Integer> igpeIds) {
		if (igpeIds == null || igpeIds.isEmpty()) {
			return Collections.emptyList();
		}
		String sql = "WITH candidatas AS ("
				+ "   SELECT ip.igpe_id, NULLIF(BTRIM(p.pers_documento), '') AS doc, p.pers_nombre"
				+ "     FROM public.tb_iglesia_persona ip"
				+ "     JOIN public.tb_persona p ON p.pers_id = ip.pers_id"
				+ "    WHERE ip.igpe_id IN (:ids))"
				+ " SELECT c.igpe_id, c.doc, c.pers_nombre, ext.nombre_iglesia FROM candidatas c"
				// Conflicto externo: el documento ya está activo fuera del conjunto.
				+ " LEFT JOIN LATERAL ("
				+ "   SELECT i.igl_nombre AS nombre_iglesia FROM public.tb_iglesia_persona o"
				+ "     JOIN public.tb_persona po ON po.pers_id = o.pers_id"
				+ "     LEFT JOIN public.tb_iglesia i ON i.igl_id = o.igl_id"
				+ "    WHERE o.estado = TRUE AND BTRIM(po.pers_documento) = c.doc"
				+ "      AND o.igpe_id NOT IN (:ids)"
				+ "    LIMIT 1) ext ON TRUE"
				+ " WHERE c.doc IS NOT NULL"
				+ "   AND (ext.nombre_iglesia IS NOT NULL"
				// Conflicto interno: otra candidata con el mismo documento se
				// reactivará antes (se conserva la de menor igpe_id).
				+ "        OR EXISTS (SELECT 1 FROM candidatas d"
				+ "                    WHERE d.doc = c.doc AND d.igpe_id < c.igpe_id))";
		@SuppressWarnings("unchecked")
		List<Object[]> filas = emSinFiltro().createNativeQuery(sql).setParameter("ids", igpeIds).getResultList();
		return filas;
	}

	/** Membresías por id, sin filtro de estado (para reactivarlas). */
	public List<IglesiaPersona> membresiasPorIds(List<Integer> ids) {
		if (ids == null || ids.isEmpty()) {
			return Collections.emptyList();
		}
		return emSinFiltro()
				.createQuery("SELECT ip FROM IglesiaPersona ip LEFT JOIN FETCH ip.persona WHERE ip.id IN :ids", IglesiaPersona.class)
				.setParameter("ids", ids).getResultList();
	}

	/** Personas por id, sin filtro de estado. */
	public List<Persona> personasPorIds(List<Integer> ids) {
		if (ids == null || ids.isEmpty()) {
			return Collections.emptyList();
		}
		return emSinFiltro().createQuery("SELECT p FROM Persona p WHERE p.id IN :ids", Persona.class)
				.setParameter("ids", ids).getResultList();
	}

	/** Usuarios por id, sin filtro de estado. */
	public List<Usuario> usuariosPorIds(List<Integer> ids) {
		if (ids == null || ids.isEmpty()) {
			return Collections.emptyList();
		}
		return emSinFiltro().createQuery("SELECT u FROM Usuario u WHERE u.id IN :ids", Usuario.class)
				.setParameter("ids", ids).getResultList();
	}

	/** Iglesia por id sin filtro de estado (una iglesia dada de baja también). */
	public Iglesia buscarSinFiltro(Integer id) {
		return id == null ? null : emSinFiltro().find(Iglesia.class, id);
	}
}
