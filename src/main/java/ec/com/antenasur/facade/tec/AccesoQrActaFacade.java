package ec.com.antenasur.facade.tec;

import java.time.Instant;
import java.util.List;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import ec.com.antenasur.model.Usuario;
import ec.com.antenasur.model.tec.AccesoQrActa;
import ec.com.antenasur.model.tec.Documentos;
import ec.com.antenasur.model.tec.MiembroJRV;
import ec.com.antenasur.model.tec.SesionQrActa;
import ec.com.antenasur.security.qr.EstadoAccesoQr;

@Stateless
public class AccesoQrActaFacade {
    @PersistenceContext(unitName = "tribunalPU") private EntityManager em;

    public AccesoQrActa bloquearPorHash(String hash) {
        var filas = em.createQuery("SELECT q FROM AccesoQrActa q WHERE q.tokenHash = :hash", AccesoQrActa.class)
                .setParameter("hash", hash).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        return filas.isEmpty() ? null : filas.get(0);
    }

    public AccesoQrActa buscar(Long id) { return em.find(AccesoQrActa.class, id); }

    public Usuario usuario(Integer id) { return em.find(Usuario.class, id); }

    public void invalidarPuentesUsuario(Integer usuarioId) {
        em.find(Usuario.class, usuarioId, LockModeType.PESSIMISTIC_WRITE);
        em.createQuery("UPDATE SesionQrActa s SET s.loginHash = NULL WHERE s.qrId IN "
                + "(SELECT q.id FROM AccesoQrActa q WHERE q.usuarioId = :usuario)")
                .setParameter("usuario", usuarioId).executeUpdate();
    }

    public void confirmarSesion(SesionQrActa sesion) {
        sesion.setLoginHash(null);
        sesion.setLoginConfirmado(true);
        em.flush();
    }

    public void revocarSesion(SesionQrActa sesion) {
        sesion.setLoginHash(null);
        sesion.setRevocadaEn(Instant.now());
        em.flush();
    }

    public void registrarSesion(SesionQrActa sesion) { em.persist(sesion); em.flush(); }

    public SesionQrActa buscarSesion(String pruebaHash) {
        var filas = em.createQuery("SELECT s FROM SesionQrActa s WHERE s.pruebaHash = :hash", SesionQrActa.class)
                .setParameter("hash", pruebaHash).getResultList();
        return filas.isEmpty() ? null : filas.get(0);
    }

    public void registrar(AccesoQrActa acceso) { em.persist(acceso); em.flush(); }

    public void consumir(AccesoQrActa acceso, Instant ahora) {
        acceso.setEstado(EstadoAccesoQr.CANJEADO);
        acceso.setCanjeadoEn(ahora);
        em.flush();
    }

    public void revocarContexto(Integer proceso, Integer mesa, Integer tipo, Instant ahora, String motivo) {
        // El generador debe mantener el bloqueo de Mesa de DocumentoFacade durante toda la transaccion.
        em.createQuery("UPDATE AccesoQrActa q SET q.estado = :revocado, q.revocadoEn = :ahora, "
                + "q.motivoRevocacion = :motivo, q.revision = q.revision + 1 "
                + "WHERE q.procesoId = :proceso AND q.mesaId = :mesa AND q.tipoDocumentoId = :tipo "
                + "AND q.estado <> :revocado")
                .setParameter("revocado", EstadoAccesoQr.REVOCADO).setParameter("ahora", ahora)
                .setParameter("motivo", motivo).setParameter("proceso", proceso)
                .setParameter("mesa", mesa).setParameter("tipo", tipo).executeUpdate();
        em.createQuery("UPDATE SesionQrActa s SET s.revocadaEn = :ahora WHERE s.revocadaEn IS NULL "
                + "AND s.qrId IN (SELECT q.id FROM AccesoQrActa q WHERE q.procesoId = :proceso "
                + "AND q.mesaId = :mesa AND q.tipoDocumentoId = :tipo AND q.estado = :revocado)")
                .setParameter("ahora", ahora).setParameter("proceso", proceso).setParameter("mesa", mesa)
                .setParameter("tipo", tipo).setParameter("revocado", EstadoAccesoQr.REVOCADO).executeUpdate();
    }

