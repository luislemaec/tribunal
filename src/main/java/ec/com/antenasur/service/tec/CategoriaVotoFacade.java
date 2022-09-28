package ec.com.antenasur.service.tec;

import javax.ejb.Stateless;

import ec.com.antenasur.domain.tec.CategoriaVoto;
import ec.com.antenasur.domain.generic.AbstractFacade;
import java.util.List;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class CategoriaVotoFacade extends AbstractFacade<CategoriaVoto, Integer> {

    public CategoriaVotoFacade() {
        super(CategoriaVoto.class, Integer.class);
    }

    public List<CategoriaVoto> getCategoriasOrdenados() {
        try {
            String sql = "FROM CategoriaVoto gc ORDER BY orden";
            TypedQuery<CategoriaVoto> query = super.getEntityManager().createQuery(sql, CategoriaVoto.class);
            List<CategoriaVoto> resultList = query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList;
            }
        } catch (NoResultException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

}
