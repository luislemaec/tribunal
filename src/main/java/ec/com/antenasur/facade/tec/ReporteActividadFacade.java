package ec.com.antenasur.facade.tec;

import java.time.Instant;
import java.util.List;

import jakarta.ejb.Stateless;

import ec.com.antenasur.model.generic.AbstractFacade;
import ec.com.antenasur.model.tec.SesionMovil;

/** Sesiones de la App móvil vigentes para la pestaña Actividad de Rep. Registros. */
@Stateless
public class ReporteActividadFacade extends AbstractFacade<SesionMovil, Long> {

    public ReporteActividadFacade() {
        super(SesionMovil.class, Long.class);
    }

    /**
     * Usuarios con una sesión de la App no revocada, no expirada y usada desde
     * {@code usadaDesde}: {usuario, inicio de la sesión más antigua}. Una consulta.
     */
    public List<Object[]> usuariosAppEnLinea(Instant usadaDesde) {
        return getEntityManager().createQuery("SELECT u.username, MIN(s.creadaEn) FROM SesionMovil s, Usuario u"
                + " WHERE u.id = s.usuarioId AND s.revocadaEn IS NULL AND s.expiraAbsoluta > :ahora"
                + " AND s.ultimoUso >= :desde GROUP BY u.username", Object[].class)
                .setParameter("ahora", Instant.now()).setParameter("desde", usadaDesde).getResultList();
    }
}
