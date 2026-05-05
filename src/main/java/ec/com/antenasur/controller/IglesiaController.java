package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;

import ec.com.antenasur.bean.DocumentoBean;
import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.dto.CronogramaFaseDTO;
import ec.com.antenasur.dto.IglesiaDTO;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.tec.Documentos;
import ec.com.antenasur.service.GeograpService;
import ec.com.antenasur.service.IglesiaService;
import ec.com.antenasur.service.tec.CronogramaService;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Named
@ViewScoped
@Slf4j
public class IglesiaController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private LoginBean loginBean;

    @Inject
    private DocumentoBean documentoBean;

    @Inject
    private IglesiaService iglesiaService;

    @Inject
    private GeograpService geograpService;

    @Inject
    private CronogramaService cronogramaService;

    @Setter
    @Getter
    private String prefijoRoles;

    @Setter
    @Getter
    private IglesiaDTO iglesiaSeleccionado;

    /** Lista de cantones — compartida entre filtros y diálogo (solo lectura en diálogo). */
    @Setter
    @Getter
    private List<Geograp> cantones;

    // ── Estado exclusivo del filtro geográfico (toolbar) ─────────────────────
    @Setter
    @Getter
    private List<Geograp> parroquias;

    @Setter
    @Getter
    private Geograp parroquiaSeleccionado, cantonSeleccionado;

    // ── Estado exclusivo del diálogo Nueva / Editar ──────────────────────────
    /**
     * ID del cantón seleccionado dentro del diálogo. Se usa Integer puro (no
     * Geograp) para evitar ambigüedades del IntegerConverter de JSF al
     * renderizar el hidden select de p:selectOneMenu con filter="true".
     */
    @Setter
    @Getter
    private Integer cantonDialogoId;

    /** Parroquias cargadas dentro del diálogo — independientes de las del filtro. */
    @Setter
    @Getter
    private List<Geograp> parroquiasDialogo;

    @Setter
    @Getter
    private List<IglesiaDTO> listaIglesias, listaIglesiasSeleccionadas, listaIglesiasFiltrada;

    @Setter
    @Getter
    private Boolean esNuevoRegistro;

    @Setter
    @Getter
    private List<Documentos> documentos;

    @Getter
    private CronogramaFaseDTO faseVigente;

    /** Bloquea los botones Nueva / Editar cuando la fase activa no es INSCRIPCION_IGLESIAS. */
    @Getter
    private boolean puedeRegistrarIglesia;

    /** [total, procesadas, porcentaje] para la barra de progreso de la fase activa. */
    @Getter
    private int[] progresoRegistro = {0, 0, 0};

    /** Controla si la iglesia en edición tiene RUC propio. Default true. */
    @Setter
    @Getter
    private boolean tieneRuc = true;

    /**
     * Los botones "Sí, tiene RUC" / "No tiene RUC" solo se habilitan cuando
     * el campo igl_documento aún está vacío: iglesia nueva o iglesia existente
     * que todavía no tiene documento asignado.
     * Una vez asignado (RUC real o código genérico) el tipo queda bloqueado.
     */
    public boolean isRucToggleHabilitado() {
        if (iglesiaSeleccionado == null) return false;
        String doc = iglesiaSeleccionado.getDocumento();
        return doc == null || doc.trim().isEmpty();
    }

    @PostConstruct
    private void init() {
        try {
            parroquiaSeleccionado = cantonSeleccionado = new Geograp();
            cantonDialogoId = null;
            cantones = geograpService.findByFatherId(7);
            listaIglesias = iglesiaService.listarDTOsConFlagDocumentos(Constantes.LISTA_MIEMBROS);
            esNuevoRegistro = false;
            faseVigente = cronogramaService.getFaseVigenteDelProcesoActivo();
            puedeRegistrarIglesia = cronogramaService.permiteRegistroIglesias();
            if (faseVigente != null) {
                progresoRegistro = iglesiaService.calcularProgresoRegistro(
                        faseVigente.getFechaInicio(), faseVigente.getFechaFin());
            }
        } catch (Exception e) {
            log.error("Error al inicializar IglesiaController", e);
        }
    }

    /** Usado por el filtro geográfico (toolbar): carga parroquias Y filtra la tabla por cantón. */
    public void obtieneParroquias() {
        if (cantonSeleccionado != null && cantonSeleccionado.getId() != null) {
            cantonSeleccionado = geograpService.find(cantonSeleccionado.getId());
            parroquias = geograpService.findByFatherId(cantonSeleccionado.getId());
            obtieneIglesiasPorCanton();
        }
    }

    /**
     * Usado por el combo de cantón dentro del diálogo: carga parroquias en
     * parroquiasDialogo y fuerza re-selección de parroquia. No toca el estado
     * del filtro geográfico ni listaIglesias.
     */
    public void obtieneParroquiasEnDialogo() {
        if (cantonDialogoId != null) {
            parroquiasDialogo = geograpService.findByFatherId(cantonDialogoId);
            if (iglesiaSeleccionado != null) {
                iglesiaSeleccionado.setUbicacionId(null);
            }
        } else {
            parroquiasDialogo = null;
        }
    }

    private void obtieneIglesiasPorCanton() {
        if (parroquias != null && !parroquias.isEmpty()) {
            listaIglesias = iglesiaService.listarDTOsPorParroquias(parroquias);
        }
    }

    /**
     * Carga el id del cantón y la lista de parroquias del diálogo a partir de
     * la parroquia de la iglesia. Sin tocar el estado del filtro.
     *
     * Estrategia 1 — cantonId del DTO (más confiable, viene del JOIN FETCH eager).
     * Estrategia 2 — SQL nativo sobre gelo_parent_id (robusto ante @Filter).
     * Estrategia 3 — navegación por entidad (fallback final).
     */
    private void cargarParroquiasParaDialogo(Integer parroquiaId) {
        cantonDialogoId = null;
        parroquiasDialogo = null;

        if (parroquiaId == null) return;

        // Estrategia 1: cantonId del DTO
        if (iglesiaSeleccionado != null && iglesiaSeleccionado.getCantonId() != null) {
            cantonDialogoId = iglesiaSeleccionado.getCantonId();
            parroquiasDialogo = geograpService.findByFatherId(cantonDialogoId);
            return;
        }

        // Estrategia 2: gelo_parent_id por SQL nativo
        Geograp canton = geograpService.findParentOf(parroquiaId);
        if (canton != null && canton.getId() != null) {
            cantonDialogoId = canton.getId();
            parroquiasDialogo = geograpService.findByFatherId(cantonDialogoId);
            return;
        }

        // Estrategia 3: navegación directa por entidad
        Geograp parroquia = geograpService.find(parroquiaId);
        if (parroquia != null && parroquia.getGeograp() != null
                && parroquia.getGeograp().getId() != null) {
            cantonDialogoId = parroquia.getGeograp().getId();
            parroquiasDialogo = geograpService.findByFatherId(cantonDialogoId);
        }
    }

    public void obtieneIglesiasPorParroquia() {
        if (parroquiaSeleccionado != null && parroquiaSeleccionado.getId() != null) {
            parroquiaSeleccionado = geograpService.find(parroquiaSeleccionado.getId());
            listaIglesias = iglesiaService.listarDTOsPorParroquia(parroquiaSeleccionado);
            PrimeFaces.current().ajax().update("frmIglesias", "msgs");
        }
    }

    public void nuevaIglesia() {
        iglesiaSeleccionado = new IglesiaDTO();
        cantonDialogoId = null;
        parroquiasDialogo = null;
        esNuevoRegistro = true;
        tieneRuc = true;
    }

    /**
     * Edición desde la columna Acciones.
     * Importante: este método se invoca vía atributo {@code action} (no
     * {@code actionListener}) para que se ejecute DESPUÉS del
     * {@code <f:setPropertyActionListener>} hijo, garantizando que
     * {@code iglesiaSeleccionado} ya esté asignado antes de leer sus campos.
     */
    public void editarIglesiaFila() {
        if (iglesiaSeleccionado == null) {
            log.warn("editarIglesiaFila: iglesiaSeleccionado es null — verifique que el botón use 'action' y no 'actionListener'");
            return;
        }
        esNuevoRegistro = false;
        tieneRuc = !esDocumentoGenerico(iglesiaSeleccionado.getDocumento());
        cargarParroquiasParaDialogo(iglesiaSeleccionado.getUbicacionId());
    }

    /** Botón "Sí, tiene RUC" — habilita el campo y limpia el código genérico si existía. */
    public void seleccionarConRuc() {
        tieneRuc = true;
        if (iglesiaSeleccionado != null && esDocumentoGenerico(iglesiaSeleccionado.getDocumento())) {
            iglesiaSeleccionado.setDocumento(null);
        }
    }

    /** Botón "No tiene RUC" — deshabilita el campo y asigna el siguiente código genérico (preview). */
    public void seleccionarSinRuc() {
        tieneRuc = false;
        if (iglesiaSeleccionado != null) {
            iglesiaSeleccionado.setDocumento(iglesiaService.generarDocumentoGenerico());
        }
    }

    private boolean esDocumentoGenerico(String documento) {
        return documento != null && documento.startsWith("000000000000");
    }

    /** Eliminación desde la columna Acciones — iglesiaSeleccionado ya viene seteado via f:setPropertyActionListener. */
    public void eliminarIglesiaFila() {
        if (iglesiaSeleccionado == null) return;
        // Re-validar permiso en el momento del click, no solo al cargar la página
        if (!cronogramaService.permiteRegistroIglesias()) {
            JsfUtil.addErrorMessage("El registro de iglesias no está habilitado en la fase electoral vigente.");
            iglesiaSeleccionado = null;
            return;
        }
        try {
            iglesiaService.eliminarPorId(iglesiaSeleccionado.getId());
            JsfUtil.addInfoMessage("Iglesia eliminada: " + iglesiaSeleccionado.getNombre());
            iglesiaSeleccionado = null;
            refrescarLista();
            PrimeFaces.current().ajax().update("frmIglesias", "frmProgreso", "frmStats", "msgs");
        } catch (Exception e) {
            log.error("Error al eliminar iglesia id={}", iglesiaSeleccionado.getId(), e);
            JsfUtil.addErrorMessage("No se pudo eliminar la iglesia. Intente nuevamente.");
        }
    }

    public boolean existeIglesiasSeleccionadas() {
        return this.listaIglesiasSeleccionadas != null && !this.listaIglesiasSeleccionadas.isEmpty();
    }

    public String getMensajeBotonEliminar() {
        if (existeIglesiasSeleccionadas()) {
            int size = this.listaIglesiasSeleccionadas.size();
            return size > 1 ? size + " Iglesias seleccionadas" : "1 iglesia seleccionada";
        }
        return "Eliminar";
    }

    public void eliminarIglesiaSeleccionadas() {
        if (!cronogramaService.permiteRegistroIglesias()) {
            JsfUtil.addErrorMessage("El registro de iglesias no está habilitado en la fase electoral vigente.");
            return;
        }
        if (listaIglesiasSeleccionadas != null) {
            int eliminadas = 0;
            for (IglesiaDTO item : listaIglesiasSeleccionadas) {
                try {
                    if (iglesiaService.eliminarPorId(item.getId()) != null) {
                        eliminadas++;
                    }
                } catch (Exception e) {
                    log.error("Error al eliminar iglesia id={} en eliminación masiva", item.getId(), e);
                }
            }
            JsfUtil.addInfoMessage(eliminadas + " iglesia(s) eliminada(s)");
            listaIglesiasSeleccionadas = null;
        }
        refrescarLista();
        PrimeFaces.current().ajax().update("frmIglesias", "frmStats", "frmProgreso", "msgs");
    }

    public void buscaIglesiaPorDocumento() {
        if (iglesiaSeleccionado == null
                || iglesiaSeleccionado.getDocumento() == null
                || iglesiaSeleccionado.getDocumento().trim().isEmpty()) {
            return;
        }
        IglesiaDTO encontrada = iglesiaService.buscarDTOPorDocumento(iglesiaSeleccionado.getDocumento());
        if (encontrada != null) {
            iglesiaSeleccionado = encontrada;
            cargarParroquiasParaDialogo(encontrada.getUbicacionId());
            esNuevoRegistro = false;
            tieneRuc = !esDocumentoGenerico(encontrada.getDocumento());
            JsfUtil.addInfoMessage("Iglesia con RUC ya está registrada. Datos cargados para actualización.");
        }
    }

    public void actualizarIglesia() {
        try {
            if (!cronogramaService.permiteRegistroIglesias()) {
                rechazarConMensaje("El registro de iglesias no está habilitado en la fase electoral vigente.");
                return;
            }
            if (iglesiaSeleccionado == null) {
                rechazarConMensaje("Complete los datos requeridos.");
                return;
            }
            if (iglesiaSeleccionado.getNombre() == null || iglesiaSeleccionado.getNombre().trim().isEmpty()) {
                rechazarConMensaje("El nombre de la iglesia es obligatorio.");
                return;
            }
            if (cantonDialogoId == null) {
                rechazarConMensaje("Seleccione el cantón.");
                return;
            }
            if (iglesiaSeleccionado.getUbicacionId() == null) {
                rechazarConMensaje("Seleccione la parroquia.");
                return;
            }
            if (tieneRuc && (iglesiaSeleccionado.getDocumento() == null
                    || iglesiaSeleccionado.getDocumento().trim().isEmpty())) {
                rechazarConMensaje("El RUC es obligatorio cuando la iglesia tiene RUC.");
                return;
            }
            if (iglesiaService.existeDuplicado(
                    iglesiaSeleccionado.getNombre(),
                    iglesiaSeleccionado.getUbicacionId(),
                    iglesiaSeleccionado.getComunidad(),
                    iglesiaSeleccionado.getId())) {
                rechazarConMensaje("Ya existe una iglesia con el mismo nombre, parroquia y comunidad/barrio.");
                return;
            }
            boolean esEdicion = iglesiaSeleccionado.getId() != null;
            IglesiaDTO persistida = iglesiaService.guardarDesdeDTO(iglesiaSeleccionado);
            if (persistida != null) {
                JsfUtil.addSuccessMessage(esEdicion ? "Iglesia actualizada correctamente." : "Iglesia registrada correctamente.");
                // No anulamos iglesiaSeleccionado: el form del diálogo aún se re-renderiza
                // por update="@form" antes de cerrarse vía oncomplete, y un null haría
                // explotar bindings como #{iglesiaSeleccionado.ubicacionId}.
                // Lo reemplazamos por el DTO persistido (con id, version y demás campos
                // actualizados). nuevaIglesia()/editarIglesiaFila() lo sobrescribirán
                // en la próxima apertura del diálogo.
                iglesiaSeleccionado = persistida;
                refrescarLista();
                PrimeFaces.current().ajax().update("frmIglesias", "frmStats", "frmProgreso", "msgs");
            } else {
                rechazarConMensaje("No se pudo guardar la iglesia. Verifique que la parroquia seleccionada sea válida.");
            }
        } catch (NegocioException e) {
            // Regla de negocio (RUC duplicado, conflicto de versión, etc.) — no loguear stack trace
            rechazarConMensaje(e.getMessage());
        } catch (Exception e) {
            log.error("Error al guardar iglesia", e);
            rechazarConMensaje("Ocurrió un error inesperado al guardar. Intente nuevamente.");
        }
    }

    /**
     * Agrega el mensaje de error Y le indica a PrimeFaces que la acción falló
     * (args.validationFailed = true en el oncomplete del botón Guardar),
     * evitando que el diálogo se cierre cuando hay errores de negocio.
     */
    private void rechazarConMensaje(String mensaje) {
        JsfUtil.addErrorMessage(mensaje);
        PrimeFaces.current().ajax().addCallbackParam("validationFailed", true);
    }

    public void cancelarIglesia() {
        iglesiaSeleccionado = null;
        cantonDialogoId = null;
        parroquiasDialogo = null;
        refrescarLista();
        PrimeFaces.current().ajax().update("frmIglesias", "frmFiltros", "msgs");
    }

    private void refrescarLista() {
        listaIglesias = iglesiaService.listarDTOsConFlagDocumentos(Constantes.LISTA_MIEMBROS);
        faseVigente = cronogramaService.getFaseVigenteDelProcesoActivo();
        puedeRegistrarIglesia = cronogramaService.permiteRegistroIglesias();
        if (faseVigente != null) {
            progresoRegistro = iglesiaService.calcularProgresoRegistro(
                    faseVigente.getFechaInicio(), faseVigente.getFechaFin());
        }
    }

    public long getIglesiasConDocumentos() {
        if (listaIglesias == null) {
            return 0;
        }
        long count = 0;
        for (IglesiaDTO i : listaIglesias) {
            if (Boolean.TRUE.equals(i.getTieneDocumentos())) {
                count++;
            }
        }
        return count;
    }

    public long getIglesiasSinDocumentos() {
        if (listaIglesias == null) {
            return 0;
        }
        return listaIglesias.size() - getIglesiasConDocumentos();
    }

    public int getCantonesCount() {
        return cantones != null ? cantones.size() : 0;
    }

    public void cargaArchivosListaMiembros() {
        try {
            if (iglesiaSeleccionado != null && iglesiaSeleccionado.getId() != null) {
                documentos = documentoBean.getDocumentosPorEntidadYTipoDoc(iglesiaSeleccionado.getId(), Constantes.LISTA_MIEMBROS);
            }
        } catch (Exception e) {
            log.error("Error al obtener documentos de iglesia id={}", iglesiaSeleccionado != null ? iglesiaSeleccionado.getId() : null, e);
            JsfUtil.addErrorMessage("No se pudieron cargar los documentos.");
        }
    }
}
