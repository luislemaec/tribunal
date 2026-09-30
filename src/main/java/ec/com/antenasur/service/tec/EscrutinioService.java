package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.EscrutinioCabeceraDTO;
import ec.com.antenasur.dto.EscrutinioDTO;
import ec.com.antenasur.dto.ResultadoCategoriaPublicaDTO;
import ec.com.antenasur.dto.ResultadoMesaPublicaDTO;
import ec.com.antenasur.enums.EstadoEscrutinio;
import ec.com.antenasur.enums.EstadoTarea;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.facade.tec.CategoriaVotoFacade;
import ec.com.antenasur.facade.tec.EscrutinioCabeceraFacade;
import ec.com.antenasur.facade.tec.EscrutinioFacade;
import ec.com.antenasur.facade.tec.MesaFacade;
import ec.com.antenasur.facade.tec.ProcesoElectoralFacade;
import ec.com.antenasur.model.tec.CategoriaVoto;
import ec.com.antenasur.model.tec.Escrutinio;
import ec.com.antenasur.model.tec.EscrutinioCabecera;
import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.model.tec.Recinto;
import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.service.AbstractService;

@Stateless
@jakarta.interceptor.Interceptors(ec.com.antenasur.security.qr.AlcanceEscrutinioQrInterceptor.class)
public class EscrutinioService extends AbstractService<Escrutinio, Integer, EscrutinioFacade> {

    @Inject
    private EscrutinioFacade escrutinioFacade;

    @Inject
    private EscrutinioCabeceraFacade escrutinioCabeceraFacade;

    @Inject
    private MesaFacade mesaFacade;

    @Inject
    private ProcesoElectoralFacade procesoElectoralFacade;

    @Inject
    private CategoriaVotoFacade categoriaVotoFacade;

    @Inject private DisponibilidadDocumentoMesaService disponibilidadDocumental;
    @Inject private DocumentoService documentoService;
    @Inject private ec.com.antenasur.facade.tec.TipoDocumentoFacade tipoDocumentoFacade;
    @Inject private AccesoDocumentoMesaService accesoDocumental;

    @Override
    protected EscrutinioFacade getFacade() {
        return escrutinioFacade;
    }

    public List<Escrutinio> buscaPorMesa(Mesa mesa) {
        return escrutinioFacade.buscaPorMesa(mesa);
    }

    public List<Escrutinio> buscaCanton(Mesa mesa) {
        return escrutinioFacade.buscaCanton(mesa);
    }

    /**
     * Devuelve el acta de escrutinios de la mesa: si ya existen registros, los
     * retorna; si no, construye una lista de Escrutinio "vacíos" — uno por
     * categoría de voto — listos para que el operador ingrese los totales.
     *
     * @param mesa mesa cuya acta se está abriendo (no null)
     * @param periodo período al que pertenecen los nuevos registros
     * @param categorias categorías de voto a usar para los placeholders
     * @return lista nunca null; vacía si {@code mesa} o {@code categorias} son null
     */
    public List<Escrutinio> prepararActaPorMesa(Mesa mesa, ProcesoElectoral proceso, List<CategoriaVoto> categorias) {
        if (mesa == null) {
            return new ArrayList<>();
        }
        List<Escrutinio> existentes = escrutinioFacade.buscaPorMesaYProceso(mesa, proceso);
        if (existentes != null && !existentes.isEmpty()) {
            return existentes;
        }
        List<Escrutinio> placeholders = new ArrayList<>();
        if (categorias != null) {
            for (CategoriaVoto categoria : categorias) {
                Escrutinio nuevo = new Escrutinio();
                nuevo.setMesa(mesa);
                nuevo.setProceso(proceso);
                nuevo.setCategoria(categoria);
                placeholders.add(nuevo);
            }
        }
        return placeholders;
    }

