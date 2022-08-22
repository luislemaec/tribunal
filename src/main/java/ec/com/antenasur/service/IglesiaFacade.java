/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.service;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import ec.com.antenasur.domain.Geograp;
import ec.com.antenasur.domain.Iglesia;
import ec.com.antenasur.domain.generic.AbstractFacade;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class IglesiaFacade extends AbstractFacade<Iglesia, Integer> {

    static final String HQL = " FROM Iglesia ig";

    public IglesiaFacade() {
        super(Iglesia.class, Integer.class);
    }

    public List<Iglesia> getIglesiasPorParroquia(Geograp parroquia) {
        try {
            String sql = HQL + " WHERE ig.ubicacion in :parroquia ORDER BY ig.id";
            TypedQuery<Iglesia> query = super.getEntityManager().createQuery(sql, Iglesia.class);
            query.setParameter("parroquia", parroquia);
            List<Iglesia> result = query.getResultList();
            if (result.size() > 0) {
                return result;
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;
    }


    public Iglesia getIglesiaPorDocumento(String documento) {
        try {
            String sql = HQL + " WHERE documento =:documento AND estado =TRUE ORDER BY id";
            TypedQuery<Iglesia> query = super.getEntityManager().createQuery(sql, Iglesia.class);
            query.setParameter("documento", documento);
            List<Iglesia> result = query.getResultList();
            if (result.size() > 0) {
                return result.get(0);
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;
    }

    public List<Iglesia> obtieneIglesiasAsignadasPorIds(List<Integer> listaIdIglesias) {
        try {
            String sql = HQL + " LEFT JOIN FETCH ig.ubicacion ub"
                    + " WHERE ig.id IN :ids AND ig.estado=TRUE ORDER BY ig.id";
            TypedQuery<Iglesia> query = super.getEntityManager().createQuery(sql, Iglesia.class);
            query.setParameter("ids", listaIdIglesias);
            List<Iglesia> result = query.getResultList();
            if (result.size() > 0) {
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public List<Iglesia> obtieneIglesiasPorAsignarPorIds(List<Integer> listaIdIglesias, List<Integer> listaIdParroquias) {
        try {
            String sql = HQL + " LEFT JOIN FETCH ig.ubicacion ub"
                    + " WHERE ig.id NOT IN :idsIglesias AND ub.id IN :idsParroquias AND  ig.estado=TRUE ORDER BY ig.id";
            TypedQuery<Iglesia> query = super.getEntityManager().createQuery(sql, Iglesia.class);
            query.setParameter("idsIglesias", listaIdIglesias);
            query.setParameter("idsParroquias", listaIdParroquias);
            List<Iglesia> result = query.getResultList();
            if (result.size() > 0) {
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

	public List<Iglesia> getIglesiasPorParroquias(List<Geograp> parroquias) {
		try {
            String sql = HQL + " LEFT JOIN FETCH ig.ubicacion ub"
                    + " WHERE  ub IN :parroquias ORDER BY ig.id";
            TypedQuery<Iglesia> query = super.getEntityManager().createQuery(sql, Iglesia.class);
            query.setParameter("parroquias", parroquias);            
            List<Iglesia> result = query.getResultList();
            if (result.size() > 0) {
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
	}
}
