package ec.com.antenasur.facade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import ec.com.antenasur.dto.GeograpDTO;
import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.model.generic.AbstractFacade;
import lombok.extern.slf4j.Slf4j;

@Stateless
@Slf4j
public class IglesiaFacade extends AbstractFacade<Iglesia, Integer> {

	private static final String ROL_IGLESIA_ADMIN = "%IglesiaAdmin";

	static final String HQL = " SELECT ig FROM Iglesia ig";
	/**
	 * HQL base con parroquia, cantón y provincia (3 niveles) ya cargados eager.
	 */
	static final String HQL_CON_CANTON = " SELECT ig FROM Iglesia ig" + " LEFT JOIN FETCH ig.ubicacion ub"
			+ " LEFT JOIN FETCH ub.geograp canton" + " LEFT JOIN FETCH canton.geograp provincia";

	public IglesiaFacade() {
		super(Iglesia.class, Integer.class);
	}

	/**
	 * Carga todas las iglesias activas con parroquia y cantón en un solo JOIN,
	 * garantizando que {@code IglesiaDTO.fromEntity()} siempre tenga acceso a
	 * {@code ubicacion.geograp} sin depender de lazy-load ni del @Filter activo.
	 */
	@Override
	public List<Iglesia> findAll() {
		try {
			String sql = HQL_CON_CANTON + " ORDER BY ig.id";
			TypedQuery<Iglesia> query = super.getEntityManager().createQuery(sql, Iglesia.class);
			return query.getResultList();
		} catch (Exception e) {
			log.error("Error al obtener todas las iglesias", e);
			return Collections.emptyList();
		}
	}

	public List<Iglesia> getIglesiasPorParroquia(Geograp parroquia) {
		try {
			String sql = HQL_CON_CANTON + " WHERE ig.ubicacion = :parroquia ORDER BY ig.id";
			TypedQuery<Iglesia> query = super.getEntityManager().createQuery(sql, Iglesia.class);
			query.setParameter("parroquia", parroquia);
			return query.getResultList();
		} catch (NoResultException e) {
			return Collections.emptyList();
		} catch (Exception e) {
			log.error("Error al obtener iglesias por parroquia id={}", parroquia != null ? parroquia.getId() : null, e);
			return Collections.emptyList();
		}
	}

	/**
	 * Carga una iglesia por id con parroquia y cantón ya inicializados (evita
	 * lazy-load posterior).
	 */
	public Iglesia findConCanton(Integer id) {
		if (id == null)
			return null;
		try {
			String sql = HQL_CON_CANTON + " WHERE ig.id = :id";
			TypedQuery<Iglesia> query = super.getEntityManager().createQuery(sql, Iglesia.class);
			query.setParameter("id", id);
			List<Iglesia> result = query.getResultList();
			return result.isEmpty() ? null : result.get(0);
		} catch (Exception e) {
			log.error("Error al cargar iglesia con canton id={}", id, e);
			return null;
		}
	}

	public List<Iglesia> listarParaAsignacionFiltrada(Integer provinciaId, Integer cantonId, Integer parroquiaId,
			Boolean conAdmin) {
		StringBuilder hql = new StringBuilder(HQL_CON_CANTON).append(" WHERE ig.estado = TRUE");
		agregarFiltrosAsignacion(hql, provinciaId, cantonId, parroquiaId, conAdmin);
		hql.append(" ORDER BY provincia.name, canton.name, ub.name, ig.nombre, ig.id");

		TypedQuery<Iglesia> query = getEntityManager().createQuery(hql.toString(), Iglesia.class);
		parametrizarFiltrosAsignacion(query, provinciaId, cantonId, parroquiaId, conAdmin);
		return query.getResultList();
	}

	public long contarParaAsignacionFiltrada(Integer provinciaId, Integer cantonId, Integer parroquiaId,
			Boolean conAdmin) {
		StringBuilder hql = new StringBuilder("SELECT COUNT(ig) FROM Iglesia ig").append(" LEFT JOIN ig.ubicacion ub")
				.append(" LEFT JOIN ub.geograp canton").append(" LEFT JOIN canton.geograp provincia")
				.append(" WHERE ig.estado = TRUE");
		agregarFiltrosAsignacion(hql, provinciaId, cantonId, parroquiaId, conAdmin);

		TypedQuery<Long> query = getEntityManager().createQuery(hql.toString(), Long.class);
		parametrizarFiltrosAsignacion(query, provinciaId, cantonId, parroquiaId, conAdmin);
		Long total = query.getSingleResult();
		return total != null ? total : 0L;
	}

	/**
	 * Provincias donde existe al menos una iglesia activa. Se resuelve con un
	 * {@code DISTINCT} en BD sobre la cadena parroquia → cantón → provincia de
	 * {@code Iglesia.ubicacion}: no se carga el catálogo geográfico nacional.
	 */
	public List<GeograpDTO> listarProvinciasConIglesias() {
		return listarUbicacionesConIglesias("provincia", null, null);
	}

