package ec.com.antenasur.facade.tec;

import java.util.List;

import jakarta.ejb.Stateless;

import ec.com.antenasur.dto.AdministradorIglesiaDTO;
import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.model.generic.AbstractFacade;

/**
 * Pestaña Administradores de Rep. Registros. Dos consultas para todas las iglesias
 * activas, ninguna por iglesia: el avance de miembros por iglesia (filas agregadas) y
 * sus administradores.
 */
@Stateless
public class ReporteAdministradoresFacade extends AbstractFacade<Iglesia, Integer> {

    /** Misma regla de administrador que IglesiaFacade (filtro conAdmin) y ReporteIglesiasFacade. */
    private static final String ROL_IGLESIA_ADMIN = "%IglesiaAdmin";

    public ReporteAdministradoresFacade() {
        super(Iglesia.class, Integer.class);
    }

    /**
     * Una fila por iglesia activa, también sin miembros. Habilitado y revisión pendiente
     * con las mismas reglas que ReporteRegistrosFacade; personas distintas.
     */
    public List<AdministradorIglesiaDTO> listarIglesias() {
        return getEntityManager().createQuery("SELECT new ec.com.antenasur.dto.AdministradorIglesiaDTO("
                + "i.id, i.nombre, pa.id, pa.name, ca.id, ca.name, pv.name,"
                + " COUNT(DISTINCT pr.id),"
                + " COUNT(DISTINCT CASE WHEN ip.habilitadoPadron = TRUE THEN pr.id ELSE NULL END),"
                + " COUNT(DISTINCT CASE WHEN ip.fechaActualiza IS NULL"
                + " OR (ip.fechaCrea IS NOT NULL AND ip.fechaActualiza < ip.fechaCrea) THEN pr.id ELSE NULL END))"
                + " FROM Iglesia i JOIN i.ubicacion pa JOIN pa.geograp ca LEFT JOIN ca.geograp pv"
                + " LEFT JOIN IglesiaPersona ip ON ip.iglesia = i AND ip.estado = TRUE"
                + " LEFT JOIN Persona pr ON pr = ip.persona AND pr.estado = TRUE"
                + " WHERE i.estado = TRUE"
                + " GROUP BY i.id, i.nombre, pa.id, pa.name, ca.id, ca.name, pv.name", AdministradorIglesiaDTO.class)
                .getResultList();
    }

    /**
     * Administradores vigentes de iglesias activas: {iglesiaId, nombre, usuario, correo,
     * permanente}, ordenados por usuario (el de menor id primero si una iglesia tuviera
     * más de uno).
     */
    public List<Object[]> listarAdministradores() {
        return getEntityManager().createQuery("SELECT DISTINCT u.iglesia.id, per.nombres, u.username, u.correo,"
                + " u.permanente, u.id FROM Usuario u LEFT JOIN u.personsa per JOIN u.rolUsuarios ru JOIN ru.rol r"
                + " WHERE u.iglesia.estado = TRUE AND u.estado = TRUE AND ru.estado = TRUE AND r.estado = TRUE"
                + " AND r.nombre LIKE :rolIglesiaAdmin ORDER BY u.id", Object[].class)
                .setParameter("rolIglesiaAdmin", ROL_IGLESIA_ADMIN).getResultList();
    }
}
