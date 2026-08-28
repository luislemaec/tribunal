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
        if (mesa == null || mesa.getId() == null) {
            return java.util.Collections.emptyList();
        }
        try {
            String sql = HQL
                    + " JOIN FETCH d.tipoDocumento tp"
                    + " LEFT JOIN FETCH d.proceso pro"
                    + " LEFT JOIN FETCH d.recinto rec"
                    + " LEFT JOIN FETCH d.mesa m"
                    + " WHERE m.id=:mesaId AND d.estado=TRUE ORDER BY d.id DESC";
            TypedQuery<Documentos> query = super.getEntityManager().createQuery(sql, Documentos.class);
            query.setParameter("mesaId", mesa.getId());
            return query.getResultList();
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    public List<Documentos> listarPorMesaProceso(Integer mesaId, Integer procesoId) {
        if (mesaId == null || procesoId == null) {
            return java.util.Collections.emptyList();
        }
        TypedQuery<Documentos> query = getEntityManager().createQuery(
                HQL
                + " JOIN FETCH d.tipoDocumento tp"
                + " LEFT JOIN FETCH d.documentoOrigen origen"
                + " LEFT JOIN FETCH d.proceso pro"
                + " LEFT JOIN FETCH d.recinto rec"
                + " LEFT JOIN FETCH d.mesa m"
                + " WHERE m.id = :mesaId AND pro.id = :procesoId"
                + " AND d.estado = TRUE ORDER BY d.id DESC", Documentos.class);
        query.setParameter("mesaId", mesaId);
        query.setParameter("procesoId", procesoId);
        return query.getResultList();
    }

    public List<Documentos> getDocumentosPorEntidadYTipoDoc(Integer entidadId, Integer tipoDocId) {
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH d.tipoDocumento  tp"
                    + " LEFT JOIN FETCH d.documentoOrigen origen"
                    + " WHERE d.entidadId=:entidadId"
                    + " AND tp.id=:tipoDocId"
                    + " AND d.estado = TRUE"
                    + " ORDER BY d.id DESC";
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
        if (entidadId == null || tipoDocId == null) {
            return false;
        }
        try {
            String sql = "SELECT COUNT(d) FROM Documentos d"
                    + " WHERE d.entidadId = :entidadId"
                    + " AND d.tipoDocumento.id = :tipoDocId"
                    + " AND d.estado = TRUE";
            TypedQuery<Long> query = super.getEntityManager().createQuery(sql, Long.class);
            query.setParameter("entidadId", entidadId);
            query.setParameter("tipoDocId", tipoDocId);
            Long total = query.getSingleResult();
            return total != null && total > 0L;
        } catch (Exception e) {
            return false;
        }
    }

    public long contarDocumentosPorEntidadYTipoDoc(Integer entidadId, Integer tipoDocId) {
        if (entidadId == null || tipoDocId == null) {
            return 0L;
        }
        try {
            String sql = "SELECT COUNT(d) FROM Documentos d"
                    + " WHERE d.entidadId = :entidadId"
                    + " AND d.tipoDocumento.id = :tipoDocId"
                    + " AND d.estado = TRUE";
            TypedQuery<Long> query = super.getEntityManager().createQuery(sql, Long.class);
            query.setParameter("entidadId", entidadId);
            query.setParameter("tipoDocId", tipoDocId);
            Long total = query.getSingleResult();
            return total != null ? total : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Devuelve el conjunto de {@code entidadId} que tienen al menos un
     * documento activo del tipo dado. Pensado para reemplazar un loop con
     * N consultas {@link #getTieneDocumentosPorEntidadYTipoDoc} (1 query por
     * entidad) por una sola query agregada.
     *
     * <p>Antes: 1000 iglesias Ã¢â€ ’ 1001 queries SQL solo para marcar el flag.
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

    public Documentos buscarActivoPorEntidadTipoYContexto(
            Integer entidadId, Integer tipoDocumentoId, String contextoHash) {
        if (entidadId == null || tipoDocumentoId == null || contextoHash == null || contextoHash.isBlank()) {
            return null;
        }
        TypedQuery<Documentos> query = getEntityManager().createQuery(
                HQL + " LEFT JOIN FETCH d.tipoDocumento"
                + " WHERE d.entidadId = :entidadId"
                + " AND d.tipoDocumento.id = :tipoDocumentoId"
                + " AND d.contextoHash = :contextoHash"
                + " AND d.estado = TRUE ORDER BY d.id DESC", Documentos.class);
        query.setParameter("entidadId", entidadId);
        query.setParameter("tipoDocumentoId", tipoDocumentoId);
        query.setParameter("contextoHash", contextoHash);
        query.setMaxResults(1);
        List<Documentos> resultado = query.getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    public Documentos buscarFirmadoActivoPorOrigen(Integer documentoOrigenId, Integer tipoDocumentoId) {
        if (documentoOrigenId == null || tipoDocumentoId == null) {
            return null;
        }
        TypedQuery<Documentos> query = getEntityManager().createQuery(
                HQL + " LEFT JOIN FETCH d.tipoDocumento"
                + " LEFT JOIN FETCH d.documentoOrigen"
                + " WHERE d.documentoOrigen.id = :documentoOrigenId"
                + " AND d.tipoDocumento.id = :tipoDocumentoId"
                + " AND d.estado = TRUE ORDER BY d.id DESC", Documentos.class);
        query.setParameter("documentoOrigenId", documentoOrigenId);
        query.setParameter("tipoDocumentoId", tipoDocumentoId);
        query.setMaxResults(1);
        List<Documentos> resultado = query.getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }
}
