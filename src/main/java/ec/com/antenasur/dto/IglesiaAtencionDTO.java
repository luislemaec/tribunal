package ec.com.antenasur.dto;

import java.io.Serializable;

import lombok.Getter;

/**
 * Iglesia con trabajo pendiente en la pestaña Iglesias: miembros por habilitar
 * o revisar, sin administrador o sin lista de miembros. Personas distintas.
 */
@Getter
public class IglesiaAtencionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Integer id;
    private final String nombre;
    private final String parroquia;
    private final String canton;
    private final long registrados;
    private final long habilitados;
    private final long pendientesRevision;
    private final boolean tieneAdministrador;
    private final boolean tieneListaMiembros;

    public IglesiaAtencionDTO(Integer id, String nombre, String parroquia, String canton, Long registrados,
            Long habilitados, Long pendientesRevision, Integer tieneAdministrador, Integer tieneListaMiembros) {
        this.id = id;
        this.nombre = nombre == null ? "" : nombre;
        this.parroquia = parroquia;
        this.canton = canton;
        this.registrados = registrados == null ? 0L : registrados;
        this.habilitados = habilitados == null ? 0L : habilitados;
        this.pendientesRevision = pendientesRevision == null ? 0L : pendientesRevision;
        this.tieneAdministrador = tieneAdministrador != null && tieneAdministrador > 0;
        this.tieneListaMiembros = tieneListaMiembros != null && tieneListaMiembros > 0;
    }

    public long getNoHabilitados() {
        return Math.max(0L, registrados - habilitados);
    }

    public boolean isRequiereAtencion() {
        return getNoHabilitados() > 0 || pendientesRevision > 0 || habilitados == 0 || !tieneAdministrador
                || !tieneListaMiembros;
    }
}
