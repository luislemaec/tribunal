/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.facade;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jakarta.ejb.Stateless;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import ec.com.antenasur.model.Rol;
import ec.com.antenasur.model.RolUsuario;
import ec.com.antenasur.dto.FiltroUsuarioDTO;
import ec.com.antenasur.model.generic.AbstractFacade;
import org.hibernate.Filter;
import org.hibernate.Session;

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
            String hql = "select ru from RolUsuario ru "
            		+ "where ru.usuario.estado=true "
            		+ "and ru.usuario.username = :userName "
            		+ "and ru.estado = true  "
            		+ "AND (ru.rol.nombre='Superadmin' OR ru.rol.nombre like :role)";
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

    /**
     * Retorna las relaciones de rol de un usuario, incluidas las inactivas.
     * La baja de un usuario es lógica, por lo que estas relaciones se reutilizan
     * al reactivar la cuenta y se evita violar {@code uk_rol_usuario}.
     */
    public List<RolUsuario> findByUsuarioIdIncluyendoInactivos(Integer usuarioId) {
        if (usuarioId == null) {
            return java.util.Collections.emptyList();
        }
        Session session = super.getEntityManager().unwrap(Session.class);
        Filter filtroActivo = session.getEnabledFilter("filterActive");
        if (filtroActivo != null) {
            session.disableFilter("filterActive");
        }
        try {
            TypedQuery<RolUsuario> query = session.createQuery(
                    "SELECT ru FROM RolUsuario ru JOIN FETCH ru.rol WHERE ru.usuario.id = :usuarioId",
                    RolUsuario.class);
            query.setParameter("usuarioId", usuarioId);
            return query.getResultList();
        } finally {
            if (filtroActivo != null) {
                session.enableFilter("filterActive");
            }
        }
    }

    /**
     * Busca solo la página solicitada por la tabla de usuarios. Las
     * asociaciones necesarias para el DTO se cargan en la misma consulta para
     * evitar consultas adicionales por cada fila.
     */
    public List<RolUsuario> buscarUsuarios(FiltroUsuarioDTO filtro, int primerRegistro, int tamanoPagina) {
        Session session = super.getEntityManager().unwrap(Session.class);
        Filter filtroActivo = deshabilitarFiltroActivoSiCorresponde(session, filtro);
        String hql = "SELECT ru FROM RolUsuario ru "
                + "JOIN FETCH ru.usuario u "
                + "JOIN FETCH ru.rol r "
                + "LEFT JOIN FETCH u.personsa p "
                + "LEFT JOIN FETCH u.iglesia i "
                + construirWhereBusqueda(filtro)
                + " ORDER BY LOWER(u.username), LOWER(COALESCE(p.nombres, '')), ru.id";
        try {
            TypedQuery<RolUsuario> query = session.createQuery(hql, RolUsuario.class);
            aplicarParametrosBusqueda(query, filtro);
            query.setFirstResult(Math.max(primerRegistro, 0));
            query.setMaxResults(Math.max(tamanoPagina, 1));
            return query.getResultList();
        } finally {
            restaurarFiltroActivo(session, filtroActivo);
        }
    }

    /** Cuenta los resultados con los mismos criterios de {@link #buscarUsuarios}. */
    public int contarUsuarios(FiltroUsuarioDTO filtro) {
        Session session = super.getEntityManager().unwrap(Session.class);
        Filter filtroActivo = deshabilitarFiltroActivoSiCorresponde(session, filtro);
        String hql = "SELECT COUNT(ru.id) FROM RolUsuario ru "
                + "JOIN ru.usuario u "
                + "JOIN ru.rol r "
                + "LEFT JOIN u.personsa p "
                + "LEFT JOIN u.iglesia i "
                + construirWhereBusqueda(filtro);
        try {
            TypedQuery<Long> query = session.createQuery(hql, Long.class);
            aplicarParametrosBusqueda(query, filtro);
            return query.getSingleResult().intValue();
        } finally {
            restaurarFiltroActivo(session, filtroActivo);
        }
    }

    private String construirWhereBusqueda(FiltroUsuarioDTO filtro) {
        List<String> condiciones = new ArrayList<>();
        boolean soloDadosBaja = filtro != null && Boolean.TRUE.equals(filtro.getSoloDadosBaja());
        condiciones.add(soloDadosBaja ? "u.estado = FALSE" : "u.estado = TRUE");
        // Una cuenta dada de baja puede tener relaciones de rol inactivas; se
        // necesitan para seleccionar de forma explícita el rol a restaurar.
        if (!soloDadosBaja) {
            condiciones.add("ru.estado = TRUE");
        }
        condiciones.add("r.estado = TRUE");
        // Mantiene el mismo alcance de la pantalla: solamente roles SITEC.
        condiciones.add("r.nombre LIKE :prefijoRol");
        if (filtro != null && filtro.getRolId() != null) {
            condiciones.add("r.id = :rolId");
        }
        agregarFiltroTexto(condiciones, "u.username", valorFiltro(filtro != null ? filtro.getUsername() : null), "username");
        agregarFiltroTexto(condiciones, "p.nombres", valorFiltro(filtro != null ? filtro.getNombres() : null), "nombres");
        agregarFiltroTexto(condiciones, "i.nombre", valorFiltro(filtro != null ? filtro.getIglesiaNombre() : null), "iglesia");
        return " WHERE " + String.join(" AND ", condiciones);
    }

    private void agregarFiltroTexto(List<String> condiciones, String campo, String valor, String parametro) {
        if (valor != null) {
            condiciones.add("LOWER(COALESCE(" + campo + ", '')) LIKE :" + parametro);
        }
    }

    private void aplicarParametrosBusqueda(Query query, FiltroUsuarioDTO filtro) {
        query.setParameter("prefijoRol", "SITEC-%");
        if (filtro != null && filtro.getRolId() != null) {
            query.setParameter("rolId", filtro.getRolId());
        }
        Map<String, String> parametros = new HashMap<>();
        parametros.put("username", valorFiltro(filtro != null ? filtro.getUsername() : null));
        parametros.put("nombres", valorFiltro(filtro != null ? filtro.getNombres() : null));
        parametros.put("iglesia", valorFiltro(filtro != null ? filtro.getIglesiaNombre() : null));
        for (Map.Entry<String, String> parametro : parametros.entrySet()) {
            if (parametro.getValue() != null) {
                query.setParameter(parametro.getKey(), '%' + parametro.getValue() + '%');
            }
        }
    }

    private String valorFiltro(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toLowerCase();
    }

    private Filter deshabilitarFiltroActivoSiCorresponde(Session session, FiltroUsuarioDTO filtro) {
        if (filtro == null || !Boolean.TRUE.equals(filtro.getSoloDadosBaja())) {
            return null;
        }
        Filter filtroActivo = session.getEnabledFilter("filterActive");
        if (filtroActivo != null) {
            session.disableFilter("filterActive");
        }
        return filtroActivo;
    }

    private void restaurarFiltroActivo(Session session, Filter filtroActivo) {
        if (filtroActivo != null) {
            session.enableFilter("filterActive");
        }
    }

}
