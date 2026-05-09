/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.facade;

import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import ec.com.antenasur.model.Usuario;
import ec.com.antenasur.model.generic.AbstractFacade;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
@Slf4j
public class UsuarioFacade extends AbstractFacade<Usuario, Integer> {

    public UsuarioFacade() {
        super(Usuario.class, Integer.class);
    }
    private static final String SQL = "SELECT u FROM Usuario u ";

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
        // Intento 1: con LEFT JOIN FETCH para evitar segunda query a Persona.
        try {
            String sql = SQL + "LEFT JOIN FETCH u.personsa p"
                    + " where u.username = :username and u.estado=true";
            TypedQuery<Usuario> query = super.getEntityManager().createQuery(sql, Usuario.class);
            query.setParameter("username", username);
            List<Usuario> resultList = query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList.get(0);
            }
            log.warn("findByUsuarioName('{}'): JOIN FETCH ejecutado sin error pero sin resultados", username);
        } catch (Exception e) {
            log.error("findByUsuarioName('{}'): JOIN FETCH lanzÃ³ excepciÃ³n, probarÃ© fallback sin fetch", username, e);
        }

        // Intento 2 (fallback): query simple sin JOIN FETCH. Persona se cargarÃ¡
        // lazy si se accede despuÃ©s. Defensivo ante cambios de Hibernate 6.
        try {
            String sqlSimple = SQL + " where u.username = :username and u.estado=true";
            TypedQuery<Usuario> query = super.getEntityManager().createQuery(sqlSimple, Usuario.class);
            query.setParameter("username", username);
            List<Usuario> resultList = query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                log.info("findByUsuarioName('{}'): resuelto vÃ­a fallback simple", username);
                return resultList.get(0);
            }
            log.warn("findByUsuarioName('{}'): fallback simple tampoco encontrÃ³ resultado", username);
        } catch (Exception e2) {
            log.error("findByUsuarioName('{}'): fallback simple tambiÃ©n fallÃ³", username, e2);
        }
        return null;
    }

    public Usuario findUsuarioByRucOrMail(String username, String correo) {
        try {
            String sql = SQL + "LEFT JOIN FETCH u.personsa p "
                    + "where u.username = :username and u.correo=:correo and  u.estado=true and p.estado=true ";
            TypedQuery<Usuario> query = super.getEntityManager().createQuery(sql, Usuario.class);
            query.setParameter("username", username);
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
     * Devuelve el {@link Usuario} IglesiaAdmin asignado a la iglesia indicada,
     * o {@code null} si la iglesia aÃƒÂºn no tiene admin. Identifica al admin por
     * el vÃƒÂ­nculo directo {@code u.iglesia} (solo IglesiaAdmin queda con iglesia
     * asignada por convenciÃƒÂ³n del flujo de creaciÃƒÂ³n).
     */
    public Usuario findAdminByIglesiaId(Integer iglesiaId) {
        if (iglesiaId == null) {
            return null;
        }
        try {
            String sql = SQL
                    + "LEFT JOIN FETCH u.personsa p"
                    + " WHERE u.iglesia.id = :iglesiaId AND u.estado = TRUE"
                    + " ORDER BY u.id DESC";
            TypedQuery<Usuario> query = super.getEntityManager().createQuery(sql, Usuario.class);
            query.setParameter("iglesiaId", iglesiaId);
            query.setMaxResults(1);
            List<Usuario> result = query.getResultList();
            return (result != null && !result.isEmpty()) ? result.get(0) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Devuelve todos los usuarios IglesiaAdmin activos del sistema (uno por
     * iglesia asignada). Pensado para construir un mapa iglesiaId Ã¢â€ â€™ admin sin
     * disparar N+1 al listar iglesias en la pantalla de asignaciÃƒÂ³n.
     */
    public List<Usuario> findAllIglesiaAdmins() {
        try {
            String sql = SQL
                    + "LEFT JOIN FETCH u.personsa p"
                    + " LEFT JOIN FETCH u.iglesia i"
                    + " WHERE u.iglesia IS NOT NULL AND u.estado = TRUE";
            TypedQuery<Usuario> query = super.getEntityManager().createQuery(sql, Usuario.class);
            return query.getResultList();
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
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