	/** Cantones de la provincia indicada donde existe al menos una iglesia activa. */
	public List<GeograpDTO> listarCantonesConIglesias(Integer provinciaId) {
		return provinciaId == null ? new ArrayList<>() : listarUbicacionesConIglesias("canton", "provincia", provinciaId);
	}

	/** Parroquias del cantón indicado donde existe al menos una iglesia activa. */
	public List<GeograpDTO> listarParroquiasConIglesias(Integer cantonId) {
		return cantonId == null ? new ArrayList<>() : listarUbicacionesConIglesias("ub", "canton", cantonId);
	}

	/**
	 * Devuelve solo id y nombre del nivel pedido. No filtra por el estado del catálogo
	 * ({@code gelo_status}): una división marcada inactiva que aún tiene iglesias activas
	 * se muestra para no ocultar esas iglesias. {@code nivel} y {@code padre} son alias
	 * internos de la consulta, nunca valores del usuario.
	 */
	private List<GeograpDTO> listarUbicacionesConIglesias(String nivel, String padre, Integer padreId) {
		StringBuilder hql = new StringBuilder("SELECT DISTINCT ").append(nivel).append(".id, ").append(nivel)
				.append(".name FROM Iglesia ig JOIN ig.ubicacion ub JOIN ub.geograp canton")
				.append(" JOIN canton.geograp provincia WHERE ig.estado = TRUE");
		if (padre != null) {
			hql.append(" AND ").append(padre).append(".id = :padreId");
		}
		hql.append(" ORDER BY ").append(nivel).append(".name");
		TypedQuery<Object[]> query = getEntityManager().createQuery(hql.toString(), Object[].class);
		if (padre != null) {
			query.setParameter("padreId", padreId);
		}
		List<GeograpDTO> resultado = new ArrayList<>();
		for (Object[] fila : query.getResultList()) {
			GeograpDTO ubicacion = new GeograpDTO();
			ubicacion.setId((Integer) fila[0]);
			ubicacion.setName((String) fila[1]);
			ubicacion.setPadreId(padreId);
			resultado.add(ubicacion);
		}
		return resultado;
	}

	private void agregarFiltrosAsignacion(StringBuilder hql, Integer provinciaId, Integer cantonId, Integer parroquiaId,
			Boolean conAdmin) {
		if (provinciaId != null) {
			hql.append(" AND provincia.id = :provinciaId");
		}
		if (cantonId != null) {
			hql.append(" AND canton.id = :cantonId");
		}
		if (parroquiaId != null) {
			hql.append(" AND ub.id = :parroquiaId");
		}
		if (conAdmin != null) {
			hql.append(Boolean.TRUE.equals(conAdmin) ? " AND EXISTS (" : " AND NOT EXISTS (")
					.append("SELECT ru.id FROM RolUsuario ru").append(" WHERE ru.usuario.iglesia = ig")
					.append(" AND ru.usuario.estado = TRUE").append(" AND ru.estado = TRUE")
					.append(" AND ru.rol.estado = TRUE").append(" AND ru.rol.nombre LIKE :rolIglesiaAdmin)");
		}
	}

	private void parametrizarFiltrosAsignacion(jakarta.persistence.Query query, Integer provinciaId, Integer cantonId,
			Integer parroquiaId, Boolean conAdmin) {
		if (provinciaId != null) {
			query.setParameter("provinciaId", provinciaId);
		}
		if (cantonId != null) {
			query.setParameter("cantonId", cantonId);
		}
		if (parroquiaId != null) {
			query.setParameter("parroquiaId", parroquiaId);
		}
		if (conAdmin != null) {
			query.setParameter("rolIglesiaAdmin", ROL_IGLESIA_ADMIN);
		}
	}

	/**
	 * Bloquea la iglesia mientras se valida y persiste su administrador. Evita que
	 * dos transacciones asignen simultáneamente IglesiaAdmin a la misma iglesia.
	 */
	public Iglesia findForAdminAssignment(Integer id) {
		if (id == null) {
			return null;
		}
		return super.getEntityManager().find(Iglesia.class, id, LockModeType.PESSIMISTIC_WRITE);
	}

	/**
	 * Busca por documento sin importar el estado. SQL nativo para no quedar
	 * oculto por el filtro de activos: uk_iglesia_documento también cubre a las
	 * iglesias eliminadas.
	 *
	 * @return {id, nombre, estado} o null si no existe
	 */
	public Object[] getIglesiaPorDocumentoCualquierEstado(String documento) {
		try {
			List<?> result = super.getEntityManager()
					.createNativeQuery("SELECT igl_id, igl_nombre, estado FROM public.tb_iglesia"
							+ " WHERE igl_documento = :documento ORDER BY igl_id")
					.setParameter("documento", documento).setMaxResults(1).getResultList();
			return result.isEmpty() ? null : (Object[]) result.get(0);
		} catch (Exception e) {
			log.error("Error al buscar iglesia por documento (cualquier estado)", e);
			return null;
		}
	}

