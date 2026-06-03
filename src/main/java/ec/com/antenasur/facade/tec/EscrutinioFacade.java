package ec.com.antenasur.facade.tec;

import jakarta.ejb.Stateless;

import ec.com.antenasur.model.tec.Escrutinio;
import ec.com.antenasur.model.generic.AbstractFacade;
import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import java.util.List;
import jakarta.persistence.TypedQuery;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class EscrutinioFacade extends AbstractFacade<Escrutinio, Integer> {

    private static final String HQL = " SELECT e FROM Escrutinio e";
    private static final String ORDENADO = " ORDER BY e.id";

    public EscrutinioFacade() {
        super(Escrutinio.class, Integer.class);
    }

    public List<Escrutinio> buscaPorMesa(Mesa mesa) {
        return buscaPorMesaYProceso(mesa, null);
    }

    public List<Escrutinio> buscaPorMesaYProceso(Mesa mesa, ProcesoElectoral proceso) {
        try {
            String sql = HQL + " WHERE e.mesa=:mesa";
            if (proceso != null) {
                sql += " AND e.proceso = :proceso";
            }
            sql += " ORDER BY e.categoria.orden";
            TypedQuery<Escrutinio> query = super.getEntityManager().createQuery(sql, Escrutinio.class);
            query.setParameter("mesa", mesa);
            if (proceso != null) {
                query.setParameter("proceso", proceso);
            }
            List<Escrutinio> result = query.getResultList();
            if (result != null) {
                return result;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
    
    public List<Escrutinio> buscaCanton(Mesa mesa) {
        try {
            String sql = HQL + " WHERE e.mesa=:mesa ORDER BY e.categoria.orden";
            TypedQuery<Escrutinio> query = super.getEntityManager().createQuery(sql, Escrutinio.class);
            query.setParameter("mesa", mesa);
            List<Escrutinio> result = query.getResultList();
            if (result != null) {
                return result;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

}
