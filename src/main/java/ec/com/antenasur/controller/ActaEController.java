package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.primefaces.PrimeFaces;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.FontProvider;

import ec.com.antenasur.bean.DocumentoBean;
import ec.com.antenasur.bean.GeograpBean;
import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.bean.ProcesoBean;
import ec.com.antenasur.dto.CandidatoDTO;
import ec.com.antenasur.dto.EscrutinioDTO;
import ec.com.antenasur.dto.MesaDTO;
import ec.com.antenasur.dto.RecintoDTO;
import ec.com.antenasur.enums.EstadoTarea;
import ec.com.antenasur.itext.ReportePFD;
import ec.com.antenasur.itext.UtilHtml;
import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.tec.CatalogoGeneral;
import ec.com.antenasur.model.tec.CategoriaVoto;
import ec.com.antenasur.model.tec.Documentos;
import ec.com.antenasur.model.tec.Lista;
import ec.com.antenasur.model.tec.Periodo;
import ec.com.antenasur.model.tec.PlantillaCorreo;
import ec.com.antenasur.model.tec.TipoDocumento;
import ec.com.antenasur.report.ReportTemplateController;
import ec.com.antenasur.service.tec.CategoriaVotoService;
import ec.com.antenasur.service.tec.EscrutinioService;
import ec.com.antenasur.service.tec.ListaService;
import ec.com.antenasur.service.tec.MesaService;
import ec.com.antenasur.service.tec.PeriodoService;
import ec.com.antenasur.service.tec.PlantillaCorreoService;
import ec.com.antenasur.service.tec.RecintoService;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Named
@ViewScoped
@Slf4j
public class ActaEController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String PATH_ACTAS_ESCRUTINIO = "/opt/ACTASE/";
    private static final Integer TAMANIO_LETRA = 0;
    private static final String FORMULARIO = "frmActaE";

    @Inject
    private LoginBean loginBean;

    @Inject
    private ProcesoBean procesoBean;

    @Inject
    private ListaService listaService;

    @Inject
    private PlantillaCorreoService plantillaCorreoService;

    @Inject
    private PeriodoService periodoService;

    @Inject
    private GeograpBean geograpBean;

    @Inject
    private RecintoService recintoService;

    @Inject
    private MesaService mesaService;

    @Inject
    private CategoriaVotoService categoriaVotoService;

    @Inject
    private EscrutinioService escrutinioService;

    @Inject
    private DocumentoBean documentoBean;

    @Getter
    @Setter
    private PlantillaCorreo plantillaCorreoSeleccionado;

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
    private List<MesaDTO> listaMesas, listaMesasCerradas;

    @Setter
    @Getter
    private MesaDTO mesaSeleccionado;

    // NOTA: Lista, CatalogoGeneral, CategoriaVoto, Periodo, PlantillaCorreo
    // siguen como entidades; sus DTOs se crean en la iteraciÃ³n de catÃ¡logos.
    @Setter
    @Getter
    private Lista listaSeleccionado;

    @Setter
    @Getter
    private List<Lista> listas;

    @Setter
    @Getter
    private List<CatalogoGeneral> cargosCandidatos;

    @Setter
    @Getter
    private List<CategoriaVoto> categoriasVotos;

    @Setter
    @Getter
    private List<EscrutinioDTO> listaCamposActaE;

    @Setter
    @Getter
    private Periodo periodoVigente;

    @Setter
    @Getter
    private CandidatoDTO candidatoSeleccionado;

    @Setter
    @Getter
    private String cedulaBuscar;

    @PostConstruct
    private void init() {
        inicializaVariables();
        cargaDatosIniciales();
    }

    private void inicializaVariables() {
        this.listaCamposActaE = new ArrayList<>();
        this.cantonSeleccionado = new Geograp();
        this.parroquiaSeleccionado = new Geograp();
        this.recintoSeleccionado = new RecintoDTO();
        this.mesaSeleccionado = new MesaDTO();
    }

    private void cargaDatosIniciales() {
        this.periodoVigente = periodoService.getPeridoActivo();
        this.cantones = geograpBean.getByFatherId(7);
        this.listaRecintos = recintoService.listarDTOs();
        this.listaMesas = mesaService.listarDTOs();
        this.listas = listaService.findAll();
        this.categoriasVotos = categoriaVotoService.getCategoriasOrdenados();

        MesaDTO mesaUsuario = obtenerMesaPorUsuario();
        if (mesaUsuario != null) {
            mesaSeleccionado = mesaUsuario;
            cargaDatosMesaSeleccionada();
        }
        PrimeFaces.current().ajax().update("frmIglesias", "msgs");
    }

    private MesaDTO obtenerMesaPorUsuario() {
        try {
            ec.com.antenasur.model.tec.Mesa m = mesaService.getMesaPorUsuario(loginBean.getUserName());
            return MesaDTO.fromEntity(m);
        } catch (Exception e) {
            return null;
        }
    }

    public void cargaParroquiasPorCanton() {
        try {
            if (cantonSeleccionado.getId() != null) {
                this.cantonSeleccionado = geograpBean.getById(this.cantonSeleccionado.getId());
                this.parroquias = geograpBean.getByFatherGeograp(this.cantonSeleccionado);
                this.cargaRecintosPorParroquias();
            }
        } catch (Exception e) {
        }
    }

    public void cargaRecintosPorParroquias() {
        try {
            List<Geograp> litaParroquiasTmp = new ArrayList<>();
            if (this.parroquiaSeleccionado != null && this.parroquiaSeleccionado.getId() != null) {
                this.parroquiaSeleccionado = geograpBean.getById(this.parroquiaSeleccionado.getId());
                litaParroquiasTmp.add(this.parroquiaSeleccionado);
                this.listaRecintos = recintoService.listarDTOsPorParroquias(litaParroquiasTmp);
            } else if (this.parroquias != null && !this.parroquias.isEmpty()) {
                this.listaRecintos = recintoService.listarDTOsPorParroquias(this.parroquias);
            }
            if (listaRecintos != null && !listaRecintos.isEmpty()) {
                this.cargaMesasPorRecintos();
            }
        } catch (Exception e) {
        }
    }

    public void cargaMesasPorRecintos() {
        if (recintoSeleccionado != null && recintoSeleccionado.getId() != null) {
            recintoSeleccionado = recintoService.obtenerDTOPorId(recintoSeleccionado.getId());
            this.listaMesas = filtrarMesasPorRecintoId(mesaService.listarDTOs(), recintoSeleccionado.getId());
            this.mesaSeleccionado = new MesaDTO();
        } else if (listaRecintos != null && !listaRecintos.isEmpty()) {
            List<Integer> recintoIds = new ArrayList<>();
            for (RecintoDTO r : listaRecintos) {
                recintoIds.add(r.getId());
            }
            this.listaMesas = filtrarMesasPorRecintoIds(mesaService.listarDTOs(), recintoIds);
        }
    }

    public void cargaDatosMesaSeleccionada() {
        if (mesaSeleccionado == null || mesaSeleccionado.getId() == null) {
            return;
        }
        mesaSeleccionado = mesaService.obtenerDTOPorId(mesaSeleccionado.getId());
        Integer periodoId = (periodoVigente != null) ? periodoVigente.getId() : null;
        List<Integer> categoriaIds = new ArrayList<>();
        if (categoriasVotos != null) {
            for (CategoriaVoto c : categoriasVotos) {
                categoriaIds.add(c.getId());
            }
        }
        this.listaCamposActaE = escrutinioService.prepararActaPorMesaDTO(
                mesaSeleccionado.getId(), periodoId, categoriaIds);
        if (mesaSeleccionado.getEstadoTarea() != null && mesaSeleccionado.getEstadoTarea().equals(EstadoTarea.COMPLETADO)) {
            JsfUtil.addInfoMessage("Mesa cerrado");
        }
        PrimeFaces.current().ajax().update("msgs", FORMULARIO + ":btnRegistrar");
    }

    public void guardaDatosMesaSeleccionada() {
        if (this.listaCamposActaE == null || this.listaCamposActaE.isEmpty()
                || mesaSeleccionado == null || mesaSeleccionado.getId() == null) {
            return;
        }
        try {
            MesaDTO mesaCerrada = escrutinioService.guardarActaCompletaDTO(
                    mesaSeleccionado.getId(), listaCamposActaE);
            if (mesaCerrada != null) {
                mesaSeleccionado = mesaCerrada;
            }
            ReportTemplateController documentoActaE = inicializaReporte();
            getListaStringDatos(documentoActaE);
            exportaPDF(documentoActaE, mesaSeleccionado.getObservacion() != null ? mesaSeleccionado.getObservacion() : "");
            if (Boolean.TRUE.equals(mesaSeleccionado.getTieneErrorConteo())) {
                JsfUtil.addWarningMessage("MESA CERRADO CON ERRORES " + mesaSeleccionado.getObservacion());
            } else {
                JsfUtil.addSuccessMessage("MESA CERRADO CORRECTO");
            }
            PrimeFaces.current().ajax().update("msgs", FORMULARIO);
        } catch (Exception e) {
            log.error("ERROR AL CERRAR MESA", e);
            mesaSeleccionado.setEstadoTarea(EstadoTarea.ABORTADO);
            mesaService.guardarDesdeDTO(mesaSeleccionado);
        }
    }

    private ReportTemplateController inicializaReporte() {
        Integer recintoId = (mesaSeleccionado.getRecinto() != null) ? mesaSeleccionado.getRecinto().getId() : 0;
        return new ReportTemplateController(
                "ACTA-R" + recintoId + "-M" + mesaSeleccionado.getId() + "-" + JsfUtil.getFechaStringYYYYMMddHHmm(new Date()),
                new float[]{20, 100, 40},
                new int[]{1200, 3000, 4000},
                new String[]{"Nro", "CATEGORIA", "TOTAL VOTOS"},
                TAMANIO_LETRA);
    }

    private void getListaStringDatos(ReportTemplateController documentoActaE) {
        try {
            if (listaCamposActaE == null) {
                return;
            }
            documentoActaE.setListaDatos(new String[listaCamposActaE.size() + 1][documentoActaE.getNumeroColumnas()]);
            int fila = 0;
            int totalVotos = 0;
            for (EscrutinioDTO item : listaCamposActaE) {
                documentoActaE.getListaDatos()[fila][0] = String.valueOf(fila + 1);
                documentoActaE.getListaDatos()[fila][1] = item.getCategoriaNombre() != null ? item.getCategoriaNombre() : "";
                documentoActaE.getListaDatos()[fila][2] = item.getTotalVotos() != null ? item.getTotalVotos().toString() : "0";
                totalVotos += (item.getTotalVotos() != null ? item.getTotalVotos() : 0);
                fila++;
            }
            documentoActaE.getListaDatos()[fila][0] = "";
            documentoActaE.getListaDatos()[fila][1] = "TOTAL";
            documentoActaE.getListaDatos()[fila][2] = String.valueOf(totalVotos);
        } catch (Exception e) {
            log.error("ERROR AL OBTENER LISTA DE DATOS REPORTE " + documentoActaE.getNombreReporte(), e);
        }
    }

    /**
     * Construye el HashMap de parÃ¡metros del acta. Resuelve la cadena
     * provincia/cantÃ³n/parroquia por id contra GeograpBean en lugar de
     * navegar relaciones lazy de la entidad Mesa.
     */
    private HashMap<String, String> getDatosActaE() {
        try {
            Date fechaActual = new Date();
            HashMap<String, String> parametros = new HashMap<>();
            if (mesaSeleccionado == null) {
                return parametros;
            }
            parametros.put("nombrePresidente", "PRESIDENTE");
            parametros.put("nombreSecretario", "SECRETARIO");
            parametros.put("nombreTesorero", "TESOREO");
            parametros.put("nombreVocal", "VOCAL");

            Geograp parroquia = (mesaSeleccionado.getUbicacionId() != null)
                    ? geograpBean.getById(mesaSeleccionado.getUbicacionId()) : null;
            Geograp canton = (parroquia != null) ? parroquia.getGeograp() : null;
            Geograp provincia = (canton != null) ? canton.getGeograp() : null;

            parametros.put("nombreProvinica", provincia != null ? provincia.getName() : "");
            parametros.put("nombreCanton", canton != null ? canton.getName() : "");
            parametros.put("nombreParroquia", parroquia != null ? parroquia.getName() : "");
            parametros.put("fechaActa", JsfUtil.getFechaParaActas(fechaActual));
            parametros.put("horaRegistro", JsfUtil.getHoraStringHHmmss(fechaActual));

            Integer recintoId = (mesaSeleccionado.getRecinto() != null) ? mesaSeleccionado.getRecinto().getId() : null;
            String recintoNombre = (mesaSeleccionado.getRecinto() != null) ? mesaSeleccionado.getRecinto().getNombre() : "";
            parametros.put("numeroJunta", recintoId != null ? recintoId.toString() : "");
            parametros.put("numeroMesa", mesaSeleccionado.getId().toString());
            parametros.put("nombreRecinto", recintoNombre);
            parametros.put("fechaRegistro", JsfUtil.getFechaStringddMMYY(fechaActual));
            return parametros;
        } catch (Exception e) {
            log.error("ERROR EN INICIALIZAR VARIABLES", e);
            return null;
        }
    }

    private String getPlantillaDocumento(String nombrePlantilla) {
        try {
            HashMap<String, String> parametros = getDatosActaE();
            this.plantillaCorreoSeleccionado = plantillaCorreoService.buscarPorAsunto(nombrePlantilla);
            this.plantillaCorreoSeleccionado.setMensaje(plantillaCorreoSeleccionado.getMensaje().replaceAll("\\{|\\}", ""));
            this.plantillaCorreoSeleccionado.setMensaje(UtilHtml.builTextHTMLToMail(parametros, plantillaCorreoSeleccionado.getMensaje()));
            return this.plantillaCorreoSeleccionado.getMensaje();
        } catch (Exception e) {
            return null;
        }
    }

    public void exportaPDF(ReportTemplateController documentoActaE, String observacion) {
        try {
            String txtContenidoActaE = getPlantillaDocumento("BIENVENIDO");
            String txtResponsableActaE = getPlantillaDocumento("RESPONSABLES ACTA ESCRUTINIOS");

            String extencion = ".pdf";
            String pathCompleto = PATH_ACTAS_ESCRUTINIO + documentoActaE.getNombreReporte() + extencion;

            Documentos documentoNuevo = new Documentos(documentoActaE.getNombreReporte(), pathCompleto, new TipoDocumento(Constantes.ACTA_ESCRUTINIO),
                    mesaSeleccionado.getId(), extencion, "application/" + extencion, documentoActaE.getNombreReporte());

            String pathCss = Constantes.getHojaEstilo();
            float tamanioLetra = 10;
            Font fuenteCabecerta = Constantes.getFuenteCabeceraDefault(tamanioLetra);
            Font fuenteContenido = Constantes.getFuenteContenidoDefault(tamanioLetra);

            String pathMontsR = Constantes.getPathFuenteExterna("Montserrat-Regular.ttf");
            FontFactory.register(pathMontsR, "montsR");
            FontFactory.getFont("montsR", tamanioLetra, Font.NORMAL, BaseColor.BLACK);

            FontProvider fontProvider = FontFactory.getFontImp();
            ReportePFD.nuevoPDF(documentoActaE.getNombreReporte());
            ReportePFD.agregaHTML(txtContenidoActaE, pathCss, fontProvider);
            ReportePFD.creaTablaCabecera(documentoActaE.getNumeroColumnas(), documentoActaE.getTamanioColumnasPDF(), documentoActaE.getNombreReporte(), documentoActaE.getNombresColumnas(), fuenteCabecerta);
            ReportePFD.creaContenidoTabla(documentoActaE.getListaDatos(), documentoActaE.getNombresColumnas(), fuenteContenido);
            ReportePFD.agregaParrafoEnBlanco();
            if (!observacion.isEmpty()) {
                ReportePFD.agregaParrafoObservacion(observacion);
            }
            ReportePFD.agregaHTML(txtResponsableActaE, pathCss, fontProvider);
            ReportePFD.getFinalParagraph(loginBean.getUsuario().getUsername());
            this.guardarDocumentoBD(documentoNuevo);
            ReportePFD.guardarDocumentosActasE(documentoActaE.getNombreReporte());
            procesoBean.okActivityRegister("GENERA " + documentoActaE.getNombreReporte(), documentoActaE.getNombreReporte() + ".pdf");
        } catch (Exception e) {
            log.error("ERROR AL EXPORTAR EXCEL DATOS REPORTE" + documentoActaE.getNombreReporte(), e);
        }
    }

    private void guardarDocumentoBD(Documentos documentoNuevo) {
        try {
            documentoBean.guardarDocumento(documentoNuevo);
        } catch (Exception e) {
        }
    }

    private static List<MesaDTO> filtrarMesasPorRecintoId(List<MesaDTO> mesas, Integer recintoId) {
        List<MesaDTO> resultado = new ArrayList<>();
        if (mesas == null || recintoId == null) {
            return resultado;
        }
        for (MesaDTO m : mesas) {
            if (m.getRecinto() != null && recintoId.equals(m.getRecinto().getId())) {
                resultado.add(m);
            }
        }
        return resultado;
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
}
