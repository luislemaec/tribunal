/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.facade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.ejb.Stateless;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import ec.com.antenasur.model.Persona;
import ec.com.antenasur.model.generic.AbstractFacade;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class PersonaFacade extends AbstractFacade<Persona, Integer> {

    public PersonaFacade() {
        super(Persona.class, Integer.class);
    }


    public List<Persona> getByRuc() {
        try {
            String sql = "SELECT e FROM Persona e WHERE estado =TRUE ORDER BY id";
            TypedQuery<Persona> query = super.getEntityManager().createQuery(sql, Persona.class);
            List<Persona> result = (List<Persona>) query.getResultList();
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
     * @param id_type_Persona
     * @return
     */
    /**
     * Busca una persona por documento. Si hay duplicados en la BD (caso real
     * detectado en producciÃƒÂ³n) retorna el primero por id ascendente, en lugar
     * de lanzar {@link jakarta.persistence.NonUniqueResultException}. La
     * deduplicaciÃƒÂ³n de personas duplicadas debe resolverse en BD; mientras
     * tanto, el login y demÃƒ¡s flujos no deben caerse.
     */
    public Persona finByPersonaDocument(String documento) {
        try {
            String sql = "select p from Persona p WHERE p.documento=:documento AND p.estado =true ORDER BY p.id ASC";
            TypedQuery<Persona> query = super.getEntityManager().createQuery(sql, Persona.class);
            query.setParameter("documento", documento);
            query.setMaxResults(1);
            List<Persona> result = query.getResultList();
            return result.isEmpty() ? null : result.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    public Persona searchPersonaByUserId(Integer user_id) {
        try {
            String sql = "select p from Persona p WHERE p.usuarios in :user_id AND p.estado =true ORDER BY p.id ASC";
            TypedQuery<Persona> query = super.getEntityManager().createQuery(sql, Persona.class);
            query.setParameter("user_id", user_id);
            query.setMaxResults(1);
            List<Persona> result = query.getResultList();
            return result.isEmpty() ? null : result.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    public Persona buscarPorCedula(String documento) {
        try {
            String sql = "select p from Persona p WHERE p.documento = :documento AND p.estado=true ORDER BY p.id ASC";
            TypedQuery<Persona> query = super.getEntityManager().createQuery(sql, Persona.class);
            query.setParameter("documento", documento);
            query.setMaxResults(1);
            List<Persona> result = query.getResultList();
            return result.isEmpty() ? null : result.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    /** Cuenta personas activas por cédula recortada en una sola consulta. */
    public Map<String, Integer> contarPersonasActivasPorDocumentos(Collection<String> documentos) {
        Map<String, Integer> resultado = new LinkedHashMap<>();
        if (documentos == null || documentos.isEmpty()) {
            return resultado;
        }
        String hql = "SELECT TRIM(p.documento), COUNT(p.id)"
                + " FROM Persona p"
                + " WHERE p.estado = TRUE"
                + "   AND p.documento IS NOT NULL"
                + "   AND TRIM(p.documento) IN :documentos"
                + " GROUP BY TRIM(p.documento)";
        List<Object[]> filas = super.getEntityManager().createQuery(hql, Object[].class)
                .setParameter("documentos", documentos)
                .getResultList();
        for (Object[] fila : filas) {
            resultado.put((String) fila[0], ((Number) fila[1]).intValue());
        }
        return resultado;
    }

    /** Verifica si una cédula ya corresponde a otra persona activa. */
    public boolean existeOtraPersonaActivaConDocumento(String documento, Integer personaId) {
        if (documento == null || documento.trim().isEmpty()) {
            return false;
        }
        String hql = "SELECT COUNT(p.id) FROM Persona p"
                + " WHERE p.estado = TRUE"
                + "   AND TRIM(p.documento) = :documento"
                + (personaId != null ? " AND p.id <> :personaId" : "");
        TypedQuery<Long> query = super.getEntityManager().createQuery(hql, Long.class);
        query.setParameter("documento", documento.trim());
        if (personaId != null) {
            query.setParameter("personaId", personaId);
        }
        return query.getSingleResult() > 0;
    }

}
