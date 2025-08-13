package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.FontProvider;

import ec.com.antenasur.bean.DocumentoBean;
import ec.com.antenasur.bean.GeograpBean;
import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.bean.ProcesoBean;
import ec.com.antenasur.domain.Geograp;
import ec.com.antenasur.domain.tec.Candidato;
import ec.com.antenasur.domain.tec.CatalogoGeneral;
import ec.com.antenasur.domain.tec.CategoriaVoto;
import ec.com.antenasur.domain.tec.Documentos;
import ec.com.antenasur.domain.tec.Escrutinio;
import ec.com.antenasur.domain.tec.Lista;
import ec.com.antenasur.domain.tec.Mesa;
import ec.com.antenasur.domain.tec.Periodo;
import ec.com.antenasur.domain.tec.PlantillaCorreo;
import ec.com.antenasur.domain.tec.Recinto;
import ec.com.antenasur.domain.tec.TipoDocumento;
import ec.com.antenasur.enums.EstadoTarea;
import ec.com.antenasur.itext.ReportePFD;
import ec.com.antenasur.itext.UtilHtml;
import ec.com.antenasur.report.ReportTemplateController;
import ec.com.antenasur.service.tec.CategoriaVotoFacade;
import ec.com.antenasur.service.tec.EscrutinioFacade;
import ec.com.antenasur.service.tec.ListaFacade;
import ec.com.antenasur.service.tec.MesaFacade;
import ec.com.antenasur.service.tec.PeriodoFacade;
import ec.com.antenasur.service.tec.PlantillaCorreoFacade;
import ec.com.antenasur.service.tec.RecintoFacade;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 * @fecha 2022-09-06 14:30
 * @version 1.0.0 Maneja acta de escritinios
 */
@Named
@ViewScoped
@Slf4j
public class ActaEController implements Serializable {

    private static final String DESTINATION = System.getProperty("java.io.tmpdir");

    //private static final String PATH_ACTAS_ESCRUTINIO = "C:\\ARCHIVOS\\ACTASE\\";
    private static final String PATH_ACTAS_ESCRUTINIO = "/opt/ACTASE/";

    private static final Integer TAMANIO_LETRA = 0;

    private static final String FORMULARIO = "frmActaE";
    private static final String TABLA = "wdTblCandidatos";
    private static final String MENSAJE_REGISTRA_OK = "Candidato registrado";
    private static final String MENSAJE_ACTUALIZA_OK = "Candidato actualizado";
    private static final String MENSAJE_ELIMINA_OK = "Candidato eliminado";
    public static final String MENSAJE_CONFORMACION_ELIMINAR = "¿Esta seguro de eliminar?";

    @Inject
    private LoginBean loginBean;

    @Inject
    private ProcesoBean procesoBean;

    @Inject
    private ListaFacade listaFacade;

    @Inject
    private PlantillaCorreoFacade plantillaCorreoFacade;

    @Getter
    @Setter
    private PlantillaCorreo plantillaCorreoSeleccionado;

    @Inject
    private PeriodoFacade periodoFacade;

    @Inject
    private GeograpBean geograpBean;

    @Inject
    private RecintoFacade recintoFacade;

    @Inject
    private MesaFacade mesaFacade;

    @Inject
    private CategoriaVotoFacade categoriaVotoFacade;

    @Inject
    private EscrutinioFacade escrutinioFacade;

    @Inject
    private DocumentoBean documentoBean;

    @Setter
    @Getter
    private List<Geograp> cantones, parroquias;

    @Setter
    @Getter
    private Geograp cantonSeleccionado, parroquiaSeleccionado;

    @Setter
    @Getter
    private Recinto recintoSeleccionado;

    @Setter
    @Getter
    private List<Recinto> listaRecintos, listaRecintosSeleccionados;

    @Setter
    @Getter
    private List<Mesa> listaMesas, listaMesasCerradas;

    @Setter
    @Getter
    private Mesa mesaSeleccionado;

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
    private List<Escrutinio> listaCamposActaE;

    @Setter
    @Getter
    private Periodo periodoVigente;

