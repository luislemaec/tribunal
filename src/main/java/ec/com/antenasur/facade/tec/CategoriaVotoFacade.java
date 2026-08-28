package ec.com.antenasur.facade.tec;

import jakarta.ejb.Stateless;

import ec.com.antenasur.model.tec.CategoriaVoto;
import ec.com.antenasur.model.generic.AbstractFacade;
import java.util.List;
import jakarta.persistence.TypedQuery;
import org.hibernate.Filter;
import org.hibernate.Session;

import ec.com.antenasur.model.generic.EntidadBase;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class CategoriaVotoFacade extends AbstractFacade<CategoriaVoto, Integer> {

    public CategoriaVotoFacade() {
        super(CategoriaVoto.class, Integer.class);
    }

    public List<CategoriaVoto> getCategoriasOrdenados() {
        return getCategoriasOrdenados(null);
    }

    /** Retorna especiales globales y categorías de listas exclusivas del proceso. */
    public List<CategoriaVoto> getCategoriasOrdenados(Integer procesoId) {
        String hql = "SELECT gc FROM CategoriaVoto gc"
                + " LEFT JOIN FETCH gc.lista l"
                + " LEFT JOIN FETCH gc.proceso p"
                + " WHERE gc.estado = TRUE AND ((gc.tipo = 'ESPECIAL' AND l IS NULL AND p IS NULL)";
        if (procesoId != null) {
            hql += " OR (gc.tipo = 'LISTA' AND p.id = :procesoId AND l.estado = TRUE)";
        }
        hql += ") ORDER BY CASE WHEN gc.tipo = 'LISTA' THEN 0 ELSE 1 END, gc.orden, gc.id";
        TypedQuery<CategoriaVoto> query = getEntityManager().createQuery(hql, CategoriaVoto.class);
        if (procesoId != null) {
            query.setParameter("procesoId", procesoId);
        }
        return query.getResultList();
    }

    public List<CategoriaVoto> buscarPorListaProcesoIncluyendoInactivas(
            Integer listaId, Integer procesoId) {
        if (listaId == null || procesoId == null) {
            return java.util.Collections.emptyList();
        }
        return consultarSinFiltroActivo(
                "SELECT c FROM CategoriaVoto c LEFT JOIN FETCH c.lista l"
                + " LEFT JOIN FETCH c.proceso p"
                + " WHERE l.id = :listaId AND p.id = :procesoId"
                + " AND c.tipo = 'LISTA'"
                + " ORDER BY CASE WHEN EXISTS (SELECT e.id FROM Escrutinio e"
                + " WHERE e.categoria.id = c.id) THEN 0 ELSE 1 END, c.estado DESC, c.id",
                listaId, procesoId);
    }

    public List<CategoriaVoto> buscarPorListaIncluyendoInactivas(Integer listaId) {
        if (listaId == null) {
            return java.util.Collections.emptyList();
        }
        Session session = getEntityManager().unwrap(Session.class);
        Filter filtro = session.getEnabledFilter(EntidadBase.FILTER_ACTIVE);
        if (filtro != null) {
            session.disableFilter(EntidadBase.FILTER_ACTIVE);
        }
        try {
            TypedQuery<CategoriaVoto> query = session.createQuery(
                    "SELECT c FROM CategoriaVoto c LEFT JOIN FETCH c.lista l"
                    + " LEFT JOIN FETCH c.proceso p WHERE l.id = :listaId"
                    + " AND c.tipo = 'LISTA' ORDER BY c.id", CategoriaVoto.class);
            query.setParameter("listaId", listaId);
            return query.getResultList();
        } finally {
            if (filtro != null) {
                session.enableFilter(EntidadBase.FILTER_ACTIVE);
            }
        }
    }

    public long contarActivasPorListaProceso(Integer listaId, Integer procesoId) {
        if (listaId == null || procesoId == null) {
            return 0L;
        }
        Session session = getEntityManager().unwrap(Session.class);
        Filter filtro = session.getEnabledFilter(EntidadBase.FILTER_ACTIVE);
        if (filtro != null) {
            session.disableFilter(EntidadBase.FILTER_ACTIVE);
        }
        try {
            TypedQuery<Long> query = session.createQuery(
                    "SELECT COUNT(c) FROM CategoriaVoto c"
                    + " WHERE c.lista.id = :listaId AND c.proceso.id = :procesoId"
                    + " AND c.tipo = 'LISTA' AND c.estado = TRUE", Long.class);
            query.setParameter("listaId", listaId);
            query.setParameter("procesoId", procesoId);
            return query.getSingleResult();
        } finally {
            if (filtro != null) {
                session.enableFilter(EntidadBase.FILTER_ACTIVE);
            }
        }
    }

    public long contarActivasPorLista(Integer listaId) {
        if (listaId == null) {
            return 0L;
        }
        Session session = getEntityManager().unwrap(Session.class);
        Filter filtro = session.getEnabledFilter(EntidadBase.FILTER_ACTIVE);
        if (filtro != null) {
            session.disableFilter(EntidadBase.FILTER_ACTIVE);
        }
        try {
            TypedQuery<Long> query = session.createQuery(
                    "SELECT COUNT(c) FROM CategoriaVoto c"
                    + " WHERE c.lista.id = :listaId AND c.tipo = 'LISTA' AND c.estado = TRUE",
                    Long.class);
            query.setParameter("listaId", listaId);
            return query.getSingleResult();
        } finally {
            if (filtro != null) {
                session.enableFilter(EntidadBase.FILTER_ACTIVE);
            }
        }
    }

    private List<CategoriaVoto> consultarSinFiltroActivo(String hql, Integer listaId, Integer procesoId) {
        Session session = getEntityManager().unwrap(Session.class);
        Filter filtro = session.getEnabledFilter(EntidadBase.FILTER_ACTIVE);
        if (filtro != null) {
            session.disableFilter(EntidadBase.FILTER_ACTIVE);
        }
        try {
            TypedQuery<CategoriaVoto> query = session.createQuery(hql, CategoriaVoto.class);
            query.setParameter("listaId", listaId);
            query.setParameter("procesoId", procesoId);
            return query.getResultList();
        } finally {
            if (filtro != null) {
                session.enableFilter(EntidadBase.FILTER_ACTIVE);
            }
        }
    }

}
