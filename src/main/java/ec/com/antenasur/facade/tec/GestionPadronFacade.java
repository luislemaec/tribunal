package ec.com.antenasur.facade.tec;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.ejb.Stateless;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import org.hibernate.Session;
import ec.com.antenasur.dto.FiltroPadronDTO;
import ec.com.antenasur.dto.FilaPadronDTO;
import ec.com.antenasur.dto.MesaPadronDTO;
import ec.com.antenasur.dto.OpcionPadronDTO;
import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.generic.AbstractFacade;
import ec.com.antenasur.model.generic.EntidadBase;
import ec.com.antenasur.model.tec.Padron;
import ec.com.antenasur.model.tec.ProcesoElectoral;

/** Proyecciones paginadas y operaciones en bloque; nunca convierte grafos JPA a DTOs. */
@Stateless
public class GestionPadronFacade extends AbstractFacade<Padron, Integer> {
    public GestionPadronFacade() { super(Padron.class, Integer.class); }

    private static final String GEO = " LEFT JOIN r.ubicacion pa LEFT JOIN pa.geograp ca LEFT JOIN ca.geograp pv";
    private static final String PADRON = " FROM Padron p JOIN p.iglesiaPersona ip JOIN ip.persona pr"
            + " JOIN ip.iglesia i JOIN p.proceso pro JOIN p.mesa m JOIN m.recinto r" + GEO;
    private static final String DISPONIBLES = " FROM IglesiaPersona ip JOIN ip.persona pr JOIN ip.iglesia i";
    private static final String CAMPOS = "p.id, ip.id, pr.id, pr.documento, pr.nombres, pr.apellidos, i.nombre, i.id,"
            + " pro.nombre, pv.name, ca.name, pa.name, r.nombre, r.id, m.nombre, m.id, p.estado";

    private static class Consulta {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        Map<String, Object> parametros = new LinkedHashMap<>();
        void filtro(String expresion, String nombre, Object valor) {
            if (valor != null) { where.append(" AND ").append(expresion).append(" = :").append(nombre); parametros.put(nombre, valor); }
        }
        <T> TypedQuery<T> aplicar(TypedQuery<T> q) { parametros.forEach(q::setParameter); return q; }
    }

    private Consulta geografia(FiltroPadronDTO f) {
        Consulta c = new Consulta();
        c.filtro("pv.id", "provincia", f.getProvinciaId());
        c.filtro("ca.id", "canton", f.getCantonId());
        c.filtro("pa.id", "parroquia", f.getParroquiaId());
        c.filtro("r.id", "recinto", f.getRecintoId());
        return c;
    }

