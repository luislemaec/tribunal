package ec.com.antenasur.service.tec;

import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.util.ArrayList;

import ec.com.antenasur.dto.TribunalDTO;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.facade.IglesiaPersonaFacade;
import ec.com.antenasur.facade.tec.CatalogoGeneralFacade;
import ec.com.antenasur.facade.tec.ProcesoElectoralFacade;
import ec.com.antenasur.facade.tec.TribunalFacade;
import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.tec.CatalogoGeneral;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.model.tec.Tribunal;
import ec.com.antenasur.service.AbstractService;

@Stateless
public class TribunalService extends AbstractService<Tribunal, Integer, TribunalFacade> {

    private static final Integer CARGO_PADRE_AUTORIDADES_TRIBUNAL = 2;

    @Inject
    private TribunalFacade tribunalFacade;

    @Inject
    private ProcesoElectoralFacade procesoElectoralFacade;

    @Inject
    private CatalogoGeneralFacade catalogoFacade;

    @Inject
    private IglesiaPersonaFacade iglesiaPersonaFacade;

    @Override
    protected TribunalFacade getFacade() {
        return tribunalFacade;
    }

    public List<Tribunal> getRegistrosActivos() {
        return tribunalFacade.getRegistrosActivos();
    }

    public TribunalDTO obtenerDTOPorId(Integer id) {
        if (id == null) return null;
        return TribunalDTO.fromEntity(tribunalFacade.find(id));
    }

    public List<TribunalDTO> listarDTOsActivos() {
        return mapearLista(tribunalFacade.getRegistrosActivos());
    }

    public List<TribunalDTO> listarDTOsActivosPorProceso(Integer procesoId) {
        ProcesoElectoral proceso = procesoId != null ? procesoElectoralFacade.find(procesoId) : null;
        if (proceso == null) {
            return new ArrayList<>();
        }
        return mapearLista(tribunalFacade.getRegistrosActivosPorProceso(proceso));
    }

    public List<TribunalDTO> listarDTOs() {
        return mapearLista(tribunalFacade.findAll());
    }

    public TribunalDTO guardarDesdeDTO(TribunalDTO dto) {
        if (dto == null) {
            throw new NegocioException("No se pudo determinar la autoridad a guardar.");
        }
        Integer procesoId = dto.getProcesoId() != null ? dto.getProcesoId() : dto.getPeriodoId();
        ProcesoElectoral proceso = (procesoId != null) ? procesoElectoralFacade.find(procesoId) : null;
        CatalogoGeneral cargo = (dto.getCargoId() != null) ? catalogoFacade.find(dto.getCargoId()) : null;
        IglesiaPersona ip = (dto.getIglesiaPersona() != null && dto.getIglesiaPersona().getId() != null)
                ? iglesiaPersonaFacade.find(dto.getIglesiaPersona().getId()) : null;

        validarAsignacion(proceso, cargo, ip);
        Tribunal registroLogico = tribunalFacade.buscarPorProcesoCargoIncluyendoInactivos(
                proceso.getId(), cargo.getId());

        if (dto.getId() == null) {
            if (registroLogico != null) {
                if (Boolean.TRUE.equals(registroLogico.getEstado())) {
                    throw new NegocioException("El cargo seleccionado ya tiene una autoridad asignada en este proceso.");
                }
                registroLogico.setIglesiaPersona(ip);
                registroLogico.setProceso(proceso);
                registroLogico.setCargo(cargo);
                registroLogico.setEstado(true);
                return TribunalDTO.fromEntity(tribunalFacade.edit(registroLogico));
            }
            Tribunal nuevo = new Tribunal();
            nuevo.setProceso(proceso);
            nuevo.setCargo(cargo);
            nuevo.setIglesiaPersona(ip);
            return TribunalDTO.fromEntity(tribunalFacade.create(nuevo));
        }
        Tribunal actual = tribunalFacade.find(dto.getId());
        if (actual == null) {
            throw new NegocioException("La autoridad seleccionada ya no está disponible.");
        }
        if (registroLogico != null && !actual.getId().equals(registroLogico.getId())
                && Boolean.TRUE.equals(registroLogico.getEstado())) {
            throw new NegocioException("El cargo seleccionado ya tiene una autoridad asignada en este proceso.");
        }
        actual.setProceso(proceso);
        actual.setCargo(cargo);
        actual.setIglesiaPersona(ip);
        return TribunalDTO.fromEntity(tribunalFacade.edit(actual));
    }

