package ec.com.antenasur.facade.tec;

import java.util.List;

import ec.com.antenasur.model.generic.AbstractFacade;
import ec.com.antenasur.model.tec.EscrutinioCabecera;
import jakarta.ejb.Stateless;
import jakarta.persistence.TypedQuery;

@Stateless
public class EscrutinioCabeceraFacade extends AbstractFacade<EscrutinioCabecera, Integer> {

    public EscrutinioCabeceraFacade() {
        super(EscrutinioCabecera.class, Integer.class);
    }

    public EscrutinioCabecera buscarPorMesaProceso(Integer mesaId, Integer procesoId) {
        if (mesaId == null || procesoId == null) {
            return null;
        }
        try {
            String sql = "SELECT e FROM EscrutinioCabecera e"
                    + " LEFT JOIN FETCH e.mesa m"
                    + " LEFT JOIN FETCH m.recinto r"
                    + " LEFT JOIN FETCH e.proceso pro"
                    + " WHERE m.id = :mesaId AND pro.id = :procesoId AND e.estado = TRUE";
            TypedQuery<EscrutinioCabecera> query = super.getEntityManager()
                    .createQuery(sql, EscrutinioCabecera.class);
            query.setParameter("mesaId", mesaId);
            query.setParameter("procesoId", procesoId);
            List<EscrutinioCabecera> resultado = query.getResultList();
            return resultado.isEmpty() ? null : resultado.get(0);
        } catch (Exception e) {
            return null;
        }
    }
}
