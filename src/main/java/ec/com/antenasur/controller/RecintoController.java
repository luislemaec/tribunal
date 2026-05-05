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
import ec.com.antenasur.dto.RecintoDTO;
import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.service.tec.RecintoService;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Named
@ViewScoped
@Slf4j
public class RecintoController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private LoginBean loginBean;

    @Inject
    private RecintoService recintoService;

    @Inject
    private GeograpBean geograpBean;

    @Setter
    @Getter
    private RecintoDTO recintoSeleccionado;

    @Setter
    @Getter
    private List<Geograp> cantones, parroquias;

    @Setter
    @Getter
    private Geograp cantonSeleccionado, parroquiaSeleccionado;

    @Setter
    @Getter
    private List<RecintoDTO> listaRecintos, listaRecintosSeleccionados;

    @PostConstruct
    private void init() {
        try {
            cantonSeleccionado = parroquiaSeleccionado = new Geograp();
            this.listaRecintosSeleccionados = new ArrayList<>();
            this.cantones = geograpBean.getByFatherId(7);
            this.listaRecintos = recintoService.listarDTOs();
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    public void inicializaRecintoSeleccionado() {
        if (listaRecintos != null) {
            listaRecintos.clear();
        }
        this.recintoSeleccionado = new RecintoDTO();
    }

    public void nuevaRecinto() {
        inicializaRecintoSeleccionado();
    }

    public boolean existeRecintosSeleccionados() {
        return this.listaRecintosSeleccionados != null && !this.listaRecintosSeleccionados.isEmpty();
    }

    public String getMensajeBotonEliminar() {
        if (existeRecintosSeleccionados()) {
            int size = this.listaRecintosSeleccionados.size();
            return size > 1 ? size + " Recintos seleccionadas" : "1 recinto seleccionada";
        }
        return "Eliminar";
    }

    public void eliminarRecintoSeleccionado() {
        if (recintoSeleccionado != null && recintoSeleccionado.getId() != null) {
            recintoService.eliminarPorId(recintoSeleccionado.getId());
        }
        JsfUtil.addInfoMessage(" Registro eliminado");
        PrimeFaces.current().ajax().update("frmPersonas:tblRecintos", "msgs");
    }

    public void obtieneParroquias() {
        if (cantonSeleccionado.getId() != null) {
            cantonSeleccionado = geograpBean.getById(cantonSeleccionado.getId());
            parroquias = geograpBean.getByFatherId(cantonSeleccionado.getId());
            listaRecintos = recintoService.listarDTOsPorParroquias(parroquias);
        } else {
            if (parroquias != null) {
                parroquias.clear();
            }
            recintoSeleccionado = null;
            listaRecintos.clear();
        }
    }

    public void obtieneRecintosPorParroquia() {
        if (parroquiaSeleccionado.getId() != null) {
            parroquiaSeleccionado = geograpBean.getById(parroquiaSeleccionado.getId());
            List<Geograp> parroquiasTmp = new ArrayList<>();
            parroquiasTmp.add(parroquiaSeleccionado);
            listaRecintos = recintoService.listarDTOsPorParroquias(parroquiasTmp);
            if (listaRecintos == null || listaRecintos.isEmpty()) {
                JsfUtil.addWarningMessage("No existe registro de Recintos en " + parroquiaSeleccionado.getName());
            } else {
                JsfUtil.addInfoMessage(listaRecintos.size() + " Recintos registrados");
            }
        } else {
            recintoSeleccionado = new RecintoDTO();
            listaRecintos.clear();
        }
    }

    public void guardarRecintoSeleccionado() {
        try {
            if (recintoSeleccionado == null) {
                return;
            }
            boolean esEdicion = recintoSeleccionado.getId() != null;
            RecintoDTO persistido = recintoService.guardarDesdeDTO(recintoSeleccionado);
            if (persistido != null) {
                JsfUtil.addSuccessMessage(esEdicion ? "Recinto actualizado" : "Recinto agregado");
                recintoSeleccionado = null;
                listaRecintos = recintoService.listarDTOs();
                PrimeFaces.current().ajax().update("msgs", "frmRecintos");
            }
        } catch (Exception e) {
            log.error("ERROR AL GUARDAR RECINTO", e);
        }
        PrimeFaces.current().executeScript("PF('dlgRecinto').hide()");
        PrimeFaces.current().ajax().update("frmRecintos:messages", "frmRecintos:tblRecintos");
    }

    public void eliminarRecintosSeleccionados() {
        int eliminados = 0;
        if (listaRecintosSeleccionados != null) {
            for (RecintoDTO item : listaRecintosSeleccionados) {
                if (item.getId() != null && recintoService.eliminarPorId(item.getId()) != null) {
                    eliminados++;
                }
            }
        }
        JsfUtil.addInfoMessage(eliminados + " Registros eliminados");
        PrimeFaces.current().ajax().update("frmRecintos:tblRecintos", "msgs");
    }

    public void cagraDatosRecintoSeleccionado() {
        try {
            if (recintoSeleccionado != null && recintoSeleccionado.getId() != null
                    && recintoSeleccionado.getUbicacionId() != null) {
                Geograp parroquia = geograpBean.getById(recintoSeleccionado.getUbicacionId());
                if (parroquia != null && parroquia.getGeograp() != null) {
                    this.cantonSeleccionado = parroquia.getGeograp();
                    this.parroquias = geograpBean.getByFatherId(cantonSeleccionado.getId());
                }
            }
        } catch (Exception e) {
        }
    }
}
