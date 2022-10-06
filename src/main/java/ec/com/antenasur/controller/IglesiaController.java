package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import ec.com.antenasur.bean.DocumentoBean;
import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.domain.Geograp;
import ec.com.antenasur.domain.Iglesia;
import ec.com.antenasur.domain.IglesiaPersona;
import ec.com.antenasur.domain.tec.Documentos;
import ec.com.antenasur.service.GeograpFacade;
import ec.com.antenasur.service.IglesiaFacade;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Named
@ViewScoped
@Slf4j
public class IglesiaController implements Serializable {

    private static final String DESTINATION = System.getProperty("java.io.tmpdir");

    private static final long serialVersionUID = 1L;

    //private static final Logger LOG = Logger.getLogger(cargarControl.class);
    @Inject
    private LoginBean loginBean;

    @Inject
    private DocumentoBean documentoBean;

    @Inject
    private IglesiaFacade iglesiaFacade;

    @Inject
    private GeograpFacade geograpFacade;

    @Setter
    @Getter
    private String prefijoRoles;

    @Setter
    @Getter
    private Iglesia iglesia, iglesiaSeleccionado;

    @Setter
    @Getter
    private IglesiaPersona iglesiaPersona;

    @Setter
    @Getter
    private List<Geograp> cantones, parroquias;

    @Setter
    @Getter
    private Geograp parroquiaSeleccionado, cantonSeleccionado;

    @Setter
    @Getter
    private List<Iglesia> listaIglesias, listaIglesiasSeleccionadas;

    @Setter
    @Getter
    private Boolean esNuevoRegistro;

    @Setter
    @Getter
    private List<Documentos> documentos;

    @PostConstruct
    private void init() {
        try {
            parroquiaSeleccionado = cantonSeleccionado = new Geograp();
            cantones = geograpFacade.findByFatherId(7);
            listaIglesias = iglesiaFacade.findAll();
            esNuevoRegistro = false;
            cargaArchivoExcelListaMiembros();
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    private void cargaArchivoExcelListaMiembros() {
        if (listaIglesias != null && !listaIglesias.isEmpty()) {
            for (Iglesia iglesiaTmp : listaIglesias) {
                iglesiaTmp.setTieneDocumentos(documentoBean.getTieneDocumentosPorEntidadYTipoDoc(iglesiaTmp.getId(), Constantes.LISTA_MIEMBROS));
            }
        }
    }

    public void obtieneParroquias() {
        if (cantonSeleccionado != null) {
            cantonSeleccionado = geograpFacade.find(cantonSeleccionado.getId());
            parroquias = geograpFacade.findByFatherId(cantonSeleccionado.getId());

            obtieneIglesiasPorCanton();
        }
    }

    private void obtieneIglesiasPorCanton() {
        if (parroquias != null && !parroquias.isEmpty()) {
            listaIglesias = iglesiaFacade.getIglesiasPorParroquias(parroquias);
        }
    }

    public void obtieneIglesiasPorParroquia() {
        if (parroquiaSeleccionado != null) {
            parroquiaSeleccionado = geograpFacade.find(parroquiaSeleccionado.getId());
            listaIglesias = iglesiaFacade.getIglesiasPorParroquia(parroquiaSeleccionado);
            PrimeFaces.current().ajax().update("frmIglesias", "msgs");
        }
    }

    public void inicializaIglesiaSeleccionado() {
        esNuevoRegistro = true;
        listaIglesias.clear();
        iglesiaSeleccionado = new Iglesia();
        iglesiaSeleccionado.setUbicacion(new Geograp());
    }

    public void nuevaIglesia() {
        inicializaIglesiaSeleccionado();
        PrimeFaces.current().ajax().update("frmIglesias", "msgs", "frmNuevaIglesia", "dv1");
        //this.iglesiaSeleccionado = new Iglesia();
    }

    public void editarIglesia() {
        if (existeIglesiasSeleccionadas()) {
            iglesiaSeleccionado = new Iglesia();
            iglesiaSeleccionado = listaIglesiasSeleccionadas.get(0);
            cantonSeleccionado = iglesiaSeleccionado.getUbicacion().getGeograp();
            obtieneParroquias();
            listaIglesiasSeleccionadas.clear();
            listaIglesias.clear();           
            PrimeFaces.current().ajax().update("frmIglesias", "msgs", "frmNuevaIglesia");            
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
        if (listaIglesiasSeleccionadas != null) {
            for (Iglesia item : listaIglesiasSeleccionadas) {
                iglesiaFacade.delete(item);
            }
        }
        listaIglesias = iglesiaFacade.findAll();
        JsfUtil.addInfoMessage(+listaIglesiasSeleccionadas.size() + " Iglesias eliminadas");
        PrimeFaces.current().ajax().update("frmIglesias:tblIglesia", "msgs");
        this.listaIglesiasSeleccionadas = null;
    }

    public void buscaIglesiaPorDocumento() {
        if (iglesiaSeleccionado != null) {
            Iglesia iglesiaBuscado = iglesiaFacade.getIglesiaPorDocumento(iglesiaSeleccionado.getDocumento());
            if (iglesiaBuscado != null) {
                iglesiaSeleccionado = iglesiaBuscado;
                cantonSeleccionado = iglesiaSeleccionado.getUbicacion().getGeograp();
                obtieneParroquias();
                esNuevoRegistro = false;
                JsfUtil.addInfoMessage("Iglesia con CI: " + iglesiaBuscado.getDocumento() + " ya se encuentra registrado ");
            }
        }
    }

    public void actualizarIglesia() {
        try {
            if (iglesiaSeleccionado != null && iglesiaSeleccionado.getUbicacion() != null) {
                if (iglesiaSeleccionado.getId() != null) {
                    Iglesia iglesiaActualiza = iglesiaFacade.edit(iglesiaSeleccionado);
                    if (iglesiaActualiza != null) {
                        JsfUtil.addSuccessMessage("Iglesia actualido");
                        iglesiaSeleccionado = null;
                        listaIglesias = iglesiaFacade.findAll();
                        PrimeFaces.current().ajax().update("frmNuevaIglesia", "msgs", "frmIglesias");
                    }
                } else {
                    Iglesia iglesiaNueva = iglesiaFacade.create(iglesiaSeleccionado);
                    if (iglesiaNueva != null) {
                        JsfUtil.addSuccessMessage("Iglesia registraga");
                        iglesiaSeleccionado = null;
                        listaIglesias = iglesiaFacade.findAll();
                        PrimeFaces.current().ajax().update("frmNuevaIglesia", "msgs", "frmIglesias");
                    }
                }
            } else {
                JsfUtil.addErrorMessage("Complete datos requeridos");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cancelarIglesia() {
        try {
            iglesiaSeleccionado = null;
            listaIglesias = iglesiaFacade.findAll();
            JsfUtil.addWarningMessage("Registro Cancelado");
            PrimeFaces.current().ajax().update("frmNuevaIglesia", "msgs", "frmIglesias");
        } catch (Exception e) {
        }
    }

    public void cargaArchivosListaMiembros() {
        try {
            documentos = documentoBean.getDocumentosPorEntidadYTipoDoc(iglesiaSeleccionado.getId(), Constantes.LISTA_MIEMBROS);
        } catch (Exception e) {
            log.error("ERROR AL OBTENER DOCUMENTOS", e);
        }
    }
}