	public Iglesia getIglesiaPorDocumento(String documento) {
		try {
			String sql = HQL + " WHERE documento =:documento AND estado =TRUE ORDER BY id";
			TypedQuery<Iglesia> query = super.getEntityManager().createQuery(sql, Iglesia.class);
			query.setParameter("documento", documento);
			List<Iglesia> result = query.getResultList();
			return result.isEmpty() ? null : result.get(0);
		} catch (Exception e) {
			log.error("Error al buscar iglesia por documento", e);
			return null;
		}
	}

	public List<Iglesia> obtieneIglesiasAsignadasPorIds(List<Integer> listaIdIglesias) {
		try {
			String sql = HQL_CON_CANTON + " WHERE ig.id IN :ids AND ig.estado=TRUE ORDER BY ig.id";
			TypedQuery<Iglesia> query = super.getEntityManager().createQuery(sql, Iglesia.class);
			query.setParameter("ids", listaIdIglesias);
			return query.getResultList();
		} catch (Exception e) {
			log.error("Error al obtener iglesias asignadas por ids", e);
			return Collections.emptyList();
		}
	}

	public List<Iglesia> obtieneIglesiasPorAsignarPorIds(List<Integer> listaIdIglesias,
			List<Integer> listaIdParroquias) {
		if (listaIdParroquias == null || listaIdParroquias.isEmpty()) {
			return Collections.emptyList();
		}
		try {
			String sql = HQL_CON_CANTON + " WHERE ub.id IN :idsParroquias AND ig.estado=TRUE";
			boolean excluirAsignadas = listaIdIglesias != null && !listaIdIglesias.isEmpty();
			if (excluirAsignadas) {
				sql += " AND ig.id NOT IN :idsIglesias";
			}
			sql += " ORDER BY ig.id";
			TypedQuery<Iglesia> query = super.getEntityManager().createQuery(sql, Iglesia.class);
			if (excluirAsignadas) {
				query.setParameter("idsIglesias", listaIdIglesias);
			}
			query.setParameter("idsParroquias", listaIdParroquias);
			return query.getResultList();
		} catch (Exception e) {
			log.error("Error al obtener iglesias por asignar", e);
			return Collections.emptyList();
		}
	}

	public List<Iglesia> getIglesiasPorParroquias(List<Geograp> parroquias) {
		try {
			String sql = HQL_CON_CANTON + " WHERE ub IN :parroquias ORDER BY ig.id";
			TypedQuery<Iglesia> query = super.getEntityManager().createQuery(sql, Iglesia.class);
			query.setParameter("parroquias", parroquias);
			return query.getResultList();
		} catch (Exception e) {
			log.error("Error al obtener iglesias por parroquias", e);
			return Collections.emptyList();
		}
	}

	/** Nombre de la secuencia PostgreSQL que genera los códigos genéricos. */
	private static final String SEQ_CODIGO_GENERICO = "seq_iglesia_codigo_generico";

	/**
	 * Genera el siguiente código genérico secuencial (13 dígitos
	 * zero-padded).
	 *
	 * Usa una secuencia PostgreSQL ({@value #SEQ_CODIGO_GENERICO}) atómica por
	 * diseño: elimina race conditions sin advisory locks y garantiza que un
	 * valor nunca se reuse, incluso si se elimina la última iglesia con código
	 * genérico.
	 *
	 * La secuencia se crea de forma idempotente en la primera invocación,
	 * alineada al mayor código existente en {@code tb_iglesia} para no
	 * colisionar con datos previos.
	 */
	public String generarDocumentoGenerico() {
		asegurarSecuenciaCodigoGenerico();
		Number proximo = (Number) getEntityManager().createNativeQuery("SELECT nextval('" + SEQ_CODIGO_GENERICO + "')")
				.getSingleResult();
		return String.format("%013d", proximo.longValue());
	}

