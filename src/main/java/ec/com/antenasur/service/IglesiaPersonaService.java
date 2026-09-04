package ec.com.antenasur.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.IglesiaPersonaDTO;
import ec.com.antenasur.dto.ResumenMiembrosIglesiaDTO;
import ec.com.antenasur.exception.IglesiaPersonaException;
import ec.com.antenasur.facade.IglesiaFacade;
import ec.com.antenasur.facade.IglesiaPersonaFacade;
import ec.com.antenasur.facade.PersonaFacade;
import ec.com.antenasur.service.tec.CronogramaService;
import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.model.IglesiaPersona;
import ec.com.antenasur.model.Persona;
import ec.com.antenasur.util.Constantes;

@Stateless
@DeclareRoles({"SITEC-Administrador", "SITEC-Tribunal", "SITEC-IglesiaAdmin"})
@RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal", "SITEC-IglesiaAdmin"})
public class IglesiaPersonaService extends AbstractService<IglesiaPersona, Integer, IglesiaPersonaFacade> {

    private static final String ROL_TRIBUNAL = "SITEC-" + Constantes.getRolTribunal();
    private static final String ROL_ADMINISTRADOR = "SITEC-" + Constantes.getRolAdministrador();

    @Inject
    private IglesiaPersonaFacade iglesiaPersonaFacade;

    @Inject
    private PersonaFacade personaFacade;

    @Inject
    private IglesiaFacade iglesiaFacade;

    @Inject
    private CronogramaService cronogramaService;

    @Resource
    private SessionContext sessionContext;

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

