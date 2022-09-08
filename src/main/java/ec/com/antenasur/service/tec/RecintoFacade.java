/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.service.tec;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import ec.com.antenasur.domain.Geograp;
import ec.com.antenasur.domain.tec.Recinto;
import ec.com.antenasur.domain.generic.AbstractFacade;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class RecintoFacade extends AbstractFacade<Recinto, Integer> {

    private static final String HQL = " FROM Recinto r";    
    private static final String ORDENADO = " ORDER BY r.id";

    public RecintoFacade() {
        super(Recinto.class, Integer.class);
    }


    /**
     *
     * @param nombreRecinto
     * @return
     */
    public Recinto buscaRecintoPorNombre(String nombreRecinto) {
        try {
            String sql = HQL + " WHERE r.nombre=:nombreRecinto ";
            TypedQuery<Recinto> query = super.getEntityManager().createQuery(sql, Recinto.class);
            query.setParameter("nombreRecinto", nombreRecinto);
            Recinto result = query.getSingleResult();
            if (result != null) {
                return result;
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;
    }

    public List<Recinto> getRecintosPorParroquias(List<Geograp> parroquias) {
        try {
            String sql = HQL + " LEFT JOIN FETCH r.ubicacion ub"
                    + " WHERE ub IN :parroquias  " + ORDENADO;
            TypedQuery<Recinto> query = super.getEntityManager().createQuery(sql, Recinto.class);
            query.setParameter("parroquias", parroquias);
            List<Recinto> result = query.getResultList();
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
