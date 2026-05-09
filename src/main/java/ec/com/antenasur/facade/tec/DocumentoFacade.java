/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.facade.tec;

import jakarta.ejb.Stateless;

import ec.com.antenasur.model.tec.Documentos;
import ec.com.antenasur.model.generic.AbstractFacade;
import ec.com.antenasur.model.tec.Mesa;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class DocumentoFacade extends AbstractFacade<Documentos, Integer> {

    private static final String HQL = " SELECT d FROM Documentos d";
    private static final String ORDENADO = " ORDER BY d.id";

    public DocumentoFacade() {
        super(Documentos.class, Integer.class);
    }

    public List<Documentos> getDocumentosPorMesa(Mesa mesa) {
        try {
            String sql = HQL + " WHERE d.mesa=:mesa" + ORDENADO;
            TypedQuery<Documentos> query = super.getEntityManager().createQuery(sql, Documentos.class);
            query.setParameter("mesa", mesa);
            List<Documentos> result = query.getResultList();
            if (result != null && !result.isEmpty()) {
                return result;
            }
        } catch (NoResultException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public List<Documentos> getDocumentosPorEntidadYTipoDoc(Integer entidadId, Integer tipoDocId) {
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH d.tipoDocumento  tp"
                    + " WHERE d.entidadId=:entidadId AND tp.id=:tipoDocId" + ORDENADO;
            TypedQuery<Documentos> query = super.getEntityManager().createQuery(sql, Documentos.class);
            query.setParameter("entidadId", entidadId);
            query.setParameter("tipoDocId", tipoDocId);
            List<Documentos> result = query.getResultList();
            if (result != null && !result.isEmpty()) {
                return result;
            }
        } catch (NoResultException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }
    
        public Boolean getTieneDocumentosPorEntidadYTipoDoc(Integer entidadId, Integer tipoDocId) {
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH d.tipoDocumento  tp"
                    + " WHERE d.entidadId=:entidadId AND tp.id=:tipoDocId" + ORDENADO;
            TypedQuery<Documentos> query = super.getEntityManager().createQuery(sql, Documentos.class);
            query.setParameter("entidadId", entidadId);
            query.setParameter("tipoDocId", tipoDocId);
            List<Documentos> result = query.getResultList();
            if (result != null && !result.isEmpty()) {
                return true;
            }
        } catch (NoResultException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    /**
     * Devuelve el conjunto de {@code entidadId} que tienen al menos un
     * documento activo del tipo dado. Pensado para reemplazar un loop con
     * N consultas {@link #getTieneDocumentosPorEntidadYTipoDoc} (1 query por
     * entidad) por una sola query agregada.
     *
     * <p>Antes: 1000 iglesias Ã¢â€ â€™ 1001 queries SQL solo para marcar el flag.
     * Ahora: 2 queries (findAll + esta).
     */
    public Set<Integer> getEntidadesIdsConDocumentos(Integer tipoDocId) {
        Set<Integer> resultado = new HashSet<>();
        if (tipoDocId == null) {
            return resultado;
        }
        try {
            String sql = "SELECT DISTINCT d.entidadId FROM Documentos d"
                    + " WHERE d.tipoDocumento.id = :tipoDocId AND d.estado = TRUE";
            TypedQuery<Integer> query = super.getEntityManager().createQuery(sql, Integer.class);
            query.setParameter("tipoDocId", tipoDocId);
            List<Integer> ids = query.getResultList();
            if (ids != null) {
                resultado.addAll(ids);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultado;
    }

    public Documentos obtenerDocumentoPorWorkspace(String workspace) {
        try {
            String sql = "SELECT e FROM Documentos e WHERE path =:workspace";
            Query query = super.getEntityManager().createQuery(sql);
            query.setParameter("workspace", workspace);
            List<Documentos> resultList = query.getResultList();
            if (resultList != null && !resultList.isEmpty()) {
                return resultList.get(0);
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;
    }
}
