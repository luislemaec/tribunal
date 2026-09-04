package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.primefaces.PrimeFaces;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.dto.CatalogoGeneralDTO;
import ec.com.antenasur.dto.ProcesoElectoralDTO;
import ec.com.antenasur.dto.TribunalDTO;
import ec.com.antenasur.service.tec.CatalogoGeneralService;
import ec.com.antenasur.service.tec.ProcesoElectoralService;
import ec.com.antenasur.service.tec.TribunalService;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Named
@ViewScoped
@Slf4j
public class TribunalController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Integer ID_CARGO_PADRE = 2;
    private static final String FORMULARIO = "frmPeriodos";
    private static final String TABLA_AUTORIDADES = FORMULARIO + ":tbtribunal";
    private static final String PANEL_BUSQUEDA = FORMULARIO + ":outPnlAsignaAutoridadBusca";
    private static final String PANEL_AUTORIDAD = FORMULARIO + ":outPnlAsignaAutoridad";
    private static final String PANEL_FOOTER = FORMULARIO + ":outPnlFooter";
    private static final String GROWL_GLOBAL = "frmGlobal:growlGlobal";

    @Inject
    private LoginBean loginBean;

    @Inject
    private ProcesoElectoralService procesoElectoralService;

    @Inject
    private TribunalService tribunalService;

    @Inject
    private CatalogoGeneralService catalogoService;

    @Setter
    @Getter
    private ProcesoElectoralDTO periodoSeleccionado;

    @Setter
    @Getter
    private TribunalDTO tribunalSeleccionado;

    @Setter
    @Getter
    private List<ProcesoElectoralDTO> listaPeriodos, listaPeriodosSeleccionados;

    @Setter
    @Getter
    private List<CatalogoGeneralDTO> listaCargos;

    @Setter
    @Getter
    private List<TribunalDTO> listaAutoridadesTribunal;

    @Setter
    @Getter
    private String cedulaBuscar;

    @PostConstruct
    private void init() {
        try {
            listaPeriodos = procesoElectoralService.listarDTOs();
            if (listaPeriodos != null && !listaPeriodos.isEmpty()) {
                periodoSeleccionado = listaPeriodos.get(0);
            }
            listaCargos = catalogoService.listarDTOsHijosDe(ID_CARGO_PADRE);
            cargarAutoridadesTribunal();
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    private void cargarAutoridadesTribunal() {
        Integer periodoId = (periodoSeleccionado != null) ? periodoSeleccionado.getId() : null;
        listaAutoridadesTribunal = tribunalService.listarAutoridadesConPlaceholders(periodoId, ID_CARGO_PADRE);
    }

    public void inicializaPersonaSeleccionado() {
        periodoSeleccionado = new ProcesoElectoralDTO();
    }

    public void nuevo() {
        inicializaPersonaSeleccionado();
    }

    public boolean existePeriodosSeleccionadas() {
        return this.listaPeriodosSeleccionados != null && !this.listaPeriodosSeleccionados.isEmpty();
    }

    public boolean isPuedeGestionarAutoridades() {
        return loginBean != null && loginBean.getRoles() != null
                && (loginBean.getRoles().contains("SITEC-Administrador")
                || loginBean.getRoles().contains("SITEC-Tribunal"));
    }

    public String getMensajeBotonEliminar() {
        if (existePeriodosSeleccionadas()) {
            int size = this.listaPeriodosSeleccionados.size();
            return size > 1 ? size + " periodos seleccionadas" : "1 periodo seleccionada";
        }
        return "Eliminar";
    }

    public void eliminarPeriodoSeleccionados() {
        int eliminados = 0;
        if (listaPeriodosSeleccionados != null) {
            for (ProcesoElectoralDTO item : listaPeriodosSeleccionados) {
                if (item.getId() != null && procesoElectoralService.eliminarPorId(item.getId()) != null) {
                    eliminados++;
                }
            }
        }
        JsfUtil.addInfoMessage(eliminados + " Periodos eliminados");
        PrimeFaces.current().ajax().update("frmPeriodos:pnListaPeriodos", "msgs");
        this.listaPeriodosSeleccionados = null;
    }

    public void eliminarAutoridadSeleccionada() {
        try {
            if (tribunalSeleccionado == null || tribunalSeleccionado.getId() == null) {
                JsfUtil.addWarningMessageFromBundle("autoridades.mensaje.no.seleccionada");
                PrimeFaces.current().ajax().update(GROWL_GLOBAL);
                return;
            }
            TribunalDTO eliminada = tribunalService.eliminarPorId(tribunalSeleccionado.getId());
            if (eliminada == null) {
                JsfUtil.addWarningMessageFromBundle("autoridades.mensaje.no.eliminada");
            } else {
                JsfUtil.addSuccessMessageFromBundle("autoridades.mensaje.eliminada");
            }
            tribunalSeleccionado = null;
            cargarAutoridadesTribunal();
            PrimeFaces.current().ajax().update(TABLA_AUTORIDADES, GROWL_GLOBAL);
        } catch (Exception e) {
            log.error("ERROR AL ELIMINAR AUTORIDAD", e);
            JsfUtil.addErrorMessageFromBundle("autoridades.mensaje.error.eliminar");
            PrimeFaces.current().ajax().update(GROWL_GLOBAL);
        }
    }

    /** Prepara una copia de la fila para asignar o reasignar sin alterar la tabla. */
    public void prepararAsignacionAutoridad(TribunalDTO autoridad) {
        if (autoridad == null || autoridad.getCargoId() == null || autoridad.getProcesoId() == null) {
            JsfUtil.addWarningMessageFromBundle("autoridades.mensaje.cargo.no.determinado");
            return;
        }
        TribunalDTO seleccion = new TribunalDTO();
        seleccion.setId(autoridad.getId());
        seleccion.setProcesoId(autoridad.getProcesoId());
        seleccion.setProcesoNombre(autoridad.getProcesoNombre());
        seleccion.setPeriodoId(autoridad.getPeriodoId());
        seleccion.setPeriodoNombre(autoridad.getPeriodoNombre());
        seleccion.setCargoId(autoridad.getCargoId());
        seleccion.setCargoNombre(autoridad.getCargoNombre());
        seleccion.setIglesiaPersona(autoridad.getIglesiaPersona());
        seleccion.setCorreoAutoridad(autoridad.getIglesiaPersona() != null
                && autoridad.getIglesiaPersona().getPersona() != null
                ? tribunalService.obtenerCorreoUsuarioPorPersonaId(
                        autoridad.getIglesiaPersona().getPersona().getId()) : null);
        tribunalSeleccionado = seleccion;
        cedulaBuscar = null;
    }

    public void guardarAutoridad() {
        try {
            if (tribunalSeleccionado == null) {
                JsfUtil.addWarningMessageFromBundle("autoridades.mensaje.no.seleccionada.guardar");
                FacesContext.getCurrentInstance().validationFailed();
                return;
            }
            boolean esEdicion = tribunalSeleccionado.getId() != null;
            TribunalDTO persistido = tribunalService.guardarDesdeDTO(tribunalSeleccionado);
            if (persistido != null) {
                tribunalSeleccionado = persistido;
                cargarAutoridadesTribunal();
                if (Boolean.TRUE.equals(persistido.getUsuarioReutilizado())) {
                    JsfUtil.addSuccessMessageFromBundle("autoridades.mensaje.usuario.reutilizado");
                } else {
                    JsfUtil.addSuccessMessageFromBundle(esEdicion
                            ? "autoridades.mensaje.actualizada"
                            : "autoridades.mensaje.asignada.usuario");
                }
                PrimeFaces.current().ajax().update(TABLA_AUTORIDADES, GROWL_GLOBAL);
            }
        } catch (ec.com.antenasur.exception.NegocioException e) {
            JsfUtil.addErrorMessageFromBundle(e.getMessage());
            FacesContext.getCurrentInstance().validationFailed();
            PrimeFaces.current().ajax().update(GROWL_GLOBAL);
        } catch (Exception e) {
            log.error("ERROR AL GUARDAR AUTORIDADES", e);
            JsfUtil.addErrorMessageFromBundle("autoridades.mensaje.error.guardar");
            FacesContext.getCurrentInstance().validationFailed();
            PrimeFaces.current().ajax().update(GROWL_GLOBAL);
        }
    }

    public void buscaPersona() {
        try {
            TribunalDTO actualizado = tribunalService.asignarPersonaPorCedula(tribunalSeleccionado, cedulaBuscar);
            if (actualizado != null) {
                tribunalSeleccionado = actualizado;
                JsfUtil.addInfoMessageFromBundle("autoridades.mensaje.persona.seleccionada");
            } else {
                JsfUtil.addWarningMessageFromBundle("autoridades.mensaje.persona.no.encontrada");
            }
            PrimeFaces.current().ajax().update(PANEL_BUSQUEDA, PANEL_AUTORIDAD, PANEL_FOOTER, GROWL_GLOBAL);
        } catch (ec.com.antenasur.exception.NegocioException e) {
            JsfUtil.addErrorMessageFromBundle(e.getMessage());
            FacesContext.getCurrentInstance().validationFailed();
            PrimeFaces.current().ajax().update(PANEL_BUSQUEDA, PANEL_AUTORIDAD, PANEL_FOOTER, GROWL_GLOBAL);
        } catch (Exception e) {
            log.error("ERROR AL BUSCAR PERSONA PARA AUTORIDAD", e);
            JsfUtil.addErrorMessageFromBundle("autoridades.mensaje.error.buscar");
            FacesContext.getCurrentInstance().validationFailed();
        }
    }
}
