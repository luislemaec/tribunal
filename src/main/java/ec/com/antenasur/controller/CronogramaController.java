package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import ec.com.antenasur.dto.CronogramaFaseDTO;
import ec.com.antenasur.dto.ProcesoElectoralDTO;
import ec.com.antenasur.enums.FaseElectoral;
import ec.com.antenasur.enums.SeveridadCronograma;
import ec.com.antenasur.service.tec.CronogramaService;
import ec.com.antenasur.service.tec.ProcesoElectoralService;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Pantalla de gestiÃ³n del cronograma electoral. Permite al Admin/Tribunal:
 * <ul>
 *   <li>Crear / editar / activar procesos electorales.</li>
 *   <li>Definir las fases del cronograma del proceso seleccionado con
 *       fechas, mensaje banner y permisos.</li>
 * </ul>
 */
@Named
@ViewScoped
@Slf4j
public class CronogramaController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ProcesoElectoralService procesoElectoralService;

    @Inject
    private CronogramaService cronogramaService;

    @Setter @Getter
    private List<ProcesoElectoralDTO> procesos;

    @Setter @Getter
    private ProcesoElectoralDTO procesoSeleccionado;

    @Setter @Getter
    private List<CronogramaFaseDTO> fases;

    @Setter @Getter
    private CronogramaFaseDTO faseSeleccionada;

    @Getter
    private FaseElectoral[] fasesDisponibles = FaseElectoral.values();

    @Getter
    private SeveridadCronograma[] severidadesDisponibles = SeveridadCronograma.values();

    /** TamaÃ±o del catÃ¡logo de fases (EL no permite array.length). */
    public int getTotalFasesCatalogo() {
        return fasesDisponibles == null ? 0 : fasesDisponibles.length;
    }

    @PostConstruct
    private void init() {
        try {
            procesos = procesoElectoralService.listarDTOs();
            procesoSeleccionado = procesoElectoralService.getActivoDTO();
            if (procesoSeleccionado == null) {
                procesoSeleccionado = new ProcesoElectoralDTO();
            } else {
                fases = cronogramaService.listarDTOsPorProceso(procesoSeleccionado.getId());
            }
            faseSeleccionada = nuevaFase();
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR CRONOGRAMA", e);
        }
    }

    public void cargarFasesDelProceso() {
        if (procesoSeleccionado != null && procesoSeleccionado.getId() != null) {
            fases = cronogramaService.listarDTOsPorProceso(procesoSeleccionado.getId());
        } else {
            fases = new ArrayList<>();
        }
    }

    public void nuevoProceso() {
        procesoSeleccionado = new ProcesoElectoralDTO();
    }

    public void guardarProceso() {
        try {
            ProcesoElectoralDTO p = procesoElectoralService.guardarDesdeDTO(procesoSeleccionado);
            if (p != null) {
                procesoSeleccionado = p;
                procesos = procesoElectoralService.listarDTOs();
                JsfUtil.addSuccessMessage("Proceso guardado");
            }
        } catch (Exception e) {
            log.error("ERROR AL GUARDAR PROCESO", e);
            JsfUtil.addErrorMessage("No se pudo guardar el proceso");
        }
    }

    public CronogramaFaseDTO nuevaFase() {
        CronogramaFaseDTO f = new CronogramaFaseDTO();
        f.setSeveridad(SeveridadCronograma.INFO);
        f.setPermiteEdicion(false);
        if (procesoSeleccionado != null && procesoSeleccionado.getId() != null) {
            f.setProcesoId(procesoSeleccionado.getId());
        }
        return f;
    }

    public void crearFase() {
        faseSeleccionada = nuevaFase();
    }

    /**
     * Recarga la fase desde BD (en lugar de reusar la referencia del
     * datatable). Evita problemas de PrimeFaces donde el tabView dentro
     * del diÃ¡logo no re-renderiza correctamente cuando reusa la misma
     * instancia DTO ya enlazada a otros componentes.
     */
    public void editarFase(CronogramaFaseDTO f) {
        if (f == null || f.getId() == null) {
            this.faseSeleccionada = nuevaFase();
            return;
        }
        CronogramaFaseDTO fresco = cronogramaService.obtenerDTOPorId(f.getId());
        this.faseSeleccionada = (fresco != null) ? fresco : f;
    }

    public void guardarFase() {
        try {
            if (faseSeleccionada == null) {
                JsfUtil.addErrorMessage("No hay fase para guardar");
                return;
            }
            // Solo seteamos procesoId desde el controller en CREACIÃ“N. En
            // EDICIÃ“N respetamos el procesoId original del DTO recargado para
            // no mover una fase entre procesos por error.
            if (faseSeleccionada.getId() == null) {
                if (procesoSeleccionado == null || procesoSeleccionado.getId() == null) {
                    JsfUtil.addErrorMessage("Seleccione un proceso electoral antes de crear una fase");
                    return;
                }
                faseSeleccionada.setProcesoId(procesoSeleccionado.getId());
            }
            // ValidaciÃ³n delegada al service: errores fatales bloquean,
            // advertencias se muestran como warning sin impedir el guardado.
            CronogramaService.ValidacionFase val = cronogramaService.validar(faseSeleccionada);
            if (!val.esValida()) {
                for (String e : val.getErrores()) {
                    JsfUtil.addErrorMessage(e);
                }
                // Flag para que el oncomplete del botÃ³n mantenga el diÃ¡logo
                // abierto (los errores de negocio NO disparan args.validationFailed).
                org.primefaces.PrimeFaces.current().ajax().addCallbackParam("faseError", true);
                return;
            }
            for (String w : val.getAdvertencias()) {
                JsfUtil.addWarningMessage(w);
            }

            boolean esEdicion = faseSeleccionada.getId() != null;
            CronogramaFaseDTO p = cronogramaService.guardarDesdeDTO(faseSeleccionada);
            if (p != null) {
                JsfUtil.addSuccessMessage(esEdicion ? "Fase actualizada" : "Fase registrada");
                cargarFasesDelProceso();
                faseSeleccionada = nuevaFase();
            } else {
                JsfUtil.addErrorMessage("No se pudo guardar la fase. Verifique que el proceso exista.");
            }
        } catch (Exception e) {
            log.error("ERROR AL GUARDAR FASE", e);
            JsfUtil.addErrorMessage("Error inesperado al guardar la fase: " + e.getMessage());
        }
    }

    public void eliminarFase(CronogramaFaseDTO f) {
        try {
            if (f == null || f.getId() == null) {
                JsfUtil.addWarningMessage("No se pudo identificar la fase a eliminar");
                return;
            }
            CronogramaFaseDTO eliminada = cronogramaService.eliminarPorId(f.getId());
            cargarFasesDelProceso();
            if (eliminada != null) {
                JsfUtil.addSuccessMessage("Fase eliminada");
            } else {
                JsfUtil.addWarningMessage("La fase ya no existe en el sistema");
            }
        } catch (Exception e) {
            log.error("ERROR AL ELIMINAR FASE", e);
            JsfUtil.addErrorMessage("No se pudo eliminar la fase");
        }
    }
}
