package ec.com.antenasur.controller;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;
import org.primefaces.model.StreamedContent;
import ec.com.antenasur.dto.FiltroPadronDTO;
import ec.com.antenasur.dto.FilaPadronDTO;
import ec.com.antenasur.dto.MesaPadronDTO;
import ec.com.antenasur.dto.OpcionPadronDTO;
import ec.com.antenasur.dto.ProcesoElectoralDTO;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.service.tec.GestionPadronService;
import ec.com.antenasur.service.tec.ProcesoElectoralService;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Named
@ViewScoped
@Slf4j
public class GestionPadronController implements Serializable {
    private static final String PROVINCIA_OPERATIVA = "CHIMBORAZO";

    @Inject
    private GestionPadronService service;
    @Inject
    private ProcesoElectoralService procesoService;
    @Getter
    private FiltroPadronDTO filtro = new FiltroPadronDTO();
    private FiltroPadronDTO consultaMesas = new FiltroPadronDTO();
    @Getter
    @Setter
    private String nombreIglesiaReporte;
    @Getter
    private List<ec.com.antenasur.dto.IglesiaPadronDTO> iglesiasDisponibles = new ArrayList<>(),
            iglesiasAsignadas = new ArrayList<>();
    @Getter
    private ProcesoElectoralDTO procesoActivo;
    /**
     * Avance global del padrón del proceso vigente. No depende de los filtros de la
     * pantalla: se calcula al abrir y solo se refresca cuando el padrón cambia, es
     * decir al asignar o retirar una iglesia.
     */
    @Getter
    private ec.com.antenasur.dto.AvancePadronDTO avance = new ec.com.antenasur.dto.AvancePadronDTO();
    private List<OpcionPadronDTO> provincias = List.of();
    @Getter
    private List<OpcionPadronDTO> cantones = List.of(), parroquias = List.of(),
            recintos = List.of(), mesas = List.of(), iglesias = List.of();
    @Getter
    @Setter
    private List<FilaPadronDTO> asignar = new ArrayList<>(), retirar = new ArrayList<>();
    @Getter
    @Setter
    private MesaPadronDTO mesaSeleccionada;
    @Getter
    @Setter
    private Integer recintoConsultaId;
    @Getter
    private LazyDataModel<FilaPadronDTO> disponibles, inscritos;
    @Getter
    private LazyDataModel<MesaPadronDTO> mesasLazy;
    @Getter
    private long total, mesasCon, mesasSin;
    @Getter
    private boolean consultado;
    @Getter
    @Setter
    private int primeraDisponible, primeraInscrita, primeraMesa;
    @Getter
    @Setter
    private int tabActivo;

    @PostConstruct
    public void init() {
        procesoActivo = procesoService.getActivoDTO();
        if (procesoActivo != null) {
            filtro.setProcesoId(procesoActivo.getId());
        } else {
            JsfUtil.addWarningMessageFromBundle("gestionPadron.error.proceso.sin.activo");
        }
        recalcularAvance();
        provincias = service.geografia(null);
        inicializarGeografiaOperativa();
        recargarRecintos();
        actualizarConsulta();
        disponibles = new FilasLazy(true);
        inscritos = new FilasLazy(false);
        mesasLazy = new MesasLazy();
    }

    /** Dos consultas agregadas; solo se invoca al abrir la vista y tras cambiar el padrón. */
    private void recalcularAvance() {
        avance = service.avance(procesoActivo == null ? null : procesoActivo.getId());
    }

    private void inicializarGeografiaOperativa() {
        provincias.stream()
                .filter(provincia -> PROVINCIA_OPERATIVA
                        .equals((provincia.getNombre() == null ? "" : provincia.getNombre())
                                .trim().toUpperCase(Locale.ROOT)))
                .findFirst()
                .ifPresent(provincia -> {
                    filtro.setProvinciaId(provincia.getId());
                    cantones = service.geografia(provincia.getId());
                });
    }

    public void cambiarProvincia() {
        filtro.setCantonId(null);
        filtro.setParroquiaId(null);
        parroquias = List.of();
        cantones = filtro.getProvinciaId() == null ? List.of() : service.geografia(filtro.getProvinciaId());
        recargarRecintos();
        actualizarConsulta();
    }

    public void cambiarCanton() {
        filtro.setParroquiaId(null);
        parroquias = filtro.getCantonId() == null ? List.of() : service.geografia(filtro.getCantonId());
        recargarRecintos();
        actualizarConsulta();
    }

    public void cambiarParroquia() {
        recargarRecintos();
        actualizarConsulta();
    }

