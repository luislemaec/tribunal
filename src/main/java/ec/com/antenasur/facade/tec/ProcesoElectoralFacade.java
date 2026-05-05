package ec.com.antenasur.facade.tec;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import ec.com.antenasur.model.generic.AbstractFacade;
import ec.com.antenasur.model.tec.ProcesoElectoral;

@Stateless
public class ProcesoElectoralFacade extends AbstractFacade<ProcesoElectoral, Integer> {

    public ProcesoElectoralFacade() {
        super(ProcesoElectoral.class, Integer.class);
    }

    /**
     * Devuelve el proceso marcado como activo. Si hay más de uno por error
     * de datos, retorna el más reciente. Null si no existe ninguno.
     */
    public ProcesoElectoral getActivo() {
        try {
            String hql = "FROM ProcesoElectoral p WHERE p.activo = TRUE ORDER BY p.id DESC";
            TypedQuery<ProcesoElectoral> q = super.getEntityManager().createQuery(hql, ProcesoElectoral.class);
            q.setMaxResults(1);
            List<ProcesoElectoral> r = q.getResultList();
            return (r != null && !r.isEmpty()) ? r.get(0) : null;
        } catch (NoResultException e) {
            return null;
        }
    }
}
