package ec.com.antenasur.domain.generic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.CriteriaBuilder;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.Filter;
import org.hibernate.Session;

/**
 * @author Luis Lema <lemaedu@gmail.com>
 */
public abstract class AbstractFacade<T, E> {

    @PersistenceContext(unitName = "tribunalPU")
    private EntityManager em;

    private Class<T> entityClass;
    private Class<E> primaryKeyClass;

    @Setter
    @Getter
    private List<Predicate> wherePredicates = new ArrayList<>();

    @Setter
    @Getter
    private List<Order> orderByPredicates = new ArrayList<>();

    @Setter
    @Getter
    private CriteriaBuilder criteriaBuilder;

    @Setter
    @Getter
    private CriteriaQuery<T> criteria;

    @Setter
    @Getter
    private Root<T> record;

    public AbstractFacade(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    public AbstractFacade(Class<T> entityClass, Class<E> primaryKeyClass) {
        this.entityClass = entityClass;
        this.primaryKeyClass = primaryKeyClass;
    }

    public void initialize() {
        this.criteriaBuilder = em.getCriteriaBuilder();
        this.criteria = criteriaBuilder.createQuery(entityClass);
        this.record = criteria.from(entityClass);
    }

    protected EntityManager getEntityManager() {
        enableFilters();
        return em;
    }

    /**
     * *********************HABILITA FILTRO*******************************
     */
    private void enableFilters() {
        Session session = resolveHibernateSession();
        Filter filter = session.getEnabledFilter(EntidadBase.FILTER_ACTIVE);
        if (filter == null) {
            filter = session.enableFilter(EntidadBase.FILTER_ACTIVE);
        }
    }

    private Session resolveHibernateSession() {
        Session session = em.unwrap(org.hibernate.Session.class);
        return session;
    }

    /**
     * **********************************************************************
     */
    public <T extends EntidadBase> T delete(T entidad) {
        entidad.setEstado(false);
        getEntityManager().merge(entidad);
        em.flush();
        return entidad;
    }

    public T create(T entity) {
        try {
            getEntityManager().persist(entity);
            em.flush();
            return entity;
        } catch (NoResultException e) {
            return null;
        }
    }

    public T edit(T entity) {
        try {
            getEntityManager().merge(entity);
            em.flush();
            return entity;
        } catch (NoResultException e) {
            return null;
        }
    }

    public void remove(T entity) {
        getEntityManager().remove(getEntityManager().merge(entity));
    }

    public T find(E id) {
        try {
            return getEntityManager().find(entityClass, id);
        } catch (NoResultException e) {
            return null;
        }

    }

    public List<T> findAll() {
        try {
            CriteriaQuery cq = getEntityManager().getCriteriaBuilder().createQuery();
            cq.select(cq.from(entityClass));
            return getEntityManager().createQuery(cq).getResultList();
        } catch (NoResultException e) {
            return null;
        }

    }

    public List<T> findRange(int[] range) {
        try {
            CriteriaQuery cq = getEntityManager().getCriteriaBuilder().createQuery();
            cq.select(cq.from(entityClass));
            javax.persistence.Query q = getEntityManager().createQuery(cq);
            q.setMaxResults(range[1] - range[0]);
            q.setFirstResult(range[0]);
            return q.getResultList();
        } catch (NoResultException e) {
            return null;
        }
    }

    public int count() {
        try {
            CriteriaQuery cq = getEntityManager().getCriteriaBuilder().createQuery();
            javax.persistence.criteria.Root<T> rt = cq.from(entityClass);
            cq.select(getEntityManager().getCriteriaBuilder().count(rt));
            javax.persistence.Query q = getEntityManager().createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } catch (NoResultException e) {
            return 0;
        }

    }

    /**
     *
     * <b> Ejecuta un namedQery con los parametros indicados en el mapa, en el
     * que la clave del mapa es el nombre del parametro, los parametro de
     * limites indican el rango del total de los registros que se necesitan.
     * </b>
     *
     * @author Luis Lema
     * @version Revision: 1.0
     * <p>
     * [Autor: llema, Fecha: Oct 28, 2014]
     * </p>
     * @param namedQueryName nombre del namedQuery
     * @param parameters parametros del query
     * @param limiteInicio rango de inicio
     * @param limiteFin rango de fin
     * @return resultado de la consulta
     */
    @SuppressWarnings("unchecked")
    public List<T> findByCreateQueryPaginado(final String query_, final Map<String, Object> parameters,
            int limiteInicio, int limiteFin) {
        try {
            Query query = getEntityManager().createQuery(query_).setFirstResult(limiteInicio).setMaxResults(limiteFin);
            if (parameters != null) {
                Set<Entry<String, Object>> parameterSet = parameters.entrySet();
                for (Entry<String, Object> entry : parameterSet) {
                    query.setParameter(entry.getKey(), entry.getValue());
                }
            }
            return query.getResultList();
        } catch (NoResultException e) {
            return null;
        }
    }

    public Query buildQuery() {
        getCriteria().select(getRecord())
                .where((Predicate[]) getWherePredicates().toArray(new Predicate[getWherePredicates().size()]))
                .orderBy((Order[]) getOrderByPredicates().toArray(new Order[getOrderByPredicates().size()]));
        return em.createQuery(criteria);
    }

}
