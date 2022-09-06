/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.service;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import ec.com.antenasur.domain.Rol;
import ec.com.antenasur.domain.generic.AbstractFacade;

;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class RolFacade extends AbstractFacade<Rol, Integer> {

    public RolFacade() {
        super(Rol.class, Integer.class);
    }

    public Rol buscaPorNombre(String nombre) {
        try {
            String sql = "FROM Rol r WHERE r.nombre=:nombre";
            Query query = super.getEntityManager().createQuery(sql);
            query.setParameter("nombre", nombre);
            List<Rol> resultList = query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList.get(0);
            }

        } catch (NoResultException e) {
            return null;
        }
        return null;
    }

    public List<Rol> getRolesAplicativoSeleccion() {
        try {
            String sql = "FROM Rol r WHERE r.nombre LIKE :rolSeleccion and r.estado=true";
            Query query = super.getEntityManager().createQuery(sql);
            query.setParameter("rolSeleccion", "SITEC-%");
            List<Rol> resultList = query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList;
            }

        } catch (NoResultException e) {
            return null;
        }
        return null;
    }

}
