package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.PadronDTO;
import ec.com.antenasur.facade.IglesiaFacade;
import ec.com.antenasur.facade.IglesiaPersonaFacade;
import ec.com.antenasur.facade.PersonaFacade;
import ec.com.antenasur.facade.tec.MesaFacade;
import ec.com.antenasur.facade.tec.PadronFacade;
import ec.com.antenasur.facade.tec.PeriodoFacade;
import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.Persona;
import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.model.tec.Padron;
import ec.com.antenasur.model.tec.Periodo;
import ec.com.antenasur.service.AbstractService;

@Stateless
public class PadronService extends AbstractService<Padron, Integer, PadronFacade> {

    @Inject
    private PadronFacade padronFacade;

    @Inject
    private IglesiaPersonaFacade iglesiaPersonaFacade;

    @Inject
    private IglesiaFacade iglesiaFacade;

    @Inject
    private PersonaFacade personaFacade;

    @Inject
    private MesaFacade mesaFacade;

    @Inject
    private PeriodoFacade periodoFacade;

    @Override
    protected PadronFacade getFacade() {
        return padronFacade;
    }

    public List<Padron> getAllOrderbyId() {
        return padronFacade.getAllOrderbyId();
    }

    public Padron buscaPadronPorMesa(String nombreMesa) {
        return padronFacade.buscaPadronPorMesa(nombreMesa);
    }

    public Padron buscaPadronPorRecinto(String nombreRecinto) {
        return padronFacade.buscaPadronPorRecinto(nombreRecinto);
    }

    public List<Padron> getPadronsEnParroquias(List<Integer> listaIdParroquias) {
        return padronFacade.getPadronsEnParroquias(listaIdParroquias);
    }

    public Padron buscaPorPesonaPeriodoIglesia(Integer idIglesiaPersona, Integer idPeriodo) {
        return padronFacade.buscaPorPesonaPeriodoIglesia(idIglesiaPersona, idPeriodo);
    }

    public List<Integer> obtieneIglesiasEnPadronCompletasPorUbicacion(List<Integer> idParroquias) {
        return padronFacade.obtieneIglesiasEnPadronCompletasPorUbicacion(idParroquias);
    }

    public List<Padron> getPadronPorMesas(List<Mesa> listaMesas) {
        return padronFacade.getPadronPorMesas(listaMesas);
    }

    /**
     * Devuelve la lista de iglesias únicas presentes en los padrones dados.
     * Útil para construir la columna "iglesias asignadas" en pantallas de
     * gestión del padrón.
     */
    public List<Iglesia> obtenerIglesiasUnicasEnPadron(List<Padron> padrones) {
        List<Iglesia> resultado = new ArrayList<>();
        if (padrones == null || padrones.isEmpty()) {
            return resultado;
        }
        for (Padron padron : padrones) {
            if (padron.getIglesiaPersona() == null) {
                continue;
            }
            Iglesia iglesia = padron.getIglesiaPersona().getIglesia();
            if (iglesia != null && !resultado.contains(iglesia)) {
                resultado.add(iglesia);
            }
        }
        return resultado;
    }

    /**
     * Asigna todas las personas vinculadas a una iglesia al padrón de una mesa
     * para el período dado, evitando duplicados (no inserta si ya hay un
     * padrón para esa IglesiaPersona+Periodo). Atómico: si falla a media
     * iteración, hace rollback de la transacción EJB.
     *
     * @return los padrones nuevos creados (vacío si todas las personas ya
     *         estaban asignadas)
     */
    public List<Padron> asignarIglesiaAMesa(Iglesia iglesia, Mesa mesa, Periodo periodo) {
        List<Padron> creados = new ArrayList<>();
        if (iglesia == null || mesa == null || periodo == null) {
            return creados;
        }
        List<IglesiaPersona> personas = iglesiaPersonaFacade.getPersonasIglesiasPorIglesia(iglesia.getId());
        if (personas == null) {
            return creados;
        }
        for (IglesiaPersona ip : personas) {
            Padron existente = padronFacade.buscaPorPesonaPeriodoIglesia(ip.getId(), periodo.getId());
            if (existente == null) {
                creados.add(padronFacade.create(new Padron(mesa, periodo, ip)));
            }
        }
        return creados;
    }

    /**
     * Importa una fila de padrón electoral desde una fuente externa (ej.
     * Excel): asegura la existencia de la iglesia (busca por nombre/comunidad/
     * ubicación, o la crea), persiste la persona, crea la relación
     * iglesia-persona y el registro de padrón vinculado a la mesa. Atómico
     * por transacción EJB: una fila se persiste completa o no se persiste.
     *
     * @return el {@link Padron} persistido, o null si la entrada es insuficiente
     */
    public Padron importarFilaPadron(Persona persona, Iglesia iglesia, Mesa mesa) {
        if (persona == null || iglesia == null) {
            return null;
        }
        Iglesia existente = iglesiaFacade.getIglesiaPorNombreNombreComunidadYUbicacion(iglesia);
        Iglesia iglesiaResuelta = (existente != null) ? existente : iglesiaFacade.create(iglesia);

        Persona personaPersistida = personaFacade.create(persona);
        if (iglesiaResuelta == null || personaPersistida == null) {
            return null;
        }
        IglesiaPersona ip = iglesiaPersonaFacade.create(new IglesiaPersona(iglesiaResuelta, personaPersistida));
        if (ip == null) {
            return null;
        }
        Padron nuevo = new Padron();
        nuevo.setIglesiaPersona(ip);
        nuevo.setMesa(mesa);
        return padronFacade.create(nuevo);
    }

