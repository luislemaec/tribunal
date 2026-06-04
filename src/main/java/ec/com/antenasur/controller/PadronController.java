package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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

    // NOTA: Periodo sigue como entidad — su DTO se creará en la iteración de
    // catálogos.
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
    }

    private void cargaDatosIniciales() {
        this.procesoActivo = procesoElectoralService.getActivo();
        this.cantones = geograpBean.getByFatherId(7);
        this.listaRecintos = recintoService.listarDTOs();
        this.listaMesas = mesaService.listarDTOs();
        // El padrón se carga vía filtros (cantón/parroquia/recinto/mesa).
        // Cargarlo aquí con TODAS las mesas dispara una query con JOIN FETCH
        // sobre millones de filas y excede el timeout JTA de 5 min.
        this.listaPadron = new ArrayList<>();
    }

    private void reseteaVariables() {
        this.listaRecintos = null;
        this.listaMesas = null;
        this.listaPadron = null;
        this.listaIglesiasPorAsignar = null;
        this.listaNombresIglesias = null;
    }

    public void obtieneListaDatosPorCanton() {
        if (cantonSeleccionado.getId() != null) {
            this.cantonSeleccionado = geograpBean.getById(this.cantonSeleccionado.getId());
            this.parroquiaSeleccionado = new Geograp();
            this.recintoSeleccionado = new RecintoDTO();
            this.mesaSeleccionado = new MesaDTO();
            reseteaVariables();
            this.parroquias = geograpBean.getByFatherId(this.cantonSeleccionado.getId());
            List<Integer> listaIdParroquias = geograpBean.getListaIdSGeograp(parroquias);
            cargaDatosGeneraPiklist(parroquias, listaIdParroquias);
        }
    }

    public void obtieneListaDatosPorParroquia() {
        if (parroquiaSeleccionado != null && parroquiaSeleccionado.getId() != null) {
            this.parroquiaSeleccionado = geograpBean.getById(this.parroquiaSeleccionado.getId());
            this.recintoSeleccionado = new RecintoDTO();
            this.mesaSeleccionado = new MesaDTO();
            reseteaVariables();
            List<Geograp> parroquiasTmp = new ArrayList<>();
            List<Integer> parroquiasIdTmp = new ArrayList<>();
            parroquiasTmp.add(parroquiaSeleccionado);
            parroquiasIdTmp.add(parroquiaSeleccionado.getId());
            cargaDatosGeneraPiklist(parroquiasTmp, parroquiasIdTmp);
        }
    }

    public void obtieneListaDatosPorRecinto() {
        if (recintoSeleccionado != null && recintoSeleccionado.getId() != null) {
            recintoSeleccionado = recintoService.obtenerDTOPorId(recintoSeleccionado.getId());
            this.mesaSeleccionado = new MesaDTO();
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
        if (mesaSeleccionado == null || mesaSeleccionado.getId() == null) {
            return;
        }
        mesaSeleccionado = mesaService.obtenerDTOPorId(mesaSeleccionado.getId());
        reseteaVariables();
        if (mesaSeleccionado == null || mesaSeleccionado.getRecinto() == null) {
            return;
        }
        this.recintoSeleccionado = mesaSeleccionado.getRecinto();
        if (recintoSeleccionado.getUbicacionId() == null) {
            return;
        }
        Geograp ubicacion = geograpBean.getById(recintoSeleccionado.getUbicacionId());
        List<Geograp> parroquiasTmp = new ArrayList<>();
        List<Integer> parroquiasIdTmp = new ArrayList<>();
        parroquiasTmp.add(ubicacion);
        parroquiasIdTmp.add(ubicacion.getId());
        cargaDatosGeneraPiklist(parroquiasTmp, parroquiasIdTmp);
    }

    private void cargaDatosGeneraPiklist(List<Geograp> parroquiasTmp, List<Integer> listaIdParroquias) {
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

        List<Integer> listaIdIglesias = padronService.obtieneIglesiasEnPadronCompletasPorUbicacion(listaIdParroquias);
        if (listaIdIglesias == null) {
            listaIdIglesias = new ArrayList<>();
        }
        if (listaIglesiasAsignadas != null) {
            for (IglesiaDTO ig : listaIglesiasAsignadas) {
                if (ig.getId() != null) {
                    listaIdIglesias.add(ig.getId());
                }
            }
        }
        this.listaIglesiasPorAsignar = iglesiaService.listarDTOsPorAsignarPorIds(listaIdIglesias, listaIdParroquias);
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
        if (listaMesas == null || listaMesas.isEmpty()) {
            listaPadron = new ArrayList<>();
            listaIglesiasAsignadas = new ArrayList<>();
            listaIglesiasPorAsignar = new ArrayList<>();
            generaPickList();
            return;
        }

        listaPadron = padronService.listarDTOsPorMesaIds(extraerIds(listaMesas));
        listaIglesiasAsignadas = padronService.obtenerIglesiasUnicasEnPadronDTO(listaPadron);

        List<Integer> idsIglesiasAsignadas = new ArrayList<>();
        if (listaIglesiasAsignadas != null) {
            for (IglesiaDTO ig : listaIglesiasAsignadas) {
                if (ig.getId() != null) {
                    idsIglesiasAsignadas.add(ig.getId());
                }
            }
        }

        listaIglesiasPorAsignar = iglesiaService.listarDTOsPorAsignarPorIds(
                idsIglesiasAsignadas, obtenerIdsParroquiasActuales());
        generaPickList();
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
}
