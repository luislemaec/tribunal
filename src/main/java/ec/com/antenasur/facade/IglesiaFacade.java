package ec.com.antenasur.facade;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.model.generic.AbstractFacade;
import lombok.extern.slf4j.Slf4j;

@Stateless
@Slf4j
public class IglesiaFacade extends AbstractFacade<Iglesia, Integer> {

    static final String HQL = " FROM Iglesia ig";
    /** HQL base con ubicación (parroquia) y cantón (padre de la parroquia) ya cargados. */
    static final String HQL_CON_CANTON =
            " FROM Iglesia ig"
            + " LEFT JOIN FETCH ig.ubicacion ub"
            + " LEFT JOIN FETCH ub.geograp canton";

    public IglesiaFacade() {
        super(Iglesia.class, Integer.class);
    }

    /**
     * Carga todas las iglesias activas con parroquia y cantón en un solo JOIN,
     * garantizando que {@code IglesiaDTO.fromEntity()} siempre tenga acceso
     * a {@code ubicacion.geograp} sin depender de lazy-load ni del @Filter activo.
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

    /** Carga una iglesia por id con parroquia y cantón ya inicializados (evita lazy-load posterior). */
    public Iglesia findConCanton(Integer id) {
        if (id == null) return null;
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
            String sql = HQL_CON_CANTON
                    + " WHERE ig.id IN :ids AND ig.estado=TRUE ORDER BY ig.id";
            TypedQuery<Iglesia> query = super.getEntityManager().createQuery(sql, Iglesia.class);
            query.setParameter("ids", listaIdIglesias);
            return query.getResultList();
        } catch (Exception e) {
            log.error("Error al obtener iglesias asignadas por ids", e);
            return Collections.emptyList();
        }
    }

    public List<Iglesia> obtieneIglesiasPorAsignarPorIds(List<Integer> listaIdIglesias, List<Integer> listaIdParroquias) {
        try {
            String sql = HQL_CON_CANTON
                    + " WHERE ig.id NOT IN :idsIglesias AND ub.id IN :idsParroquias AND ig.estado=TRUE ORDER BY ig.id";
            TypedQuery<Iglesia> query = super.getEntityManager().createQuery(sql, Iglesia.class);
            query.setParameter("idsIglesias", listaIdIglesias);
            query.setParameter("idsParroquias", listaIdParroquias);
            return query.getResultList();
        } catch (Exception e) {
            log.error("Error al obtener iglesias por asignar", e);
            return Collections.emptyList();
        }
    }

    public List<Iglesia> getIglesiasPorParroquias(List<Geograp> parroquias) {
        try {
            String sql = HQL_CON_CANTON
                    + " WHERE ub IN :parroquias ORDER BY ig.id";
            TypedQuery<Iglesia> query = super.getEntityManager().createQuery(sql, Iglesia.class);
            query.setParameter("parroquias", parroquias);
            return query.getResultList();
        } catch (Exception e) {
            log.error("Error al obtener iglesias por parroquias", e);
            return Collections.emptyList();
        }
    }

    /**
     * Genera el siguiente código genérico secuencial (0000000000001, 0000000000002…).
     *
     * Usa pg_advisory_xact_lock (vía JDBC directo) para garantizar exclusión mutua
     * dentro de la transacción activa: dos EJBs concurrentes no obtienen el mismo número.
     * El lock se libera automáticamente al commit/rollback de la transacción.
     *
     * La llamada se hace con doWork de Hibernate para evitar que Hibernate intente
     * mapear el tipo void de retorno de pg_advisory_xact_lock (JDBC type 1111).
     */
    @SuppressWarnings("unchecked")
    public String generarDocumentoGenerico() {
        // Lock transaccional exclusivo vía JDBC directo — evita race condition
        try {
            getEntityManager().unwrap(org.hibernate.Session.class)
                .doWork(conn -> {
                    try (java.sql.PreparedStatement ps =
                             conn.prepareStatement("SELECT pg_advisory_xact_lock(20250101)")) {
                        ps.execute();
                    }
                });
        } catch (Exception e) {
            log.warn("No se pudo adquirir advisory lock para RUC genérico", e);
        }

        javax.persistence.Query q = getEntityManager().createNativeQuery(
                "SELECT igl_documento FROM public.tb_iglesia"
                + " WHERE igl_documento LIKE '000000000000%'"
                + " ORDER BY igl_documento DESC LIMIT 1");
        List<Object> r = q.getResultList();
        if (r == null || r.isEmpty()) {
            return "0000000000001";
        }
        try {
            long ultimo = Long.parseLong(r.get(0).toString().trim());
            return String.format("%013d", ultimo + 1);
        } catch (NumberFormatException e) {
            log.error("RUC genérico no parseable: {}", r.get(0), e);
            return "0000000000001";
        }
    }

    /**
     * Cuenta iglesias cuya fecha de actividad (actualización o creación) cae
     * dentro del rango [{@code desde}, {@code hasta}].
     */
    public long countActualizadasEnRango(Date desde, Date hasta) {
        String hql = "SELECT COUNT(ig) FROM Iglesia ig"
                + " WHERE (ig.fechaActualiza BETWEEN :desde AND :hasta"
                + "   OR  (ig.fechaActualiza IS NULL AND ig.fechaCrea BETWEEN :desde AND :hasta))";
        TypedQuery<Long> q = getEntityManager().createQuery(hql, Long.class);
        q.setParameter("desde", desde);
        q.setParameter("hasta", hasta);
        Long resultado = q.getSingleResult();
        return resultado != null ? resultado : 0L;
    }

    public Iglesia getIglesiaPorNombreNombreComunidadYUbicacion(Iglesia iglesiaTmp) {
        try {
            String sql = HQL_CON_CANTON
                    + " WHERE ub = :ubicacion"
                    + "   AND UPPER(TRIM(ig.nombre)) = UPPER(TRIM(:nombreIglesia))"
                    + "   AND ((:nombreComunidad IS NULL AND ig.comunidad IS NULL)"
                    + "        OR UPPER(TRIM(ig.comunidad)) = UPPER(TRIM(:nombreComunidad)))"
                    + " ORDER BY ig.id";
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
