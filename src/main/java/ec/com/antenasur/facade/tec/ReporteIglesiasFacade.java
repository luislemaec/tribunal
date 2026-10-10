package ec.com.antenasur.facade.tec;

import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.TypedQuery;

import ec.com.antenasur.dto.ConteoIglesiasDTO;
import ec.com.antenasur.dto.IglesiaAtencionDTO;
import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.model.generic.AbstractFacade;

/**
 * Pestaña Iglesias de Rep. Registros: consultas agregadas sobre iglesias
 * activas, sin cargar entidades ni recorrer iglesias una a una.
 *
 * <p>Reglas reutilizadas, no redefinidas:</p>
 * <ul>
 * <li>Administrador: usuario y rol activos con rol {@code %IglesiaAdmin}
 * (IglesiaFacade, filtro {@code conAdmin}).</li>
 * <li>Lista de miembros: documento activo del tipo indicado con
 * {@code entidadId} de la iglesia (DocumentoFacade).</li>
 * <li>Habilitado y revisión pendiente: mismas reglas que
 * {@link ReporteRegistrosFacade}.</li>
 * </ul>
 */
@Stateless
public class ReporteIglesiasFacade extends AbstractFacade<Iglesia, Integer> {

    private static final String ROL_IGLESIA_ADMIN = "%IglesiaAdmin";

    private static final String TIENE_ADMIN = "EXISTS (SELECT ru.id FROM RolUsuario ru WHERE ru.usuario.iglesia = i"
            + " AND ru.usuario.estado = TRUE AND ru.estado = TRUE AND ru.rol.estado = TRUE"
            + " AND ru.rol.nombre LIKE :rolIglesiaAdmin)";

    private static final String TIENE_LISTA = "EXISTS (SELECT d.id FROM Documentos d WHERE d.entidadId = i.id"
            + " AND d.tipoDocumento.id = :tipoLista AND d.estado = TRUE)";

    private static final String TIENE_HABILITADOS = "EXISTS (SELECT ip.id FROM IglesiaPersona ip"
            + " WHERE ip.iglesia = i AND ip.estado = TRUE AND ip.habilitadoPadron = TRUE AND ip.persona.estado = TRUE)";

    private static final String FROM_IGLESIAS = " FROM Iglesia i JOIN i.ubicacion pa JOIN pa.geograp ca"
            + " LEFT JOIN ca.geograp pv WHERE i.estado = TRUE";

    private static final String CONTEOS = "COUNT(DISTINCT i.id),"
            + " COUNT(DISTINCT CASE WHEN " + TIENE_ADMIN + " THEN i.id ELSE NULL END),"
            + " COUNT(DISTINCT CASE WHEN " + TIENE_LISTA + " THEN i.id ELSE NULL END),"
            + " COUNT(DISTINCT CASE WHEN " + TIENE_HABILITADOS + " THEN i.id ELSE NULL END)";

    private static final String DTO = "SELECT new ec.com.antenasur.dto.ConteoIglesiasDTO(";

    public ReporteIglesiasFacade() {
        super(Iglesia.class, Integer.class);
    }

    public List<ConteoIglesiasDTO> contarPorCanton(Integer tipoLista) {
        return parametros(getEntityManager().createQuery(DTO + "ca.id, ca.name, pv.name, " + CONTEOS + ")"
                + FROM_IGLESIAS + " GROUP BY ca.id, ca.name, pv.name", ConteoIglesiasDTO.class), tipoLista)
                .getResultList();
    }

    public List<ConteoIglesiasDTO> contarPorParroquia(Integer tipoLista, Integer cantonId) {
        return parametros(getEntityManager().createQuery(DTO + "pa.id, pa.name, pv.name, " + CONTEOS + ")"
                + FROM_IGLESIAS + " AND ca.id = :canton GROUP BY pa.id, pa.name, pv.name", ConteoIglesiasDTO.class),
                tipoLista).setParameter("canton", cantonId).getResultList();
    }

    public ConteoIglesiasDTO totales(Integer tipoLista, Integer cantonId) {
        TypedQuery<Object[]> query = parametros(getEntityManager().createQuery("SELECT " + CONTEOS + FROM_IGLESIAS
                + (cantonId == null ? "" : " AND ca.id = :canton"), Object[].class), tipoLista);
        if (cantonId != null) {
            query.setParameter("canton", cantonId);
        }
        Object[] fila = query.getSingleResult();
        return new ConteoIglesiasDTO(null, null, null, (Long) fila[0], (Long) fila[1], (Long) fila[2],
                (Long) fila[3]);
    }

    /**
     * Una fila por iglesia activa (también las que no tienen miembros) con sus
     * conteos de personas distintas. Filas agregadas: tantas como iglesias.
     */
    public List<IglesiaAtencionDTO> listarIglesias(Integer tipoLista, Integer cantonId) {
        String jpql = "SELECT new ec.com.antenasur.dto.IglesiaAtencionDTO(i.id, i.nombre, pa.name, ca.name,"
                + " COUNT(DISTINCT pr.id),"
                + " COUNT(DISTINCT CASE WHEN ip.habilitadoPadron = TRUE THEN pr.id ELSE NULL END),"
                + " COUNT(DISTINCT CASE WHEN ip.fechaActualiza IS NULL"
                + " OR (ip.fechaCrea IS NOT NULL AND ip.fechaActualiza < ip.fechaCrea) THEN pr.id ELSE NULL END),"
                + " MAX(CASE WHEN " + TIENE_ADMIN + " THEN 1 ELSE 0 END),"
                + " MAX(CASE WHEN " + TIENE_LISTA + " THEN 1 ELSE 0 END))"
                + " FROM Iglesia i JOIN i.ubicacion pa JOIN pa.geograp ca"
                + " LEFT JOIN IglesiaPersona ip ON ip.iglesia = i AND ip.estado = TRUE"
                + " LEFT JOIN Persona pr ON pr = ip.persona AND pr.estado = TRUE"
                + " WHERE i.estado = TRUE" + (cantonId == null ? "" : " AND ca.id = :canton")
                + " GROUP BY i.id, i.nombre, pa.name, ca.name";
        TypedQuery<IglesiaAtencionDTO> query = parametros(
                getEntityManager().createQuery(jpql, IglesiaAtencionDTO.class), tipoLista);
        if (cantonId != null) {
            query.setParameter("canton", cantonId);
        }
        return query.getResultList();
    }

    private static <T> TypedQuery<T> parametros(TypedQuery<T> query, Integer tipoLista) {
        return query.setParameter("rolIglesiaAdmin", ROL_IGLESIA_ADMIN).setParameter("tipoLista", tipoLista);
    }
}
