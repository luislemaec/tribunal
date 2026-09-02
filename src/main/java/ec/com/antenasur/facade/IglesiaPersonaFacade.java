/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.facade;

import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.IglesiaPersona;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.ejb.Stateless;
import jakarta.persistence.NoResultException;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;

import ec.com.antenasur.model.generic.AbstractFacade;
import ec.com.antenasur.model.generic.EntidadBase;
import org.hibernate.Filter;
import org.hibernate.Session;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class IglesiaPersonaFacade extends AbstractFacade<IglesiaPersona, Integer> {

    static final String HQL = "SELECT ip FROM IglesiaPersona ip";

    public IglesiaPersonaFacade() {
        super(IglesiaPersona.class, Integer.class);
    }

    /**
     * Devuelve el vÃƒÂ­nculo iglesia-persona vigente mÃƒ¡s reciente para una
     * persona dada. "Vigente" = estado activo. Si la persona pertenece a
     * varias iglesias histÃƒÂ³ricamente, retorna la Ãƒºltima registrada.
     */
    public IglesiaPersona getVigentePorPersonaId(Integer personaId) {
        if (personaId == null) {
            return null;
        }
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH ip.iglesia i"
                    + " WHERE ip.persona.id = :personaId AND ip.estado = TRUE"
                    + " ORDER BY ip.id DESC";
            TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
            query.setParameter("personaId", personaId);
            query.setMaxResults(1);
            List<IglesiaPersona> result = query.getResultList();
            return (result != null && !result.isEmpty()) ? result.get(0) : null;
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<IglesiaPersona> getPersonasIglesiasPorParroquia(Geograp parroquia) {
        if (parroquia == null) {
            return java.util.Collections.emptyList();
        }
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH ip.iglesia igl"
                    + " LEFT JOIN FETCH igl.ubicacion parroquia"
                    + " LEFT JOIN FETCH parroquia.geograp canton"
                    + " LEFT JOIN FETCH canton.geograp provincia"
                    + " LEFT JOIN FETCH ip.persona p"
                    + " WHERE igl.ubicacion = :parroquia AND ip.estado = TRUE"
                    + " ORDER BY ip.id";
            TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
            query.setParameter("parroquia", parroquia);
            List<IglesiaPersona> result = query.getResultList();
            return result != null ? result : java.util.Collections.<IglesiaPersona>emptyList();
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    public List<IglesiaPersona> getPersonasIglesiasPorIglesia(int iglesiaId) {
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH ip.iglesia i"
                    + " LEFT JOIN FETCH i.ubicacion parroquia"
                    + " LEFT JOIN FETCH parroquia.geograp canton"
                    + " LEFT JOIN FETCH canton.geograp provincia"
                    + " LEFT JOIN FETCH ip.persona p"
                    + " WHERE i.id = :iglesiaId AND ip.estado = TRUE"
                    + " ORDER BY ip.id";
            TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
            query.setParameter("iglesiaId", iglesiaId);
            List<IglesiaPersona> result = query.getResultList();
            return result != null ? result : java.util.Collections.<IglesiaPersona>emptyList();
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    public List<IglesiaPersona> getPersonasHabilitadasPadronPorIglesia(int iglesiaId) {
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH ip.iglesia i"
                    + " LEFT JOIN FETCH ip.persona p"
                    + " WHERE i.id = :iglesiaId"
                    + "   AND ip.estado = TRUE"
                    + "   AND p.estado = TRUE"
                    + "   AND ip.habilitadoPadron = TRUE"
                    + " ORDER BY ip.id";
            TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
            query.setParameter("iglesiaId", iglesiaId);
            List<IglesiaPersona> result = query.getResultList();
            return result != null ? result : java.util.Collections.<IglesiaPersona>emptyList();
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    public Map<Integer, Integer> contarPersonasHabilitadasPadronPorIglesias(List<Integer> iglesiaIds) {
        Map<Integer, Integer> resultado = new HashMap<>();
        if (iglesiaIds == null || iglesiaIds.isEmpty()) {
            return resultado;
        }
        try {
            String sql = "SELECT i.id, COUNT(ip.id)"
                    + " FROM IglesiaPersona ip"
                    + " JOIN ip.iglesia i"
                    + " JOIN ip.persona p"
                    + " WHERE i.id IN :iglesiaIds"
                    + "   AND ip.estado = TRUE"
                    + "   AND p.estado = TRUE"
                    + "   AND ip.habilitadoPadron = TRUE"
                    + " GROUP BY i.id";
            List<Object[]> filas = super.getEntityManager()
                    .createQuery(sql, Object[].class)
                    .setParameter("iglesiaIds", iglesiaIds)
                    .getResultList();
            for (Object[] fila : filas) {
                Integer iglesiaId = (Integer) fila[0];
                Number total = (Number) fila[1];
                resultado.put(iglesiaId, total != null ? total.intValue() : 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultado;
    }


    /**
     * Trae IglesiaPersona activos por parroquia(s) hidratando en una sola
     * query las relaciones que la vista/DTO consultan despuÃƒÂ©s
     * ({@code iglesia}, {@code iglesia.ubicacion}, {@code persona}). Evita
     * N+1: sin estos JOIN FETCH, mapear cada IglesiaPersona a DTO disparaba
     * una query por persona y otra por iglesia, multiplicando el tiempo de
     * respuesta hasta sobrepasar el timeout JTA (300s) y romper la
     * transacciÃƒÂ³n.
     *
     * <p>Filtra por {@code ip.estado = TRUE} para excluir soft-deleted y
     * limita por relaciÃƒÂ³n con la lista de parroquias.
     */
    public List<IglesiaPersona> getIglesiasPersonasPorParroquias(List<Geograp> parroquias) {
        if (parroquias == null || parroquias.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH ip.iglesia igl"
                    + " LEFT JOIN FETCH igl.ubicacion ub"
                    + " LEFT JOIN FETCH ub.geograp canton"
                    + " LEFT JOIN FETCH canton.geograp provincia"
                    + " LEFT JOIN FETCH ip.persona p"
                    + " WHERE ub IN :parroquias AND ip.estado = TRUE"
                    + " ORDER BY ip.id";
            TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
            query.setParameter("parroquias", parroquias);
            List<IglesiaPersona> result = query.getResultList();
            return result != null ? result : java.util.Collections.<IglesiaPersona>emptyList();
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Devuelve el vÃƒÂ­nculo activo mÃƒ¡s reciente para la persona identificada
     * por su DOCUMENTO (cÃƒÂ©dula), independiente del id interno de la persona.
     *
     * <p>Pensado para entornos donde existen filas duplicadas en
     * {@code tb_persona} con el mismo documento (caso real en producciÃƒÂ³n).
     * El mÃƒÂ©todo {@link #getVigentePorPersonaId(Integer)} requiere conocer el
     * id exacto, pero {@code finByPersonaDocument} devuelve la persona con
     * id ASC y el vÃƒÂ­nculo en {@code tb_iglesia_persona} podrÃƒÂ­a apuntar al
     * id duplicado mayor Ã¢â‚¬” generando "sin iglesia" falso. Esta variante
     * resuelve por documento y evita ese problema.
     */
    public IglesiaPersona getVigentePorDocumentoPersona(String documento) {
        if (documento == null || documento.trim().isEmpty()) {
            return null;
        }
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH ip.iglesia i"
                    + " LEFT JOIN FETCH ip.persona p"
                    + " WHERE p.documento = :documento"
                    + "   AND ip.estado = TRUE"
                    + "   AND p.estado = TRUE"
                    + " ORDER BY ip.id DESC";
            TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
            query.setParameter("documento", documento.trim());
            query.setMaxResults(1);
            List<IglesiaPersona> result = query.getResultList();
            return (result != null && !result.isEmpty()) ? result.get(0) : null;
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Lista todos los vinculos activos de una persona usando el documento
     * institucional como identidad funcional. La variante con bloqueo se usa
     * antes de altas y regularizaciones para serializar operaciones concurrentes.
     */
    public List<IglesiaPersona> listarActivasPorDocumento(String documento, boolean bloquear) {
        if (documento == null || documento.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        String fetchUbicacion = bloquear ? ""
                : " LEFT JOIN FETCH i.ubicacion parroquia"
                + " LEFT JOIN FETCH parroquia.geograp canton"
                + " LEFT JOIN FETCH canton.geograp provincia";
        String sql = HQL
                + " JOIN FETCH ip.iglesia i"
                + fetchUbicacion
                + " JOIN FETCH ip.persona p"
                + " WHERE TRIM(p.documento) = :documento"
                + "   AND ip.estado = TRUE"
                + "   AND p.estado = TRUE"
                + " ORDER BY ip.id";
        TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
        query.setParameter("documento", documento.trim());
        if (bloquear) {
            query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        }
        List<IglesiaPersona> resultado = query.getResultList();
        return resultado != null ? resultado : java.util.Collections.emptyList();
    }

    public List<IglesiaPersona> listarActivasPorDocumento(String documento) {
        return listarActivasPorDocumento(documento, false);
    }

    /**
     * Cuenta iglesias activas distintas por documento en una sola consulta.
     * El documento es la identidad funcional porque existen personas historicas
     * duplicadas con distintos ids internos.
     */
    public Map<String, Integer> contarIglesiasActivasPorDocumentos(Collection<String> documentos) {
        Map<String, Integer> resultado = new LinkedHashMap<>();
        if (documentos == null || documentos.isEmpty()) {
            return resultado;
        }
        String hql = "SELECT TRIM(p.documento), COUNT(DISTINCT i.id)"
                + " FROM IglesiaPersona ip"
                + " JOIN ip.iglesia i"
                + " JOIN ip.persona p"
                + " WHERE ip.estado = TRUE"
                + "   AND p.estado = TRUE"
                + "   AND TRIM(p.documento) IN :documentos"
                + " GROUP BY TRIM(p.documento)";
        List<Object[]> filas = getEntityManager().createQuery(hql, Object[].class)
                .setParameter("documentos", documentos)
                .getResultList();
        for (Object[] fila : filas) {
            resultado.put((String) fila[0], ((Number) fila[1]).intValue());
        }
        return resultado;
    }

    /** Obtiene los documentos que actualmente pertenecen a mas de una iglesia. */
    public List<String> listarDocumentosConMultiplesIglesiasActivas() {
        String hql = "SELECT TRIM(p.documento)"
                + " FROM IglesiaPersona ip"
                + " JOIN ip.iglesia i"
                + " JOIN ip.persona p"
                + " WHERE ip.estado = TRUE"
                + "   AND p.estado = TRUE"
                + "   AND p.documento IS NOT NULL"
                + " GROUP BY TRIM(p.documento)"
                + " HAVING COUNT(DISTINCT i.id) > 1"
                + " ORDER BY TRIM(p.documento)";
        return getEntityManager().createQuery(hql, String.class).getResultList();
    }

    /**
     * Carga en bloque todas las relaciones de los documentos inconsistentes,
     * incluyendo ubicacion completa. No filtra el estado de la relacion para
     * que el reporte preserve tambien la trazabilidad historica.
     */
    public List<IglesiaPersona> listarRelacionesPorDocumentos(Collection<String> documentos) {
        if (documentos == null || documentos.isEmpty()) {
            return new ArrayList<>();
        }
        Session session = getEntityManager().unwrap(Session.class);
        Filter filtro = session.getEnabledFilter(EntidadBase.FILTER_ACTIVE);
        if (filtro != null) {
            session.disableFilter(EntidadBase.FILTER_ACTIVE);
        }
        String hql = HQL
                + " JOIN FETCH ip.iglesia i"
                + " LEFT JOIN FETCH i.ubicacion parroquia"
                + " LEFT JOIN FETCH parroquia.geograp canton"
                + " LEFT JOIN FETCH canton.geograp provincia"
                + " JOIN FETCH ip.persona p"
                + " WHERE TRIM(p.documento) IN :documentos"
                + " ORDER BY TRIM(p.documento), ip.estado DESC, i.nombre, ip.id";
        try {
            return session.createQuery(hql, IglesiaPersona.class)
                    .setParameter("documentos", documentos)
                    .getResultList();
        } finally {
            if (filtro != null) {
                session.enableFilter(EntidadBase.FILTER_ACTIVE);
            }
        }
    }

    /** Fuerza las bajas pendientes antes de actualizar la relacion definitiva. */
    public void flushCambios() {
        getEntityManager().flush();
    }

    /**
     * Devuelve el vÃƒÂ­nculo activo entre la iglesia y la persona indicadas, o
     * {@code null} si no existe ninguno. ÃƒÅ¡til para garantizar idempotencia al
     * crear el vÃƒÂ­nculo desde el flujo de asignaciÃƒÂ³n de admins.
     */
    public IglesiaPersona findByIglesiaAndPersona(Integer iglesiaId, Integer personaId) {
        if (iglesiaId == null || personaId == null) {
            return null;
        }
        try {
            String sql = HQL
                    + " WHERE ip.iglesia.id = :iglesiaId"
                    + "   AND ip.persona.id = :personaId"
                    + "   AND ip.estado = TRUE"
                    + " ORDER BY ip.id DESC";
            TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
            query.setParameter("iglesiaId", iglesiaId);
            query.setParameter("personaId", personaId);
            query.setMaxResults(1);
            List<IglesiaPersona> result = query.getResultList();
            return (result != null && !result.isEmpty()) ? result.get(0) : null;
        } catch (NoResultException e) {
            return null;
        }
    }

    public IglesiaPersona buscarPorCedulaPersona(String cedula) {
        try {
            String sql = HQL
                    + " LEFT JOIN FETCH ip.persona p"
                    + " WHERE p.documento=:cedula ORDER BY ip.id";
            TypedQuery<IglesiaPersona> query = super.getEntityManager().createQuery(sql, IglesiaPersona.class);
            query.setParameter("cedula", cedula);
            List<IglesiaPersona> result = query.getResultList();
            if (result.size() > 0) {
                return result.get(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

}
