package ec.com.antenasur.facade.tec;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.hibernate.Session;

import ec.com.antenasur.dto.AyudaSinRespuestaDTO;
import ec.com.antenasur.model.Rol;
import ec.com.antenasur.model.generic.AbstractFacade;
import ec.com.antenasur.model.generic.EntidadBase;
import ec.com.antenasur.model.tec.AyudaContacto;
import ec.com.antenasur.model.tec.AyudaPregunta;

/**
 * Consultas del chatbot de ayuda. La búsqueda usa el índice de texto de PostgreSQL
 * (configuración {@code spanish}, sin extensiones) con SQL nativo; los parámetros de
 * texto nunca son nulos para que PostgreSQL pueda inferir su tipo.
 */
@Stateless
public class AyudaFacade extends AbstractFacade<AyudaPregunta, Integer> {

    /** Visible para el usuario: sin roles asignados o con alguno de sus roles. */
    private static final String VISIBLE_PARA_ROLES = " AND (NOT EXISTS (SELECT 1 FROM tec.ayuda_pregunta_rol pr"
            + " WHERE pr.ayup_id = p.ayup_id) OR EXISTS (SELECT 1 FROM tec.ayuda_pregunta_rol pr"
            + " JOIN public.tb_rol r ON r.rol_id = pr.rol_id WHERE pr.ayup_id = p.ayup_id AND r.rol_nombre IN (:roles)))";

    private static final String COLUMNAS = "SELECT p.ayup_id, p.ayup_pregunta, p.ayup_respuesta, p.ayup_enlace_pagina";

    public AyudaFacade() {
        super(AyudaPregunta.class, Integer.class);
    }

    /** Preguntas sugeridas para la pantalla y la fase, de lo más específico a lo general. */
    public List<Object[]> sugerencias(List<String> roles, String pagina, String fase, int limite) {
        return filas(getEntityManager().createNativeQuery(COLUMNAS + " FROM tec.ayuda_pregunta p"
                + " WHERE p.estado = TRUE" + VISIBLE_PARA_ROLES
                + " AND (p.ayup_pagina IS NULL OR p.ayup_pagina = :pagina)"
                + " AND (p.ayup_fase IS NULL OR p.ayup_fase = :fase)"
                + " ORDER BY CASE WHEN p.ayup_pagina = :pagina THEN 0 ELSE 1 END,"
                + " CASE WHEN p.ayup_fase = :fase THEN 0 ELSE 1 END, p.ayup_orden, p.ayup_id")
                .setParameter("roles", roles).setParameter("pagina", pagina).setParameter("fase", fase)
                .setMaxResults(limite));
    }

    /**
     * Búsqueda por texto. {@code alternativa} = false exige todas las palabras
     * (plainto_tsquery); true acepta cualquiera (to_tsquery con «|»). La relevancia sube
     * si la pregunta es de la pantalla actual o de la fase vigente.
     */
    public List<Object[]> buscar(String consulta, boolean alternativa, List<String> roles, String pagina,
            String fase, int limite) {
        String tsquery = alternativa ? "to_tsquery('spanish', :consulta)" : "plainto_tsquery('spanish', :consulta)";
        return filas(getEntityManager().createNativeQuery(COLUMNAS + " FROM tec.ayuda_pregunta p, " + tsquery + " q"
                + " WHERE p.estado = TRUE AND p.ayup_busqueda @@ q" + VISIBLE_PARA_ROLES
                + " ORDER BY ts_rank(p.ayup_busqueda, q)"
                + " + CASE WHEN p.ayup_pagina = :pagina THEN 0.5 ELSE 0 END"
                + " + CASE WHEN p.ayup_fase = :fase THEN 0.3 ELSE 0 END DESC, p.ayup_orden, p.ayup_id")
                .setParameter("consulta", consulta).setParameter("roles", roles)
                .setParameter("pagina", pagina).setParameter("fase", fase).setMaxResults(limite));
    }

    /** Suma un voto «me sirvió» o «no me sirvió» sin cargar la entidad ni crear revisión. */
    public void valorar(Integer id, boolean util) {
        getEntityManager().createNativeQuery("UPDATE tec.ayuda_pregunta SET "
                + (util ? "ayup_util_si = ayup_util_si + 1" : "ayup_util_no = ayup_util_no + 1")
                + " WHERE ayup_id = :id AND estado = TRUE").setParameter("id", id).executeUpdate();
    }

