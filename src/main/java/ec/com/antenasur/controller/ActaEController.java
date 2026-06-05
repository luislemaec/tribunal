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

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.FontProvider;

import ec.com.antenasur.bean.DocumentoBean;
import ec.com.antenasur.bean.GeograpBean;
import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.bean.ProcesoBean;
import ec.com.antenasur.dto.CandidatoDTO;
import ec.com.antenasur.dto.EscrutinioCabeceraDTO;
import ec.com.antenasur.dto.EscrutinioDTO;
import ec.com.antenasur.dto.MesaDTO;
import ec.com.antenasur.dto.MiembroJRVDTO;
import ec.com.antenasur.dto.RecintoDTO;
import ec.com.antenasur.enums.EstadoEscrutinio;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.itext.ReportePFD;
import ec.com.antenasur.itext.UtilHtml;
import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.tec.CatalogoGeneral;
import ec.com.antenasur.model.tec.CategoriaVoto;
import ec.com.antenasur.model.tec.Documentos;
import ec.com.antenasur.model.tec.Lista;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.model.tec.PlantillaCorreo;
import ec.com.antenasur.model.tec.TipoDocumento;
import ec.com.antenasur.report.ReportTemplateController;
import ec.com.antenasur.service.tec.CategoriaVotoService;
import ec.com.antenasur.service.tec.EscrutinioService;
import ec.com.antenasur.service.tec.ListaService;
import ec.com.antenasur.service.tec.MesaService;
import ec.com.antenasur.service.tec.MiembroJRVService;
import ec.com.antenasur.service.tec.PadronService;
import ec.com.antenasur.service.tec.ProcesoElectoralService;
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
    private ProcesoElectoralService procesoElectoralService;

    @Inject
    private GeograpBean geograpBean;

    @Inject
    private RecintoService recintoService;

    @Inject
    private MesaService mesaService;

    @Inject
    private MiembroJRVService miembroJRVService;

    @Inject
    private CategoriaVotoService categoriaVotoService;

    @Inject
    private EscrutinioService escrutinioService;

    @Inject
    private DocumentoBean documentoBean;

    @Inject
    private PadronService padronService;

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
    // siguen como entidades; sus DTOs se crean en la iteración de catálogos.
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
    private EscrutinioCabeceraDTO escrutinioCabecera;

    @Setter
    @Getter
    private ProcesoElectoral procesoActivo;

    @Setter
    @Getter
    private CandidatoDTO candidatoSeleccionado;

    @Setter
    @Getter
    private String cedulaBuscar;

    @Setter
    @Getter
    private String observacionApertura;

    @Setter
    @Getter
    private Integer totalSufragantesAsignados;

    @Getter
    private boolean accesoRestringidoPresidenteMesa;

    @Getter
    private boolean sinMesaAsignada;

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
        this.totalSufragantesAsignados = 0;
        this.observacionApertura = "";
        this.escrutinioCabecera = new EscrutinioCabeceraDTO();
    }

    private void cargaDatosIniciales() {
        this.procesoActivo = procesoElectoralService.getActivo();
        this.cantones = geograpBean.getByFatherId(7);
        this.listaRecintos = recintoService.listarDTOs();
        this.listaMesas = mesaService.listarDTOs();
        this.listas = listaService.findAll();
        this.categoriasVotos = categoriaVotoService.getCategoriasOrdenados();

        accesoRestringidoPresidenteMesa = esPresidenteMesa();
        MesaDTO mesaUsuario = obtenerMesaPorUsuario();
        if (accesoRestringidoPresidenteMesa && mesaUsuario == null) {
            sinMesaAsignada = true;
            JsfUtil.addWarningMessageFromBundle("actaE.mensaje.sin.mesa.asignada");
            return;
        }
        if (mesaUsuario != null) {
            mesaSeleccionado = mesaUsuario;
            recintoSeleccionado = mesaUsuario.getRecinto();
            listaMesas = new ArrayList<>();
            listaMesas.add(mesaUsuario);
            listaRecintos = new ArrayList<>();
            if (mesaUsuario.getRecinto() != null) {
                listaRecintos.add(mesaUsuario.getRecinto());
            }
            cargaDatosMesaSeleccionada();
        }
    }

    private MesaDTO obtenerMesaPorUsuario() {
        MesaDTO mesaPorJunta = obtenerMesaPorDesignacionJRV();
        if (mesaPorJunta != null) {
            return mesaPorJunta;
        }
        try {
            ec.com.antenasur.model.tec.Mesa m = mesaService.getMesaPorUsuario(loginBean.getUserName());
            return MesaDTO.fromEntity(m);
        } catch (Exception e) {
            return null;
        }
    }

    private MesaDTO obtenerMesaPorDesignacionJRV() {
        try {
            Integer personaId = loginBean != null && loginBean.getUsuario() != null
                    ? loginBean.getUsuario().getPersonaId() : null;
            Integer procesoId = procesoActivo != null ? procesoActivo.getId() : null;
            if (personaId == null || procesoId == null) {
                return null;
            }
            MiembroJRVDTO designacion = miembroJRVService.obtenerDesignacionPorPersonaProceso(personaId, procesoId);
            if (designacion == null || designacion.getMesa() == null
                    || !esCargoPresidenteMesa(designacion.getCargoNombre())) {
                return null;
            }
            sincronizarResponsableMesa(designacion.getMesa());
            return designacion.getMesa();
        } catch (Exception e) {
            log.warn("NO SE PUDO RESOLVER MESA POR DESIGNACION JRV", e);
            return null;
        }
    }

    private void sincronizarResponsableMesa(MesaDTO mesa) {
        if (mesa == null || mesa.getId() == null || loginBean == null
                || loginBean.getUserName() == null || loginBean.getUserName().isBlank()) {
            return;
        }
        if (!loginBean.getUserName().equals(mesa.getResponsable())) {
            MesaDTO mesaActualizada = mesaService.asignarResponsable(mesa.getId(), loginBean.getUserName());
            if (mesaActualizada != null) {
                mesa.setResponsable(mesaActualizada.getResponsable());
            }
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
        if (!puedeGestionarMesa(mesaSeleccionado.getId())) {
            limpiarActa();
            JsfUtil.addErrorMessageFromBundle("actaE.mensaje.mesa.no.autorizada");
            return;
        }
        mesaSeleccionado = mesaService.obtenerDTOPorId(mesaSeleccionado.getId());
        cargarTotalSufragantes();
        Integer procesoId = (procesoActivo != null) ? procesoActivo.getId() : null;
        List<Integer> categoriaIds = new ArrayList<>();
        if (categoriasVotos != null) {
            for (CategoriaVoto c : categoriasVotos) {
                categoriaIds.add(c.getId());
            }
        }
        this.listaCamposActaE = escrutinioService.prepararActaPorMesaDTO(
                mesaSeleccionado.getId(), procesoId, categoriaIds);
        escrutinioCabecera = escrutinioService.obtenerOCrearCabeceraDTO(
                mesaSeleccionado.getId(), procesoId, totalSufragantesAsignados);
        if (escrutinioCabecera != null && escrutinioCabecera.getObservacionApertura() != null) {
            observacionApertura = escrutinioCabecera.getObservacionApertura();
        }
        if (isMesaCerrada()) {
            JsfUtil.addInfoMessageFromBundle("actaE.mensaje.mesa.cerrada");
        }
    }

    public void guardaDatosMesaSeleccionada() {
        cerrarMesa();
    }

    public void registrarApertura() {
        try {
            if (!mesaSeleccionadaValida()) {
                JsfUtil.addWarningMessageFromBundle("actaE.mensaje.seleccione.mesa");
                return;
            }
            if (!puedeGestionarMesa(mesaSeleccionado.getId())) {
                JsfUtil.addErrorMessageFromBundle("actaE.mensaje.mesa.no.autorizada");
                return;
            }
            Integer procesoId = procesoActivo != null ? procesoActivo.getId() : null;
            escrutinioCabecera = escrutinioService.abrirEscrutinioDTO(mesaSeleccionado.getId(), procesoId,
                    loginBean.getUserName(), observacionApertura, totalSufragantesAsignados);
            procesoBean.okActivityRegister("APERTURA MESA " + mesaSeleccionado.getNombre(), mesaSeleccionado.getId().toString());
            JsfUtil.addSuccessMessageFromBundle("actaE.mensaje.apertura.ok");
            cargaDatosMesaSeleccionada();
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("ERROR AL REGISTRAR APERTURA DE MESA", e);
            JsfUtil.addErrorMessageFromBundle("actaE.mensaje.error");
        }
    }

    public void guardarBorradorConteo() {
        if (!validarOperacionConteo(false)) {
            return;
        }
        try {
            Integer procesoId = procesoActivo != null ? procesoActivo.getId() : null;
            escrutinioCabecera = escrutinioService.guardarBorradorConteoDTO(
                    mesaSeleccionado.getId(), procesoId, listaCamposActaE, totalSufragantesAsignados);
            procesoBean.okActivityRegister("GUARDA BORRADOR ACTA MESA " + mesaSeleccionado.getNombre(),
                    mesaSeleccionado.getId().toString());
            if (escrutinioCabecera != null && escrutinioCabecera.getObservacionConteo() != null
                    && !escrutinioCabecera.getObservacionConteo().isBlank()) {
                JsfUtil.addWarningMessage("Conteo guardado con observacion: " + escrutinioCabecera.getObservacionConteo());
            } else {
                JsfUtil.addSuccessMessageFromBundle("actaE.mensaje.borrador.ok");
            }
            cargaDatosMesaSeleccionada();
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("ERROR AL GUARDAR BORRADOR DE ACTA", e);
            JsfUtil.addErrorMessageFromBundle("actaE.mensaje.error");
        }
    }

    public void cerrarMesa() {
        if (this.listaCamposActaE == null || this.listaCamposActaE.isEmpty()
                || mesaSeleccionado == null || mesaSeleccionado.getId() == null) {
            return;
        }
        if (!validarOperacionConteo(true)) {
            return;
        }
        try {
            Integer procesoId = procesoActivo != null ? procesoActivo.getId() : null;
            escrutinioCabecera = escrutinioService.guardarBorradorConteoDTO(
                    mesaSeleccionado.getId(), procesoId, listaCamposActaE, totalSufragantesAsignados);
            ReportTemplateController documentoActaE = inicializaReporte();
            getListaStringDatos(documentoActaE);
            String observacion = escrutinioCabecera != null && escrutinioCabecera.getObservacionConteo() != null
                    ? escrutinioCabecera.getObservacionConteo() : "";
            exportaPDF(documentoActaE, observacion);

            MesaDTO mesaCerrada = escrutinioService.guardarActaCompletaDTO(
                    mesaSeleccionado.getId(), listaCamposActaE);
            if (mesaCerrada != null) {
                mesaSeleccionado = mesaCerrada;
            }
            escrutinioCabecera = escrutinioService.obtenerOCrearCabeceraDTO(
                    mesaSeleccionado.getId(), procesoId, totalSufragantesAsignados);
            JsfUtil.addSuccessMessageFromBundle("actaE.mensaje.cierre.ok");
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("ERROR AL CERRAR MESA", e);
            JsfUtil.addErrorMessageFromBundle("actaE.mensaje.pdf.error");
        }
    }

    public void generarActaMesaCerrada() {
        try {
            if (!mesaSeleccionadaValida()) {
                JsfUtil.addWarningMessageFromBundle("actaE.mensaje.seleccione.mesa");
                return;
            }
            if (!isMesaCerrada()) {
                JsfUtil.addWarningMessageFromBundle("actaE.mensaje.mesa.no.cerrada");
                return;
            }
            if (listaCamposActaE == null || listaCamposActaE.isEmpty()) {
                JsfUtil.addWarningMessageFromBundle("actaE.mensaje.sin.categorias");
                return;
            }
            ReportTemplateController documentoActaE = inicializaReporte();
            getListaStringDatos(documentoActaE);
            String observacion = escrutinioCabecera != null && escrutinioCabecera.getObservacionCierre() != null
                    ? escrutinioCabecera.getObservacionCierre() : "";
            exportaPDF(documentoActaE, observacion);
            JsfUtil.addSuccessMessageFromBundle("actaE.mensaje.pdf.regenerado");
        } catch (Exception e) {
            log.error("ERROR AL REGENERAR ACTA DE MESA CERRADA", e);
            JsfUtil.addErrorMessageFromBundle("actaE.mensaje.pdf.error.regenerar");
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
     * Construye el HashMap de parámetros del acta. Resuelve la cadena
     * provincia/cantón/parroquia por id contra GeograpBean en lugar de
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

    public int getTotalVotosRegistrados() {
        return escrutinioService.calcularTotalVotos(listaCamposActaE);
    }

    public int getDiferenciaConteo() {
        return (totalSufragantesAsignados != null ? totalSufragantesAsignados : 0) - getTotalVotosRegistrados();
    }

    public boolean isMesaSeleccionadaValida() {
        return mesaSeleccionadaValida();
    }

    public boolean isMesaAbierta() {
        return mesaSeleccionadaValida() && escrutinioCabecera != null
                && (EstadoEscrutinio.ABIERTO.equals(escrutinioCabecera.getEstadoEscrutinio())
                || EstadoEscrutinio.EN_CONTEO.equals(escrutinioCabecera.getEstadoEscrutinio())
                || EstadoEscrutinio.CONTEO_REGISTRADO.equals(escrutinioCabecera.getEstadoEscrutinio())
                || EstadoEscrutinio.OBSERVADO.equals(escrutinioCabecera.getEstadoEscrutinio()));
    }

    public boolean isMesaCerrada() {
        return mesaSeleccionadaValida() && escrutinioCabecera != null
                && EstadoEscrutinio.CERRADO.equals(escrutinioCabecera.getEstadoEscrutinio());
    }

    public boolean isPuedeRegistrarApertura() {
        return mesaSeleccionadaValida()
                && !sinMesaAsignada
                && !isMesaAbierta()
                && !isMesaCerrada();
    }

    public boolean isPuedeEditarConteo() {
        return mesaSeleccionadaValida() && isMesaAbierta() && !isMesaCerrada();
    }

    public boolean isPuedeCerrarMesa() {
        return isPuedeEditarConteo() && getTotalVotosRegistrados() == (totalSufragantesAsignados != null ? totalSufragantesAsignados : 0);
    }

    public String getEstadoValidacionCierreTexto() {
        if (isMesaCerrada()) {
            return JsfUtil.getProperty("actaE.validacion.cerrada", true);
        }
        return isPuedeCerrarMesa()
                ? JsfUtil.getProperty("actaE.validacion.lista", true)
                : JsfUtil.getProperty("actaE.validacion.pendiente", true);
    }

    public String getEstadoValidacionCierreSeverity() {
        if (isMesaCerrada()) {
            return "success";
        }
        return isPuedeCerrarMesa() ? "success" : "warning";
    }

    public String getEstadoMesaTexto() {
        if (isMesaCerrada()) {
            return JsfUtil.getProperty("actaE.estado.cerrada", true);
        }
        if (isMesaAbierta()) {
            if (escrutinioCabecera != null && EstadoEscrutinio.OBSERVADO.equals(escrutinioCabecera.getEstadoEscrutinio())) {
                return JsfUtil.getProperty("actaE.estado.observada", true);
            }
            return getTotalVotosRegistrados() > 0
                    ? JsfUtil.getProperty("actaE.estado.en.conteo", true)
                    : JsfUtil.getProperty("actaE.estado.abierta", true);
        }
        return JsfUtil.getProperty("actaE.estado.pendiente", true);
    }

    public String getEstadoMesaSeverity() {
        if (isMesaCerrada()) {
            return "success";
        }
        if (isMesaAbierta()) {
            return "warning";
        }
        return "secondary";
    }

    public String getProcesoActivoNombre() {
        return procesoActivo != null ? procesoActivo.getNombre() : "";
    }

    private boolean validarOperacionConteo(boolean cierreFinal) {
        if (!mesaSeleccionadaValida()) {
            JsfUtil.addWarningMessageFromBundle("actaE.mensaje.seleccione.mesa");
            return false;
        }
        if (!puedeGestionarMesa(mesaSeleccionado.getId())) {
            JsfUtil.addErrorMessageFromBundle("actaE.mensaje.mesa.no.autorizada");
            return false;
        }
        if (!isMesaAbierta()) {
            JsfUtil.addWarningMessageFromBundle("actaE.mensaje.apertura.requerida");
            return false;
        }
        if (listaCamposActaE == null || listaCamposActaE.isEmpty()) {
            JsfUtil.addWarningMessageFromBundle("actaE.mensaje.sin.categorias");
            return false;
        }
        for (EscrutinioDTO item : listaCamposActaE) {
            if (item.getTotalVotos() != null && item.getTotalVotos() < 0) {
                JsfUtil.addErrorMessageFromBundle("actaE.mensaje.votos.negativos");
                return false;
            }
        }
        if (getTotalVotosRegistrados() > (totalSufragantesAsignados != null ? totalSufragantesAsignados : 0)) {
            JsfUtil.addErrorMessageFromBundle("actaE.mensaje.total.excede");
            return false;
        }
        if (cierreFinal && getDiferenciaConteo() != 0) {
            JsfUtil.addErrorMessageFromBundle("actaE.mensaje.total.no.cuadra");
            return false;
        }
        return true;
    }

    private boolean puedeGestionarMesa(Integer mesaId) {
        if (!accesoRestringidoPresidenteMesa) {
            return true;
        }
        MesaDTO mesaUsuario = obtenerMesaPorUsuario();
        return mesaUsuario != null && mesaUsuario.getId() != null && mesaUsuario.getId().equals(mesaId);
    }

    private boolean esPresidenteMesa() {
        return loginBean != null && loginBean.getRoles() != null
                && loginBean.getRoles().contains("SITEC-Presidente-mesa");
    }

    private boolean esCargoPresidenteMesa(String cargoNombre) {
        return cargoNombre != null && cargoNombre.trim().toUpperCase().contains("PRESIDENTE");
    }

    private boolean mesaSeleccionadaValida() {
        return mesaSeleccionado != null && mesaSeleccionado.getId() != null;
    }

    private void cargarTotalSufragantes() {
        if (!mesaSeleccionadaValida()) {
            totalSufragantesAsignados = 0;
            return;
        }
        Integer procesoId = procesoActivo != null ? procesoActivo.getId() : null;
        if (procesoId == null) {
            totalSufragantesAsignados = 0;
            return;
        }
        totalSufragantesAsignados = padronService.contarSufragantesPorMesaYProceso(
                mesaSeleccionado.getId(), procesoId);
    }

    private void limpiarActa() {
        listaCamposActaE = new ArrayList<>();
        mesaSeleccionado = new MesaDTO();
        escrutinioCabecera = new EscrutinioCabeceraDTO();
        totalSufragantesAsignados = 0;
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

    public String exportaPDF(ReportTemplateController documentoActaE, String observacion) throws Exception {
        String txtContenidoActaE = getPlantillaDocumento("BIENVENIDO");
        String txtResponsableActaE = getPlantillaDocumento("RESPONSABLES ACTA ESCRUTINIOS");
        if (txtContenidoActaE == null || txtContenidoActaE.isBlank()
                || txtResponsableActaE == null || txtResponsableActaE.isBlank()) {
            throw new IllegalStateException("No se pudo resolver la plantilla del acta de escrutinio.");
        }

        String extencion = ".pdf";
        String pathCompleto = Constantes.getPathActaEscrutinio(documentoActaE.getNombreReporte());

        Documentos documentoNuevo = new Documentos(documentoActaE.getNombreReporte(), pathCompleto, new TipoDocumento(Constantes.ACTA_ESCRUTINIO),
                mesaSeleccionado.getId(), extencion, "application/pdf", documentoActaE.getNombreReporte());

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
        if (observacion != null && !observacion.isBlank()) {
            ReportePFD.agregaParrafoObservacion(observacion);
        }
        ReportePFD.agregaHTML(txtResponsableActaE, pathCss, fontProvider);
        ReportePFD.getFinalParagraph(loginBean.getUsuario().getUsername());
        String archivoGenerado = ReportePFD.guardarDocumentosActasEObligatorio(documentoActaE.getNombreReporte());
        this.guardarDocumentoBD(documentoNuevo);
        procesoBean.okActivityRegister("GENERA " + documentoActaE.getNombreReporte(), documentoActaE.getNombreReporte() + ".pdf");
        return archivoGenerado;
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
