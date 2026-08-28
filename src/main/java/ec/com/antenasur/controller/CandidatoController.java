package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.primefaces.PrimeFaces;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;
import org.primefaces.event.FileUploadEvent;

import ec.com.antenasur.dto.CandidatoDTO;
import ec.com.antenasur.dto.EstadoActaInscripcionDTO;
import ec.com.antenasur.dto.ListaDTO;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.model.tec.CatalogoGeneral;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.model.tec.Documentos;
import ec.com.antenasur.service.tec.ActaInscripcionService;
import ec.com.antenasur.service.tec.CandidatoService;
import ec.com.antenasur.service.tec.CatalogoGeneralService;
import ec.com.antenasur.service.tec.ListaService;
import ec.com.antenasur.service.tec.ProcesoElectoralService;
import ec.com.antenasur.util.JsfUtil;
import ec.com.antenasur.util.Constantes;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Named
@ViewScoped
@Slf4j
public class CandidatoController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String FORMULARIO = "frmCandidatos";
    private static final String FORMULARIO_ABSOLUTO = ":" + FORMULARIO;
    private static final String GROWL_GLOBAL = ":frmGlobal:growlGlobal";
    private static final String TABLA_CANDIDATOS = "tblCandidatos";
    private static final String TABLA_LISTAS = "tblListas";

    @Inject
    private ListaService listaService;
    @Inject
    private CandidatoService candidatoService;
    @Inject
    private CatalogoGeneralService catalogoService;
    @Inject
    private ProcesoElectoralService procesoElectoralService;
    @Inject
    private ActaInscripcionService actaInscripcionService;

    @Setter @Getter
    private ListaDTO listaSeleccionado;
    @Setter @Getter
    private ListaDTO listaEdicion;
    @Getter
    private LazyDataModel<ListaDTO> listas;
    @Setter @Getter
    private String filtroLista;
    @Setter @Getter
    private boolean incluirListasInactivas;
    @Setter @Getter
    private List<CandidatoDTO> candidatos;
    @Setter @Getter
    private List<CatalogoGeneral> cargosCandidatos;
    @Setter @Getter
    private ProcesoElectoral procesoActivo;
    @Setter @Getter
    private CandidatoDTO candidatoSeleccionado;
    @Setter @Getter
    private String cedulaBuscar;
    @Getter
    private EstadoActaInscripcionDTO estadoActa;
    @Getter
    private List<Documentos> documentosActa;

    @PostConstruct
    private void init() {
        try {
            listaEdicion = new ListaDTO();
            candidatos = new ArrayList<>();
            documentosActa = new ArrayList<>();
            estadoActa = new EstadoActaInscripcionDTO();
            procesoActivo = procesoElectoralService.getActivo();
            cargosCandidatos = catalogoService.listaCatalogoHijo(8);
            inicializarModeloListas();
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR MODULO DE CANDIDATOS", e);
        }
    }

    public void seleccionarLista(ListaDTO lista) {
        if (lista == null || lista.getId() == null || !Boolean.TRUE.equals(lista.getEstado())) {
            JsfUtil.addWarningMessage("Seleccione una lista activa para administrar sus candidatos.");
            return;
        }
        listaSeleccionado = copiarLista(lista);
        candidatoSeleccionado = null;
        cedulaBuscar = null;
        obtieneCandidatosPorListaSeleccionada();
    }

    public void obtieneCandidatosPorListaSeleccionada() {
        if (listaSeleccionado == null || listaSeleccionado.getId() == null || cargosCandidatos == null) {
            candidatos = new ArrayList<>();
            documentosActa = new ArrayList<>();
            estadoActa = new EstadoActaInscripcionDTO();
            return;
        }
        Integer procesoId = procesoActivo != null ? procesoActivo.getId() : null;
        candidatos = candidatoService.listarDTOsPorListaConCargos(
                listaSeleccionado.getId(), procesoId, cargosCandidatos);
        actualizarEstadoActa();
    }

    public void prepararNuevaLista() {
        listaEdicion = new ListaDTO();
    }

    public void prepararEdicionLista(ListaDTO lista) {
        listaEdicion = copiarLista(lista);
    }

    public void guardarLista() {
        try {
            boolean esEdicion = listaEdicion != null && listaEdicion.getId() != null;
            ListaDTO persistida = listaService.guardarDesdeDTO(listaEdicion,
                    procesoActivo != null ? procesoActivo.getId() : null);
            // Relee el registro persistido y reconstruye el modelo lazy para
            // que la tabla no conserve una pagina anterior tras la edicion.
            ListaDTO listaActualizada = listaService.obtenerDTOPorId(persistida.getId());
            listaEdicion = copiarLista(listaActualizada);
            inicializarModeloListas();
            if (!esEdicion || listaSeleccionado == null
                    || listaActualizada.getId().equals(listaSeleccionado.getId())) {
                listaSeleccionado = copiarLista(listaActualizada);
                obtieneCandidatosPorListaSeleccionada();
            }
            JsfUtil.addSuccessMessage(esEdicion ? "Lista actualizada correctamente." : "Lista registrada correctamente.");
            actualizarListasYDetalle();
            PrimeFaces.current().executeScript("PF('dlgLista').hide()");
        } catch (NegocioException e) {
            mostrarErrorValidacion(e.getMessage());
        } catch (Exception e) {
            log.error("ERROR AL GUARDAR LISTA", e);
            mostrarErrorValidacion("No se pudo guardar la lista.");
        }
    }

    public void desactivarLista(ListaDTO lista) {
        try {
            if (listaService.eliminarPorId(lista != null ? lista.getId() : null) == null) {
                JsfUtil.addWarningMessage("La lista ya no está disponible.");
                return;
            }
            if (listaSeleccionado != null && listaSeleccionado.getId().equals(lista.getId())) {
                listaSeleccionado = null;
                candidatos = new ArrayList<>();
                documentosActa = new ArrayList<>();
                estadoActa = new EstadoActaInscripcionDTO();
            }
            inicializarModeloListas();
            JsfUtil.addSuccessMessage("Lista dada de baja correctamente.");
            actualizarListasYDetalle();
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
            PrimeFaces.current().ajax().update(GROWL_GLOBAL);
        } catch (Exception e) {
            log.error("ERROR AL DAR DE BAJA LA LISTA", e);
            JsfUtil.addErrorMessage("No se pudo dar de baja la lista.");
            PrimeFaces.current().ajax().update(GROWL_GLOBAL);
        }
    }

    public void reactivarLista(ListaDTO lista) {
        try {
            ListaDTO reactivada = listaService.reactivarPorId(lista != null ? lista.getId() : null,
                    procesoActivo != null ? procesoActivo.getId() : null);
            listaSeleccionado = copiarLista(reactivada);
            obtieneCandidatosPorListaSeleccionada();
            inicializarModeloListas();
            JsfUtil.addSuccessMessage("Lista reactivada correctamente.");
            actualizarListasYDetalle();
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
            PrimeFaces.current().ajax().update(GROWL_GLOBAL);
        } catch (Exception e) {
            log.error("ERROR AL REACTIVAR LA LISTA", e);
            JsfUtil.addErrorMessage("No se pudo reactivar la lista.");
            PrimeFaces.current().ajax().update(GROWL_GLOBAL);
        }
    }

    /** Prepara una copia de la plaza para asignar o reasignar sin alterar la tabla. */
    public void prepararAsignacionCandidato(CandidatoDTO candidato) {
        if (listaSeleccionado == null || candidato == null || candidato.getListaId() == null
                || candidato.getProcesoId() == null || candidato.getCargoId() == null) {
            JsfUtil.addWarningMessage("No se pudo determinar la candidatura a asignar.");
            return;
        }
        if (!listaSeleccionado.getId().equals(candidato.getListaId())) {
            JsfUtil.addWarningMessage("La candidatura no corresponde a la lista seleccionada.");
            return;
        }
        CandidatoDTO seleccion = new CandidatoDTO();
        seleccion.setId(candidato.getId());
        seleccion.setListaId(candidato.getListaId());
        seleccion.setListaNombre(candidato.getListaNombre());
        seleccion.setListaNumero(candidato.getListaNumero());
        seleccion.setProcesoId(candidato.getProcesoId());
        seleccion.setProcesoNombre(candidato.getProcesoNombre());
        seleccion.setPeriodoId(candidato.getPeriodoId());
        seleccion.setPeriodoNombre(candidato.getPeriodoNombre());
        seleccion.setCargoId(candidato.getCargoId());
        seleccion.setCargoNombre(candidato.getCargoNombre());
        seleccion.setIglesiaPersona(candidato.getIglesiaPersona());
        candidatoSeleccionado = seleccion;
        cedulaBuscar = null;
    }

    public void buscaPersona() {
        try {
            CandidatoDTO actualizado = candidatoService.asignarPersonaPorCedula(candidatoSeleccionado, cedulaBuscar);
            if (actualizado != null) {
                candidatoSeleccionado = actualizado;
                JsfUtil.addInfoMessage("PERSONA SELECCIONADA");
            } else {
                JsfUtil.addWarningMessage("PERSONA NO ENCONTRADA");
            }
            PrimeFaces.current().ajax().update(FORMULARIO_ABSOLUTO + ":outPnlAsignaCandidatoBusca",
                    FORMULARIO_ABSOLUTO + ":outPnlAsignaCandidato", FORMULARIO_ABSOLUTO + ":outPnlFooter",
                    GROWL_GLOBAL);
        } catch (NegocioException e) {
            mostrarErrorValidacion(e.getMessage());
        } catch (Exception e) {
            log.error("ERROR AL BUSCAR PERSONA PARA CANDIDATO", e);
            mostrarErrorValidacion("No se pudo buscar la persona.");
        }
    }

    public void guardarCandidato() {
        try {
            if (candidatoSeleccionado == null) {
                mostrarErrorValidacion("Seleccione una candidatura para guardar.");
                return;
            }
            if (listaSeleccionado == null || listaSeleccionado.getId() == null
                    || !listaSeleccionado.getId().equals(candidatoSeleccionado.getListaId())) {
                mostrarErrorValidacion("La candidatura ya no corresponde a la lista seleccionada.");
                return;
            }
            boolean esEdicion = candidatoSeleccionado.getId() != null;
            CandidatoDTO persistido = candidatoService.guardarDesdeDTO(candidatoSeleccionado);
            if (persistido != null) {
                candidatoSeleccionado = persistido;
                obtieneCandidatosPorListaSeleccionada();
                JsfUtil.addSuccessMessage(esEdicion ? "Candidato reasignado correctamente." : "Candidato registrado correctamente.");
                PrimeFaces.current().ajax().update(FORMULARIO_ABSOLUTO + ":" + TABLA_CANDIDATOS, GROWL_GLOBAL);
                PrimeFaces.current().ajax().update(FORMULARIO_ABSOLUTO + ":panelActaInscripcion");
                PrimeFaces.current().executeScript("PF('dlgAsignaCandidato').hide()");
            }
        } catch (NegocioException e) {
            mostrarErrorValidacion(e.getMessage());
        } catch (Exception e) {
            log.error("ERROR AL GUARDAR CANDIDATO", e);
            mostrarErrorValidacion("No se pudo guardar el candidato.");
        }
    }

    public void eliminarCandidato(CandidatoDTO candidato) {
        try {
            if (candidato == null || candidato.getId() == null) {
                JsfUtil.addWarningMessage("Seleccione un candidato asignado para eliminar.");
                return;
            }
            if (listaSeleccionado == null || !listaSeleccionado.getId().equals(candidato.getListaId())) {
                JsfUtil.addWarningMessage("El candidato no corresponde a la lista seleccionada.");
                PrimeFaces.current().ajax().update(GROWL_GLOBAL);
                return;
            }
            if (candidatoService.eliminarPorId(candidato.getId()) == null) {
                JsfUtil.addWarningMessage("El candidato ya no está disponible.");
                return;
            }
            obtieneCandidatosPorListaSeleccionada();
            candidatoSeleccionado = null;
            JsfUtil.addInfoMessage("Candidato dado de baja correctamente.");
            PrimeFaces.current().ajax().update(FORMULARIO_ABSOLUTO + ":" + TABLA_CANDIDATOS, GROWL_GLOBAL);
            PrimeFaces.current().ajax().update(FORMULARIO_ABSOLUTO + ":panelActaInscripcion");
        } catch (Exception e) {
            log.error("ERROR AL ELIMINAR CANDIDATO", e);
            JsfUtil.addErrorMessage("No se pudo eliminar el candidato.");
            PrimeFaces.current().ajax().update(GROWL_GLOBAL);
        }
    }

    public void generarActaInscripcion() {
        try {
            validarListaSeleccionada();
            Documentos documento = actaInscripcionService.generar(
                    listaSeleccionado.getId(), procesoActivo != null ? procesoActivo.getId() : null);
            actualizarEstadoActa();
            JsfUtil.addSuccessMessage(Constantes.getMensaje(
                    "form.candidatos.acta.success.generated", documento.getNombre()));
            PrimeFaces.current().ajax().update(FORMULARIO_ABSOLUTO + ":panelActaInscripcion", GROWL_GLOBAL);
        } catch (NegocioException e) {
            mostrarErrorValidacion(e.getMessage());
        } catch (Exception e) {
            log.error("ERROR AL GENERAR ACTA DE INSCRIPCION", e);
            mostrarErrorValidacion(Constantes.getMensaje("form.candidatos.acta.error.generate"));
        }
    }

    public void cargarActaFirmada(FileUploadEvent event) {
        try {
            validarListaSeleccionada();
            if (event == null || event.getFile() == null) {
                throw new NegocioException(Constantes.getMensaje("form.candidatos.acta.error.select.file"));
            }
            Documentos documento = actaInscripcionService.cargarFirmada(
                    listaSeleccionado.getId(), procesoActivo != null ? procesoActivo.getId() : null,
                    event.getFile().getFileName(), event.getFile().getContent());
            actualizarEstadoActa();
            JsfUtil.addSuccessMessage(Constantes.getMensaje(
                    "form.candidatos.acta.success.uploaded", documento.getNombre()));
            PrimeFaces.current().ajax().update(FORMULARIO_ABSOLUTO + ":panelActaInscripcion", GROWL_GLOBAL);
        } catch (NegocioException e) {
            mostrarErrorValidacion(e.getMessage());
        } catch (Exception e) {
            log.error("ERROR AL CARGAR ACTA DE INSCRIPCION FIRMADA", e);
            mostrarErrorValidacion(Constantes.getMensaje("form.candidatos.acta.error.upload"));
        }
    }

    private void actualizarEstadoActa() {
        if (listaSeleccionado == null || listaSeleccionado.getId() == null || procesoActivo == null) {
            estadoActa = new EstadoActaInscripcionDTO();
            documentosActa = new ArrayList<>();
            return;
        }
        estadoActa = actaInscripcionService.evaluar(listaSeleccionado.getId(), procesoActivo.getId());
        documentosActa = actaInscripcionService.listarDocumentos(listaSeleccionado.getId());
    }

    private void validarListaSeleccionada() {
        if (listaSeleccionado == null || listaSeleccionado.getId() == null) {
            throw new NegocioException(Constantes.getMensaje("form.candidatos.acta.error.select.list"));
        }
    }

    private void inicializarModeloListas() {
        listas = new LazyDataModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public List<ListaDTO> load(int first, int pageSize, Map<String, SortMeta> sortBy,
                    Map<String, FilterMeta> filterBy) {
                SortMeta orden = sortBy == null || sortBy.isEmpty()
                        ? null : sortBy.values().iterator().next();
                String campo = orden != null ? orden.getField() : "nombre";
                boolean ascendente = orden == null || orden.getOrder() != SortOrder.DESCENDING;
                List<ListaDTO> pagina = listaService.buscarPaginado(filtroLista,
                        incluirListasInactivas, first, pageSize, campo, ascendente);
                return pagina;
            }

            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                return listaService.contar(filtroLista, incluirListasInactivas);
            }
        };
    }

    private void actualizarListasYDetalle() {
        PrimeFaces.current().ajax().update(FORMULARIO_ABSOLUTO + ":" + TABLA_LISTAS,
                FORMULARIO_ABSOLUTO + ":panelDetalle", GROWL_GLOBAL);
    }

    private void mostrarErrorValidacion(String mensaje) {
        JsfUtil.addErrorMessage(mensaje);
        FacesContext.getCurrentInstance().validationFailed();
        PrimeFaces.current().ajax().update(GROWL_GLOBAL);
    }

    private ListaDTO copiarLista(ListaDTO lista) {
        if (lista == null) {
            return null;
        }
        return new ListaDTO(lista.getId(), lista.getNombre(), lista.getSlogan(), lista.getNumero(), lista.getEstado());
    }
}