    public Map<Integer, Integer> contarPersonasHabilitadasPadronPorIglesias(List<Integer> iglesiaIds) {
        return iglesiaPersonaFacade.contarPersonasHabilitadasPadronPorIglesias(iglesiaIds);
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
        String documento = normalizarDocumento(persona.getDocumento());
        validarDocumentoDisponibleParaPersona(documento, persona.getId());
        List<IglesiaPersona> activas = iglesiaPersonaFacade.listarActivasPorDocumento(documento, true);
        validarAsignacionNormal(activas, iglesiaPersona.getId(), iglesiaPersona.getIglesia().getId());
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

    /**
     * Resumen eficiente para el dashboard de la iglesia. Mantiene el acceso a
     * datos en el facade y evita cargar entidades solo para contar indicadores.
     */
    public ResumenMiembrosIglesiaDTO obtenerResumenMiembrosActivosPorIglesia(Integer iglesiaId) {
        return iglesiaPersonaFacade.obtenerResumenMiembrosActivosPorIglesia(iglesiaId);
    }

    public List<IglesiaPersonaDTO> listarDTOsActivosPorDocumento(String documento) {
        return mapearLista(iglesiaPersonaFacade.listarActivasPorDocumento(documento));
    }

    /**
     * Reporte controlado de personas con mas de una iglesia activa. La consulta
     * es agregada por documento y las relaciones se hidratan en bloque.
     */
    @RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal"})
    public List<IglesiaPersonaDTO> listarInconsistenciasIglesias() {
        if (!esCallerAdministradorOTribunal()) {
            throw new IglesiaPersonaException("form.personas.inconsistencias.error.permiso");
        }
        List<String> documentos = iglesiaPersonaFacade.listarDocumentosConMultiplesIglesiasActivas();
        if (documentos.isEmpty()) {
            return new ArrayList<>();
        }
        List<IglesiaPersona> relaciones = iglesiaPersonaFacade.listarRelacionesPorDocumentos(documentos);
        Map<String, Integer> cantidades = iglesiaPersonaFacade
                .contarIglesiasActivasPorDocumentos(documentos);
        Map<String, String> nombresActivos = construirNombresIglesiasActivas(relaciones);
        List<IglesiaPersonaDTO> resultado = mapearLista(relaciones, cantidades);
        for (IglesiaPersonaDTO dto : resultado) {
            String documento = documentoDe(dto);
            dto.setIglesiasActivas(nombresActivos.getOrDefault(documento, ""));
        }
        return resultado;
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

        String documento = normalizarDocumento(dto.getPersona().getDocumento());
        IglesiaPersona ip = dto.getId() != null ? iglesiaPersonaFacade.find(dto.getId()) : null;
        if (dto.getId() != null && ip == null) {
            return null;
        }
        Persona persona = resolverPersona(dto, ip, documento);
        if (persona == null) {
            return null;
        }
        validarDocumentoDisponibleParaPersona(documento, persona.getId());
        List<IglesiaPersona> activas = iglesiaPersonaFacade.listarActivasPorDocumento(documento, true);
        validarAsignacionNormal(activas, dto.getId(), iglesia.getId());
        actualizarPersona(persona, dto, documento);
        persona = persona.getId() != null ? personaFacade.edit(persona) : personaFacade.create(persona);

        if (ip != null) {
            ip.setIglesia(iglesia);
            ip.setPersona(persona);
            ip.setDesde(dto.getDesde());
            ip.setHasta(dto.getHasta());
            // Conserva el valor existente si el DTO llega con null (retrocompatibilidad)
            ip.setHabilitadoPadron(dto.getHabilitadoPadron() != null
                    ? dto.getHabilitadoPadron()
                    : (ip.getHabilitadoPadron() != null ? ip.getHabilitadoPadron() : Boolean.TRUE));
            // La revisión del miembro pertenece al vínculo iglesia-persona.
            // Si se editan solo datos de Persona, Hibernate no siempre marca
            // este vínculo como modificado; por eso se persiste explícitamente.
            ip.setFechaActualiza(new Date());
            ip = iglesiaPersonaFacade.edit(ip);
        } else {
            IglesiaPersona nueva = new IglesiaPersona(iglesia, persona);
            nueva.setDesde(dto.getDesde());
            nueva.setHasta(dto.getHasta());
            // Por defecto habilitado: el admin puede desmarcarlo explícitamente
            nueva.setHabilitadoPadron(dto.getHabilitadoPadron() != null
                    ? dto.getHabilitadoPadron() : Boolean.TRUE);
            ip = iglesiaPersonaFacade.create(nueva);
            // Marca como revisada desde su creación cuando el alta ocurre
            // desde esta pantalla administrativa.
            if (ip != null && ip.getFechaCrea() != null) {
                ip.setFechaActualiza(new Date());
                ip = iglesiaPersonaFacade.edit(ip);
            }
        }
        return IglesiaPersonaDTO.fromEntity(ip);
    }

    /**
     * Conserva una sola relacion activa para el documento indicado. El rol se
     * comprueba en el contexto Elytron del EJB y todas las bajas quedan dentro
     * de la misma transaccion y registradas por Envers.
     */
    @RolesAllowed("SITEC-Tribunal")
    public IglesiaPersonaDTO regularizarDesdeDTO(IglesiaPersonaDTO dto, Integer vinculoDefinitivoId) {
        if (!esCallerTribunal()) {
            throw new IglesiaPersonaException("form.personas.regularizacion.error.permiso");
        }
        if (dto == null || dto.getPersona() == null) {
            return null;
        }
        if (!cronogramaService.permiteEdicionPadron()) {
            throw new IglesiaPersonaException("form.personas.error.cronograma");
        }
        if (vinculoDefinitivoId == null) {
            throw new IglesiaPersonaException("form.personas.regularizacion.error.seleccion");
        }

        IglesiaPersona seleccionada = iglesiaPersonaFacade.find(vinculoDefinitivoId);
        if (seleccionada == null || seleccionada.getPersona() == null) {
            throw new IglesiaPersonaException("form.personas.regularizacion.error.seleccion.invalida");
        }
        String documentoActual = normalizarDocumento(seleccionada.getPersona().getDocumento());
        List<IglesiaPersona> activas = iglesiaPersonaFacade.listarActivasPorDocumento(documentoActual, true);
        if (contarIglesiasDistintas(activas) < 2) {
            throw new IglesiaPersonaException("form.personas.regularizacion.error.no.requerida");
        }

        IglesiaPersona definitiva = null;
        for (IglesiaPersona activa : activas) {
            if (vinculoDefinitivoId.equals(activa.getId())) {
                definitiva = activa;
                break;
            }
        }
        if (definitiva == null) {
            throw new IglesiaPersonaException("form.personas.regularizacion.error.seleccion.invalida");
        }

        Date ahora = new Date();
        Map<Integer, Persona> personasDescartadas = new LinkedHashMap<>();
        Integer personaDefinitivaId = definitiva.getPersona() != null ? definitiva.getPersona().getId() : null;
        for (IglesiaPersona activa : activas) {
            if (!activa.getId().equals(definitiva.getId())) {
                activa.setEstado(Boolean.FALSE);
                activa.setHasta(new java.sql.Timestamp(ahora.getTime()));
                activa.setHabilitadoPadron(Boolean.FALSE);
                iglesiaPersonaFacade.edit(activa);
                if (activa.getPersona() != null && activa.getPersona().getId() != null
                        && !activa.getPersona().getId().equals(personaDefinitivaId)) {
                    personasDescartadas.put(activa.getPersona().getId(), activa.getPersona());
                }
            }
        }
        // El trigger de unicidad debe observar primero las bajas. Mantiene la
        // auditoria Envers porque no se sustituye por un UPDATE masivo.
        iglesiaPersonaFacade.flushCambios();

        // Una persona duplicada sin vínculos activos deja de participar en
        // validaciones de cédula, pero se conserva con baja lógica y auditoría.
        Set<Integer> personasConRelacionActiva = iglesiaPersonaFacade
                .listarPersonasConRelacionesActivas(personasDescartadas.keySet());
        for (Map.Entry<Integer, Persona> descartada : personasDescartadas.entrySet()) {
            if (!personasConRelacionActiva.contains(descartada.getKey())) {
                descartada.getValue().setEstado(Boolean.FALSE);
                personaFacade.edit(descartada.getValue());
            }
        }

        Persona personaDefinitiva = definitiva.getPersona();
        String documentoSolicitado = normalizarDocumento(dto.getPersona().getDocumento());
        if (!documentoActual.equals(documentoSolicitado)) {
            validarDocumentoDisponibleParaPersona(documentoSolicitado, personaDefinitiva.getId());
        }
        actualizarPersona(personaDefinitiva, dto, documentoSolicitado);
        personaFacade.edit(personaDefinitiva);
        definitiva.setHasta(null);
        definitiva.setHabilitadoPadron(dto.getHabilitadoPadron() != null
                ? dto.getHabilitadoPadron() : definitiva.getHabilitadoPadron());
        definitiva.setFechaActualiza(ahora);
        return IglesiaPersonaDTO.fromEntity(iglesiaPersonaFacade.edit(definitiva));
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
        Iglesia iglesia = iglesiaFacade.find(iglesiaId);
        Persona persona = personaFacade.find(personaId);
        if (iglesia == null || persona == null) {
            return null;
        }
        String documento = normalizarDocumento(persona.getDocumento());
        validarDocumentoDisponibleParaPersona(documento, persona.getId());
        List<IglesiaPersona> activas = iglesiaPersonaFacade.listarActivasPorDocumento(documento, true);
        if (contarIglesiasDistintas(activas) > 1) {
            throw new IglesiaPersonaException("form.personas.error.varias.iglesias");
        }
        if (!activas.isEmpty()) {
            IglesiaPersona existente = activas.get(0);
            if (existente.getIglesia() != null && iglesiaId.equals(existente.getIglesia().getId())) {
                return new ResultadoVinculo(existente, false);
            }
            throw conflictoOtraIglesia(existente);
        }
        IglesiaPersona nuevo = new IglesiaPersona(iglesia, persona);
        nuevo.setDesde(new java.sql.Timestamp(System.currentTimeMillis()));
        IglesiaPersona creado = iglesiaPersonaFacade.create(nuevo);
        return new ResultadoVinculo(creado, true);
    }

    private void validarAsignacionNormal(List<IglesiaPersona> activas,
            Integer vinculoActualId, Integer iglesiaDestinoId) {
        if (contarIglesiasDistintas(activas) > 1) {
            throw new IglesiaPersonaException("form.personas.error.varias.iglesias");
        }
        if (activas.isEmpty()) {
            return;
        }
        IglesiaPersona activa = activas.get(0);
        if (vinculoActualId == null || !vinculoActualId.equals(activa.getId())) {
            throw conflictoOtraIglesia(activa);
        }
        if (activa.getIglesia() == null || !iglesiaDestinoId.equals(activa.getIglesia().getId())) {
            throw conflictoOtraIglesia(activa);
        }
    }

    private IglesiaPersonaException conflictoOtraIglesia(IglesiaPersona activa) {
        String iglesia = activa != null && activa.getIglesia() != null
                ? activa.getIglesia().getNombre() : "";
        return new IglesiaPersonaException("form.personas.error.otra.iglesia", iglesia);
    }

    private Persona resolverPersona(IglesiaPersonaDTO dto, IglesiaPersona ip, String documento) {
        if (ip != null && ip.getPersona() != null) {
            return ip.getPersona();
        }
        if (dto.getPersona().getId() != null) {
            return personaFacade.find(dto.getPersona().getId());
        }
        Persona existente = personaFacade.finByPersonaDocument(documento);
        return existente != null ? existente : dto.getPersona().toEntity();
    }

    private void actualizarPersona(Persona persona, IglesiaPersonaDTO dto, String documento) {
        persona.setNombres(dto.getPersona().getNombres());
        persona.setApellidos(dto.getPersona().getApellidos());
        persona.setDocumento(documento);
        persona.setTratamiento(dto.getPersona().getTratamiento());
        persona.setSexo(dto.getPersona().getSexo());
    }

    private String normalizarDocumento(String documento) {
        if (documento == null || documento.trim().isEmpty()) {
            throw new IglesiaPersonaException("form.personas.error.documento.requerido");
        }
        return documento.trim();
    }

    private void validarDocumentoDisponibleParaPersona(String documento, Integer personaId) {
        if (esDocumentoHistoricoEspecial(documento)) {
            return;
        }
        if (personaFacade.existeOtraPersonaActivaConDocumento(documento, personaId)) {
            throw new IglesiaPersonaException("form.personas.error.documento.duplicado");
        }
    }

    private boolean esDocumentoHistoricoEspecial(String documento) {
        if (documento == null) {
            return false;
        }
        String normalizado = documento.trim().toUpperCase(java.util.Locale.ROOT);
        return "S/N".equals(normalizado) || normalizado.matches("SN-\\d+");
    }

    private boolean esCallerTribunal() {
        return sessionContext != null && sessionContext.isCallerInRole(ROL_TRIBUNAL);
    }

    private boolean esCallerAdministradorOTribunal() {
        return sessionContext != null
                && (sessionContext.isCallerInRole(ROL_ADMINISTRADOR)
                    || sessionContext.isCallerInRole(ROL_TRIBUNAL));
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
            if (esRevisionCompleta(ip)) {
                actualizados++;
            }
        }
        resultado[0] = total;
        resultado[1] = actualizados;
        resultado[2] = (int) Math.round((actualizados * 100.0) / total);
        return resultado;
    }

