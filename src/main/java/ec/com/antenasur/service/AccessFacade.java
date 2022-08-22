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

import ec.com.antenasur.domain.AccessAuditory;
import ec.com.antenasur.domain.generic.AbstractFacade;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class AccessFacade extends AbstractFacade<AccessAuditory, Integer> {

    public AccessFacade() {
        super(AccessAuditory.class, Integer.class);
    }

    public AccessAuditory findBySession(String session) {
        try {
            String sql = "FROM AccessAuditory a WHERE a.session =:session ORDER BY id";
            TypedQuery<AccessAuditory> query = super.getEntityManager().createQuery(sql, AccessAuditory.class);
            query.setParameter("session", session);
            List<AccessAuditory> result = query.getResultList();
            if (result.size() > 0) {
                return result.get(0);
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;

    }

    public List<AccessAuditory> findAllOrderByIdDesc() {
        try {
            String sql = "FROM AccessAuditory ORDER BY id DESC";
            TypedQuery<AccessAuditory> query = super.getEntityManager().createQuery(sql, AccessAuditory.class);
            List<AccessAuditory> result = query.getResultList();
            if (result.size() > 0) {
                return result;
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;

    }

}
