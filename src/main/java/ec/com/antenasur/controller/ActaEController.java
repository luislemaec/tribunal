package ec.com.antenasur.controller;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import ec.com.antenasur.dto.ActaEGerencialDTO;
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
    private List<Geograp> provincias, cantones, parroquias;

    @Setter
    @Getter
    private Geograp cantonSeleccionado, parroquiaSeleccionado;

    @Setter
    @Getter
    private Integer provinciaFiltroId;

    @Setter
    @Getter
    private Integer procesoConsultaId;

    @Setter
    @Getter
    private EstadoEscrutinio estadoFiltro;

    @Setter
    @Getter
    private List<EstadoEscrutinio> estadosEscrutinio;

    @Setter
    @Getter
    private List<ProcesoElectoral> procesosElectorales;

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
    private List<Documentos> documentosActa;

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
    private String motivoCambioEstado;

    @Setter
    @Getter
    private Integer totalSufragantesAsignados;

    @Getter
    private boolean accesoRestringidoPresidenteMesa;

    @Getter
    private boolean sinMesaAsignada;

    @Getter
    private boolean usuarioConsultaGerencial;

    @Setter
    @Getter
    private int tabActivoActaE;

    @Setter
    @Getter
    private List<ActaEGerencialDTO> listaConsultaGerencial;

    @Getter
    private int totalMesasGerencial;

    @Getter
    private int mesasPendientesGerencial;

    @Getter
    private int mesasAbiertasGerencial;

    @Getter
    private int mesasConteoGerencial;

    @Getter
    private int mesasCerradasGerencial;

    @Getter
    private int mesasObservadasGerencial;

    @Getter
    private int totalSufragantesGerencial;

    @Getter
    private int totalVotosRegistradosGerencial;

    @Getter
    private int totalVotosValidosGerencial;

    @Getter
    private int totalVotosBlancosGerencial;

    @Getter
    private int totalVotosNulosGerencial;

    @Getter
    private int totalActasGeneradasGerencial;

    @Getter
    private int totalActasPendientesGerencial;

    @PostConstruct
    private void init() {
        inicializaVariables();
        cargaDatosIniciales();
    }

    private void inicializaVariables() {
        this.listaCamposActaE = new ArrayList<>();
        this.documentosActa = new ArrayList<>();
        this.listaConsultaGerencial = new ArrayList<>();
        this.cantonSeleccionado = new Geograp();
        this.parroquiaSeleccionado = new Geograp();
        this.recintoSeleccionado = new RecintoDTO();
        this.mesaSeleccionado = new MesaDTO();
        this.totalSufragantesAsignados = 0;
        this.observacionApertura = "";
        this.motivoCambioEstado = "";
        this.escrutinioCabecera = new EscrutinioCabeceraDTO();
        this.estadosEscrutinio = List.of(EstadoEscrutinio.values());
        limpiarResumenGerencial();
    }

    private void cargaDatosIniciales() {
        this.procesoActivo = procesoElectoralService.getActivo();
        this.procesosElectorales = procesoElectoralService.findAll();
        this.procesoConsultaId = procesoActivo != null ? procesoActivo.getId() : null;
        cargarProvincias();
        this.listaRecintos = new ArrayList<>();
        this.listaMesas = new ArrayList<>();
        this.listas = listaService.findAll();
        this.categoriasVotos = categoriaVotoService.getCategoriasOrdenados();

        accesoRestringidoPresidenteMesa = esPresidenteMesa();
        usuarioConsultaGerencial = !accesoRestringidoPresidenteMesa && tieneRolConsultaGerencial();
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

    private void cargarProvincias() {
        provincias = new ArrayList<>();
        cantones = new ArrayList<>();
        parroquias = new ArrayList<>();
        try {
            Geograp provRef = geograpBean.getById(7);
            if (provRef != null && provRef.getGeograp() != null) {
                provincias = geograpBean.getByFatherId(provRef.getGeograp().getId());
            }
        } catch (Exception e) {
            log.warn("NO SE PUDO CARGAR PROVINCIAS PARA CONSULTA DE ACTAS", e);
        }
        if (provincias == null) {
            provincias = new ArrayList<>();
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
            limpiarSeleccionMesa();
            parroquias = new ArrayList<>();
            listaRecintos = new ArrayList<>();
            listaMesas = new ArrayList<>();
            if (cantonSeleccionado.getId() != null) {
                this.cantonSeleccionado = geograpBean.getById(this.cantonSeleccionado.getId());
                this.parroquias = geograpBean.getByFatherGeograp(this.cantonSeleccionado);
            }
        } catch (Exception e) {
            log.warn("NO SE PUDO CARGAR PARROQUIAS", e);
        }
    }

    public void cargaCantonesPorProvincia() {
        if (!usuarioConsultaGerencial) {
            return;
        }
        limpiarSeleccionMesa();
        cantonSeleccionado = new Geograp();
        parroquiaSeleccionado = new Geograp();
        cantones = new ArrayList<>();
        parroquias = new ArrayList<>();
        listaRecintos = new ArrayList<>();
        listaMesas = new ArrayList<>();
        if (provinciaFiltroId != null) {
            List<Geograp> hijos = geograpBean.getByFatherId(provinciaFiltroId);
            cantones = hijos != null ? hijos : new ArrayList<>();
        }
    }

    public void cargaRecintosPorParroquias() {
        try {
            limpiarSeleccionMesa();
            listaRecintos = new ArrayList<>();
            listaMesas = new ArrayList<>();
            List<Geograp> litaParroquiasTmp = new ArrayList<>();
            if (this.parroquiaSeleccionado != null && this.parroquiaSeleccionado.getId() != null) {
                this.parroquiaSeleccionado = geograpBean.getById(this.parroquiaSeleccionado.getId());
                litaParroquiasTmp.add(this.parroquiaSeleccionado);
                this.listaRecintos = recintoService.listarDTOsPorParroquias(litaParroquiasTmp);
            } else if (this.parroquias != null && !this.parroquias.isEmpty()) {
                this.listaRecintos = recintoService.listarDTOsPorParroquias(this.parroquias);
            }
        } catch (Exception e) {
            log.warn("NO SE PUDO CARGAR RECINTOS", e);
        }
    }

    public void cargaMesasPorRecintos() {
        limpiarSeleccionMesa();
        listaMesas = new ArrayList<>();
        if (recintoSeleccionado != null && recintoSeleccionado.getId() != null) {
            recintoSeleccionado = recintoService.obtenerDTOPorId(recintoSeleccionado.getId());
            this.listaMesas = filtrarMesasPorRecintoId(mesaService.listarDTOs(), recintoSeleccionado.getId());
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
            limpiarSeleccionMesa();
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
        cargarDocumentosActa();
    }

    public void consultarEscrutiniosGerenciales() {
        if (!usuarioConsultaGerencial) {
            listaConsultaGerencial = new ArrayList<>();
            limpiarResumenGerencial();
            JsfUtil.addErrorMessageFromBundle("actaE.mensaje.accesoDenegado");
            return;
        }
        Integer procesoId = procesoConsultaId != null ? procesoConsultaId
                : (procesoActivo != null ? procesoActivo.getId() : null);
        if (procesoId == null) {
            listaConsultaGerencial = new ArrayList<>();
            limpiarResumenGerencial();
            JsfUtil.addWarningMessageFromBundle("actaE.mensaje.sin.proceso");
            return;
        }
        List<ActaEGerencialDTO> resultado = new ArrayList<>();
        for (MesaDTO mesa : obtenerMesasFiltradas()) {
            ActaEGerencialDTO fila = construirFilaGerencial(mesa, procesoId);
            if (estadoFiltro == null || estadoFiltro.equals(fila.getEstadoEscrutinio())) {
                resultado.add(fila);
            }
        }
        listaConsultaGerencial = resultado;
        calcularResumenGerencial();
        if (listaConsultaGerencial.isEmpty()) {
            JsfUtil.addInfoMessageFromBundle("actaE.mensaje.sinResultados");
        }
    }

    public void limpiarFiltrosGerenciales() {
        if (!usuarioConsultaGerencial) {
            return;
        }
        provinciaFiltroId = null;
        cantonSeleccionado = new Geograp();
        parroquiaSeleccionado = new Geograp();
        recintoSeleccionado = new RecintoDTO();
        mesaSeleccionado = new MesaDTO();
        estadoFiltro = null;
        procesoConsultaId = procesoActivo != null ? procesoActivo.getId() : null;
        cantones = new ArrayList<>();
        parroquias = new ArrayList<>();
        listaRecintos = new ArrayList<>();
        listaMesas = new ArrayList<>();
        listaConsultaGerencial = new ArrayList<>();
        limpiarSeleccionMesa();
        limpiarResumenGerencial();
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
            cargarDocumentosActa();
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
            if (!puedeGestionarMesa(mesaSeleccionado.getId())) {
                JsfUtil.addErrorMessageFromBundle("actaE.mensaje.mesa.no.autorizada");
                return;
            }
            cargarDocumentosActa();
            if (!isPuedeGenerarActaPdf()) {
                if (!isMesaCerrada()) {
                    JsfUtil.addWarningMessageFromBundle("actaE.mensaje.mesa.no.cerrada");
                } else {
                    JsfUtil.addInfoMessageFromBundle("actaE.mensaje.pdf.ya.existe");
                }
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
            cargarDocumentosActa();
            procesoBean.okActivityRegister("REGENERA ACTA PDF " + documentoActaE.getNombreReporte(),
                    "MESA " + mesaSeleccionado.getId());
            JsfUtil.addSuccessMessageFromBundle("actaE.mensaje.pdf.regenerado");
        } catch (Exception e) {
            log.error("ERROR AL REGENERAR ACTA DE MESA CERRADA", e);
            JsfUtil.addErrorMessageFromBundle("actaE.mensaje.pdf.error.regenerar");
        }
    }

    public void observarEscrutinio() {
        cambiarEstadoAdministrativo(EstadoEscrutinio.OBSERVADO, "actaE.mensaje.observar.ok");
    }

    public void anularEscrutinio() {
        cambiarEstadoAdministrativo(EstadoEscrutinio.ANULADO, "actaE.mensaje.anular.ok");
    }

    public void reabrirEscrutinio() {
        cambiarEstadoAdministrativo(EstadoEscrutinio.REABIERTO, "actaE.mensaje.reabrir.ok");
    }

    private void cambiarEstadoAdministrativo(EstadoEscrutinio estadoNuevo, String mensajeOk) {
        try {
            if (!mesaSeleccionadaValida()) {
                JsfUtil.addWarningMessageFromBundle("actaE.mensaje.seleccione.mesa");
                return;
            }
            if (!puedeCambiarEstadoAdministrativo(estadoNuevo)) {
                JsfUtil.addErrorMessageFromBundle("actaE.mensaje.accesoDenegado");
                return;
            }
            if (requiereMotivo(estadoNuevo) && (motivoCambioEstado == null || motivoCambioEstado.trim().isEmpty())) {
                JsfUtil.addWarningMessageFromBundle("actaE.mensaje.motivo.requerido");
                return;
            }
            EstadoEscrutinio estadoAnterior = escrutinioCabecera != null
                    ? escrutinioCabecera.getEstadoEscrutinio() : null;
            Integer procesoId = procesoActivo != null ? procesoActivo.getId() : null;
            escrutinioCabecera = escrutinioService.cambiarEstadoCabeceraDTO(
                    mesaSeleccionado.getId(), procesoId, estadoNuevo, motivoCambioEstado);
            auditarCambioEstado(estadoAnterior, estadoNuevo, motivoCambioEstado);
            motivoCambioEstado = "";
            JsfUtil.addSuccessMessageFromBundle(mensajeOk);
            cargaDatosMesaSeleccionada();
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("ERROR AL CAMBIAR ESTADO DE ESCRUTINIO", e);
            JsfUtil.addErrorMessageFromBundle("actaE.mensaje.estado.error");
        }
    }

    private ReportTemplateController inicializaReporte() {
        Integer procesoId = procesoActivo != null ? procesoActivo.getId() : 0;
        return new ReportTemplateController(
                "ACTA-" + procesoId + "-M" + mesaSeleccionado.getId() + "-" + JsfUtil.getFechaStringYYYYMMddHHmm(new Date()),
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
            cargarResponsablesJRV(parametros);

            Geograp parroquia = (obtenerParroquiaId(mesaSeleccionado) != null)
                    ? geograpBean.getById(obtenerParroquiaId(mesaSeleccionado)) : null;
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

    private void cargarResponsablesJRV(HashMap<String, String> parametros) {
        inicializarResponsablesJRV(parametros);
        Integer mesaId = mesaSeleccionado != null ? mesaSeleccionado.getId() : null;
        Integer procesoId = procesoActivo != null ? procesoActivo.getId() : null;
        if (mesaId == null || procesoId == null) {
            return;
        }
        List<MiembroJRVDTO> miembros = miembroJRVService.listarDTOsPorMesaProceso(mesaId, procesoId);
        if (miembros == null) {
            return;
        }
        for (MiembroJRVDTO miembro : miembros) {
            if (miembro == null || miembro.getCargoNombre() == null) {
                continue;
            }
            String cargo = miembro.getCargoNombre().trim().toUpperCase();
            if (cargo.contains("PRESIDENTE")) {
                asignarResponsableJRV(parametros, "Presidente", miembro);
            } else if (cargo.contains("SECRETARIO")) {
                asignarResponsableJRV(parametros, "Secretario", miembro);
            } else if (cargo.contains("TESORERO") || cargo.contains("TESOREO")) {
                asignarResponsableJRV(parametros, "Tesorero", miembro);
            } else if (cargo.contains("VOCAL")) {
                asignarResponsableJRV(parametros, "Vocal", miembro);
            }
        }
    }

    private void inicializarResponsablesJRV(HashMap<String, String> parametros) {
        asignarResponsableVacio(parametros, "Presidente");
        asignarResponsableVacio(parametros, "Secretario");
        asignarResponsableVacio(parametros, "Tesorero");
        asignarResponsableVacio(parametros, "Vocal");
        // Alias historico con error ortografico para plantillas existentes.
        parametros.put("nombreTesoreo", "");
        parametros.put("documentoTesoreo", "");
        parametros.put("cargoTesoreo", "TESORERO");
    }

    private void asignarResponsableVacio(HashMap<String, String> parametros, String dignidad) {
        parametros.put("nombre" + dignidad, "");
        parametros.put("documento" + dignidad, "");
        parametros.put("cargo" + dignidad, dignidad.toUpperCase());
        parametros.put("iglesia" + dignidad, "");
    }

    private void asignarResponsableJRV(HashMap<String, String> parametros, String dignidad, MiembroJRVDTO miembro) {
        String nombre = "";
        String documento = "";
        String iglesia = "";
        if (miembro.getIglesiaPersona() != null) {
            if (miembro.getIglesiaPersona().getPersona() != null) {
                nombre = nombreCompletoPersona(miembro.getIglesiaPersona().getPersona());
                documento = textoNulo(miembro.getIglesiaPersona().getPersona().getDocumento());
            }
            if (miembro.getIglesiaPersona().getIglesia() != null) {
                iglesia = textoNulo(miembro.getIglesiaPersona().getIglesia().getNombre());
            }
        }
        parametros.put("nombre" + dignidad, nombre);
        parametros.put("documento" + dignidad, documento);
        parametros.put("cargo" + dignidad, textoNulo(miembro.getCargoNombre()));
        parametros.put("iglesia" + dignidad, iglesia);
        if ("Tesorero".equals(dignidad)) {
            parametros.put("nombreTesoreo", nombre);
            parametros.put("documentoTesoreo", documento);
            parametros.put("cargoTesoreo", textoNulo(miembro.getCargoNombre()));
        }
    }

    private String nombreCompletoPersona(ec.com.antenasur.dto.PersonaDTO persona) {
        if (persona == null) {
            return "";
        }
        String nombres = textoNulo(persona.getNombres());
        String apellidos = textoNulo(persona.getApellidos());
        return (nombres + " " + apellidos).trim();
    }

    private String textoNulo(String valor) {
        return valor != null ? valor.trim() : "";
    }

    public String getDirectorioActasEscrutinio() {
        return Constantes.getDirectorioActasEscrutinio();
    }

    public boolean isPuedeGenerarActaPdf() {
        return isMesaCerrada() && getDocumentoActaValido() == null;
    }

    public boolean isActaPdfDisponible() {
        return getDocumentoActaValido() != null;
    }

    public boolean isActaPdfRegistradaNoDisponible() {
        return isMesaCerrada() && documentosActa != null && !documentosActa.isEmpty() && getDocumentoActaValido() == null;
    }

    public Documentos getDocumentoActaValido() {
        if (documentosActa == null || documentosActa.isEmpty()) {
            return null;
        }
        for (int i = documentosActa.size() - 1; i >= 0; i--) {
            Documentos documento = documentosActa.get(i);
            if (esDocumentoActaValido(documento)) {
                return documento;
            }
        }
        return null;
    }

    public boolean esDocumentoActaValido(Documentos documento) {
        if (documento == null || documento.getPath() == null || documento.getPath().isBlank()) {
            return false;
        }
        try {
            Path path = Paths.get(documento.getPath()).toAbsolutePath().normalize();
            if (!Files.isRegularFile(path)) {
                return false;
            }
            if (documento.getHashSha256() == null || documento.getHashSha256().isBlank()) {
                return true;
            }
            String hashActual = ReportePFD.calcularSha256(path);
            return documento.getHashSha256().equalsIgnoreCase(hashActual);
        } catch (Exception e) {
            log.warn("NO SE PUDO VALIDAR DOCUMENTO DE ACTA {}", documento.getPath(), e);
            return false;
        }
    }

    public boolean existeArchivoDocumento(Documentos documento) {
        if (documento == null || documento.getPath() == null || documento.getPath().isBlank()) {
            return false;
        }
        return Files.isRegularFile(Paths.get(documento.getPath()).toAbsolutePath().normalize());
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
                || EstadoEscrutinio.REABIERTO.equals(escrutinioCabecera.getEstadoEscrutinio()));
    }

    public boolean isMesaCerrada() {
        return mesaSeleccionadaValida() && escrutinioCabecera != null
                && EstadoEscrutinio.CERRADO.equals(escrutinioCabecera.getEstadoEscrutinio());
    }

    public boolean isPuedeRegistrarApertura() {
        return mesaSeleccionadaValida()
                && isPuedeOperarActa()
                && !sinMesaAsignada
                && !isMesaAbierta()
                && !isMesaCerrada();
    }

    public boolean isPuedeEditarConteo() {
        return mesaSeleccionadaValida() && isPuedeOperarActa() && isMesaAbierta() && !isMesaCerrada();
    }

    public boolean isPuedeCerrarMesa() {
        return isPuedeEditarConteo()
                && escrutinioCabecera != null
                && (EstadoEscrutinio.CONTEO_REGISTRADO.equals(escrutinioCabecera.getEstadoEscrutinio())
                || EstadoEscrutinio.REABIERTO.equals(escrutinioCabecera.getEstadoEscrutinio()))
                && getTotalVotosRegistrados() == (totalSufragantesAsignados != null ? totalSufragantesAsignados : 0);
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
        if (escrutinioCabecera != null && EstadoEscrutinio.OBSERVADO.equals(escrutinioCabecera.getEstadoEscrutinio())) {
            return JsfUtil.getProperty("actaE.estado.observada", true);
        }
        if (escrutinioCabecera != null && EstadoEscrutinio.ANULADO.equals(escrutinioCabecera.getEstadoEscrutinio())) {
            return JsfUtil.getProperty("actaE.estado.anulada", true);
        }
        if (escrutinioCabecera != null && EstadoEscrutinio.REABIERTO.equals(escrutinioCabecera.getEstadoEscrutinio())) {
            return JsfUtil.getProperty("actaE.estado.reabierta", true);
        }
        if (isMesaAbierta()) {
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
        if (escrutinioCabecera != null && (EstadoEscrutinio.OBSERVADO.equals(escrutinioCabecera.getEstadoEscrutinio())
                || EstadoEscrutinio.ANULADO.equals(escrutinioCabecera.getEstadoEscrutinio()))) {
            return "danger";
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
            return tieneRolOperacionActa();
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

    public boolean isPuedeOperarActa() {
        return !sinMesaAsignada && (accesoRestringidoPresidenteMesa || tieneRolOperacionActa());
    }

    public boolean isPuedeObservarEscrutinio() {
        return mesaSeleccionadaValida() && tieneRolSupervisorOAdministrador()
                && escrutinioCabecera != null
                && !EstadoEscrutinio.CERRADO.equals(escrutinioCabecera.getEstadoEscrutinio())
                && !EstadoEscrutinio.ANULADO.equals(escrutinioCabecera.getEstadoEscrutinio())
                && !EstadoEscrutinio.OBSERVADO.equals(escrutinioCabecera.getEstadoEscrutinio());
    }

    public boolean isPuedeAnularEscrutinio() {
        return mesaSeleccionadaValida() && tieneRolAdministrador()
                && escrutinioCabecera != null
                && !EstadoEscrutinio.CERRADO.equals(escrutinioCabecera.getEstadoEscrutinio())
                && !EstadoEscrutinio.ANULADO.equals(escrutinioCabecera.getEstadoEscrutinio());
    }

    public boolean isPuedeReabrirEscrutinio() {
        return mesaSeleccionadaValida() && tieneRolAdministrador()
                && escrutinioCabecera != null
                && (EstadoEscrutinio.CERRADO.equals(escrutinioCabecera.getEstadoEscrutinio())
                || EstadoEscrutinio.OBSERVADO.equals(escrutinioCabecera.getEstadoEscrutinio())
                || EstadoEscrutinio.ANULADO.equals(escrutinioCabecera.getEstadoEscrutinio()));
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

    private void cargarDocumentosActa() {
        documentosActa = new ArrayList<>();
        if (!mesaSeleccionadaValida()) {
            return;
        }
        List<Documentos> documentos = documentoBean.getDocumentosPorEntidadYTipoDoc(
                mesaSeleccionado.getId(), Constantes.ACTA_ESCRUTINIO);
        if (documentos != null) {
            for (Documentos documento : documentos) {
                if (esDocumentoDelProcesoActivo(documento)) {
                    documentosActa.add(documento);
                }
            }
        }
    }

    private boolean esDocumentoDelProcesoActivo(Documentos documento) {
        if (documento == null || procesoActivo == null || procesoActivo.getId() == null
                || mesaSeleccionado == null || mesaSeleccionado.getId() == null) {
            return false;
        }
        return esDocumentoDelProceso(documento, mesaSeleccionado.getId(), procesoActivo.getId());
    }

    private void limpiarActa() {
        listaCamposActaE = new ArrayList<>();
        documentosActa = new ArrayList<>();
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
        cargarDocumentosActa();
        if (getDocumentoActaValido() != null) {
            throw new IllegalStateException("El acta PDF ya fue generada y validada para esta mesa.");
        }
        String txtContenidoActaE = getPlantillaDocumento("BIENVENIDO");
        String txtResponsableActaE = getPlantillaDocumento("RESPONSABLES ACTA ESCRUTINIOS");
        if (txtContenidoActaE == null || txtContenidoActaE.isBlank()
                || txtResponsableActaE == null || txtResponsableActaE.isBlank()) {
            throw new IllegalStateException("No se pudo resolver la plantilla del acta de escrutinio.");
        }

        String extencion = ".pdf";
        String pathCompleto = Constantes.getPathActaEscrutinio(documentoActaE.getNombreReporte());
        String codigoActa = documentoActaE.getNombreReporte();

        Documentos documentoNuevo = new Documentos(documentoActaE.getNombreReporte(), pathCompleto, new TipoDocumento(Constantes.ACTA_ESCRUTINIO),
                mesaSeleccionado.getId(), extencion, "application/pdf", codigoActa);

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
            ReportePFD.creaTablaCabecera(documentoActaE.getNumeroColumnas(), documentoActaE.getTamanioColumnasPDF(),
                    "RESULTADOS DEL ESCRUTINIO", documentoActaE.getNombresColumnas(), fuenteCabecerta);
        ReportePFD.creaContenidoTabla(documentoActaE.getListaDatos(), documentoActaE.getNombresColumnas(), fuenteContenido);
        ReportePFD.agregaParrafoEnBlanco();
        if (observacion != null && !observacion.isBlank()) {
            ReportePFD.agregaParrafoObservacion(observacion);
        }
        ReportePFD.agregaHTML(txtResponsableActaE, pathCss, fontProvider);
        ReportePFD.agregaCodigoVerificacion(codigoActa, construirContenidoQr(codigoActa));
        ReportePFD.cerrarDocumento();
        documentoNuevo.setHashSha256(ReportePFD.calcularHashSha256Actual());
        String archivoGenerado = ReportePFD.guardarDocumentosActasEObligatorio(documentoActaE.getNombreReporte());
        documentoNuevo.setPath(archivoGenerado);
        this.guardarDocumentoBD(documentoNuevo);
        procesoBean.okActivityRegister("GENERA " + documentoActaE.getNombreReporte(), documentoActaE.getNombreReporte() + ".pdf");
        return archivoGenerado;
    }

    private Documentos guardarDocumentoBD(Documentos documentoNuevo) {
        Documentos documentoPersistido = documentoBean.guardarDocumentoPersistido(documentoNuevo);
        if (documentoPersistido == null || documentoPersistido.getId() == null) {
            throw new IllegalStateException("No se pudo registrar el documento generado.");
        }
        return documentoPersistido;
    }

    private String construirContenidoQr(String codigoActa) {
        StringBuilder contenido = new StringBuilder();
        contenido.append("codigo=").append(codigoActa);
        contenido.append(";proceso=").append(procesoActivo != null ? procesoActivo.getId() : "");
        contenido.append(";mesa=").append(mesaSeleccionado != null ? mesaSeleccionado.getId() : "");
        contenido.append(";recinto=").append(mesaSeleccionado != null && mesaSeleccionado.getRecinto() != null
                ? mesaSeleccionado.getRecinto().getId() : "");
        contenido.append(";fecha=").append(JsfUtil.getFechaStringYYYYMMddHHmm(new Date()));
        contenido.append(";usuario=").append(loginBean != null ? loginBean.getUserName() : "");
        return contenido.toString();
    }

    private List<MesaDTO> obtenerMesasFiltradas() {
        if (!usuarioConsultaGerencial) {
            return Collections.emptyList();
        }
        List<MesaDTO> mesas = mesaService.listarDTOs();
        List<MesaDTO> resultado = new ArrayList<>();
        if (mesas == null) {
            return resultado;
        }
        Integer mesaIdFiltro = mesaSeleccionado != null ? mesaSeleccionado.getId() : null;
        Integer recintoIdFiltro = recintoSeleccionado != null ? recintoSeleccionado.getId() : null;
        Integer parroquiaIdFiltro = parroquiaSeleccionado != null ? parroquiaSeleccionado.getId() : null;
        Integer cantonIdFiltro = cantonSeleccionado != null ? cantonSeleccionado.getId() : null;
        Map<Integer, Geograp> cacheCantones = new HashMap<>();
        for (MesaDTO mesa : mesas) {
            if (mesa == null || mesa.getId() == null) {
                continue;
            }
            if (mesaIdFiltro != null && !mesaIdFiltro.equals(mesa.getId())) {
                continue;
            }
            RecintoDTO recinto = mesa.getRecinto();
            if (recintoIdFiltro != null && (recinto == null || !recintoIdFiltro.equals(recinto.getId()))) {
                continue;
            }
            Integer parroquiaIdMesa = obtenerParroquiaId(mesa);
            if (parroquiaIdFiltro != null && !parroquiaIdFiltro.equals(parroquiaIdMesa)) {
                continue;
            }
            Integer cantonIdMesa = obtenerCantonId(mesa);
            if (cantonIdFiltro != null && !cantonIdFiltro.equals(cantonIdMesa)) {
                continue;
            }
            if (provinciaFiltroId != null && !provinciaFiltroId.equals(obtenerProvinciaId(cantonIdMesa, cacheCantones))) {
                continue;
            }
            resultado.add(mesa);
        }
        return resultado;
    }

    private ActaEGerencialDTO construirFilaGerencial(MesaDTO mesa, Integer procesoId) {
        ActaEGerencialDTO fila = new ActaEGerencialDTO();
        fila.setMesaId(mesa.getId());
        fila.setMesa(textoNulo(mesa.getNombre()));
        RecintoDTO recinto = mesa.getRecinto();
        fila.setRecinto(recinto != null ? textoNulo(recinto.getNombre()) : "");
        fila.setParroquia(obtenerParroquiaNombre(mesa));
        fila.setCanton(obtenerCantonNombre(mesa));
        fila.setProvincia(obtenerProvinciaNombre(mesa));

        EscrutinioCabeceraDTO cabecera = escrutinioService.buscarCabeceraDTO(mesa.getId(), procesoId);
        if (cabecera != null) {
            fila.setEstadoEscrutinio(cabecera.getEstadoEscrutinio() != null
                    ? cabecera.getEstadoEscrutinio() : EstadoEscrutinio.PENDIENTE);
            fila.setPresidenteMesa(textoNulo(cabecera.getPresidenteResponsable()));
            fila.setFechaApertura(cabecera.getFechaApertura());
            fila.setFechaCierre(cabecera.getFechaCierre());
            fila.setSufragantesAsignados(valorEntero(cabecera.getTotalSufragantes()));
            fila.setVotosRegistrados(valorEntero(cabecera.getTotalVotosRegistrados()));
            fila.setVotosValidos(valorEntero(cabecera.getTotalVotosValidos()));
            fila.setVotosBlancos(valorEntero(cabecera.getTotalVotosBlancos()));
            fila.setVotosNulos(valorEntero(cabecera.getTotalVotosNulos()));
        } else {
            fila.setEstadoEscrutinio(EstadoEscrutinio.PENDIENTE);
            fila.setSufragantesAsignados(padronService.contarSufragantesPorMesaYProceso(mesa.getId(), procesoId));
            fila.setVotosRegistrados(0);
            fila.setVotosValidos(0);
            fila.setVotosBlancos(0);
            fila.setVotosNulos(0);
        }
        String presidente = obtenerPresidenteMesa(mesa.getId(), procesoId);
        if (fila.getPresidenteMesa() == null || fila.getPresidenteMesa().isBlank()) {
            fila.setPresidenteMesa(presidente);
        }
        Documentos actaValida = obtenerActaValidaMesa(mesa.getId(), procesoId);
        fila.setDocumentoActa(actaValida);
        fila.setActaPdfGenerada(actaValida != null);
        return fila;
    }

    private void calcularResumenGerencial() {
        limpiarResumenGerencial();
        if (listaConsultaGerencial == null) {
            return;
        }
        totalMesasGerencial = listaConsultaGerencial.size();
        for (ActaEGerencialDTO fila : listaConsultaGerencial) {
            EstadoEscrutinio estado = fila.getEstadoEscrutinio() != null
                    ? fila.getEstadoEscrutinio() : EstadoEscrutinio.PENDIENTE;
            if (EstadoEscrutinio.PENDIENTE.equals(estado)) {
                mesasPendientesGerencial++;
            } else if (EstadoEscrutinio.ABIERTO.equals(estado)) {
                mesasAbiertasGerencial++;
            } else if (EstadoEscrutinio.EN_CONTEO.equals(estado)
                    || EstadoEscrutinio.CONTEO_REGISTRADO.equals(estado)
                    || EstadoEscrutinio.REABIERTO.equals(estado)) {
                mesasConteoGerencial++;
            } else if (EstadoEscrutinio.CERRADO.equals(estado)) {
                mesasCerradasGerencial++;
            } else if (EstadoEscrutinio.OBSERVADO.equals(estado)) {
                mesasObservadasGerencial++;
            }
            totalSufragantesGerencial += valorEntero(fila.getSufragantesAsignados());
            totalVotosRegistradosGerencial += valorEntero(fila.getVotosRegistrados());
            totalVotosValidosGerencial += valorEntero(fila.getVotosValidos());
            totalVotosBlancosGerencial += valorEntero(fila.getVotosBlancos());
            totalVotosNulosGerencial += valorEntero(fila.getVotosNulos());
            if (Boolean.TRUE.equals(fila.getActaPdfGenerada())) {
                totalActasGeneradasGerencial++;
            }
        }
        totalActasPendientesGerencial = Math.max(0, totalMesasGerencial - totalActasGeneradasGerencial);
    }

    private void limpiarResumenGerencial() {
        totalMesasGerencial = 0;
        mesasPendientesGerencial = 0;
        mesasAbiertasGerencial = 0;
        mesasConteoGerencial = 0;
        mesasCerradasGerencial = 0;
        mesasObservadasGerencial = 0;
        totalSufragantesGerencial = 0;
        totalVotosRegistradosGerencial = 0;
        totalVotosValidosGerencial = 0;
        totalVotosBlancosGerencial = 0;
        totalVotosNulosGerencial = 0;
        totalActasGeneradasGerencial = 0;
        totalActasPendientesGerencial = 0;
    }

    private void limpiarSeleccionMesa() {
        listaCamposActaE = new ArrayList<>();
        documentosActa = new ArrayList<>();
        escrutinioCabecera = new EscrutinioCabeceraDTO();
        totalSufragantesAsignados = 0;
        observacionApertura = "";
    }

    private boolean tieneRolConsultaGerencial() {
        return tieneRol("SITEC-Administrador")
                || tieneRol("SITEC-Gerencial")
                || tieneRol("SITEC-Supervisor")
                || tieneRol("SITEC-SuperAdministrador")
                || tieneRol("SITEC-Superadministrador");
    }

    private boolean tieneRolOperacionActa() {
        return tieneRol("SITEC-Administrador")
                || tieneRol("SITEC-Supervisor")
                || tieneRol("SITEC-SuperAdministrador")
                || tieneRol("SITEC-Superadministrador");
    }

    private boolean tieneRolSupervisorOAdministrador() {
        return tieneRol("SITEC-Supervisor") || tieneRolAdministrador();
    }

    private boolean tieneRolAdministrador() {
        return tieneRol("SITEC-Administrador")
                || tieneRol("SITEC-SuperAdministrador")
                || tieneRol("SITEC-Superadministrador");
    }

    private boolean puedeCambiarEstadoAdministrativo(EstadoEscrutinio estadoNuevo) {
        if (EstadoEscrutinio.OBSERVADO.equals(estadoNuevo)) {
            return isPuedeObservarEscrutinio();
        }
        if (EstadoEscrutinio.ANULADO.equals(estadoNuevo)) {
            return isPuedeAnularEscrutinio();
        }
        if (EstadoEscrutinio.REABIERTO.equals(estadoNuevo)) {
            return isPuedeReabrirEscrutinio();
        }
        return false;
    }

    private boolean requiereMotivo(EstadoEscrutinio estadoNuevo) {
        return EstadoEscrutinio.ANULADO.equals(estadoNuevo)
                || EstadoEscrutinio.REABIERTO.equals(estadoNuevo)
                || EstadoEscrutinio.OBSERVADO.equals(estadoNuevo);
    }

    private void auditarCambioEstado(EstadoEscrutinio estadoAnterior, EstadoEscrutinio estadoNuevo, String motivo) {
        String datos = "MESA=" + (mesaSeleccionado != null ? mesaSeleccionado.getId() : "")
                + ";PROCESO=" + (procesoActivo != null ? procesoActivo.getId() : "")
                + ";USUARIO=" + (loginBean != null ? loginBean.getUserName() : "")
                + ";FECHA=" + JsfUtil.getFechaStringYYYYMMddHHmm(new Date())
                + ";ESTADO_ANTERIOR=" + (estadoAnterior != null ? estadoAnterior.name() : "")
                + ";ESTADO_NUEVO=" + (estadoNuevo != null ? estadoNuevo.name() : "")
                + ";MOTIVO=" + (motivo != null ? motivo.trim() : "");
        procesoBean.okActivityRegister("CAMBIO ESTADO ESCRUTINIO " + estadoNuevo, datos);
    }

    private boolean tieneRol(String rol) {
        return loginBean != null && loginBean.getRoles() != null && loginBean.getRoles().contains(rol);
    }

    private Documentos obtenerActaValidaMesa(Integer mesaId, Integer procesoId) {
        if (mesaId == null || procesoId == null) {
            return null;
        }
        List<Documentos> documentos = documentoBean.getDocumentosPorEntidadYTipoDoc(mesaId, Constantes.ACTA_ESCRUTINIO);
        if (documentos == null) {
            return null;
        }
        for (int i = documentos.size() - 1; i >= 0; i--) {
            Documentos documento = documentos.get(i);
            if (esDocumentoDelProceso(documento, mesaId, procesoId) && esDocumentoActaValido(documento)) {
                return documento;
            }
        }
        return null;
    }

    private boolean esDocumentoDelProceso(Documentos documento, Integer mesaId, Integer procesoId) {
        if (documento == null || mesaId == null || procesoId == null) {
            return false;
        }
        String prefijo = "ACTA-" + procesoId + "-M" + mesaId + "-";
        String codigo = documento.getCodigo() != null ? documento.getCodigo() : "";
        String nombre = documento.getNombre() != null ? documento.getNombre() : "";
        return codigo.startsWith(prefijo) || nombre.startsWith(prefijo);
    }

    private String obtenerPresidenteMesa(Integer mesaId, Integer procesoId) {
        List<MiembroJRVDTO> miembros = miembroJRVService.listarDTOsPorMesaProceso(mesaId, procesoId);
        if (miembros == null) {
            return "";
        }
        for (MiembroJRVDTO miembro : miembros) {
            if (miembro != null && esCargoPresidenteMesa(miembro.getCargoNombre())
                    && miembro.getIglesiaPersona() != null
                    && miembro.getIglesiaPersona().getPersona() != null) {
                return nombreCompletoPersona(miembro.getIglesiaPersona().getPersona());
            }
        }
        return "";
    }

    private Integer obtenerParroquiaId(MesaDTO mesa) {
        if (mesa == null) {
            return null;
        }
        if (mesa.getRecinto() != null && mesa.getRecinto().getUbicacionId() != null) {
            return mesa.getRecinto().getUbicacionId();
        }
        return mesa.getUbicacionId();
    }

    private Integer obtenerCantonId(MesaDTO mesa) {
        if (mesa == null) {
            return null;
        }
        if (mesa.getRecinto() != null && mesa.getRecinto().getCantonId() != null) {
            return mesa.getRecinto().getCantonId();
        }
        return mesa.getCantonId();
    }

    private Integer obtenerProvinciaId(Integer cantonId, Map<Integer, Geograp> cacheCantones) {
        Geograp canton = obtenerCanton(cantonId, cacheCantones);
        return canton != null && canton.getGeograp() != null ? canton.getGeograp().getId() : null;
    }

    private String obtenerProvinciaNombre(MesaDTO mesa) {
        Geograp canton = obtenerCanton(obtenerCantonId(mesa), new HashMap<>());
        return canton != null && canton.getGeograp() != null ? textoNulo(canton.getGeograp().getName()) : "";
    }

    private String obtenerCantonNombre(MesaDTO mesa) {
        if (mesa == null) {
            return "";
        }
        if (mesa.getRecinto() != null && mesa.getRecinto().getCantonNombre() != null) {
            return textoNulo(mesa.getRecinto().getCantonNombre());
        }
        return textoNulo(mesa.getCantonNombre());
    }

    private String obtenerParroquiaNombre(MesaDTO mesa) {
        if (mesa == null) {
            return "";
        }
        if (mesa.getRecinto() != null && mesa.getRecinto().getUbicacionNombre() != null) {
            return textoNulo(mesa.getRecinto().getUbicacionNombre());
        }
        return textoNulo(mesa.getUbicacionNombre());
    }

    private Geograp obtenerCanton(Integer cantonId, Map<Integer, Geograp> cacheCantones) {
        if (cantonId == null) {
            return null;
        }
        if (cacheCantones.containsKey(cantonId)) {
            return cacheCantones.get(cantonId);
        }
        Geograp canton = geograpBean.getById(cantonId);
        cacheCantones.put(cantonId, canton);
        return canton;
    }

    private int valorEntero(Integer valor) {
        return valor != null ? valor : 0;
    }

    public int getPorcentajeAvanceGerencial() {
        if (totalMesasGerencial == 0) {
            return 0;
        }
        return Math.round((mesasCerradasGerencial * 100f) / totalMesasGerencial);
    }

    public int getPorcentajeParticipacionGerencial() {
        if (totalSufragantesGerencial == 0) {
            return 0;
        }
        return Math.round((totalVotosRegistradosGerencial * 100f) / totalSufragantesGerencial);
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