    private boolean esRevisionCompleta(IglesiaPersona ip) {
        if (ip == null || ip.getFechaActualiza() == null) {
            return false;
        }
        return ip.getFechaCrea() == null || !ip.getFechaActualiza().before(ip.getFechaCrea());
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
        String documento = ip.getPersona() != null ? ip.getPersona().getDocumento() : null;
        if (documento != null && contarIglesiasDistintas(
                iglesiaPersonaFacade.listarActivasPorDocumento(documento, true)) > 1) {
            throw new IglesiaPersonaException("form.personas.error.varias.iglesias");
        }
        Persona persona = ip.getPersona();
        IglesiaPersona eliminada = iglesiaPersonaFacade.delete(ip);
        if (persona != null && persona.getId() != null) {
            Set<Integer> personasConRelacionActiva = iglesiaPersonaFacade
                    .listarPersonasConRelacionesActivas(java.util.Collections.singleton(persona.getId()));
            if (!personasConRelacionActiva.contains(persona.getId())) {
                persona.setEstado(Boolean.FALSE);
                personaFacade.edit(persona);
            }
        }
        return IglesiaPersonaDTO.fromEntity(eliminada);
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
        Set<String> documentos = new LinkedHashSet<>();
        if (entidades != null) {
            for (IglesiaPersona entidad : entidades) {
                if (entidad.getPersona() != null && entidad.getPersona().getDocumento() != null) {
                    documentos.add(entidad.getPersona().getDocumento().trim());
                }
            }
        }
        return mapearLista(entidades,
                iglesiaPersonaFacade.contarIglesiasActivasPorDocumentos(documentos));
    }