    public Documentos documento(Integer id) {
        var filas = em.createQuery("SELECT d FROM Documentos d JOIN FETCH d.proceso "
                + "JOIN FETCH d.mesa m JOIN FETCH m.recinto JOIN FETCH d.recinto "
                + "JOIN FETCH d.tipoDocumento WHERE d.id = :id", Documentos.class)
                .setParameter("id", id).getResultList();
        return filas.isEmpty() ? null : filas.get(0);
    }

    public boolean esUnicoDocumentoActivo(AccesoQrActa qr) {
        var ids = em.createQuery("SELECT d.id FROM Documentos d WHERE d.estado = TRUE "
                + "AND d.proceso.id = :proceso AND d.mesa.id = :mesa AND d.tipoDocumento.id = :tipo",
                Integer.class).setParameter("proceso", qr.getProcesoId()).setParameter("mesa", qr.getMesaId())
                .setParameter("tipo", qr.getTipoDocumentoId()).setMaxResults(2).getResultList();
        return ids.size() == 1 && ids.get(0).equals(qr.getDocumentoId());
    }

    public List<MiembroJRV> presidentes(Integer mesa, Integer proceso) {
        return em.createQuery("SELECT j FROM MiembroJRV j JOIN FETCH j.iglesiaPersona ip "
                + "JOIN FETCH ip.persona JOIN FETCH j.cargo c WHERE j.mesa.id = :mesa "
                + "AND j.proceso.id = :proceso AND j.estado = TRUE AND c.estado = TRUE "
                + "AND c.padre.nombre = 'CARGO AUTORIDADES MESA' "
                + "AND UPPER(TRIM(c.nombre)) IN ('PRESIDENTE', 'PRESIDENTE DE MESA')", MiembroJRV.class)
                .setParameter("mesa", mesa).setParameter("proceso", proceso).setMaxResults(2).getResultList();
    }

    public List<Usuario> usuariosPresidente(Integer persona) {
        return em.createQuery("SELECT DISTINCT u FROM Usuario u JOIN u.rolUsuarios ru JOIN ru.rol r "
                + "WHERE u.personsa.id = :persona AND u.estado = TRUE AND ru.estado = TRUE "
                + "AND r.estado = TRUE AND r.nombre = :rol", Usuario.class)
                .setParameter("persona", persona).setParameter("rol", "SITEC-Presidente-mesa")
                .setMaxResults(2).getResultList();
    }

    public boolean usernameUnico(String username) {
        return em.createQuery("SELECT COUNT(u.id) FROM Usuario u WHERE u.username = :nombre", Long.class)
                .setParameter("nombre", username).getSingleResult() == 1L;
    }

    public void auditar(AccesoQrActa qr, String ip, String userAgent, String resultado) {
        em.createNativeQuery("INSERT INTO tec.auditoria_acceso_qr "
                + "(qr_id, proce_id, mesa_id, usu_id, ip, user_agent, resultado) "
                + "VALUES (:qr, :proceso, :mesa, :usuario, :ip, :agente, :resultado)")
                .setParameter("qr", qr == null ? null : qr.getId())
                .setParameter("proceso", qr == null ? null : qr.getProcesoId())
                .setParameter("mesa", qr == null ? null : qr.getMesaId())
                .setParameter("usuario", qr == null ? null : qr.getUsuarioId())
                .setParameter("ip", limitar(ip, 64)).setParameter("agente", limitar(userAgent, 512))
                .setParameter("resultado", limitar(resultado, 64)).executeUpdate();
    }

    private static String limitar(String texto, int longitud) {
        if (texto == null) return "";
        String limpio = texto.replaceAll("[\\p{Cntrl}]", " ")
                .replaceAll("[A-Za-z0-9_-]{43,}", "[redacted]");
        return limpio.substring(0, Math.min(limpio.length(), longitud));
    }
}
