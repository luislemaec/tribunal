package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Resumen de una habilitación masiva: cuántos miembros pendientes se procesaron,
 * cuántos quedaron revisados y habilitados, y los que no se modificaron con su motivo
 * (siguen pendientes y se corrigen uno por uno con la edición individual).
 */
@Getter
@AllArgsConstructor
public class ResultadoHabilitacionMasivaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String iglesia;
    /** Miembros que ya estaban revisados al iniciar: no se procesan ni se modifican. */
    private final int revisadosPrevios;
    private final int totalPendientes;
    private final int habilitados;
    private final List<Rechazo> rechazados;

    public int getConInconsistencias() {
        return rechazados.size();
    }

    /** Miembro no modificado; {@code motivo} es el mismo mensaje que da la edición individual. */
    @Getter
    @AllArgsConstructor
    public static class Rechazo implements Serializable {

        private static final long serialVersionUID = 1L;

        private final Integer iglesiaPersonaId;
        private final String nombres;
        private final String cedula;
        private final String motivo;
    }
}