    /**
     * Versión por ids de {@link #asignarIglesiaAMesa(Iglesia, Mesa, Periodo)}.
     * Resuelve las entidades a partir de ids y delega al método principal.
     */
    public int asignarIglesiaAMesaPorIds(Integer iglesiaId, Integer mesaId, Integer periodoId) {
        if (iglesiaId == null || mesaId == null || periodoId == null) {
            return 0;
        }
        Iglesia iglesia = iglesiaFacade.find(iglesiaId);
        Mesa mesa = mesaFacade.find(mesaId);
        Periodo periodo = periodoFacade.find(periodoId);
        if (iglesia == null || mesa == null || periodo == null) {
            return 0;
        }
        List<Padron> creados = asignarIglesiaAMesa(iglesia, mesa, periodo);
        return creados.size();
    }

    // ----- API basada en DTO -----

    public PadronDTO obtenerDTOPorId(Integer id) {
        if (id == null) {
            return null;
        }
        return PadronDTO.fromEntity(padronFacade.find(id));
    }

    public List<PadronDTO> listarDTOs() {
        return mapearLista(padronFacade.findAll());
    }

    public List<PadronDTO> listarDTOsOrdenadoPorId() {
        return mapearLista(padronFacade.getAllOrderbyId());
    }

    public List<PadronDTO> listarDTOsPorMesas(List<Mesa> mesas) {
        return mapearLista(padronFacade.getPadronPorMesas(mesas));
    }

    /**
     * Versión basada en ids de mesa: resuelve internamente los Mesa stubs y
     * delega. Útil para controllers que no manejan entidades.
     */
    public List<PadronDTO> listarDTOsPorMesaIds(List<Integer> mesaIds) {
        if (mesaIds == null || mesaIds.isEmpty()) {
            return new ArrayList<>();
        }
        return mapearLista(padronFacade.getPadronPorMesaIds(mesaIds));
    }

    public List<PadronDTO> listarDTOsTodosOrdenados() {
        return mapearLista(padronFacade.getAllOrderbyId());
    }

    /**
     * Versión DTO de {@link #obtenerIglesiasUnicasEnPadron(List)}: dada una
     * lista de PadronDTO, extrae los IglesiaDTO únicos (preservando orden de
     * aparición).
     */
    public List<ec.com.antenasur.dto.IglesiaDTO> obtenerIglesiasUnicasEnPadronDTO(List<PadronDTO> padrones) {
        List<ec.com.antenasur.dto.IglesiaDTO> resultado = new ArrayList<>();
        if (padrones == null) {
            return resultado;
        }
        java.util.Set<Integer> idsVistos = new java.util.LinkedHashSet<>();
        for (PadronDTO p : padrones) {
            if (p.getIglesiaPersona() != null && p.getIglesiaPersona().getIglesia() != null
                    && p.getIglesiaPersona().getIglesia().getId() != null
                    && idsVistos.add(p.getIglesiaPersona().getIglesia().getId())) {
                resultado.add(p.getIglesiaPersona().getIglesia());
            }
        }
        return resultado;
    }

    /**
     * Persiste un padrón a partir del DTO. Resuelve mesa, periodo e
     * iglesiaPersona por sus ids embebidos. Si el id es null crea, si no
     * hidrata el padrón existente con los nuevos valores.
     */
    public PadronDTO guardarDesdeDTO(PadronDTO dto) {
        if (dto == null) {
            return null;
        }
        Mesa mesa = (dto.getMesa() != null && dto.getMesa().getId() != null)
                ? mesaFacade.find(dto.getMesa().getId()) : null;
        Periodo periodo = (dto.getPeriodoId() != null) ? periodoFacade.find(dto.getPeriodoId()) : null;
        IglesiaPersona ip = (dto.getIglesiaPersona() != null && dto.getIglesiaPersona().getId() != null)
                ? iglesiaPersonaFacade.find(dto.getIglesiaPersona().getId()) : null;

        if (dto.getId() == null) {
            Padron nuevo = new Padron(mesa, periodo, ip);
            nuevo.setSufrago(dto.getSufrago() != null ? dto.getSufrago() : false);
            return PadronDTO.fromEntity(padronFacade.create(nuevo));
        }
        Padron actual = padronFacade.find(dto.getId());
        if (actual == null) {
            return null;
        }
        actual.setMesa(mesa);
        actual.setPeriodo(periodo);
        actual.setIglesiaPersona(ip);
        actual.setSufrago(dto.getSufrago());
        return PadronDTO.fromEntity(padronFacade.edit(actual));
    }

    public PadronDTO eliminarPorId(Integer id) {
        if (id == null) {
            return null;
        }
        Padron p = padronFacade.find(id);
        if (p == null) {
            return null;
        }
        return PadronDTO.fromEntity(padronFacade.delete(p));
    }

    private List<PadronDTO> mapearLista(List<Padron> padrones) {
        List<PadronDTO> resultado = new ArrayList<>();
        if (padrones == null) {
            return resultado;
        }
        for (Padron p : padrones) {
            resultado.add(PadronDTO.fromEntity(p));
        }
        return resultado;
    }
}