    private List<IglesiaPersonaDTO> mapearLista(List<IglesiaPersona> entidades,
            Map<String, Integer> cantidades) {
        Set<String> documentos = new LinkedHashSet<>();
        if (entidades != null) {
            for (IglesiaPersona entidad : entidades) {
                if (entidad.getPersona() != null && !esDocumentoHistoricoEspecial(entidad.getPersona().getDocumento())) {
                    String documento = entidad.getPersona().getDocumento();
                    if (documento != null && !documento.trim().isEmpty()) {
                        documentos.add(documento.trim());
                    }
                }
            }
        }
        Map<String, Integer> cantidadesCedula = personaFacade
                .contarPersonasActivasPorDocumentos(documentos);
        List<IglesiaPersonaDTO> resultado = new ArrayList<>();
        if (entidades == null) {
            return resultado;
        }
        for (IglesiaPersona entidad : entidades) {
            IglesiaPersonaDTO dto = IglesiaPersonaDTO.fromEntity(entidad);
            String documento = documentoDe(dto);
            int cantidad = cantidades.getOrDefault(documento, 0);
            dto.setCantidadIglesiasActivas(cantidad);
            dto.setInconsistenciaIglesias(cantidad > 1);
            int cantidadCedula = cantidadesCedula.getOrDefault(documento, 0);
            dto.setCantidadCedulaDuplicada(cantidadCedula);
            dto.setInconsistenciaCedula(cantidadCedula > 1);
            resultado.add(dto);
        }
        return resultado;
    }

