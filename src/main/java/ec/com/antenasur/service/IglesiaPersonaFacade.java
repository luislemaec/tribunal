/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.service;

import ec.com.antenasur.domain.Geograp;
import ec.com.antenasur.domain.IglesiaPersona;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import ec.com.antenasur.domain.generic.AbstractFacade;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class IglesiaPersonaFacade extends AbstractFacade<IglesiaPersona, Integer> {

    static final String HQL = "FROM IglesiaPersona ip";

    public IglesiaPersonaFacade() {
        super(IglesiaPersona.class, Integer.class);
    }



    public List<IglesiaPersona> getPersonasIglesiasPorParroquia(Geograp parroquia) {
        try {
            String sql = HQL + " WHERE ip.iglesia.ubicacion in :parroquia ORDER BY ip.id";
            TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
            query.setParameter("parroquia", parroquia);
            List<IglesiaPersona> result = query.getResultList();
            if (result.size() > 0) {
                return result;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public List<IglesiaPersona> getPersonasIglesiasPorIglesia(int iglesiaId) {
        try {
            String sql = HQL + " LEFT JOIN FETCH ip.iglesia i WHERE i.id = :iglesiaId AND ip.estado=TRUE ORDER BY ip.id";
            TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
            query.setParameter("iglesiaId", iglesiaId);
            List<IglesiaPersona> result = query.getResultList();
            if (result.size() > 0) {
                return result;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public List<IglesiaPersona> getIglesiasPersonasPorParroquias(List<Geograp> parroquias) {
        try {
            String sql = HQL + " LEFT JOIN FETCH ip.iglesia igl"
                    + " LEFT JOIN FETCH igl.ubicacion ub"
                    + " WHERE ub IN :parroquias ORDER BY ip.id";
            TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
            query.setParameter("parroquias", parroquias);
            List<IglesiaPersona> result = query.getResultList();
            if (result.size() > 0) {
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public IglesiaPersona buscarPorCedulaPersona(String cedula) {
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH ip.persona p"
                    + " WHERE p.documento=:cedula ORDER BY ip.id";
            TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
            query.setParameter("cedula", cedula);
            List<IglesiaPersona> result = query.getResultList();
            if (result.size() > 0) {
                return result.get(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

}
