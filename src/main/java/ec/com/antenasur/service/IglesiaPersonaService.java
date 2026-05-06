package ec.com.antenasur.service;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import ec.com.antenasur.dto.IglesiaPersonaDTO;
import ec.com.antenasur.facade.IglesiaFacade;
import ec.com.antenasur.facade.IglesiaPersonaFacade;
import ec.com.antenasur.facade.PersonaFacade;
import ec.com.antenasur.service.tec.CronogramaService;
import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.Persona;

@Stateless
public class IglesiaPersonaService extends AbstractService<IglesiaPersona, Integer, IglesiaPersonaFacade> {

    @Inject
    private IglesiaPersonaFacade iglesiaPersonaFacade;

    @Inject
    private PersonaFacade personaFacade;

    @Inject
    private IglesiaFacade iglesiaFacade;

    @Inject
    private CronogramaService cronogramaService;

    @Override
    protected IglesiaPersonaFacade getFacade() {
        return iglesiaPersonaFacade;
    }

    public List<IglesiaPersona> getPersonasIglesiasPorParroquia(Geograp parroquia) {
        return iglesiaPersonaFacade.getPersonasIglesiasPorParroquia(parroquia);
    }

    public List<IglesiaPersona> getPersonasIglesiasPorIglesia(int iglesiaId) {
        return iglesiaPersonaFacade.getPersonasIglesiasPorIglesia(iglesiaId);
    }

    public List<IglesiaPersona> getIglesiasPersonasPorParroquias(List<Geograp> parroquias) {
        return iglesiaPersonaFacade.getIglesiasPersonasPorParroquias(parroquias);
    }

    public IglesiaPersona buscarPorCedulaPersona(String cedula) {
        return iglesiaPersonaFacade.buscarPorCedulaPersona(cedula);
    }

    /**
     * Devuelve la {@link Iglesia} a la que pertenece la persona indicada
     * (vínculo activo más reciente), o {@code null} si la persona no está
     * en ninguna iglesia.
     */
    public Iglesia obtenerIglesiaDePersona(Integer personaId) {
        IglesiaPersona ip = iglesiaPersonaFacade.getVigentePorPersonaId(personaId);
        return (ip != null) ? ip.getIglesia() : null;
    }

    /**
     * Variante por DOCUMENTO de persona: robusta contra duplicados en
     * {@code tb_persona}. Cuando dos filas tienen el mismo documento pero
     * distintos ids, {@link #obtenerIglesiaDePersona(Integer)} puede fallar
     * (porque depende del id) — esta versión joinea por documento y evita
     * el falso "sin iglesia asignada".
     */
    public Iglesia obtenerIglesiaDePersonaPorDocumento(String documento) {
        IglesiaPersona ip = iglesiaPersonaFacade.getVigentePorDocumentoPersona(documento);
        return (ip != null) ? ip.getIglesia() : null;
    }

    /**
     * Persiste el binding iglesia-persona junto con la persona contenida.
     * Si la persona ya existe (id != null) hace edit; si no, hace create. Lo
     * mismo aplica al propio {@link IglesiaPersona}. Operación atómica por
     * transacción EJB.
     *
     * @return el IglesiaPersona persistido (con su Persona ligada y sus IDs
     *         actualizados), o null si la entrada es inválida
     */
    public IglesiaPersona guardarConPersona(IglesiaPersona iglesiaPersona) {
        if (iglesiaPersona == null || iglesiaPersona.getPersona() == null
                || iglesiaPersona.getIglesia() == null) {
            return null;
        }
        Persona persona = iglesiaPersona.getPersona();
        Persona personaPersistida = (persona.getId() != null)
                ? personaFacade.edit(persona)
                : personaFacade.create(persona);
        iglesiaPersona.setPersona(personaPersistida);
        return (iglesiaPersona.getId() != null)
                ? iglesiaPersonaFacade.edit(iglesiaPersona)
                : iglesiaPersonaFacade.create(iglesiaPersona);
    }

    // ----- API basada en DTO -----

    public IglesiaPersonaDTO obtenerDTOPorId(Integer id) {
        if (id == null) {
            return null;
        }
        return IglesiaPersonaDTO.fromEntity(iglesiaPersonaFacade.find(id));
    }

