package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.EscrutinioCabeceraDTO;
import ec.com.antenasur.dto.EscrutinioDTO;
import ec.com.antenasur.enums.EstadoEscrutinio;
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
import ec.com.antenasur.service.AbstractService;

@Stateless
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
        if (EstadoEscrutinio.CERRADO.equals(cabecera.getEstadoEscrutinio())) {
            throw new NegocioException("El escrutinio ya se encuentra cerrado.");
        }
        int totalPapeletasUso = 0;
        for (Escrutinio item : actaItems) {
            validarItemEscrutinio(item);
            if (item.getId() != null) {
                escrutinioFacade.edit(item);
            } else {
                escrutinioFacade.create(item);
            }
            totalPapeletasUso += item.getTotalVotos();
        }
        int totalSufragantes = cabecera.getTotalSufragantes() != null ? cabecera.getTotalSufragantes() : 0;
        if (totalPapeletasUso > totalSufragantes) {
            throw new NegocioException("El total de votos registrados supera el total de sufragantes de la mesa.");
        }
        if (totalPapeletasUso != totalSufragantes) {
            throw new NegocioException("No se puede cerrar la mesa porque el total de votos no cuadra con los sufragantes asignados.");
        }
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
        if (EstadoEscrutinio.CERRADO.equals(cabecera.getEstadoEscrutinio())) {
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
        cabecera.setEstadoEscrutinio(EstadoEscrutinio.CONTEO_REGISTRADO);
        actualizarTotalesCabecera(cabecera, actaItems, totalSufragantes != null ? totalSufragantes : cabecera.getTotalSufragantes());
        return escrutinioCabeceraFacade.edit(cabecera);
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

    private void actualizarTotalesCabecera(EscrutinioCabecera cabecera, List<Escrutinio> items, Integer totalSufragantes) {
        int total = 0;
        int blancos = 0;
        int nulos = 0;
        for (Escrutinio item : items) {
            int votos = item.getTotalVotos() != null ? item.getTotalVotos() : 0;
            total += votos;
            String categoria = item.getCategoria() != null && item.getCategoria().getNombre() != null
                    ? item.getCategoria().getNombre().trim().toUpperCase() : "";
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
        int diferencia = cabecera.getTotalSufragantes() - total;
        cabecera.setObservacionConteo(diferencia == 0 ? "" : Math.abs(diferencia)
                + (diferencia > 0 ? " VOTOS FALTANTES" : " VOTOS EXCEDENTES"));
        if (diferencia != 0 && !EstadoEscrutinio.CERRADO.equals(cabecera.getEstadoEscrutinio())) {
            cabecera.setEstadoEscrutinio(EstadoEscrutinio.OBSERVADO);
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