    private Map<String, String> construirNombresIglesiasActivas(List<IglesiaPersona> relaciones) {
        Map<String, Set<String>> nombres = new LinkedHashMap<>();
        for (IglesiaPersona relacion : relaciones) {
            if (!Boolean.TRUE.equals(relacion.getEstado()) || relacion.getPersona() == null
                    || relacion.getIglesia() == null) {
                continue;
            }
            String documento = relacion.getPersona().getDocumento() != null
                    ? relacion.getPersona().getDocumento().trim() : "";
            nombres.computeIfAbsent(documento, key -> new LinkedHashSet<>())
                    .add(relacion.getIglesia().getNombre());
        }
        return nombres.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> String.join(", ", entry.getValue()),
                (primero, segundo) -> primero,
                LinkedHashMap::new));
    }

    private int contarIglesiasDistintas(Collection<IglesiaPersona> relaciones) {
        if (relaciones == null) {
            return 0;
        }
        return (int) relaciones.stream()
                .filter(relacion -> relacion.getIglesia() != null
                        && relacion.getIglesia().getId() != null)
                .map(relacion -> relacion.getIglesia().getId())
                .distinct()
                .count();
    }

    private String documentoDe(IglesiaPersonaDTO dto) {
        return dto != null && dto.getPersona() != null && dto.getPersona().getDocumento() != null
                ? dto.getPersona().getDocumento().trim() : "";
    }
}
