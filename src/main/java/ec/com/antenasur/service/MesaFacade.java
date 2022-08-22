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

import ec.com.antenasur.domain.Mesa;
import ec.com.antenasur.domain.Recinto;
import ec.com.antenasur.domain.generic.AbstractFacade;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class MesaFacade extends AbstractFacade<Mesa, Integer> {

    private static final String HQL = " FROM Mesa m";
    private static final String ACTIVOS = " m.estado = TRUE";
    private static final String ORDENADO = " ORDER BY m.id";

    public MesaFacade() {
        super(Mesa.class, Integer.class);
    }



    /**
     *
     * @param nombreRecinto
     * @return
     */
    public Mesa buscaRecintoPorNombre(String nombreRecinto) {
        try {
            String sql = HQL + " WHERE m.nombre=:nombreRecinto AND " + ACTIVOS;
            TypedQuery<Mesa> query = super.getEntityManager().createQuery(sql, Mesa.class);
            query.setParameter("nombreRecinto", nombreRecinto);
            Mesa result = query.getSingleResult();
            if (result != null) {
                return result;
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;
    }

    public List<Mesa> getMesasPorParroquias(List<Integer> listaIdParroquias) {
        try {
            String sql = HQL + " LEFT JOIN FETCH m.ubicacion ub"
                    + " WHERE ub.id IN :ids AND " + ACTIVOS + ORDENADO;
            TypedQuery<Mesa> query = super.getEntityManager().createQuery(sql, Mesa.class);
            query.setParameter("ids", listaIdParroquias);
            List<Mesa> result = query.getResultList();
            if (result.size() > 0) {
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public List<Mesa> getMesasPorRecintos(List<Recinto> recintos) {
        try {
            String sql = HQL + " LEFT JOIN FETCH m.recinto r WHERE r IN :recintos AND m.estado=TRUE ORDER BY m.id";
            TypedQuery<Mesa> query = super.getEntityManager().createQuery(sql, Mesa.class);
            query.setParameter("recintos", recintos);
            List<Mesa> result = query.getResultList();
            if (result.size() > 0) {
                return result;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

}
