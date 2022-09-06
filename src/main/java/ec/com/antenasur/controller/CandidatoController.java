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
import ec.com.antenasur.domain.Candidato;
import ec.com.antenasur.domain.CatalogoGeneral;
import ec.com.antenasur.domain.IglesiaPersona;
import ec.com.antenasur.domain.Lista;
import ec.com.antenasur.domain.Periodo;
import ec.com.antenasur.service.CandidatoFacade;
import ec.com.antenasur.service.CatalogoGeneralFacade;
import ec.com.antenasur.service.IglesiaPersonaFacade;
import ec.com.antenasur.service.ListaFacade;
import ec.com.antenasur.service.PeriodoFacade;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Named
@ViewScoped
@Slf4j
public class CandidatoController implements Serializable {

    private static final String DESTINATION = System.getProperty("java.io.tmpdir");

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
    private ListaFacade listaFacade;

    @Inject
    private CandidatoFacade candidatoFacade;

    @Inject
    private CatalogoGeneralFacade catalogoFacade;

    @Inject
    private PeriodoFacade periodoFacade;

    @Inject
    private IglesiaPersonaFacade iglesiaPersonaFacade;

    @Setter
    @Getter
    private Lista listaSeleccionado;

    @Setter
    @Getter
    private List<Lista> listas;

    @Setter
    @Getter
    private List<Candidato> candidatos;

    @Setter
    @Getter
    private List<CatalogoGeneral> cargosCandidatos;

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
        try {
            this.listaSeleccionado = new Lista();
            this.listas = new ArrayList<>();

            this.periodoVigente = periodoFacade.getPeriodoVigente();

            this.listas = listaFacade.findAll();
            this.cargosCandidatos = catalogoFacade.listaCatalogoHijo(8);

        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    public void obtieneCandidatosPorListaSeleccionada() {
        if (listaSeleccionado != null && listaSeleccionado.getId() != null) {
            this.candidatos = new ArrayList<>();
            for (CatalogoGeneral cargo : cargosCandidatos) {
                Candidato candidatoBuscado = candidatoFacade.getPorCargoYLista(cargo, listaSeleccionado);
                if (candidatoBuscado != null) {
                    candidatos.add(candidatoBuscado);
                } else {
                    Candidato nuevoCandidato = new Candidato(null, null, listaSeleccionado, periodoVigente, cargo);
                    candidatos.add(nuevoCandidato);
                }
            }
        }
    }

    public void buscaPersona() {
        try {
            IglesiaPersona iglesiaPersonaBuscado = iglesiaPersonaFacade.buscarPorCedulaPersona(cedulaBuscar);
            if (iglesiaPersonaBuscado != null) {
                candidatoSeleccionado.setIglesiaPersona(iglesiaPersonaBuscado);
                JsfUtil.addInfoMessage("PERSONA SELECCIONADA");
            } else {
                JsfUtil.addWarningMessage("PERSONA NO ENCONTRADA");
            }
            PrimeFaces.current().ajax().update(FORMULARIO + ":outPnlAsignaCandidatoBusca", FORMULARIO + ":outPnlAsignaCandidato", "msgs");
        } catch (Exception e) {
        }
    }

    public void guardarCandidato() {
        try {
            if (candidatoSeleccionado != null) {
                if (this.candidatoSeleccionado.getId() != null) {
                    Candidato candidatoActualiza = candidatoFacade.edit(candidatoSeleccionado);
                    if (candidatoActualiza != null) {
                        JsfUtil.addSuccessMessage(MENSAJE_ACTUALIZA_OK);

                        PrimeFaces.current().ajax().update("msgs", FORMULARIO);
                    }
                } else {
                    candidatoSeleccionado = candidatoFacade.create(candidatoSeleccionado);
                    if (candidatoSeleccionado != null) {
                        JsfUtil.addSuccessMessage(MENSAJE_REGISTRA_OK);
                        PrimeFaces.current().ajax().update("msgs", FORMULARIO);
                    }
                }
            }
        } catch (Exception e) {
            log.error("ERROR AL GUARDAR AUTORIDADES", e);
        }
        PrimeFaces.current().executeScript("PF('dlgPeriodo').hide()");
        PrimeFaces.current().ajax().update(FORMULARIO, FORMULARIO + ":" + TABLA);
    }

    public void eliminarSeleccionado() {
        try {
            if (candidatoSeleccionado != null) {
                candidatoFacade.delete(candidatoSeleccionado);
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
