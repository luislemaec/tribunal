package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.primefaces.event.SelectEvent;
import org.primefaces.event.TransferEvent;
import org.primefaces.model.DualListModel;

import ec.com.antenasur.bean.GeograpBean;
import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.dto.IglesiaDTO;
import ec.com.antenasur.dto.MesaDTO;
import ec.com.antenasur.dto.PadronDTO;
import ec.com.antenasur.dto.RecintoDTO;
import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.service.IglesiaPersonaService;
import ec.com.antenasur.service.IglesiaService;
import ec.com.antenasur.service.tec.MesaService;
import ec.com.antenasur.service.tec.PadronService;
import ec.com.antenasur.service.tec.ProcesoElectoralService;
import ec.com.antenasur.service.tec.RecintoService;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Named
@ViewScoped
@Slf4j
@NoArgsConstructor
public class PadronController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Integer OPCION_TODOS = 0;

    @Inject
    private LoginBean loginBean;

    @Inject
    private GeograpBean geograpBean;

    @Inject
    private PadronService padronService;

    @Inject
    private RecintoService recintoService;

    @Inject
    private IglesiaService iglesiaService;

    @Inject
    private MesaService mesaService;

    @Inject
    private ProcesoElectoralService procesoElectoralService;

    @Inject
    private IglesiaPersonaService iglesiaPersonaService;

    @Setter
    @Getter
    private List<Geograp> cantones, parroquias;

    @Setter
    @Getter
    private Geograp cantonSeleccionado, parroquiaSeleccionado;

    @Setter
    @Getter
    private RecintoDTO recintoSeleccionado;

    @Setter
    @Getter
    private List<RecintoDTO> listaRecintos, listaRecintosSeleccionados;

    @Setter
    @Getter
    private List<PadronDTO> listaPadron, listaPadronSeleccionado;

    @Setter
    @Getter
    private List<IglesiaDTO> listaIglesiasAsignadas, listaIglesiasPorAsignar;

    @Setter
    @Getter
    private List<IglesiaDTO> listaIglesiasSeleccionadasPorAsignar, listaIglesiasSeleccionadasPorQuitar;

    @Setter
    @Getter
    private List<MesaDTO> listaMesas, listaMesasCerradas;

    @Setter
    @Getter
    private MesaDTO mesaSeleccionado;

    @Setter
    @Getter
    private PadronDTO padronSeleccionado;

    @Setter
    @Getter
    private DualListModel<IglesiaDTO> listaNombresIglesias;

    @Setter
    @Getter
    private List<String> iglesiasOrigen, iglesiasDestino;

    @Getter
    private Map<Integer, Integer> totalMiembrosHabilitadosPorIglesia = new HashMap<>();

    // NOTA: Periodo sigue como entidad; su DTO se creara en la iteracion de
    // catalogos.
    @Setter
    @Getter
    private ProcesoElectoral procesoActivo;

    @PostConstruct
    private void init() {
        try {
            inicializaVariables();
            cargaDatosIniciales();
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    private void inicializaVariables() {
        this.cantonSeleccionado = this.parroquiaSeleccionado = new Geograp();
        this.recintoSeleccionado = new RecintoDTO();
        this.padronSeleccionado = new PadronDTO();
        this.mesaSeleccionado = new MesaDTO();
        this.listaIglesiasSeleccionadasPorAsignar = new ArrayList<>();
        this.listaIglesiasSeleccionadasPorQuitar = new ArrayList<>();
    }

    private void cargaDatosIniciales() {
        this.procesoActivo = procesoElectoralService.getActivo();
        this.cantones = geograpBean.getByFatherId(7);
        this.parroquias = new ArrayList<>();
        this.listaRecintos = null;
        this.listaMesas = null;
        // El padron se carga via filtros bajo demanda (canton/parroquia/recinto/mesa).
        // Cargarlo aqui con TODAS las mesas dispara una query con JOIN FETCH
        // sobre millones de filas y excede el timeout JTA de 5 min.
        this.listaPadron = new ArrayList<>();
        limpiarDatosAsignacionMesa();
    }

    private void reseteaVariables() {
        this.listaRecintos = null;
        this.listaMesas = null;
        limpiarDatosAsignacionMesa();
    }

    private void limpiarDatosAsignacionMesa() {
        this.listaPadron = new ArrayList<>();
        this.listaIglesiasAsignadas = new ArrayList<>();
        this.listaIglesiasPorAsignar = new ArrayList<>();
        this.listaIglesiasSeleccionadasPorAsignar = new ArrayList<>();
        this.listaIglesiasSeleccionadasPorQuitar = new ArrayList<>();
        this.listaNombresIglesias = null;
        this.totalMiembrosHabilitadosPorIglesia = new HashMap<>();
    }

    public void obtieneListaDatosPorCanton() {
        liberarSeleccionGeograficaDesdeCanton();
        if (cantonSeleccionado == null || cantonSeleccionado.getId() == null) {
            return;
        }
        if (OPCION_TODOS.equals(cantonSeleccionado.getId())) {
            this.parroquias = obtenerParroquiasDeCantones(cantones);
            cargaDatosGeneraPiklist(parroquias, obtenerIdsGeograp(parroquias));
            return;
        }
        this.cantonSeleccionado = geograpBean.getById(this.cantonSeleccionado.getId());
        this.parroquias = geograpBean.getByFatherId(this.cantonSeleccionado.getId());
        cargaDatosGeneraPiklist(parroquias, obtenerIdsGeograp(parroquias));
    }

    public void obtieneListaDatosPorParroquia() {
        liberarSeleccionGeograficaDesdeParroquia();
        if (parroquiaSeleccionado == null || parroquiaSeleccionado.getId() == null) {
            return;
        }
        if (OPCION_TODOS.equals(parroquiaSeleccionado.getId())) {
            cargaDatosGeneraPiklist(parroquias, obtenerIdsGeograp(parroquias));
            return;
        }
        this.parroquiaSeleccionado = geograpBean.getById(this.parroquiaSeleccionado.getId());
        List<Geograp> parroquiasTmp = new ArrayList<>();
        parroquiasTmp.add(parroquiaSeleccionado);
        cargaDatosGeneraPiklist(parroquiasTmp, obtenerIdsGeograp(parroquiasTmp));
    }

    public void obtieneListaDatosPorRecinto() {
        limpiarDatosAsignacionMesa();
        this.mesaSeleccionado = new MesaDTO();
        if (recintoSeleccionado != null && recintoSeleccionado.getId() != null) {
            recintoSeleccionado = recintoService.obtenerDTOPorId(recintoSeleccionado.getId());
            if (recintoSeleccionado == null || recintoSeleccionado.getUbicacionId() == null) {
                return;
            }
            Geograp ubicacion = geograpBean.getById(recintoSeleccionado.getUbicacionId());
            List<Geograp> parroquiasTmp = new ArrayList<>();
            List<Integer> parroquiasIdTmp = new ArrayList<>();
            parroquiasTmp.add(ubicacion);
            parroquiasIdTmp.add(ubicacion.getId());
            cargaDatosGeneraPiklist(parroquiasTmp, parroquiasIdTmp);
        }
    }

    public void obtieneListaDatosPorMesa() {
        limpiarDatosAsignacionMesa();
        if (mesaSeleccionado == null || mesaSeleccionado.getId() == null) {
            return;
        }
        mesaSeleccionado = mesaService.obtenerDTOPorId(mesaSeleccionado.getId());
        if (mesaSeleccionado == null || mesaSeleccionado.getRecinto() == null) {
            return;
        }
        this.recintoSeleccionado = mesaSeleccionado.getRecinto();
        if (recintoSeleccionado.getUbicacionId() == null) {
            return;
        }
        cargarAsignacionMesaSeleccionada();
    }

    private void cargarAsignacionMesaSeleccionada() {
        if (mesaSeleccionado == null || mesaSeleccionado.getId() == null
                || recintoSeleccionado == null || recintoSeleccionado.getUbicacionId() == null) {
            return;
        }
        List<Integer> mesaIds = new ArrayList<>();
        mesaIds.add(mesaSeleccionado.getId());
        listaPadron = padronService.listarDTOsPorMesaIds(mesaIds);
        listaIglesiasAsignadas = padronService.obtenerIglesiasUnicasEnPadronDTO(listaPadron);

        List<Integer> parroquiaIds = new ArrayList<>();
        parroquiaIds.add(recintoSeleccionado.getUbicacionId());
        List<Integer> iglesiasExcluir = obtenerIdsIglesiasAsignadasAProceso(parroquiaIds);
        listaIglesiasPorAsignar = iglesiaService.listarDTOsPorAsignarPorIds(iglesiasExcluir, parroquiaIds);
        cargarTotalesMiembrosHabilitados();
        limpiarSeleccionIglesias();
        generaPickList();
    }

    private void cargaDatosGeneraPiklist(List<Geograp> parroquiasTmp, List<Integer> listaIdParroquias) {
        limpiarDatosAsignacionMesa();
        if (parroquiasTmp == null) {
            return;
        }
        this.listaRecintos = recintoService.listarDTOsPorParroquias(parroquiasTmp);
        if (listaRecintos == null || listaRecintos.isEmpty()) {
            JsfUtil.addInfoMessageFromBundle("padron.mensaje.sin.recintos");
            return;
        }
        this.listaMesas = mesaService.listarDTOs();
        // filtrar mesas a las de los recintos cargados
        this.listaMesas = filtrarMesasPorRecintoIds(listaMesas, extraerIds(listaRecintos));
        this.listaMesas = filtrarMesaSeleccionada(listaMesas);
        if (listaMesas == null || listaMesas.isEmpty()) {
            JsfUtil.addInfoMessageFromBundle("padron.mensaje.sin.mesas");
            return;
        }
        this.listaPadron = padronService.listarDTOsPorMesaIds(extraerIds(listaMesas));
        if (listaPadron == null) {
            JsfUtil.addInfoMessageFromBundle("padron.mensaje.sin.padron");
            return;
        }
        this.listaIglesiasAsignadas = padronService.obtenerIglesiasUnicasEnPadronDTO(listaPadron);

        List<Integer> listaIdIglesias = obtenerIdsIglesiasAsignadasAProceso(listaIdParroquias);
        this.listaIglesiasPorAsignar = iglesiaService.listarDTOsPorAsignarPorIds(listaIdIglesias, listaIdParroquias);
        cargarTotalesMiembrosHabilitados();
        generaPickList();
    }

    private void generaPickList() {
        List<IglesiaDTO> origen = listaIglesiasPorAsignar != null
                ? new ArrayList<>(listaIglesiasPorAsignar) : new ArrayList<>();
        List<IglesiaDTO> destino = listaIglesiasAsignadas != null
                ? new ArrayList<>(listaIglesiasAsignadas) : new ArrayList<>();

        this.listaNombresIglesias = new DualListModel<>(origen, destino);
        if (origen.isEmpty() && destino.isEmpty()) {
            JsfUtil.addWarningMessageFromBundle("padron.mensaje.sin.iglesias.disponibles");
        }
    }

    public void asignarIglesiasSeleccionadas() {
        actualizarSeleccionIglesiasPorAsignar();
        Integer mesaId = (mesaSeleccionado != null) ? mesaSeleccionado.getId() : null;
        Integer procesoId = (procesoActivo != null) ? procesoActivo.getId() : null;
        if (mesaId == null) {
            JsfUtil.addWarningMessageFromBundle("padron.mensaje.mesa.requerida");
            return;
        }
        if (procesoId == null) {
            JsfUtil.addWarningMessageFromBundle("padron.mensaje.proceso.requerido");
            return;
        }
        if (listaIglesiasSeleccionadasPorAsignar == null || listaIglesiasSeleccionadasPorAsignar.isEmpty()) {
            JsfUtil.addWarningMessageFromBundle("padron.mensaje.iglesias.seleccion.requerida");
            return;
        }
        int registrosProcesados = 0;
        for (IglesiaDTO iglesia : listaIglesiasSeleccionadasPorAsignar) {
            if (iglesia != null && iglesia.getId() != null) {
                registrosProcesados += padronService.asignarIglesiaAMesaPorIds(iglesia.getId(), mesaId, procesoId);
            }
        }
        recargarVistaPadronActual();
        if (registrosProcesados > 0) {
            JsfUtil.addSuccessMessageFromBundle("padron.mensaje.exito.conteo", registrosProcesados);
        } else {
            JsfUtil.addWarningMessageFromBundle("padron.mensaje.sin.registros");
        }
    }

    public void quitarIglesiasSeleccionadas() {
        actualizarSeleccionIglesiasPorQuitar();
        Integer mesaId = (mesaSeleccionado != null) ? mesaSeleccionado.getId() : null;
        Integer procesoId = (procesoActivo != null) ? procesoActivo.getId() : null;
        if (mesaId == null) {
            JsfUtil.addWarningMessageFromBundle("padron.mensaje.mesa.requerida");
            return;
        }
        if (procesoId == null) {
            JsfUtil.addWarningMessageFromBundle("padron.mensaje.proceso.requerido");
            return;
        }
        if (listaIglesiasSeleccionadasPorQuitar == null || listaIglesiasSeleccionadasPorQuitar.isEmpty()) {
            JsfUtil.addWarningMessageFromBundle("padron.mensaje.iglesias.quitar.seleccion.requerida");
            return;
        }
        int registrosProcesados = 0;
        for (IglesiaDTO iglesia : listaIglesiasSeleccionadasPorQuitar) {
            if (iglesia != null && iglesia.getId() != null) {
                registrosProcesados += padronService.quitarIglesiaDeMesaPorIds(iglesia.getId(), mesaId, procesoId);
            }
        }
        recargarVistaPadronActual();
        if (registrosProcesados > 0) {
            JsfUtil.addSuccessMessageFromBundle("padron.mensaje.quitar.exito.conteo", registrosProcesados);
        } else {
            JsfUtil.addWarningMessageFromBundle("padron.mensaje.quitar.sin.registros");
        }
    }

    public void limpiarSeleccionIglesias() {
        this.listaIglesiasSeleccionadasPorAsignar = new ArrayList<>();
        this.listaIglesiasSeleccionadasPorQuitar = new ArrayList<>();
    }

    public void actualizarSeleccionIglesiasPorAsignar() {
        this.listaIglesiasSeleccionadasPorAsignar = depurarSeleccionIglesias(
                listaIglesiasSeleccionadasPorAsignar, listaIglesiasPorAsignar);
    }

    public void actualizarSeleccionIglesiasPorQuitar() {
        this.listaIglesiasSeleccionadasPorQuitar = depurarSeleccionIglesias(
                listaIglesiasSeleccionadasPorQuitar, listaIglesiasAsignadas);
    }

    public void onTransfer(TransferEvent event) {
        Integer mesaId = (mesaSeleccionado != null) ? mesaSeleccionado.getId() : null;
        Integer procesoId = (procesoActivo != null) ? procesoActivo.getId() : null;
        if (mesaId == null) {
            JsfUtil.addWarningMessageFromBundle("padron.mensaje.mesa.requerida");
            return;
        }
        if (procesoId == null) {
            JsfUtil.addWarningMessageFromBundle("padron.mensaje.proceso.requerido");
            return;
        }
        int registrosProcesados = 0;
        for (Object item : event.getItems()) {
            IglesiaDTO ig = (IglesiaDTO) item;
            if (event.isAdd()) {
                registrosProcesados += padronService.asignarIglesiaAMesaPorIds(ig.getId(), mesaId, procesoId);
            } else if (event.isRemove()) {
                registrosProcesados += padronService.quitarIglesiaDeMesaPorIds(ig.getId(), mesaId, procesoId);
            }
        }
        recargarVistaPadronActual();
        if (registrosProcesados > 0 && event.isAdd()) {
            JsfUtil.addSuccessMessageFromBundle("padron.mensaje.exito.conteo", registrosProcesados);
        } else if (registrosProcesados > 0 && event.isRemove()) {
            JsfUtil.addSuccessMessageFromBundle("padron.mensaje.quitar.exito.conteo", registrosProcesados);
        } else if (event.isRemove()) {
            JsfUtil.addWarningMessageFromBundle("padron.mensaje.quitar.sin.registros");
        } else {
            JsfUtil.addWarningMessageFromBundle("padron.mensaje.sin.registros");
        }
    }

    public void onSelect(SelectEvent<IglesiaDTO> event) {
        JsfUtil.addInfoMessageFromBundle("padron.mensaje.iglesia.seleccionada", event.getObject().getNombre());
    }

    public int getTotalSufragantesMesaSeleccionada() {
        if (mesaSeleccionado == null || mesaSeleccionado.getId() == null || listaPadron == null) {
            return 0;
        }
        return listaPadron.size();
    }

    public int getTotalIglesiasMesaSeleccionada() {
        if (mesaSeleccionado == null || mesaSeleccionado.getId() == null || listaIglesiasAsignadas == null) {
            return 0;
        }
        return listaIglesiasAsignadas.size();
    }

    public int getTotalIglesiasSeleccionadasPorAsignar() {
        return listaIglesiasSeleccionadasPorAsignar != null ? listaIglesiasSeleccionadasPorAsignar.size() : 0;
    }

    public boolean isExisteIglesiasSeleccionadasPorAsignar() {
        return listaIglesiasSeleccionadasPorAsignar != null && !listaIglesiasSeleccionadasPorAsignar.isEmpty();
    }

    public boolean isExisteIglesiasSeleccionadasPorQuitar() {
        return listaIglesiasSeleccionadasPorQuitar != null && !listaIglesiasSeleccionadasPorQuitar.isEmpty();
    }

    public int getTotalMiembrosHabilitadosSeleccionados() {
        int total = 0;
        if (listaIglesiasSeleccionadasPorAsignar == null) {
            return total;
        }
        for (IglesiaDTO iglesia : listaIglesiasSeleccionadasPorAsignar) {
            total += getTotalMiembrosHabilitados(iglesia);
        }
        return total;
    }

    public int getTotalMiembrosHabilitados(IglesiaDTO iglesia) {
        if (iglesia == null || iglesia.getId() == null) {
            return 0;
        }
        return totalMiembrosHabilitadosPorIglesia.getOrDefault(iglesia.getId(), 0);
    }

    private static List<Integer> extraerIds(List<? extends Object> dtos) {
        List<Integer> ids = new ArrayList<>();
        if (dtos == null) {
            return ids;
        }
        for (Object o : dtos) {
            if (o instanceof MesaDTO) {
                ids.add(((MesaDTO) o).getId());
            } else if (o instanceof RecintoDTO) {
                ids.add(((RecintoDTO) o).getId());
            }
        }
        return ids;
    }

    private static List<MesaDTO> filtrarMesasPorRecintoIds(List<MesaDTO> mesas, List<Integer> recintoIds) {
        List<MesaDTO> resultado = new ArrayList<>();
        if (mesas == null || recintoIds == null) {
            return resultado;
        }
        for (MesaDTO m : mesas) {
            if (m.getRecinto() != null && recintoIds.contains(m.getRecinto().getId())) {
                resultado.add(m);
            }
        }
        return resultado;
    }

    private List<MesaDTO> filtrarMesaSeleccionada(List<MesaDTO> mesas) {
        if (mesaSeleccionado == null || mesaSeleccionado.getId() == null || mesas == null || mesas.isEmpty()) {
            return mesas;
        }
        List<MesaDTO> resultado = new ArrayList<>();
        for (MesaDTO mesa : mesas) {
            if (mesa != null && mesaSeleccionado.getId().equals(mesa.getId())) {
                resultado.add(mesa);
                break;
            }
        }
        return resultado;
    }

    private void recargarVistaPadronActual() {
        if (mesaSeleccionado != null && mesaSeleccionado.getId() != null) {
            limpiarDatosAsignacionMesa();
            cargarAsignacionMesaSeleccionada();
            return;
        }
        if (listaMesas == null || listaMesas.isEmpty()) {
            listaPadron = new ArrayList<>();
            listaIglesiasAsignadas = new ArrayList<>();
            listaIglesiasPorAsignar = new ArrayList<>();
            limpiarSeleccionIglesias();
            totalMiembrosHabilitadosPorIglesia = new HashMap<>();
            generaPickList();
            return;
        }

        listaPadron = padronService.listarDTOsPorMesaIds(extraerIds(listaMesas));
        listaIglesiasAsignadas = padronService.obtenerIglesiasUnicasEnPadronDTO(listaPadron);

        List<Integer> idsParroquias = obtenerIdsParroquiasActuales();
        List<Integer> idsIglesiasAsignadas = obtenerIdsIglesiasAsignadasAProceso(idsParroquias);
        listaIglesiasPorAsignar = iglesiaService.listarDTOsPorAsignarPorIds(
                idsIglesiasAsignadas, idsParroquias);
        cargarTotalesMiembrosHabilitados();
        limpiarSeleccionIglesias();
        generaPickList();
    }

    private void cargarTotalesMiembrosHabilitados() {
        List<Integer> ids = new ArrayList<>();
        if (listaIglesiasPorAsignar != null) {
            for (IglesiaDTO iglesia : listaIglesiasPorAsignar) {
                if (iglesia != null && iglesia.getId() != null) {
                    ids.add(iglesia.getId());
                }
            }
        }
        if (listaIglesiasAsignadas != null) {
            for (IglesiaDTO iglesia : listaIglesiasAsignadas) {
                if (iglesia != null && iglesia.getId() != null && !ids.contains(iglesia.getId())) {
                    ids.add(iglesia.getId());
                }
            }
        }
        totalMiembrosHabilitadosPorIglesia = iglesiaPersonaService.contarPersonasHabilitadasPadronPorIglesias(ids);
    }

    private List<Integer> obtenerIdsParroquiasActuales() {
        List<Integer> ids = new ArrayList<>();
        if (parroquiaSeleccionado != null && parroquiaSeleccionado.getId() != null) {
            ids.add(parroquiaSeleccionado.getId());
            return ids;
        }
        if (recintoSeleccionado != null && recintoSeleccionado.getUbicacionId() != null) {
            ids.add(recintoSeleccionado.getUbicacionId());
            return ids;
        }
        if (parroquias != null && !parroquias.isEmpty()) {
            ids.addAll(geograpBean.getListaIdSGeograp(parroquias));
        }
        return ids;
    }

    private List<Integer> obtenerIdsIglesiasAsignadasAProceso(List<Integer> idsParroquias) {
        Integer procesoId = procesoActivo != null ? procesoActivo.getId() : null;
        List<Integer> ids = padronService.obtieneIglesiasEnPadronPorUbicacionYProceso(idsParroquias, procesoId);
        return ids != null ? ids : new ArrayList<>();
    }

    private void liberarSeleccionGeograficaDesdeCanton() {
        this.parroquiaSeleccionado = new Geograp();
        this.recintoSeleccionado = new RecintoDTO();
        this.mesaSeleccionado = new MesaDTO();
        this.parroquias = new ArrayList<>();
        reseteaVariables();
    }

    private void liberarSeleccionGeograficaDesdeParroquia() {
        this.recintoSeleccionado = new RecintoDTO();
        this.mesaSeleccionado = new MesaDTO();
        reseteaVariables();
    }

    private List<Geograp> obtenerParroquiasDeCantones(List<Geograp> cantonesBase) {
        List<Geograp> resultado = new ArrayList<>();
        if (cantonesBase == null) {
            return resultado;
        }
        for (Geograp canton : cantonesBase) {
            if (canton != null && canton.getId() != null) {
                List<Geograp> parroquiasCanton = geograpBean.getByFatherId(canton.getId());
                if (parroquiasCanton != null) {
                    resultado.addAll(parroquiasCanton);
                }
            }
        }
        return resultado;
    }

    private static List<Integer> obtenerIdsGeograp(List<Geograp> geograps) {
        List<Integer> ids = new ArrayList<>();
        if (geograps == null) {
            return ids;
        }
        for (Geograp geograp : geograps) {
            if (geograp != null && geograp.getId() != null) {
                ids.add(geograp.getId());
            }
        }
        return ids;
    }

    private static List<IglesiaDTO> depurarSeleccionIglesias(List<IglesiaDTO> seleccion, List<IglesiaDTO> disponibles) {
        List<IglesiaDTO> resultado = new ArrayList<>();
        if (seleccion == null || disponibles == null || disponibles.isEmpty()) {
            return resultado;
        }
        for (IglesiaDTO iglesia : seleccion) {
            if (iglesia != null && iglesia.getId() != null && contieneIglesia(disponibles, iglesia.getId())
                    && !contieneIglesia(resultado, iglesia.getId())) {
                resultado.add(iglesia);
            }
        }
        return resultado;
    }

    private static boolean contieneIglesia(List<IglesiaDTO> iglesias, Integer iglesiaId) {
        if (iglesias == null || iglesiaId == null) {
            return false;
        }
        for (IglesiaDTO iglesia : iglesias) {
            if (iglesia != null && iglesiaId.equals(iglesia.getId())) {
                return true;
            }
        }
        return false;
    }
}
