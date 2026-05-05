package ec.com.antenasur.enums;

/**
 * Fases del ciclo electoral organizadas en 10 bloques temáticos.
 * El orden refleja la secuencia típica del proceso; algunas fases pueden
 * solaparse (p. ej. INICIO_CAMPANIA y SILENCIO_ELECTORAL). El cronograma
 * define cuándo está vigente cada fase y qué edición permite.
 *
 * <p>Bloques temáticos:
 * <ol>
 *   <li>Registro inicial (1-2)</li>
 *   <li>Gestión de miembros (3-5)</li>
 *   <li>Convocatoria (6-7)</li>
 *   <li>Candidaturas (8-15)</li>
 *   <li>Organización electoral (16-21)</li>
 *   <li>Campaña electoral (22-24)</li>
 *   <li>Jornada electoral (25-27)</li>
 *   <li>Escrutinio (28)</li>
 *   <li>Resultados (29-32)</li>
 *   <li>Cierre del proceso (33-34)</li>
 * </ol>
 */
public enum FaseElectoral {

    // ── 1. Registro inicial ──────────────────────────────────────────────────
    /** Inscripción de iglesias al sistema. Docs: formulario de inscripción, resolución de registro. */
    INSCRIPCION_IGLESIAS,
    /** Asignación de usuarios a cada iglesia. Docs: acta de creación de usuarios, credenciales. */
    ASIGNACION_USUARIOS,

    // ── 2. Gestión de miembros ───────────────────────────────────────────────
    /** Actualización del listado de miembros. Docs: formulario de actualización, historial de cambios. */
    ACTUALIZACION_MIEMBROS,
    /** Generación del padrón preliminar. Docs: padrón preliminar. */
    PADRON_PRELIMINAR,
    /** Cierre y publicación del padrón definitivo (OFICIAL). Docs: padrón definitivo. */
    PADRON_DEFINITIVO,

    // ── 3. Convocatoria ──────────────────────────────────────────────────────
    /** Emisión de la convocatoria oficial a elecciones. Docs: resolución de convocatoria. */
    CONVOCATORIA,
    /** Difusión y publicación del cronograma electoral. Docs: cronograma electoral. */
    DIFUSION_CRONOGRAMA,

    // ── 4. Candidaturas ──────────────────────────────────────────────────────
    /** Recepción de formularios de inscripción de candidatos. Docs: formulario de inscripción, lista por iglesia. */
    INSCRIPCION_CANDIDATURAS,
    /** Revisión de los requisitos de cada candidatura. Docs: informe de revisión. */
    REVISION_REQUISITOS,
    /** Notificación de observaciones a las listas. Docs: notificación de observaciones. */
    NOTIFICACION_OBSERVACIONES,
    /** Plazo para subsanar observaciones. Docs: documento de corrección presentado. */
    SUBSANACION,
    /** Calificación formal de las listas. Docs: resolución de aprobación o rechazo. */
    CALIFICACION_LISTAS,
    /** Presentación de recursos de apelación. Docs: recurso de apelación. */
    APELACION,
    /** Resolución de los recursos de apelación. Docs: resolución de apelación. */
    RESOLUCION_APELACION,
    /** Resolución definitiva de listas habilitadas. Docs: resolución final de listas. */
    CALIFICACION_FINAL,

    // ── 5. Organización electoral ────────────────────────────────────────────
    /** Sorteo de casilleros en la papeleta. Docs: acta de sorteo. */
    SORTEO_CASILLEROS,
    /** Definición y resolución de recintos electorales. Docs: resolución de recintos. */
    DEFINICION_RECINTOS,
    /** Publicación del listado de mesas. Docs: listado de mesas. */
    DEFINICION_MESAS,
    /** Designación y nombramiento de miembros de JRV. Docs: acta de designación, nombramientos. */
    DESIGNACION_JRV,
    /** Capacitación de los miembros de JRV. Docs: registro de asistencia, constancia. */
    CAPACITACION_JRV,
    /** Aprobación del diseño e impresión de papeletas. Docs: diseño aprobado, orden de impresión. */
    DISENO_PAPELETAS,

    // ── 6. Campaña electoral ─────────────────────────────────────────────────
    /** Inicio oficial de la campaña. Docs: resolución de inicio de campaña. */
    INICIO_CAMPANIA,
    /** Período de silencio electoral. */
    SILENCIO_ELECTORAL,
    /** Cierre formal de la campaña. Docs: resolución de cierre de campaña. */
    CIERRE_CAMPANIA,

    // ── 7. Jornada electoral ─────────────────────────────────────────────────
    /** Instalación de mesas de votación. Docs: acta de instalación (clave). */
    INSTALACION_MESAS,
    /** Período de votación. Docs: registro de sufragantes, lista de firmas. */
    SUFRAGIO,
    /** Cierre de mesas de votación. Docs: acta de cierre. */
    CIERRE_MESAS,

    // ── 8. Escrutinio ────────────────────────────────────────────────────────
    /** Escrutinio en mesa. Docs: acta de escrutinio (MUY CLAVE), hoja de conteo. */
    ESCRUTINIO_MESA,

    // ── 9. Resultados ────────────────────────────────────────────────────────
    /** Publicación de resultados preliminares. Docs: reporte de resultados preliminares. */
    RESULTADOS_PRELIMINARES,
    /** Recepción de recursos de impugnación. Docs: recurso de impugnación. */
    RECEPCION_IMPUGNACIONES,
    /** Resolución de impugnaciones presentadas. Docs: resolución de impugnación. */
    RESOLUCION_IMPUGNACIONES,
    /** Cierre y publicación de resultados finales. Docs: acta o resolución de resultados finales. */
    RESULTADOS_FINALES,

    // ── 10. Cierre del proceso ───────────────────────────────────────────────
    /** Proclamación de ganadores. Docs: acta de proclamación de ganadores. */
    PROCLAMACION_ELECTOS,
    /** Cierre formal del proceso y entrega de credenciales. Docs: credenciales de autoridades electas. */
    CIERRE_PROCESO;

    /**
     * Indica si la fase, por su naturaleza, permite que los IglesiaAdmin
     * editen el listado de miembros. Sirve como default cuando la columna
     * {@code cref_permite_edicion} no está seteada en BD.
     */
    public boolean defaultPermiteEdicionPadron() {
        return this == ACTUALIZACION_MIEMBROS;
    }
}
