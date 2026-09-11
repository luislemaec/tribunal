package ec.com.antenasur.service.tec;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import ec.com.antenasur.dto.EstadoJuntaDTO;

import ec.com.antenasur.dto.MiembroJRVDTO;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.facade.IglesiaPersonaFacade;
import ec.com.antenasur.facade.RolFacade;
import ec.com.antenasur.facade.tec.CatalogoGeneralFacade;
import ec.com.antenasur.facade.tec.MesaFacade;
import ec.com.antenasur.facade.tec.MiembroJRVFacade;
import ec.com.antenasur.facade.tec.PadronFacade;
import ec.com.antenasur.facade.tec.ProcesoElectoralFacade;
import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.Persona;
import ec.com.antenasur.model.Rol;
import ec.com.antenasur.model.Usuario;
import ec.com.antenasur.model.tec.CatalogoGeneral;
import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.model.tec.MiembroJRV;
import ec.com.antenasur.model.tec.Padron;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.service.AbstractService;
import ec.com.antenasur.service.UsuarioService;
import ec.com.antenasur.util.Constantes;

@Stateless
public class MiembroJRVService extends AbstractService<MiembroJRV, Integer, MiembroJRVFacade> {

    public static final List<String> DIGNIDADES_OBLIGATORIAS = List.of("PRESIDENTE", "SECRETARIO", "TESORERO", "VOCAL");

    @Inject
    private MiembroJRVFacade miembroJRVFacade;

    @Inject
    private IglesiaPersonaFacade iglesiaPersonaFacade;

    @Inject
    private MesaFacade mesaFacade;

    @Inject
    private ProcesoElectoralFacade procesoElectoralFacade;

    @Inject
    private CatalogoGeneralFacade catalogoGeneralFacade;

    @Inject
    private PadronFacade padronFacade;

    @Inject
    private UsuarioService usuarioService;

    @Inject
    private RolFacade rolFacade;

    @Override
    protected MiembroJRVFacade getFacade() {
        return miembroJRVFacade;
    }

    public Set<MiembroJRV> getJRVPorMesa(Mesa mesa) {
        return miembroJRVFacade.getJRVPorMesa(mesa);
    }

    public MiembroJRVDTO obtenerDTOPorId(Integer id) {
        if (id == null) return null;
        return MiembroJRVDTO.fromEntity(miembroJRVFacade.find(id));
    }

    public List<MiembroJRVDTO> listarDTOs() {
        List<MiembroJRVDTO> resultado = new ArrayList<>();
        List<MiembroJRV> ms = miembroJRVFacade.findAll();
        if (ms == null) return resultado;
        for (MiembroJRV m : ms) resultado.add(MiembroJRVDTO.fromEntity(m));
        return resultado;
    }

    public MiembroJRVDTO eliminarPorId(Integer id) {
        if (id == null) return null;
        MiembroJRV m = miembroJRVFacade.find(id);
        if (m == null) return null;
        Integer mesaId = m.getMesa() != null ? m.getMesa().getId() : null;
        Integer procesoId = m.getProceso() != null ? m.getProceso().getId() : null;
        if (juntaCompletadaRegistrada(mesaId, procesoId)) {
            throw new NegocioException("La junta ya fue completada. Para modificarla debe reabrirse mediante una accion autorizada.");
        }
        return MiembroJRVDTO.fromEntity(miembroJRVFacade.delete(m));
    }

    public List<MiembroJRVDTO> listarDTOsPorMesaProceso(Integer mesaId, Integer procesoId) {
        List<MiembroJRVDTO> resultado = new ArrayList<>();
        for (MiembroJRV miembro : miembroJRVFacade.listarPorMesaProceso(mesaId, procesoId)) {
            resultado.add(MiembroJRVDTO.fromEntity(miembro));
        }
        return resultado;
    }

