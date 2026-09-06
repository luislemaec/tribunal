package ec.com.antenasur.controller;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
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
    @Inject private GestionPadronService service;
    @Inject private ProcesoElectoralService procesoService;
    @Getter private FiltroPadronDTO filtro = new FiltroPadronDTO();
    private FiltroPadronDTO consultaMesas = new FiltroPadronDTO();
    @Getter @Setter private String nombreIglesiaReporte;
    @Getter private List<ec.com.antenasur.dto.IglesiaPadronDTO> iglesiasDisponibles = List.of(), iglesiasAsignadas = List.of();
    @Getter private List<ProcesoElectoralDTO> procesos;
    @Getter private List<OpcionPadronDTO> provincias, cantones = List.of(), parroquias = List.of(),
            recintos = List.of(), mesas = List.of(), iglesias = List.of();
    @Getter @Setter private List<FilaPadronDTO> asignar = new ArrayList<>(), retirar = new ArrayList<>();
    @Getter @Setter private MesaPadronDTO mesaSeleccionada;
    @Getter private LazyDataModel<FilaPadronDTO> disponibles, inscritos;
    @Getter private LazyDataModel<MesaPadronDTO> mesasLazy;
    @Getter private long total, mesasCon, mesasSin;
    @Getter private boolean consultado;
    @Getter @Setter private int primeraDisponible, primeraInscrita, primeraMesa;
    @Getter @Setter private int tabActivo;

    @PostConstruct
    public void init() {
        procesos = procesoService.listarDTOs();
        procesos.stream().filter(p -> Boolean.TRUE.equals(p.getActivo())).findFirst().ifPresent(p -> filtro.setProcesoId(p.getId()));
        provincias = service.geografia(null);
        disponibles = new FilasLazy(true);
        inscritos = new FilasLazy(false);
        mesasLazy = new MesasLazy();
    }

    public void cambiarProceso() { limpiarRecinto(); consultado = false; total = mesasCon = mesasSin = 0; }
    public void cambiarProvincia() {
        filtro.setCantonId(null); filtro.setParroquiaId(null); parroquias = List.of();
        cantones = filtro.getProvinciaId() == null ? List.of() : service.geografia(filtro.getProvinciaId());
        cambiarProceso();
    }
    public void cambiarCanton() {
        filtro.setParroquiaId(null);
        parroquias = filtro.getCantonId() == null ? List.of() : service.geografia(filtro.getCantonId());
        cambiarProceso();
    }
    public void cambiarParroquia() { cambiarProceso(); }

    private void limpiarRecinto() {
        filtro.setRecintoId(null); recintos = List.of(); limpiarMesa();
    }
    private void limpiarMesa() {
        iglesiasDisponibles = List.of(); iglesiasAsignadas = List.of();
        filtro.setMesaId(null); filtro.setIglesiaId(null); mesas = List.of(); iglesias = List.of();
        mesaSeleccionada = null; limpiarSeleccion();
    }
    private void limpiarSeleccion() {
        asignar = new ArrayList<>(); retirar = new ArrayList<>();
        primeraDisponible = primeraInscrita = primeraMesa = 0;
    }
    public void buscar() {
        limpiarSeleccion();
        consultaMesas = new FiltroPadronDTO(filtro);
        recintos = service.recintos(filtro);
        consultado = filtro.getProcesoId() != null;
        actualizarResumen();
    }
    public void cambiarRecinto() {
        limpiarMesa();
        if (filtro.getRecintoId() != null) mesas = service.mesas(filtro.getRecintoId());
        consultado = filtro.getProcesoId() != null;
        consultaMesas = new FiltroPadronDTO(filtro);
        actualizarResumen();
    }
    public void cambiarMesa() {
        filtro.setIglesiaId(null); limpiarSeleccion();
        iglesias = filtro.getMesaId() == null || filtro.getProcesoId() == null
                ? List.of() : service.iglesias(filtro.getMesaId(), filtro.getProcesoId());
        consultaMesas = new FiltroPadronDTO(filtro);
        actualizarResumen();
        cargarIglesias();
    }
    private void cargarIglesias() {
        iglesiasDisponibles = service.resumenIglesias(filtro.getMesaId(), filtro.getProcesoId(), false);
        iglesiasAsignadas = service.resumenIglesias(filtro.getMesaId(), filtro.getProcesoId(), true);
    }
    public void asignarIglesia(Integer id) {
        filtro.setIglesiaId(id);
        try { asignarSeleccionados(); } finally { filtro.setIglesiaId(null); cargarIglesias(); }
    }
    public void retirarIglesia(Integer id) {
        filtro.setIglesiaId(id);
        try { retirarSeleccionados(); } finally { filtro.setIglesiaId(null); cargarIglesias(); }
    }
    public String getNombreMesa() {
        return mesas.stream().filter(m -> m.getId().equals(filtro.getMesaId())).map(OpcionPadronDTO::getNombre)
                .findFirst().orElse("");
    }
    public void cambiarIglesia() { limpiarSeleccion(); actualizarResumen(); }
    public void seleccionarMesa(SelectEvent<MesaPadronDTO> event) {
        FiltroPadronDTO consultaAnterior = consultaMesas;
        int paginaAnterior = primeraMesa;
        mesaSeleccionada = event.getObject();
        filtro.setRecintoId(mesaSeleccionada.getRecintoId());
        mesas = service.mesas(filtro.getRecintoId());
        filtro.setMesaId(mesaSeleccionada.getId()); cambiarMesa();
        consultaMesas = consultaAnterior; primeraMesa = paginaAnterior; actualizarResumen();
    }
    public void buscarPersonas() { asignar = new ArrayList<>(); retirar = new ArrayList<>(); primeraDisponible = primeraInscrita = 0; }
    public void cambiarEstadoMesas() { primeraMesa = 0; consultaMesas.setConPadron(filtro.getConPadron()); }

    public boolean isPuedeEditar() {
        return filtro.getMesaId() != null && filtro.getRecintoId() != null && procesos.stream()
                .anyMatch(p -> p.getId().equals(filtro.getProcesoId()) && Boolean.TRUE.equals(p.getActivo()));
    }

    public void asignarSeleccionados() {
        try {
            int n = service.asignarIglesia(filtro.getProcesoId(), filtro.getRecintoId(), filtro.getMesaId(), filtro.getIglesiaId());
            asignar = new ArrayList<>(); actualizarResumen();
            JsfUtil.addSuccessMessage(Constantes.getMensaje("gestionPadron.asignados", n));
        } catch (NegocioException e) { JsfUtil.addErrorMessage(e.getMessage()); }
        catch (Exception e) { log.error("Error asignando padron", e); JsfUtil.addErrorMessage(Constantes.getMensaje("gestionPadron.error.operacion")); }
    }
    public void retirarSeleccionados() {
        try {
            int n = service.retirarIglesia(filtro.getProcesoId(), filtro.getRecintoId(), filtro.getMesaId(), filtro.getIglesiaId());
            retirar = new ArrayList<>(); actualizarResumen();
            JsfUtil.addSuccessMessage(Constantes.getMensaje("gestionPadron.retirados", n));
        } catch (NegocioException e) { JsfUtil.addErrorMessage(e.getMessage()); }
        catch (Exception e) { log.error("Error retirando padron", e); JsfUtil.addErrorMessage(Constantes.getMensaje("gestionPadron.error.operacion")); }
    }
    private void actualizarResumen() {
        long[] valores = consultado ? service.resumen(consultaMesas) : new long[3];
        total = valores[0]; mesasCon = valores[1]; mesasSin = valores[2];
    }
    public StreamedContent generarReporte() {
        try {
            FiltroPadronDTO reporte = new FiltroPadronDTO(filtro);
            reporte.setIglesiaNombre(nombreIglesiaReporte);
            byte[] contenido = service.reporte(reporte);
            return DefaultStreamedContent.builder().name("empadronados_" + java.time.LocalDate.now() + ".xlsx")
                    .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .stream(() -> new ByteArrayInputStream(contenido)).build();
        } catch (NegocioException e) { JsfUtil.addErrorMessage(e.getMessage()); }
        catch (Exception e) { log.error("Error exportando padron", e); JsfUtil.addErrorMessage(Constantes.getMensaje("gestionPadron.error.reporte")); }
        return null;
    }

    private class FilasLazy extends LazyDataModel<FilaPadronDTO> {
        private final boolean candidatos;
        private List<FilaPadronDTO> pagina = List.of();
        FilasLazy(boolean candidatos) { this.candidatos = candidatos; }
        @Override public int count(Map<String, FilterMeta> filters) {
            return consultado ? service.contar(filtro, candidatos) : 0;
        }
        @Override public List<FilaPadronDTO> load(int first, int pageSize, Map<String, SortMeta> sort, Map<String, FilterMeta> filters) {
            SortMeta orden = sort.values().stream().findFirst().orElse(null);
            pagina = consultado ? service.listar(filtro, candidatos, first, pageSize,
                    orden == null ? null : orden.getField(), orden != null && orden.getOrder() == SortOrder.DESCENDING) : List.of();
            return pagina;
        }
        @Override public String getRowKey(FilaPadronDTO item) { return item.getId().toString(); }
        @Override public FilaPadronDTO getRowData(String key) {
            List<FilaPadronDTO> seleccion = candidatos ? asignar : retirar;
            return java.util.stream.Stream.concat(pagina.stream(), (seleccion == null ? List.<FilaPadronDTO>of() : seleccion).stream())
                    .filter(p -> p.getId().toString().equals(key)).findFirst().orElse(null);
        }
    }
    private class MesasLazy extends LazyDataModel<MesaPadronDTO> {
        private List<MesaPadronDTO> pagina = List.of();
        @Override public int count(Map<String, FilterMeta> filters) { return consultado ? service.contarMesas(consultaMesas) : 0; }
        @Override public List<MesaPadronDTO> load(int first, int pageSize, Map<String, SortMeta> sort, Map<String, FilterMeta> filters) {
            pagina = consultado ? service.listarMesas(consultaMesas, first, pageSize) : List.of(); return pagina;
        }
        @Override public String getRowKey(MesaPadronDTO item) { return item.getId().toString(); }
        @Override public MesaPadronDTO getRowData(String key) { return pagina.stream().filter(m -> m.getId().toString().equals(key)).findFirst().orElse(null); }
    }
}
