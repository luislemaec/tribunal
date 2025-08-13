package ec.com.antenasur.service.tec;

import javax.ejb.Stateless;

import ec.com.antenasur.domain.Geograp;
import ec.com.antenasur.domain.generic.AbstractFacade;
import ec.com.antenasur.domain.tec.VwTotalVotos;
import ec.com.antenasur.domain.tec.Mesa;
import ec.com.antenasur.domain.tec.Recinto;

import java.util.List;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class VwTotalVotosFacade extends AbstractFacade<VwTotalVotos, Integer> {

    private static final String HQL = " FROM VwTotalVotos e";
    private static final String ORDENADO = " ORDER BY e.id";

    public VwTotalVotosFacade() {
        super(VwTotalVotos.class, Integer.class);
    }

    public List<VwTotalVotos> buscaPorMesa(VwTotalVotos mesa) {
        try {
            String sql = HQL + " WHERE e.mesa=:mesa ORDER BY e.categoria.orden";
            TypedQuery<VwTotalVotos> query = super.getEntityManager().createQuery(sql, VwTotalVotos.class);
            query.setParameter("mesa", mesa);
            List<VwTotalVotos> result = query.getResultList();
            if (result != null) {
                return result;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public List<VwTotalVotos> buscaCanton(Mesa mesa) {
        try {
            String sql = HQL + " WHERE e.mesa=:mesa ORDER BY e.categoria.orden";
            TypedQuery<VwTotalVotos> query = super.getEntityManager().createQuery(sql, VwTotalVotos.class);
            query.setParameter("mesa", mesa);
            List<VwTotalVotos> result = query.getResultList();
            if (result != null) {
                return result;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public List<Object[]> sumaGlobal() {
        try {
            String sql = "SELECT v.categoria, SUM(v.totalVotos), v.orden "
                    + "FROM VwTotalVotos v"
                    + " GROUP BY v.categoria, v.orden ORDER BY v.orden";
            Query query = super.getEntityManager().createQuery(sql);
            List<Object[]> resultList = query.getResultList();

            if (resultList != null) {
                return resultList;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public List<Object[]> votosPorRecinto(Recinto recintoSeleccionado) {
        try {
            String sql = "SELECT v.categoria, SUM(v.totalVotos), v.orden "
                    + "FROM VwTotalVotos v "
                    + "WHERE v.recinto=:recinto "
                    + " GROUP BY v.categoria, v.orden ORDER BY v.orden";
            Query query = super.getEntityManager().createQuery(sql);
            query.setParameter("recinto", recintoSeleccionado);
            List<Object[]> resultList = query.getResultList();

            if (resultList != null) {
                return resultList;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public List<Object[]> votosPorParroquias(List<Geograp> parroquias) {
        try {
            String sql = "SELECT v.categoria, SUM(v.totalVotos), v.orden "
                    + "FROM VwTotalVotos v "
                    + "WHERE v.mesa.ubicacion IN :parroquias "
                    + " GROUP BY v.categoria, v.orden ORDER BY v.orden";
            Query query = super.getEntityManager().createQuery(sql);
            query.setParameter("parroquias", parroquias);
            List<Object[]> resultList = query.getResultList();

            if (resultList != null) {
                return resultList;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public List<Object[]> votosPorMesa(Mesa mesa) {
        try {
            String sql = "SELECT v.categoria, SUM(v.totalVotos), v.orden "
                    + "FROM VwTotalVotos v "
                    + "WHERE v.mesa=:mesa "
                    + " GROUP BY v.categoria, v.orden ORDER BY v.orden";
            Query query = super.getEntityManager().createQuery(sql);
            query.setParameter("mesa", mesa);
            List<Object[]> resultList = query.getResultList();
            if (resultList != null) {
                return resultList;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public List<Object[]> votosPorRecintos(List<Recinto> recintos) {
        try {
            String sql = "SELECT v.categoria, SUM(v.totalVotos), v.orden "
                    + "FROM VwTotalVotos v "
                    + "WHERE v.recinto IN :recintos "
                    + " GROUP BY v.categoria, v.orden ORDER BY v.orden";
            Query query = super.getEntityManager().createQuery(sql);
            query.setParameter("recintos", recintos);
            List<Object[]> resultList = query.getResultList();
            if (resultList != null) {
                return resultList;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public List<Object[]> votosPorMesas(List<Mesa> mesas) {
        try {
            String sql = "SELECT v.categoria, SUM(v.totalVotos), v.orden "
                    + "FROM VwTotalVotos v "
                    + "WHERE v.mesa IN :mesas "
                    + " GROUP BY v.categoria, v.orden ORDER BY v.orden";
            Query query = super.getEntityManager().createQuery(sql);
            query.setParameter("mesas", mesas);
            List<Object[]> resultList = query.getResultList();
            if (resultList != null) {
                return resultList;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

}
