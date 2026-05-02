package ec.com.antenasur.enums;

/**
 * Fases del ciclo electoral. El orden refleja la secuencia típica de un
 * proceso, pero las fases pueden solaparse (p. ej. CAMPANIA y SILENCIO
 * en sus últimas horas). El cronograma define cuándo está vigente cada
 * fase y qué edición permite.
 *
 * <p>Bloques temáticos:
 * <ol>
 *   <li>Pre-electoral (1-4)</li>
 *   <li>Padrón (5-9)</li>
 *   <li>Listas y candidatos (10-17)</li>
 *   <li>Campaña (18-20)</li>
 *   <li>Logística (21-25)</li>
 *   <li>Día E (26-28)</li>
 *   <li>Post-votación (29-40)</li>
 * </ol>
 */
public enum FaseElectoral {

    // Pre-electoral
    CONVOCATORIA,
    DEFINICION_CARGOS,
    APROBACION_PRESUPUESTO,
    DIFUSION_CRONOGRAMA,

    // Padrón
    ACTUALIZACION_PADRON,
    CIERRE_PADRON_PROVISIONAL,
    RECLAMOS_PADRON,
    RESOLUCION_RECLAMOS_PADRON,
    PADRON_DEFINITIVO,

    // Listas y candidatos
    INSCRIPCION_LISTAS,
    REVISION_LISTAS,
    SUBSANACION_LISTAS,
    NOTIFICACION_CALIFICACION,
    APELACION_CALIFICACION,
    RESOLUCION_APELACIONES,
    PROCLAMACION_LISTAS,
    SORTEO_CASILLEROS,

    // Campaña
    INICIO_CAMPANIA,
    REGISTRO_PROPAGANDA,
    SILENCIO_ELECTORAL,

    // Logística
    DESIGNACION_JRV,
    CAPACITACION_JRV,
    HABILITACION_RECINTOS,
    ACREDITACION_OBSERVADORES,
    DISTRIBUCION_MATERIAL,

    // Día E
    INSTALACION_MESAS,
    SUFRAGIO,
    CIERRE_MESAS,

    // Post-votación
    ESCRUTINIO_MESA,
    ACTA_FIRMADA,
    TRANSMISION_RESULTADOS,
    RESULTADOS_PROVISIONALES,
    RECEPCION_IMPUGNACIONES,
    RESOLUCION_IMPUGNACIONES,
    ADJUDICACION_ESCANIOS,
    RESULTADOS_OFICIALES,
    PROCLAMACION_ELECTOS,
    RECURSOS_FINALES,
    POSESION,
    CIERRE_PROCESO;

    /**
     * Indica si la fase, por su naturaleza, permite que los IglesiaAdmin
     * editen el padrón. Sirve como default cuando la columna
     * {@code cref_permite_edicion} no está seteada en BD.
     */
    public boolean defaultPermiteEdicionPadron() {
        return this == ACTUALIZACION_PADRON;
    }
}