    /**
     * Guarda el acta completa de una mesa en una sola transaccion: persiste
     * el detalle por categoria y actualiza la cabecera normalizada del
     * escrutinio con estado, fecha de cierre, totales y observaciones.
     *
     * @param mesa mesa a cerrar (no null)
     * @param actaItems escrutinios a persistir (cada uno debe tener totalVotos)
     * @return la mesa persistida con sus campos calculados; null si los args
     *         son inválidos
     */
    public Mesa guardarActaCompleta(Mesa mesa, List<Escrutinio> actaItems) {
        if (mesa == null || actaItems == null || actaItems.isEmpty()) {
            return null;
        }
        ProcesoElectoral proceso = obtenerProcesoDesdeItems(actaItems);
        EscrutinioCabecera cabecera = obtenerOCrearCabecera(mesa, proceso);
        if (EstadoEscrutinio.PENDIENTE.equals(cabecera.getEstadoEscrutinio())) {
            throw new NegocioException("Debe registrar la apertura de la mesa antes de cerrar el acta.");
        }
        if (EstadoEscrutinio.CERRADO.equals(cabecera.getEstadoEscrutinio())
                || EstadoEscrutinio.ANULADO.equals(cabecera.getEstadoEscrutinio())) {
            throw new NegocioException("El escrutinio ya se encuentra cerrado.");
        }
        if (!EstadoEscrutinio.EN_CONTEO.equals(cabecera.getEstadoEscrutinio())
                && !EstadoEscrutinio.CONTEO_REGISTRADO.equals(cabecera.getEstadoEscrutinio())
                && !EstadoEscrutinio.REABIERTO.equals(cabecera.getEstadoEscrutinio())) {
            throw new NegocioException("Debe guardar el conteo antes de cerrar la mesa.");
        }
        for (Escrutinio item : actaItems) {
            validarItemEscrutinio(item);
            if (item.getId() != null) {
                escrutinioFacade.edit(item);
            } else {
                escrutinioFacade.create(item);
            }
        }
        int totalSufragantes = cabecera.getTotalSufragantes() != null ? cabecera.getTotalSufragantes() : 0;
        actualizarTotalesCabecera(cabecera, actaItems, totalSufragantes);
        cabecera.setEstadoEscrutinio(EstadoEscrutinio.CERRADO);
        cabecera.setFechaCierre(new Date());
        cabecera.setObservacionCierre("");
        escrutinioCabeceraFacade.edit(cabecera);
        return mesa;
    }

    public EscrutinioCabecera abrirMesa(Integer mesaId, Integer procesoId, String presidente, String observacion, Integer totalSufragantes) {
        if (mesaId == null) {
            throw new NegocioException("Debe seleccionar una mesa para registrar la apertura.");
        }
        Mesa mesa = mesaFacade.find(mesaId);
        ProcesoElectoral proceso = procesoId != null ? procesoElectoralFacade.find(procesoId) : null;
        if (mesa == null) {
            throw new NegocioException("No se pudo resolver la mesa seleccionada.");
        }
        // Una apertura ya registrada se informa como tal, aunque después cambie la junta o el padrón.
        EscrutinioCabecera existente = proceso != null
                ? escrutinioCabeceraFacade.buscarPorMesaProceso(mesaId, proceso.getId()) : null;
        if (existente == null || EstadoEscrutinio.PENDIENTE.equals(existente.getEstadoEscrutinio())) {
            // Se valida antes de crear la cabecera: una mesa no habilitada no deja registros.
            disponibilidadDocumental.validarApertura(procesoId, mesaId);
        }
        EscrutinioCabecera cabecera = obtenerOCrearCabecera(mesa, proceso);
        if (EstadoEscrutinio.CERRADO.equals(cabecera.getEstadoEscrutinio())) {
            throw new NegocioException("El escrutinio ya se encuentra cerrado.");
        }
        if (!EstadoEscrutinio.PENDIENTE.equals(cabecera.getEstadoEscrutinio())) {
            throw new NegocioException("La apertura de la mesa ya fue registrada.");
        }
        cabecera.setEstadoEscrutinio(EstadoEscrutinio.ABIERTO);
        cabecera.setFechaApertura(new Date());
        cabecera.setPresidenteResponsable(presidente);
        cabecera.setObservacionApertura(observacion);
        cabecera.setTotalSufragantes(totalSufragantes != null ? totalSufragantes : 0);
        return escrutinioCabeceraFacade.edit(cabecera);
    }

    public EscrutinioCabecera guardarBorradorConteo(Mesa mesa, List<Escrutinio> actaItems, Integer totalSufragantes) {
        if (mesa == null || actaItems == null || actaItems.isEmpty()) {
            return null;
        }
        ProcesoElectoral proceso = obtenerProcesoDesdeItems(actaItems);
        EscrutinioCabecera cabecera = obtenerOCrearCabecera(mesa, proceso);
        if (EstadoEscrutinio.PENDIENTE.equals(cabecera.getEstadoEscrutinio())) {
            throw new NegocioException("Debe registrar la apertura de la mesa antes de guardar el conteo.");
        }
        if (EstadoEscrutinio.CERRADO.equals(cabecera.getEstadoEscrutinio())
                || EstadoEscrutinio.ANULADO.equals(cabecera.getEstadoEscrutinio())) {
            throw new NegocioException("El escrutinio ya se encuentra cerrado. No se puede modificar el conteo.");
        }
        for (Escrutinio item : actaItems) {
            validarItemEscrutinio(item);
            if (item.getId() != null) {
                escrutinioFacade.edit(item);
            } else {
                escrutinioFacade.create(item);
            }
        }
        cabecera.setFechaInicioConteo(cabecera.getFechaInicioConteo() != null ? cabecera.getFechaInicioConteo() : new Date());
        int diferencia = actualizarTotalesCabecera(cabecera, actaItems,
                totalSufragantes != null ? totalSufragantes : cabecera.getTotalSufragantes());
        // Conteo registrado = cuadre de papeletas en cero (ver actualizarTotalesCabecera).
        cabecera.setEstadoEscrutinio(diferencia == 0
                ? EstadoEscrutinio.CONTEO_REGISTRADO : EstadoEscrutinio.EN_CONTEO);
        return escrutinioCabeceraFacade.edit(cabecera);
    }

