package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;

import ec.com.antenasur.bean.GeograpBean;
import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.domain.Geograp;
import ec.com.antenasur.domain.tec.Candidato;
import ec.com.antenasur.domain.tec.CatalogoGeneral;
import ec.com.antenasur.domain.tec.CategoriaVoto;
import ec.com.antenasur.domain.tec.Escrutinio;
import ec.com.antenasur.domain.tec.Lista;
import ec.com.antenasur.domain.tec.Mesa;
import ec.com.antenasur.domain.tec.Periodo;
import ec.com.antenasur.domain.tec.Recinto;
import ec.com.antenasur.enums.EstadoTarea;
import ec.com.antenasur.service.tec.CatalogoGeneralFacade;
import ec.com.antenasur.service.IglesiaPersonaFacade;
import ec.com.antenasur.service.tec.ListaFacade;
import ec.com.antenasur.service.tec.MesaFacade;
import ec.com.antenasur.service.tec.PadronFacade;
import ec.com.antenasur.service.tec.PeriodoFacade;
import ec.com.antenasur.service.tec.RecintoFacade;
import ec.com.antenasur.service.tec.CategoriaVotoFacade;
import ec.com.antenasur.service.tec.EscrutinioFacade;
import ec.com.antenasur.util.Constantes;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 * @fecha 2022-09-06 14:30
 * @version 1.0.0 Maneja acta de escritinios
 */
@Named
@ViewScoped
@Slf4j
public class ActaEController implements Serializable {

    private static final String DESTINATION = System.getProperty("java.io.tmpdir");

    private static final String FORMULARIO = "frmActaE";
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
    private CatalogoGeneralFacade catalogoFacade;

    @Inject
    private PeriodoFacade periodoFacade;

    @Inject
    private IglesiaPersonaFacade iglesiaPersonaFacade;

    @Inject
    private GeograpBean geograpBean;

    @Inject
    private PadronFacade padronFacade;

    @Inject
    private RecintoFacade recintoFacade;

    @Inject
    private MesaFacade mesaFacade;

    @Inject
    private CategoriaVotoFacade categoriaVotoFacade;

    @Inject
    private EscrutinioFacade escrutinioFacade;

    @Setter
    @Getter
    private List<Geograp> cantones, parroquias;

    @Setter
    @Getter
    private Geograp cantonSeleccionado, parroquiaSeleccionado;

    @Setter
    @Getter
    private Recinto recintoSeleccionado;

    @Setter
    @Getter
    private List<Recinto> listaRecintos, listaRecintosSeleccionados;

    @Setter
    @Getter
    private List<Mesa> listaMesas, listaMesasCerradas;

    @Setter
    @Getter
    private Mesa mesaSeleccionado;

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
    private List<Escrutinio> listaCamposActaE;

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
        inicializaVariables();
        cargaDatosIniciales();

    }

    private void inicializaVariables() {
        this.listaCamposActaE = new ArrayList<>();
        this.cantonSeleccionado = new Geograp();
        this.parroquiaSeleccionado = new Geograp();
        this.recintoSeleccionado = new Recinto();
        this.mesaSeleccionado = new Mesa();
    }

    private void cargaDatosIniciales() {
        //PERIODO ACTIVO
        this.periodoVigente = periodoFacade.getPeridoActivo();
        //CANTONES DE CHIMBORAZO
        this.cantones = geograpBean.getByFatherId(7);
        //RECINTOS POR CERRAR
        this.listaRecintos = recintoFacade.findAll();
        //MESAS POR CERRAR
        this.listaMesas = mesaFacade.getMesasPorRecintos(listaRecintos);
        //LISTAS CALIFICADAS
        this.listas = listaFacade.findAll();

        //CATEGORIA VOTOS, para registrar votos por mesas
        this.categoriasVotos = categoriaVotoFacade.findAll();

        //IGLESIAS ASIGNADAS
        PrimeFaces.current().ajax().update("frmIglesias", "msgs");

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
            List<Geograp> litaParroquiasTmp = new ArrayList();
            if (this.parroquiaSeleccionado != null && this.parroquiaSeleccionado.getId() != null) {
                this.parroquiaSeleccionado = geograpBean.getById(this.parroquiaSeleccionado.getId());

                litaParroquiasTmp.add(this.parroquiaSeleccionado);
                this.listaRecintos = recintoFacade.getRecintosPorParroquias(litaParroquiasTmp);
            } else {
                //CARGA RECINTOS
                if (this.parroquias != null && !this.parroquias.isEmpty()) {
                    this.listaRecintos = recintoFacade.getRecintosPorParroquias(this.parroquias);
                }
            }

            //CARGA MESAS
            if (listaRecintos != null && !listaRecintos.isEmpty()) {
                this.cargaMesasPorRecintos();
            }
        } catch (Exception e) {
        }
    }

    public void cargaMesasPorRecintos() {
        if (recintoSeleccionado != null && recintoSeleccionado.getId() != null) {
            recintoSeleccionado = recintoFacade.find(recintoSeleccionado.getId());
            List<Recinto> listaRecintosTmp = new ArrayList<>();
            listaRecintosTmp.add(recintoSeleccionado);
            listaMesas.clear();
            this.listaMesas = mesaFacade.getMesasPorRecintos(listaRecintosTmp);
        } else {
            if (listaRecintos != null && !listaRecintos.isEmpty()) {
                this.listaMesas = mesaFacade.getMesasPorRecintos(listaRecintos);
            }
        }
    }

    public void cargaDatosMesaSeleccionada() {
        if (mesaSeleccionado != null && mesaSeleccionado.getId() != null) {
            mesaSeleccionado = mesaFacade.find(mesaSeleccionado.getId());
            this.listaCamposActaE = new ArrayList<>();
            for (CategoriaVoto categoria : categoriasVotos) {
                Escrutinio escruitinio = new Escrutinio();
                escruitinio.setMesa(mesaSeleccionado);
                escruitinio.setPeriodo(periodoVigente);
                escruitinio.setCategoria(categoria);
                listaCamposActaE.add(escruitinio);
            }
        }
    }

    public void guardaDatosMesaSeleccionada() {
        if (this.listaCamposActaE != null && !this.listaCamposActaE.isEmpty()) {
            try {
                for (Escrutinio itemActa : listaCamposActaE) {
                    if (itemActa.getId() != null) {
                        escrutinioFacade.edit(itemActa);
                    } else {
                        escrutinioFacade.create(itemActa);
                    }
                }
                mesaSeleccionado.setEstadoTarea(EstadoTarea.COMPLETADO);
                mesaFacade.edit(mesaSeleccionado);
            } catch (Exception e) {
                mesaSeleccionado.setEstadoTarea(EstadoTarea.ABORTADO);
                mesaFacade.edit(mesaSeleccionado);
            }
        }
    }
}
