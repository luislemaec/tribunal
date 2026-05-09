/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.facade;

import ec.com.antenasur.model.Menu;
import jakarta.ejb.Stateless;

import ec.com.antenasur.model.MenuRol;
import ec.com.antenasur.model.Rol;
import ec.com.antenasur.model.generic.AbstractFacade;
import java.util.List;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class MenuRolFacade extends AbstractFacade<MenuRol, Integer> {

    static final String HQL = " SELECT mr FROM MenuRol mr";

    public MenuRolFacade() {
        super(MenuRol.class, Integer.class);
    }

    public MenuRol getPorMenuYRol(Menu menu, Rol rol) {
        try {
            String sql = HQL + " WHERE mr.menu = :menu AND mr.rol =: rol ORDER BY mr.id";
            TypedQuery<MenuRol> query = super.getEntityManager().createQuery(sql, MenuRol.class);
            query.setParameter("menu", menu);
            query.setParameter("rol", rol);
            List<MenuRol> result = query.getResultList();
            if (result.size() > 0) {
                return result.get(0);
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;
    }

    public List<MenuRol> getPorRol(Rol rol) {
        try {
            String sql = HQL + " WHERE mr.rol =: rol ORDER BY mr.id";
            TypedQuery<MenuRol> query = super.getEntityManager().createQuery(sql, MenuRol.class);            
            query.setParameter("rol", rol);
            List<MenuRol> result = query.getResultList();
            if (result.size() > 0) {
                return result;
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;
    }

}
