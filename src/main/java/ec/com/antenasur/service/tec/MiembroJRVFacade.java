/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.service.tec;

import javax.ejb.Stateless;

import ec.com.antenasur.domain.tec.MiembroJRV;
import ec.com.antenasur.domain.generic.AbstractFacade;
import ec.com.antenasur.domain.tec.Mesa;
import java.util.List;
import java.util.Set;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class MiembroJRVFacade extends AbstractFacade<MiembroJRV, Integer> {

    private static final String HQL = " FROM MiembroJRV jrv";
    private static final String ORDENADO = " ORDER BY jrv.id";

    public MiembroJRVFacade() {
        super(MiembroJRV.class, Integer.class);
    }

    public Set<MiembroJRV> getJRVPorMesa(Mesa mesa) {
        try {
            String sql = HQL + " WHERE jrv.mesa=:mesa" + ORDENADO;
            TypedQuery<MiembroJRV> query = super.getEntityManager().createQuery(sql, MiembroJRV.class);
            query.setParameter("mesa", mesa);
            Set<MiembroJRV> result = (Set<MiembroJRV>) query.getSingleResult();
            if (result != null && !result.isEmpty()) {
                return result;
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;
    }

}
