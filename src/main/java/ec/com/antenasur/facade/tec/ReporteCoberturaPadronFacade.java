package ec.com.antenasur.facade.tec;

import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.TypedQuery;

import ec.com.antenasur.dto.ConteoCoberturaDTO;
import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.generic.AbstractFacade;

/**
 * Cobertura del padrón: de las personas habilitadas, cuántas ya están en el
 * padrón del proceso. Una consulta agregada por agrupación, sin cargar personas.
 *
 * <p>La ubicación es la de la iglesia de la persona, igual que en
 * {@link ReporteRegistrosFacade}, para que ambos reportes cuadren. La presencia
 * en el padrón se comprueba por persona y proceso, como en
 * {@link GestionPadronFacade}.</p>
 */
@Stateless
public class ReporteCoberturaPadronFacade extends AbstractFacade<IglesiaPersona, Integer> {

    /** Habilitado: misma condición que el padrón (PadronFacade / GestionPadronFacade). */
    private static final String FROM_HABILITADOS = " FROM IglesiaPersona ip JOIN ip.persona pr JOIN ip.iglesia i"
            + " JOIN i.ubicacion pa JOIN pa.geograp ca LEFT JOIN ca.geograp pv"
            + " WHERE ip.estado = TRUE AND ip.habilitadoPadron = TRUE AND pr.estado = TRUE AND i.estado = TRUE";

    private static final String CONTEOS = "COUNT(DISTINCT pr.id),"
            + " COUNT(DISTINCT CASE WHEN EXISTS (SELECT p.id FROM Padron p WHERE p.estado = TRUE"
            + " AND p.proceso.id = :proceso AND p.iglesiaPersona.persona.id = pr.id) THEN pr.id ELSE NULL END)";

    private static final String DTO = "SELECT new ec.com.antenasur.dto.ConteoCoberturaDTO(";

    public ReporteCoberturaPadronFacade() {
        super(IglesiaPersona.class, Integer.class);
    }

    public List<ConteoCoberturaDTO> contarPorCanton(Integer procesoId) {
        return getEntityManager().createQuery(DTO + "ca.id, ca.name, pv.name, " + CONTEOS + ")" + FROM_HABILITADOS
                + " GROUP BY ca.id, ca.name, pv.name", ConteoCoberturaDTO.class)
                .setParameter("proceso", procesoId).getResultList();
    }

    public List<ConteoCoberturaDTO> contarPorParroquia(Integer procesoId, Integer cantonId) {
        return getEntityManager().createQuery(DTO + "pa.id, pa.name, pv.name, " + CONTEOS + ")" + FROM_HABILITADOS
                + " AND ca.id = :canton GROUP BY pa.id, pa.name, pv.name", ConteoCoberturaDTO.class)
                .setParameter("proceso", procesoId).setParameter("canton", cantonId).getResultList();
    }

    public ConteoCoberturaDTO totales(Integer procesoId, Integer cantonId) {
        TypedQuery<Object[]> query = getEntityManager().createQuery("SELECT " + CONTEOS + FROM_HABILITADOS
                + (cantonId == null ? "" : " AND ca.id = :canton"), Object[].class)
                .setParameter("proceso", procesoId);
        if (cantonId != null) {
            query.setParameter("canton", cantonId);
        }
        Object[] fila = query.getSingleResult();
        return new ConteoCoberturaDTO(null, null, null, (Long) fila[0], (Long) fila[1]);
    }
}