    public void limpiarFiltros() {
        recintoConsultaId = null;
        filtro.setCantonId(null);
        filtro.setParroquiaId(null);
        filtro.setRecintoId(null);
        filtro.setMesaId(null);
        filtro.setIglesiaId(null);
        filtro.setBusqueda(null);
        filtro.setIglesiaNombre(null);
        filtro.setConPadron(null);
        parroquias = List.of();
        cantones = filtro.getProvinciaId() == null ? List.of() : service.geografia(filtro.getProvinciaId());
        limpiarMesa();
        recargarRecintos();
        actualizarConsulta();
    }

    /**
     * Los filtros territoriales son opcionales: sin ellos se muestran todos los
     * recintos activos. Si un filtro excluye el recinto seleccionado, se libera
     * tambien la mesa y el contexto dependiente para no operar con datos obsoletos.
     */
    private void recargarRecintos() {
        FiltroPadronDTO territorio = new FiltroPadronDTO(filtro);
        territorio.setRecintoId(null);
        territorio.setMesaId(null);
        recintos = service.recintos(territorio);
        if (recintoConsultaId != null && recintos.stream().noneMatch(r -> r.getId().equals(recintoConsultaId))) {
            recintoConsultaId = null;
        }
        if (filtro.getRecintoId() != null
                && recintos.stream().noneMatch(r -> r.getId().equals(filtro.getRecintoId()))) {
            limpiarMesa();
            filtro.setRecintoId(recintoConsultaId);
        }
    }

    private void actualizarConsulta() {
        consultado = filtro.getProcesoId() != null;
        consultaMesas = new FiltroPadronDTO(filtro);
        consultaMesas.setMesaId(null);
        consultaMesas.setRecintoId(recintoConsultaId);
        primeraMesa = 0;
        actualizarResumen();
    }

    private void limpiarMesa() {
        iglesiasDisponibles = new ArrayList<>();
        iglesiasAsignadas = new ArrayList<>();
        filtro.setMesaId(null);
        filtro.setIglesiaId(null);
        mesas = List.of();
        iglesias = List.of();
        mesaSeleccionada = null;
        limpiarSeleccion();
    }

    private void limpiarSeleccion() {
        asignar = new ArrayList<>();
        retirar = new ArrayList<>();
        primeraDisponible = primeraInscrita = primeraMesa = 0;
    }

    public void buscar() {
        limpiarSeleccion();
        recargarRecintos();
        actualizarConsulta();
    }

    public void cambiarRecinto() {
        limpiarMesa();
        filtro.setRecintoId(recintoConsultaId);
        if (filtro.getRecintoId() != null)
            mesas = service.mesas(filtro.getRecintoId());
        consultado = filtro.getProcesoId() != null;
        consultaMesas = new FiltroPadronDTO(filtro);
        actualizarResumen();
    }

    public void cambiarMesa() {
        filtro.setIglesiaId(null);
        limpiarSeleccion();
        iglesias = filtro.getMesaId() == null || filtro.getProcesoId() == null
                ? List.of()
                : service.iglesias(filtro.getMesaId(), filtro.getProcesoId());
        consultaMesas = new FiltroPadronDTO(filtro);
        actualizarResumen();
        cargarIglesias();
    }

    private void cargarIglesias() {
        iglesiasDisponibles = new ArrayList<>(
                service.resumenIglesias(filtro.getMesaId(), filtro.getProcesoId(), false));
        iglesiasAsignadas = new ArrayList<>(service.resumenIglesias(filtro.getMesaId(), filtro.getProcesoId(), true));
    }

    public void asignarIglesia(Integer id) {
        filtro.setIglesiaId(id);
        try {
            asignarSeleccionados();
        } finally {
            filtro.setIglesiaId(null);
            cargarIglesias();
        }
    }

    public void retirarIglesia(Integer id) {
        filtro.setIglesiaId(id);
        try {
            retirarSeleccionados();
        } finally {
            filtro.setIglesiaId(null);
            cargarIglesias();
        }
    }

    public String getNombreMesa() {
        return mesas.stream().filter(m -> m.getId().equals(filtro.getMesaId())).map(OpcionPadronDTO::getNombre)
                .findFirst().orElse("");
    }

    public long getTotalMiembrosMesa() {
        return iglesiasAsignadas.stream().mapToLong(iglesia -> iglesia.getTotal() == null ? 0 : iglesia.getTotal())
                .sum();
    }

    public void cambiarIglesia() {
        limpiarSeleccion();
        actualizarResumen();
    }

    public void seleccionarMesa(SelectEvent<MesaPadronDTO> event) {
        FiltroPadronDTO consultaAnterior = consultaMesas;
        int paginaAnterior = primeraMesa;
        mesaSeleccionada = event.getObject();
        filtro.setRecintoId(mesaSeleccionada.getRecintoId());
        mesas = service.mesas(filtro.getRecintoId());
        filtro.setMesaId(mesaSeleccionada.getId());
        cambiarMesa();
        consultaMesas = consultaAnterior;
        primeraMesa = paginaAnterior;
        actualizarResumen();
    }

