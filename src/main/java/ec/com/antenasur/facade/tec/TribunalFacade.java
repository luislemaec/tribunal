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

import ec.com.antenasur.model.tec.Tribunal;
import ec.com.antenasur.model.generic.AbstractFacade;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import org.hibernate.Filter;
import org.hibernate.Session;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class TribunalFacade extends AbstractFacade<Tribunal, Integer> {

    static final String HQL = " SELECT t FROM Tribunal t";
    static final String ACTIVOS = "  t.estado =TRUE";
    static final String ORDENADO = "  ORDER BY t.id";

    public TribunalFacade() {
        super(Tribunal.class, Integer.class);
    }

    public List<Tribunal> getRegistrosActivos() {
        return getRegistrosActivosPorProceso(null);
    }

    public List<Tribunal> getRegistrosActivosPorProceso(ProcesoElectoral proceso) {
        try {
            String sql = HQL + " INNER JOIN FETCH t.cargo c"
                    + " INNER JOIN FETCH t.iglesiaPersona ip"
                    + " LEFT JOIN FETCH ip.persona p"
                    + " LEFT JOIN FETCH ip.iglesia i"
                    + " WHERE " + ACTIVOS;
            if (proceso != null) {
                sql += " AND t.proceso = :proceso";
            }
            sql += " ORDER BY c.orden, t.id";
            TypedQuery<Tribunal> query = super.getEntityManager().createQuery(sql, Tribunal.class);
            if (proceso != null) {
                query.setParameter("proceso", proceso);
            }
            List<Tribunal> result = query.getResultList();
            if (result.size() > 0) {
                return result;
            }
        } catch (NoResultException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    /** Busca el registro lógico de un cargo, incluso si fue dado de baja. */
    public Tribunal buscarPorProcesoCargoIncluyendoInactivos(Integer procesoId, Integer cargoId) {
        if (procesoId == null || cargoId == null) {
            return null;
        }
        Session session = super.getEntityManager().unwrap(Session.class);
        Filter filtroActivo = session.getEnabledFilter("filterActive");
        if (filtroActivo != null) {
            session.disableFilter("filterActive");
        }
        try {
            TypedQuery<Tribunal> query = session.createQuery(
                    HQL + " WHERE t.proceso.id = :procesoId AND t.cargo.id = :cargoId", Tribunal.class);
            query.setParameter("procesoId", procesoId);
            query.setParameter("cargoId", cargoId);
            query.setMaxResults(1);
            List<Tribunal> resultado = query.getResultList();
            return resultado.isEmpty() ? null : resultado.get(0);
        } finally {
            if (filtroActivo != null) {
                session.enableFilter("filterActive");
            }
        }
    }

}