    /**
     * Asigna a un DTO una IglesiaPersona resuelta por cédula. NO persiste.
     */
    public TribunalDTO asignarPersonaPorCedula(TribunalDTO dto, String cedula) {
        if (dto == null) {
            throw new NegocioException("Seleccione el cargo al que asignará la autoridad.");
        }
        if (cedula == null || !cedula.trim().matches("\\d{10}")) {
            throw new NegocioException("Ingrese una cédula válida de 10 dígitos.");
        }
        IglesiaPersona ip = iglesiaPersonaFacade.buscarPorCedulaPersona(cedula.trim());
        if (ip == null || ip.getPersona() == null || ip.getIglesia() == null) {
            throw new NegocioException("No existe un miembro activo de iglesia para la cédula ingresada.");
        }
        dto.setIglesiaPersona(ec.com.antenasur.dto.IglesiaPersonaDTO.fromEntity(ip));
        return dto;
    }

    /**
     * Devuelve la lista de autoridades vigentes; si faltan cargos, agrega
     * placeholders (TribunalDTO sin id) por cada cargo que no esté asignado.
     */
    public List<TribunalDTO> listarAutoridadesConPlaceholders(Integer procesoId, Integer cargoPadreId) {
        List<TribunalDTO> resultado = new ArrayList<>();
        ProcesoElectoral proceso = (procesoId != null) ? procesoElectoralFacade.find(procesoId) : null;
        List<Tribunal> activos = tribunalFacade.getRegistrosActivosPorProceso(proceso);

        if (activos == null || activos.isEmpty()) {
            List<CatalogoGeneral> cargos = catalogoFacade.listaCatalogoHijo(cargoPadreId);
            if (cargos != null) {
                for (CatalogoGeneral cargo : cargos) {
                    Tribunal placeholder = new Tribunal();
                    placeholder.setCargo(cargo);
                    placeholder.setProceso(proceso);
                    resultado.add(TribunalDTO.fromEntity(placeholder));
                }
            }
            return resultado;
        }

        List<Integer> idsCargosAsignados = new ArrayList<>();
        for (Tribunal t : activos) {
            resultado.add(TribunalDTO.fromEntity(t));
            if (t.getCargo() != null) idsCargosAsignados.add(t.getCargo().getId());
        }
        List<CatalogoGeneral> cargosFaltantes = catalogoFacade.listaCatalogoHijo(cargoPadreId, idsCargosAsignados);
        if (cargosFaltantes != null) {
            for (CatalogoGeneral cargo : cargosFaltantes) {
                Tribunal placeholder = new Tribunal();
                placeholder.setCargo(cargo);
                placeholder.setProceso(proceso);
                resultado.add(TribunalDTO.fromEntity(placeholder));
            }
        }
        return resultado;
    }

    public TribunalDTO eliminarPorId(Integer id) {
        if (id == null) return null;
        Tribunal t = tribunalFacade.find(id);
        if (t == null) return null;
        return TribunalDTO.fromEntity(tribunalFacade.delete(t));
    }

    private List<TribunalDTO> mapearLista(List<Tribunal> entidades) {
        List<TribunalDTO> resultado = new ArrayList<>();
        if (entidades == null) return resultado;
        for (Tribunal t : entidades) resultado.add(TribunalDTO.fromEntity(t));
        return resultado;
    }

    private void validarAsignacion(ProcesoElectoral proceso, CatalogoGeneral cargo, IglesiaPersona iglesiaPersona) {
        if (proceso == null) {
            throw new NegocioException("Debe seleccionar un proceso electoral válido.");
        }
        if (cargo == null || cargo.getPadre() == null
                || !CARGO_PADRE_AUTORIDADES_TRIBUNAL.equals(cargo.getPadre().getId())) {
            throw new NegocioException("El cargo seleccionado no corresponde a una autoridad del tribunal.");
        }
        if (iglesiaPersona == null) {
            throw new NegocioException("Debe buscar y seleccionar un miembro de iglesia para asignar la autoridad.");
        }
    }
}
