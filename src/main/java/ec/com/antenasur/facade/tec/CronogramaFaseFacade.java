package ec.com.antenasur.facade.tec;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import ec.com.antenasur.enums.FaseElectoral;
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
     * Devuelve la fase vigente (now() entre fechaInicio y fechaFin) con menor
     * {@code orden} del proceso dado. Útil para el banner general del proceso.
     * <p>
     * Cuando varias fases se solapan temporalmente, esta query retorna solo la
     * de mayor prioridad (menor orden); para consultar permisos use
     * {@link #getVigentesPorProceso(Integer)} que devuelve todas.
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

    /**
     * Devuelve <b>todas</b> las fases cuyo rango de fechas incluye el instante
     * actual dentro del proceso indicado, ordenadas por {@code orden} y luego
     * por {@code id}. A diferencia de {@link #getVigentePorProceso}, no aplica
     * {@code LIMIT 1}, por lo que expone correctamente las fases paralelas.
     */
    public List<CronogramaFase> getVigentesPorProceso(Integer procesoId) {
        if (procesoId == null) return Collections.emptyList();
        try {
            String hql = "FROM CronogramaFase f"
                    + " WHERE f.proceso.id = :pid"
                    + " AND :ahora BETWEEN f.fechaInicio AND f.fechaFin"
                    + " ORDER BY f.orden ASC, f.id ASC";
            TypedQuery<CronogramaFase> q = super.getEntityManager().createQuery(hql, CronogramaFase.class);
            q.setParameter("pid", procesoId);
            q.setParameter("ahora", new Date());
            return q.getResultList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Devuelve la primera fase (por {@code orden} y luego {@code id}) del tipo
     * indicado en el proceso, <b>sin filtro de fecha</b>. Permite mostrar la
     * configuración de una fase en la UI independientemente de si está vigente.
     */
    public CronogramaFase getFasePorTipo(Integer procesoId, FaseElectoral fase) {
        if (procesoId == null || fase == null) return null;
        try {
            String hql = "FROM CronogramaFase f"
                    + " WHERE f.proceso.id = :pid AND f.fase = :fase"
                    + " ORDER BY f.orden ASC, f.id ASC";
            TypedQuery<CronogramaFase> q = super.getEntityManager().createQuery(hql, CronogramaFase.class);
            q.setParameter("pid", procesoId);
            q.setParameter("fase", fase);
            q.setMaxResults(1);
            List<CronogramaFase> r = q.getResultList();
            return r.isEmpty() ? null : r.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Fase inmediatamente anterior a {@code ordenRef}: la de mayor {@code orden}
     * estrictamente menor que la referencia. Sin filtro de fecha.
     */
    public CronogramaFase getFaseAnterior(Integer procesoId, Integer ordenRef) {
        if (procesoId == null || ordenRef == null) return null;
        try {
            String hql = "FROM CronogramaFase f"
                    + " WHERE f.proceso.id = :pid AND f.orden < :orden"
                    + " ORDER BY f.orden DESC, f.id DESC";
            TypedQuery<CronogramaFase> q = super.getEntityManager().createQuery(hql, CronogramaFase.class);
            q.setParameter("pid", procesoId);
            q.setParameter("orden", ordenRef);
            q.setMaxResults(1);
            List<CronogramaFase> r = q.getResultList();
            return r.isEmpty() ? null : r.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Fase inmediatamente siguiente a {@code ordenRef}: la de menor {@code orden}
     * estrictamente mayor que la referencia. Sin filtro de fecha.
     */
    public CronogramaFase getFaseSiguiente(Integer procesoId, Integer ordenRef) {
        if (procesoId == null || ordenRef == null) return null;
        try {
            String hql = "FROM CronogramaFase f"
                    + " WHERE f.proceso.id = :pid AND f.orden > :orden"
                    + " ORDER BY f.orden ASC, f.id ASC";
            TypedQuery<CronogramaFase> q = super.getEntityManager().createQuery(hql, CronogramaFase.class);
            q.setParameter("pid", procesoId);
            q.setParameter("orden", ordenRef);
            q.setMaxResults(1);
            List<CronogramaFase> r = q.getResultList();
            return r.isEmpty() ? null : r.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Indica si existe al menos una fase del tipo indicado cuyo rango de fechas
     * incluye el instante actual. Usado para validar permisos específicos sin
     * cargar la lista completa.
     */
    public boolean existeFaseActivaPorTipo(Integer procesoId, FaseElectoral fase) {
        if (procesoId == null || fase == null) return false;
        try {
            String hql = "SELECT COUNT(f) FROM CronogramaFase f"
                    + " WHERE f.proceso.id = :pid"
                    + " AND f.fase = :fase"
                    + " AND :ahora BETWEEN f.fechaInicio AND f.fechaFin";
            TypedQuery<Long> q = super.getEntityManager().createQuery(hql, Long.class);
            q.setParameter("pid", procesoId);
            q.setParameter("fase", fase);
            q.setParameter("ahora", new Date());
            Long count = q.getSingleResult();
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
