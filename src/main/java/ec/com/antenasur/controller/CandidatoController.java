package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.dto.CandidatoDTO;
import ec.com.antenasur.model.tec.CatalogoGeneral;
import ec.com.antenasur.model.tec.Lista;
import ec.com.antenasur.model.tec.Periodo;
import ec.com.antenasur.service.tec.CandidatoService;
import ec.com.antenasur.service.tec.CatalogoGeneralService;
import ec.com.antenasur.service.tec.ListaService;
import ec.com.antenasur.service.tec.PeriodoService;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Named
@ViewScoped
@Slf4j
public class CandidatoController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String FORMULARIO = "frmCandidatos";
    private static final String TABLA = "wdTblCandidatos";
    private static final String MENSAJE_REGISTRA_OK = "Candidato registrado";
    private static final String MENSAJE_ACTUALIZA_OK = "Candidato actualizado";
    private static final String MENSAJE_ELIMINA_OK = "Candidato eliminado";
    public static final String MENSAJE_CONFORMACION_ELIMINAR = "¿Esta seguro de eliminar?";

    @Inject
    private LoginBean loginBean;

    @Inject
    private ListaService listaService;

    @Inject
    private CandidatoService candidatoService;

    @Inject
    private CatalogoGeneralService catalogoService;

    @Inject
    private PeriodoService periodoService;

    // NOTA: Lista, Periodo, CatalogoGeneral siguen como entidades porque son
    // catálogos cuyo dominio aún no se ha migrado a DTO. Cuando se migren
    // (iteraciones futuras), estos campos pasarán a sus respectivos DTOs.

    @Setter
    @Getter
    private Lista listaSeleccionado;

    @Setter
    @Getter
    private List<Lista> listas;

    @Setter
    @Getter
    private List<CandidatoDTO> candidatos;

    @Setter
    @Getter
    private List<CatalogoGeneral> cargosCandidatos;

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
        try {
            this.listaSeleccionado = new Lista();
            this.listas = new ArrayList<>();
            this.periodoVigente = periodoService.getPeriodoVigente();
            this.listas = listaService.findAll();
            this.cargosCandidatos = catalogoService.listaCatalogoHijo(8);
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    public void obtieneCandidatosPorListaSeleccionada() {
        if (listaSeleccionado == null || listaSeleccionado.getId() == null || cargosCandidatos == null) {
            this.candidatos = new ArrayList<>();
            return;
        }
        List<Integer> cargoIds = new ArrayList<>();
        for (CatalogoGeneral cargo : cargosCandidatos) {
            cargoIds.add(cargo.getId());
        }
        Integer periodoId = (periodoVigente != null) ? periodoVigente.getId() : null;
        this.candidatos = candidatoService.listarDTOsPorLista(listaSeleccionado.getId(), periodoId, cargoIds);
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
            PrimeFaces.current().ajax().update(FORMULARIO + ":outPnlAsignaCandidatoBusca",
                    FORMULARIO + ":outPnlAsignaCandidato", "msgs");
        } catch (Exception e) {
        }
    }

    public void guardarCandidato() {
        try {
            if (candidatoSeleccionado == null) {
                return;
            }
            boolean esEdicion = candidatoSeleccionado.getId() != null;
            CandidatoDTO persistido = candidatoService.guardarDesdeDTO(candidatoSeleccionado);
            if (persistido != null) {
                candidatoSeleccionado = persistido;
                JsfUtil.addSuccessMessage(esEdicion ? MENSAJE_ACTUALIZA_OK : MENSAJE_REGISTRA_OK);
                PrimeFaces.current().ajax().update("msgs", FORMULARIO);
            }
        } catch (Exception e) {
            log.error("ERROR AL GUARDAR CANDIDATO", e);
        }
        PrimeFaces.current().executeScript("PF('dlgPeriodo').hide()");
        PrimeFaces.current().ajax().update(FORMULARIO, FORMULARIO + ":" + TABLA);
    }

    public void eliminarSeleccionado() {
        try {
            if (candidatoSeleccionado != null && candidatoSeleccionado.getId() != null) {
                candidatoService.eliminarPorId(candidatoSeleccionado.getId());
            }
            this.obtieneCandidatosPorListaSeleccionada();
            candidatoSeleccionado = null;
            JsfUtil.addInfoMessage(MENSAJE_ELIMINA_OK);
            PrimeFaces.current().ajax().update(FORMULARIO, "msgs");
        } catch (Exception e) {
            log.error("ERROR EN ELIMINAR AL CANDIDATO", e);
        }
    }
}
