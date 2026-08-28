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
import org.hibernate.Filter;
import org.hibernate.Session;

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
    private static final String ROL_IGLESIA_ADMIN = "%IglesiaAdmin";

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
            log.error("findByUsuarioName('{}'): JOIN FETCH lanzó excepción, probaré fallback sin fetch", username, e);
        }

        // Intento 2 (fallback): query simple sin JOIN FETCH. Persona se cargará
        // lazy si se accede después. Defensivo ante cambios de Hibernate 6.
        try {
            String sqlSimple = SQL + " where u.username = :username and u.estado=true";
            TypedQuery<Usuario> query = super.getEntityManager().createQuery(sqlSimple, Usuario.class);
            query.setParameter("username", username);
            List<Usuario> resultList = query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                log.info("findByUsuarioName('{}'): resuelto vía fallback simple", username);
                return resultList.get(0);
            }
            log.warn("findByUsuarioName('{}'): fallback simple tampoco encontró resultado", username);
        } catch (Exception e2) {
            log.error("findByUsuarioName('{}'): fallback simple también falló", username, e2);
        }
        return null;
    }

    /**
     * Busca por nombre de usuario sin aplicar el filtro de registros activos.
     * Se usa exclusivamente en flujos administrativos para reactivar una cuenta
     * dada de baja, ya que {@code usu_nombre} es único incluso cuando
     * {@code estado = false}.
     */
    public Usuario findByUsuarioNameIncluyendoInactivos(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        Session session = super.getEntityManager().unwrap(Session.class);
        Filter filtroActivo = session.getEnabledFilter("filterActive");
        if (filtroActivo != null) {
            session.disableFilter("filterActive");
        }
        try {
            TypedQuery<Usuario> query = session.createQuery(
                    SQL + "LEFT JOIN FETCH u.personsa p WHERE u.username = :username", Usuario.class);
            query.setParameter("username", username.trim());
            query.setMaxResults(1);
            List<Usuario> resultado = query.getResultList();
            return resultado.isEmpty() ? null : resultado.get(0);
        } finally {
            if (filtroActivo != null) {
                session.enableFilter("filterActive");
            }
        }
    }

    /** Busca un usuario por persona, sin ocultar cuentas eliminadas lógicamente. */
    public Usuario findByPersonaIdIncluyendoInactivos(Integer personaId) {
        if (personaId == null) {
            return null;
        }
        Session session = super.getEntityManager().unwrap(Session.class);
        Filter filtroActivo = session.getEnabledFilter("filterActive");
        if (filtroActivo != null) {
            session.disableFilter("filterActive");
        }
        try {
            TypedQuery<Usuario> query = session.createQuery(
                    SQL + "LEFT JOIN FETCH u.personsa p WHERE p.id = :personaId ORDER BY u.id ASC", Usuario.class);
            query.setParameter("personaId", personaId);
            query.setMaxResults(1);
            List<Usuario> resultado = query.getResultList();
            return resultado.isEmpty() ? null : resultado.get(0);
        } finally {
            if (filtroActivo != null) {
                session.enableFilter("filterActive");
            }
        }
    }

    /** Recupera una cuenta por id sin ocultar registros dados de baja. */
    public Usuario findByIdIncluyendoInactivos(Integer usuarioId) {
        if (usuarioId == null) {
            return null;
        }
        Session session = super.getEntityManager().unwrap(Session.class);
        Filter filtroActivo = session.getEnabledFilter("filterActive");
        if (filtroActivo != null) {
            session.disableFilter("filterActive");
        }
        try {
            TypedQuery<Usuario> query = session.createQuery(
                    SQL + "LEFT JOIN FETCH u.personsa p LEFT JOIN FETCH u.iglesia i WHERE u.id = :usuarioId",
                    Usuario.class);
            query.setParameter("usuarioId", usuarioId);
            query.setMaxResults(1);
            List<Usuario> resultado = query.getResultList();
            return resultado.isEmpty() ? null : resultado.get(0);
        } finally {
            if (filtroActivo != null) {
                session.enableFilter("filterActive");
            }
        }
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
     * o {@code null} si la iglesia aÃƒºn no tiene admin. Identifica al admin por
     * el vÃƒÂ­nculo directo {@code u.iglesia} (solo IglesiaAdmin queda con iglesia
     * asignada por convenciÃƒÂ³n del flujo de creaciÃƒÂ³n).
     */
    public Usuario findAdminByIglesiaId(Integer iglesiaId) {
        if (iglesiaId == null) {
            return null;
        }
        try {
            String sql = "SELECT DISTINCT u FROM Usuario u "
                    + "LEFT JOIN FETCH u.personsa p "
                    + "JOIN u.rolUsuarios ru "
                    + "JOIN ru.rol r "
                    + "WHERE u.iglesia.id = :iglesiaId "
                    + "AND u.estado = TRUE AND ru.estado = TRUE AND r.estado = TRUE "
                    + "AND r.nombre LIKE :rolIglesiaAdmin "
                    + "ORDER BY u.id DESC";
            TypedQuery<Usuario> query = super.getEntityManager().createQuery(sql, Usuario.class);
            query.setParameter("iglesiaId", iglesiaId);
            query.setParameter("rolIglesiaAdmin", ROL_IGLESIA_ADMIN);
            query.setMaxResults(1);
            List<Usuario> result = query.getResultList();
            return (result != null && !result.isEmpty()) ? result.get(0) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Devuelve todos los usuarios IglesiaAdmin activos del sistema (uno por
     * iglesia asignada). Pensado para construir un mapa iglesiaId Ã¢â€ ’ admin sin
     * disparar N+1 al listar iglesias en la pantalla de asignaciÃƒÂ³n.
     */
    public List<Usuario> findAllIglesiaAdmins() {
        try {
            String sql = "SELECT DISTINCT u FROM Usuario u "
                    + "LEFT JOIN FETCH u.personsa p "
                    + "LEFT JOIN FETCH u.iglesia i "
                    + "JOIN u.rolUsuarios ru "
                    + "JOIN ru.rol r "
                    + "WHERE u.iglesia IS NOT NULL "
                    + "AND u.estado = TRUE AND ru.estado = TRUE AND r.estado = TRUE "
                    + "AND r.nombre LIKE :rolIglesiaAdmin";
            TypedQuery<Usuario> query = super.getEntityManager().createQuery(sql, Usuario.class);
            query.setParameter("rolIglesiaAdmin", ROL_IGLESIA_ADMIN);
            return query.getResultList();
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    public List<Usuario> findIglesiaAdminsPorIglesias(List<Integer> iglesiaIds) {
        if (iglesiaIds == null || iglesiaIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        String hql = "SELECT DISTINCT u FROM Usuario u "
                + "LEFT JOIN FETCH u.personsa p "
                + "LEFT JOIN FETCH u.iglesia i "
                + "JOIN u.rolUsuarios ru "
                + "JOIN ru.rol r "
                + "WHERE i.id IN :iglesiaIds "
                + "AND u.estado = TRUE AND ru.estado = TRUE AND r.estado = TRUE "
                + "AND r.nombre LIKE :rolIglesiaAdmin";
        TypedQuery<Usuario> query = getEntityManager().createQuery(hql, Usuario.class);
        query.setParameter("iglesiaIds", iglesiaIds);
        query.setParameter("rolIglesiaAdmin", ROL_IGLESIA_ADMIN);
        return query.getResultList();
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
