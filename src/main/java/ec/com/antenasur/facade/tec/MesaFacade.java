package ec.com.antenasur.facade.tec;

import ec.com.antenasur.model.Geograp;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.ejb.Stateless;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.model.tec.Recinto;
import ec.com.antenasur.enums.EstadoTarea;
import ec.com.antenasur.model.generic.AbstractFacade;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class MesaFacade extends AbstractFacade<Mesa, Integer> {

    private static final String HQL = " SELECT m FROM Mesa m";
    private static final String ACTIVOS = " m.estado = TRUE";
    private static final String ORDENADO = " ORDER BY m.id";

    public MesaFacade() {
        super(Mesa.class, Integer.class);
    }

    /**
     * Suma {@code totalVotos} de todas las mesas activas en una sola query
     * agregada. Reemplaza el patrÃƒÂ³n anti-rendimiento de cargar TODAS las
     * mesas y sumar en Java (que en MesaBean.totalVotantes hacÃƒÂ­a findAll() +
     * iteraciÃƒÂ³n, generando lazy-loads N+1 al accionar getTotalVotos en
     * algunas implementaciones JPA).
     */
    public long sumTotalVotos() {
        try {
            String sql = "SELECT COALESCE(SUM(m.totalVotos), 0) FROM Mesa m WHERE " + ACTIVOS;
            Long total = super.getEntityManager().createQuery(sql, Long.class).getSingleResult();
            return total != null ? total : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     *
     * @param nombreRecinto
     * @return
     */
    public Mesa buscaRecintoPorNombre(String nombreRecinto) {
        try {
            String sql = HQL + " WHERE m.nombre=:nombreRecinto AND " + ACTIVOS;
            TypedQuery<Mesa> query = super.getEntityManager().createQuery(sql, Mesa.class);
            query.setParameter("nombreRecinto", nombreRecinto);
            Mesa result = query.getSingleResult();
            if (result != null) {
                return result;
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;
    }

    public List<Mesa> getMesasPorParroquias(List<Geograp> parroquias) {
        try {
            String sql = HQL + " LEFT JOIN FETCH m.ubicacion ub"
                    + " WHERE ub IN :parroquias AND " + ACTIVOS + ORDENADO;
            TypedQuery<Mesa> query = super.getEntityManager().createQuery(sql, Mesa.class);
            query.setParameter("parroquias", parroquias);
            List<Mesa> result = query.getResultList();
            if (result.size() > 0) {
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public List<Mesa> getMesasPorRecintos(List<Recinto> recintos) {
        try {
            String sql = HQL + " LEFT JOIN FETCH m.recinto r WHERE r IN :recintos AND m.estado=TRUE ORDER BY m.id";
            TypedQuery<Mesa> query = super.getEntityManager().createQuery(sql, Mesa.class);
            query.setParameter("recintos", recintos);
            List<Mesa> result = query.getResultList();
            if (result.size() > 0) {
                return result;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public List<Mesa> getMesasEscrutadasPorRecintos(List<Recinto> recintos) {
        try {
            String sql = HQL + " LEFT JOIN FETCH m.recinto r "
                    + "WHERE r IN :recintos AND m.estadoTarea=:estadoTarea AND m.estado=TRUE ORDER BY m.id";
            TypedQuery<Mesa> query = super.getEntityManager().createQuery(sql, Mesa.class);
            query.setParameter("recintos", recintos);
            query.setParameter("estadoTarea", EstadoTarea.COMPLETADO);
            List<Mesa> result = query.getResultList();
            if (result.size() > 0) {
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public List<Mesa> mesasEscrutadas(EstadoTarea estadoTarea) {

        try {
            String sql = HQL + " LEFT JOIN FETCH m.ubicacion ub"
                    + " WHERE m.estadoTarea=:estadoTarea AND " + ACTIVOS + ORDENADO;
            TypedQuery<Mesa> query = super.getEntityManager().createQuery(sql, Mesa.class);
            //query.setParameter("ids", listaIdParroquias);
            query.setParameter("estadoTarea", estadoTarea);
            List<Mesa> result = query.getResultList();
            if (result.size() > 0) {
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public List<Mesa> getMesasPorRecinto(Recinto recinto) {
        try {
            String sql = HQL + " LEFT JOIN FETCH m.recinto r WHERE r = :recintos AND m.estado=TRUE ORDER BY m.id";
            TypedQuery<Mesa> query = super.getEntityManager().createQuery(sql, Mesa.class);
            query.setParameter("recinto", recinto);
            List<Mesa> result = query.getResultList();
            if (result.size() > 0) {
                return result;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public Mesa getMesaPorUsuario(String usuario) {
        try {
            String sql = HQL + " LEFT JOIN FETCH m.recinto r "
                    + " LEFT JOIN FETCH m.ubicacion u "
                    + "WHERE m.responsable = :usuario AND m.estado=TRUE ORDER BY m.id";
            TypedQuery<Mesa> query = super.getEntityManager().createQuery(sql, Mesa.class);
            query.setParameter("usuario", usuario);
            List<Mesa> result = query.getResultList();
            if (result.size() > 0) {
                return result.get(0);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /**
     *
     * @param nombreRecinto
     * @return
     */
    public Mesa buscaPorNombreMesa(String nombreMesa) {
        try {
            String sql = HQL + " WHERE m.nombre=:nombreMesa AND " + ACTIVOS;
            TypedQuery<Mesa> query = super.getEntityManager().createQuery(sql, Mesa.class);
            query.setParameter("nombreMesa", nombreMesa);
            Mesa result = query.getSingleResult();
            if (result != null) {
                return result;
            }
        } catch (NoResultException e) {
            return null;
        }
        return null;
    }

    /** Obtiene los conteos por recinto sin materializar todas las mesas. */
    public Map<Integer, Long> contarActivasPorRecinto() {
        String hql = "SELECT r.id, COUNT(m.id) FROM Mesa m JOIN m.recinto r "
                + "WHERE m.estado = TRUE GROUP BY r.id";
        List<Object[]> filas = getEntityManager().createQuery(hql, Object[].class).getResultList();
        Map<Integer, Long> resultado = new LinkedHashMap<>();
        for (Object[] fila : filas) {
            resultado.put((Integer) fila[0], (Long) fila[1]);
        }
        return resultado;
    }

    public List<Mesa> listarPorRecinto(Integer recintoId) {
        if (recintoId == null) {
            return java.util.Collections.emptyList();
        }
        String hql = HQL
                + " JOIN FETCH m.recinto r"
                + " LEFT JOIN FETCH r.ubicacion ru"
                + " LEFT JOIN FETCH ru.geograp canton"
                + " LEFT JOIN FETCH canton.geograp provincia"
                + " LEFT JOIN FETCH m.ubicacion mu"
                + " WHERE r.id = :recintoId AND m.estado = TRUE ORDER BY m.nombre, m.id";
        TypedQuery<Mesa> query = getEntityManager().createQuery(hql, Mesa.class);
        query.setParameter("recintoId", recintoId);
        return query.getResultList();
    }

    /** Carga solo mesas que contienen información o documentos del proceso. */
    public List<Mesa> listarPorRecintoYProceso(Integer recintoId, Integer procesoId) {
        if (recintoId == null || procesoId == null) {
            return java.util.Collections.emptyList();
        }
        String hql = HQL
                + " JOIN FETCH m.recinto r"
                + " LEFT JOIN FETCH r.ubicacion ru"
                + " LEFT JOIN FETCH ru.geograp canton"
                + " LEFT JOIN FETCH canton.geograp provincia"
                + " LEFT JOIN FETCH m.ubicacion mu"
                + " WHERE r.id = :recintoId AND m.estado = TRUE AND ("
                + " EXISTS (SELECT p.id FROM Padron p WHERE p.mesa.id = m.id"
                + " AND p.proceso.id = :procesoId AND p.estado = TRUE)"
                + " OR EXISTS (SELECT ec.id FROM EscrutinioCabecera ec WHERE ec.mesa.id = m.id"
                + " AND ec.proceso.id = :procesoId AND ec.estado = TRUE)"
                + " OR EXISTS (SELECT j.id FROM MiembroJRV j WHERE j.mesa.id = m.id"
                + " AND j.proceso.id = :procesoId AND j.estado = TRUE)"
                + " OR EXISTS (SELECT d.id FROM Documentos d WHERE d.mesa.id = m.id"
                + " AND d.proceso.id = :procesoId AND d.estado = TRUE))"
                + " ORDER BY m.nombre, m.id";
        TypedQuery<Mesa> query = getEntityManager().createQuery(hql, Mesa.class);
        query.setParameter("recintoId", recintoId);
        query.setParameter("procesoId", procesoId);
        return query.getResultList();
    }

    public Mesa buscarDetallePorId(Integer mesaId) {
        if (mesaId == null) {
            return null;
        }
        String hql = HQL
                + " JOIN FETCH m.recinto r"
                + " LEFT JOIN FETCH r.ubicacion ru"
                + " LEFT JOIN FETCH ru.geograp canton"
                + " LEFT JOIN FETCH canton.geograp provincia"
                + " LEFT JOIN FETCH m.ubicacion mu"
                + " WHERE m.id = :mesaId AND m.estado = TRUE";
        TypedQuery<Mesa> query = getEntityManager().createQuery(hql, Mesa.class);
        query.setParameter("mesaId", mesaId);
        List<Mesa> resultado = query.getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }

}