    public MiembroJRVDTO designarMiembro(Integer iglesiaPersonaId, Integer mesaId, Integer procesoId, Integer cargoId) {
        validarParametros(iglesiaPersonaId, mesaId, procesoId, cargoId);
        if (padronFacade.contarIglesiasPorMesas(procesoId, List.of(mesaId)).getOrDefault(mesaId, 0L) == 0) {
            throw new NegocioException(Constantes.getMensaje("mjrv.mesas.sin.iglesias"));
        }
        if (juntaCompletadaRegistrada(mesaId, procesoId)) {
            throw new NegocioException("La junta ya fue completada. Para modificarla debe reabrirse mediante una accion autorizada.");
        }
        if (miembroJRVFacade.buscarPorIglesiaPersonaProceso(iglesiaPersonaId, procesoId) != null) {
            throw new NegocioException("La persona seleccionada ya pertenece a una Junta Receptora del Voto.");
        }
        if (miembroJRVFacade.buscarPorMesaCargoProceso(mesaId, cargoId, procesoId) != null) {
            throw new NegocioException("La dignidad seleccionada ya se encuentra asignada.");
        }
        if (!perteneceAPadronDeMesa(iglesiaPersonaId, mesaId, procesoId)) {
            throw new NegocioException("La persona seleccionada no pertenece a una iglesia asignada a esta mesa.");
        }

        IglesiaPersona iglesiaPersona = iglesiaPersonaFacade.find(iglesiaPersonaId);
        Mesa mesa = mesaFacade.find(mesaId);
        ProcesoElectoral proceso = procesoElectoralFacade.find(procesoId);
        CatalogoGeneral cargo = catalogoGeneralFacade.find(cargoId);
        if (iglesiaPersona == null || mesa == null || proceso == null || cargo == null) {
            throw new NegocioException("No se pudo resolver la informacion requerida para designar el miembro.");
        }
        MiembroJRV registroLogico = miembroJRVFacade.buscarPorMesaCargoProcesoIncluyeInactivos(
                mesaId, cargoId, procesoId);
        if (registroLogico != null && !Boolean.TRUE.equals(registroLogico.getEstado())) {
            registroLogico.setIglesiaPersona(iglesiaPersona);
            registroLogico.setMesa(mesa);
            registroLogico.setProceso(proceso);
            registroLogico.setCargo(cargo);
            registroLogico.setEstado(true);
            return MiembroJRVDTO.fromEntity(miembroJRVFacade.edit(registroLogico));
        }
        MiembroJRV nuevo = new MiembroJRV();
        nuevo.setIglesiaPersona(iglesiaPersona);
        nuevo.setMesa(mesa);
        nuevo.setProceso(proceso);
        nuevo.setCargo(cargo);
        return MiembroJRVDTO.fromEntity(miembroJRVFacade.create(nuevo));
    }

    public MiembroJRVDTO completarJunta(Integer mesaId, Integer procesoId) {
        if (mesaId == null || procesoId == null) {
            throw new NegocioException("Debe seleccionar una mesa y un proceso electoral activo.");
        }
        Mesa mesa = mesaFacade.find(mesaId);
        if (mesa == null) {
            throw new NegocioException("No se pudo resolver la mesa seleccionada.");
        }
        List<MiembroJRV> miembros = miembroJRVFacade.listarPorMesaProceso(mesaId, procesoId);
        validarJuntaCompleta(mesaId, procesoId);

        MiembroJRV presidente = obtenerMiembroPorDignidad(miembros, "PRESIDENTE");
        if (presidente == null || presidente.getIglesiaPersona() == null
                || presidente.getIglesiaPersona().getPersona() == null) {
            throw new NegocioException("No se pudo resolver la persona designada como Presidente de Mesa.");
        }
        Rol rolPresidenteMesa = obtenerRolPresidenteMesa();
        if (rolPresidenteMesa == null) {
            throw new NegocioException("El rol SITEC-Presidente-mesa no esta configurado en el modulo de roles.");
        }
        Persona personaPresidente = presidente.getIglesiaPersona().getPersona();
        Usuario usuario = usuarioService.asegurarUsuarioConRol(personaPresidente, rolPresidenteMesa);
        mesa.setResponsable(usuario.getUsername());
        mesaFacade.edit(mesa);
        return MiembroJRVDTO.fromEntity(presidente);
    }

