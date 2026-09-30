package ec.com.antenasur.facade.tec;

import java.util.List;

import ec.com.antenasur.model.generic.AbstractFacade;
import ec.com.antenasur.enums.EstadoEscrutinio;
import ec.com.antenasur.model.tec.EscrutinioCabecera;
import jakarta.ejb.Stateless;
import jakarta.persistence.TypedQuery;

@Stateless
public class EscrutinioCabeceraFacade extends AbstractFacade<EscrutinioCabecera, Integer> {

    public EscrutinioCabeceraFacade() {
        super(EscrutinioCabecera.class, Integer.class);
    }

    /**
     * Condición JPQL de dato oficial: la mesa tiene, en el proceso, un acta física
     * vigente VALIDADA. Usa el parámetro :procesoId de la consulta que la incluye y los
     * que fija {@link #parametrosActaFisicaValidada}. La cubre el índice
     * idx_documentos_mesa_proceso_tipo_revision (V3).
     *
     * @param mesaId expresión JPQL con el id de la mesa (p. ej. "m.id")
     */
    public static String existeActaFisicaValidada(String mesaId) {
        return " EXISTS (SELECT d.id FROM Documentos d"
                + " WHERE d.mesa.id = " + mesaId
                + " AND d.proceso.id = :procesoId"
                + " AND d.estado = TRUE"
                + " AND d.estadoRevision = :revisionValidada"
                + " AND UPPER(d.tipoDocumento.nombre) = :tipoActaFisica)";
    }

    public static void parametrosActaFisicaValidada(jakarta.persistence.Query query) {
        query.setParameter("revisionValidada", ec.com.antenasur.service.tec.ActaFisicaEscrutinioService.VALIDADA);
        query.setParameter("tipoActaFisica", ec.com.antenasur.service.tec.ActaFisicaEscrutinioService.TIPO_DOCUMENTO);
    }