    @Setter
    @Getter
    private Candidato candidatoSeleccionado;

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
        this.recintoSeleccionado = new Recinto();
        this.mesaSeleccionado = new Mesa();
    }

    private void cargaDatosIniciales() {
        //PERIODO ACTIVO
        this.periodoVigente = periodoFacade.getPeridoActivo();
        //CANTONES DE CHIMBORAZO
        this.cantones = geograpBean.getByFatherId(7);
        //RECINTOS POR CERRAR
        this.listaRecintos = recintoFacade.findAll();
        //MESAS POR CERRAR
        this.listaMesas = mesaFacade.getMesasPorRecintos(listaRecintos);
        //LISTAS CALIFICADAS
        this.listas = listaFacade.findAll();

        //CATEGORIA VOTOS, para registrar votos por mesas
        this.categoriasVotos = categoriaVotoFacade.getCategoriasOrdenados();

        mesaSeleccionado = mesaFacade.getMesaPorUsuario(loginBean.getUserName());
        if (mesaSeleccionado != null) {
            cargaDatosMesaSeleccionada();
        } else {
            mesaSeleccionado = new Mesa();
        }
        //IGLESIAS ASIGNADAS
        PrimeFaces.current().ajax().update("frmIglesias", "msgs");

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
            List<Geograp> litaParroquiasTmp = new ArrayList<Geograp>();
            if (this.parroquiaSeleccionado != null && this.parroquiaSeleccionado.getId() != null) {
                this.parroquiaSeleccionado = geograpBean.getById(this.parroquiaSeleccionado.getId());

                litaParroquiasTmp.add(this.parroquiaSeleccionado);
                this.listaRecintos = recintoFacade.getRecintosPorParroquias(litaParroquiasTmp);
            } else {
                //CARGA RECINTOS
                if (this.parroquias != null && !this.parroquias.isEmpty()) {
                    this.listaRecintos = recintoFacade.getRecintosPorParroquias(this.parroquias);
                }
            }

            //CARGA MESAS
            if (listaRecintos != null && !listaRecintos.isEmpty()) {
                this.cargaMesasPorRecintos();
            }
        } catch (Exception e) {
        }
    }

    public void cargaMesasPorRecintos() {
        if (recintoSeleccionado != null && recintoSeleccionado.getId() != null) {
            recintoSeleccionado = recintoFacade.find(recintoSeleccionado.getId());
            List<Recinto> listaRecintosTmp = new ArrayList<>();
            listaRecintosTmp.add(recintoSeleccionado);
            listaMesas.clear();
            this.listaMesas = mesaFacade.getMesasPorRecintos(listaRecintosTmp);
            this.mesaSeleccionado = new Mesa();
        } else {
            if (listaRecintos != null && !listaRecintos.isEmpty()) {
                this.listaMesas = mesaFacade.getMesasPorRecintos(listaRecintos);
            }
        }
    }

    public void cargaDatosMesaSeleccionada() {
        if (mesaSeleccionado != null && mesaSeleccionado.getId() != null) {
            mesaSeleccionado = mesaFacade.find(mesaSeleccionado.getId());
            this.listaCamposActaE = escrutinioFacade.buscaPorMesa(mesaSeleccionado);
            if (listaCamposActaE == null || listaCamposActaE.isEmpty()) {
                this.listaCamposActaE = new ArrayList<>();
                for (CategoriaVoto categoria : categoriasVotos) {
                    Escrutinio escruitinio = new Escrutinio();
                    escruitinio.setMesa(mesaSeleccionado);
                    escruitinio.setPeriodo(periodoVigente);
                    escruitinio.setCategoria(categoria);
                    listaCamposActaE.add(escruitinio);
                }
            }
            if (mesaSeleccionado.getEstadoTarea() != null && mesaSeleccionado.getEstadoTarea().equals(EstadoTarea.COMPLETADO)) {
                JsfUtil.addInfoMessage("Mesa cerrado");
            }
            PrimeFaces.current().ajax().update("msgs", FORMULARIO + ":btnRegistrar");
        }
    }

    public void guardaDatosMesaSeleccionada() {
        if (this.listaCamposActaE != null && !this.listaCamposActaE.isEmpty()) {
            try {
                int totalPapeletasUso = 0;
                for (Escrutinio itemActa : listaCamposActaE) {
                    if (itemActa.getId() != null) {
                        escrutinioFacade.edit(itemActa);
                        totalPapeletasUso = totalPapeletasUso + itemActa.getTotalVotos();
                    } else {
                        escrutinioFacade.create(itemActa);
                        totalPapeletasUso = totalPapeletasUso + itemActa.getTotalVotos();
                    }
                }
                mesaSeleccionado.setEstadoTarea(EstadoTarea.COMPLETADO);
                mesaSeleccionado.setTotalPapetelasUso(totalPapeletasUso);
                if (mesaSeleccionado.getTotalVotos() == totalPapeletasUso) {
                    mesaSeleccionado.setTieneErrorConteo(false);
                    mesaSeleccionado.setObservacion("");
                }
                if (mesaSeleccionado.getTotalVotos() > totalPapeletasUso) {
                    mesaSeleccionado.setTieneErrorConteo(true);
                    mesaSeleccionado.setObservacion(mesaSeleccionado.getTotalVotos() - totalPapeletasUso + " PAPELETAS FALTANTES");
                }
                if (mesaSeleccionado.getTotalVotos() < totalPapeletasUso) {
                    mesaSeleccionado.setTieneErrorConteo(true);
                    mesaSeleccionado.setObservacion(totalPapeletasUso - mesaSeleccionado.getTotalVotos() + " PAPELETAS EXCEDENTES");
                }
                mesaSeleccionado = mesaFacade.edit(mesaSeleccionado);

                ReportTemplateController documentoActaE = inicializaReporte();
                getListaStringDatos(documentoActaE);
                exportaPDF(documentoActaE, mesaSeleccionado.getObservacion());
                if (mesaSeleccionado.getTieneErrorConteo()) {
                    JsfUtil.addWarningMessage("MESA CERRADO CON ERRORES " + mesaSeleccionado.getObservacion());
                } else {
                    JsfUtil.addSuccessMessage("MESA CERRADO CORRECTO");
                }

                PrimeFaces.current().ajax().update("msgs", FORMULARIO);

            } catch (Exception e) {
                mesaSeleccionado.setEstadoTarea(EstadoTarea.ABORTADO);
                mesaFacade.edit(mesaSeleccionado);
            }
        }
    }

    private ReportTemplateController inicializaReporte() {
        ReportTemplateController documentoActaE = new ReportTemplateController(
                "ACTA-" + "R" + mesaSeleccionado.getRecinto().getId() + "-M" + mesaSeleccionado.getId() + "-" + JsfUtil.getFechaStringYYYYMMddHHmm(new Date()),
                new float[]{20, 100, 40},
                new int[]{1200, 3000, 4000},
                new String[]{"Nro", "CATEGORIA", "TOTAL VOTOS"},
                TAMANIO_LETRA);
        return documentoActaE;
    }

    private void getListaStringDatos(ReportTemplateController documentoActaE) {
        try {
            if (listaCamposActaE != null) {
                documentoActaE.setListaDatos(new String[listaCamposActaE.size() + 1][documentoActaE.getNumeroColumnas()]);
                int fila = 0;
                Integer totalVotos = 0;
                for (Escrutinio item : listaCamposActaE) {
                    documentoActaE.getListaDatos()[fila][0] = String.valueOf(fila + 1);
                    documentoActaE.getListaDatos()[fila][1] = item.getCategoria().getNombre();
                    documentoActaE.getListaDatos()[fila][2] = item.getTotalVotos().toString();
                    totalVotos = totalVotos + item.getTotalVotos();
                    fila++;
                }

                documentoActaE.getListaDatos()[fila][0] = "";
                documentoActaE.getListaDatos()[fila][1] = "TOTAL";
                documentoActaE.getListaDatos()[fila][2] = totalVotos.toString();

            }
        } catch (Exception e) {
            log.error("ERROR AL OBTENER LISTA DE DATOS REPORTE " + documentoActaE.getNombreReporte(), e);
        }
    }

    /**
     *
     */
    private HashMap<String, String> getDatosActaE() {
        try {
            Date fechaActual = new Date();
            String fechaActa = JsfUtil.getFechaParaActas(fechaActual);
            HashMap<String, String> parametros = new HashMap<>();
            if (mesaSeleccionado != null) {
                parametros.put("nombrePresidente", "PRESIDENTE");
                parametros.put("nombreSecretario", "SECRETARIO");
                parametros.put("nombreTesorero", "TESOREO");
                parametros.put("nombreVocal", "VOCAL");

                parametros.put("nombreProvinica", mesaSeleccionado.getRecinto().getUbicacion().getGeograp().getGeograp().getName());
                parametros.put("nombreCanton", mesaSeleccionado.getRecinto().getUbicacion().getGeograp().getName());
                parametros.put("nombreCanton", mesaSeleccionado.getRecinto().getUbicacion().getGeograp().getName());
                parametros.put("nombreParroquia", mesaSeleccionado.getRecinto().getUbicacion().getName());
                parametros.put("fechaActa", fechaActa);
                parametros.put("horaRegistro", JsfUtil.getHoraStringHHmmss(fechaActual));

                parametros.put("numeroJunta", mesaSeleccionado.getRecinto().getId().toString());
                parametros.put("numeroMesa", mesaSeleccionado.getId().toString());
                parametros.put("nombreRecinto", mesaSeleccionado.getRecinto().getNombre());

                parametros.put("fechaRegistro", JsfUtil.getFechaStringddMMYY(fechaActual));

            }
            return parametros;
        } catch (Exception e) {
            log.error("ERROR EN INICIALIZAR VARIABLES", e);
            return null;
        }
    }

    private String getPlantillaDocumento(String nombrePlantilla) {
        try {
            HashMap<String, String> parametros = getDatosActaE();
            //trae plantilla
            this.plantillaCorreoSeleccionado = plantillaCorreoFacade.buscarPorAsunto(nombrePlantilla);

            //Trae elimina llaves del texto
            this.plantillaCorreoSeleccionado.setMensaje(plantillaCorreoSeleccionado.getMensaje().replaceAll("\\{|\\}", ""));
            //RemplazaConstantes por variables
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

            float tamanioLetra = 10;//pnt
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

            //TABLA PARA FIRMAS DE RESPONSABILIDAD
            ReportePFD.agregaHTML(txtResponsableActaE, pathCss, fontProvider);

            ReportePFD.getFinalParagraph(loginBean.getUsuario().getUsername());
            //GUARDA REFERENCIA DE DOCUMENTO EN BD
            this.guardarDocumentoBD(documentoNuevo);
            ReportePFD.guardarDocumentosActasE(documentoActaE.getNombreReporte());
            //ReportePFD.descargarPDF(documentoActaE.getNombreReporte());
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
}