    private Consulta filas(FiltroPadronDTO f, boolean disponibles) {
        Consulta c = disponibles ? new Consulta() : geografia(f);
        c.filtro("i.id", "iglesia", f.getIglesiaId());
        if (disponibles) {
            c.where.append(" AND ip.estado = TRUE AND pr.estado = TRUE AND i.estado = TRUE"
                    + " AND ip.habilitadoPadron = TRUE"
                    + " AND NOT EXISTS (SELECT p2.id FROM Padron p2 WHERE p2.iglesiaPersona.persona.id = pr.id"
                    + " AND p2.proceso.id = :proceso AND p2.estado = TRUE)");
            c.parametros.put("proceso", f.getProcesoId());
        } else {
            c.where.append(" AND p.estado = TRUE");
            c.filtro("pro.id", "proceso", f.getProcesoId());
            c.filtro("m.id", "mesa", f.getMesaId());
            if (f.getIglesiaNombre() != null && !f.getIglesiaNombre().isBlank()) {
                c.where.append(" AND LOWER(i.nombre) LIKE :nombreIglesia ESCAPE '!'");
                c.parametros.put("nombreIglesia", "%" + f.getIglesiaNombre().trim().toLowerCase(java.util.Locale.ROOT)
                        .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
            }
        }
        if (f.getBusqueda() != null && !f.getBusqueda().isBlank()) {
            c.where.append(" AND (LOWER(COALESCE(pr.documento,'')) LIKE :texto ESCAPE '!'"
                    + " OR LOWER(CONCAT(CONCAT(COALESCE(pr.nombres,''),' '),COALESCE(pr.apellidos,''))) LIKE :texto ESCAPE '!')");
            c.parametros.put("texto", "%" + f.getBusqueda().trim().toLowerCase(java.util.Locale.ROOT)
                    .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
        }
        return c;
    }

    public List<FilaPadronDTO> listar(FiltroPadronDTO f, boolean disponibles, int primero, int cantidad,
            String orden, boolean descendente, Integer despuesId) {
        Consulta c = filas(f, disponibles);
        if (despuesId != null) { c.where.append(" AND p.id > :ultimo"); c.parametros.put("ultimo", despuesId); }
        String campos = disponibles ? "ip.id, ip.id, pr.id, pr.documento, pr.nombres, pr.apellidos, i.nombre, i.id,"
                + " null, null, null, null, null, null, null, null, TRUE" : CAMPOS;
        String campoOrden = switch (orden == null ? "" : orden) {
            case "documento" -> "pr.documento";
            case "nombres" -> "pr.nombres";
            case "iglesia" -> "i.nombre";
            default -> disponibles ? "ip.id" : "p.id";
        };
        String id = disponibles ? "ip.id" : "p.id";
        return c.aplicar(getEntityManager().createQuery("SELECT new ec.com.antenasur.dto.FilaPadronDTO("
                + campos + ")" + (disponibles ? DISPONIBLES : PADRON) + c.where
                + " ORDER BY " + campoOrden + (descendente ? " DESC" : " ASC") + ", " + id, FilaPadronDTO.class))
                .setFirstResult(primero).setMaxResults(cantidad).getResultList();
    }

    public long contar(FiltroPadronDTO f, boolean disponibles) {
        Consulta c = filas(f, disponibles);
        return c.aplicar(getEntityManager().createQuery("SELECT COUNT(" + (disponibles ? "ip.id)" : "p.id)")
                + (disponibles ? DISPONIBLES : PADRON) + c.where, Long.class)).getSingleResult();
    }

    private Consulta mesas(FiltroPadronDTO f, Boolean conPadron) {
        Consulta c = geografia(f);
        c.where.append(" AND m.estado = TRUE AND r.estado = TRUE");
        c.filtro("m.id", "mesa", f.getMesaId());
        if (conPadron != null) {
            c.where.append(conPadron ? " AND EXISTS" : " AND NOT EXISTS").append(
                    " (SELECT p.id FROM Padron p WHERE p.mesa.id = m.id AND p.proceso.id = :proceso AND p.estado = TRUE)");
            c.parametros.put("proceso", f.getProcesoId());
        }
        return c;
    }

    public long contarMesas(FiltroPadronDTO f, Boolean conPadron) {
        Consulta c = mesas(f, conPadron);
        return c.aplicar(getEntityManager().createQuery("SELECT COUNT(m.id) FROM Mesa m JOIN m.recinto r"
                + GEO + c.where, Long.class)).getSingleResult();
    }

    public List<MesaPadronDTO> listarMesas(FiltroPadronDTO f, int primero, int cantidad) {
        Consulta c = mesas(f, f.getConPadron());
        c.parametros.put("proceso", f.getProcesoId());
        return c.aplicar(getEntityManager().createQuery("SELECT new ec.com.antenasur.dto.MesaPadronDTO("
                + "m.id, m.nombre, r.id, r.nombre, COUNT(p.id)) FROM Mesa m JOIN m.recinto r" + GEO
                + " LEFT JOIN Padron p ON p.mesa.id = m.id AND p.proceso.id = :proceso AND p.estado = TRUE"
                + c.where + " GROUP BY m.id, m.nombre, r.id, r.nombre ORDER BY r.nombre, m.nombre, m.id", MesaPadronDTO.class))
                .setFirstResult(primero).setMaxResults(cantidad).getResultList();
    }

    public List<OpcionPadronDTO> geografia(Integer padre) {
        String where = padre == null ? "g.geograp IS NOT NULL AND g.geograp.geograp IS NULL" : "g.geograp.id = :padre";
        var q = getEntityManager().createQuery("SELECT new ec.com.antenasur.dto.OpcionPadronDTO(g.id,g.name)"
                + " FROM Geograp g WHERE g.status = TRUE AND " + where + " ORDER BY g.name", OpcionPadronDTO.class);
        if (padre != null) q.setParameter("padre", padre);
        return q.getResultList();
    }

    public List<OpcionPadronDTO> recintos(FiltroPadronDTO f) {
        Consulta c = geografia(f);
        return c.aplicar(getEntityManager().createQuery("SELECT new ec.com.antenasur.dto.OpcionPadronDTO(r.id,r.nombre)"
                + " FROM Recinto r" + GEO + c.where + " AND r.estado = TRUE ORDER BY r.nombre", OpcionPadronDTO.class)).getResultList();
    }

    public List<OpcionPadronDTO> opcionesMesas(Integer recinto) {
        return getEntityManager().createQuery("SELECT new ec.com.antenasur.dto.OpcionPadronDTO(m.id,m.nombre)"
                + " FROM Mesa m WHERE m.recinto.id = :recinto AND m.estado = TRUE ORDER BY m.nombre,m.id", OpcionPadronDTO.class)
                .setParameter("recinto", recinto).getResultList();
    }

    public List<OpcionPadronDTO> iglesias(Integer mesa, Integer proceso) {
        return getEntityManager().createQuery("SELECT new ec.com.antenasur.dto.OpcionPadronDTO(i.id,i.nombre) FROM Iglesia i"
                + " WHERE i.estado = TRUE AND i.ubicacion.id = (SELECT m.recinto.ubicacion.id FROM Mesa m WHERE m.id = :mesa)"
                + " AND NOT EXISTS (SELECT p.id FROM Padron p WHERE p.iglesiaPersona.iglesia.id = i.id"
                + " AND p.proceso.id = :proceso AND p.mesa.id <> :mesa AND p.estado = TRUE) ORDER BY i.nombre", OpcionPadronDTO.class)
                .setParameter("mesa", mesa).setParameter("proceso", proceso).getResultList();
    }

    public List<ec.com.antenasur.dto.IglesiaPadronDTO> resumenIglesias(Integer mesa, Integer proceso, boolean asignadas) {
        String campos = "SELECT new ec.com.antenasur.dto.IglesiaPadronDTO(i.id, i.nombre, COUNT(";
        String jpql = asignadas
                ? campos + "p.id)) FROM Padron p JOIN p.iglesiaPersona ip JOIN ip.iglesia i"
                    + " WHERE p.estado = TRUE AND p.mesa.id = :mesa AND p.proceso.id = :proceso"
                : campos + "ip.id)) FROM IglesiaPersona ip JOIN ip.iglesia i"
                    + " WHERE ip.estado = TRUE AND ip.habilitadoPadron = TRUE AND ip.persona.estado = TRUE AND i.estado = TRUE"
                    + " AND i.ubicacion.id = (SELECT m.recinto.ubicacion.id FROM Mesa m WHERE m.id = :mesa)"
                    + " AND NOT EXISTS (SELECT p.id FROM Padron p WHERE p.estado = TRUE AND p.proceso.id = :proceso"
                    + " AND p.iglesiaPersona.iglesia.id = i.id AND p.mesa.id <> :mesa)"
                    + " AND NOT EXISTS (SELECT p.id FROM Padron p WHERE p.estado = TRUE AND p.proceso.id = :proceso"
                    + " AND p.iglesiaPersona.persona.id = ip.persona.id AND p.mesa.id = :mesa)";
        return getEntityManager().createQuery(jpql + " GROUP BY i.id, i.nombre ORDER BY i.nombre", ec.com.antenasur.dto.IglesiaPadronDTO.class)
                .setParameter("mesa", mesa).setParameter("proceso", proceso).getResultList();
    }

    public List<Integer> habilitadosPendientes(Integer iglesia, Integer mesa, Integer proceso) {
        return getEntityManager().createQuery("SELECT ip.id FROM IglesiaPersona ip"
                + " WHERE ip.estado = TRUE AND ip.habilitadoPadron = TRUE AND ip.persona.estado = TRUE"
                + " AND ip.iglesia.id = :iglesia AND ip.iglesia.estado = TRUE"
                + " AND NOT EXISTS (SELECT p.id FROM Padron p WHERE p.estado = TRUE"
                + " AND p.proceso.id = :proceso AND p.mesa.id = :mesa AND p.iglesiaPersona.persona.id = ip.persona.id)"
                + " ORDER BY ip.id", Integer.class)
                .setParameter("iglesia", iglesia).setParameter("mesa", mesa).setParameter("proceso", proceso).getResultList();
    }

    public List<Integer> empadronadosIglesia(Integer iglesia, Integer mesa, Integer proceso) {
        return getEntityManager().createQuery("SELECT p.id FROM Padron p WHERE p.estado = TRUE"
                + " AND p.iglesiaPersona.iglesia.id = :iglesia AND p.mesa.id = :mesa"
                + " AND p.proceso.id = :proceso ORDER BY p.id", Integer.class)
                .setParameter("iglesia", iglesia).setParameter("mesa", mesa).setParameter("proceso", proceso).getResultList();
    }

    public ProcesoElectoral bloquearProceso(Integer id) {
        return getEntityManager().find(ProcesoElectoral.class, id, LockModeType.PESSIMISTIC_WRITE);
    }

    public List<IglesiaPersona> miembros(List<Integer> ids) {
        return getEntityManager().createQuery("SELECT ip FROM IglesiaPersona ip JOIN FETCH ip.persona JOIN FETCH ip.iglesia i"
                + " LEFT JOIN FETCH i.ubicacion WHERE ip.id IN :ids ORDER BY ip.id", IglesiaPersona.class)
                .setParameter("ids", ids).getResultList();
    }

    public List<Padron> existentes(Integer proceso, List<Integer> personas) {
        var em = getEntityManager();
        Session session = em.unwrap(Session.class);
        session.disableFilter(EntidadBase.FILTER_ACTIVE);
        try {
            return em.createQuery("SELECT p FROM Padron p JOIN FETCH p.iglesiaPersona ip JOIN FETCH ip.persona"
                    + " JOIN FETCH p.mesa WHERE p.proceso.id = :proceso AND ip.persona.id IN :personas", Padron.class)
                    .setParameter("proceso", proceso).setParameter("personas", personas).getResultList();
        } finally { session.enableFilter(EntidadBase.FILTER_ACTIVE); }
    }

    public boolean iglesiaEnOtraMesa(Integer iglesia, Integer mesa, Integer proceso) {
        return getEntityManager().createQuery("SELECT COUNT(p.id) FROM Padron p WHERE p.estado = TRUE"
                + " AND p.proceso.id = :proceso AND p.mesa.id <> :mesa AND p.iglesiaPersona.iglesia.id = :iglesia", Long.class)
                .setParameter("proceso", proceso).setParameter("mesa", mesa).setParameter("iglesia", iglesia).getSingleResult() > 0;
    }

    public List<Padron> seleccion(List<Integer> ids, Integer mesa, Integer proceso) {
        return getEntityManager().createQuery("SELECT p FROM Padron p JOIN FETCH p.iglesiaPersona"
                + " WHERE p.id IN :ids AND p.mesa.id = :mesa AND p.proceso.id = :proceso AND p.estado = TRUE", Padron.class)
                .setParameter("ids", ids).setParameter("mesa", mesa).setParameter("proceso", proceso).getResultList();
    }

    public boolean tieneJrv(List<Integer> miembros, Integer proceso) {
        return getEntityManager().createQuery("SELECT COUNT(j.id) FROM MiembroJRV j WHERE j.estado = TRUE"
                + " AND j.proceso.id = :proceso AND j.iglesiaPersona.id IN :ids", Long.class)
                .setParameter("proceso", proceso).setParameter("ids", miembros).getSingleResult() > 0;
    }

    public boolean escrutinioIniciado(Integer mesa, Integer proceso) {
        return getEntityManager().createQuery("SELECT COUNT(e.id) FROM EscrutinioCabecera e WHERE e.estado = TRUE"
                + " AND e.mesa.id = :mesa AND e.proceso.id = :proceso AND e.estadoEscrutinio <> :pendiente", Long.class)
                .setParameter("mesa", mesa).setParameter("proceso", proceso)
                .setParameter("pendiente", ec.com.antenasur.enums.EstadoEscrutinio.PENDIENTE).getSingleResult() > 0;
    }

    public void guardar(List<Padron> registros) {
        var em = getEntityManager();
        for (Padron p : registros) if (p.getId() == null) em.persist(p);
        em.flush();
    }
}