    public EscrutinioCabecera buscarPorMesaProceso(Integer mesaId, Integer procesoId) {
        if (mesaId == null || procesoId == null) {
            return null;
        }
        try {
            String sql = "SELECT e FROM EscrutinioCabecera e"
                    + " LEFT JOIN FETCH e.mesa m"
                    + " LEFT JOIN FETCH m.recinto r"
                    + " LEFT JOIN FETCH e.proceso pro"
                    + " WHERE m.id = :mesaId AND pro.id = :procesoId AND e.estado = TRUE";
            TypedQuery<EscrutinioCabecera> query = super.getEntityManager()
                    .createQuery(sql, EscrutinioCabecera.class);
            query.setParameter("mesaId", mesaId);
            query.setParameter("procesoId", procesoId);
            List<EscrutinioCabecera> resultado = query.getResultList();
            return resultado.isEmpty() ? null : resultado.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Cabeceras activas de todas las mesas del proceso en una sola consulta, indexadas
     * por mesa. Evita una consulta por fila al construir el listado de escrutinios.
     */
    public java.util.Map<Integer, EscrutinioCabecera> buscarPorProcesoIndexadoPorMesa(Integer procesoId) {
        java.util.Map<Integer, EscrutinioCabecera> resultado = new java.util.HashMap<>();
        if (procesoId == null) {
            return resultado;
        }
        try {
            String sql = "SELECT e FROM EscrutinioCabecera e"
                    + " LEFT JOIN FETCH e.mesa m"
                    + " LEFT JOIN FETCH m.recinto r"
                    + " WHERE e.proceso.id = :procesoId AND e.estado = TRUE";
            for (EscrutinioCabecera cabecera : super.getEntityManager()
                    .createQuery(sql, EscrutinioCabecera.class)
                    .setParameter("procesoId", procesoId).getResultList()) {
                if (cabecera.getMesa() != null && cabecera.getMesa().getId() != null) {
                    resultado.put(cabecera.getMesa().getId(), cabecera);
                }
            }
        } catch (Exception e) {
            return resultado;
        }
        return resultado;
    }

    /** Mesas cerradas y con acta física VALIDADA (datos oficiales) del proceso. */
    public long contarCerradasPorProceso(Integer procesoId) {
        if (procesoId == null) {
            return 0L;
        }
        String sql = "SELECT COUNT(e.id) FROM EscrutinioCabecera e"
                + " JOIN e.proceso pro"
                + " WHERE pro.id = :procesoId"
                + " AND e.estado = TRUE"
                + " AND e.estadoEscrutinio = :estadoCerrado"
                + " AND" + existeActaFisicaValidada("e.mesa.id");
        TypedQuery<Long> query = super.getEntityManager().createQuery(sql, Long.class)
                .setParameter("procesoId", procesoId)
                .setParameter("estadoCerrado", EstadoEscrutinio.CERRADO);
        parametrosActaFisicaValidada(query);
        Long total = query.getSingleResult();
        return total != null ? total : 0L;
    }

    /**
     * Mesas del proceso agrupadas por estado de escrutinio, en una sola consulta
     * agregada. Alimenta los indicadores gerenciales del panel sin recorrer las
     * cabeceras ni lanzar un COUNT por estado.
     *
     * @return mapa estado → número de mesas; los estados sin mesas no aparecen.
     */
    public java.util.Map<EstadoEscrutinio, Long> contarPorEstadoProceso(Integer procesoId) {
        java.util.Map<EstadoEscrutinio, Long> resultado = new java.util.EnumMap<>(EstadoEscrutinio.class);
        if (procesoId == null) {
            return resultado;
        }
        String sql = "SELECT e.estadoEscrutinio, COUNT(e.id) FROM EscrutinioCabecera e"
                + " JOIN e.proceso pro"
                + " WHERE pro.id = :procesoId AND e.estado = TRUE"
                + " GROUP BY e.estadoEscrutinio";
        List<Object[]> filas = super.getEntityManager().createQuery(sql, Object[].class)
                .setParameter("procesoId", procesoId).getResultList();
        for (Object[] fila : filas) {
            if (fila[0] != null) {
                resultado.put((EstadoEscrutinio) fila[0], ((Number) fila[1]).longValue());
            }
        }
        return resultado;
    }

    /**
     * Fecha y hora de validación del acta física vigente de cada mesa del proceso: la
     * {@code fechaRevision} del documento VALIDADO, con las mismas condiciones que
     * {@link #existeActaFisicaValidada}. Una sola consulta para todo el listado público.
     *
     * @return mapa id de mesa → fecha de validación
     */
    public java.util.Map<Integer, java.util.Date> fechasValidacionPorProceso(Integer procesoId) {
        java.util.Map<Integer, java.util.Date> resultado = new java.util.HashMap<>();
        if (procesoId == null) {
            return resultado;
        }
        String sql = "SELECT d.mesa.id, MAX(d.fechaRevision) FROM Documentos d"
                + " WHERE d.proceso.id = :procesoId"
                + " AND d.estado = TRUE"
                + " AND d.estadoRevision = :revisionValidada"
                + " AND UPPER(d.tipoDocumento.nombre) = :tipoActaFisica"
                + " GROUP BY d.mesa.id";
        jakarta.persistence.TypedQuery<Object[]> query = super.getEntityManager().createQuery(sql, Object[].class)
                .setParameter("procesoId", procesoId);
        parametrosActaFisicaValidada(query);
        for (Object[] fila : query.getResultList()) {
            resultado.put((Integer) fila[0], (java.util.Date) fila[1]);
        }
        return resultado;
    }

    /** Cabeceras cerradas y con acta física VALIDADA (datos oficiales) del proceso. */
    public List<EscrutinioCabecera> listarCerradasPorProceso(Integer procesoId) {
        if (procesoId == null) {
            return java.util.Collections.emptyList();
        }
        String sql = "SELECT e FROM EscrutinioCabecera e"
                + " JOIN FETCH e.mesa m"
                + " JOIN FETCH m.recinto r"
                + " LEFT JOIN FETCH r.ubicacion parroquia"
                + " LEFT JOIN FETCH parroquia.geograp canton"
                + " LEFT JOIN FETCH canton.geograp provincia"
                + " JOIN FETCH e.proceso pro"
                + " WHERE pro.id = :procesoId"
                + " AND e.estado = TRUE"
                + " AND e.estadoEscrutinio = :estadoCerrado"
                + " AND" + existeActaFisicaValidada("m.id")
                + " ORDER BY provincia.name, canton.name, parroquia.name, r.nombre, m.nombre";
        TypedQuery<EscrutinioCabecera> query = super.getEntityManager()
                .createQuery(sql, EscrutinioCabecera.class);
        query.setParameter("procesoId", procesoId);
        query.setParameter("estadoCerrado", EstadoEscrutinio.CERRADO);
        parametrosActaFisicaValidada(query);
        return query.getResultList();
    }
}
