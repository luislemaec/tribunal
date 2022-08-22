/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.service;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import ec.com.antenasur.domain.Persona;
import ec.com.antenasur.domain.generic.AbstractFacade;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class PersonaFacade extends AbstractFacade<Persona, Integer> {

    public PersonaFacade() {
        super(Persona.class, Integer.class);
    }


    public List<Persona> getByRuc() {
        try {
            String sql = "FROM Persona WHERE estado =TRUE ORDER BY id";
            TypedQuery<Persona> query = super.getEntityManager().createQuery(sql, Persona.class);
            List<Persona> result = (List<Persona>) query.getResultList();
            if (result.size() > 0) {
                return result;
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;
    }

    /**
     *
     * @param id_type_Persona
     * @return
     */
    public Persona finByPersonaDocument(String documento) {
        try {
            String sql = "select p from Persona p WHERE p.documento=:documento AND p.estado =true";
            TypedQuery<Persona> query = super.getEntityManager().createQuery(sql, Persona.class);
            query.setParameter("documento", documento);

            Persona result = query.getSingleResult();
            if (result != null) {
                return result;
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;
    }

    public Persona searchPersonaByUserId(Integer user_id) {

        try {
            String sql = "select p from Persona p WHERE p.usuarios in :user_id AND p.estado =true";
            TypedQuery<Persona> query = super.getEntityManager().createQuery(sql, Persona.class);
            query.setParameter("user_id", user_id);

            Persona result = query.getSingleResult();
            if (result != null) {
                return result;
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;
    }

    public Persona buscarPorCedula(String documento) {
        try {
            String sql = "select p from Persona p WHERE p.documento = :documento AND p.estado=true";
            TypedQuery<Persona> query = super.getEntityManager().createQuery(sql, Persona.class);
            query.setParameter("documento", documento);

            Persona result = query.getSingleResult();
            if (result != null) {
                return result;
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;
    }

}
