package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.CandidatoDTO;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.facade.IglesiaPersonaFacade;
import ec.com.antenasur.facade.tec.CandidatoFacade;
import ec.com.antenasur.facade.tec.CatalogoGeneralFacade;
import ec.com.antenasur.facade.tec.ListaFacade;
import ec.com.antenasur.facade.tec.ProcesoElectoralFacade;
import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.tec.Candidato;
import ec.com.antenasur.model.tec.CatalogoGeneral;
import ec.com.antenasur.model.tec.Lista;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.service.AbstractService;
import ec.com.antenasur.util.Constantes;

@Stateless
public class CandidatoService extends AbstractService<Candidato, Integer, CandidatoFacade> {

    private static final Integer CARGO_PADRE_CANDIDATOS = 8;

    @Inject
    private CandidatoFacade candidatoFacade;

    @Inject
    private ListaFacade listaFacade;

    @Inject
    private ProcesoElectoralFacade procesoElectoralFacade;

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
    public List<Candidato> obtenerCandidatosPorLista(Lista lista, ProcesoElectoral proceso, List<CatalogoGeneral> cargos) {
        List<Candidato> resultado = new ArrayList<>();
        if (lista == null || lista.getId() == null || proceso == null || cargos == null) {
            return resultado;
        }
        List<Integer> cargoIds = new ArrayList<>();
        for (CatalogoGeneral cargo : cargos) {
            if (cargo != null && cargo.getId() != null) {
                cargoIds.add(cargo.getId());
            }
        }
        Map<Integer, Candidato> candidatosPorCargo = new HashMap<>();
        for (Candidato candidato : candidatoFacade.listarPorListaProcesoYCargos(
                lista.getId(), proceso.getId(), cargoIds)) {
            if (candidato.getCargo() != null) {
                candidatosPorCargo.putIfAbsent(candidato.getCargo().getId(), candidato);
            }
        }
        for (CatalogoGeneral cargo : cargos) {
            if (cargo == null || cargo.getId() == null) {
                continue;
            }
            Candidato encontrado = candidatosPorCargo.get(cargo.getId());
            if (encontrado != null) {
                resultado.add(encontrado);
            } else {
                Candidato placeholder = new Candidato();
                placeholder.setLista(lista);
                placeholder.setProceso(proceso);
                placeholder.setCargo(cargo);
                resultado.add(placeholder);
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
    public List<CandidatoDTO> listarDTOsPorLista(Integer listaId, Integer procesoId, List<Integer> cargoIds) {
        List<CatalogoGeneral> cargos = catalogoFacade.listarPorIds(cargoIds);
        return listarDTOsPorListaConCargos(listaId, procesoId, cargos);
    }

    /**
     * Carga los candidatos de la lista en una consulta y completa los cargos
     * pendientes como placeholders, respetando el orden del catálogo recibido.
     */
    public List<CandidatoDTO> listarDTOsPorListaConCargos(
            Integer listaId, Integer procesoId, List<CatalogoGeneral> cargos) {
        List<CandidatoDTO> resultado = new ArrayList<>();
        if (listaId == null || procesoId == null || cargos == null || cargos.isEmpty()) {
            return resultado;
        }
        Lista lista = listaFacade.find(listaId);
        ProcesoElectoral proceso = procesoElectoralFacade.find(procesoId);
        if (lista == null || proceso == null) {
            return resultado;
        }
        List<Integer> cargoIds = new ArrayList<>();
        for (CatalogoGeneral cargo : cargos) {
            if (cargo != null && cargo.getId() != null) {
                cargoIds.add(cargo.getId());
            }
        }
        Map<Integer, Candidato> candidatosPorCargo = new HashMap<>();
        for (Candidato candidato : candidatoFacade.listarPorListaProcesoYCargos(listaId, procesoId, cargoIds)) {
            if (candidato.getCargo() != null) {
                // La UI representa una plaza por cargo; ante datos históricos
                // duplicados conserva la primera fila y no crea más registros.
                candidatosPorCargo.putIfAbsent(candidato.getCargo().getId(), candidato);
            }
        }
        for (CatalogoGeneral cargo : cargos) {
            if (cargo == null || cargo.getId() == null) {
                continue;
            }
            Candidato encontrado = candidatosPorCargo.get(cargo.getId());
            if (encontrado != null) {
                resultado.add(CandidatoDTO.fromEntity(encontrado));
            } else {
                Candidato placeholder = new Candidato();
                placeholder.setLista(lista);
                placeholder.setProceso(proceso);
                placeholder.setCargo(cargo);
                resultado.add(CandidatoDTO.fromEntity(placeholder));
            }
        }
        return resultado;
    }

    /**
     * Persiste el candidato a partir del DTO. Resuelve {@code lista},
     * {@code proceso}, {@code cargo} e {@code iglesiaPersona} contra BD por
     * sus ids. Si el id es null hace create, si no, hidrata el candidato
     * existente.
     */
    public CandidatoDTO guardarDesdeDTO(CandidatoDTO dto) {
        if (dto == null) {
            throw new NegocioException("No se pudo determinar el candidato a guardar.");
        }
        Lista lista = (dto.getListaId() != null) ? listaFacade.find(dto.getListaId()) : null;
        Integer procesoId = dto.getProcesoId() != null ? dto.getProcesoId() : dto.getPeriodoId();
        ProcesoElectoral proceso = (procesoId != null) ? procesoElectoralFacade.find(procesoId) : null;
        CatalogoGeneral cargo = (dto.getCargoId() != null) ? catalogoFacade.find(dto.getCargoId()) : null;
        IglesiaPersona iglesiaPersona = (dto.getIglesiaPersona() != null && dto.getIglesiaPersona().getId() != null)
                ? iglesiaPersonaFacade.find(dto.getIglesiaPersona().getId()) : null;

        validarAsignacion(lista, proceso, cargo, iglesiaPersona);
        if (candidatoFacade.existePersonaActivaEnListaProceso(
                lista.getId(), proceso.getId(), iglesiaPersona.getId(), dto.getId())) {
            throw new NegocioException(Constantes.getMensaje("form.candidatos.error.duplicate.person"));
        }
        Candidato registroLogico = candidatoFacade.buscarPorListaCargoProcesoIncluyendoInactivos(
                lista.getId(), cargo.getId(), proceso.getId());

        if (dto.getId() == null) {
            if (registroLogico != null) {
                if (Boolean.TRUE.equals(registroLogico.getEstado())) {
                    throw new NegocioException("El cargo ya tiene un candidato asignado en esta lista y proceso.");
                }
                registroLogico.setIglesiaPersona(iglesiaPersona);
                registroLogico.setLista(lista);
                registroLogico.setProceso(proceso);
                registroLogico.setCargo(cargo);
                registroLogico.setEstado(true);
                return CandidatoDTO.fromEntity(candidatoFacade.edit(registroLogico));
            }
            Candidato nuevo = new Candidato();
            nuevo.setIglesiaPersona(iglesiaPersona);
            nuevo.setLista(lista);
            nuevo.setProceso(proceso);
            nuevo.setCargo(cargo);
            return CandidatoDTO.fromEntity(candidatoFacade.create(nuevo));
        }
        Candidato actual = candidatoFacade.find(dto.getId());
        if (actual == null) {
            throw new NegocioException("El candidato seleccionado ya no está disponible.");
        }
        if (registroLogico != null && !actual.getId().equals(registroLogico.getId())
                && Boolean.TRUE.equals(registroLogico.getEstado())) {
            throw new NegocioException("El cargo ya tiene un candidato asignado en esta lista y proceso.");
        }
        actual.setLista(lista);
        actual.setProceso(proceso);
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
        if (candidatoDto == null) {
            throw new NegocioException("Seleccione el cargo al que asignará el candidato.");
        }
        if (cedula == null || !cedula.trim().matches("\\d{10}")) {
            throw new NegocioException("Ingrese una cédula válida de 10 dígitos.");
        }
        IglesiaPersona ip = iglesiaPersonaFacade.buscarPorCedulaPersona(cedula.trim());
        if (ip == null || ip.getPersona() == null || ip.getIglesia() == null) {
            throw new NegocioException("No existe un miembro activo de iglesia para la cédula ingresada.");
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

    private void validarAsignacion(Lista lista, ProcesoElectoral proceso,
            CatalogoGeneral cargo, IglesiaPersona iglesiaPersona) {
        if (lista == null) {
            throw new NegocioException("Debe seleccionar una lista electoral válida.");
        }
        if (proceso == null) {
            throw new NegocioException("No existe un proceso electoral activo para asignar candidatos.");
        }
        if (cargo == null || cargo.getPadre() == null
                || !CARGO_PADRE_CANDIDATOS.equals(cargo.getPadre().getId())) {
            throw new NegocioException("El cargo seleccionado no corresponde a una candidatura.");
        }
        if (iglesiaPersona == null) {
            throw new NegocioException("Debe buscar y seleccionar un miembro de iglesia para asignar el candidato.");
        }
    }
}
