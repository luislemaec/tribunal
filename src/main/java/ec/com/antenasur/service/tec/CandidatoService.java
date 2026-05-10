package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.CandidatoDTO;
import ec.com.antenasur.facade.IglesiaPersonaFacade;
import ec.com.antenasur.facade.tec.CandidatoFacade;
import ec.com.antenasur.facade.tec.CatalogoGeneralFacade;
import ec.com.antenasur.facade.tec.ListaFacade;
import ec.com.antenasur.facade.tec.PeriodoFacade;
import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.tec.Candidato;
import ec.com.antenasur.model.tec.CatalogoGeneral;
import ec.com.antenasur.model.tec.Lista;
import ec.com.antenasur.model.tec.Periodo;
import ec.com.antenasur.service.AbstractService;

@Stateless
public class CandidatoService extends AbstractService<Candidato, Integer, CandidatoFacade> {

    @Inject
    private CandidatoFacade candidatoFacade;

    @Inject
    private ListaFacade listaFacade;

    @Inject
    private PeriodoFacade periodoFacade;

    @Inject
    private CatalogoGeneralFacade catalogoFacade;

    @Inject
    private IglesiaPersonaFacade iglesiaPersonaFacade;

    @Override
    protected CandidatoFacade getFacade() {
        return candidatoFacade;
    }

    public Candidato getPorCargoYLista(CatalogoGeneral cargo, Lista listaSeleccionado) {
        return candidatoFacade.getPorCargoYLista(cargo, listaSeleccionado);
    }

    /**
     * Devuelve los candidatos de una lista para los cargos dados; si un cargo
     * no tiene candidato asignado, lo representa con un Candidato placeholder
     * (sin id, sin persona) para que la vista pueda exponerlo como slot vacío.
     *
     * @param lista lista política seleccionada (no null, con id)
     * @param periodo período al que pertenecen los candidatos (no null)
     * @param cargos cargos a iterar (orden definido por el caller)
     * @return lista del mismo tamaño que {@code cargos}; vacía si la lista o
     *         alguno de los argumentos es null/incompleto
     */
    public List<Candidato> obtenerCandidatosPorLista(Lista lista, Periodo periodo, List<CatalogoGeneral> cargos) {
        List<Candidato> resultado = new ArrayList<>();
        if (lista == null || lista.getId() == null || cargos == null) {
            return resultado;
        }
        for (CatalogoGeneral cargo : cargos) {
            Candidato encontrado = candidatoFacade.getPorCargoYLista(cargo, lista);
            if (encontrado != null) {
                resultado.add(encontrado);
            } else {
                resultado.add(new Candidato(null, null, lista, periodo, cargo));
            }
        }
        return resultado;
    }

    // ----- API basada en DTO -----

    public CandidatoDTO obtenerDTOPorId(Integer id) {
        if (id == null) {
            return null;
        }
        return CandidatoDTO.fromEntity(candidatoFacade.find(id));
    }

    public List<CandidatoDTO> listarDTOs() {
        return mapearLista(candidatoFacade.findAll());
    }

    /**
     * Versión DTO de {@link #obtenerCandidatosPorLista}: devuelve la lista de
     * candidatos para una lista política dada. Para los cargos sin candidato
     * asignado retorna placeholders (DTO sin id, con cargo/lista/periodo
     * referenciados por id+nombre).
     */
    public List<CandidatoDTO> listarDTOsPorLista(Integer listaId, Integer periodoId, List<Integer> cargoIds) {
        List<CandidatoDTO> resultado = new ArrayList<>();
        if (listaId == null || cargoIds == null || cargoIds.isEmpty()) {
            return resultado;
        }
        Lista lista = listaFacade.find(listaId);
        Periodo periodo = (periodoId != null) ? periodoFacade.find(periodoId) : null;
        if (lista == null) {
            return resultado;
        }
        for (Integer cargoId : cargoIds) {
            CatalogoGeneral cargo = catalogoFacade.find(cargoId);
            if (cargo == null) {
                continue;
            }
            Candidato encontrado = candidatoFacade.getPorCargoYLista(cargo, lista);
            if (encontrado != null) {
                resultado.add(CandidatoDTO.fromEntity(encontrado));
            } else {
                Candidato placeholder = new Candidato(null, null, lista, periodo, cargo);
                resultado.add(CandidatoDTO.fromEntity(placeholder));
            }
        }
        return resultado;
    }

    /**
     * Persiste el candidato a partir del DTO. Resuelve {@code lista},
     * {@code periodo}, {@code cargo} e {@code iglesiaPersona} contra BD por
     * sus ids. Si el id es null hace create, si no, hidrata el candidato
     * existente.
     */
    public CandidatoDTO guardarDesdeDTO(CandidatoDTO dto) {
        if (dto == null) {
            return null;
        }
        Lista lista = (dto.getListaId() != null) ? listaFacade.find(dto.getListaId()) : null;
        Periodo periodo = (dto.getPeriodoId() != null) ? periodoFacade.find(dto.getPeriodoId()) : null;
        CatalogoGeneral cargo = (dto.getCargoId() != null) ? catalogoFacade.find(dto.getCargoId()) : null;
        IglesiaPersona iglesiaPersona = (dto.getIglesiaPersona() != null && dto.getIglesiaPersona().getId() != null)
                ? iglesiaPersonaFacade.find(dto.getIglesiaPersona().getId()) : null;

        if (dto.getId() == null) {
            Candidato nuevo = new Candidato(null, iglesiaPersona, lista, periodo, cargo);
            return CandidatoDTO.fromEntity(candidatoFacade.create(nuevo));
        }
        Candidato actual = candidatoFacade.find(dto.getId());
        if (actual == null) {
            return null;
        }
        actual.setLista(lista);
        actual.setPeriodo(periodo);
        actual.setCargo(cargo);
        actual.setIglesiaPersona(iglesiaPersona);
        return CandidatoDTO.fromEntity(candidatoFacade.edit(actual));
    }

    public CandidatoDTO eliminarPorId(Integer id) {
        if (id == null) {
            return null;
        }
        Candidato c = candidatoFacade.find(id);
        if (c == null) {
            return null;
        }
        return CandidatoDTO.fromEntity(candidatoFacade.delete(c));
    }

    /**
     * Asigna a un candidato una IglesiaPersona resuelta por su cédula.
     * Devuelve null si no existe persona con esa cédula. NO persiste — el
     * caller decide cuándo guardar el candidato completo.
     */
    public CandidatoDTO asignarPersonaPorCedula(CandidatoDTO candidatoDto, String cedula) {
        if (candidatoDto == null || cedula == null || cedula.isEmpty()) {
            return candidatoDto;
        }
        IglesiaPersona ip = iglesiaPersonaFacade.buscarPorCedulaPersona(cedula);
        if (ip == null) {
            return null;
        }
        candidatoDto.setIglesiaPersona(ec.com.antenasur.dto.IglesiaPersonaDTO.fromEntity(ip));
        return candidatoDto;
    }

    private List<CandidatoDTO> mapearLista(List<Candidato> candidatos) {
        List<CandidatoDTO> resultado = new ArrayList<>();
        if (candidatos == null) {
            return resultado;
        }
        for (Candidato c : candidatos) {
            resultado.add(CandidatoDTO.fromEntity(c));
        }
        return resultado;
    }
}