    public EscrutinioCabeceraDTO cambiarEstadoCabeceraDTO(Integer mesaId, Integer procesoId,
            EstadoEscrutinio estadoNuevo, String motivo) {
        if (mesaId == null || procesoId == null || estadoNuevo == null) {
            throw new NegocioException("No se pudo resolver la mesa, proceso o estado requerido.");
        }
        EscrutinioCabecera cabecera = escrutinioCabeceraFacade.buscarPorMesaProceso(mesaId, procesoId);
        if (cabecera == null) {
            throw new NegocioException("No existe una cabecera de escrutinio para la mesa seleccionada.");
        }
        validarCambioEstado(cabecera, estadoNuevo, motivo);
        cabecera.setEstadoEscrutinio(estadoNuevo);
        if (EstadoEscrutinio.REABIERTO.equals(estadoNuevo)) {
            cabecera.setFechaCierre(null);
        }
        if (EstadoEscrutinio.ANULADO.equals(estadoNuevo)) {
            cabecera.setFechaCierre(new Date());
        }
        cabecera.setObservacionCierre(motivo);
        return EscrutinioCabeceraDTO.fromEntity(escrutinioCabeceraFacade.edit(cabecera));
    }

    // ----- API basada en DTO -----

    public EscrutinioDTO obtenerDTOPorId(Integer id) {
        if (id == null) {
            return null;
        }
        return EscrutinioDTO.fromEntity(escrutinioFacade.find(id));
    }

    public List<EscrutinioDTO> listarDTOsPorMesa(Integer mesaId) {
        if (mesaId == null) {
            return new ArrayList<>();
        }
        Mesa mesa = mesaFacade.find(mesaId);
        return mapearLista(escrutinioFacade.buscaPorMesa(mesa));
    }

    public List<EscrutinioDTO> listarDTOsPorMesaYProceso(Integer mesaId, Integer procesoId) {
        return mapearLista(escrutinioFacade.listarPorMesaProceso(mesaId, procesoId));
    }

    /**
     * Versión DTO de {@link #prepararActaPorMesa(Mesa, Periodo, List)}: dado
     * un id de mesa, id de periodo e ids de categorías, devuelve la lista de
     * Escrutinio (existentes o placeholders).
     */
    public List<EscrutinioDTO> prepararActaPorMesaDTO(Integer mesaId, Integer procesoId, List<Integer> categoriaIds) {
        List<EscrutinioDTO> resultado = new ArrayList<>();
        if (mesaId == null) {
            return resultado;
        }
        Mesa mesa = mesaFacade.find(mesaId);
        ProcesoElectoral proceso = (procesoId != null) ? procesoElectoralFacade.find(procesoId) : null;
        List<CategoriaVoto> categorias = new ArrayList<>();
        if (categoriaIds != null) {
            for (Integer cid : categoriaIds) {
                CategoriaVoto cat = categoriaVotoFacade.find(cid);
                if (cat != null) {
                    categorias.add(cat);
                }
            }
        }
        return mapearLista(prepararActaPorMesa(mesa, proceso, categorias));
    }

    /**
     * Versión DTO de {@link #guardarActaCompleta(Mesa, List)}: recibe el id
     * de la mesa y los DTOs de los items del acta. Reconstruye los
     * {@link Escrutinio} hidratando relaciones, ejecuta el cierre atómico, y
     * retorna el {@link MesaDTO} actualizado o null si la mesa no existe.
     *
     * <p>Usado desde el controller del acta sin tocar entidades.
     */
    public ec.com.antenasur.dto.MesaDTO guardarActaCompletaDTO(Integer mesaId, List<EscrutinioDTO> items) {
        if (mesaId == null || items == null || items.isEmpty()) {
            return null;
        }
        Mesa mesa = mesaFacade.find(mesaId);
        if (mesa == null) {
            return null;
        }
        List<Escrutinio> entidades = new ArrayList<>();
        for (EscrutinioDTO dto : items) {
            Escrutinio e;
            if (dto.getId() != null) {
                e = escrutinioFacade.find(dto.getId());
                if (e == null) {
                    continue;
                }
                e.setTotalVotos(dto.getTotalVotos());
            } else {
                e = new Escrutinio();
                e.setMesa(mesa);
                Integer procesoId = dto.getProcesoId() != null ? dto.getProcesoId() : dto.getPeriodoId();
                e.setProceso((procesoId != null) ? procesoElectoralFacade.find(procesoId) : null);
                e.setCategoria((dto.getCategoriaId() != null) ? categoriaVotoFacade.find(dto.getCategoriaId()) : null);
                e.setTotalVotos(dto.getTotalVotos());
            }
            entidades.add(e);
        }
        Mesa mesaCerrada = guardarActaCompleta(mesa, entidades);
        return ec.com.antenasur.dto.MesaDTO.fromEntity(mesaCerrada);
    }

