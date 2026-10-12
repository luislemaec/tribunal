package ec.com.antenasur.facade.tec;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import ec.com.antenasur.model.tec.DispositivoMovil;
import ec.com.antenasur.model.tec.NotificacionMovil;

/**
 * Persistencia de dispositivos y notificaciones push de la App móvil (V12). Usa su propio
 * EntityManager, igual que SesionMovilFacade: la vigencia se comprueba en cada consulta.
 */
@Stateless
public class NotificacionMovilFacade {

    @PersistenceContext(unitName = "tribunalPU")
    private EntityManager em;

    // ---------------------------------------------------------------- Dispositivos

    public DispositivoMovil buscarPorToken(String token) {
        List<DispositivoMovil> r = em.createQuery("SELECT d FROM DispositivoMovil d WHERE d.token = :t",
                DispositivoMovil.class).setParameter("t", token).getResultList();
        return r.isEmpty() ? null : r.get(0);
    }

    public void guardar(DispositivoMovil dispositivo) {
        if (dispositivo.getId() == null) {
            em.persist(dispositivo);
        } else {
            em.merge(dispositivo);
        }
    }

    /** Tokens activos de los usuarios indicados, solo si el usuario sigue activo. */
    public List<DispositivoMovil> dispositivosDeUsuarios(Collection<Integer> usuarioIds) {
        if (usuarioIds.isEmpty()) {
            return List.of();
        }
        return em.createQuery("SELECT d FROM DispositivoMovil d, Usuario u WHERE u.id = d.usuarioId"
                + " AND d.activo = TRUE AND u.estado = TRUE AND d.usuarioId IN :ids", DispositivoMovil.class)
                .setParameter("ids", usuarioIds).getResultList();
    }

    /** Tokens activos de los usuarios activos con el rol indicado (rol y asignación vigentes). */
    public List<DispositivoMovil> dispositivosDeRol(String rol) {
        return em.createQuery("SELECT DISTINCT d FROM DispositivoMovil d, RolUsuario ru"
                + " WHERE ru.usuario.id = d.usuarioId AND d.activo = TRUE AND ru.estado = TRUE"
                + " AND ru.usuario.estado = TRUE AND ru.rol.estado = TRUE AND ru.rol.nombre = :rol",
                DispositivoMovil.class).setParameter("rol", rol).getResultList();
    }

    /** Usuarios activos vinculados a una persona (el Presidente designado de una mesa). */
    public List<Integer> usuariosDePersona(Integer personaId) {
        return em.createQuery("SELECT u.id FROM Usuario u WHERE u.personsa.id = :p AND u.estado = TRUE",
                Integer.class).setParameter("p", personaId).getResultList();
    }

    public void desactivar(Long dispositivoId) {
        em.createQuery("UPDATE DispositivoMovil d SET d.activo = FALSE, d.actualizadoEn = :ahora WHERE d.id = :id")
                .setParameter("ahora", Instant.now()).setParameter("id", dispositivoId).executeUpdate();
    }

    // ---------------------------------------------------------------- Bandeja de salida

    public void encolar(NotificacionMovil notificacion) {
        em.persist(notificacion);
    }

    public List<NotificacionMovil> pendientes(int maximo, int intentosMaximos) {
        return em.createQuery("SELECT n FROM NotificacionMovil n WHERE n.enviadaEn IS NULL"
                + " AND n.intentos < :max ORDER BY n.creadaEn", NotificacionMovil.class)
                .setParameter("max", intentosMaximos).setMaxResults(maximo).getResultList();
    }

    public void actualizar(NotificacionMovil notificacion) {
        em.merge(notificacion);
    }

    // ---------------------------------------------------------------- Eventos del cronograma

    /**
     * Registra un evento del cronograma; devuelve {@code false} si ya estaba registrado (otro
     * ciclo o un reinicio ya lo notificó). Atómico gracias a la clave primaria.
     */
    public boolean registrarEvento(String clave) {
        return em.createNativeQuery("INSERT INTO tec.notificacion_evento (clave, registrado_en) VALUES (?1, ?2)"
                + " ON CONFLICT (clave) DO NOTHING")
                .setParameter(1, clave).setParameter(2, java.time.OffsetDateTime.now()).executeUpdate() == 1;
    }
}
