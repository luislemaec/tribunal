package ec.com.antenasur.facade.tec;

import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.TypedQuery;

import ec.com.antenasur.dto.ConteoRegistrosDTO;
import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.generic.AbstractFacade;

/**
 * Consultas agregadas de Rep. Registros: un COUNT DISTINCT por persona y un
 * GROUP BY geográfico, sin cargar personas en memoria.
 *
 * <p>La ubicación de una persona es la de su iglesia: {@code iglesia.ubicacion}
 * es la parroquia y su padre el cantón (mismo criterio que
 * {@link PadronFacade#contarElectoresPorCanton}).</p>
 */
@Stateless
public class ReporteRegistrosFacade extends AbstractFacade<IglesiaPersona, Integer> {

    /** Registrado: vínculo, persona e iglesia activos. */
    private static final String FROM_REGISTRADOS = " FROM IglesiaPersona ip JOIN ip.persona pr JOIN ip.iglesia i"
            + " JOIN i.ubicacion pa JOIN pa.geograp ca LEFT JOIN ca.geograp pv"
            + " WHERE ip.estado = TRUE AND pr.estado = TRUE AND i.estado = TRUE";

    /** Misma regla del padrón: solo TRUE habilita; FALSE y NULL son «No habilitado». */
    private static final String HABILITADO = "ip.habilitadoPadron = TRUE";

    /** Misma regla de revisión pendiente que IglesiaPersonaFacade.obtenerResumenMiembrosActivosPorIglesia. */
    private static final String REVISION_PENDIENTE = "ip.fechaActualiza IS NULL"
            + " OR (ip.fechaCrea IS NOT NULL AND ip.fechaActualiza < ip.fechaCrea)";

    private static final String CONTEOS = "COUNT(DISTINCT pr.id),"
            + " COUNT(DISTINCT CASE WHEN " + HABILITADO + " THEN pr.id ELSE NULL END),"
            + " COUNT(DISTINCT CASE WHEN " + REVISION_PENDIENTE + " THEN pr.id ELSE NULL END)";

    private static final String DTO = "SELECT new ec.com.antenasur.dto.ConteoRegistrosDTO(";

    public ReporteRegistrosFacade() {
        super(IglesiaPersona.class, Integer.class);
    }

    public List<ConteoRegistrosDTO> contarPorCanton() {
        return getEntityManager().createQuery(DTO + "ca.id, ca.name, pv.name, " + CONTEOS + ")" + FROM_REGISTRADOS
                + " GROUP BY ca.id, ca.name, pv.name", ConteoRegistrosDTO.class).getResultList();
    }

    public List<ConteoRegistrosDTO> contarPorParroquia(Integer cantonId) {
        return getEntityManager().createQuery(DTO + "pa.id, pa.name, pv.name, " + CONTEOS + ")" + FROM_REGISTRADOS
                + " AND ca.id = :canton GROUP BY pa.id, pa.name, pv.name", ConteoRegistrosDTO.class)
                .setParameter("canton", cantonId).getResultList();
    }

    /**
     * Totales de personas distintas. No es la suma de las categorías: una
     * persona con iglesias en dos cantones cuenta una sola vez.
     */
    public ConteoRegistrosDTO totales(Integer cantonId) {
        TypedQuery<Object[]> query = getEntityManager().createQuery("SELECT " + CONTEOS + FROM_REGISTRADOS
                + (cantonId == null ? "" : " AND ca.id = :canton"), Object[].class);
        if (cantonId != null) {
            query.setParameter("canton", cantonId);
        }
        Object[] fila = query.getSingleResult();
        return new ConteoRegistrosDTO(null, null, null, (Long) fila[0], (Long) fila[1], (Long) fila[2]);
    }
}
