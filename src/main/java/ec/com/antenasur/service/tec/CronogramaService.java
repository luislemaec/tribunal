package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import ec.com.antenasur.dto.CronogramaFaseDTO;
import ec.com.antenasur.enums.FaseElectoral;
import ec.com.antenasur.facade.tec.CronogramaFaseFacade;
import ec.com.antenasur.facade.tec.ProcesoElectoralFacade;
import ec.com.antenasur.model.tec.CronogramaFase;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.service.AbstractService;

/**
 * Lógica del cronograma electoral. Centraliza:
 * <ul>
 *   <li>Cálculo de la fase vigente del proceso activo.</li>
 *   <li>Permisos de edición por fase (gobierna el padrón, listas, etc.).</li>
 *   <li>CRUD de fases para la pantalla de gestión.</li>
 * </ul>
 *
 * <p>Sin proceso activo o sin fase vigente, el sistema cae en
 * <b>solo lectura por defecto</b> — los servicios que dependen de fase
 * deben rechazar escrituras.
 */
@Stateless
public class CronogramaService extends AbstractService<CronogramaFase, Integer, CronogramaFaseFacade> {

    @Inject
    private CronogramaFaseFacade cronogramaFaseFacade;

    @Inject
    private ProcesoElectoralFacade procesoElectoralFacade;

    @Override
    protected CronogramaFaseFacade getFacade() {
        return cronogramaFaseFacade;
    }

    /**
     * Devuelve la fase vigente del proceso electoral activo, o {@code null}
     * si no hay proceso activo o si todas las fases están fuera de rango.
     */
    public CronogramaFaseDTO getFaseVigenteDelProcesoActivo() {
        ProcesoElectoral activo = procesoElectoralFacade.getActivo();
        if (activo == null) return null;
        return CronogramaFaseDTO.fromEntity(
                cronogramaFaseFacade.getVigentePorProceso(activo.getId()));
    }

    /**
     * Indica si la fase vigente permite edición del padrón. Combina la
     * columna {@code cref_permite_edicion} (override del admin) con el
     * default semántico del enum {@link FaseElectoral}.
     *
     * <p>Sin proceso activo o sin fase vigente devuelve {@code false}
     * (modo seguro: nada se edita fuera del cronograma).
     */
    public boolean permiteEdicionPadron() {
        CronogramaFaseDTO vigente = getFaseVigenteDelProcesoActivo();
        if (vigente == null) return false;
        if (vigente.getPermiteEdicion() != null) {
            return vigente.getPermiteEdicion();
        }
        return vigente.getFase() != null && vigente.getFase().defaultPermiteEdicionPadron();
    }

    /**
     * Indica si el registro/edición de iglesias está habilitado.
     * Se permite únicamente cuando la fase vigente es {@link FaseElectoral#INSCRIPCION_IGLESIAS}.
     *
     * <p>No usa {@code cref_permite_edicion} porque ese campo es exclusivo del
     * control del padrón de miembros ({@link #permiteEdicionPadron()}).
     */
    public boolean permiteRegistroIglesias() {
        CronogramaFaseDTO vigente = getFaseVigenteDelProcesoActivo();
        if (vigente == null) return false;
        return vigente.getFase() == FaseElectoral.INSCRIPCION_IGLESIAS;
    }

    /**
     * Indica si la asignación/reasignación de usuarios IglesiaAdmin a iglesias
     * está habilitada. Se permite únicamente cuando la fase vigente es
     * {@link FaseElectoral#ASIGNACION_USUARIOS}.
     */
    public boolean permiteAsignacionUsuarios() {
        CronogramaFaseDTO vigente = getFaseVigenteDelProcesoActivo();
        if (vigente == null) return false;
        return vigente.getFase() == FaseElectoral.ASIGNACION_USUARIOS;
    }

    public CronogramaFaseDTO obtenerDTOPorId(Integer id) {
        if (id == null) return null;
        return CronogramaFaseDTO.fromEntity(cronogramaFaseFacade.find(id));
    }

    /**
     * Resultado de la validación de una fase antes de guardar. Contiene
     * errores fatales (bloquean) y advertencias (informan al usuario pero
     * no impiden guardar).
     */
    public static class ValidacionFase {
        private final List<String> errores = new ArrayList<>();
        private final List<String> advertencias = new ArrayList<>();
        public boolean esValida() { return errores.isEmpty(); }
        public List<String> getErrores() { return errores; }
        public List<String> getAdvertencias() { return advertencias; }
        public void error(String m) { errores.add(m); }
        public void advertencia(String m) { advertencias.add(m); }
    }