    /** Registra o incrementa una consulta sin respuesta (agrupada por texto y pantalla). */
    public void registrarSinRespuesta(String texto, String pagina) {
        getEntityManager().createNativeQuery("INSERT INTO tec.ayuda_sin_respuesta"
                + " (ayus_texto, ayus_pagina, ayus_veces, ayus_primera_fecha, ayus_ultima_fecha)"
                + " VALUES (:texto, :pagina, 1, NOW(), NOW())"
                + " ON CONFLICT (ayus_texto, ayus_pagina)"
                + " DO UPDATE SET ayus_veces = tec.ayuda_sin_respuesta.ayus_veces + 1, ayus_ultima_fecha = NOW()")
                .setParameter("texto", texto).setParameter("pagina", pagina).executeUpdate();
    }

    public int purgarSinRespuesta(LocalDateTime anterioresA) {
        return getEntityManager().createNativeQuery("DELETE FROM tec.ayuda_sin_respuesta WHERE ayus_ultima_fecha < :limite")
                .setParameter("limite", Timestamp.valueOf(anterioresA)).executeUpdate();
    }

    public List<AyudaSinRespuestaDTO> listarSinRespuesta() {
        List<AyudaSinRespuestaDTO> resultado = new ArrayList<>();
        for (Object[] fila : filas(getEntityManager().createNativeQuery("SELECT ayus_id, ayus_texto, ayus_pagina,"
                + " ayus_veces, ayus_ultima_fecha FROM tec.ayuda_sin_respuesta"
                + " ORDER BY ayus_veces DESC, ayus_ultima_fecha DESC"))) {
            resultado.add(new AyudaSinRespuestaDTO(((Number) fila[0]).intValue(), (String) fila[1], (String) fila[2],
                    ((Number) fila[3]).intValue(), (Date) fila[4]));
        }
        return resultado;
    }

    public void eliminarSinRespuesta(Integer id) {
        getEntityManager().createNativeQuery("DELETE FROM tec.ayuda_sin_respuesta WHERE ayus_id = :id")
                .setParameter("id", id).executeUpdate();
    }

    /** Todas las preguntas, activas e inactivas (los borradores se cargan inactivos). */
    public List<AyudaPregunta> listarTodas() {
        return sinFiltroActivos(em -> em.createQuery("SELECT DISTINCT p FROM AyudaPregunta p LEFT JOIN FETCH p.roles"
                + " ORDER BY p.orden, p.id", AyudaPregunta.class).getResultList());
    }

    public AyudaPregunta buscarPorId(Integer id) {
        return sinFiltroActivos(em -> em.createQuery("SELECT p FROM AyudaPregunta p LEFT JOIN FETCH p.roles"
                + " WHERE p.id = :id", AyudaPregunta.class).setParameter("id", id).getResultStream().findFirst()
                .orElse(null));
    }

    public List<AyudaContacto> contactosActivos() {
        return getEntityManager().createQuery("SELECT c FROM AyudaContacto c WHERE c.estado = TRUE"
                + " ORDER BY c.orden, c.id", AyudaContacto.class).getResultList();
    }

    public List<AyudaContacto> contactosTodos() {
        return sinFiltroActivos(em -> em.createQuery("SELECT c FROM AyudaContacto c ORDER BY c.orden, c.id",
                AyudaContacto.class).getResultList());
    }

    /** Roles SITEC activos: candidatos para filtrar preguntas por rol. */
    public List<Rol> rolesActivos() {
        return getEntityManager().createQuery("SELECT r FROM Rol r WHERE r.estado = TRUE AND r.nombre LIKE 'SITEC-%'"
                + " ORDER BY r.nombre", Rol.class).getResultList();
    }

    public List<Rol> rolesPorIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return getEntityManager().createQuery("SELECT r FROM Rol r WHERE r.id IN :ids", Rol.class)
                .setParameter("ids", ids).getResultList();
    }

    @SuppressWarnings("unchecked")
    private static List<Object[]> filas(Query query) {
        return query.getResultList();
    }

    /** La pantalla de mantenimiento necesita ver también lo inactivo. */
    private <T> T sinFiltroActivos(Function<EntityManager, T> consulta) {
        EntityManager em = super.getEntityManager();
        Session session = em.unwrap(Session.class);
        boolean filtroActivo = session.getEnabledFilter(EntidadBase.FILTER_ACTIVE) != null;
        if (filtroActivo) session.disableFilter(EntidadBase.FILTER_ACTIVE);
        try {
            return consulta.apply(em);
        } finally {
            if (filtroActivo) session.enableFilter(EntidadBase.FILTER_ACTIVE);
        }
    }
}
