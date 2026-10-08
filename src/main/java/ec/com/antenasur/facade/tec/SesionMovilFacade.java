package ec.com.antenasur.facade.tec;

import java.time.Instant;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import ec.com.antenasur.model.Usuario;
import ec.com.antenasur.model.tec.SesionMovil;

/**
 * Persistencia de las sesiones móviles. Usa su propio EntityManager (sin el filtro de
 * activos de AbstractFacade), igual que AccesoQrActaFacade: la vigencia de la sesión y del
 * usuario se comprueba de forma explícita en cada consulta.
 */
@Stateless
public class SesionMovilFacade {

    @PersistenceContext(unitName = "tribunalPU")
    private EntityManager em;

    public void registrar(SesionMovil sesion) {
        em.persist(sesion);
        em.flush();
    }

    public SesionMovil buscarPorAcceso(String accesoHash) {
        return primera(em.createQuery("SELECT s FROM SesionMovil s WHERE s.accesoHash = :hash", SesionMovil.class)
                .setParameter("hash", accesoHash).getResultList());
    }

    /** Bloquea la fila para que dos renovaciones simultáneas no roten el mismo refresh. */
    public SesionMovil bloquearPorRefresh(String refreshHash) {
        return primera(em.createQuery("SELECT s FROM SesionMovil s WHERE s.refreshHash = :hash", SesionMovil.class)
                .setParameter("hash", refreshHash).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList());
    }

    public SesionMovil bloquearPorRefreshAnterior(String refreshHash) {
        return primera(em.createQuery("SELECT s FROM SesionMovil s WHERE s.refreshAnteriorHash = :hash",
                SesionMovil.class).setParameter("hash", refreshHash).setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList());
    }

    public SesionMovil buscar(Long id) {
        return id == null ? null : em.find(SesionMovil.class, id);
    }

    /** Usuario de la sesión, incluido si fue desactivado (la vigencia se decide en el servicio). */
    public Usuario usuario(Integer usuarioId) {
        return usuarioId == null ? null : em.find(Usuario.class, usuarioId);
    }

    public void actualizar(SesionMovil sesion) {
        em.merge(sesion);
    }

    /** Revoca todas las sesiones vigentes del usuario (cambio o restablecimiento de clave). */
    public int revocarPorUsuario(Integer usuarioId, Instant ahora, String motivo) {
        if (usuarioId == null) {
            return 0;
        }
        return em.createQuery("UPDATE SesionMovil s SET s.revocadaEn = :ahora, s.motivoRevocacion = :motivo"
                + " WHERE s.usuarioId = :usuario AND s.revocadaEn IS NULL")
                .setParameter("ahora", ahora).setParameter("motivo", motivo).setParameter("usuario", usuarioId)
                .executeUpdate();
    }

    private static SesionMovil primera(List<SesionMovil> filas) {
        return filas.isEmpty() ? null : filas.get(0);
    }
}
