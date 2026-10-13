package ec.com.antenasur.controller;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import ec.com.antenasur.bean.GeograpBean;
import ec.com.antenasur.dto.IglesiaDTO;
import ec.com.antenasur.dto.MesaDTO;
import ec.com.antenasur.dto.PadronDTO;
import ec.com.antenasur.dto.RecintoDTO;
import ec.com.antenasur.itext.ReporteXLSX;
import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.tec.ProcesoElectoral;
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
    private GeograpBean geograpBean;

    @Inject
    private PadronService padronService;

    @Inject
    private RecintoService recintoService;

    @Inject
    private MesaService mesaService;

    @Inject
    private ProcesoElectoralService procesoElectoralService;

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
    private List<RecintoDTO> listaRecintos;

    @Setter
    @Getter
    private List<PadronDTO> listaPadron;

    @Setter
    @Getter
    private List<IglesiaDTO> listaIglesiasAsignadas;

    @Setter
    @Getter
    private List<MesaDTO> listaMesas;

    @Setter
    @Getter
    private MesaDTO mesaSeleccionado;

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
        this.cantonSeleccionado = new Geograp();
        this.parroquiaSeleccionado = new Geograp();
        this.recintoSeleccionado = new RecintoDTO();
        this.mesaSeleccionado = new MesaDTO();
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
        limpiarDatosMesa();
    }

    private void reseteaVariables() {
        this.listaRecintos = null;
        this.listaMesas = null;
        limpiarDatosMesa();
    }

    private void limpiarDatosMesa() {
        this.listaPadron = new ArrayList<>();
        this.listaIglesiasAsignadas = new ArrayList<>();
    }

    public void obtieneReportePadronPorCanton() {
        liberarSeleccionGeograficaDesdeCanton();
        if (cantonSeleccionado == null || cantonSeleccionado.getId() == null) {
            return;
        }
        if (OPCION_TODOS.equals(cantonSeleccionado.getId())) {
            this.parroquias = obtenerParroquiasDeCantones(cantones);
            cargarCombosReportePadron(parroquias);
            return;
        }
        this.cantonSeleccionado = geograpBean.getById(this.cantonSeleccionado.getId());
        this.parroquias = geograpBean.getByFatherId(this.cantonSeleccionado.getId());
        cargarCombosReportePadron(parroquias);
    }

    public void obtieneReportePadronPorParroquia() {
        liberarSeleccionGeograficaDesdeParroquia();
        if (parroquiaSeleccionado == null || parroquiaSeleccionado.getId() == null) {
            return;
        }
        if (OPCION_TODOS.equals(parroquiaSeleccionado.getId())) {
            cargarCombosReportePadron(parroquias);
            return;
        }
        this.parroquiaSeleccionado = geograpBean.getById(this.parroquiaSeleccionado.getId());
        List<Geograp> parroquiasTmp = new ArrayList<>();
        parroquiasTmp.add(parroquiaSeleccionado);
        cargarCombosReportePadron(parroquiasTmp);
    }

    public void obtieneReportePadronPorRecinto() {
        limpiarDatosMesa();
        this.mesaSeleccionado = new MesaDTO();
        this.listaMesas = new ArrayList<>();
        if (recintoSeleccionado == null || recintoSeleccionado.getId() == null) {
            return;
        }
        recintoSeleccionado = recintoService.obtenerDTOPorId(recintoSeleccionado.getId());
        if (recintoSeleccionado == null || recintoSeleccionado.getId() == null) {
            return;
        }
        this.listaMesas = mesaService.listarDTOsPorRecintoId(recintoSeleccionado.getId());
    }

    /**
     * Reporte de la mesa elegida: solo el padrón de la mesa en el proceso activo y sus
     * iglesias (indicador).
     */
    public void obtieneReportePadronPorMesa() {
        limpiarDatosMesa();
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
        listaPadron = listarPadronPorMesasProcesoActivo(List.of(mesaSeleccionado.getId()));
        listaIglesiasAsignadas = padronService.obtenerIglesiasUnicasEnPadronDTO(listaPadron);
    }

    private void cargarCombosReportePadron(List<Geograp> parroquiasTmp) {
        limpiarDatosMesa();
        if (parroquiasTmp == null || parroquiasTmp.isEmpty()) {
            this.listaRecintos = new ArrayList<>();
            this.listaMesas = new ArrayList<>();
            return;
        }
        this.listaRecintos = recintoService.listarDTOsPorParroquias(parroquiasTmp);
        if (listaRecintos == null || listaRecintos.isEmpty()) {
            this.listaRecintos = new ArrayList<>();
            this.listaMesas = new ArrayList<>();
            JsfUtil.addInfoMessageFromBundle("padron.mensaje.sin.recintos");
            return;
        }
        this.listaMesas = filtrarMesasPorRecintoIds(mesaService.listarDTOsActivasConUbicacion(), extraerIds(listaRecintos));
        if (listaMesas == null || listaMesas.isEmpty()) {
            this.listaMesas = new ArrayList<>();
            JsfUtil.addInfoMessageFromBundle("padron.mensaje.sin.mesas");
        }
    }

    public int getTotalSufragantesMesaSeleccionada() {
        if (mesaSeleccionado == null || mesaSeleccionado.getId() == null || listaPadron == null) {
            return 0;
        }
        return listaPadron.size();
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

    private List<PadronDTO> listarPadronPorMesasProcesoActivo(List<Integer> mesaIds) {
        Integer procesoId = procesoActivo != null ? procesoActivo.getId() : null;
        if (procesoId == null) {
            return new ArrayList<>();
        }
        return padronService.listarDTOsPorMesaIdsYProceso(mesaIds, procesoId);
    }

    public void exportarExcelReportePadron() {
        try {
            if (mesaSeleccionado == null || mesaSeleccionado.getId() == null) {
                JsfUtil.addWarningMessageFromBundle("padron.mensaje.mesa.requerida");
                return;
            }
            List<PadronDTO> lista = listaPadron != null ? listaPadron : Collections.emptyList();
            String fecha = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
            String hora = new SimpleDateFormat("HH:mm:ss").format(new Date());

            ReporteXLSX.nuevoExcel("Reporte del Padron Electoral");
            ReporteXLSX.creaEspacioInformativoPadron(fecha, hora, ReporteXLSX.getNombreUsuarioAutenticado(),
                    getProcesoReportePadron(), getProvinciaReportePadron(), getCantonReportePadron(), getParroquiaReportePadron(),
                    getRecintoReportePadron(), getMesaReportePadron());

            String[] columnas = {
                "NRO.", "IDENTIFICACION", "NOMBRES", "IGLESIA", "COMUNIDAD / BARRIO", "ESTADO"
            };
            int[] anchos = { 1800, 5000, 9000, 10000, 8000, 4000 };
            ReporteXLSX.creaCabeceraTabla(columnas, anchos);

            String[][] datos = new String[lista.size()][columnas.length];
            for (int i = 0; i < lista.size(); i++) {
                PadronDTO padron = lista.get(i);
                datos[i][0] = String.valueOf(i + 1);
                datos[i][1] = texto(padron != null && padron.getIglesiaPersona() != null
                        && padron.getIglesiaPersona().getPersona() != null
                        ? padron.getIglesiaPersona().getPersona().getDocumento() : null);
                datos[i][2] = texto(padron != null && padron.getIglesiaPersona() != null
                        && padron.getIglesiaPersona().getPersona() != null
                        ? padron.getIglesiaPersona().getPersona().getNombres() : null);
                datos[i][3] = texto(padron != null && padron.getIglesiaPersona() != null
                        && padron.getIglesiaPersona().getIglesia() != null
                        ? padron.getIglesiaPersona().getIglesia().getNombre() : null);
                datos[i][4] = texto(padron != null && padron.getIglesiaPersona() != null
                        && padron.getIglesiaPersona().getIglesia() != null
                        ? padron.getIglesiaPersona().getIglesia().getComunidad() : null);
                datos[i][5] = Boolean.TRUE.equals(padron != null ? padron.getSufrago() : null) ? "Sufrago" : "Pendiente";
            }

            ReporteXLSX.creaContenidoTabla(datos, columnas);
            ReporteXLSX.setFinalParagraph(lista.size());
            ReporteXLSX.descargarExcel("reporte_padron_electoral");
        } catch (Exception e) {
            log.error("Error al exportar Excel del reporte de padron", e);
            JsfUtil.addErrorMessageFromBundle("reportePadron.mensaje.exportar.excel.error");
        }
    }

    private static String texto(String valor) {
        return valor != null ? valor : "";
    }

    private String getProcesoReportePadron() {
        if (procesoActivo != null && procesoActivo.getNombre() != null) {
            return procesoActivo.getNombre();
        }
        if (listaPadron != null && !listaPadron.isEmpty()) {
            return texto(listaPadron.get(0).getProcesoNombre());
        }
        return "";
    }

    private String getProvinciaReportePadron() {
        Integer cantonId = mesaSeleccionado != null && mesaSeleccionado.getCantonId() != null
                ? mesaSeleccionado.getCantonId()
                : (recintoSeleccionado != null && recintoSeleccionado.getCantonId() != null
                        ? recintoSeleccionado.getCantonId()
                        : (cantonSeleccionado != null ? cantonSeleccionado.getId() : null));
        if (cantonId == null || OPCION_TODOS.equals(cantonId)) {
            return "";
        }
        Geograp canton = geograpBean.getById(cantonId);
        return canton != null && canton.getGeograp() != null ? texto(canton.getGeograp().getName()) : "";
    }

    private String getCantonReportePadron() {
        if (mesaSeleccionado != null && mesaSeleccionado.getCantonNombre() != null) {
            return mesaSeleccionado.getCantonNombre();
        }
        if (recintoSeleccionado != null && recintoSeleccionado.getCantonNombre() != null) {
            return recintoSeleccionado.getCantonNombre();
        }
        return cantonSeleccionado != null ? texto(cantonSeleccionado.getName()) : "";
    }

    private String getParroquiaReportePadron() {
        if (mesaSeleccionado != null && mesaSeleccionado.getUbicacionNombre() != null) {
            return mesaSeleccionado.getUbicacionNombre();
        }
        if (recintoSeleccionado != null && recintoSeleccionado.getUbicacionNombre() != null) {
            return recintoSeleccionado.getUbicacionNombre();
        }
        return parroquiaSeleccionado != null ? texto(parroquiaSeleccionado.getName()) : "";
    }

    private String getRecintoReportePadron() {
        if (mesaSeleccionado != null && mesaSeleccionado.getRecinto() != null) {
            return texto(mesaSeleccionado.getRecinto().getNombre());
        }
        return recintoSeleccionado != null ? texto(recintoSeleccionado.getNombre()) : "";
    }

    private String getMesaReportePadron() {
        return mesaSeleccionado != null ? texto(mesaSeleccionado.getNombre()) : "";
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
}