    public ec.com.antenasur.dto.MesaDTO abrirMesaDTO(Integer mesaId, String observacion) {
        Mesa mesa = mesaId != null ? mesaFacade.find(mesaId) : null;
        Integer procesoId = null;
        if (mesa != null) {
            EscrutinioCabecera cabecera = escrutinioCabeceraFacade.buscarPorMesaProceso(mesaId, procesoId);
            if (cabecera != null && cabecera.getProceso() != null) {
                procesoId = cabecera.getProceso().getId();
            }
        }
        abrirMesa(mesaId, procesoId, null, observacion, mesa != null ? mesa.getTotalVotos() : 0);
        return ec.com.antenasur.dto.MesaDTO.fromEntity(mesa);
    }

    public EscrutinioCabeceraDTO abrirEscrutinioDTO(Integer mesaId, Integer procesoId, String presidente,
            String observacion, Integer totalSufragantes) {
        return EscrutinioCabeceraDTO.fromEntity(
                abrirMesa(mesaId, procesoId, presidente, observacion, totalSufragantes));
    }

    public ec.com.antenasur.dto.MesaDTO guardarBorradorConteoDTO(Integer mesaId, List<EscrutinioDTO> items) {
        if (mesaId == null || items == null || items.isEmpty()) {
            return null;
        }
        Mesa mesa = mesaFacade.find(mesaId);
        if (mesa == null) {
            return null;
        }
        List<Escrutinio> entidades = reconstruirEscrutinios(mesa, items);
        guardarBorradorConteo(mesa, entidades, mesa.getTotalVotos());
        return ec.com.antenasur.dto.MesaDTO.fromEntity(mesa);
    }

    public EscrutinioCabeceraDTO guardarBorradorConteoDTO(Integer mesaId, Integer procesoId,
            List<EscrutinioDTO> items, Integer totalSufragantes) {
        if (mesaId == null || items == null || items.isEmpty()) {
            return null;
        }
        Mesa mesa = mesaFacade.find(mesaId);
        if (mesa == null) {
            return null;
        }
        List<Escrutinio> entidades = reconstruirEscrutinios(mesa, items);
        return EscrutinioCabeceraDTO.fromEntity(guardarBorradorConteo(mesa, entidades, totalSufragantes));
    }

    public EscrutinioCabeceraDTO obtenerOCrearCabeceraDTO(Integer mesaId, Integer procesoId, Integer totalSufragantes) {
        if (mesaId == null || procesoId == null) {
            return null;
        }
        Mesa mesa = mesaFacade.find(mesaId);
        ProcesoElectoral proceso = procesoElectoralFacade.find(procesoId);
        EscrutinioCabecera cabecera = obtenerOCrearCabecera(mesa, proceso);
        if (cabecera.getTotalSufragantes() == null || cabecera.getTotalSufragantes() == 0) {
            cabecera.setTotalSufragantes(totalSufragantes != null ? totalSufragantes : 0);
            cabecera = escrutinioCabeceraFacade.edit(cabecera);
        }
        return EscrutinioCabeceraDTO.fromEntity(cabecera);
    }

    public EscrutinioCabeceraDTO buscarCabeceraDTO(Integer mesaId, Integer procesoId) {
        if (mesaId == null || procesoId == null) {
            return null;
        }
        return EscrutinioCabeceraDTO.fromEntity(
                escrutinioCabeceraFacade.buscarPorMesaProceso(mesaId, procesoId));
    }

    /**
     * Cabeceras de todas las mesas del proceso, indexadas por mesa, en una sola
     * consulta. Para listados que de otro modo consultarian mesa por mesa.
     */
    public java.util.Map<Integer, EscrutinioCabeceraDTO> buscarCabecerasDTOPorProceso(Integer procesoId) {
        java.util.Map<Integer, EscrutinioCabeceraDTO> resultado = new java.util.HashMap<>();
        if (procesoId == null) {
            return resultado;
        }
        escrutinioCabeceraFacade.buscarPorProcesoIndexadoPorMesa(procesoId)
                .forEach((mesaId, cabecera) -> resultado.put(mesaId, EscrutinioCabeceraDTO.fromEntity(cabecera)));
        return resultado;
    }

