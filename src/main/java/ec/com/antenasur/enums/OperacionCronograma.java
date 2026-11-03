package ec.com.antenasur.enums;

/**
 * Operaciones de escritura sujetas al cronograma electoral. La decisión de si están
 * permitidas para el usuario actual la toma {@code CronogramaService#permite}.
 */
public enum OperacionCronograma {
    /** Crear, editar y eliminar iglesias (fase {@link FaseElectoral#INSCRIPCION_IGLESIAS}). */
    REGISTRO_IGLESIAS,
    /** Asignar o cambiar el administrador de una iglesia (fase {@link FaseElectoral#ASIGNACION_USUARIOS}). */
    ASIGNACION_ADMINISTRADORES,
    /**
     * Crear, editar, habilitar, regularizar y dar de baja miembros, individual o masivamente
     * (fase que permite la edición del padrón).
     */
    EDICION_PADRON
}
