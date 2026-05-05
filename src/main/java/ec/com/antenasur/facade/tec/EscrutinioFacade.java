package ec.com.antenasur.facade.tec;

import javax.ejb.Stateless;

import ec.com.antenasur.model.tec.Escrutinio;
import ec.com.antenasur.model.generic.AbstractFacade;
import ec.com.antenasur.model.tec.Mesa;
import java.util.List;
import javax.persistence.TypedQuery;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class EscrutinioFacade extends AbstractFacade<Escrutinio, Integer> {

    private static final String HQL = " FROM Escrutinio e";
    private static final String ORDENADO = " ORDER BY e.id";

    public EscrutinioFacade() {
        super(Escrutinio.class, Integer.class);
    }

    public List<Escrutinio> buscaPorMesa(Mesa mesa) {
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
