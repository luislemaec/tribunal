package ec.com.antenasur.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

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
import ec.com.antenasur.itext.ReporteXLSX;
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

    /**
     * Fase de menor orden vigente en el proceso activo (banner general del
     * proceso). Puede ser cualquier fase del ciclo electoral.
     */
    @Getter
    private CronogramaFaseDTO faseVigente;

    /**
     * Configuración de {@link ec.com.antenasur.enums.FaseElectoral#ACTUALIZACION_MIEMBROS}
     * en el proceso activo (presente aunque la fase no esté vigente ahora).
     * El flag {@code vigente} del DTO indica si la ventana de fechas está activa.
     * Alimenta el card central del timeline de cronograma.
     */
    @Getter
    private CronogramaFaseDTO faseActualizacionMiembros;

    /**
     * Fase inmediatamente anterior a ACTUALIZACION_MIEMBROS por {@code orden}.
     * Puede ser pasada, vigente o futura (sin filtro de fecha). Null si no existe.
     */
    @Getter
    private CronogramaFaseDTO faseAnterior;

    /**
     * Fase inmediatamente siguiente a ACTUALIZACION_MIEMBROS por {@code orden}.
     * Puede ser pasada, vigente o futura (sin filtro de fecha). Null si no existe.
     */
    @Getter
    private CronogramaFaseDTO faseSiguiente;

    /** Indica si la fase activa permite editar el padrón. Bloquea botones. */
    @Getter
    private boolean puedeEditarPadron;

    @PostConstruct
    private void init() {
        try {
            listaIglesias = new ArrayList<>();
            listaIglesiaPersona = new ArrayList<>();
            parroquiaSeleccionado = cantonSeleccionado = new Geograp();
            iglesiaSeleccionado = new IglesiaDTO();

            // Cronograma electoral: timeline del módulo personas.
            // faseVigente            → fase de mayor prioridad (menor orden) activa ahora.
            // faseActualizacionMiembros → config de ACTUALIZACION_MIEMBROS (sin filtro de fecha).
            // faseAnterior / faseSiguiente → fases adyacentes por orden (sin filtro de fecha).
            // puedeEditarPadron     → evaluado sobre TODAS las fases activas simultáneas.
            faseVigente = cronogramaService.getFaseVigenteDelProcesoActivo();
            faseActualizacionMiembros = cronogramaService.getFaseActualizacionMiembros();
            faseAnterior = cronogramaService.getFaseAnteriorAActualizacion();
            faseSiguiente = cronogramaService.getFaseSiguienteAActualizacion();
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
        // Por defecto habilitado para padrón: el admin puede desmarcarlo
        iglesiaPersonaSeleccionado.setHabilitadoPadron(Boolean.TRUE);
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
        List<Integer> ids = new ArrayList<>();
        if (listaIglesiaPersonaSeleccionados != null && !listaIglesiaPersonaSeleccionados.isEmpty()) {
            for (IglesiaPersonaDTO item : listaIglesiaPersonaSeleccionados) {
                if (puedeEliminarMiembro(item)) {
                    ids.add(item.getId());
                }
            }
        }
        eliminarMiembrosPorIds(ids);
    }

    public void eliminarIglesiaPersonaSeleccionada() {
        List<Integer> ids = new ArrayList<>();
        if (puedeEliminarMiembro(iglesiaPersonaSeleccionado)) {
            ids.add(iglesiaPersonaSeleccionado.getId());
        }
        eliminarMiembrosPorIds(ids);
    }

    private void eliminarMiembrosPorIds(List<Integer> ids) {
        int eliminadas = iglesiaPersonaService.eliminarPorIds(ids);
        refrescarListadoMiembrosActual();
        if (eliminadas > 0) {
            JsfUtil.addInfoMessage(eliminadas + " Personas eliminadas");
        } else {
            JsfUtil.addWarningMessage("No se encontraron miembros seleccionados para eliminar");
        }
        this.listaIglesiaPersonaSeleccionados = null;
        this.iglesiaPersonaSeleccionado = null;
        PrimeFaces.current().ajax().update("frmPersonas", "frmGlobal:growlGlobal");
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
                String nombreMiembro = (persistido.getPersona() != null
                        && persistido.getPersona().getNombres() != null)
                        ? persistido.getPersona().getNombres() : "";
                boolean enPadron = Boolean.TRUE.equals(persistido.getHabilitadoPadron());
                String estadoPadron = enPadron ? " · Habilitado para padrón" : " · No habilitado para padrón";
                if (esActualizacion) {
                    JsfUtil.addSuccessMessage("Miembro actualizado correctamente: "
                            + nombreMiembro + estadoPadron);
                } else {
                    JsfUtil.addSuccessMessage("Miembro registrado correctamente: "
                            + nombreMiembro + estadoPadron);
                }
                personaSeleccionado = null;
                iglesiaPersonaSeleccionado = null;
                refrescarListadoMiembrosActual();
            }
        } catch (Exception e) {
            log.error("Error al guardar persona", e);
        }
        PrimeFaces.current().executeScript("PF('dlgPersona').hide()");
        PrimeFaces.current().ajax().update("frmPersonas");
    }

    private void refrescarListadoMiembrosActual() {
        if (iglesiaSeleccionado != null && iglesiaSeleccionado.getId() != null) {
            listaIglesiaPersona = iglesiaPersonaService.listarDTOsPorIglesia(iglesiaSeleccionado.getId());
            progreso = iglesiaPersonaService.calcularProgresoActualizacion(iglesiaSeleccionado.getId());
            return;
        }
        if (parroquiaSeleccionado != null && parroquiaSeleccionado.getId() != null) {
            List<Geograp> parroquiasFiltro = new ArrayList<>();
            parroquiasFiltro.add(parroquiaSeleccionado);
            listaIglesiaPersona = iglesiaPersonaService.listarDTOsPorParroquias(parroquiasFiltro);
            return;
        }
        if (parroquias != null && !parroquias.isEmpty()) {
            listaIglesiaPersona = iglesiaPersonaService.listarDTOsPorParroquias(parroquias);
            return;
        }
        listaIglesiaPersona = iglesiaPersonaService.listarDTOs();
    }

    private boolean puedeEliminarMiembro(IglesiaPersonaDTO miembro) {
        if (miembro == null || miembro.getId() == null) {
            return false;
        }
        if (!restringidoAIglesia) {
            return true;
        }
        Integer iglesiaAsignadaId = iglesiaSeleccionado != null ? iglesiaSeleccionado.getId() : null;
        Integer iglesiaMiembroId = miembro.getIglesia() != null ? miembro.getIglesia().getId() : null;
        return iglesiaAsignadaId != null && iglesiaAsignadaId.equals(iglesiaMiembroId);
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

    public void exportarExcel() {
        try {
            List<IglesiaPersonaDTO> lista = listaIglesiaPersona != null ? listaIglesiaPersona : new ArrayList<>();
            String fecha = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
            String hora = new SimpleDateFormat("HH:mm:ss").format(new Date());

            ReporteXLSX.nuevoExcel("Listado de Miembros");
            ReporteXLSX.creaEspacioInformativo(fecha, hora, ReporteXLSX.getNombreUsuarioAutenticado());

            String[] columnas = {
                "N°", "CEDULA", "NOMBRES", "SEXO", "IGLESIA",
                "PADRON", "REVISION", "FECHA ACTUALIZACION"
            };
            int[] anchos = { 1800, 4500, 9500, 2500, 9000, 4500, 4500, 6000 };
            ReporteXLSX.creaCabeceraTabla(columnas, anchos);

            String[][] datos = new String[lista.size()][columnas.length];
            SimpleDateFormat fmtFechaHora = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            for (int i = 0; i < lista.size(); i++) {
                IglesiaPersonaDTO ip = lista.get(i);
                PersonaDTO persona = ip.getPersona();
                IglesiaDTO iglesia = ip.getIglesia();

                datos[i][0] = String.valueOf(i + 1);
                datos[i][1] = persona != null ? safe(persona.getDocumento()) : "";
                datos[i][2] = persona != null
                        ? (safe(persona.getApellidos()) + " " + safe(persona.getNombres())).trim() : "";
                datos[i][3] = persona != null ? safe(persona.getSexo()) : "";
                datos[i][4] = iglesia != null ? safe(iglesia.getNombre()) : safe(iglesiaSeleccionado.getNombre());
                datos[i][5] = Boolean.TRUE.equals(ip.getHabilitadoPadron()) ? "Habilitado" : "No habilitado";
                datos[i][6] = Boolean.TRUE.equals(ip.getActualizada()) ? "Revisado" : "Pendiente";
                datos[i][7] = ip.getFechaActualiza() != null ? fmtFechaHora.format(ip.getFechaActualiza()) : "";
            }

            ReporteXLSX.creaContenidoTabla(datos, columnas);
            ReporteXLSX.setFinalParagraph(lista.size());
            ReporteXLSX.descargarExcel("Personas");
        } catch (Exception e) {
            log.error("Error al exportar listado de personas a Excel", e);
            JsfUtil.addErrorMessage("No se pudo generar el archivo Excel.");
        }
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
            String pathCompleto = Constantes.getPathListaMiembros(nombreArchivo, extencion);

            Documentos documentoNuevo = new Documentos(nombreArchivo, pathCompleto, new TipoDocumento(2),
                    iglesiaSeleccionado.getId(), extencion, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", nombreArchivo);
            documentoBean.guardarDocumento(documentoNuevo);

            Path path = Paths.get(pathCompleto);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
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