    /** Confirma los valores revisados contra el acta f\u00edsica, sin modificar la evidencia. */
    public EscrutinioCabeceraDTO validarResultadosFinalesDTO(Integer documentoId, Integer mesaId, Integer procesoId,
            ec.com.antenasur.dto.RevisionActaFinalDTO revision) {
        disponibilidadDocumental.validarFinal(procesoId, mesaId);
        documentoService.bloquearMesaParaVersion(mesaId);
        var evidencia = documentoService.obtenerEntidad(documentoId);
        if (evidencia == null || evidencia.getTipoDocumento() == null
                || !ActaFisicaEscrutinioService.TIPO_DOCUMENTO.equals(evidencia.getTipoDocumento().getNombre())
                || !Boolean.TRUE.equals(evidencia.getEstado()) || ActaFisicaEscrutinioService.VALIDADA.equals(evidencia.getEstadoRevision())
                || !"image/jpeg".equalsIgnoreCase(evidencia.getMime())
                || !ec.com.antenasur.util.RepositorioDocumentos.estaDisponible(evidencia.getPath())) {
            throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.regla.evidencia"));
        }
        if (revision == null || !revision.isRevisada() || revision.getResultados() == null || revision.getResultados().isEmpty()) {
            throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.regla.revisada"));
        }
        var items = revision.getResultados();
        String usuario = accesoDocumental.usuarioActual();
        Mesa mesa = mesaFacade.find(mesaId);
        if (mesa == null || mesa.getRecinto() == null)
            throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.error.seleccion"));
        documentoService.validarContextoMesa(evidencia, mesaId, procesoId, mesa.getRecinto().getId());
        var vigente = documentoService.buscarActivoPorMesaProcesoTipo(mesaId, procesoId, evidencia.getTipoDocumento().getId());
        if (vigente == null || !vigente.getId().equals(documentoId))
            throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.regla.evidencia"));
        EscrutinioCabecera cabecera = escrutinioCabeceraFacade.buscarPorMesaProceso(mesaId, procesoId);
        if (mesa == null || cabecera == null || !EstadoEscrutinio.CERRADO.equals(cabecera.getEstadoEscrutinio())) {
            throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.regla.actaFisica.cierre"));
        }
        List<Escrutinio> persistidos = escrutinioFacade.listarPorMesaProceso(mesaId, procesoId);
        java.util.Map<Integer, Escrutinio> porId = new java.util.HashMap<>();
        persistidos.forEach(e -> porId.put(e.getId(), e));
        java.util.Set<Integer> recibidos = new java.util.HashSet<>();
        java.util.Set<Integer> categorias = new java.util.HashSet<>();
        var comprobacion = new ec.com.antenasur.dto.RevisionActaFinalDTO();
        comprobacion.setValidosDeclarados(revision.getValidosDeclarados());
        comprobacion.setTotalDeclarado(revision.getTotalDeclarado());
        for (EscrutinioDTO dto : items) {
            Escrutinio item = dto == null ? null : porId.get(dto.getId());
            if (item == null || item.getCategoria() == null || !recibidos.add(item.getId())
                    || !categorias.add(item.getCategoria().getId()) || !item.getCategoria().getId().equals(dto.getCategoriaId())
                    || (dto.getProcesoId() != null && !procesoId.equals(dto.getProcesoId()))
                    || (dto.getMesa() != null && !mesaId.equals(dto.getMesa().getId()))) {
                throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.regla.resultados"));
            }
            EscrutinioDTO seguro = new EscrutinioDTO();
            seguro.setCategoriaId(item.getCategoria().getId());
            seguro.setCategoriaNombre(item.getCategoria().getNombre());
            seguro.setCategoriaTipo(item.getCategoria().getTipo());
            seguro.setTotalVotos(dto.getTotalVotos());
            comprobacion.getResultados().add(seguro);
        }
        var esperadas = categoriaVotoFacade.getCategoriasOrdenados(procesoId).stream().map(CategoriaVoto::getId).toList();
        if (recibidos.size() != persistidos.size() || !categorias.containsAll(esperadas))
            throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.regla.resultados"));
        if (!comprobacion.isCuadrada())
            throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.regla.cuadre"));
        // Todas las validaciones preceden a la primera mutacion de entidades administradas.
        for (EscrutinioDTO dto : items) {
            var item = porId.get(dto.getId());
            item.setTotalVotos(dto.getTotalVotos());
            item.setUsuarioActualiza(usuario);
        }
        cabecera.setTotalVotosValidos(Math.toIntExact(comprobacion.getValidos()));
        cabecera.setTotalVotosBlancos(Math.toIntExact(comprobacion.getBlancos()));
        cabecera.setTotalVotosNulos(Math.toIntExact(comprobacion.getNulos()));
        cabecera.setTotalVotosRegistrados(Math.toIntExact(comprobacion.getTotal()));
        cabecera.setUsuarioActualiza(usuario);
        escrutinioCabeceraFacade.edit(cabecera);
        mesa.setEstadoTarea(EstadoTarea.COMPLETADO);
        mesa.setUsuarioActualiza(usuario);
        mesaFacade.edit(mesa);
        evidencia.setEstadoRevision(ActaFisicaEscrutinioService.VALIDADA);
        evidencia.setObservacionRevision(null);
        evidencia.setUsuarioRevision(usuario);
        evidencia.setFechaRevision(new Date());
        evidencia.setUsuarioActualiza(usuario);
        documentoService.actualizar(evidencia);
        return EscrutinioCabeceraDTO.fromEntity(cabecera);
    }

