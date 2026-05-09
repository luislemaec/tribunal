/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.facade;

import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.IglesiaPersona;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import ec.com.antenasur.model.generic.AbstractFacade;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class IglesiaPersonaFacade extends AbstractFacade<IglesiaPersona, Integer> {

    static final String HQL = "SELECT ip FROM IglesiaPersona ip";

    public IglesiaPersonaFacade() {
        super(IglesiaPersona.class, Integer.class);
    }

    /**
     * Devuelve el vÃƒÂ­nculo iglesia-persona vigente mÃƒÂ¡s reciente para una
     * persona dada. "Vigente" = estado activo. Si la persona pertenece a
     * varias iglesias histÃƒÂ³ricamente, retorna la ÃƒÂºltima registrada.
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
     * query las relaciones que la vista/DTO consultan despuÃƒÂ©s
     * ({@code iglesia}, {@code iglesia.ubicacion}, {@code persona}). Evita
     * N+1: sin estos JOIN FETCH, mapear cada IglesiaPersona a DTO disparaba
     * una query por persona y otra por iglesia, multiplicando el tiempo de
     * respuesta hasta sobrepasar el timeout JTA (300s) y romper la
     * transacciÃƒÂ³n.
     *
     * <p>Filtra por {@code ip.estado = TRUE} para excluir soft-deleted y
     * limita por relaciÃƒÂ³n con la lista de parroquias.
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

    /**
     * Devuelve el vÃƒÂ­nculo activo mÃƒÂ¡s reciente para la persona identificada
     * por su DOCUMENTO (cÃƒÂ©dula), independiente del id interno de la persona.
     *
     * <p>Pensado para entornos donde existen filas duplicadas en
     * {@code tb_persona} con el mismo documento (caso real en producciÃƒÂ³n).
     * El mÃƒÂ©todo {@link #getVigentePorPersonaId(Integer)} requiere conocer el
     * id exacto, pero {@code finByPersonaDocument} devuelve la persona con
     * id ASC y el vÃƒÂ­nculo en {@code tb_iglesia_persona} podrÃƒÂ­a apuntar al
     * id duplicado mayor Ã¢â‚¬â€ generando "sin iglesia" falso. Esta variante
     * resuelve por documento y evita ese problema.
     */
    public IglesiaPersona getVigentePorDocumentoPersona(String documento) {
        if (documento == null || documento.trim().isEmpty()) {
            return null;
        }
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH ip.iglesia i"
                    + " LEFT JOIN FETCH ip.persona p"
                    + " WHERE p.documento = :documento"
                    + "   AND ip.estado = TRUE"
                    + "   AND p.estado = TRUE"
                    + " ORDER BY ip.id DESC";
            TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
            query.setParameter("documento", documento.trim());
            query.setMaxResults(1);
            List<IglesiaPersona> result = query.getResultList();
            return (result != null && !result.isEmpty()) ? result.get(0) : null;
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Devuelve el vÃƒÂ­nculo activo entre la iglesia y la persona indicadas, o
     * {@code null} si no existe ninguno. ÃƒÅ¡til para garantizar idempotencia al
     * crear el vÃƒÂ­nculo desde el flujo de asignaciÃƒÂ³n de admins.
     */
    public IglesiaPersona findByIglesiaAndPersona(Integer iglesiaId, Integer personaId) {
        if (iglesiaId == null || personaId == null) {
            return null;
        }
        try {
            String sql = HQL
                    + " WHERE ip.iglesia.id = :iglesiaId"
                    + "   AND ip.persona.id = :personaId"
                    + "   AND ip.estado = TRUE"
                    + " ORDER BY ip.id DESC";
            TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
            query.setParameter("iglesiaId", iglesiaId);
            query.setParameter("personaId", personaId);
            query.setMaxResults(1);
            List<IglesiaPersona> result = query.getResultList();
            return (result != null && !result.isEmpty()) ? result.get(0) : null;
        } catch (NoResultException e) {
            return null;
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
