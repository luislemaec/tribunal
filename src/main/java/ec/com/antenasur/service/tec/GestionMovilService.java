package ec.com.antenasur.service.tec;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.ConsultaMovilDTO.Miembro;
import ec.com.antenasur.dto.IglesiaPersonaDTO;
import ec.com.antenasur.dto.PersonaDTO;
import ec.com.antenasur.service.IglesiaPersonaService;

/**
 * Operaciones de la App móvil que modifican datos (docs/api-movil.md). Separadas de las
 * consultas para que cada escritura declare sus roles de forma explícita; las reglas de
 * negocio viven en los servicios de la web, que se reutilizan sin duplicarlas.
 */
@Stateless
@DeclareRoles({ GestionMovilService.IGLESIA_ADMIN })
public class GestionMovilService {

    static final String IGLESIA_ADMIN = "SITEC-IglesiaAdmin";

    @Inject
    private IglesiaPersonaService iglesiaPersonaService;

    /**
     * El IglesiaAdmin habilita o deshabilita a un miembro de su iglesia para participar en
     * las elecciones. IglesiaPersonaService valida el cronograma y el alcance, y marca al
     * miembro como revisado (igual que la web).
     */
    @RolesAllowed(IGLESIA_ADMIN)
    public Miembro cambiarHabilitacionMiembro(Integer miembroId, boolean habilitado) {
        IglesiaPersonaDTO ip = iglesiaPersonaService.cambiarHabilitacion(miembroId, habilitado);
        PersonaDTO persona = ip.getPersona();
        String nombre = persona == null ? null
                : ((persona.getApellidos() == null ? "" : persona.getApellidos().trim()) + " "
                        + (persona.getNombres() == null ? "" : persona.getNombres().trim())).trim();
        return new Miembro(ip.getId(), nombre, Boolean.TRUE.equals(ip.getHabilitadoPadron()), true);
    }
}