	/**
	 * Crea la secuencia si no existe, alineada a {@code MAX(igl_documento) + 1} de
	 * los códigos genéricos previos (cualquier documento de 13 dígitos
	 * numéricos que empiece con "00" — los RUC reales ecuatorianos nunca
	 * empiezan con 00, provincias 01-24).
	 *
	 * Idempotente: tras la primera ejecución, el bloque DO no hace nada.
	 */
	private void asegurarSecuenciaCodigoGenerico() {
		try {
			String sql = "DO $$ " + "DECLARE max_val BIGINT; " + "BEGIN " + "  IF NOT EXISTS (SELECT 1 FROM pg_class "
					+ "                 WHERE relkind='S' AND relname='" + SEQ_CODIGO_GENERICO + "') THEN "
					+ "    SELECT COALESCE(MAX(CAST(igl_documento AS BIGINT)), 0) INTO max_val "
					+ "    FROM public.tb_iglesia " + "    WHERE igl_documento ~ '^00[0-9]{11}$'; "
					+ "    EXECUTE format('CREATE SEQUENCE " + SEQ_CODIGO_GENERICO + " START %s', max_val + 1); "
					+ "  END IF; " + "END $$;";
			getEntityManager().createNativeQuery(sql).executeUpdate();
		} catch (Exception e) {
			log.error("No se pudo asegurar la existencia de la secuencia " + SEQ_CODIGO_GENERICO, e);
			throw e;
		}
	}

	/**
	 * Cuenta iglesias cuya fecha de actividad (actualización o creación) cae
	 * dentro del rango [{@code desde}, {@code hasta}].
	 */
	public long countActualizadasEnRango(Date desde, Date hasta) {
		String hql = "SELECT COUNT(ig) FROM Iglesia ig" + " WHERE (ig.fechaActualiza BETWEEN :desde AND :hasta"
				+ "   OR  (ig.fechaActualiza IS NULL AND ig.fechaCrea BETWEEN :desde AND :hasta))";
		TypedQuery<Long> q = getEntityManager().createQuery(hql, Long.class);
		q.setParameter("desde", desde);
		q.setParameter("hasta", hasta);
		Long resultado = q.getSingleResult();
		return resultado != null ? resultado : 0L;
	}

	/**
	 * Revisiones Envers de una iglesia (tabla {@code tb_iglesia_aud} unida a
	 * {@code tec.tec_auditoria}) en orden cronológico, en una sola consulta. Consulta
	 * nativa para incluir bajas y no depender del filtro de estado. Columnas:
	 * rev, revtype, fecha, nombre, comunidad, documento, parroquia, estado,
	 * usuario, cantón y provincia.
	 */
	@SuppressWarnings("unchecked")
	public List<Object[]> listarRevisionesAuditoria(Integer iglesiaId) {
		if (iglesiaId == null) {
			return Collections.emptyList();
		}
		// La fecha y el usuario de cada revisión viven en la entidad de revisión
		// (tec.tec_auditoria, poblada por el listener de auditoría). Las columnas
		// f_crea/f_actualiza/u_crea/u_actualiza de la tabla _AUD son herencia del
		// baseline y Envers nunca las escribe, por lo que no se consultan.
		// El cantón y la provincia no se auditan: se derivan del árbol geográfico
		// vigente a partir de la parroquia registrada en cada revisión.
		String sql = "SELECT a.rev, a.revtype, COALESCE(r.audit_date, r.create_date),"
				+ " a.igl_nombre, a.igl_comunidad_barrio, a.igl_documento, g.gelo_name,"
				+ " a.estado, COALESCE(r.update_user, r.create_user), gc.gelo_name, gp.gelo_name"
				+ " FROM public.tb_iglesia_aud a"
				+ " LEFT JOIN tec.tec_auditoria r ON r.aud_id = a.rev"
				+ " LEFT JOIN public.tb_geograp g ON g.gelo_id = a.gelo_id"
				+ " LEFT JOIN public.tb_geograp gc ON gc.gelo_id = g.gelo_parent_id"
				+ " LEFT JOIN public.tb_geograp gp ON gp.gelo_id = gc.gelo_parent_id"
				+ " WHERE a.igl_id = :id ORDER BY a.rev";
		return getEntityManager().createNativeQuery(sql).setParameter("id", iglesiaId).getResultList();
	}

	public Iglesia getIglesiaPorNombreNombreComunidadYUbicacion(Iglesia iglesiaTmp) {
		try {
			String sql = HQL_CON_CANTON + " WHERE ub = :ubicacion"
					+ "   AND UPPER(TRIM(ig.nombre)) = UPPER(TRIM(:nombreIglesia))"
					+ "   AND ((:nombreComunidad IS NULL AND ig.comunidad IS NULL)"
					+ "        OR UPPER(TRIM(ig.comunidad)) = UPPER(TRIM(:nombreComunidad)))" + " ORDER BY ig.id";
			TypedQuery<Iglesia> query = super.getEntityManager().createQuery(sql, Iglesia.class);
			query.setParameter("ubicacion", iglesiaTmp.getUbicacion());
			query.setParameter("nombreIglesia", iglesiaTmp.getNombre());
			query.setParameter("nombreComunidad", iglesiaTmp.getComunidad());
			List<Iglesia> result = query.getResultList();
			return result.isEmpty() ? null : result.get(0);
		} catch (Exception e) {
			log.error("Error al buscar iglesia por nombre/comunidad/ubicación", e);
			return null;
		}
	}
}
