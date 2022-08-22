/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.service;

import ec.com.antenasur.domain.Iglesia;
import ec.com.antenasur.domain.Mesa;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import ec.com.antenasur.domain.Padron;
import ec.com.antenasur.domain.generic.AbstractFacade;
import javax.persistence.Query;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class PadronFacade extends AbstractFacade<Padron, Integer> {

    private static final String HQL = " FROM Padron p";
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

}