    public MiembroJRVDTO obtenerDesignacionPorPersonaProceso(Integer personaId, Integer procesoId) {
        return MiembroJRVDTO.fromEntity(miembroJRVFacade.buscarPorPersonaProceso(personaId, procesoId));
    }

    public MiembroJRVDTO obtenerDesignacionPresidentePorPersonaProceso(Integer personaId, Integer procesoId) {
        return MiembroJRVDTO.fromEntity(miembroJRVFacade.buscarPresidentePorPersonaProceso(personaId, procesoId));
    }

    public Set<Integer> obtenerIglesiaPersonaIdsDesignadas(Integer procesoId) {
        if (procesoId == null) {
            return new HashSet<>();
        }
        return miembroJRVFacade.listarIglesiaPersonaIdsDesignadas(procesoId);
    }

    public boolean juntaCompletadaRegistrada(Integer mesaId, Integer procesoId) {
        if (mesaId == null || procesoId == null) {
            return false;
        }
        Mesa mesa = mesaFacade.find(mesaId);
        if (mesa == null || mesa.getResponsable() == null || mesa.getResponsable().isBlank()) {
            return false;
        }
        // El bloqueo de una junta completada no se levanta por cambios posteriores en el padron.
        Set<String> cargos = new HashSet<>();
        for (MiembroJRV miembro : miembroJRVFacade.listarPorMesaProceso(mesaId, procesoId)) {
            if (miembro.getCargo() != null) cargos.add(normalizarCargo(miembro.getCargo().getNombre()));
        }
        return cargos.containsAll(DIGNIDADES_OBLIGATORIAS);
    }

    private void validarParametros(Integer iglesiaPersonaId, Integer mesaId, Integer procesoId, Integer cargoId) {
        if (mesaId == null) {
            throw new NegocioException("Debe seleccionar una mesa antes de designar miembros.");
        }
        if (iglesiaPersonaId == null) {
            throw new NegocioException("Debe seleccionar una persona para designar.");
        }
        if (cargoId == null) {
            throw new NegocioException("Debe seleccionar una dignidad.");
        }
        if (procesoId == null) {
            throw new NegocioException("Debe seleccionar un proceso electoral.");
        }
    }

    private boolean perteneceAPadronDeMesa(Integer iglesiaPersonaId, Integer mesaId, Integer procesoId) {
        List<Integer> mesaIds = new ArrayList<>();
        mesaIds.add(mesaId);
        List<Padron> padrones = padronFacade.getPadronPorMesaIdsYProceso(mesaIds, procesoId);
        if (padrones == null) {
            return false;
        }
        for (Padron padron : padrones) {
            if (padron.getIglesiaPersona() != null
                    && iglesiaPersonaId.equals(padron.getIglesiaPersona().getId())) {
                return true;
            }
        }
        return false;
    }

    private void validarJuntaCompleta(Integer mesa, Integer proceso) {
        EstadoJuntaDTO estado = consultarEstadoJunta(mesa, proceso);
        if (!estado.isCompleta()) throw new NegocioException(Constantes.getMensaje("mjrv.validacion.conformacion")
                + (estado.getCargosFaltantes().isEmpty() ? "" : " " + Constantes.getMensaje(
                        "reportesMesa.regla.cargos", String.join(", ", estado.getCargosFaltantes()))));
    }

    public EstadoJuntaDTO consultarEstadoJunta(Integer mesa, Integer proceso) {
        if (mesa == null || proceso == null) return evaluarConformacion(List.of());
        return consultarEstadosJuntas(proceso, List.of(mesa)).get(mesa);
    }