    /**
     * Validación sin transcripción: el revisor declara que el acta física coincide con lo
     * registrado. Los valores no llegan de la vista: se leen de la base y se validan con
     * las mismas reglas de {@link #validarResultadosFinalesDTO} (mesa cerrada, evidencia
     * vigente, todas las categorías, listas, blancos y nulos presentes y sin vacíos).
     */
    public EscrutinioCabeceraDTO validarDatosRegistradosDTO(Integer documentoId, Integer mesaId, Integer procesoId) {
        if (mesaId == null || procesoId == null) {
            throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.error.seleccion"));
        }
        var revision = new ec.com.antenasur.dto.RevisionActaFinalDTO();
        for (Escrutinio registrado : escrutinioFacade.listarPorMesaProceso(mesaId, procesoId)) {
            EscrutinioDTO dto = new EscrutinioDTO();
            dto.setId(registrado.getId());
            if (registrado.getCategoria() != null) {
                dto.setCategoriaId(registrado.getCategoria().getId());
                dto.setCategoriaNombre(registrado.getCategoria().getNombre());
                dto.setCategoriaTipo(registrado.getCategoria().getTipo());
            }
            dto.setTotalVotos(registrado.getTotalVotos());
            revision.getResultados().add(dto);
        }
        // Lo declarado es lo registrado: el revisor dio fe de que el acta coincide.
        revision.setValidosDeclarados(Math.toIntExact(revision.getValidos()));
        revision.setTotalDeclarado(Math.toIntExact(revision.getTotal()));
        revision.setRevisada(true);
        return validarResultadosFinalesDTO(documentoId, mesaId, procesoId, revision);
    }

    public List<ResultadoCategoriaPublicaDTO> obtenerResultadosPublicosPorCategoria(Integer procesoId) {
        List<ResultadoCategoriaPublicaDTO> resultados = escrutinioFacade.obtenerResultadosPublicosPorCategoria(procesoId);
        resultados.removeIf(resultado -> !esCategoriaLista(resultado));
        long totalGeneral = 0L;
        for (ResultadoCategoriaPublicaDTO resultado : resultados) {
            totalGeneral += resultado.getTotalVotos() != null ? resultado.getTotalVotos() : 0L;
        }
        for (ResultadoCategoriaPublicaDTO resultado : resultados) {
            resultado.calcularPorcentaje(totalGeneral);
        }
        return resultados;
    }

    private boolean esCategoriaLista(ResultadoCategoriaPublicaDTO resultado) {
        if (resultado == null || resultado.getCategoria() == null) {
            return false;
        }
        String categoria = resultado.getCategoria().trim().toUpperCase(java.util.Locale.ROOT);
        return categoria.startsWith("LISTA")
                && !categoria.contains("BLANCO")
                && !categoria.contains("NULO")
                && !categoria.contains("PAPELETA")
                && !categoria.contains("PAPELTA")
                && !categoria.contains("RESTANTE");
    }

    public List<ResultadoMesaPublicaDTO> listarMesasCerradasPublicas(Integer procesoId) {
        List<ResultadoMesaPublicaDTO> resultado = new ArrayList<>();
        for (EscrutinioCabecera cabecera : escrutinioCabeceraFacade.listarCerradasPorProceso(procesoId)) {
            resultado.add(toResultadoMesaPublicaDTO(cabecera));
        }
        return resultado;
    }

    public long contarMesasCerradasPorProceso(Integer procesoId) {
        return escrutinioCabeceraFacade.contarCerradasPorProceso(procesoId);
    }

    public int calcularTotalVotos(List<EscrutinioDTO> items) {
        int total = 0;
        if (items == null) {
            return total;
        }
        for (EscrutinioDTO item : items) {
            total += item.getTotalVotos() != null ? item.getTotalVotos() : 0;
        }
        return total;
    }

    private List<Escrutinio> reconstruirEscrutinios(Mesa mesa, List<EscrutinioDTO> items) {
        List<Escrutinio> entidades = new ArrayList<>();
        for (EscrutinioDTO dto : items) {
            Escrutinio e;
            if (dto.getId() != null) {
                e = escrutinioFacade.find(dto.getId());
                if (e == null) {
                    continue;
                }
                e.setTotalVotos(dto.getTotalVotos());
            } else {
                e = new Escrutinio();
                e.setMesa(mesa);
                Integer procesoId = dto.getProcesoId() != null ? dto.getProcesoId() : dto.getPeriodoId();
                e.setProceso((procesoId != null) ? procesoElectoralFacade.find(procesoId) : null);
                e.setCategoria((dto.getCategoriaId() != null) ? categoriaVotoFacade.find(dto.getCategoriaId()) : null);
                e.setTotalVotos(dto.getTotalVotos());
            }
            entidades.add(e);
        }
        return entidades;
    }