    public List<IglesiaPersonaDTO> listarDTOs() {
        return mapearLista(iglesiaPersonaFacade.findAll());
    }

    public List<IglesiaPersonaDTO> listarDTOsPorParroquias(List<Geograp> parroquias) {
        return mapearLista(iglesiaPersonaFacade.getIglesiasPersonasPorParroquias(parroquias));
    }

    public List<IglesiaPersonaDTO> listarDTOsPorIglesia(int iglesiaId) {
        return mapearLista(iglesiaPersonaFacade.getPersonasIglesiasPorIglesia(iglesiaId));
    }

    public IglesiaPersonaDTO buscarDTOPorCedula(String cedula) {
        if (cedula == null || cedula.isEmpty()) {
            return null;
        }
        return IglesiaPersonaDTO.fromEntity(iglesiaPersonaFacade.buscarPorCedulaPersona(cedula));
    }

    /**
     * Persiste el binding iglesia-persona a partir del DTO compuesto.
     * Resuelve la {@link Iglesia} y {@link Persona} contra BD usando los ids
     * del DTO; si la persona no tiene id, la crea con los datos del DTO.
     * Operación atómica por transacción EJB.
     */
    public IglesiaPersonaDTO guardarDesdeDTO(IglesiaPersonaDTO dto) {
        if (dto == null || dto.getIglesia() == null || dto.getPersona() == null) {
            return null;
        }
        // Validación dura por cronograma electoral: rechazamos saves si la
        // fase vigente no permite edición del padrón. UI también lo bloquea
        // pero esta capa protege contra requests fuera de flujo.
        if (!cronogramaService.permiteEdicionPadron()) {
            throw new IllegalStateException(
                    "La actualización del padrón está cerrada por el cronograma electoral.");
        }
        Iglesia iglesia = (dto.getIglesia().getId() != null)
                ? iglesiaFacade.find(dto.getIglesia().getId()) : null;
        if (iglesia == null) {
            return null;
        }

        Persona persona;
        if (dto.getPersona().getId() != null) {
            persona = personaFacade.find(dto.getPersona().getId());
            if (persona == null) {
                return null;
            }
            persona.setNombres(dto.getPersona().getNombres());
            persona.setApellidos(dto.getPersona().getApellidos());
            persona.setDocumento(dto.getPersona().getDocumento());
            persona.setTratamiento(dto.getPersona().getTratamiento());
            persona.setSexo(dto.getPersona().getSexo());
            persona = personaFacade.edit(persona);
        } else {
            persona = personaFacade.create(dto.getPersona().toEntity());
        }

        IglesiaPersona ip;
        if (dto.getId() != null) {
            ip = iglesiaPersonaFacade.find(dto.getId());
            if (ip == null) {
                return null;
            }
            ip.setIglesia(iglesia);
            ip.setPersona(persona);
            ip.setDesde(dto.getDesde());
            ip.setHasta(dto.getHasta());
            ip = iglesiaPersonaFacade.edit(ip);
        } else {
            IglesiaPersona nueva = new IglesiaPersona(iglesia, persona);
            nueva.setDesde(dto.getDesde());
            nueva.setHasta(dto.getHasta());
            ip = iglesiaPersonaFacade.create(nueva);
            // Marca como "actualizada" desde su creación: forzamos
            // fechaActualiza = fechaCrea + 3s para que la regla del DTO
            // (delta > 2s) los considere ya actualizados al venir de un alta
            // hecha por el IglesiaAdmin. Cualquier edit posterior la moverá
            // a now() y seguirá siendo > fechaCrea.
            if (ip != null && ip.getFechaCrea() != null) {
                ip.setFechaActualiza(new java.util.Date(ip.getFechaCrea().getTime() + 3000L));
                ip = iglesiaPersonaFacade.edit(ip);
            }
        }
        return IglesiaPersonaDTO.fromEntity(ip);
    }

