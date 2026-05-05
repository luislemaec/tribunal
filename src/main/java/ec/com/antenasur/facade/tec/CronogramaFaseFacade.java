package ec.com.antenasur.facade.tec;

import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import ec.com.antenasur.model.generic.AbstractFacade;
import ec.com.antenasur.model.tec.CronogramaFase;

@Stateless
public class CronogramaFaseFacade extends AbstractFacade<CronogramaFase, Integer> {

    public CronogramaFaseFacade() {
        super(CronogramaFase.class, Integer.class);
    }

    public List<CronogramaFase> listarPorProceso(Integer procesoId) {
        String hql = "FROM CronogramaFase f WHERE f.proceso.id = :pid ORDER BY f.orden, f.fechaInicio";
        TypedQuery<CronogramaFase> q = super.getEntityManager().createQuery(hql, CronogramaFase.class);
        q.setParameter("pid", procesoId);
        return q.getResultList();
    }

    /**
     * Devuelve la fase vigente (now() entre fechaInicio y fechaFin) del
     * proceso activo. Si hay solapamiento, retorna la de menor {@code orden}.
     */
    public CronogramaFase getVigentePorProceso(Integer procesoId) {
        try {
            String hql = "FROM CronogramaFase f"
                    + " WHERE f.proceso.id = :pid"
                    + " AND :ahora BETWEEN f.fechaInicio AND f.fechaFin"
                    + " ORDER BY f.orden ASC, f.id ASC";
            TypedQuery<CronogramaFase> q = super.getEntityManager().createQuery(hql, CronogramaFase.class);
            q.setParameter("pid", procesoId);
            q.setParameter("ahora", new Date());
            q.setMaxResults(1);
            List<CronogramaFase> r = q.getResultList();
            return (r != null && !r.isEmpty()) ? r.get(0) : null;
        } catch (NoResultException e) {
            return null;
        }
    }
}
