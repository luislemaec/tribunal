package ec.com.antenasur.facade.tec;

import jakarta.ejb.Stateless;

import ec.com.antenasur.model.generic.AbstractFacade;
import ec.com.antenasur.model.tec.VwLugarVotacion;
import java.util.List;
import jakarta.persistence.TypedQuery;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class VwLugarVotacionFacade extends AbstractFacade<VwLugarVotacion, Integer> {

    private static final String HQL = " SELECT l FROM VwLugarVotacion l";
    private static final String ORDENADO = " ORDER BY l.id";

    public VwLugarVotacionFacade() {
        super(VwLugarVotacion.class, Integer.class);
    }

    /**
     * Lugar de votación de una cédula en el proceso electoral activo, para la consulta pública.
     * Solo registros vigentes (padrón, membresía, persona, mesa y recinto) del proceso activo;
     * no consulta la vista {@code vw_lugar_votacion}, que no filtra por proceso. Columnas: nombres,
     * apellidos, id y nombre del proceso, mesa, recinto, parroquia y cantón. No devuelve la
     * iglesia: la afiliación religiosa es un dato sensible y no se expone públicamente.
     */
    public List<Object[]> buscarLugarProcesoActivo(String cedula) {
        if (cedula == null || cedula.isBlank()) {
            return java.util.Collections.emptyList();
        }
        TypedQuery<Object[]> query = getEntityManager().createQuery(
                "SELECT per.nombres, per.apellidos, pr.id, pr.nombre, m.nombre, r.nombre, pa.name, ca.name"
                        + " FROM Padron p JOIN p.proceso pr JOIN p.mesa m JOIN m.recinto r JOIN r.ubicacion pa"
                        + " LEFT JOIN pa.geograp ca JOIN p.iglesiaPersona ip JOIN ip.persona per"
                        + " WHERE p.estado = TRUE AND pr.estado = TRUE AND pr.activo = TRUE"
                        + " AND ip.estado = TRUE AND per.estado = TRUE AND m.estado = TRUE AND r.estado = TRUE"
                        + " AND per.documento = :cedula ORDER BY p.id",
                Object[].class);
        return query.setParameter("cedula", cedula).setMaxResults(2).getResultList();
    }
}
