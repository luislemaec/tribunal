package ec.com.antenasur.controller;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.primefaces.PrimeFaces;

import ec.com.antenasur.bean.DocumentoBean;
import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.itext.ReporteXLSX;
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

    // â”€â”€ Provincias (cargadas al inicio, compartidas entre filtro y diÃ¡logo) â”€â”€
    @Setter
    @Getter
    private List<Geograp> provincias;

    // â”€â”€ Estado exclusivo del filtro geogrÃ¡fico (toolbar) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Setter
    @Getter
    private Integer provinciaFiltroId;

    @Setter
    @Getter
    private List<Geograp> cantonesFiltro;

    @Setter
    @Getter
    private List<Geograp> parroquias;

    @Setter
    @Getter
    private Geograp parroquiaSeleccionado, cantonSeleccionado;

    // â”€â”€ Estado exclusivo del diÃ¡logo Nueva / Editar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Setter
    @Getter
    private Integer provinciaDialogoId;

    @Setter
    @Getter
    private List<Geograp> cantonesDialogo;

    /**
     * ID del cantÃ³n seleccionado dentro del diÃ¡logo. Se usa Integer puro (no
     * Geograp) para evitar ambigÃ¼edades del IntegerConverter de JSF al
     * renderizar el hidden select de p:selectOneMenu con filter="true".
     */
    @Setter
    @Getter
    private Integer cantonDialogoId;

    /** Parroquias cargadas dentro del diÃ¡logo â€” independientes de las del filtro. */
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

    /** Controla si la iglesia en ediciÃ³n tiene RUC propio. Default true. */
    @Setter
    @Getter
    private boolean tieneRuc = true;

    /**
     * Los botones "SÃ­, tiene RUC" / "No tiene RUC" solo se habilitan cuando
     * el campo igl_documento aÃºn estÃ¡ vacÃ­o: iglesia nueva o iglesia existente
     * que todavÃ­a no tiene documento asignado.
     * Una vez asignado (RUC real o cÃ³digo genÃ©rico) el tipo queda bloqueado.
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
            provinciaDialogoId = null;

            // Carga provincias dinÃ¡micamente usando el nodo 7 (provincia conocida)
            // como referencia para encontrar el padre (zona/paÃ­s) de todas las provincias.
            Geograp provRef = geograpService.find(7);
            if (provRef != null && provRef.getGeograp() != null) {
                provincias = geograpService.findByFatherId(provRef.getGeograp().getId());
            }
            if (provincias == null) {
                provincias = Collections.emptyList();
            }

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

    // â”€â”€ Filtro geogrÃ¡fico â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Filtro: cuando cambia la provincia carga los cantones correspondientes,
     * limpia cantÃ³n/parroquia y filtra la tabla por la provincia seleccionada.
     */
    public void obtieneCantonesFiltro() {
        cantonesFiltro = null;
        cantonSeleccionado = new Geograp();
        parroquias = null;
        parroquiaSeleccionado = new Geograp();

        if (provinciaFiltroId != null) {
            cantonesFiltro = geograpService.findByFatherId(provinciaFiltroId);
            List<Geograp> parroquiasDeProvincia = geograpService.obtenerParroquiasDeCantones(cantonesFiltro);
            if (parroquiasDeProvincia != null && !parroquiasDeProvincia.isEmpty()) {
                listaIglesias = iglesiaService.listarDTOsPorParroquias(parroquiasDeProvincia);
            } else {
                listaIglesias = Collections.emptyList();
            }
        } else {
            listaIglesias = iglesiaService.listarDTOsConFlagDocumentos(Constantes.LISTA_MIEMBROS);
        }
    }

    /** Filtro: cuando cambia el cantÃ³n carga parroquias y filtra la tabla. */
    public void obtieneParroquias() {
        if (cantonSeleccionado != null && cantonSeleccionado.getId() != null) {
            cantonSeleccionado = geograpService.find(cantonSeleccionado.getId());
            parroquias = geograpService.findByFatherId(cantonSeleccionado.getId());
            parroquiaSeleccionado = new Geograp();
            obtieneIglesiasPorCanton();
        }
    }

    private void obtieneIglesiasPorCanton() {
        if (parroquias != null && !parroquias.isEmpty()) {
            listaIglesias = iglesiaService.listarDTOsPorParroquias(parroquias);
        }
    }

    public void obtieneIglesiasPorParroquia() {
        if (parroquiaSeleccionado != null && parroquiaSeleccionado.getId() != null) {
            parroquiaSeleccionado = geograpService.find(parroquiaSeleccionado.getId());
            listaIglesias = iglesiaService.listarDTOsPorParroquia(parroquiaSeleccionado);
            PrimeFaces.current().ajax().update("frmIglesias", "msgs");
        }
    }

    // â”€â”€ DiÃ¡logo Nueva / Editar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * DiÃ¡logo: cuando cambia la provincia carga los cantones del diÃ¡logo,
     * limpia la selecciÃ³n de cantÃ³n y parroquia sin tocar el estado del filtro.
     */
    public void obtieneCantonesDialogo() {
        cantonDialogoId = null;
        cantonesDialogo = null;
        parroquiasDialogo = null;
        if (iglesiaSeleccionado != null) {
            iglesiaSeleccionado.setUbicacionId(null);
        }
        if (provinciaDialogoId != null) {
            cantonesDialogo = geograpService.findByFatherId(provinciaDialogoId);
        }
    }

    /**
     * DiÃ¡logo: cuando cambia el cantÃ³n carga las parroquias del diÃ¡logo.
     * No toca el estado del filtro geogrÃ¡fico ni listaIglesias.
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

    /**
     * Carga provincia, cantÃ³n y parroquias del diÃ¡logo a partir de la parroquia
     * de la iglesia. Sin tocar el estado del filtro.
     *
     * Estrategia 1 â€” datos del DTO (mÃ¡s confiable, viene del JOIN FETCH eager).
     * Estrategia 2 â€” SQL nativo sobre gelo_parent_id (robusto ante @Filter).
     * Estrategia 3 â€” navegaciÃ³n por entidad (fallback final).
     */
    private void cargarParroquiasParaDialogo(Integer parroquiaId) {
        cantonDialogoId = null;
        provinciaDialogoId = null;
        cantonesDialogo = null;
        parroquiasDialogo = null;

        if (parroquiaId == null) return;

        // Estrategia 1: cantonId y provinciaId del DTO
        if (iglesiaSeleccionado != null && iglesiaSeleccionado.getCantonId() != null) {
            cantonDialogoId = iglesiaSeleccionado.getCantonId();
            parroquiasDialogo = geograpService.findByFatherId(cantonDialogoId);
            if (iglesiaSeleccionado.getProvinciaId() != null) {
                provinciaDialogoId = iglesiaSeleccionado.getProvinciaId();
                cantonesDialogo = geograpService.findByFatherId(provinciaDialogoId);
            }
            return;
        }

        // Estrategia 2: gelo_parent_id por SQL nativo
        Geograp canton = geograpService.findParentOf(parroquiaId);
        if (canton != null && canton.getId() != null) {
            cantonDialogoId = canton.getId();
            parroquiasDialogo = geograpService.findByFatherId(cantonDialogoId);
            Geograp provincia = geograpService.findParentOf(cantonDialogoId);
            if (provincia != null) {
                provinciaDialogoId = provincia.getId();
                cantonesDialogo = geograpService.findByFatherId(provinciaDialogoId);
            }
            return;
        }

        // Estrategia 3: navegaciÃ³n directa por entidad
        Geograp parroquia = geograpService.find(parroquiaId);
        if (parroquia != null && parroquia.getGeograp() != null) {
            cantonDialogoId = parroquia.getGeograp().getId();
            parroquiasDialogo = geograpService.findByFatherId(cantonDialogoId);
            if (parroquia.getGeograp().getGeograp() != null) {
                provinciaDialogoId = parroquia.getGeograp().getGeograp().getId();
                cantonesDialogo = geograpService.findByFatherId(provinciaDialogoId);
            }
        }
    }

    // â”€â”€ Acciones de la tabla â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public void nuevaIglesia() {
        iglesiaSeleccionado = new IglesiaDTO();
        provinciaDialogoId = null;
        cantonesDialogo = null;
        cantonDialogoId = null;
        parroquiasDialogo = null;
        esNuevoRegistro = true;
        tieneRuc = true;
    }

    /**
     * EdiciÃ³n desde la columna Acciones.
     * Importante: este mÃ©todo se invoca vÃ­a atributo {@code action} (no
     * {@code actionListener}) para que se ejecute DESPUÃ‰S del
     * {@code <f:setPropertyActionListener>} hijo, garantizando que
     * {@code iglesiaSeleccionado} ya estÃ© asignado antes de leer sus campos.
     */
    public void editarIglesiaFila() {
        if (iglesiaSeleccionado == null) {
            log.warn("editarIglesiaFila: iglesiaSeleccionado es null â€” verifique que el botÃ³n use 'action' y no 'actionListener'");
            return;
        }
        esNuevoRegistro = false;
        tieneRuc = !esDocumentoGenerico(iglesiaSeleccionado.getDocumento());
        cargarParroquiasParaDialogo(iglesiaSeleccionado.getUbicacionId());
    }

    /** BotÃ³n "SÃ­, tiene RUC" â€” habilita el campo y limpia el cÃ³digo genÃ©rico si existÃ­a. */
    public void seleccionarConRuc() {
        tieneRuc = true;
        if (iglesiaSeleccionado != null && esDocumentoGenerico(iglesiaSeleccionado.getDocumento())) {
            iglesiaSeleccionado.setDocumento(null);
        }
    }

    /** BotÃ³n "No tiene RUC" â€” deshabilita el campo y asigna el siguiente cÃ³digo genÃ©rico (preview). */
    public void seleccionarSinRuc() {
        tieneRuc = false;
        if (iglesiaSeleccionado != null) {
            iglesiaSeleccionado.setDocumento(iglesiaService.generarDocumentoGenerico());
        }
    }

    /**
     * Identifica cÃ³digos genÃ©ricos. Real Ecuadorian RUCs jamÃ¡s empiezan con "00"
     * (cÃ³digos de provincia 01-24), por lo que el prefijo de 2 ceros es un
     * discriminador estable independiente del valor de la secuencia.
     */
    private boolean esDocumentoGenerico(String documento) {
        return documento != null && documento.startsWith("00");
    }

    /** EliminaciÃ³n desde la columna Acciones â€” iglesiaSeleccionado ya viene seteado via f:setPropertyActionListener. */
    public void eliminarIglesiaFila() {
        if (iglesiaSeleccionado == null) return;
        if (!cronogramaService.permiteRegistroIglesias()) {
            JsfUtil.addErrorMessage("El registro de iglesias no estÃ¡ habilitado en la fase electoral vigente.");
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
            JsfUtil.addErrorMessage("El registro de iglesias no estÃ¡ habilitado en la fase electoral vigente.");
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
                    log.error("Error al eliminar iglesia id={} en eliminaciÃ³n masiva", item.getId(), e);
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
            JsfUtil.addInfoMessage("Iglesia con RUC ya estÃ¡ registrada. Datos cargados para actualizaciÃ³n.");
        }
    }

    public void actualizarIglesia() {
        try {
            if (!cronogramaService.permiteRegistroIglesias()) {
                rechazarConMensaje("El registro de iglesias no estÃ¡ habilitado en la fase electoral vigente.");
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
            if (provinciaDialogoId == null) {
                rechazarConMensaje("Seleccione la provincia.");
                return;
            }
            if (cantonDialogoId == null) {
                rechazarConMensaje("Seleccione el cantÃ³n.");
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
                iglesiaSeleccionado = persistida;
                refrescarLista();
                PrimeFaces.current().ajax().update("frmIglesias", "frmStats", "frmProgreso", "msgs");
            } else {
                rechazarConMensaje("No se pudo guardar la iglesia. Verifique que la parroquia seleccionada sea vÃ¡lida.");
            }
        } catch (NegocioException e) {
            rechazarConMensaje(e.getMessage());
        } catch (Exception e) {
            log.error("Error al guardar iglesia", e);
            rechazarConMensaje("OcurriÃ³ un error inesperado al guardar. Intente nuevamente.");
        }
    }

    /**
     * Agrega el mensaje de error Y le indica a PrimeFaces que la acciÃ³n fallÃ³
     * (args.validationFailed = true en el oncomplete del botÃ³n Guardar),
     * evitando que el diÃ¡logo se cierre cuando hay errores de negocio.
     */
    private void rechazarConMensaje(String mensaje) {
        JsfUtil.addErrorMessage(mensaje);
        PrimeFaces.current().ajax().addCallbackParam("validationFailed", true);
    }

    public void cancelarIglesia() {
        iglesiaSeleccionado = null;
        provinciaFiltroId = null;
        cantonesFiltro = null;
        cantonSeleccionado = new Geograp();
        parroquias = null;
        parroquiaSeleccionado = new Geograp();
        provinciaDialogoId = null;
        cantonesDialogo = null;
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

    public void exportarExcel() {
        try {
            List<IglesiaDTO> lista = listaIglesias != null ? listaIglesias : Collections.emptyList();
            String fecha = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
            String hora = new SimpleDateFormat("HH:mm:ss").format(new Date());

            ReporteXLSX.nuevoExcel("Listado de Iglesias");
            ReporteXLSX.creaEspacioInformativo(fecha, hora, ReporteXLSX.getNombreUsuarioAutenticado());

            String[] columnas = {
                "NÂ°", "RUC / CÃ“DIGO", "NOMBRE", "COMUNIDAD / BARRIO",
                "PROVINCIA", "CANTÃ“N", "PARROQUIA", "TOTAL MIEMBROS", "DOCUMENTOS"
            };
            int[] anchos = { 2000, 5000, 9000, 7000, 5500, 5500, 5500, 4500, 3500 };
            ReporteXLSX.creaCabeceraTabla(columnas, anchos);

            String[][] datos = new String[lista.size()][columnas.length];
            for (int i = 0; i < lista.size(); i++) {
                IglesiaDTO ig = lista.get(i);
                datos[i][0] = String.valueOf(i + 1);
                datos[i][1] = ig.getDocumento() != null ? ig.getDocumento() : "";
                datos[i][2] = ig.getNombre() != null ? ig.getNombre() : "";
                datos[i][3] = ig.getComunidad() != null ? ig.getComunidad() : "";
                datos[i][4] = ig.getProvinciaNombre() != null ? ig.getProvinciaNombre() : "";
                datos[i][5] = ig.getCantonNombre() != null ? ig.getCantonNombre() : "";
                datos[i][6] = ig.getUbicacionNombre() != null ? ig.getUbicacionNombre() : "";
                datos[i][7] = ig.getTotalMiembros() != null ? String.valueOf(ig.getTotalMiembros()) : "";
                datos[i][8] = Boolean.TRUE.equals(ig.getTieneDocumentos()) ? "SÃ­" : "No";
            }

            ReporteXLSX.creaContenidoTabla(datos, columnas);
            ReporteXLSX.setFinalParagraph(lista.size());
            ReporteXLSX.descargarExcel("iglesias");
        } catch (Exception e) {
            log.error("Error al exportar Excel de iglesias", e);
            JsfUtil.addErrorMessage("No se pudo generar el archivo Excel.");
        }
    }
}