    /**
     * Valida una fase candidata a guardar. Verifica:
     * <ul>
     *   <li>Campos obligatorios (fase, fechas, título).</li>
     *   <li>Que fechaInicio sea anterior a fechaFin.</li>
     *   <li>Que el rango de la fase caiga dentro del rango del proceso.</li>
     *   <li>Que no haya OTRA fase con el mismo enum FaseElectoral en el
     *       mismo proceso (advertencia: dos veces ACTUALIZACION_PADRON
     *       suele ser un error humano).</li>
     *   <li>Que no haya OTRA fase del mismo proceso con rango solapado
     *       (advertencia: superposición temporal puede ser intencional).</li>
     * </ul>
     */
    public ValidacionFase validar(CronogramaFaseDTO dto) {
        ValidacionFase v = new ValidacionFase();
        if (dto == null) {
            v.error("No hay datos para validar");
            return v;
        }
        // 1. Campos obligatorios
        if (dto.getFase() == null) {
            v.error("Debe seleccionar la fase del catálogo");
        }
        if (dto.getTitulo() == null || dto.getTitulo().trim().isEmpty()) {
            v.error("El título es obligatorio");
        }
        if (dto.getFechaInicio() == null) {
            v.error("La fecha de inicio es obligatoria");
        }
        if (dto.getFechaFin() == null) {
            v.error("La fecha de fin es obligatoria");
        }
        if (dto.getProcesoId() == null) {
            v.error("Debe asociar la fase a un proceso electoral");
        }
        if (!v.esValida()) {
            return v; // sin fechas o ids no tiene sentido seguir
        }

        // 2. Coherencia de rango
        if (!dto.getFechaInicio().before(dto.getFechaFin())) {
            v.error("La fecha de inicio debe ser anterior a la fecha de fin");
            return v;
        }
        long durMs = dto.getFechaFin().getTime() - dto.getFechaInicio().getTime();
        if (durMs < 60000L) { // < 1 minuto
            v.error("La duración de la fase debe ser mayor a un minuto");
            return v;
        }

        // 3. Rango contra el proceso electoral
        ProcesoElectoral proceso = procesoElectoralFacade.find(dto.getProcesoId());
        if (proceso == null) {
            v.error("El proceso electoral no existe");
            return v;
        }
        if (proceso.getFechaInicio() != null
                && dto.getFechaInicio().before(proceso.getFechaInicio())) {
            v.error("La fase inicia antes que el proceso electoral ("
                    + formatear(proceso.getFechaInicio()) + ")");
        }
        if (proceso.getFechaFin() != null
                && dto.getFechaFin().after(proceso.getFechaFin())) {
            v.error("La fase termina después que el proceso electoral ("
                    + formatear(proceso.getFechaFin()) + ")");
        }

        // 4. Duplicados y superposiciones (consultando otras fases del proceso)
        List<CronogramaFase> otras = cronogramaFaseFacade.listarPorProceso(dto.getProcesoId());
        if (otras != null) {
            for (CronogramaFase f : otras) {
                // ignoramos la propia fase si es edición
                if (dto.getId() != null && dto.getId().equals(f.getId())) continue;

                // 4a. Misma fase enum repetida en el proceso
                if (f.getFase() != null && f.getFase().equals(dto.getFase())) {
                    v.advertencia("Ya existe otra fase '" + dto.getFase()
                            + "' en este proceso (id #" + f.getId() + ")");
                }
                // 4b. Solapamiento de rangos
                if (rangosSolapan(dto.getFechaInicio(), dto.getFechaFin(),
                        f.getFechaInicio(), f.getFechaFin())) {
                    v.advertencia("Rango solapado con la fase '"
                            + (f.getFase() == null ? "?" : f.getFase().name())
                            + "' (" + formatear(f.getFechaInicio())
                            + " — " + formatear(f.getFechaFin()) + ")");
                }
            }
        }
        return v;
    }

    private static boolean rangosSolapan(java.util.Date aIni, java.util.Date aFin,
                                         java.util.Date bIni, java.util.Date bFin) {
        if (aIni == null || aFin == null || bIni == null || bFin == null) return false;
        return aIni.before(bFin) && bIni.before(aFin);
    }

    private static String formatear(java.util.Date d) {
        if (d == null) return "—";
        return new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(d);
    }

    public List<CronogramaFaseDTO> listarDTOsPorProceso(Integer procesoId) {
        List<CronogramaFaseDTO> r = new ArrayList<>();
        if (procesoId == null) return r;
        for (CronogramaFase f : cronogramaFaseFacade.listarPorProceso(procesoId)) {
            r.add(CronogramaFaseDTO.fromEntity(f));
        }
        return r;
    }

    public CronogramaFaseDTO guardarDesdeDTO(CronogramaFaseDTO dto) {
        if (dto == null || dto.getProcesoId() == null
                || dto.getFase() == null
                || dto.getFechaInicio() == null || dto.getFechaFin() == null) {
            return null;
        }
        ProcesoElectoral proceso = procesoElectoralFacade.find(dto.getProcesoId());
        if (proceso == null) return null;

        CronogramaFase f;
        if (dto.getId() == null) {
            f = dto.toEntity();
            f.setProceso(proceso);
            return CronogramaFaseDTO.fromEntity(cronogramaFaseFacade.create(f));
        }
        f = cronogramaFaseFacade.find(dto.getId());
        if (f == null) return null;
        f.setProceso(proceso);
        f.setFase(dto.getFase());
        f.setTitulo(dto.getTitulo());
        f.setMensaje(dto.getMensaje());
        f.setSeveridad(dto.getSeveridad());
        f.setFechaInicio(dto.getFechaInicio());
        f.setFechaFin(dto.getFechaFin());
        f.setPermiteEdicion(dto.getPermiteEdicion());
        f.setOrden(dto.getOrden());
        return CronogramaFaseDTO.fromEntity(cronogramaFaseFacade.edit(f));
    }

    public CronogramaFaseDTO eliminarPorId(Integer id) {
        if (id == null) return null;
        CronogramaFase f = cronogramaFaseFacade.find(id);
        if (f == null) return null;
        return CronogramaFaseDTO.fromEntity(cronogramaFaseFacade.delete(f));
    }
}
