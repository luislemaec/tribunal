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

import ec.com.antenasur.domain.Tribunal;
import ec.com.antenasur.domain.generic.AbstractFacade;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class TribunalFacade extends AbstractFacade<Tribunal, Integer> {

    static final String HQL = " FROM Tribunal t";
    static final String ACTIVOS = "  t.estado =TRUE";
    static final String ORDENADO = "  ORDER BY t.id";

    public TribunalFacade() {
        super(Tribunal.class, Integer.class);
    }

    public List<Tribunal> getRegistrosActivos() {
        try {
            String sql = HQL + " INNER JOIN FETCH t.cargo c"
                    + " WHERE " + ACTIVOS + ORDENADO;
            TypedQuery<Tribunal> query = super.getEntityManager().createQuery(sql, Tribunal.class);
            List<Tribunal> result = query.getResultList();
            if (result.size() > 0) {
                return result;
            }
        } catch (NoResultException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

}
