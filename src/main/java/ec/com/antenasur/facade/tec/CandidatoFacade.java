/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.facade.tec;

import jakarta.ejb.Stateless;

import ec.com.antenasur.model.tec.Candidato;
import ec.com.antenasur.model.tec.CatalogoGeneral;
import ec.com.antenasur.model.tec.Lista;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.model.generic.AbstractFacade;
import java.util.List;
import jakarta.persistence.TypedQuery;
import org.hibernate.Filter;
import org.hibernate.Session;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class CandidatoFacade extends AbstractFacade<Candidato, Integer> {

    private static final String HQL = " SELECT c FROM Candidato c";
    private static final String ORDENADO = " ORDER BY c.id";

    public CandidatoFacade() {
        super(Candidato.class, Integer.class);
    }

    public Candidato getPorCargoYLista(CatalogoGeneral cargo, Lista listaSeleccionado) {
        return getPorCargoListaYProceso(cargo, listaSeleccionado, null);
    }

    public Candidato getPorCargoListaYProceso(CatalogoGeneral cargo, Lista listaSeleccionado, ProcesoElectoral proceso) {
        try {
            String sql = HQL + " WHERE c.cargo=:cargo AND c.lista =:lista ";
            if (proceso != null) {
                sql += " AND c.proceso = :proceso";
            }
            TypedQuery<Candidato> query = super.getEntityManager().createQuery(sql, Candidato.class);
            query.setParameter("cargo", cargo);
            query.setParameter("lista", listaSeleccionado);
            if (proceso != null) {
                query.setParameter("proceso", proceso);
            }
            List<Candidato> resultList = query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList.get(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    /**
     * Carga en una sola consulta los candidatos de una lista y proceso con
     * todos los datos requeridos por la vista. Evita una consulta por cargo.
     */
    public List<Candidato> listarPorListaProcesoYCargos(Integer listaId, Integer procesoId, List<Integer> cargoIds) {
        if (listaId == null || procesoId == null || cargoIds == null || cargoIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        String sql = HQL
                + " JOIN FETCH c.lista l"
                + " JOIN FETCH c.proceso p"
                + " JOIN FETCH c.cargo ca"
                + " JOIN FETCH c.iglesiaPersona ip"
                + " LEFT JOIN FETCH ip.persona pe"
                + " LEFT JOIN FETCH ip.iglesia i"
                + " WHERE c.lista.id = :listaId"
                + " AND c.proceso.id = :procesoId"
                + " AND c.cargo.id IN :cargoIds"
                + " AND c.estado = TRUE"
                + " ORDER BY ca.orden, c.id";
        TypedQuery<Candidato> query = super.getEntityManager().createQuery(sql, Candidato.class);
        query.setParameter("listaId", listaId);
        query.setParameter("procesoId", procesoId);
        query.setParameter("cargoIds", cargoIds);
        return query.getResultList();
    }

    /** Busca la plaza lógica, incluso si fue dada de baja. */
    public Candidato buscarPorListaCargoProcesoIncluyendoInactivos(
            Integer listaId, Integer cargoId, Integer procesoId) {
        if (listaId == null || cargoId == null || procesoId == null) {
            return null;
        }
        Session session = super.getEntityManager().unwrap(Session.class);
        Filter filtroActivo = session.getEnabledFilter("filterActive");
        if (filtroActivo != null) {
            session.disableFilter("filterActive");
        }
        try {
            TypedQuery<Candidato> query = session.createQuery(
                    HQL + " WHERE c.lista.id = :listaId AND c.cargo.id = :cargoId"
                    + " AND c.proceso.id = :procesoId ORDER BY c.estado DESC, c.id", Candidato.class);
            query.setParameter("listaId", listaId);
            query.setParameter("cargoId", cargoId);
            query.setParameter("procesoId", procesoId);
            query.setMaxResults(1);
            List<Candidato> resultado = query.getResultList();
            return resultado.isEmpty() ? null : resultado.get(0);
        } finally {
            if (filtroActivo != null) {
                session.enableFilter("filterActive");
            }
        }
    }

    public long contarActivosPorLista(Integer listaId) {
        if (listaId == null) {
            return 0;
        }
        TypedQuery<Long> query = super.getEntityManager().createQuery(
                "SELECT COUNT(c) FROM Candidato c WHERE c.lista.id = :listaId AND c.estado = TRUE",
                Long.class);
        query.setParameter("listaId", listaId);
        return query.getSingleResult();
    }

    public boolean existePersonaActivaEnListaProceso(Integer listaId, Integer procesoId,
            Integer iglesiaPersonaId, Integer candidatoIdExcluir) {
        if (listaId == null || procesoId == null || iglesiaPersonaId == null) {
            return false;
        }
        String hql = "SELECT COUNT(c) FROM Candidato c"
                + " WHERE c.lista.id = :listaId AND c.proceso.id = :procesoId"
                + " AND c.iglesiaPersona.id = :iglesiaPersonaId AND c.estado = TRUE";
        if (candidatoIdExcluir != null) {
            hql += " AND c.id <> :candidatoIdExcluir";
        }
        TypedQuery<Long> query = getEntityManager().createQuery(hql, Long.class);
        query.setParameter("listaId", listaId);
        query.setParameter("procesoId", procesoId);
        query.setParameter("iglesiaPersonaId", iglesiaPersonaId);
        if (candidatoIdExcluir != null) {
            query.setParameter("candidatoIdExcluir", candidatoIdExcluir);
        }
        return query.getSingleResult() > 0L;
    }

}
