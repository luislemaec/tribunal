/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.facade.tec;

import jakarta.ejb.Stateless;

import ec.com.antenasur.model.tec.MiembroJRV;
import ec.com.antenasur.model.generic.AbstractFacade;
import ec.com.antenasur.model.tec.Mesa;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.hibernate.Session;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class MiembroJRVFacade extends AbstractFacade<MiembroJRV, Integer> {

    private static final String HQL = " SELECT jrv FROM MiembroJRV jrv";
    private static final String ORDENADO = " ORDER BY jrv.id";

    public MiembroJRVFacade() {
        super(MiembroJRV.class, Integer.class);
    }

    public Set<MiembroJRV> getJRVPorMesa(Mesa mesa) {
        if (mesa == null || mesa.getId() == null) {
            return Collections.emptySet();
        }
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH jrv.mesa m"
                    + " LEFT JOIN FETCH jrv.proceso pro"
                    + " LEFT JOIN FETCH jrv.iglesiaPersona ip"
                    + " LEFT JOIN FETCH ip.persona per"
                    + " LEFT JOIN FETCH ip.iglesia ig"
                    + " LEFT JOIN FETCH jrv.cargo car"
                    + " WHERE m.id = :mesaId AND jrv.estado = TRUE"
                    + ORDENADO;
            TypedQuery<MiembroJRV> query = super.getEntityManager().createQuery(sql, MiembroJRV.class);
            query.setParameter("mesaId", mesa.getId());
            return new LinkedHashSet<>(query.getResultList());
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    public List<MiembroJRV> listarPorMesaProceso(Integer mesaId, Integer procesoId) {
        if (mesaId == null || procesoId == null) {
            return Collections.emptyList();
        }
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH jrv.mesa m"
                    + " LEFT JOIN FETCH jrv.proceso pro"
                    + " LEFT JOIN FETCH jrv.iglesiaPersona ip"
                    + " LEFT JOIN FETCH ip.persona per"
                    + " LEFT JOIN FETCH ip.iglesia ig"
                    + " LEFT JOIN FETCH jrv.cargo car"
                    + " WHERE m.id = :mesaId AND pro.id = :procesoId AND jrv.estado = TRUE"
                    + ORDENADO;
            TypedQuery<MiembroJRV> query = super.getEntityManager().createQuery(sql, MiembroJRV.class);
            query.setParameter("mesaId", mesaId);
            query.setParameter("procesoId", procesoId);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public MiembroJRV buscarPorIglesiaPersonaProceso(Integer iglesiaPersonaId, Integer procesoId) {
        if (iglesiaPersonaId == null || procesoId == null) {
            return null;
        }
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH jrv.mesa m"
                    + " LEFT JOIN FETCH jrv.proceso pro"
                    + " LEFT JOIN FETCH jrv.iglesiaPersona ip"
                    + " LEFT JOIN FETCH ip.persona per"
                    + " LEFT JOIN FETCH ip.iglesia ig"
                    + " LEFT JOIN FETCH jrv.cargo car"
                    + " WHERE ip.id = :iglesiaPersonaId AND pro.id = :procesoId AND jrv.estado = TRUE"
                    + ORDENADO;
            TypedQuery<MiembroJRV> query = super.getEntityManager().createQuery(sql, MiembroJRV.class);
            query.setParameter("iglesiaPersonaId", iglesiaPersonaId);
            query.setParameter("procesoId", procesoId);
            List<MiembroJRV> resultado = query.getResultList();
            return resultado.isEmpty() ? null : resultado.get(0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public MiembroJRV buscarPorMesaCargoProceso(Integer mesaId, Integer cargoId, Integer procesoId) {
        if (mesaId == null || cargoId == null || procesoId == null) {
            return null;
        }
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH jrv.mesa m"
                    + " LEFT JOIN FETCH jrv.proceso pro"
                    + " LEFT JOIN FETCH jrv.cargo car"
                    + " WHERE m.id = :mesaId AND car.id = :cargoId AND pro.id = :procesoId AND jrv.estado = TRUE";
            TypedQuery<MiembroJRV> query = super.getEntityManager().createQuery(sql, MiembroJRV.class);
            query.setParameter("mesaId", mesaId);
            query.setParameter("cargoId", cargoId);
            query.setParameter("procesoId", procesoId);
            List<MiembroJRV> resultado = query.getResultList();
            return resultado.isEmpty() ? null : resultado.get(0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public MiembroJRV buscarPorMesaCargoProcesoIncluyeInactivos(Integer mesaId, Integer cargoId, Integer procesoId) {
        if (mesaId == null || cargoId == null || procesoId == null) {
            return null;
        }
        EntityManager em = super.getEntityManager();
        Session session = em.unwrap(Session.class);
        session.disableFilter(ec.com.antenasur.model.generic.EntidadBase.FILTER_ACTIVE);
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH jrv.mesa m"
                    + " LEFT JOIN FETCH jrv.proceso pro"
                    + " LEFT JOIN FETCH jrv.iglesiaPersona ip"
                    + " LEFT JOIN FETCH jrv.cargo car"
                    + " WHERE m.id = :mesaId AND car.id = :cargoId AND pro.id = :procesoId"
                    + " ORDER BY jrv.estado DESC, jrv.id DESC";
            TypedQuery<MiembroJRV> query = em.createQuery(sql, MiembroJRV.class);
            query.setParameter("mesaId", mesaId);
            query.setParameter("cargoId", cargoId);
            query.setParameter("procesoId", procesoId);
            List<MiembroJRV> resultado = query.getResultList();
            return resultado.isEmpty() ? null : resultado.get(0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            session.enableFilter(ec.com.antenasur.model.generic.EntidadBase.FILTER_ACTIVE);
        }
    }

    public MiembroJRV buscarPorPersonaProceso(Integer personaId, Integer procesoId) {
        if (personaId == null || procesoId == null) {
            return null;
        }
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH jrv.mesa m"
                    + " LEFT JOIN FETCH m.recinto r"
                    + " LEFT JOIN FETCH jrv.proceso pro"
                    + " LEFT JOIN FETCH jrv.iglesiaPersona ip"
                    + " LEFT JOIN FETCH ip.persona per"
                    + " LEFT JOIN FETCH ip.iglesia ig"
                    + " LEFT JOIN FETCH jrv.cargo car"
                    + " WHERE per.id = :personaId AND pro.id = :procesoId AND jrv.estado = TRUE"
                    + ORDENADO;
            TypedQuery<MiembroJRV> query = super.getEntityManager().createQuery(sql, MiembroJRV.class);
            query.setParameter("personaId", personaId);
            query.setParameter("procesoId", procesoId);
            List<MiembroJRV> resultado = query.getResultList();
            return resultado.isEmpty() ? null : resultado.get(0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Set<Integer> listarIglesiaPersonaIdsDesignadas(Integer procesoId) {
        if (procesoId == null) {
            return Collections.emptySet();
        }
        try {
            String sql = "SELECT ip.id FROM MiembroJRV jrv"
                    + " JOIN jrv.proceso pro"
                    + " JOIN jrv.iglesiaPersona ip"
                    + " WHERE pro.id = :procesoId AND jrv.estado = TRUE";
            TypedQuery<Integer> query = super.getEntityManager().createQuery(sql, Integer.class);
            query.setParameter("procesoId", procesoId);
            return new LinkedHashSet<>(query.getResultList());
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptySet();
        }
    }

}
