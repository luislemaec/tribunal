package ec.com.antenasur.service.tec;

import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.util.ArrayList;

import ec.com.antenasur.dto.TribunalDTO;
import ec.com.antenasur.facade.IglesiaPersonaFacade;
import ec.com.antenasur.facade.tec.CatalogoGeneralFacade;
import ec.com.antenasur.facade.tec.PeriodoFacade;
import ec.com.antenasur.facade.tec.TribunalFacade;
import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.tec.CatalogoGeneral;
import ec.com.antenasur.model.tec.Periodo;
import ec.com.antenasur.model.tec.Tribunal;
import ec.com.antenasur.service.AbstractService;

@Stateless
public class TribunalService extends AbstractService<Tribunal, Integer, TribunalFacade> {

    @Inject
    private TribunalFacade tribunalFacade;

    @Inject
    private PeriodoFacade periodoFacade;

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

    public List<TribunalDTO> listarDTOs() {
        return mapearLista(tribunalFacade.findAll());
    }

    public TribunalDTO guardarDesdeDTO(TribunalDTO dto) {
        if (dto == null) return null;
        Periodo periodo = (dto.getPeriodoId() != null) ? periodoFacade.find(dto.getPeriodoId()) : null;
        CatalogoGeneral cargo = (dto.getCargoId() != null) ? catalogoFacade.find(dto.getCargoId()) : null;
        IglesiaPersona ip = (dto.getIglesiaPersona() != null && dto.getIglesiaPersona().getId() != null)
                ? iglesiaPersonaFacade.find(dto.getIglesiaPersona().getId()) : null;

        if (dto.getId() == null) {
            Tribunal nuevo = new Tribunal();
            nuevo.setPeriodo(periodo);
            nuevo.setCargo(cargo);
            nuevo.setIglesiaPersona(ip);
            return TribunalDTO.fromEntity(tribunalFacade.create(nuevo));
        }
        Tribunal actual = tribunalFacade.find(dto.getId());
        if (actual == null) return null;
        actual.setPeriodo(periodo);
        actual.setCargo(cargo);
        actual.setIglesiaPersona(ip);
        return TribunalDTO.fromEntity(tribunalFacade.edit(actual));
    }

    /**
     * Asigna a un DTO una IglesiaPersona resuelta por cédula. NO persiste.
     */
    public TribunalDTO asignarPersonaPorCedula(TribunalDTO dto, String cedula) {
        if (dto == null || cedula == null || cedula.isEmpty()) return dto;
        IglesiaPersona ip = iglesiaPersonaFacade.buscarPorCedulaPersona(cedula);
        if (ip == null) return null;
        dto.setIglesiaPersona(ec.com.antenasur.dto.IglesiaPersonaDTO.fromEntity(ip));
        return dto;
    }

    /**
     * Devuelve la lista de autoridades vigentes; si faltan cargos, agrega
     * placeholders (TribunalDTO sin id) por cada cargo que no esté asignado.
     */
    public List<TribunalDTO> listarAutoridadesConPlaceholders(Integer periodoId, Integer cargoPadreId) {
        List<TribunalDTO> resultado = new ArrayList<>();
        List<Tribunal> activos = tribunalFacade.getRegistrosActivos();
        Periodo periodo = (periodoId != null) ? periodoFacade.find(periodoId) : null;

        if (activos == null || activos.isEmpty()) {
            List<CatalogoGeneral> cargos = catalogoFacade.listaCatalogoHijo(cargoPadreId);
            if (cargos != null) {
                for (CatalogoGeneral cargo : cargos) {
                    Tribunal placeholder = new Tribunal();
                    placeholder.setCargo(cargo);
                    placeholder.setPeriodo(periodo);
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
                placeholder.setPeriodo(periodo);
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
}