    private void validarItemEscrutinio(Escrutinio item) {
        if (item == null || item.getCategoria() == null) {
            throw new NegocioException("Existen categorias de votos sin configurar correctamente.");
        }
        if (item.getTotalVotos() == null) {
            item.setTotalVotos(0);
        }
        if (item.getTotalVotos() < 0) {
            throw new NegocioException("Los votos no pueden ser negativos.");
        }
    }

    private EscrutinioCabecera obtenerOCrearCabecera(Mesa mesa, ProcesoElectoral proceso) {
        if (mesa == null || proceso == null) {
            throw new NegocioException("No se pudo resolver la mesa o el proceso electoral del escrutinio.");
        }
        EscrutinioCabecera existente = escrutinioCabeceraFacade.buscarPorMesaProceso(mesa.getId(), proceso.getId());
        if (existente != null) {
            return existente;
        }
        EscrutinioCabecera nuevo = new EscrutinioCabecera();
        nuevo.setMesa(mesa);
        nuevo.setProceso(proceso);
        nuevo.setEstadoEscrutinio(EstadoEscrutinio.PENDIENTE);
        nuevo.setTotalSufragantes(mesa.getTotalVotos() != null ? mesa.getTotalVotos() : 0);
        nuevo.setTotalVotosRegistrados(0);
        nuevo.setTotalVotosValidos(0);
        nuevo.setTotalVotosBlancos(0);
        nuevo.setTotalVotosNulos(0);
        return escrutinioCabeceraFacade.create(nuevo);
    }

    /**
     * Categoría de papeletas no utilizadas/restantes (se acepta la errata «PAPELTAS»).
     * Criterio único para separarlas de los votos emitidos en la cabecera, el cuadre y
     * la pantalla de escrutinio.
     */
    public static boolean esCategoriaPapeletas(String nombreCategoria) {
        String categoria = nombreCategoria != null ? nombreCategoria.trim().toUpperCase(java.util.Locale.ROOT) : "";
        return categoria.contains("PAPELETA") || categoria.contains("PAPELTA");
    }

    /**
     * Cuadre de papeletas de la mesa: sufragantes del padrón menos votos emitidos
     * (válidos, nulos y blancos) menos papeletas no utilizadas. Cero significa que todas
     * las papeletas están justificadas; la abstención queda en las papeletas no
     * utilizadas y no cuenta como descuadre.
     */
    public static int calcularCuadrePapeletas(int sufragantes, int votosEmitidos, int papeletasNoUtilizadas) {
        return sufragantes - votosEmitidos - papeletasNoUtilizadas;
    }

    /** Cuadre de papeletas de una mesa ya registrada (lo usa el dashboard del presidente). */
    public int calcularCuadrePapeletasMesa(Integer mesaId, Integer procesoId, int sufragantes) {
        int votosEmitidos = 0;
        int papeletas = 0;
        if (mesaId != null && procesoId != null) {
            for (Escrutinio item : escrutinioFacade.listarPorMesaProceso(mesaId, procesoId)) {
                int votos = item.getTotalVotos() != null ? item.getTotalVotos() : 0;
                if (esCategoriaPapeletas(item.getCategoria() != null ? item.getCategoria().getNombre() : null)) {
                    papeletas += votos;
                } else {
                    votosEmitidos += votos;
                }
            }
        }
        return calcularCuadrePapeletas(sufragantes, votosEmitidos, papeletas);
    }

    /**
     * Recalcula los totales de la cabecera (votos emitidos, sin papeletas no utilizadas)
     * y devuelve el cuadre de papeletas, que también deja anotado en la observación del
     * conteo cuando no es cero.
     */
    private int actualizarTotalesCabecera(EscrutinioCabecera cabecera, List<Escrutinio> items, Integer totalSufragantes) {
        int total = 0;
        int blancos = 0;
        int nulos = 0;
        int papeletas = 0;
        for (Escrutinio item : items) {
            int votos = item.getTotalVotos() != null ? item.getTotalVotos() : 0;
            String categoria = item.getCategoria() != null && item.getCategoria().getNombre() != null
                    ? item.getCategoria().getNombre().trim().toUpperCase() : "";
            if (esCategoriaPapeletas(categoria)) {
                papeletas += votos;
                continue;
            }
            total += votos;
            if (categoria.contains("BLANCO")) {
                blancos += votos;
            } else if (categoria.contains("NULO")) {
                nulos += votos;
            }
        }
        cabecera.setTotalSufragantes(totalSufragantes != null ? totalSufragantes : 0);
        cabecera.setTotalVotosRegistrados(total);
        cabecera.setTotalVotosBlancos(blancos);
        cabecera.setTotalVotosNulos(nulos);
        cabecera.setTotalVotosValidos(total - blancos - nulos);
        int diferencia = calcularCuadrePapeletas(cabecera.getTotalSufragantes(), total, papeletas);
        cabecera.setObservacionConteo(diferencia == 0 ? "" : Math.abs(diferencia)
                + (diferencia > 0 ? " PAPELETAS SIN JUSTIFICAR" : " PAPELETAS EXCEDENTES"));
        return diferencia;
    }