    public void buscarPersonas() {
        asignar = new ArrayList<>();
        retirar = new ArrayList<>();
        primeraDisponible = primeraInscrita = 0;
    }

    public void cambiarEstadoMesas() {
        primeraMesa = 0;
        consultaMesas.setConPadron(filtro.getConPadron());
    }

    public boolean isPuedeEditar() {
        return procesoActivo != null && procesoActivo.getId().equals(filtro.getProcesoId())
                && filtro.getMesaId() != null && filtro.getRecintoId() != null;
    }

    public void asignarSeleccionados() {
        try {
            int n = service.asignarIglesia(filtro.getProcesoId(), filtro.getRecintoId(), filtro.getMesaId(),
                    filtro.getIglesiaId());
            asignar = new ArrayList<>();
            actualizarResumen();
            recalcularAvance();
            JsfUtil.addSuccessMessage(Constantes.getMensaje("gestionPadron.asignados", n));
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Error asignando padron", e);
            JsfUtil.addErrorMessage(Constantes.getMensaje("gestionPadron.error.operacion"));
        }
    }

    public void retirarSeleccionados() {
        try {
            int n = service.retirarIglesia(filtro.getProcesoId(), filtro.getRecintoId(), filtro.getMesaId(),
                    filtro.getIglesiaId());
            retirar = new ArrayList<>();
            actualizarResumen();
            recalcularAvance();
            JsfUtil.addSuccessMessage(Constantes.getMensaje("gestionPadron.retirados", n));
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Error retirando padron", e);
            JsfUtil.addErrorMessage(Constantes.getMensaje("gestionPadron.error.operacion"));
        }
    }

    private void actualizarResumen() {
        long[] valores = consultado ? service.resumen(consultaMesas) : new long[3];
        total = valores[0];
        mesasCon = valores[1];
        mesasSin = valores[2];
    }

    public StreamedContent generarReporte() {
        try {
            if (procesoActivo == null) {
                JsfUtil.addWarningMessageFromBundle("gestionPadron.error.proceso.sin.activo");
                return null;
            }
            FiltroPadronDTO reporte = new FiltroPadronDTO(filtro);
            reporte.setProcesoId(procesoActivo.getId());
            reporte.setIglesiaNombre(nombreIglesiaReporte);
            byte[] contenido = service.reporte(reporte, ambitoReporteGeneral());
            return DefaultStreamedContent.builder().name("empadronados_" + java.time.LocalDate.now() + ".xlsx")
                    .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .stream(() -> new ByteArrayInputStream(contenido)).build();
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Error exportando padron", e);
            JsfUtil.addErrorMessage(Constantes.getMensaje("gestionPadron.error.reporte"));
        }
        return null;
    }

    /**
     * Reporte XLSX de los empadronados de una sola mesa, la de la fila pulsada, sin
     * depender de la selección actual. Reutiliza el mismo servicio y filtro que el
     * reporte general: se copia el filtro vigente y se fija la mesa, de modo que se
     * respetan el proceso activo y los filtros geográficos ya aplicados.
     */
    public StreamedContent generarReporteMesa(MesaPadronDTO mesa) {
        try {
            if (procesoActivo == null) {
                JsfUtil.addWarningMessageFromBundle("gestionPadron.error.proceso.sin.activo");
                return null;
            }
            if (mesa == null || mesa.getId() == null) {
                JsfUtil.addWarningMessageFromBundle("gestionPadron.error.mesa.requerida");
                return null;
            }
            FiltroPadronDTO reporte = new FiltroPadronDTO(filtro);
            reporte.setProcesoId(procesoActivo.getId());
            reporte.setMesaId(mesa.getId());
            reporte.setIglesiaNombre(null);
            reporte.setBusqueda(null);
            byte[] contenido = service.reporte(reporte, ambitoDeMesa(mesa));
            return DefaultStreamedContent.builder().name(nombreArchivoMesa(mesa))
                    .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .stream(() -> new ByteArrayInputStream(contenido)).build();
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Error exportando padron de la mesa", e);
            JsfUtil.addErrorMessage(Constantes.getMensaje("gestionPadron.error.reporte"));
        }
        return null;
    }

    /** Nombre de archivo con recinto y mesa, para identificarlo sin abrirlo. */
    private String nombreArchivoMesa(MesaPadronDTO mesa) {
        String recinto = sanearNombreArchivo(mesa.getRecinto());
        String nombreMesa = sanearNombreArchivo(mesa.getNombre() == null || mesa.getNombre().isBlank()
                ? String.valueOf(mesa.getId())
                : mesa.getNombre());
        StringBuilder nombre = new StringBuilder("empadronados");
        if (!recinto.isEmpty()) {
            nombre.append('_').append(recinto);
        }
        return nombre.append('_').append(nombreMesa).append('_')
                .append(java.time.LocalDate.now()).append(".xlsx").toString();
    }

