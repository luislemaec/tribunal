/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.facade.tec;

import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.model.tec.Mesa;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import ec.com.antenasur.model.tec.Padron;
import ec.com.antenasur.model.generic.AbstractFacade;
import jakarta.persistence.Query;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class PadronFacade extends AbstractFacade<Padron, Integer> {

    private static final String HQL = " SELECT p FROM Padron p";
    private static final String ACTIVOS = " p.estado = TRUE";
    private static final String ORDENADO = " ORDER BY p.id";

    public PadronFacade() {
        super(Padron.class, Integer.class);
    }

    public List<Padron> getAllOrderbyId() {
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH p.mesa m"
                    + " LEFT JOIN FETCH m.recinto r"
                    + " LEFT JOIN FETCH m.ubicacion u"
                    + " LEFT JOIN FETCH p.iglesiaPersona ip"
                    + " LEFT JOIN FETCH ip.iglesia i"
                    + " LEFT JOIN FETCH ip.persona prsn"
                    + " LEFT JOIN FETCH p.periodo prd"
                    + " LEFT JOIN FETCH p.proceso pro"
                    + " WHERE " + ACTIVOS + ORDENADO;
            TypedQuery<Padron> query = super.getEntityManager().createQuery(sql, Padron.class);
            List<Padron> result = query.getResultList();
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
     * @param nombreMesa
     * @return
     */
    public Padron buscaPadronPorMesa(String nombreMesa) {
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH p.mesa m"
                    + " LEFT JOIN FETCH m.recinto r"
                    + " LEFT JOIN FETCH m.ubicacion u"
                    + " LEFT JOIN FETCH p.iglesiaPersona ip"
                    + " LEFT JOIN FETCH ip.iglesia i"
                    + " LEFT JOIN FETCH ip.persona prsn"
                    + " LEFT JOIN FETCH p.periodo prd"
                    + " LEFT JOIN FETCH p.proceso pro"
                    + " WHERE m.nombre=:nombreMesa AND " + ACTIVOS;
            TypedQuery<Padron> query = super.getEntityManager().createQuery(sql, Padron.class);
            query.setParameter("nombreMesa", nombreMesa);
            Padron result = query.getSingleResult();
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
     * @param nombreRecinto
     * @return
     */
    public Padron buscaPadronPorRecinto(String nombreRecinto) {
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH p.mesa m"
                    + " LEFT JOIN FETCH m.recinto r"
                    + " LEFT JOIN FETCH m.ubicacion u"
                    + " LEFT JOIN FETCH p.iglesiaPersona ip"
                    + " LEFT JOIN FETCH ip.iglesia i"
                    + " LEFT JOIN FETCH ip.persona prsn"
                    + " LEFT JOIN FETCH p.periodo prd"
                    + " LEFT JOIN FETCH p.proceso pro"
                    + " WHERE r.nombre=:nombreRecinto AND " + ACTIVOS;
            TypedQuery<Padron> query = super.getEntityManager().createQuery(sql, Padron.class);
            query.setParameter("nombreRecinto", nombreRecinto);
            Padron result = query.getSingleResult();
            if (result != null) {
                return result;
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;
    }

    public List<Padron> getPadronsEnParroquias(List<Integer> listaIdParroquias) {
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH p.mesa m"
                    + " LEFT JOIN FETCH m.recinto r"
                    + " LEFT JOIN FETCH m.ubicacion u"
                    + " LEFT JOIN FETCH p.iglesiaPersona ip"
                    + " LEFT JOIN FETCH ip.iglesia i"
                    + " LEFT JOIN FETCH ip.persona prsn"
                    + " LEFT JOIN FETCH p.periodo prd"
                    + " LEFT JOIN FETCH p.proceso pro"
                    + " WHERE r.id IN :ids AND " + ACTIVOS + ORDENADO;
            TypedQuery<Padron> query = super.getEntityManager().createQuery(sql, Padron.class);
            query.setParameter("ids", listaIdParroquias);
            List<Padron> result = query.getResultList();
            if (result.size() > 0) {
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public Padron buscaPorPesonaPeriodoIglesia(Integer idIglesiaPersona, Integer idPeriodo) {
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH p.iglesiaPersona ip"
                    + " LEFT JOIN FETCH p.periodo prd"
                    + " WHERE ip.id IN :idIglesiaPersona AND prd.id IN : idPeriodo AND " + ACTIVOS + ORDENADO;
            TypedQuery<Padron> query = super.getEntityManager().createQuery(sql, Padron.class);
            query.setParameter("idIglesiaPersona", idIglesiaPersona);
            query.setParameter("idPeriodo", idPeriodo);
            List<Padron> result = query.getResultList();
            if (result.size() > 0) {
                return result.get(0);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public Padron buscaPorPersonaProcesoIglesia(Integer idIglesiaPersona, Integer idProceso) {
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH p.iglesiaPersona ip"
                    + " LEFT JOIN FETCH p.proceso pro"
                    + " WHERE ip.id = :idIglesiaPersona AND pro.id = :idProceso AND " + ACTIVOS + ORDENADO;
            TypedQuery<Padron> query = super.getEntityManager().createQuery(sql, Padron.class);
            query.setParameter("idIglesiaPersona", idIglesiaPersona);
            query.setParameter("idProceso", idProceso);
            List<Padron> result = query.getResultList();
            if (result.size() > 0) {
                return result.get(0);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public List<Integer> obtieneIglesiasEnPadronCompletasPorUbicacion(List<Integer> idParroquias) {
        try {
            String parametro = idParroquias.toString().replace("[", "(");
            parametro = parametro.replace("]", ")");

            Query query = super.getEntityManager().createNativeQuery(
                    "SELECT v1.igl_id FROM tec.vw_total_miembros_por_iglesias v1 "
                    + "LEFT JOIN tec.vw_total_miembos_iglesias_padron v2 on v1.igl_id=v2.igl_id "
                    + "WHERE v1.total = v2.total AND v1.gelo_id IN " + parametro + " ;");
            List<Integer> result = query.getResultList();
            if (result != null && !result.isEmpty()) {
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public List<Integer> obtieneIglesiasEnPadronPorUbicacionYProceso(List<Integer> idParroquias, Integer procesoId) {
        if (idParroquias == null || idParroquias.isEmpty() || procesoId == null) {
            return java.util.Collections.emptyList();
        }
        try {
            String sql = "SELECT DISTINCT i.id FROM Padron p"
                    + " JOIN p.iglesiaPersona ip"
                    + " JOIN ip.iglesia i"
                    + " JOIN i.ubicacion ub"
                    + " JOIN p.proceso pro"
                    + " WHERE ub.id IN :idParroquias"
                    + " AND pro.id = :procesoId"
                    + " AND " + ACTIVOS
                    + " ORDER BY i.id";
            TypedQuery<Integer> query = super.getEntityManager().createQuery(sql, Integer.class);
            query.setParameter("idParroquias", idParroquias);
            query.setParameter("procesoId", procesoId);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    public boolean existeIglesiaEnPadronProceso(Integer iglesiaId, Integer procesoId) {
        if (iglesiaId == null || procesoId == null) {
            return false;
        }
        try {
            String sql = "SELECT COUNT(p.id) FROM Padron p"
                    + " JOIN p.iglesiaPersona ip"
                    + " JOIN ip.iglesia i"
                    + " JOIN p.proceso pro"
                    + " WHERE i.id = :iglesiaId"
                    + " AND pro.id = :procesoId"
                    + " AND " + ACTIVOS;
            TypedQuery<Long> query = super.getEntityManager().createQuery(sql, Long.class);
            query.setParameter("iglesiaId", iglesiaId);
            query.setParameter("procesoId", procesoId);
            Long total = query.getSingleResult();
            return total != null && total > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Padron> getPadronPorMesas(List<Mesa> listaMesas) {
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH p.mesa m"
                    + " LEFT JOIN FETCH p.iglesiaPersona ip"
                    + " LEFT JOIN FETCH ip.iglesia i"
                    + " WHERE m IN :listaMesas AND " + ACTIVOS ;
            TypedQuery<Padron> query = super.getEntityManager().createQuery(sql, Padron.class);
            query.setParameter("listaMesas", listaMesas);
            List<Padron> result = query.getResultList();
            if (result.size() > 0) {
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    /**
     * Variante de {@link #getPadronPorMesas(List)} que recibe ids de mesa
     * directamente, evitando el N+1 de resolver cada {@code Mesa} antes de
     * la query principal. Devuelve lista vacÃƒÂ­a (no null) cuando no hay
     * resultados Ã¢â‚¬” mÃƒ¡s predecible para el caller.
     */
    public List<Padron> getPadronPorMesaIds(List<Integer> mesaIds) {
        if (mesaIds == null || mesaIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH p.mesa m"
                    + " LEFT JOIN FETCH p.iglesiaPersona ip"
                    + " LEFT JOIN FETCH ip.iglesia i"
                    + " WHERE m.id IN :mesaIds AND " + ACTIVOS;
            TypedQuery<Padron> query = super.getEntityManager().createQuery(sql, Padron.class);
            query.setParameter("mesaIds", mesaIds);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    public List<Padron> getPadronesPorIglesiaMesaProceso(Integer iglesiaId, Integer mesaId, Integer procesoId) {
        if (iglesiaId == null || mesaId == null || procesoId == null) {
            return java.util.Collections.emptyList();
        }
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH p.mesa m"
                    + " LEFT JOIN FETCH p.iglesiaPersona ip"
                    + " LEFT JOIN FETCH ip.iglesia i"
                    + " LEFT JOIN FETCH p.proceso pro"
                    + " WHERE i.id = :iglesiaId"
                    + " AND m.id = :mesaId"
                    + " AND pro.id = :procesoId"
                    + " AND " + ACTIVOS
                    + ORDENADO;
            TypedQuery<Padron> query = super.getEntityManager().createQuery(sql, Padron.class);
            query.setParameter("iglesiaId", iglesiaId);
            query.setParameter("mesaId", mesaId);
            query.setParameter("procesoId", procesoId);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

}
