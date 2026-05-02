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

    public CronogramaFaseDTO obtenerDTOPorId(Integer id) {
        if (id == null) return null;
        return CronogramaFaseDTO.fromEntity(cronogramaFaseFacade.find(id));
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