    private String sanearNombreArchivo(String valor) {
        return valor == null ? "" : valor.trim().replaceAll("[^A-Za-z0-9._-]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    /**
     * Ámbito de un reporte de una sola mesa: los seis datos son iguales en todas las
     * filas, así que van a la cabecera del archivo y salen de las columnas.
     */
    private java.util.Map<String, String> ambitoDeMesa(MesaPadronDTO mesa) {
        java.util.LinkedHashMap<String, String> ambito = new java.util.LinkedHashMap<>();
        ambito.put("proceso", procesoActivo == null ? "" : procesoActivo.getNombre());
        ambito.put("provincia", nombreOpcion(provincias, filtro.getProvinciaId()));
        ambito.put("canton", mesa.getCanton());
        ambito.put("parroquia", mesa.getParroquia());
        ambito.put("recinto", mesa.getRecinto());
        ambito.put("mesa", mesa.getNombre());
        return ambito;
    }

    /**
     * Ámbito del reporte general de empadronados: a la cabecera van solo el proceso
     * electoral, que es siempre el vigente, y la provincia, porque todos los recintos
     * pertenecen a ella. Cantón, parroquia, recinto, mesa e iglesia se quedan como
     * columnas del detalle aunque haya filtros aplicados, ya que este reporte se lee
     * por fila.
     */
    private java.util.Map<String, String> ambitoReporteGeneral() {
        java.util.LinkedHashMap<String, String> ambito = new java.util.LinkedHashMap<>();
        ambito.put("proceso", procesoActivo == null ? "" : procesoActivo.getNombre());
        ambito.put("provincia", nombreOpcion(provincias, filtro.getProvinciaId()));
        return ambito;
    }

    private String nombreOpcion(List<OpcionPadronDTO> opciones, Integer id) {
        if (opciones == null || id == null) {
            return "";
        }
        return opciones.stream().filter(opcion -> id.equals(opcion.getId()))
                .map(OpcionPadronDTO::getNombre).findFirst().orElse("");
    }

    private class FilasLazy extends LazyDataModel<FilaPadronDTO> {
        private final boolean candidatos;
        private List<FilaPadronDTO> pagina = List.of();

        FilasLazy(boolean candidatos) {
            this.candidatos = candidatos;
        }

        @Override
        public int count(Map<String, FilterMeta> filters) {
            return consultado ? service.contar(filtro, candidatos) : 0;
        }

        @Override
        public List<FilaPadronDTO> load(int first, int pageSize, Map<String, SortMeta> sort,
                Map<String, FilterMeta> filters) {
            SortMeta orden = sort.values().stream().findFirst().orElse(null);
            pagina = consultado ? service.listar(filtro, candidatos, first, pageSize,
                    orden == null ? null : orden.getField(), orden != null && orden.getOrder() == SortOrder.DESCENDING)
                    : List.of();
            return pagina;
        }

        @Override
        public String getRowKey(FilaPadronDTO item) {
            return item.getId().toString();
        }

        @Override
        public FilaPadronDTO getRowData(String key) {
            List<FilaPadronDTO> seleccion = candidatos ? asignar : retirar;
            return java.util.stream.Stream
                    .concat(pagina.stream(), (seleccion == null ? List.<FilaPadronDTO>of() : seleccion).stream())
                    .filter(p -> p.getId().toString().equals(key)).findFirst().orElse(null);
        }
    }

    private class MesasLazy extends LazyDataModel<MesaPadronDTO> {
        private List<MesaPadronDTO> pagina = List.of();

        @Override
        public int count(Map<String, FilterMeta> filters) {
            return consultado ? service.contarMesas(consultaMesas) : 0;
        }

        @Override
        public List<MesaPadronDTO> load(int first, int pageSize, Map<String, SortMeta> sort,
                Map<String, FilterMeta> filters) {
            SortMeta orden = sort.values().stream().findFirst().orElse(null);
            pagina = consultado ? service.listarMesas(consultaMesas, first, pageSize,
                    orden == null ? null : orden.getField(), orden != null && orden.getOrder() == SortOrder.DESCENDING)
                    : new ArrayList<>();
            return pagina;
        }

        @Override
        public String getRowKey(MesaPadronDTO item) {
            return item.getId().toString();
        }

        @Override
        public MesaPadronDTO getRowData(String key) {
            return pagina.stream().filter(m -> m.getId().toString().equals(key)).findFirst().orElse(null);
        }
    }
}