    private void validarCambioEstado(EscrutinioCabecera cabecera, EstadoEscrutinio estadoNuevo, String motivo) {
        EstadoEscrutinio estadoActual = cabecera.getEstadoEscrutinio();
        if (EstadoEscrutinio.OBSERVADO.equals(estadoNuevo)) {
            if (EstadoEscrutinio.CERRADO.equals(estadoActual) || EstadoEscrutinio.ANULADO.equals(estadoActual)) {
                throw new NegocioException("No se puede observar un escrutinio cerrado o anulado.");
            }
            return;
        }
        if (EstadoEscrutinio.ANULADO.equals(estadoNuevo)) {
            validarMotivo(motivo);
            if (EstadoEscrutinio.CERRADO.equals(estadoActual)) {
                throw new NegocioException("No se puede anular un escrutinio cerrado sin una reapertura autorizada.");
            }
            return;
        }
        if (EstadoEscrutinio.REABIERTO.equals(estadoNuevo)) {
            validarMotivo(motivo);
            if (!EstadoEscrutinio.CERRADO.equals(estadoActual)
                    && !EstadoEscrutinio.OBSERVADO.equals(estadoActual)
                    && !EstadoEscrutinio.ANULADO.equals(estadoActual)) {
                throw new NegocioException("Solo se puede reabrir un escrutinio cerrado, observado o anulado.");
            }
            // Una mesa con acta física VALIDADA es dato oficial: solo se reabre revirtiendo
            // antes la validación (ActaFisicaEscrutinioService#revertirValidacion).
            if (cabecera.getMesa() != null && cabecera.getProceso() != null
                    && tieneActaFisicaValidada(cabecera.getMesa().getId(), cabecera.getProceso().getId())) {
                throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("actaE.oficial.reabrir.bloqueado"));
            }
            return;
        }
        throw new NegocioException("El cambio de estado solicitado no esta permitido.");
    }

    /** El acta física vigente de la mesa en el proceso está VALIDADA (dato oficial). */
    private boolean tieneActaFisicaValidada(Integer mesaId, Integer procesoId) {
        var tipo = tipoDocumentoFacade.buscarActivoPorNombre(ActaFisicaEscrutinioService.TIPO_DOCUMENTO);
        if (tipo == null) {
            return false;
        }
        var vigente = documentoService.buscarActivoPorMesaProcesoTipo(mesaId, procesoId, tipo.getId());
        return vigente != null && ActaFisicaEscrutinioService.VALIDADA.equals(vigente.getEstadoRevision());
    }

    private void validarMotivo(String motivo) {
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new NegocioException("Debe ingresar un motivo para realizar esta accion.");
        }
    }

    private ProcesoElectoral obtenerProcesoDesdeItems(List<Escrutinio> items) {
        if (items != null) {
            for (Escrutinio item : items) {
                if (item != null && item.getProceso() != null) {
                    return item.getProceso();
                }
            }
        }
        return null;
    }

    private ResultadoMesaPublicaDTO toResultadoMesaPublicaDTO(EscrutinioCabecera cabecera) {
        ResultadoMesaPublicaDTO dto = new ResultadoMesaPublicaDTO();
        if (cabecera == null) {
            return dto;
        }
        Mesa mesa = cabecera.getMesa();
        Recinto recinto = mesa != null ? mesa.getRecinto() : null;
        Geograp parroquia = recinto != null ? recinto.getUbicacion() : null;
        Geograp canton = parroquia != null ? parroquia.getGeograp() : null;
        Geograp provincia = canton != null ? canton.getGeograp() : null;
        dto.setMesaId(mesa != null ? mesa.getId() : null);
        dto.setMesa(mesa != null ? mesa.getNombre() : "");
        dto.setRecinto(recinto != null ? recinto.getNombre() : "");
        dto.setParroquia(parroquia != null ? parroquia.getName() : "");
        dto.setCanton(canton != null ? canton.getName() : "");
        dto.setProvincia(provincia != null ? provincia.getName() : "");
        dto.setSufragantesAsignados(cabecera.getTotalSufragantes());
        dto.setVotosRegistrados(cabecera.getTotalVotosRegistrados());
        dto.setVotosValidos(cabecera.getTotalVotosValidos());
        dto.setVotosBlancos(cabecera.getTotalVotosBlancos());
        dto.setVotosNulos(cabecera.getTotalVotosNulos());
        dto.setFechaCierre(cabecera.getFechaCierre());
        return dto;
    }

    private List<EscrutinioDTO> mapearLista(List<Escrutinio> escrutinios) {
        List<EscrutinioDTO> resultado = new ArrayList<>();
        if (escrutinios == null) {
            return resultado;
        }
        for (Escrutinio e : escrutinios) {
            resultado.add(EscrutinioDTO.fromEntity(e));
        }
        return resultado;
    }
}
