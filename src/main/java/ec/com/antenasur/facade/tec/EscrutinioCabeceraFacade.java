package ec.com.antenasur.facade.tec;

import java.util.List;

import ec.com.antenasur.model.generic.AbstractFacade;
import ec.com.antenasur.enums.EstadoEscrutinio;
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

    public long contarCerradasPorProceso(Integer procesoId) {
        if (procesoId == null) {
            return 0L;
        }
        String sql = "SELECT COUNT(e.id) FROM EscrutinioCabecera e"
                + " JOIN e.proceso pro"
                + " WHERE pro.id = :procesoId"
                + " AND e.estado = TRUE"
                + " AND e.estadoEscrutinio = :estadoCerrado";
        Long total = super.getEntityManager().createQuery(sql, Long.class)
                .setParameter("procesoId", procesoId)
                .setParameter("estadoCerrado", EstadoEscrutinio.CERRADO)
                .getSingleResult();
        return total != null ? total : 0L;
    }

    public List<EscrutinioCabecera> listarCerradasPorProceso(Integer procesoId) {
        if (procesoId == null) {
            return java.util.Collections.emptyList();
        }
        String sql = "SELECT e FROM EscrutinioCabecera e"
                + " JOIN FETCH e.mesa m"
                + " JOIN FETCH m.recinto r"
                + " LEFT JOIN FETCH r.ubicacion parroquia"
                + " LEFT JOIN FETCH parroquia.geograp canton"
                + " LEFT JOIN FETCH canton.geograp provincia"
                + " JOIN FETCH e.proceso pro"
                + " WHERE pro.id = :procesoId"
                + " AND e.estado = TRUE"
                + " AND e.estadoEscrutinio = :estadoCerrado"
                + " ORDER BY provincia.name, canton.name, parroquia.name, r.nombre, m.nombre";
        TypedQuery<EscrutinioCabecera> query = super.getEntityManager()
                .createQuery(sql, EscrutinioCabecera.class);
        query.setParameter("procesoId", procesoId);
        query.setParameter("estadoCerrado", EstadoEscrutinio.CERRADO);
        return query.getResultList();
    }
}