    public Map<Integer, EstadoJuntaDTO> consultarEstadosJuntas(Integer proceso, List<Integer> mesas) {
        Map<Integer, List<Object[]>> porMesa = new HashMap<>();
        mesas.forEach(mesa -> porMesa.put(mesa, new ArrayList<>()));
        for (Object[] fila : miembroJRVFacade.consultarConformacion(proceso, mesas)) {
            porMesa.get((Integer) fila[0]).add(fila);
        }
        Map<Integer, EstadoJuntaDTO> estados = new HashMap<>();
        porMesa.forEach((mesa, miembros) -> estados.put(mesa, evaluarConformacion(miembros)));
        return estados;
    }

    static EstadoJuntaDTO evaluarConformacion(List<Object[]> miembros) {
        Map<String, Integer> cargos = new HashMap<>();
        Set<Integer> personas = new HashSet<>();
        boolean valida = true;
        for (Object[] miembro : miembros) {
            String cargo = normalizarCargo((String) miembro[1]);
            if (miembro[2] == null
                    || !personas.add((Integer) miembro[2]) || !DIGNIDADES_OBLIGATORIAS.contains(cargo)) valida = false;
            if (miembro[2] != null) cargos.merge(cargo, 1, Integer::sum);
        }
        List<String> faltantes = DIGNIDADES_OBLIGATORIAS.stream()
                .filter(cargo -> cargos.getOrDefault(cargo, 0) != 1).toList();
        return new EstadoJuntaDTO(valida && faltantes.isEmpty(), DIGNIDADES_OBLIGATORIAS.size() - faltantes.size(), faltantes);
    }

    public Map<Integer, ec.com.antenasur.dto.ResumenMesaJrvDTO> consultarResumenMesas(
            Integer proceso, List<Integer> mesas) {
        Map<Integer, ec.com.antenasur.dto.ResumenMesaJrvDTO> resultado = new HashMap<>();
        if (proceso == null || mesas == null || mesas.isEmpty()) return resultado;
        Map<Integer, List<Object[]>> miembros = new HashMap<>();
        mesas.forEach(id -> miembros.put(id, new ArrayList<>()));
        for (Object[] fila : miembroJRVFacade.consultarConformacion(proceso, mesas)) {
            miembros.get((Integer) fila[0]).add(fila);
        }
        Map<Integer, Long> iglesias = padronFacade.contarIglesiasPorMesas(proceso, mesas);
        miembros.forEach((id, filas) -> resultado.put(id, new ec.com.antenasur.dto.ResumenMesaJrvDTO(
                filas.size(), DIGNIDADES_OBLIGATORIAS.size(), iglesias.getOrDefault(id, 0L),
                evaluarConformacion(filas).isCompleta())));
        return resultado;
    }

    public static String normalizarCargo(String nombre) {
        return nombre == null ? "" : nombre.trim().toUpperCase(java.util.Locale.ROOT).replaceFirst(" DE MESA$", "");
    }

    private MiembroJRV obtenerMiembroPorDignidad(List<MiembroJRV> miembros, String dignidad) {
        if (miembros == null) {
            return null;
        }
        for (MiembroJRV miembro : miembros) {
            if (miembro != null && miembro.getCargo() != null
                    && contieneCargo(normalizar(miembro.getCargo().getNombre()), dignidad)) {
                return miembro;
            }
        }
        return null;
    }

    private Rol obtenerRolPresidenteMesa() {
        String nombreRol = Constantes.getRolPresidenteMesa();
        Rol rol = rolFacade.buscaPorNombre("SITEC-" + nombreRol);
        return rol != null ? rol : rolFacade.buscaPorNombre(nombreRol);
    }

    private static boolean contieneCargo(String cargo, String dignidad) {
        return dignidad.equals(normalizarCargo(cargo));
    }

    private static String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase();
    }
}
