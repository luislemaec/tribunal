/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.facade.tec;

import ec.com.antenasur.model.generic.AbstractFacade;
import ec.com.antenasur.model.tec.Proceso;

import java.util.List;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.ejb.Stateless;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import ec.com.antenasur.audit.CatalogoActividades;
import ec.com.antenasur.dto.FiltroActividadAuditoriaDTO;
import ec.com.antenasur.dto.UsuarioAuditoriaDTO;
import ec.com.antenasur.model.generic.EntidadBase;
import org.hibernate.Session;

/**
 *
 * @author Usuario
 */
@Stateless
@DeclareRoles({"SITEC-Administrador", "SITEC-Tribunal",
    "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa"})
@RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal",
    "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa"})
public class ProcesoFacade extends AbstractFacade<Proceso, Integer> {

    /**
     * La anotación en la clase no siempre se aplica al método heredado por el
     * proxy EJB de WildFly. La operación normal de auditoría se declara de
     * forma explícita para que Elytron evalúe los roles SITEC reales.
     */
    @Override
    @RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal",
        "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa"})
    public Proceso create(Proceso entity) {
        return super.create(entity);
    }

    /**
     * Punto interno y acotado para fallos anteriores a request.login(). No
     * recibe actividad libre ni secretos, por lo que no expone un canal de
     * escritura general para usuarios anónimos.
     */
    @PermitAll
    public void registrarLoginFallidoPreautenticacion(String usuarioIntentado, String ip) {
        super.getEntityManager().createNativeQuery("INSERT INTO tec.procesos "
                + "(actividad, ip, estado, f_crea, u_crea) "
                + "VALUES (:actividad, :ip, TRUE, CURRENT_TIMESTAMP, :usuario)")
                .setParameter("actividad", "LOGIN | MÓDULO: ACCESO; RESULTADO: FALLIDO; DETALLE: Credenciales rechazadas")
                .setParameter("ip", ip)
                .setParameter("usuario", usuarioIntentado)
                .executeUpdate();
    }

    /**
     * Registra una solicitud pública de recuperación con valores funcionales
     * fijos. No admite contenido, acción o resultado controlados por cliente.
     */
    @PermitAll
    public void registrarSolicitudRecuperacionClavePreautenticacion(String usuario, String ip) {
        super.getEntityManager().createNativeQuery("INSERT INTO tec.procesos "
                + "(actividad, ip, estado, f_crea, u_crea) "
                + "VALUES (:actividad, :ip, TRUE, CURRENT_TIMESTAMP, :usuario)")
                .setParameter("actividad", "RECUPERACION_CLAVE | MÓDULO: ACCESO; RESULTADO: SOLICITADO; DETALLE: Solicitud de recuperación de clave")
                .setParameter("ip", ip)
                .setParameter("usuario", usuario)
                .executeUpdate();
    }

    public ProcesoFacade() {
        super(Proceso.class, Integer.class);
    }

    public List<Proceso> getProcesoPorUsuario(String usuario) {
        try {
            String hql = "SELECT p FROM Proceso p WHERE p.usuarioCrea=:usuario AND p.estado=TRUE";
            Query query = super.getEntityManager().createQuery(hql);
            query.setParameter("usuario", usuario);
            List<Proceso> resultList = query.getResultList();
            if (resultList != null && !resultList.isEmpty()) {
                return resultList;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public List<Proceso> getProcesoPorUsuario(Date fechaInicio, Date fechaFin, String usuario) {
        try {
            String hql = "SELECT p FROM Proceso p "
                    + " WHERE cast(p.fechaCrea as date) BETWEEN :fechaInicio AND :fechaFin "
                    + " AND p.usuarioCrea=:usuario "
                    + "ORDER BY p.fechaCrea DESC ";
            Query query = super.getEntityManager().createQuery(hql);
            query.setParameter("fechaInicio", fechaInicio);
            query.setParameter("fechaFin", fechaFin);
            query.setParameter("usuario", usuario);
            List<Proceso> resultList = query.getResultList();
            if (resultList != null && !resultList.isEmpty()) {
                return resultList;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    /** Rol cuyas actividades no ve Tribunal. */
    private static final String ROL_ADMINISTRADOR = "SITEC-Administrador";

    /**
     * Excluye las actividades de usuarios con el rol Administrador vigente (asignación y
     * rol activos). {@code u_crea} es texto: se une por {@code usu_nombre}, lo que cubre
     * también los intentos de login hechos con el nombre de un Administrador.
     */
    private static final String SIN_ADMINISTRADORES = " AND NOT EXISTS (SELECT ru.id FROM RolUsuario ru"
            + " WHERE ru.usuario.username = p.usuarioCrea AND ru.estado = TRUE AND ru.rol.estado = TRUE"
            + " AND ru.rol.nombre = :rolAdministrador)";

    public int contarAuditoria(FiltroActividadAuditoriaDTO filtro, String usuarioAlcance,
            boolean excluirAdministradores) {
        return conAlcance(excluirAdministradores, em -> ((Long) crearConsultaAuditoria(em, "SELECT COUNT(p)",
                filtro, usuarioAlcance, excluirAdministradores).getSingleResult()).intValue());
    }

    @SuppressWarnings("unchecked")
    public List<Proceso> buscarAuditoria(FiltroActividadAuditoriaDTO filtro, String usuarioAlcance,
            boolean excluirAdministradores, int first, int pageSize) {
        return conAlcance(excluirAdministradores, em -> {
            TypedQuery<Proceso> query = (TypedQuery<Proceso>) crearConsultaAuditoria(em, "SELECT p", filtro,
                    usuarioAlcance, excluirAdministradores);
            query.setFirstResult(Math.max(first, 0));
            query.setMaxResults(Math.max(pageSize, 1));
            return query.getResultList();
        });
    }

    /**
     * Ejecuta la consulta en un solo EntityManager. Al excluir Administradores se desactiva
     * el filtro de activos para que un Administrador desactivado también se excluya (la
     * bitácora es historia); el resto de condiciones de estado son explícitas.
     */
    private <T> T conAlcance(boolean excluirAdministradores, java.util.function.Function<EntityManager, T> consulta) {
        EntityManager em = super.getEntityManager();
        if (!excluirAdministradores) return consulta.apply(em);
        Session session = em.unwrap(Session.class);
        boolean filtroActivo = session.getEnabledFilter(EntidadBase.FILTER_ACTIVE) != null;
        if (filtroActivo) session.disableFilter(EntidadBase.FILTER_ACTIVE);
        try {
            return consulta.apply(em);
        } finally {
            if (filtroActivo) session.enableFilter(EntidadBase.FILTER_ACTIVE);
        }
    }

    public List<String> listarUsuariosConActividad(boolean excluirAdministradores) {
        return conAlcance(excluirAdministradores, em -> {
            TypedQuery<String> query = em.createQuery(
                    "SELECT DISTINCT p.usuarioCrea FROM Proceso p WHERE p.estado = true "
                    + "AND p.usuarioCrea IS NOT NULL" + (excluirAdministradores ? SIN_ADMINISTRADORES : "")
                    + " ORDER BY p.usuarioCrea", String.class);
            if (excluirAdministradores) query.setParameter("rolAdministrador", ROL_ADMINISTRADOR);
            return query.getResultList();
        });
    }

    /** Usuarios por consulta: muy por debajo del límite de parámetros de PostgreSQL. */
    private static final int LOTE_IDENTIDADES = 1000;

    /**
     * Identidad (nombre completo y roles activos) de los nombres de acceso indicados, en
     * bloques de {@value #LOTE_IDENTIDADES}: una consulta por bloque, nunca una por fila.
     *
     * <p>{@code procesos.u_crea} es texto, sin relación JPA con Usuario: se une por
     * {@code usu_nombre}. Se desactiva el filtro de activos para resolver también usuarios
     * desactivados (la bitácora es historia); los roles se limitan a los activos. Si un
     * nombre de acceso se repite entre usuarios, prevalece el activo y, entre iguales, el
     * de menor id. Los nombres que no corresponden a ningún usuario (intentos de login,
     * {@code <desconocido>}) no aparecen en el resultado.
     */
    public Map<String, UsuarioAuditoriaDTO> buscarIdentidades(Collection<String> usuarios) {
        Map<String, UsuarioAuditoriaDTO> resultado = new LinkedHashMap<>();
        List<String> pendientes = new ArrayList<>();
        if (usuarios != null) {
            for (String usuario : usuarios) {
                if (usuario != null && !usuario.isBlank() && !pendientes.contains(usuario)) pendientes.add(usuario);
            }
        }
        if (pendientes.isEmpty()) return resultado;

        // Se reutiliza el mismo EntityManager: getEntityManager() vuelve a activar el filtro.
        EntityManager em = super.getEntityManager();
        Session session = em.unwrap(Session.class);
        boolean filtroActivo = session.getEnabledFilter(EntidadBase.FILTER_ACTIVE) != null;
        if (filtroActivo) session.disableFilter(EntidadBase.FILTER_ACTIVE);
        try {
            for (int desde = 0; desde < pendientes.size(); desde += LOTE_IDENTIDADES) {
                List<String> lote = pendientes.subList(desde, Math.min(desde + LOTE_IDENTIDADES, pendientes.size()));
                List<Object[]> filas = em.createQuery(
                        "SELECT u.id, u.username, per.apellidos, per.nombres, r.nombre FROM Usuario u"
                        + " LEFT JOIN u.personsa per"
                        + " LEFT JOIN u.rolUsuarios ru ON ru.estado = TRUE"
                        + " LEFT JOIN ru.rol r"
                        + " WHERE u.username IN :usuarios"
                        + " ORDER BY u.username, CASE WHEN u.estado = TRUE THEN 0 ELSE 1 END, u.id, r.nombre",
                        Object[].class).setParameter("usuarios", lote).getResultList();
                Map<String, Integer> usuarioElegido = new HashMap<>();
                for (Object[] fila : filas) {
                    Integer id = (Integer) fila[0];
                    String usuario = (String) fila[1];
                    // El orden pone primero al usuario que prevalece; las filas de otros ids se ignoran.
                    if (!id.equals(usuarioElegido.computeIfAbsent(usuario, u -> id))) continue;
                    UsuarioAuditoriaDTO identidad = resultado.computeIfAbsent(usuario,
                            u -> new UsuarioAuditoriaDTO(u, nombreCompleto((String) fila[2], (String) fila[3])));
                    identidad.agregarRol((String) fila[4]);
                }
            }
        } finally {
            if (filtroActivo) session.enableFilter(EntidadBase.FILTER_ACTIVE);
        }
        return resultado;
    }

    /**
     * Fecha del último inicio de sesión exitoso de cada usuario indicado, con la misma
     * clasificación que el filtro Acción ("Inició sesión", formatos vigente y antiguo).
     * Una consulta agregada por bloque de {@value #LOTE_IDENTIDADES} usuarios; los que
     * nunca iniciaron sesión no aparecen.
     */
    public Map<String, java.util.Date> buscarUltimoInicioSesion(Collection<String> usuarios) {
        Map<String, java.util.Date> resultado = new HashMap<>();
        List<String> pendientes = usuarios == null ? List.of()
                : usuarios.stream().filter(u -> u != null && !u.isBlank()).distinct().toList();
        for (int desde = 0; desde < pendientes.size(); desde += LOTE_IDENTIDADES) {
            List<ParametroAuditoria> parametros = new ArrayList<>();
            String login = condicion(CatalogoActividades.criterioPorAccion("Inició sesión"), "log", parametros);
            Query query = super.getEntityManager().createQuery("SELECT p.usuarioCrea, MAX(p.fechaCrea) FROM Proceso p"
                    + " WHERE p.estado = true AND p.usuarioCrea IN :usuarios AND " + login
                    + " GROUP BY p.usuarioCrea");
            query.setParameter("usuarios", pendientes.subList(desde, Math.min(desde + LOTE_IDENTIDADES, pendientes.size())));
            for (ParametroAuditoria parametro : parametros) query.setParameter(parametro.nombre(), parametro.valor());
            for (Object fila : query.getResultList()) {
                Object[] columnas = (Object[]) fila;
                if (columnas[1] != null) resultado.put((String) columnas[0], (java.util.Date) columnas[1]);
            }
        }
        return resultado;
    }

    /**
     * Transacciones por día desde {@code desde} (inclusive): {día, total}. Una consulta
     * agregada; con {@code excluirAdministradores} aplica el mismo alcance que Tribunal en
     * Actividades.
     */
    public List<Object[]> contarPorDia(LocalDate desde, boolean excluirAdministradores) {
        return conAlcance(excluirAdministradores, em -> {
            Query query = em.createQuery("SELECT CAST(p.fechaCrea AS LocalDate), COUNT(p) FROM Proceso p"
                    + " WHERE p.estado = true AND p.fechaCrea >= :desde"
                    + (excluirAdministradores ? SIN_ADMINISTRADORES : "")
                    + " GROUP BY CAST(p.fechaCrea AS LocalDate) ORDER BY CAST(p.fechaCrea AS LocalDate)");
            query.setParameter("desde", Timestamp.valueOf(desde.atStartOfDay()));
            if (excluirAdministradores) query.setParameter("rolAdministrador", ROL_ADMINISTRADOR);
            @SuppressWarnings("unchecked")
            List<Object[]> filas = query.getResultList();
            return filas;
        });
    }

    /**
     * Transacciones por módulo desde {@code desde}, en una sola consulta: una suma
     * condicional por módulo con la misma clasificación que el filtro Módulo
     * (CatalogoActividades). Devuelve los totales en el orden de {@code modulos}.
     */
    public long[] contarPorModulo(LocalDate desde, List<String> modulos, boolean excluirAdministradores) {
        return conAlcance(excluirAdministradores, em -> {
            List<ParametroAuditoria> parametros = new ArrayList<>();
            StringBuilder hql = new StringBuilder("SELECT ");
            for (int i = 0; i < modulos.size(); i++) {
                if (i > 0) hql.append(", ");
                hql.append("SUM(CASE WHEN ").append(condicion(CatalogoActividades.criterioPorModulo(modulos.get(i)),
                        "m" + i + "_", parametros)).append(" THEN 1 ELSE 0 END)");
            }
            hql.append(" FROM Proceso p WHERE p.estado = true AND p.fechaCrea >= :desde");
            if (excluirAdministradores) {
                hql.append(SIN_ADMINISTRADORES);
                parametros.add(new ParametroAuditoria("rolAdministrador", ROL_ADMINISTRADOR));
            }
            parametros.add(new ParametroAuditoria("desde", Timestamp.valueOf(desde.atStartOfDay())));
            Query query = em.createQuery(hql.toString());
            for (ParametroAuditoria parametro : parametros) query.setParameter(parametro.nombre(), parametro.valor());
            Object resultado = query.getSingleResult();
            Object[] fila = modulos.size() == 1 ? new Object[] { resultado } : (Object[]) resultado;
            long[] totales = new long[modulos.size()];
            for (int i = 0; i < totales.length; i++) totales[i] = fila[i] == null ? 0L : ((Number) fila[i]).longValue();
            return totales;
        });
    }

    /** Apellidos y nombres, igual que el listado de personas; null si la persona no tiene ninguno. */
    private static String nombreCompleto(String apellidos, String nombres) {
        String completo = ((apellidos == null ? "" : apellidos.trim()) + " " + (nombres == null ? "" : nombres.trim())).trim();
        return completo.isEmpty() ? null : completo;
    }

    private Query crearConsultaAuditoria(EntityManager em, String seleccion, FiltroActividadAuditoriaDTO filtro,
            String usuarioAlcance, boolean excluirAdministradores) {
        FiltroActividadAuditoriaDTO criterio = filtro != null ? filtro : new FiltroActividadAuditoriaDTO();
        StringBuilder hql = new StringBuilder(seleccion).append(" FROM Proceso p WHERE p.estado = true");
        List<ParametroAuditoria> parametros = new ArrayList<>();

        if (usuarioAlcance != null) {
            hql.append(" AND p.usuarioCrea = :usuarioAlcance");
            parametros.add(new ParametroAuditoria("usuarioAlcance", usuarioAlcance));
        } else if (tieneTexto(criterio.getUsuario())) {
            hql.append(" AND p.usuarioCrea = :usuario");
            parametros.add(new ParametroAuditoria("usuario", criterio.getUsuario().trim()));
        }
        // Se aplica también al filtro Usuario: elegir a un Administrador no devuelve filas.
        if (excluirAdministradores) {
            hql.append(SIN_ADMINISTRADORES);
            parametros.add(new ParametroAuditoria("rolAdministrador", ROL_ADMINISTRADOR));
        }
        if (criterio.getFechaInicio() != null) {
            hql.append(" AND p.fechaCrea >= :fechaInicio");
            parametros.add(new ParametroAuditoria("fechaInicio", Timestamp.valueOf(criterio.getFechaInicio().atStartOfDay())));
        }
        if (criterio.getFechaFin() != null) {
            hql.append(" AND p.fechaCrea < :fechaFin");
            LocalDate siguienteDia = criterio.getFechaFin().plusDays(1);
            parametros.add(new ParametroAuditoria("fechaFin", Timestamp.valueOf(siguienteDia.atStartOfDay())));
        }
        // Acción, módulo y resultado llegan con la descripción mostrada al usuario;
        // el catálogo los traduce a los patrones del texto técnico almacenado.
        if (tieneTexto(criterio.getAccion())) {
            hql.append(" AND ").append(condicion(CatalogoActividades.criterioPorAccion(criterio.getAccion().trim()),
                    "acc", parametros));
        }
        if (tieneTexto(criterio.getModulo())) {
            hql.append(" AND ").append(condicion(CatalogoActividades.criterioPorModulo(criterio.getModulo().trim()),
                    "mod", parametros));
        }
        if (tieneTexto(criterio.getResultado())) {
            hql.append(" AND ").append(condicion(
                    CatalogoActividades.criterioPorResultado(criterio.getResultado().trim()), "res", parametros));
        }
        if (tieneTexto(criterio.getBusqueda())) {
            String texto = criterio.getBusqueda().trim();
            hql.append(" AND (LOWER(p.actividad) LIKE :busqueda OR LOWER(p.usuarioCrea) LIKE :busqueda OR p.ip LIKE :busqueda");
            CatalogoActividades.CriterioFiltro porAccion = CatalogoActividades.criterioPorTextoDeAccion(texto);
            if (!porAccion.vacio()) hql.append(" OR ").append(condicion(porAccion, "bus", parametros));
            hql.append(")");
            parametros.add(new ParametroAuditoria("busqueda", "%" + texto.toLowerCase() + "%"));
        }
        if (!seleccion.startsWith("SELECT COUNT")) hql.append(" ORDER BY p.fechaCrea DESC, p.id DESC");

        Query query = em.createQuery(hql.toString());
        for (ParametroAuditoria parametro : parametros) query.setParameter(parametro.nombre(), parametro.valor());
        return query;
    }

    /**
     * Traduce un {@link CatalogoActividades.CriterioFiltro} a JPQL con
     * parámetros. Una selección desconocida no devuelve filas.
     */
    private String condicion(CatalogoActividades.CriterioFiltro criterio, String prefijo,
            List<ParametroAuditoria> parametros) {
        if (criterio.vacio()) return "(1 = 0)";
        List<String> alternativas = new ArrayList<>();
        if (!criterio.especificos().isEmpty()) {
            alternativas.add(algunLike(criterio.especificos(), prefijo + "e", parametros));
        }
        if (!criterio.auxiliares().isEmpty()) {
            alternativas.add("(" + algunLike(criterio.auxiliares(), prefijo + "a", parametros)
                    + " AND NOT " + algunLike(criterio.todosEspecificos(), prefijo + "x", parametros) + ")");
        }
        if (criterio.sinClasificar()) {
            alternativas.add("(p.actividad IS NULL OR NOT "
                    + algunLike(criterio.todos(), prefijo + "t", parametros) + ")");
        }
        return "(" + String.join(" OR ", alternativas) + ")";
    }

    private String algunLike(List<String> patrones, String prefijo, List<ParametroAuditoria> parametros) {
        List<String> partes = new ArrayList<>();
        for (int i = 0; i < patrones.size(); i++) {
            String nombre = prefijo + i;
            // TRIM y LOWER replican CatalogoActividades.comoTrimSql + toLowerCase.
            partes.add("LOWER(TRIM(p.actividad)) LIKE :" + nombre + " ESCAPE '" + CatalogoActividades.ESCAPE + "'");
            parametros.add(new ParametroAuditoria(nombre, patrones.get(i)));
        }
        return "(" + String.join(" OR ", partes) + ")";
    }

    private boolean tieneTexto(String texto) {
        return texto != null && !texto.isBlank();
    }

    private record ParametroAuditoria(String nombre, Object valor) {
    }
}
