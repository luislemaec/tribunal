package ec.com.antenasur.model.generic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.Filter;
import org.hibernate.Session;
import org.hibernate.envers.Audited;

import ec.com.antenasur.model.tec.Proceso;
import ec.com.antenasur.model.IglesiaPersona;

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
        T persistida = getEntityManager().merge(entidad);
        em.flush();
        registrarActividadPersistencia(persistida, "DESACTIVA");
        return entidad;
    }

    public T create(T entity) {
        try {
            getEntityManager().persist(entity);
            em.flush();
            registrarActividadPersistencia(entity, "CREA");
            return entity;
        } catch (NoResultException e) {
            return null;
        }
    }

    public T edit(T entity) {
        try {
            T persistida = getEntityManager().merge(entity);
            em.flush();
            registrarActividadPersistencia(persistida, "ACTUALIZA");
            return entity;
        } catch (NoResultException e) {
            return null;
        }
    }

    public void remove(T entity) {
        getEntityManager().remove(getEntityManager().merge(entity));
        registrarActividadPersistencia(entity, "ELIMINA");
    }

    /**
     * Complementa Envers con una entrada funcional consultable en
     * {@code tec.procesos}. Solo se registra para entidades versionadas por
     * Envers: no duplica consultas, no guarda el estado completo de la entidad
     * ni incluye credenciales, tokens, rutas de archivos o valores sensibles.
     * El detalle de columnas continúa en las tablas {@code *_aud} de Envers.
     */
    private void registrarActividadPersistencia(Object entity, String accion) {
        if (!(entity instanceof EntidadBase)
                || entity instanceof Proceso
                || !entity.getClass().isAnnotationPresent(Audited.class)) {
            return;
        }

        Proceso actividad = new Proceso();
        String entidad = entity.getClass().getSimpleName();
        String descripcion = accion + " | MÓDULO: " + resolverModulo(entidad)
                + "; ENTIDAD: " + entidad + "; REGISTRO: " + identificarRegistro(entity);
        actividad.setActividad(descripcion.substring(0, Math.min(descripcion.length(), 255)));
        getEntityManager().persist(actividad);
    }

    private String resolverModulo(String entidad) {
        return switch (entidad) {
            case "Usuario", "Rol", "RolUsuario", "Menu", "MenuRol" -> "SEGURIDAD";
            case "Persona", "Iglesia", "IglesiaPersona" -> "PERSONAS E IGLESIAS";
            case "ProcesoElectoral", "Periodo", "CronogramaFase" -> "PROCESO ELECTORAL";
            case "Recinto", "Mesa" -> "RECINTOS Y MESAS";
            case "Padron" -> "PADRÓN";
            case "MiembroJRV", "Cargo" -> "JRV";
            case "Escrutinio", "EscrutinioCabecera", "CategoriaVoto" -> "ESCRUTINIO";
            case "Documentos", "PlantillaCorreo", "Correo" -> "DOCUMENTOS";
            case "Lista", "Candidato", "Tribunal" -> "POSTULACIONES";
            default -> "CONFIGURACIÓN";
        };
    }

    private String identificarRegistro(Object entity) {
        if (entity instanceof IglesiaPersona relacion) {
            String persona = relacion.getPersona() != null
                    ? obtenerEtiquetaSegura(relacion.getPersona(), "getDocumento", "getNombres") : null;
            String iglesia = relacion.getIglesia() != null
                    ? obtenerEtiquetaSegura(relacion.getIglesia(), "getNombre", "getCodigo") : null;
            if (persona != null || iglesia != null) {
                return "Persona " + (persona != null ? persona : "sin identificar")
                        + "; Iglesia " + (iglesia != null ? iglesia : "sin identificar");
            }
        }
        String etiqueta = obtenerEtiquetaSegura(entity, "getUsername", "getNombre", "getCodigo", "getCedula", "getNumero");
        String id = entity instanceof EntidadBase base && base.getId() != null
                ? String.valueOf(base.getId()) : "sin identificador";
        return etiqueta == null || etiqueta.isBlank() ? "ID " + id : etiqueta + " (ID " + id + ")";
    }

    private String obtenerEtiquetaSegura(Object entity, String... metodos) {
        for (String metodo : metodos) {
            try {
                Object valor = entity.getClass().getMethod(metodo).invoke(entity);
                if (valor != null && !valor.toString().isBlank()) {
                    return valor.toString().replaceAll("[\\r\\n\\t]", " ").substring(0,
                            Math.min(valor.toString().length(), 160));
                }
            } catch (ReflectiveOperationException ignored) {
                // La entidad no expone esa etiqueta; se usará su identificador.
            }
        }
        return null;
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
            jakarta.persistence.Query q = getEntityManager().createQuery(cq);
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
            jakarta.persistence.criteria.Root<T> rt = cq.from(entityClass);
            cq.select(getEntityManager().getCriteriaBuilder().count(rt));
            jakarta.persistence.Query q = getEntityManager().createQuery(cq);
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
