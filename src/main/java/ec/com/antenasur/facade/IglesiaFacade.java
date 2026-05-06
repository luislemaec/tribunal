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
    /** HQL base con parroquia, cantón y provincia (3 niveles) ya cargados eager. */
    static final String HQL_CON_CANTON =
            " FROM Iglesia ig"
            + " LEFT JOIN FETCH ig.ubicacion ub"
            + " LEFT JOIN FETCH ub.geograp canton"
            + " LEFT JOIN FETCH canton.geograp provincia";

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

    /** Nombre de la secuencia PostgreSQL que genera los códigos genéricos. */
    private static final String SEQ_CODIGO_GENERICO = "seq_iglesia_codigo_generico";

    /**
     * Genera el siguiente código genérico secuencial (13 dígitos zero-padded).
     *
     * Usa una secuencia PostgreSQL ({@value #SEQ_CODIGO_GENERICO}) atómica por diseño:
     * elimina race conditions sin advisory locks y garantiza que un valor nunca se reuse,
     * incluso si se elimina la última iglesia con código genérico.
     *
     * La secuencia se crea de forma idempotente en la primera invocación, alineada al
     * mayor código existente en {@code tb_iglesia} para no colisionar con datos previos.
     */
    public String generarDocumentoGenerico() {
        asegurarSecuenciaCodigoGenerico();
        Number proximo = (Number) getEntityManager()
                .createNativeQuery("SELECT nextval('" + SEQ_CODIGO_GENERICO + "')")
                .getSingleResult();
        return String.format("%013d", proximo.longValue());
    }

    /**
     * Crea la secuencia si no existe, alineada a {@code MAX(igl_documento) + 1} de
     * los códigos genéricos previos (cualquier documento de 13 dígitos numéricos
     * que empiece con "00" — los RUC reales ecuatorianos nunca empiezan con 00,
     * provincias 01-24).
     *
     * Idempotente: tras la primera ejecución, el bloque DO no hace nada.
     */
    private void asegurarSecuenciaCodigoGenerico() {
        try {
            String sql =
                "DO $$ "
              + "DECLARE max_val BIGINT; "
              + "BEGIN "
              + "  IF NOT EXISTS (SELECT 1 FROM pg_class "
              + "                 WHERE relkind='S' AND relname='" + SEQ_CODIGO_GENERICO + "') THEN "
              + "    SELECT COALESCE(MAX(CAST(igl_documento AS BIGINT)), 0) INTO max_val "
              + "    FROM public.tb_iglesia "
              + "    WHERE igl_documento ~ '^00[0-9]{11}$'; "
              + "    EXECUTE format('CREATE SEQUENCE " + SEQ_CODIGO_GENERICO + " START %s', max_val + 1); "
              + "  END IF; "
              + "END $$;";
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
