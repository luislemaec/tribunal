/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.facade;

import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.generic.AbstractFacade;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class GeograpFacade extends AbstractFacade<Geograp, Integer> {

    public GeograpFacade() {
        super(Geograp.class, Integer.class);
    }

    public List<Geograp> findByFatherId(Integer idFather) {
        try {
            String sql = "FROM Geograp m WHERE m.geograp.id=:idFather and m.status=true";
            Query query = super.getEntityManager().createQuery(sql);
            query.setParameter("idFather", idFather);
            List<Geograp> resultList = query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList;
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;
    }

    public Geograp findByFather_Id(Integer idFather) {
        try {
            String sql = "FROM Geograp m WHERE m.geograp.id=:idFather and m.status=true";
            //Query query = super.getEntityManager().createQuery(sql);
            TypedQuery<Geograp> query = super.getEntityManager().createQuery(sql, Geograp.class);
            query.setParameter("idFather", idFather);
            List<Geograp> resultList = query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList.get(0);
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;
    }

    public Geograp findByGeograpName(String nameGeograp) {
        try {
            String sql = "FROM Geograp m WHERE m.name=:nameGeograp and m.status=true";
            TypedQuery<Geograp> query = super.getEntityManager().createQuery(sql, Geograp.class);
            query.setParameter("nameGeograp", nameGeograp);
            List<Geograp> resultList = query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList.get(0);
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;
    }

    public Geograp findByFatherIdAndGeographName(Integer idFather, String nameGeograp) {
        try {
            String sql = "FROM Geograp m WHERE m.geograp.id=:idFather AND m.name LIKE '%'||:nameGeograp||'%' and m.status=true";
            
            TypedQuery<Geograp> query = super.getEntityManager().createQuery(sql, Geograp.class);
            query.setParameter("idFather", idFather);
            query.setParameter("nameGeograp", nameGeograp);
            List<Geograp> resultList = query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList.get(0);
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;
    }

    public List<Geograp> findByFatherGeograp(Geograp geograp) {
        try {
            String sql = "FROM Geograp m WHERE m.geograp=:geograp order by m.name ";
            TypedQuery<Geograp> query = super.getEntityManager().createQuery(sql, Geograp.class);
            query.setParameter("geograp", geograp);
            List<Geograp> resultList = query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList;
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;
    }

    /**
     * Devuelve el Geograp padre (cantón) de la parroquia indicada consultando
     * directamente la columna {@code gelo_parent_id} vía SQL nativo.
     * Se usa SQL nativo para evitar que el {@code @Filter} activo de Hibernate
     * interfiera con la navegación por la relación {@code @ManyToOne}.
     */
    @SuppressWarnings("unchecked")
    public Geograp findParentOf(Integer childId) {
        if (childId == null) return null;
        try {
            Query q = getEntityManager().createNativeQuery(
                    "SELECT gelo_parent_id FROM public.tb_geograp WHERE gelo_id = :id");
            q.setParameter("id", childId);
            Object parentId = q.getSingleResult();
            if (parentId == null) return null;
            return find(((Number) parentId).intValue());
        } catch (NoResultException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
