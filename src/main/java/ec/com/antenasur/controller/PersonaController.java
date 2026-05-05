package ec.com.antenasur.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

import ec.com.antenasur.bean.DocumentoBean;
import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.dto.FilaPadronImportadaDTO;
import ec.com.antenasur.dto.IglesiaDTO;
import ec.com.antenasur.dto.IglesiaPersonaDTO;
import ec.com.antenasur.dto.PersonaDTO;
import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.tec.Documentos;
import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.model.tec.TipoDocumento;
import ec.com.antenasur.service.GeograpService;
import ec.com.antenasur.service.IglesiaPersonaService;
import ec.com.antenasur.service.IglesiaService;
import ec.com.antenasur.service.PersonaService;
import ec.com.antenasur.dto.CronogramaFaseDTO;
import ec.com.antenasur.service.tec.CronogramaService;
import ec.com.antenasur.service.tec.MesaService;
import ec.com.antenasur.service.tec.PadronService;
import ec.com.antenasur.service.tec.RecintoService;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.ExcelPadronParser;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Named
@ViewScoped
@Slf4j
public class PersonaController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String PATH_LISTA_MIEMBROS = "C:\\ARCHIVOS\\ACTASE\\";

    @Inject
    private LoginBean loginBean;

    @Inject
    private DocumentoBean documentoBean;

    @Inject
    private PersonaService personaService;

    @Inject
    private IglesiaService iglesiaService;

    @Inject
    private RecintoService recintoService;

    @Inject
    private MesaService mesaService;

    @Inject
    private PadronService padronService;

    @Inject
    private IglesiaPersonaService iglesiaPersonaService;

    @Inject
    private GeograpService geograpService;

    @Inject
    private CronogramaService cronogramaService;

    @Setter
    @Getter
    private PersonaDTO personaSeleccionado;

    @Setter
    @Getter
    private IglesiaDTO iglesiaSeleccionado;

    @Setter
    @Getter
    private IglesiaPersonaDTO iglesiaPersonaSeleccionado;

    @Setter
    @Getter
    private Geograp parroquiaSeleccionado, cantonSeleccionado;

    @Setter
    @Getter
    private List<Geograp> cantones, parroquias;

    @Setter
    @Getter
    private List<IglesiaDTO> listaIglesias;

    @Setter
    @Getter
    private List<IglesiaPersonaDTO> listaIglesiaPersona, listaIglesiaPersonaSeleccionados, listaIglesiaPersonaExistente;

    @Setter
    @Getter
    private UploadedFile file;

    @Setter
    @Getter
    private StreamedContent fileDown;

    @Setter
    @Getter
    private InputStream in;

    @Setter
    @Getter
    private XSSFWorkbook excelMigracion;

    @Setter
    @Getter
    private List<PersonaDTO> listaPersonas;

    /**
     * Bandera derivada del usuario logueado: true si su rol es IglesiaAdmin
     * y tiene una iglesia asignada. Cuando es true, la vista debe ocultar
     * los selectores de cantón/parroquia/iglesia (operará solo sobre su iglesia).
     */
    @Getter
    private boolean restringidoAIglesia;

    /** Progreso de actualización: [total, actualizados, porcentaje]. */
    private int[] progreso = {0, 0, 0};

    public int getTotalMiembros() { return progreso[0]; }
    public int getMiembrosActualizados() { return progreso[1]; }
    public int getMiembrosPendientes() { return progreso[0] - progreso[1]; }
    public int getPorcentajeActualizacion() { return progreso[2]; }
    public boolean isActualizacionCompleta() { return progreso[0] > 0 && progreso[1] == progreso[0]; }

    /** Fase vigente del cronograma electoral (alimenta el banner superior). */
    @Getter
    private CronogramaFaseDTO faseVigente;

    /** Indica si la fase vigente permite editar el padrón. Bloquea botones. */
    @Getter
    private boolean puedeEditarPadron;

    @PostConstruct
    private void init() {
        try {
            listaIglesias = new ArrayList<>();
            listaIglesiaPersona = new ArrayList<>();
            parroquiaSeleccionado = cantonSeleccionado = new Geograp();
            iglesiaSeleccionado = new IglesiaDTO();

            // Cronograma electoral: alimenta banner y permisos de edición.
            faseVigente = cronogramaService.getFaseVigenteDelProcesoActivo();
            puedeEditarPadron = cronogramaService.permiteEdicionPadron();

            // Detección de rol IglesiaAdmin: si el usuario logueado tiene este
            // rol y una iglesia asignada, lo confinamos a esa iglesia y
            // precargamos sus miembros directamente.
            if (esUsuarioIglesiaAdminConIglesia()) {
                restringidoAIglesia = true;
                Integer iglesiaId = loginBean.getUsuario().getIglesiaId();
                iglesiaSeleccionado = iglesiaService.obtenerDTOPorId(iglesiaId);
                listaIglesias = new ArrayList<>();
                listaIglesias.add(iglesiaSeleccionado);
                listaIglesiaPersona = iglesiaPersonaService.listarDTOsPorIglesia(iglesiaId);
                progreso = iglesiaPersonaService.calcularProgresoActualizacion(iglesiaId);
                return;
            }

            // Camino normal (admin global): permite filtrar por cantón/parroquia.
            cantones = geograpService.findByFatherId(7);
            listaIglesias = iglesiaService.listarDTOs();
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    private boolean esUsuarioIglesiaAdminConIglesia() {
        if (loginBean == null || loginBean.getUsuario() == null
                || loginBean.getUsuario().getIglesiaId() == null
                || loginBean.getRoles() == null) {
            return false;
        }
        String prefijo = (String) JsfUtil.getProperty("roles.sitec", true);
        String rolIglesia = (prefijo == null ? "" : prefijo) + Constantes.getRolIglesiaAdmin();
        for (String r : loginBean.getRoles()) {
            if (rolIglesia.equals(r)) {
                return true;
            }
        }
        return false;
    }

    public void obtieneParroquias() {
        if (cantonSeleccionado.getId() != null) {
            cantonSeleccionado = geograpService.find(cantonSeleccionado.getId());
            parroquias = geograpService.findByFatherId(cantonSeleccionado.getId());
            listaIglesiaPersona = iglesiaPersonaService.listarDTOsPorParroquias(parroquias);
            listaIglesias = iglesiaService.listarDTOsPorParroquias(parroquias);
        } else {
            if (parroquias != null) {
                parroquias.clear();
            }
            iglesiaSeleccionado = new IglesiaDTO();
            listaIglesias.clear();
            listaIglesiaPersona.clear();
        }
    }

    public void obtieneIglesiasPorParroquia() {
        if (parroquiaSeleccionado.getId() != null) {
            parroquiaSeleccionado = geograpService.find(parroquiaSeleccionado.getId());
            List<Geograp> parroquiasTmp = new ArrayList<>();
            parroquiasTmp.add(parroquiaSeleccionado);
            listaIglesias = iglesiaService.listarDTOsPorParroquias(parroquiasTmp);
            listaIglesiaPersona = iglesiaPersonaService.listarDTOsPorParroquias(parroquiasTmp);
            if (listaIglesias == null || listaIglesias.isEmpty()) {
                JsfUtil.addWarningMessage("No existe registro de Iglesias en " + parroquiaSeleccionado.getName());
            } else {
                JsfUtil.addInfoMessage(listaIglesias.size() + " Iglesias registradas");
            }
        } else {
            iglesiaSeleccionado = new IglesiaDTO();
            listaIglesias.clear();
            listaIglesiaPersona.clear();
        }
    }

    public void obtienePersonasPorIglesias() {
        if (iglesiaSeleccionado != null && iglesiaSeleccionado.getId() != null) {
            iglesiaSeleccionado = iglesiaService.obtenerDTOPorId(iglesiaSeleccionado.getId());
            listaIglesiaPersona = iglesiaPersonaService.listarDTOsPorIglesia(iglesiaSeleccionado.getId());
            if (listaIglesiaPersona == null || listaIglesiaPersona.isEmpty()) {
                JsfUtil.addWarningMessage("No existe registro de personas en " + iglesiaSeleccionado.getNombre());
            } else {
                JsfUtil.addInfoMessage(listaIglesiaPersona.size() + " personas registradas");
            }
        } else {
            listaIglesiaPersona.clear();
        }
    }

    public void inicializaPersonaSeleccionado() {
        if (listaIglesiaPersona != null) {
            listaIglesiaPersona.clear();
        }
        iglesiaPersonaSeleccionado = new IglesiaPersonaDTO();
        iglesiaPersonaSeleccionado.setPersona(new PersonaDTO());
        iglesiaPersonaSeleccionado.setIglesia(new IglesiaDTO());
        this.iglesiaSeleccionado = new IglesiaDTO();
        this.personaSeleccionado = new PersonaDTO();
    }

    public void nuevaPersona() {
        inicializaPersonaSeleccionado();
    }

    public boolean existeIglesiaPersonasSeleccionadas() {
        return this.listaIglesiaPersonaSeleccionados != null && !this.listaIglesiaPersonaSeleccionados.isEmpty();
    }

    public String getMensajeBotonEliminar() {
        if (existeIglesiaPersonasSeleccionadas()) {
            int size = this.listaIglesiaPersonaSeleccionados.size();
            return size > 1 ? size + " personas seleccionadas" : "1 persona seleccionada";
        }
        return "Eliminar";
    }

    public void eliminarIglesiaPersonaSeleccionadas() {
        int eliminadas = 0;
        if (listaIglesiaPersonaSeleccionados != null) {
            List<Integer> ids = new ArrayList<>();
            for (IglesiaPersonaDTO item : listaIglesiaPersonaSeleccionados) {
                if (item.getId() != null) {
                    ids.add(item.getId());
                }
            }
            eliminadas = iglesiaPersonaService.eliminarPorIds(ids);
        }
        listaIglesiaPersona = iglesiaPersonaService.listarDTOs();
        JsfUtil.addInfoMessage(eliminadas + " Personas eliminadas");
        this.listaIglesiaPersonaSeleccionados = null;
        PrimeFaces.current().ajax().update("frmPersonas", "msgs");
    }

    public void buscaPersonaPorCedula() {
        if (iglesiaPersonaSeleccionado == null || iglesiaPersonaSeleccionado.getPersona() == null) {
            return;
        }
        PersonaDTO encontrada = personaService.buscarDTOPorDocumento(iglesiaPersonaSeleccionado.getPersona().getDocumento());
        if (encontrada != null) {
            iglesiaPersonaSeleccionado.setPersona(encontrada);
            JsfUtil.addInfoMessage("Persona con CI: " + encontrada.getDocumento() + " ya se encuentra registrado ");
        }
    }

    public void actualizarPersona() {
        try {
            boolean esActualizacion = iglesiaPersonaSeleccionado != null
                    && iglesiaPersonaSeleccionado.getId() != null;
            // Si el contexto está restringido a una iglesia (IglesiaAdmin),
            // forzamos el binding a esa iglesia para evitar registros cruzados.
            if (restringidoAIglesia && iglesiaPersonaSeleccionado != null
                    && iglesiaSeleccionado != null && iglesiaSeleccionado.getId() != null) {
                iglesiaPersonaSeleccionado.setIglesia(iglesiaSeleccionado);
            }
            if (!cronogramaService.permiteEdicionPadron()) {
                JsfUtil.addErrorMessage("La actualización del padrón está cerrada por el cronograma electoral.");
                return;
            }
            IglesiaPersonaDTO persistido = iglesiaPersonaService.guardarDesdeDTO(iglesiaPersonaSeleccionado);
            if (persistido != null) {
                JsfUtil.addSuccessMessage(esActualizacion ? "Persona actualizada" : "Persona registrada y marcada como actualizada");
                personaSeleccionado = null;
                iglesiaPersonaSeleccionado = null;
                // Refresca lista de miembros y recalcula progreso (la edición
                // mueve f_actualiza vía @PreUpdate de Hibernate, lo cual hace
                // que la regla "delta > 2s" del DTO marque actualizada).
                if (restringidoAIglesia && iglesiaSeleccionado != null && iglesiaSeleccionado.getId() != null) {
                    listaIglesiaPersona = iglesiaPersonaService.listarDTOsPorIglesia(iglesiaSeleccionado.getId());
                    progreso = iglesiaPersonaService.calcularProgresoActualizacion(iglesiaSeleccionado.getId());
                } else if (iglesiaSeleccionado != null && iglesiaSeleccionado.getId() != null) {
                    listaIglesiaPersona = iglesiaPersonaService.listarDTOsPorIglesia(iglesiaSeleccionado.getId());
                    progreso = iglesiaPersonaService.calcularProgresoActualizacion(iglesiaSeleccionado.getId());
                }
            }
        } catch (Exception e) {
            log.error("Error al guardar persona", e);
        }
        PrimeFaces.current().executeScript("PF('dlgPersona').hide()");
        PrimeFaces.current().ajax().update("frmPersonas");
    }

    /**
     * Genera y descarga el acta PDF de actualización de miembros de la iglesia
     * actualmente cargada en {@code iglesiaSeleccionado}. El acta marca
     * "PARCIAL" cuando el porcentaje no es 100% para evidenciar que aún hay
     * miembros pendientes. Solo lista los miembros ya marcados como actualizados.
     */
    public void generarActaActualizacion() {
        try {
            if (iglesiaSeleccionado == null || iglesiaSeleccionado.getId() == null) {
                JsfUtil.addWarningMessage("Seleccione una iglesia primero");
                return;
            }
            List<IglesiaPersonaDTO> actualizados = iglesiaPersonaService
                    .listarDTOsActualizadosPorIglesia(iglesiaSeleccionado.getId());

            String nombreReporte = "ACTA_ACTUALIZACION_" + iglesiaSeleccionado.getNombre()
                    .replaceAll("[^A-Za-z0-9]", "_");
            ec.com.antenasur.itext.ReportePFD.nuevoPDF(nombreReporte);

            String tituloPrefijo = isActualizacionCompleta() ? "" : "[PARCIAL " + getPorcentajeActualizacion() + "%] ";
            String titulo = tituloPrefijo + "ACTA DE ACTUALIZACIÓN DE MIEMBROS";
            String[] columnas = {"#", "CÉDULA", "NOMBRES", "FECHA ACTUALIZACIÓN"};
            float[] anchos = {30, 90, 200, 120};

            com.itextpdf.text.Font fuenteCab = ec.com.antenasur.util.Constantes.getFuenteCabeceraDefault(10);
            ec.com.antenasur.itext.ReportePFD.creaTablaCabecera(columnas.length, anchos, titulo, columnas, fuenteCab);

            // Encabezado adicional con datos de la iglesia
            ec.com.antenasur.itext.ReportePFD.addParagraph(
                    "Iglesia: " + iglesiaSeleccionado.getNombre()
                            + "  |  Comunidad: " + (iglesiaSeleccionado.getComunidad() == null ? "—" : iglesiaSeleccionado.getComunidad())
                            + "  |  Total miembros: " + getTotalMiembros()
                            + "  |  Actualizados: " + getMiembrosActualizados()
                            + "  |  Pendientes: " + getMiembrosPendientes());
            ec.com.antenasur.itext.ReportePFD.agregaParrafoEnBlanco();

            String[][] datos = new String[actualizados.size()][columnas.length];
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
            for (int i = 0; i < actualizados.size(); i++) {
                IglesiaPersonaDTO ip = actualizados.get(i);
                datos[i][0] = String.valueOf(i + 1);
                datos[i][1] = ip.getPersona() != null ? safe(ip.getPersona().getDocumento()) : "";
                datos[i][2] = ip.getPersona() != null ? safe(ip.getPersona().getNombres()) : "";
                datos[i][3] = ip.getFechaActualiza() != null ? fmt.format(ip.getFechaActualiza()) : "";
            }
            com.itextpdf.text.Font fuenteCont = ec.com.antenasur.util.Constantes.getFuenteContenidoDefault(9);
            ec.com.antenasur.itext.ReportePFD.creaContenidoTabla(datos, columnas, fuenteCont);

            String userName = (loginBean.getUsuario() != null && loginBean.getUsuario().getUsername() != null)
                    ? loginBean.getUsuario().getUsername() : "—";
            ec.com.antenasur.itext.ReportePFD.getFinalParagraph(userName);
            ec.com.antenasur.itext.ReportePFD.descargarPDF(nombreReporte);
        } catch (Exception e) {
            log.error("Error al generar acta de actualización", e);
            JsfUtil.addErrorMessage("No se pudo generar el acta. Intente nuevamente.");
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    public void handleFileUpload(FileUploadEvent event) {
        try {
            file = event.getFile();
            if (file != null && file.getContent() != null && file.getContent().length > 0 && file.getFileName() != null) {
                excelMigracion = new XSSFWorkbook(file.getInputStream());
                if (excelMigracion == null) {
                    JsfUtil.addFatalMessage("Error al procesar archivo");
                } else {
                    cargaArchivoABD();
                    JsfUtil.addInfoMessage("Archivo cargado correctamente");
                }
            }
        } catch (Exception e) {
            JsfUtil.addFatalMessage("Error en formato de archivo");
            file = null;
            log.error("ERROR AL CARGAR ARCHIVO", e);
        }
    }

    public void cargaArchivoABD() {
        if (file != null) {
            procesaArchivo(file);
        }
    }

    public void guardarArchivoExcel() {
        try {
            int tamanioNombre = file.getFileName().length();
            String extencion = file.getFileName().substring(tamanioNombre - 5, tamanioNombre);
            String nombreArchivo = iglesiaSeleccionado.getNombre() + "-" + JsfUtil.getFechaStringYYYYMMddHHmm(new Date());
            String pathCompleto = PATH_LISTA_MIEMBROS + nombreArchivo + extencion;

            Documentos documentoNuevo = new Documentos(nombreArchivo, pathCompleto, new TipoDocumento(2),
                    iglesiaSeleccionado.getId(), extencion, "application/" + extencion, nombreArchivo);
            documentoBean.guardarDocumento(documentoNuevo);

            Path path = Paths.get(pathCompleto);
            Files.write(path, file.getContent());
            JsfUtil.addSuccessMessage(nombreArchivo + " Almacenado");
        } catch (IOException e) {
            log.error("ERROR AL GUARDAR ARCHIVOS", e);
        }
    }

    public void procesaArchivo(UploadedFile file) {
        try {
            if (file == null || file.getContent() == null || file.getContent().length == 0
                    || file.getFileName() == null) {
                return;
            }
            guardarArchivoExcel();
            if (excelMigracion == null) {
                JsfUtil.addWarningMessage("Archivo formato incorrecto");
                return;
            }

            List<FilaPadronImportadaDTO> filas = ExcelPadronParser.parsear(excelMigracion);
            for (FilaPadronImportadaDTO filaDto : filas) {
                Mesa mesa = filaDto.getNombreMesa() != null
                        ? mesaService.buscaPorNombreMesa(filaDto.getNombreMesa()) : null;
                Geograp ubicacion = filaDto.getUbicacionId() != null
                        ? geograpService.find(filaDto.getUbicacionId()) : null;
                if (ubicacion != null) {
                    filaDto.getIglesia().setUbicacion(ubicacion);
                }
                padronService.importarFilaPadron(filaDto.getPersona(), filaDto.getIglesia(), mesa);
            }

            listaPersonas = personaService.listarDTOs();
            excelMigracion.close();
        } catch (Exception e) {
            log.error("ERROR AL CARGAR ARCHIVO", e);
        }
    }
}
