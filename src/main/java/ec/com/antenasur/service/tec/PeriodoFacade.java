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

import ec.com.antenasur.domain.tec.Periodo;
import ec.com.antenasur.domain.generic.AbstractFacade;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class PeriodoFacade extends AbstractFacade<Periodo, Integer> {

    static final String HQL = " FROM Periodo p";
    static final String ACTIVOS = "  p.estado =TRUE";
    static final String ORDENADO = "  ORDER BY p.id";

    public PeriodoFacade() {
        super(Periodo.class, Integer.class);
    }

    public Periodo getPeridoActivo() {
        try {
            String sql = HQL + " WHERE" + ACTIVOS + ORDENADO + " DESC";
            TypedQuery<Periodo> query = super.getEntityManager().createQuery(sql, Periodo.class);
            List<Periodo> result = query.getResultList();
            if (result.size() > 0) {
                return result.get(0);
            }
        } catch (NoResultException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public Periodo getPeriodoVigente() {
        try {
            String sql = HQL + ORDENADO + " DESC";
            TypedQuery<Periodo> query = super.getEntityManager().createQuery(sql, Periodo.class);
            Periodo result = query.getSingleResult();
            return result;
        } catch (NoResultException e) {
            e.printStackTrace();
            return null;
        }
    }

}
