/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.facade;

import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.dto.FiltroMiembrosDTO;
import ec.com.antenasur.dto.ResumenMiembrosIglesiaDTO;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.ejb.Stateless;
import jakarta.persistence.NoResultException;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;

import ec.com.antenasur.model.generic.AbstractFacade;
import ec.com.antenasur.model.generic.EntidadBase;
import org.hibernate.Filter;
import org.hibernate.Session;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class IglesiaPersonaFacade extends AbstractFacade<IglesiaPersona, Integer> {

	static final String HQL = "SELECT ip FROM IglesiaPersona ip";

	public IglesiaPersonaFacade() {
		super(IglesiaPersona.class, Integer.class);
	}

	/**
	 * Devuelve el vínculo iglesia-persona vigente más reciente para una
	 * persona dada. "Vigente" = estado activo. Si la persona pertenece a varias
	 * iglesias históricamente, retorna la última registrada.
	 */
	public IglesiaPersona getVigentePorPersonaId(Integer personaId) {
		if (personaId == null) {
			return null;
		}
		try {
			String sql = HQL + " LEFT JOIN FETCH ip.iglesia i"
					+ " WHERE ip.persona.id = :personaId AND ip.estado = TRUE" + " ORDER BY ip.id DESC";
			TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
			query.setParameter("personaId", personaId);
			query.setMaxResults(1);
			List<IglesiaPersona> result = query.getResultList();
			return (result != null && !result.isEmpty()) ? result.get(0) : null;
		} catch (NoResultException e) {
			return null;
		}
	}

	public List<IglesiaPersona> getPersonasIglesiasPorParroquia(Geograp parroquia) {
		if (parroquia == null) {
			return java.util.Collections.emptyList();
		}
		try {
			String sql = HQL + " LEFT JOIN FETCH ip.iglesia igl" + " LEFT JOIN FETCH igl.ubicacion parroquia"
					+ " LEFT JOIN FETCH parroquia.geograp canton" + " LEFT JOIN FETCH canton.geograp provincia"
					+ " LEFT JOIN FETCH ip.persona p" + " WHERE igl.ubicacion = :parroquia AND ip.estado = TRUE"
					+ " ORDER BY ip.id";
			TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
			query.setParameter("parroquia", parroquia);
			List<IglesiaPersona> result = query.getResultList();
			return result != null ? result : java.util.Collections.<IglesiaPersona>emptyList();
		} catch (Exception e) {
			e.printStackTrace();
			return java.util.Collections.emptyList();
		}
	}

	public List<IglesiaPersona> getPersonasIglesiasPorIglesia(int iglesiaId) {
		try {
			String sql = HQL + " LEFT JOIN FETCH ip.iglesia i" + " LEFT JOIN FETCH i.ubicacion parroquia"
					+ " LEFT JOIN FETCH parroquia.geograp canton" + " LEFT JOIN FETCH canton.geograp provincia"
					+ " LEFT JOIN FETCH ip.persona p" + " WHERE i.id = :iglesiaId AND ip.estado = TRUE"
					+ " ORDER BY ip.id";
			TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
			query.setParameter("iglesiaId", iglesiaId);
			List<IglesiaPersona> result = query.getResultList();
			return result != null ? result : java.util.Collections.<IglesiaPersona>emptyList();
		} catch (Exception e) {
			e.printStackTrace();
			return java.util.Collections.emptyList();
		}
	}

	/**
	 * Obtiene los indicadores de miembros para una iglesia mediante una sola
	 * consulta. Solo considera vinculos y personas activos. La identidad se
	 * consolida por documento para que una inconsistencia historica no infle los
	 * totales de una misma iglesia; cuando no existe documento se usa el
	 * identificador tecnico de la persona.
	 */
	public ResumenMiembrosIglesiaDTO obtenerResumenMiembrosActivosPorIglesia(Integer iglesiaId) {
		if (iglesiaId == null) {
			return new ResumenMiembrosIglesiaDTO(0, 0, 0);
		}

		String identidad = "COALESCE(NULLIF(BTRIM(p.pers_documento), ''), CONCAT('#', p.pers_id::TEXT))";
		// El sistema conserva nombres y apellidos consolidados en pers_nombre;
		// pers_apellido es un dato historico no requerido para esta validacion.
		String informacionCompleta = "NULLIF(BTRIM(p.pers_documento), '') IS NOT NULL"
				+ " AND NULLIF(BTRIM(p.pers_nombre), '') IS NOT NULL"
				+ " AND NULLIF(BTRIM(p.pers_sexo), '') IS NOT NULL";
		String revisionPendiente = "ip.f_actualiza IS NULL"
				+ " OR (ip.f_crea IS NOT NULL AND ip.f_actualiza < ip.f_crea)";
		String sql = "SELECT COUNT(DISTINCT " + identidad + "), " + "COUNT(DISTINCT CASE WHEN " + informacionCompleta
				+ " THEN " + identidad + " END), " + "COUNT(DISTINCT CASE WHEN " + revisionPendiente + " THEN "
				+ identidad + " END) " + "FROM public.tb_iglesia_persona ip "
				+ "JOIN public.tb_persona p ON p.pers_id = ip.pers_id " + "WHERE ip.igl_id = :iglesiaId "
				+ "AND ip.estado = TRUE " + "AND p.estado = TRUE";

		Object[] fila = (Object[]) getEntityManager().createNativeQuery(sql).setParameter("iglesiaId", iglesiaId)
				.getSingleResult();
		return new ResumenMiembrosIglesiaDTO(numeroComoEntero(fila[0]), numeroComoEntero(fila[1]),
				numeroComoEntero(fila[2]));
	}

	private int numeroComoEntero(Object valor) {
		return valor instanceof Number ? ((Number) valor).intValue() : 0;
	}

	public List<IglesiaPersona> getPersonasHabilitadasPadronPorIglesia(int iglesiaId) {
		try {
			String sql = HQL + " LEFT JOIN FETCH ip.iglesia i" + " LEFT JOIN FETCH ip.persona p"
					+ " WHERE i.id = :iglesiaId" + "   AND ip.estado = TRUE" + "   AND p.estado = TRUE"
					+ "   AND ip.habilitadoPadron = TRUE" + " ORDER BY ip.id";
			TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
			query.setParameter("iglesiaId", iglesiaId);
			List<IglesiaPersona> result = query.getResultList();
			return result != null ? result : java.util.Collections.<IglesiaPersona>emptyList();
		} catch (Exception e) {
			e.printStackTrace();
			return java.util.Collections.emptyList();
		}
	}

	/**
	 * Miembros activos por iglesia, en una sola consulta agregada para toda la
	 * lista. Evita el N+1 de contar por iglesia y permite que la tabla muestre el
	 * dato sin consultas desde los getters.
	 *
	 * @return mapa iglesiaId → miembros activos; las iglesias sin miembros no
	 *         aparecen en el mapa.
	 */
	public Map<Integer, Integer> contarMiembrosActivosPorIglesias(List<Integer> iglesiaIds) {
		Map<Integer, Integer> resultado = new HashMap<>();
		if (iglesiaIds == null || iglesiaIds.isEmpty()) {
			return resultado;
		}
		try {
			String jpql = "SELECT i.id, COUNT(ip.id)" + " FROM IglesiaPersona ip" + " JOIN ip.iglesia i"
					+ " JOIN ip.persona p" + " WHERE i.id IN :iglesiaIds" + "   AND ip.estado = TRUE"
					+ "   AND p.estado = TRUE" + " GROUP BY i.id";
			List<Object[]> filas = super.getEntityManager().createQuery(jpql, Object[].class)
					.setParameter("iglesiaIds", iglesiaIds).getResultList();
			for (Object[] fila : filas) {
				Number total = (Number) fila[1];
				resultado.put((Integer) fila[0], total != null ? total.intValue() : 0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultado;
	}

	public Map<Integer, Integer> contarPersonasHabilitadasPadronPorIglesias(List<Integer> iglesiaIds) {
		Map<Integer, Integer> resultado = new HashMap<>();
		if (iglesiaIds == null || iglesiaIds.isEmpty()) {
			return resultado;
		}
		try {
			String sql = "SELECT i.id, COUNT(ip.id)" + " FROM IglesiaPersona ip" + " JOIN ip.iglesia i"
					+ " JOIN ip.persona p" + " WHERE i.id IN :iglesiaIds" + "   AND ip.estado = TRUE"
					+ "   AND p.estado = TRUE" + "   AND ip.habilitadoPadron = TRUE" + " GROUP BY i.id";
			List<Object[]> filas = super.getEntityManager().createQuery(sql, Object[].class)
					.setParameter("iglesiaIds", iglesiaIds).getResultList();
			for (Object[] fila : filas) {
				Integer iglesiaId = (Integer) fila[0];
				Number total = (Number) fila[1];
				resultado.put(iglesiaId, total != null ? total.intValue() : 0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultado;
	}

	/**
	 * Trae IglesiaPersona activos por parroquia(s) hidratando en una sola query las
	 * relaciones que la vista/DTO consultan después ({@code iglesia},
	 * {@code iglesia.ubicacion}, {@code persona}). Evita N+1: sin estos JOIN FETCH,
	 * mapear cada IglesiaPersona a DTO disparaba una query por persona y otra por
	 * iglesia, multiplicando el tiempo de respuesta hasta sobrepasar el timeout JTA
	 * (300s) y romper la transacción.
	 *
	 * <p>
	 * Filtra por {@code ip.estado = TRUE} para excluir soft-deleted y limita por
	 * relación con la lista de parroquias.
	 */
	public List<IglesiaPersona> getIglesiasPersonasPorParroquias(List<Geograp> parroquias) {
		if (parroquias == null || parroquias.isEmpty()) {
			return java.util.Collections.emptyList();
		}
		try {
			String sql = HQL + " LEFT JOIN FETCH ip.iglesia igl" + " LEFT JOIN FETCH igl.ubicacion ub"
					+ " LEFT JOIN FETCH ub.geograp canton" + " LEFT JOIN FETCH canton.geograp provincia"
					+ " LEFT JOIN FETCH ip.persona p" + " WHERE ub IN :parroquias AND ip.estado = TRUE"
					+ " ORDER BY ip.id";
			TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
			query.setParameter("parroquias", parroquias);
			List<IglesiaPersona> result = query.getResultList();
			return result != null ? result : java.util.Collections.<IglesiaPersona>emptyList();
		} catch (Exception e) {
			e.printStackTrace();
			return java.util.Collections.emptyList();
		}
	}

	// ------------------------------------------------------------------
	// Listado paginado de miembros (pantalla Personas). Paginación, orden,
	// filtros y conteo se resuelven en BD: la tabla nunca carga el listado
	// completo en memoria.
	// ------------------------------------------------------------------

	/**
	 * Documentos con más de una iglesia activa: misma regla que
	 * {@link #contarIglesiasActivasPorDocumentos(Collection)} (relación y persona activas).
	 */
	private static final String SUB_DOCUMENTOS_VARIAS_IGLESIAS = "SELECT TRIM(pvi.documento)"
			+ " FROM IglesiaPersona ipvi JOIN ipvi.iglesia ivi JOIN ipvi.persona pvi"
			+ " WHERE ipvi.estado = TRUE AND pvi.estado = TRUE"
			+ " GROUP BY TRIM(pvi.documento) HAVING COUNT(DISTINCT ivi.id) > 1";

	/**
	 * Documentos repetidos entre personas activas, sin los documentos históricos
	 * especiales ({@code S/N} y {@code SN-<dígitos>}), igual que el mapeo del servicio.
	 */
	private static final String SUB_DOCUMENTOS_CEDULA_REPETIDA = "SELECT TRIM(pcr.documento) FROM Persona pcr"
			+ " WHERE pcr.estado = TRUE AND pcr.documento IS NOT NULL"
			+ " AND NOT " + esDocumentoHistoricoEspecial("UPPER(TRIM(pcr.documento))")
			+ " GROUP BY TRIM(pcr.documento) HAVING COUNT(pcr.id) > 1";

	/** Equivale a {@code S/N} o {@code SN-\d+} sin depender de expresiones regulares de la BD. */
	private static String esDocumentoHistoricoEspecial(String documento) {
		String resto = "SUBSTRING(" + documento + ", 4)";
		for (int digito = 0; digito <= 9; digito++) {
			resto = "REPLACE(" + resto + ", '" + digito + "', '')";
		}
		return "(" + documento + " = 'S/N' OR (" + documento + " LIKE 'SN-%' AND LENGTH(" + documento
				+ ") > 3 AND LENGTH(" + resto + ") = 0))";
	}

	private static String inconsistencia(String persona) {
		return "(TRIM(" + persona + ".documento) IN (" + SUB_DOCUMENTOS_VARIAS_IGLESIAS + ") OR TRIM(" + persona
				+ ".documento) IN (" + SUB_DOCUMENTOS_CEDULA_REPETIDA + "))";
	}

	/** Misma regla que {@code IglesiaPersonaDTO.esActualizada}. */
	private static String revisada(String relacion) {
		return "(" + relacion + ".fechaActualiza IS NOT NULL AND (" + relacion + ".fechaCrea IS NULL OR " + relacion
				+ ".fechaActualiza >= " + relacion + ".fechaCrea))";
	}

	/**
	 * FROM y WHERE del listado. {@code sufijo} distingue los alias cuando se usa como
	 * subconsulta; {@code fetch} carga iglesia, ubicación y persona en la misma consulta.
	 * Solo relaciones activas; el alcance es la iglesia o el nivel geográfico más específico.
	 */
	private static String desdeMiembros(FiltroMiembrosDTO f, String sufijo, boolean fetch) {
		String join = fetch ? " LEFT JOIN FETCH " : " LEFT JOIN ";
		String ip = "ip" + sufijo;
		String p = "p" + sufijo;
		StringBuilder hql = new StringBuilder(" FROM IglesiaPersona ").append(ip)
				.append(join).append(ip).append(".iglesia igl").append(sufijo)
				.append(join).append("igl").append(sufijo).append(".ubicacion ub").append(sufijo)
				.append(join).append("ub").append(sufijo).append(".geograp canton").append(sufijo)
				.append(join).append("canton").append(sufijo).append(".geograp provincia").append(sufijo)
				.append(join).append(ip).append(".persona ").append(p)
				.append(" WHERE ").append(ip).append(".estado = TRUE");
		if (f.getIglesiaId() != null) {
			hql.append(" AND igl").append(sufijo).append(".id = :iglesiaId");
		} else if (f.getParroquiaId() != null) {
			hql.append(" AND ub").append(sufijo).append(".id = :ubicacionId");
		} else if (f.getCantonId() != null) {
			hql.append(" AND canton").append(sufijo).append(".id = :ubicacionId");
		} else {
			hql.append(" AND provincia").append(sufijo).append(".id = :ubicacionId");
		}
		if (tieneTexto(f.getDocumento())) {
			hql.append(" AND LOWER(").append(p).append(".documento) LIKE :documento ESCAPE '!'");
		}
		if (tieneTexto(f.getNombres())) {
			hql.append(" AND LOWER(").append(p).append(".nombres) LIKE :nombres ESCAPE '!'");
		}
		if (tieneTexto(f.getBusqueda())) {
			hql.append(" AND (LOWER(").append(p).append(".documento) LIKE :busqueda ESCAPE '!' OR LOWER(")
					.append(p).append(".nombres) LIKE :busqueda ESCAPE '!')");
		}
		if (f.getHabilitado() != null) {
			hql.append(Boolean.TRUE.equals(f.getHabilitado()) ? " AND " + ip + ".habilitadoPadron = TRUE"
					: " AND (" + ip + ".habilitadoPadron IS NULL OR " + ip + ".habilitadoPadron = FALSE)");
		}
		if (f.getRevisado() != null) {
			hql.append(Boolean.TRUE.equals(f.getRevisado()) ? " AND " : " AND NOT ").append(revisada(ip));
		}
		if (f.getInconsistencia() != null) {
			hql.append(Boolean.TRUE.equals(f.getInconsistencia()) ? " AND " : " AND NOT ").append(inconsistencia(p));
		}
		return hql.toString();
	}

	private static void parametrizarMiembros(jakarta.persistence.Query query, FiltroMiembrosDTO f) {
		if (f.getIglesiaId() != null) {
			query.setParameter("iglesiaId", f.getIglesiaId());
		} else {
			query.setParameter("ubicacionId", f.getParroquiaId() != null ? f.getParroquiaId()
					: f.getCantonId() != null ? f.getCantonId() : f.getProvinciaId());
		}
		if (tieneTexto(f.getDocumento())) {
			query.setParameter("documento", contiene(f.getDocumento()));
		}
		if (tieneTexto(f.getNombres())) {
			query.setParameter("nombres", contiene(f.getNombres()));
		}
		if (tieneTexto(f.getBusqueda())) {
			query.setParameter("busqueda", contiene(f.getBusqueda()));
		}
	}

	private static boolean tieneTexto(String valor) {
		return valor != null && !valor.isBlank();
	}

	/** Patrón «contiene» sin distinguir mayúsculas; escapa los comodines del usuario. */
	private static String contiene(String valor) {
		String limpio = valor.trim().toLowerCase(java.util.Locale.ROOT).replace("!", "!!").replace("%", "!%")
				.replace("_", "!_");
		return "%" + limpio + "%";
	}

	/** Columnas ordenables de la tabla; cualquier otro campo ordena por id. */
	private static String expresionOrden(String campo) {
		if (campo == null) {
			return null;
		}
		switch (campo) {
		case "persona.documento":
			return "p.documento";
		case "persona.nombres":
			return "p.nombres";
		case "persona.fechaCrea":
			return "p.fechaCrea";
		case "habilitadoPadron":
			return "CASE WHEN ip.habilitadoPadron = TRUE THEN 1 ELSE 0 END";
		case "actualizada":
			return "CASE WHEN " + revisada("ip") + " THEN 1 ELSE 0 END";
		case "tieneInconsistencia":
			return "CASE WHEN " + inconsistencia("p") + " THEN 1 ELSE 0 END";
		default:
			return null;
		}
	}

	/** Total de miembros que cumplen el filtro. Sin alcance devuelve 0. */
	public long contarMiembros(FiltroMiembrosDTO f) {
		if (f == null || !f.tieneAlcance()) {
			return 0;
		}
		TypedQuery<Long> query = getEntityManager().createQuery("SELECT COUNT(ip)" + desdeMiembros(f, "", false),
				Long.class);
		parametrizarMiembros(query, f);
		Long total = query.getSingleResult();
		return total != null ? total : 0;
	}

	/**
	 * Página de miembros con iglesia, ubicación y persona cargadas en una sola consulta.
	 * {@code maximo <= 0} devuelve todos (exportación). Una persona con más de una iglesia
	 * activa aparece una vez por cada relación que cumpla el filtro.
	 */
	public List<IglesiaPersona> listarMiembros(FiltroMiembrosDTO f, int primero, int maximo, String campoOrden,
			boolean descendente) {
		if (f == null || !f.tieneAlcance()) {
			return new ArrayList<>();
		}
		String orden = expresionOrden(campoOrden);
		String hql = "SELECT ip" + desdeMiembros(f, "", true) + " ORDER BY "
				+ (orden == null ? "ip.id" : orden + (descendente ? " DESC" : " ASC") + ", ip.id");
		TypedQuery<IglesiaPersona> query = getEntityManager().createQuery(hql, IglesiaPersona.class);
		parametrizarMiembros(query, f);
		query.setFirstResult(Math.max(primero, 0));
		if (maximo > 0) {
			query.setMaxResults(maximo);
		}
		return query.getResultList();
	}

	/**
	 * Iglesias activas por documento para todos los miembros del filtro. Usa el filtro como
	 * subconsulta en lugar de una lista de parámetros: no depende del límite de 32.767
	 * parámetros de PostgreSQL. Misma regla que {@link #contarIglesiasActivasPorDocumentos}.
	 */
	public Map<String, Integer> contarIglesiasActivasPorFiltro(FiltroMiembrosDTO f) {
		Map<String, Integer> resultado = new LinkedHashMap<>();
		if (f == null || !f.tieneAlcance()) {
			return resultado;
		}
		String hql = "SELECT TRIM(pa.documento), COUNT(DISTINCT ia.id) FROM IglesiaPersona ipa JOIN ipa.iglesia ia"
				+ " JOIN ipa.persona pa WHERE ipa.estado = TRUE AND pa.estado = TRUE"
				+ " AND TRIM(pa.documento) IN (SELECT TRIM(p_s.documento)" + desdeMiembros(f, "_s", false) + ")"
				+ " GROUP BY TRIM(pa.documento)";
		TypedQuery<Object[]> query = getEntityManager().createQuery(hql, Object[].class);
		parametrizarMiembros(query, f);
		for (Object[] fila : query.getResultList()) {
			resultado.put((String) fila[0], ((Number) fila[1]).intValue());
		}
		return resultado;
	}

	/**
	 * Personas activas por documento para todos los miembros del filtro (cédula repetida),
	 * con la misma subconsulta. Misma regla que {@code PersonaFacade.contarPersonasActivasPorDocumentos}.
	 */
	public Map<String, Integer> contarPersonasActivasPorFiltro(FiltroMiembrosDTO f) {
		Map<String, Integer> resultado = new LinkedHashMap<>();
		if (f == null || !f.tieneAlcance()) {
			return resultado;
		}
		String hql = "SELECT TRIM(pc.documento), COUNT(pc.id) FROM Persona pc"
				+ " WHERE pc.estado = TRUE AND pc.documento IS NOT NULL"
				+ " AND TRIM(pc.documento) IN (SELECT TRIM(p_s.documento)" + desdeMiembros(f, "_s", false) + ")"
				+ " GROUP BY TRIM(pc.documento)";
		TypedQuery<Object[]> query = getEntityManager().createQuery(hql, Object[].class);
		parametrizarMiembros(query, f);
		for (Object[] fila : query.getResultList()) {
			resultado.put((String) fila[0], ((Number) fila[1]).intValue());
		}
		return resultado;
	}

	/**
	 * Devuelve el vínculo activo más reciente para la persona identificada por
	 * su DOCUMENTO (cédula), independiente del id interno de la persona.
	 *
	 * <p>
	 * Pensado para entornos donde existen filas duplicadas en {@code tb_persona}
	 * con el mismo documento (caso real en producción). El método
	 * {@link #getVigentePorPersonaId(Integer)} requiere conocer el id exacto, pero
	 * {@code finByPersonaDocument} devuelve la persona con id ASC y el vínculo
	 * en {@code tb_iglesia_persona} podría apuntar al id duplicado mayor —
	 * generando "sin iglesia" falso. Esta variante resuelve por documento y evita
	 * ese problema.
	 */
	public IglesiaPersona getVigentePorDocumentoPersona(String documento) {
		if (documento == null || documento.trim().isEmpty()) {
			return null;
		}
		try {
			String sql = HQL + " LEFT JOIN FETCH ip.iglesia i" + " LEFT JOIN FETCH ip.persona p"
					+ " WHERE p.documento = :documento" + "   AND ip.estado = TRUE" + "   AND p.estado = TRUE"
					+ " ORDER BY ip.id DESC";
			TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
			query.setParameter("documento", documento.trim());
			query.setMaxResults(1);
			List<IglesiaPersona> result = query.getResultList();
			return (result != null && !result.isEmpty()) ? result.get(0) : null;
		} catch (NoResultException e) {
			return null;
		}
	}

	/**
	 * Lista todos los vinculos activos de una persona usando el documento
	 * institucional como identidad funcional. La variante con bloqueo se usa antes
	 * de altas y regularizaciones para serializar operaciones concurrentes.
	 */
	public List<IglesiaPersona> listarActivasPorDocumento(String documento, boolean bloquear) {
		if (documento == null || documento.trim().isEmpty()) {
			return java.util.Collections.emptyList();
		}
		String fetchUbicacion = bloquear ? ""
				: " LEFT JOIN FETCH i.ubicacion parroquia" + " LEFT JOIN FETCH parroquia.geograp canton"
						+ " LEFT JOIN FETCH canton.geograp provincia";
		String sql = HQL + " JOIN FETCH ip.iglesia i" + fetchUbicacion + " JOIN FETCH ip.persona p"
				+ " WHERE TRIM(p.documento) = :documento" + "   AND ip.estado = TRUE" + "   AND p.estado = TRUE"
				+ " ORDER BY ip.id";
		TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
		query.setParameter("documento", documento.trim());
		if (bloquear) {
			query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
		}
		List<IglesiaPersona> resultado = query.getResultList();
		return resultado != null ? resultado : java.util.Collections.emptyList();
	}

	public List<IglesiaPersona> listarActivasPorDocumento(String documento) {
		return listarActivasPorDocumento(documento, false);
	}

	/**
	 * Relaciones activas del documento con el mismo criterio que el trigger
	 * fn_validar_iglesia_activa_persona: basta {@code ip.estado = TRUE}, sin
	 * importar si el registro de persona está activo. Permite validar antes de
	 * persistir, en lugar de enterarse por la violación 23505 del trigger.
	 */
	public List<IglesiaPersona> listarRelacionesActivasPorDocumento(String documento, boolean bloquear) {
		if (documento == null || documento.trim().isEmpty()) {
			return java.util.Collections.emptyList();
		}
		// Igual que listarRelacionesPorDocumentos: el filtro de activos ocultaría las
		// personas inactivas, que el trigger sí considera.
		Session session = getEntityManager().unwrap(Session.class);
		Filter filtro = session.getEnabledFilter(EntidadBase.FILTER_ACTIVE);
		if (filtro != null) {
			session.disableFilter(EntidadBase.FILTER_ACTIVE);
		}
		try {
			String sql = HQL + " JOIN FETCH ip.iglesia i JOIN FETCH ip.persona p"
					+ " WHERE TRIM(p.documento) = :documento AND ip.estado = TRUE ORDER BY ip.id";
			TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
			query.setParameter("documento", documento.trim());
			if (bloquear) {
				query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
			}
			List<IglesiaPersona> resultado = query.getResultList();
			return resultado != null ? resultado : java.util.Collections.emptyList();
		} finally {
			if (filtro != null) {
				session.enableFilter(EntidadBase.FILTER_ACTIVE);
			}
		}
	}

	/**
	 * Búsqueda de consulta de miembros activos por cédula o nombres. Carga la
	 * ubicación completa de la iglesia en una sola consulta para que el DTO pueda
	 * mostrar provincia, cantón y parroquia sin consultas por fila.
	 */
	public List<IglesiaPersona> buscarActivasPorCriterio(String criterio, Integer iglesiaId, int limite) {
		if (criterio == null || criterio.isBlank() || limite <= 0) {
			return java.util.Collections.emptyList();
		}
		String hql = "SELECT DISTINCT ip FROM IglesiaPersona ip" + " JOIN FETCH ip.persona p"
				+ " JOIN FETCH ip.iglesia i" + " LEFT JOIN FETCH i.ubicacion parroquia"
				+ " LEFT JOIN FETCH parroquia.geograp canton" + " LEFT JOIN FETCH canton.geograp provincia"
				+ " WHERE ip.estado = TRUE AND p.estado = TRUE AND i.estado = TRUE"
				+ " AND (LOWER(TRIM(COALESCE(p.documento, ''))) LIKE :criterio"
				+ " OR LOWER(COALESCE(p.nombres, '')) LIKE :criterio"
				+ " OR LOWER(COALESCE(p.apellidos, '')) LIKE :criterio)"
				+ (iglesiaId != null ? " AND i.id = :iglesiaId" : "")
				+ " ORDER BY p.nombres, p.apellidos, p.documento, ip.id";
		TypedQuery<IglesiaPersona> query = getEntityManager().createQuery(hql, IglesiaPersona.class);
		query.setParameter("criterio", "%" + criterio.trim().toLowerCase(java.util.Locale.ROOT) + "%");
		if (iglesiaId != null) {
			query.setParameter("iglesiaId", iglesiaId);
		}
		query.setMaxResults(limite);
		return query.getResultList();
	}

	/**
	 * Identifica en bloque las personas que conservan al menos un vínculo activo.
	 * Se usa al regularizar duplicidades para no deshabilitar una persona que aún
	 * mantiene una pertenencia válida.
	 */
	public java.util.Set<Integer> listarPersonasConRelacionesActivas(Collection<Integer> personaIds) {
		if (personaIds == null || personaIds.isEmpty()) {
			return java.util.Collections.emptySet();
		}
		String hql = "SELECT DISTINCT ip.persona.id" + " FROM IglesiaPersona ip" + " JOIN ip.persona p"
				+ " WHERE ip.estado = TRUE" + "   AND p.estado = TRUE" + "   AND ip.persona.id IN :personaIds";
		return new java.util.LinkedHashSet<>(getEntityManager().createQuery(hql, Integer.class)
				.setParameter("personaIds", personaIds).getResultList());
	}

	/**
	 * Cuenta iglesias activas distintas por documento en una sola consulta. El
	 * documento es la identidad funcional porque existen personas historicas
	 * duplicadas con distintos ids internos.
	 */
	public Map<String, Integer> contarIglesiasActivasPorDocumentos(Collection<String> documentos) {
		Map<String, Integer> resultado = new LinkedHashMap<>();
		if (documentos == null || documentos.isEmpty()) {
			return resultado;
		}
		String hql = "SELECT TRIM(p.documento), COUNT(DISTINCT i.id)" + " FROM IglesiaPersona ip" + " JOIN ip.iglesia i"
				+ " JOIN ip.persona p" + " WHERE ip.estado = TRUE" + "   AND p.estado = TRUE"
				+ "   AND TRIM(p.documento) IN :documentos" + " GROUP BY TRIM(p.documento)";
		List<Object[]> filas = getEntityManager().createQuery(hql, Object[].class)
				.setParameter("documentos", documentos).getResultList();
		for (Object[] fila : filas) {
			resultado.put((String) fila[0], ((Number) fila[1]).intValue());
		}
		return resultado;
	}

	/** Obtiene los documentos que actualmente pertenecen a mas de una iglesia. */
	public List<String> listarDocumentosConMultiplesIglesiasActivas() {
		String hql = "SELECT TRIM(p.documento)" + " FROM IglesiaPersona ip" + " JOIN ip.iglesia i"
				+ " JOIN ip.persona p" + " WHERE ip.estado = TRUE" + "   AND p.estado = TRUE"
				+ "   AND p.documento IS NOT NULL" + " GROUP BY TRIM(p.documento)" + " HAVING COUNT(DISTINCT i.id) > 1"
				+ " ORDER BY TRIM(p.documento)";
		return getEntityManager().createQuery(hql, String.class).getResultList();
	}

	/**
	 * Carga en bloque todas las relaciones de los documentos inconsistentes,
	 * incluyendo ubicacion completa. No filtra el estado de la relacion para que el
	 * reporte preserve tambien la trazabilidad historica.
	 */
	public List<IglesiaPersona> listarRelacionesPorDocumentos(Collection<String> documentos) {
		if (documentos == null || documentos.isEmpty()) {
			return new ArrayList<>();
		}
		Session session = getEntityManager().unwrap(Session.class);
		Filter filtro = session.getEnabledFilter(EntidadBase.FILTER_ACTIVE);
		if (filtro != null) {
			session.disableFilter(EntidadBase.FILTER_ACTIVE);
		}
		String hql = HQL + " JOIN FETCH ip.iglesia i" + " LEFT JOIN FETCH i.ubicacion parroquia"
				+ " LEFT JOIN FETCH parroquia.geograp canton" + " LEFT JOIN FETCH canton.geograp provincia"
				+ " JOIN FETCH ip.persona p" + " WHERE TRIM(p.documento) IN :documentos"
				+ " ORDER BY TRIM(p.documento), ip.estado DESC, i.nombre, ip.id";
		try {
			return session.createQuery(hql, IglesiaPersona.class).setParameter("documentos", documentos)
					.getResultList();
		} finally {
			if (filtro != null) {
				session.enableFilter(EntidadBase.FILTER_ACTIVE);
			}
		}
	}

	/** Fuerza las bajas pendientes antes de actualizar la relacion definitiva. */
	public void flushCambios() {
		getEntityManager().flush();
	}

	/**
	 * Devuelve el vínculo activo entre la iglesia y la persona indicadas, o
	 * {@code null} si no existe ninguno. Útil para garantizar idempotencia al
	 * crear el vínculo desde el flujo de asignación de admins.
	 */
	public IglesiaPersona findByIglesiaAndPersona(Integer iglesiaId, Integer personaId) {
		if (iglesiaId == null || personaId == null) {
			return null;
		}
		try {
			String sql = HQL + " WHERE ip.iglesia.id = :iglesiaId" + "   AND ip.persona.id = :personaId"
					+ "   AND ip.estado = TRUE" + " ORDER BY ip.id DESC";
			TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
			query.setParameter("iglesiaId", iglesiaId);
			query.setParameter("personaId", personaId);
			query.setMaxResults(1);
			List<IglesiaPersona> result = query.getResultList();
			return (result != null && !result.isEmpty()) ? result.get(0) : null;
		} catch (NoResultException e) {
			return null;
		}
	}

	public IglesiaPersona buscarPorCedulaPersona(String cedula) {
		try {
			String sql = HQL + " LEFT JOIN FETCH ip.persona p" + " WHERE p.documento=:cedula ORDER BY ip.id";
			TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
			query.setParameter("cedula", cedula);
			List<IglesiaPersona> result = query.getResultList();
			if (result.size() > 0) {
				return result.get(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}

}
