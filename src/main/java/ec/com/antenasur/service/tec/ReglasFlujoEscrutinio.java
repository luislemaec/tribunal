package ec.com.antenasur.service.tec;

import ec.com.antenasur.enums.EstadoEscrutinio;

/**
 * Reglas del orden del escrutinio: APERTURA → CONTEO → ACTA FÍSICA → CIERRE → VALIDACIÓN.
 * Única fuente para el servicio (que las hace cumplir), actaE, la API móvil y la
 * proyección de fases ({@link ec.com.antenasur.dto.ProgresoEscrutinioDTO}). No define
 * estados nuevos: combina el estado de la cabecera ({@link EstadoEscrutinio}) con la
 * revisión del acta física vigente ({@code tec.documentos.doc_estado_revision}).
 */
public final class ReglasFlujoEscrutinio {

    // Mismos valores que ActaFisicaEscrutinioService; sin depender del EJB para poder
    // verificar la proyección sin contenedor (docs/progreso-escrutinio.md).
    private static final String PENDIENTE_REVISION = "PENDIENTE_REVISION";
    private static final String VALIDADA = "VALIDADA";

    private ReglasFlujoEscrutinio() {
    }

    /**
     * El conteo ya se guardó al menos una vez y la mesa sigue abierta: mismos estados en
     * los que se permitía cerrar (EN_CONTEO, CONTEO_REGISTRADO, REABIERTO).
     */
    public static boolean conteoGuardado(EstadoEscrutinio estado) {
        return estado == EstadoEscrutinio.EN_CONTEO || estado == EstadoEscrutinio.CONTEO_REGISTRADO
                || estado == EstadoEscrutinio.REABIERTO;
    }

    /**
     * Se puede cargar (o reemplazar) el acta física: con el conteo guardado o, como
     * transición para mesas cerradas con el flujo anterior, con la mesa CERRADA; nunca si
     * el acta vigente ya está VALIDADA (dato oficial).
     */
    public static boolean permiteCargaActaFisica(EstadoEscrutinio estado, String revisionVigente) {
        return (conteoGuardado(estado) || estado == EstadoEscrutinio.CERRADO)
                && !VALIDADA.equals(revisionVigente);
    }

    /**
     * El acta física vigente habilita el cierre: existe y está pendiente de revisión. Un
     * acta devuelta por el Tribunal (OBSERVADA o RECHAZADA) queda en el historial y exige
     * cargar una nueva versión.
     *
     * @param hayVigente        existe un acta física activa para la mesa y el proceso
     * @param revisionVigente   su estado de revisión (null se trata como pendiente)
     */
    public static boolean actaValidaParaCierre(boolean hayVigente, String revisionVigente) {
        return hayVigente && (revisionVigente == null
                || PENDIENTE_REVISION.equals(revisionVigente));
    }

    /** Se puede cerrar: conteo guardado y acta física válida cargada. */
    public static boolean permiteCierre(EstadoEscrutinio estado, boolean hayVigente, String revisionVigente) {
        return conteoGuardado(estado) && actaValidaParaCierre(hayVigente, revisionVigente);
    }
}
