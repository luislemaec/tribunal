package ec.com.antenasur.service.tec;

import javax.ejb.Stateless;

import ec.com.antenasur.domain.generic.AbstractFacade;
import ec.com.antenasur.domain.tec.VwLugarVotacion;
import ec.com.antenasur.domain.tec.VwTotalVotos;
import java.util.List;
import javax.persistence.TypedQuery;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class VwLugarVotacionFacade extends AbstractFacade<VwLugarVotacion, Integer> {

    private static final String HQL = " FROM VwLugarVotacion l";
    private static final String ORDENADO = " ORDER BY l.id";

    public VwLugarVotacionFacade() {
        super(VwLugarVotacion.class, Integer.class);
    }

    public List<VwLugarVotacion> buscaLugarVotacion(String nombreCedula) {
        try {
            String sql = HQL + " WHERE (l.nombres = :nombreCedula OR l.cedula=:nombreCedula) ";
            TypedQuery<VwLugarVotacion> query = super.getEntityManager().createQuery(sql, VwLugarVotacion.class);
            query.setParameter("nombreCedula", nombreCedula);
            List<VwLugarVotacion> result = query.getResultList();
            if (result != null) {
                return result;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
