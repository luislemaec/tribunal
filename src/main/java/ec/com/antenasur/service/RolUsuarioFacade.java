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

import ec.com.antenasur.domain.Rol;
import ec.com.antenasur.domain.RolUsuario;
import ec.com.antenasur.domain.generic.AbstractFacade;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class RolUsuarioFacade extends AbstractFacade<RolUsuario, Integer> {

    public RolUsuarioFacade() {
        super(RolUsuario.class, Integer.class);
    }

    /**
     * Buscar Roles por usuario
     *
     * @param userName nombre de usuario
     * @param roleName nombre rol
     * @return Devuelve null si no encuntra ningun registro
     */
    public List<RolUsuario> findByUserNameAndRoleName2(String userName, String roleName) {
        try {
            String hql = "select ru from RolUsuario ru where ru.usuario.estado=true and ru.usuario.username = :userName and ru.estado = true  AND (ru.rol.nombre='Superadmin' OR ru.rol.nombre like :role)";
            TypedQuery<RolUsuario> query = super.getEntityManager().createQuery(hql, RolUsuario.class);
            query.setParameter("userName", userName);
            query.setParameter("role", roleName);
            List<RolUsuario> resultList = (List<RolUsuario>) query.getResultList();
            if (resultList != null && !resultList.isEmpty()) {
                return resultList;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public List<RolUsuario> findByUserNameAndRoleName_(String userName) {

        try {
            String hql = "select ru from RolUsuario ru "
                    + "where ru.usuario.estado=true and ru.usuario.username = :userName and ru.estado=true";

            TypedQuery<RolUsuario> query = super.getEntityManager().createQuery(hql, RolUsuario.class);

            query.setParameter("userName", userName);
            List<RolUsuario> resultList = query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList;
            }

        } catch (Exception e) {
            e.getStackTrace();
            return null;
        }
        return null;
    }

    public List<RolUsuario> findByUserName(String userName) {

        try {
            // + "LEFT JOIN FETCH ru.user u "
            String sql = "select ru from RolUsuario ru " + "LEFT JOIN FETCH ru.rol r " + "LEFT JOIN FETCH ru.usuario u "
                    + "WHERE ru.estado=true and u.estado=true and u.username = :userName  and r.estado=true";

            Query query = super.getEntityManager().createQuery(sql);

            query.setParameter("userName", userName);
            List<RolUsuario> resultList = query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList;
            }

        } catch (NoResultException e) {
            e.getStackTrace();
            return null;
        }
        return null;
    }

    public List<RolUsuario> findByUserNameAndRoleName(String userName, String roleName) {

        try {
            // + "LEFT JOIN FETCH ru.user u "
            String sql = "select ru from RolUsuario ru " + "LEFT JOIN FETCH ru.rol r " + "LEFT JOIN FETCH ru.usuario u "
                    // + "WHERE ru.estado=true and ru.user.estado=true and ru.user.username =
                    // :userName and ru.role.estado=true";
                    + "WHERE ru.estado=true and u.estado=true and u.username = :userName and r.nombre=:roleName and r.estado=true";

            // TypedQuery<RolUsuario> query = super.getEntityManager().createQuery(sql,
            // RolUsuario.class);
            Query query = super.getEntityManager().createQuery(sql);

            query.setParameter("userName", userName);
            query.setParameter("roleName", roleName);
            // List<RolUsuario> resultList =(List<RolUsuario>) query.getResultList();
            List<RolUsuario> resultList = query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList;
            }

        } catch (NoResultException e) {
            e.getStackTrace();
            return null;
        }
        return null;
    }

    public List<RolUsuario> findByRoleName(String roleName) {
        try {
            String sql = "select ru from RolUsuario ru " + "INNER JOIN FETCH ru.usuario u "
                    + "INNER JOIN FETCH ru.rol r "
                    + "where u.estado=true and r.nombre = :roleName and ru.estado=true and r.estado=true ";
            // + "where ru.user.estado=true and ru.role.name = :roleName and ru.estado=true
            // and ru.role.estado=true ";

            // TypedQuery<RolUsuario> query = super.getEntityManager().createQuery(sql,
            // RolUsuario.class);
            Query query = super.getEntityManager().createQuery(sql);

            query.setParameter("roleName", roleName);
            // List<RolUsuario> resultList = query.getResultList();
            List<RolUsuario> resultList = (List<RolUsuario>) query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList;
            }

        } catch (Exception e) {
            e.getStackTrace();
            return null;
        }
        return null;
    }

    public List<RolUsuario> getAllActiveRolesUsers() {
        try {
            String sql = "select ru from RolUsuario ru " + "INNER JOIN FETCH ru.usuario u "
                    + "INNER JOIN FETCH ru.rol r " + "where u.estado=true and ru.estado=true and r.estado=true ";
            Query query = super.getEntityManager().createQuery(sql);
            List<RolUsuario> resultList = (List<RolUsuario>) query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList;
            }

        } catch (Exception e) {
            e.getStackTrace();
            return null;
        }
        return null;
    }

    public List<RolUsuario> getRolesUsuariosActivos(List<Rol> listaRoles) {
        try {
            String sql = "select ru from RolUsuario ru " + "INNER JOIN FETCH ru.usuario u "
                    + "INNER JOIN FETCH ru.rol r " + "where u.estado=true and ru.estado=true and r.estado=true and r IN :listaRoles  ";
            Query query = super.getEntityManager().createQuery(sql);
            query.setParameter("listaRoles", listaRoles);
            List<RolUsuario> resultList = (List<RolUsuario>) query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList;
            }

        } catch (Exception e) {
            e.getStackTrace();
            return null;
        }
        return null;
    }

}