    /**
     * Crea el vínculo {@link IglesiaPersona} entre la persona y la iglesia
     * indicadas si aún no existe. Idempotente: si ya hay un vínculo activo,
     * lo retorna sin tocar BD.
     *
     * @return par (vinculo, fueCreado) — {@code fueCreado=true} cuando se
     *         persistió un nuevo vínculo, {@code false} cuando ya existía.
     *         Devuelve {@code null} si los argumentos son inválidos o la
     *         iglesia/persona no existen.
     */
    public ResultadoVinculo crearVinculoSiNoExiste(Integer iglesiaId, Integer personaId) {
        if (iglesiaId == null || personaId == null) {
            return null;
        }
        IglesiaPersona existente = iglesiaPersonaFacade.findByIglesiaAndPersona(iglesiaId, personaId);
        if (existente != null) {
            return new ResultadoVinculo(existente, false);
        }
        Iglesia iglesia = iglesiaFacade.find(iglesiaId);
        Persona persona = personaFacade.find(personaId);
        if (iglesia == null || persona == null) {
            return null;
        }
        IglesiaPersona nuevo = new IglesiaPersona(iglesia, persona);
        nuevo.setDesde(new java.sql.Timestamp(System.currentTimeMillis()));
        IglesiaPersona creado = iglesiaPersonaFacade.create(nuevo);
        return new ResultadoVinculo(creado, true);
    }

    /** Resultado de {@link #crearVinculoSiNoExiste(Integer, Integer)}. */
    public static class ResultadoVinculo {
        private final IglesiaPersona vinculo;
        private final boolean creado;
        public ResultadoVinculo(IglesiaPersona vinculo, boolean creado) {
            this.vinculo = vinculo;
            this.creado = creado;
        }
        public IglesiaPersona getVinculo() { return vinculo; }
        public boolean fueCreado() { return creado; }
    }

    /**
     * Calcula el progreso de actualización de los miembros de una iglesia.
     * El array devuelto es: [total, actualizados, porcentaje].
     */
    public int[] calcularProgresoActualizacion(Integer iglesiaId) {
        int[] resultado = {0, 0, 0};
        if (iglesiaId == null) {
            return resultado;
        }
        List<IglesiaPersona> miembros = iglesiaPersonaFacade.getPersonasIglesiasPorIglesia(iglesiaId);
        if (miembros == null || miembros.isEmpty()) {
            return resultado;
        }
        int total = miembros.size();
        int actualizados = 0;
        for (IglesiaPersona ip : miembros) {
            if (ip.getFechaCrea() != null && ip.getFechaActualiza() != null
                    && ip.getFechaActualiza().getTime() - ip.getFechaCrea().getTime() > 2000L) {
                actualizados++;
            }
        }
        resultado[0] = total;
        resultado[1] = actualizados;
        resultado[2] = (int) Math.round((actualizados * 100.0) / total);
        return resultado;
    }

    /**
     * Devuelve los DTOs de miembros de una iglesia que ya fueron marcados
     * como actualizados (para incluir en el acta de actualización).
     */
    public List<IglesiaPersonaDTO> listarDTOsActualizadosPorIglesia(Integer iglesiaId) {
        List<IglesiaPersonaDTO> todos = listarDTOsPorIglesia(iglesiaId);
        List<IglesiaPersonaDTO> resultado = new ArrayList<>();
        for (IglesiaPersonaDTO dto : todos) {
            if (Boolean.TRUE.equals(dto.getActualizada())) {
                resultado.add(dto);
            }
        }
        return resultado;
    }

    public IglesiaPersonaDTO eliminarPorId(Integer id) {
        if (id == null) {
            return null;
        }
        IglesiaPersona ip = iglesiaPersonaFacade.find(id);
        if (ip == null) {
            return null;
        }
        return IglesiaPersonaDTO.fromEntity(iglesiaPersonaFacade.delete(ip));
    }

    /** Soft-delete batch por lista de ids. */
    public int eliminarPorIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Integer id : ids) {
            if (eliminarPorId(id) != null) {
                count++;
            }
        }
        return count;
    }

    private List<IglesiaPersonaDTO> mapearLista(List<IglesiaPersona> entidades) {
        List<IglesiaPersonaDTO> resultado = new ArrayList<>();
        if (entidades == null) {
            return resultado;
        }
        for (IglesiaPersona ip : entidades) {
            resultado.add(IglesiaPersonaDTO.fromEntity(ip));
        }
        return resultado;
    }
}
