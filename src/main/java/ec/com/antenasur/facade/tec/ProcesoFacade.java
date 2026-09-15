/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.facade.tec;

import ec.com.antenasur.model.generic.AbstractFacade;
import ec.com.antenasur.model.tec.Proceso;

import java.util.List;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;

import jakarta.ejb.Stateless;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import ec.com.antenasur.dto.FiltroActividadAuditoriaDTO;

/**
 *
 * @author Usuario
 */
@Stateless
@DeclareRoles({"SITEC-Administrador", "SITEC-Tecnico", "SITEC-Analista", "SITEC-Tribunal",
    "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa"})
@RolesAllowed({"SITEC-Administrador", "SITEC-Tecnico", "SITEC-Analista", "SITEC-Tribunal",
    "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa"})
public class ProcesoFacade extends AbstractFacade<Proceso, Integer> {

    /**
     * La anotación en la clase no siempre se aplica al método heredado por el
     * proxy EJB de WildFly. La operación normal de auditoría se declara de
     * forma explícita para que Elytron evalúe los roles SITEC reales.
     */
    @Override
    @RolesAllowed({"SITEC-Administrador", "SITEC-Tecnico", "SITEC-Analista", "SITEC-Tribunal",
        "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa"})
    public Proceso create(Proceso entity) {
        return super.create(entity);
    }

    /**
     * Punto interno y acotado para fallos anteriores a request.login(). No
     * recibe actividad libre ni secretos, por lo que no expone un canal de
     * escritura general para usuarios anónimos.
     */
    @PermitAll
    public void registrarLoginFallidoPreautenticacion(String usuarioIntentado, String ip) {
        super.getEntityManager().createNativeQuery("INSERT INTO tec.procesos "
                + "(actividad, ip, estado, f_crea, u_crea) "
                + "VALUES (:actividad, :ip, TRUE, CURRENT_TIMESTAMP, :usuario)")
                .setParameter("actividad", "LOGIN | MÓDULO: ACCESO; RESULTADO: FALLIDO; DETALLE: Credenciales rechazadas")
                .setParameter("ip", ip)
                .setParameter("usuario", usuarioIntentado)
                .executeUpdate();
    }

    public ProcesoFacade() {
        super(Proceso.class, Integer.class);
    }

    public List<Proceso> getProcesoPorUsuario(String usuario) {
        try {
            String hql = "SELECT p FROM Proceso p WHERE p.usuarioCrea=:usuario AND p.estado=TRUE";
            Query query = super.getEntityManager().createQuery(hql);
            query.setParameter("usuario", usuario);
            List<Proceso> resultList = query.getResultList();
            if (resultList != null && !resultList.isEmpty()) {
                return resultList;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public List<Proceso> getProcesoPorUsuario(Date fechaInicio, Date fechaFin, String usuario) {
        try {
            String hql = "SELECT p FROM Proceso p "
                    + " WHERE cast(p.fechaCrea as date) BETWEEN :fechaInicio AND :fechaFin "
                    + " AND p.usuarioCrea=:usuario "
                    + "ORDER BY p.fechaCrea DESC ";
            Query query = super.getEntityManager().createQuery(hql);
            query.setParameter("fechaInicio", fechaInicio);
            query.setParameter("fechaFin", fechaFin);
            query.setParameter("usuario", usuario);
            List<Proceso> resultList = query.getResultList();
            if (resultList != null && !resultList.isEmpty()) {
                return resultList;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public int contarAuditoria(FiltroActividadAuditoriaDTO filtro, String usuarioAlcance) {
        Query query = crearConsultaAuditoria("SELECT COUNT(p)", filtro, usuarioAlcance);
        return ((Long) query.getSingleResult()).intValue();
    }

    public List<Proceso> buscarAuditoria(FiltroActividadAuditoriaDTO filtro, String usuarioAlcance,
            int first, int pageSize) {
        TypedQuery<Proceso> query = (TypedQuery<Proceso>) crearConsultaAuditoria("SELECT p", filtro, usuarioAlcance);
        query.setFirstResult(Math.max(first, 0));
        query.setMaxResults(Math.max(pageSize, 1));
        return query.getResultList();
    }

    public List<String> listarUsuariosConActividad() {
        TypedQuery<String> query = super.getEntityManager().createQuery(
                "SELECT DISTINCT p.usuarioCrea FROM Proceso p WHERE p.estado = true "
                + "AND p.usuarioCrea IS NOT NULL ORDER BY p.usuarioCrea", String.class);
        return query.getResultList();
    }

    private Query crearConsultaAuditoria(String seleccion, FiltroActividadAuditoriaDTO filtro, String usuarioAlcance) {
        FiltroActividadAuditoriaDTO criterio = filtro != null ? filtro : new FiltroActividadAuditoriaDTO();
        StringBuilder hql = new StringBuilder(seleccion).append(" FROM Proceso p WHERE p.estado = true");
        List<ParametroAuditoria> parametros = new ArrayList<>();

        if (usuarioAlcance != null) {
            hql.append(" AND p.usuarioCrea = :usuarioAlcance");
            parametros.add(new ParametroAuditoria("usuarioAlcance", usuarioAlcance));
        } else if (tieneTexto(criterio.getUsuario())) {
            hql.append(" AND p.usuarioCrea = :usuario");
            parametros.add(new ParametroAuditoria("usuario", criterio.getUsuario().trim()));
        }
        if (criterio.getFechaInicio() != null) {
            hql.append(" AND p.fechaCrea >= :fechaInicio");
            parametros.add(new ParametroAuditoria("fechaInicio", Timestamp.valueOf(criterio.getFechaInicio().atStartOfDay())));
        }
        if (criterio.getFechaFin() != null) {
            hql.append(" AND p.fechaCrea < :fechaFin");
            LocalDate siguienteDia = criterio.getFechaFin().plusDays(1);
            parametros.add(new ParametroAuditoria("fechaFin", Timestamp.valueOf(siguienteDia.atStartOfDay())));
        }
        agregarFiltroActividad(hql, parametros, "accion", criterio.getAccion());
        agregarFiltroActividad(hql, parametros, "modulo", criterio.getModulo());
        agregarFiltroActividad(hql, parametros, "resultado", criterio.getResultado());
        if (tieneTexto(criterio.getBusqueda())) {
            hql.append(" AND (LOWER(p.actividad) LIKE :busqueda OR LOWER(p.usuarioCrea) LIKE :busqueda OR p.ip LIKE :busqueda)");
            parametros.add(new ParametroAuditoria("busqueda", "%" + criterio.getBusqueda().trim().toLowerCase() + "%"));
        }
        if (!seleccion.startsWith("SELECT COUNT")) hql.append(" ORDER BY p.fechaCrea DESC, p.id DESC");

        Query query = super.getEntityManager().createQuery(hql.toString());
        for (ParametroAuditoria parametro : parametros) query.setParameter(parametro.nombre(), parametro.valor());
        return query;
    }

    private void agregarFiltroActividad(StringBuilder hql, List<ParametroAuditoria> parametros,
            String nombre, String valor) {
        if (!tieneTexto(valor)) return;
        hql.append(" AND LOWER(p.actividad) LIKE :").append(nombre);
        parametros.add(new ParametroAuditoria(nombre, "%" + valor.trim().toLowerCase() + "%"));
    }

    private boolean tieneTexto(String texto) {
        return texto != null && !texto.isBlank();
    }

    private record ParametroAuditoria(String nombre, Object valor) {
    }
}
