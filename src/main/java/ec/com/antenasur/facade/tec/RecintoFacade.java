/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.facade.tec;

import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.tec.Recinto;
import ec.com.antenasur.model.generic.AbstractFacade;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class RecintoFacade extends AbstractFacade<Recinto, Integer> {

    private static final String HQL = " SELECT r FROM Recinto r";    
    private static final String ORDENADO = " ORDER BY r.id";

    public RecintoFacade() {
        super(Recinto.class, Integer.class);
    }


    /**
     *
     * @param nombreRecinto
     * @return
     */
    public Recinto buscaRecintoPorNombre(String nombreRecinto) {
        try {
            String sql = HQL + " WHERE r.nombre=:nombreRecinto ";
            TypedQuery<Recinto> query = super.getEntityManager().createQuery(sql, Recinto.class);
            query.setParameter("nombreRecinto", nombreRecinto);
            Recinto result = query.getSingleResult();
            if (result != null) {
                return result;
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;
    }

    public List<Recinto> getRecintosPorParroquias(List<Geograp> parroquias) {
        try {
            String sql = HQL + " LEFT JOIN FETCH r.ubicacion ub"
                    + " WHERE ub IN :parroquias  " + ORDENADO;
            TypedQuery<Recinto> query = super.getEntityManager().createQuery(sql, Recinto.class);
            query.setParameter("parroquias", parroquias);
            List<Recinto> result = query.getResultList();
            if (result.size() > 0) {
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    /** Carga solo recintos con información electoral para el proceso indicado. */
    public List<Recinto> listarPorProceso(Integer procesoId) {
        if (procesoId == null) {
            return java.util.Collections.emptyList();
        }
        String hql = "SELECT DISTINCT r FROM Mesa m"
                + " JOIN m.recinto r"
                + " LEFT JOIN FETCH r.ubicacion parroquia"
                + " LEFT JOIN FETCH parroquia.geograp canton"
                + " LEFT JOIN FETCH canton.geograp provincia"
                + " WHERE m.estado = TRUE AND r.estado = TRUE AND ("
                + " EXISTS (SELECT p.id FROM Padron p WHERE p.mesa.id = m.id"
                + " AND p.proceso.id = :procesoId AND p.estado = TRUE)"
                + " OR EXISTS (SELECT ec.id FROM EscrutinioCabecera ec WHERE ec.mesa.id = m.id"
                + " AND ec.proceso.id = :procesoId AND ec.estado = TRUE)"
                + " OR EXISTS (SELECT j.id FROM MiembroJRV j WHERE j.mesa.id = m.id"
                + " AND j.proceso.id = :procesoId AND j.estado = TRUE)"
                + " OR EXISTS (SELECT d.id FROM Documentos d WHERE d.mesa.id = m.id"
                + " AND d.proceso.id = :procesoId AND d.estado = TRUE))"
                + " ORDER BY r.nombre";
        TypedQuery<Recinto> query = getEntityManager().createQuery(hql, Recinto.class);
        query.setParameter("procesoId", procesoId);
        return query.getResultList();
    }

    /**
     * Revisiones Envers de un recinto ({@code tec.recintos_aud} unida a {@code tec.tec_auditoria})
     * en orden cronológico, en una sola consulta nativa para incluir bajas sin depender del filtro
     * de estado. Columnas: rev, revtype, fecha, nombre, parroquia, estado, usuario, cantón y
     * provincia. Igual que en Iglesias, la fecha y el usuario salen de la revisión (Envers no
     * escribe f_crea/u_crea en la tabla _AUD) y cantón/provincia se derivan de la parroquia.
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> listarRevisionesAuditoria(Integer recintoId) {
        if (recintoId == null) {
            return java.util.Collections.emptyList();
        }
        String sql = "SELECT a.rev, a.revtype, COALESCE(r.audit_date, r.create_date),"
                + " a.rec_nombre, g.gelo_name, a.estado, COALESCE(r.update_user, r.create_user),"
                + " gc.gelo_name, gp.gelo_name"
                + " FROM tec.recintos_aud a"
                + " LEFT JOIN tec.tec_auditoria r ON r.aud_id = a.rev"
                + " LEFT JOIN public.tb_geograp g ON g.gelo_id = a.gelo_id"
                + " LEFT JOIN public.tb_geograp gc ON gc.gelo_id = g.gelo_parent_id"
                + " LEFT JOIN public.tb_geograp gp ON gp.gelo_id = gc.gelo_parent_id"
                + " WHERE a.rec_id = :id ORDER BY a.rev";
        return getEntityManager().createNativeQuery(sql).setParameter("id", recintoId).getResultList();
    }

}
