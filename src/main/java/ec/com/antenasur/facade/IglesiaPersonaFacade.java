/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.facade;

import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.IglesiaPersona;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import ec.com.antenasur.model.generic.AbstractFacade;

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

    /**
     * Devuelve el vínculo iglesia-persona vigente más reciente para una
     * persona dada. "Vigente" = estado activo. Si la persona pertenece a
     * varias iglesias históricamente, retorna la última registrada.
     */
    public IglesiaPersona getVigentePorPersonaId(Integer personaId) {
        if (personaId == null) {
            return null;
        }
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH ip.iglesia i"
                    + " WHERE ip.persona.id = :personaId AND ip.estado = TRUE"
                    + " ORDER BY ip.id DESC";
            TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
            query.setParameter("personaId", personaId);
            query.setMaxResults(1);
            List<IglesiaPersona> result = query.getResultList();
            return (result != null && !result.isEmpty()) ? result.get(0) : null;
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<IglesiaPersona> getPersonasIglesiasPorParroquia(Geograp parroquia) {
        if (parroquia == null) {
            return java.util.Collections.emptyList();
        }
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH ip.iglesia igl"
                    + " LEFT JOIN FETCH ip.persona p"
                    + " WHERE igl.ubicacion = :parroquia AND ip.estado = TRUE"
                    + " ORDER BY ip.id";
            TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
            query.setParameter("parroquia", parroquia);
            List<IglesiaPersona> result = query.getResultList();
            return result != null ? result : java.util.Collections.<IglesiaPersona>emptyList();
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    public List<IglesiaPersona> getPersonasIglesiasPorIglesia(int iglesiaId) {
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH ip.iglesia i"
                    + " LEFT JOIN FETCH ip.persona p"
                    + " WHERE i.id = :iglesiaId AND ip.estado = TRUE"
                    + " ORDER BY ip.id";
            TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
            query.setParameter("iglesiaId", iglesiaId);
            List<IglesiaPersona> result = query.getResultList();
            return result != null ? result : java.util.Collections.<IglesiaPersona>emptyList();
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Trae IglesiaPersona activos por parroquia(s) hidratando en una sola
     * query las relaciones que la vista/DTO consultan después
     * ({@code iglesia}, {@code iglesia.ubicacion}, {@code persona}). Evita
     * N+1: sin estos JOIN FETCH, mapear cada IglesiaPersona a DTO disparaba
     * una query por persona y otra por iglesia, multiplicando el tiempo de
     * respuesta hasta sobrepasar el timeout JTA (300s) y romper la
     * transacción.
     *
     * <p>Filtra por {@code ip.estado = TRUE} para excluir soft-deleted y
     * limita por relación con la lista de parroquias.
     */
    public List<IglesiaPersona> getIglesiasPersonasPorParroquias(List<Geograp> parroquias) {
        if (parroquias == null || parroquias.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH ip.iglesia igl"
                    + " LEFT JOIN FETCH igl.ubicacion ub"
                    + " LEFT JOIN FETCH ip.persona p"
                    + " WHERE ub IN :parroquias AND ip.estado = TRUE"
                    + " ORDER BY ip.id";
            TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
            query.setParameter("parroquias", parroquias);
            List<IglesiaPersona> result = query.getResultList();
            return result != null ? result : java.util.Collections.<IglesiaPersona>emptyList();
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
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
