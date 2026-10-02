package ec.com.antenasur.facade.tec;

import jakarta.ejb.Stateless;

import ec.com.antenasur.model.tec.Escrutinio;
import ec.com.antenasur.dto.ResultadoCategoriaPublicaDTO;
import ec.com.antenasur.enums.EstadoEscrutinio;
import ec.com.antenasur.model.generic.AbstractFacade;
import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import java.util.List;
import jakarta.persistence.TypedQuery;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class EscrutinioFacade extends AbstractFacade<Escrutinio, Integer> {

    private static final String HQL = " SELECT e FROM Escrutinio e";
    private static final String ORDENADO = " ORDER BY e.id";

    public EscrutinioFacade() {
        super(Escrutinio.class, Integer.class);
    }

    public List<Escrutinio> buscaPorMesa(Mesa mesa) {
        return buscaPorMesaYProceso(mesa, null);
    }

    public List<Escrutinio> buscaPorMesaYProceso(Mesa mesa, ProcesoElectoral proceso) {
        try {
            String sql = HQL + " WHERE e.mesa=:mesa";
            if (proceso != null) {
                sql += " AND e.proceso = :proceso";
            }
            sql += " ORDER BY e.categoria.orden";
            TypedQuery<Escrutinio> query = super.getEntityManager().createQuery(sql, Escrutinio.class);
            query.setParameter("mesa", mesa);
            if (proceso != null) {
                query.setParameter("proceso", proceso);
            }
            List<Escrutinio> result = query.getResultList();
            if (result != null) {
                return result;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public List<Escrutinio> listarPorMesaProceso(Integer mesaId, Integer procesoId) {
        if (mesaId == null || procesoId == null) {
            return java.util.Collections.emptyList();
        }
        String hql = HQL
                + " JOIN FETCH e.mesa m"
                + " JOIN FETCH e.proceso pro"
                + " JOIN FETCH e.categoria cat"
                + " WHERE m.id = :mesaId AND pro.id = :procesoId"
                + " AND e.estado = TRUE ORDER BY cat.orden, e.id";
        TypedQuery<Escrutinio> query = getEntityManager().createQuery(hql, Escrutinio.class);
        query.setParameter("mesaId", mesaId);
        query.setParameter("procesoId", procesoId);
        return query.getResultList();
    }
    
    public List<Escrutinio> buscaCanton(Mesa mesa) {
        try {
            String sql = HQL + " WHERE e.mesa=:mesa ORDER BY e.categoria.orden";
            TypedQuery<Escrutinio> query = super.getEntityManager().createQuery(sql, Escrutinio.class);
            query.setParameter("mesa", mesa);
            List<Escrutinio> result = query.getResultList();
            if (result != null) {
                return result;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public List<ResultadoCategoriaPublicaDTO> obtenerResultadosPublicosPorCategoria(Integer procesoId) {
        return obtenerResultadosValidadosPorCategoria(procesoId, null);
    }

    /**
     * Votos por categoría con las condiciones oficiales (mesa cerrada y acta física VALIDADA del
     * proceso). Con {@code mesaIds} se limita a esas mesas (filtro geográfico de escrutinios);
     * con null abarca todo el proceso, como el portal público.
     */
    public List<ResultadoCategoriaPublicaDTO> obtenerResultadosValidadosPorCategoria(Integer procesoId,
            java.util.Collection<Integer> mesaIds) {
        if (procesoId == null || (mesaIds != null && mesaIds.isEmpty())) {
            return java.util.Collections.emptyList();
        }
        String sql = "SELECT new ec.com.antenasur.dto.ResultadoCategoriaPublicaDTO("
                + " c.id, c.nombre, COALESCE(SUM(e.totalVotos), 0), c.orden,"
                + " c.tipo, l.numero, l.nombre, l.slogan)"
                + " FROM Escrutinio e"
                + " JOIN e.categoria c"
                + " LEFT JOIN c.lista l"
                + " JOIN e.mesa m"
                + " JOIN e.proceso pro"
                + " WHERE pro.id = :procesoId"
                + " AND e.estado = TRUE"
                + " AND EXISTS ("
                + "     SELECT cab.id FROM EscrutinioCabecera cab"
                + "     WHERE cab.mesa.id = m.id"
                + "     AND cab.proceso.id = :procesoId"
                + "     AND cab.estado = TRUE"
                + "     AND cab.estadoEscrutinio = :estadoCerrado"
                + " )"
                // Solo datos oficiales: mesas con acta física VALIDADA.
                + " AND" + EscrutinioCabeceraFacade.existeActaFisicaValidada("m.id")
                + (mesaIds != null ? " AND m.id IN :mesaIds" : "")
                + " GROUP BY c.id, c.nombre, c.orden, c.tipo, l.numero, l.nombre, l.slogan"
                + " ORDER BY c.orden, c.nombre";
        TypedQuery<ResultadoCategoriaPublicaDTO> query = super.getEntityManager()
                .createQuery(sql, ResultadoCategoriaPublicaDTO.class);
        query.setParameter("procesoId", procesoId);
        query.setParameter("estadoCerrado", EstadoEscrutinio.CERRADO);
        if (mesaIds != null) {
            query.setParameter("mesaIds", mesaIds);
        }
        EscrutinioCabeceraFacade.parametrosActaFisicaValidada(query);
        return query.getResultList();
    }

    /**
     * Votos de cada mesa por categoría, con las mismas condiciones que
     * {@link #obtenerResultadosPublicosPorCategoria}: mesa cerrada y acta física validada.
     * Una sola consulta agregada para todo el listado público.
     *
     * @return filas [mesaId, categoriaId, tipo, nombre de la categoría, votos]
     */
    public List<Object[]> obtenerVotosPublicosPorMesaYCategoria(Integer procesoId) {
        if (procesoId == null) {
            return java.util.Collections.emptyList();
        }
        String sql = "SELECT m.id, c.id, c.tipo, c.nombre, COALESCE(SUM(e.totalVotos), 0)"
                + " FROM Escrutinio e"
                + " JOIN e.categoria c"
                + " JOIN e.mesa m"
                + " JOIN e.proceso pro"
                + " WHERE pro.id = :procesoId"
                + " AND e.estado = TRUE"
                + " AND EXISTS ("
                + "     SELECT cab.id FROM EscrutinioCabecera cab"
                + "     WHERE cab.mesa.id = m.id"
                + "     AND cab.proceso.id = :procesoId"
                + "     AND cab.estado = TRUE"
                + "     AND cab.estadoEscrutinio = :estadoCerrado"
                + " )"
                + " AND" + EscrutinioCabeceraFacade.existeActaFisicaValidada("m.id")
                + " GROUP BY m.id, c.id, c.tipo, c.nombre";
        TypedQuery<Object[]> query = super.getEntityManager().createQuery(sql, Object[].class);
        query.setParameter("procesoId", procesoId);
        query.setParameter("estadoCerrado", EstadoEscrutinio.CERRADO);
        EscrutinioCabeceraFacade.parametrosActaFisicaValidada(query);
        return query.getResultList();
    }

}
