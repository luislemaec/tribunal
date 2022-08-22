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
import javax.persistence.TypedQuery;

import ec.com.antenasur.domain.Usuario;
import ec.com.antenasur.domain.generic.AbstractFacade;



/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class UsuarioFacade extends AbstractFacade<Usuario, Integer> {

    public UsuarioFacade() {
        super(Usuario.class, Integer.class);
    }
    private static final String SQL = "FROM Usuario u ";

    /**
     * 
     * @param docuId, 
     * @return 
     */
    public Usuario getUsuarioByRuc(String docuId) {
        try {
            String sql = SQL + "WHERE docuId =:docuId and estado=true ";
            TypedQuery<Usuario> query = super.getEntityManager().createQuery(sql, Usuario.class);
            query.setParameter("docuId", docuId);
            Usuario result = (Usuario) query.getSingleResult();
            if (result != null) {
                return result;
            }
        } catch (NoResultException e) {
            e.getStackTrace();
            return null;
        }
        return null;
    }

    public Usuario findByUsuarioName(String username, String contrasenia) {

        try {
            String sql = SQL + "where u.username = :username and u.contrasenia=:contrasenia and u.estado = true ";
            TypedQuery<Usuario> query = super.getEntityManager().createQuery(sql, Usuario.class);
            query.setParameter("username", username);
            query.setParameter("contrasenia", contrasenia);

            List<Usuario> resultList = query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList.get(0);
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;

    }

    public Usuario findByUsuarioName(String username) {

        try {
            String sql = SQL + " where u.username = :username and u.estado=true";
            TypedQuery<Usuario> query = super.getEntityManager().createQuery(sql, Usuario.class);
            query.setParameter("username", username);
            List<Usuario> resultList = query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList.get(0);
            }
        } catch (Exception e) {
            return null;
        }
        return null;

    }

    public Usuario findUsuarioByRucOrMail(String username, String correo) {
        try {
            String sql = SQL + "LEFT JOIN FETCH u.personsa p "
                    + "where u.username = :username and u.correo=:correo and  u.estado=true and p.estado=true ";
            TypedQuery<Usuario> query = super.getEntityManager().createQuery(sql, Usuario.class);
            query.setParameter("Usuarioname", username);
            query.setParameter("correo", correo);
            List<Usuario> resultList = query.getResultList();
            if (resultList != null && !resultList.isEmpty()) {
                return resultList.get(0);
            }
        } catch (Exception e) {
            return null;
        }
        return null;

    }

    public Usuario findUsuarioByTemportalPassword(String username, String contraseniaTemp) {
        try {
            String sql = SQL + "LEFT JOIN FETCH u.persona p "
                    + "where u.username = :username and u.contraseniaTemp=:contraseniaTemp and u.estado=true and p.estado=true and u.permanente=true";
            TypedQuery<Usuario> query = super.getEntityManager().createQuery(sql, Usuario.class);
            query.setParameter("username", username);
            query.setParameter("contraseniaTemp", contraseniaTemp);
            List<Usuario> resultList = query.getResultList();
            if (resultList != null && !resultList.isEmpty()) {
                return resultList.get(0);
            }
        } catch (Exception e) {
            return null;
        }
        return null;

    }

    public Usuario findUsuarioByPeople(int persona_id) {
        try {
            String sql = SQL + "where p.id=:persona_id and u.estado=true  and  p.estado=true ";
            TypedQuery<Usuario> query = super.getEntityManager().createQuery(sql, Usuario.class);
            query.setParameter("persona_id", persona_id);
            List<Usuario> resultList = query.getResultList();
            if (resultList != null && !resultList.isEmpty()) {
                return resultList.get(0);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public Usuario findUsuariobyUsuarioName(String username) {
        try {
            String sql = SQL + "INNER JOIN FETCH u.persona p "
                    + "WHERE u.username=:username and u.estado=true ";
            Query query = super.getEntityManager().createQuery(sql);

            query.setParameter("username", username);
            Usuario result = (Usuario) query.getSingleResult();
            if (result != null) {
                return result;
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;
    }

    /**
     *
     * @return Usuarios activos
     */
    public List<Usuario> findAllActiveUsuario() {
        try {
            String sql = SQL + "where u.estado=true ";
            TypedQuery<Usuario> query = super.getEntityManager().createQuery(sql, Usuario.class);
            List<Usuario> resultList = query.getResultList();
            if (resultList != null && !resultList.isEmpty()) {
                return resultList;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

}
