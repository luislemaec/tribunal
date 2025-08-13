package ec.com.antenasur.service.tec;

import ec.com.antenasur.domain.Geograp;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import ec.com.antenasur.domain.tec.Mesa;
import ec.com.antenasur.domain.tec.Recinto;
import ec.com.antenasur.enums.EstadoTarea;
import ec.com.antenasur.domain.generic.AbstractFacade;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class MesaFacade extends AbstractFacade<Mesa, Integer> {

    private static final String HQL = " FROM Mesa m";
    private static final String ACTIVOS = " m.estado = TRUE";
    private static final String ORDENADO = " ORDER BY m.id";

    public MesaFacade() {
        super(Mesa.class, Integer.class);
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

}
