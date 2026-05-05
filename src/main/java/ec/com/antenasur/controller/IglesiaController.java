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
import ec.com.antenasur.dto.IglesiaDTO;
import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.tec.Documentos;
import ec.com.antenasur.service.GeograpService;
import ec.com.antenasur.service.IglesiaService;
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

    @Setter
    @Getter
    private String prefijoRoles;

    @Setter
    @Getter
    private IglesiaDTO iglesiaSeleccionado;

    @Setter
    @Getter
    private List<Geograp> cantones, parroquias;

    @Setter
    @Getter
    private Geograp parroquiaSeleccionado, cantonSeleccionado;

    @Setter
    @Getter
    private List<IglesiaDTO> listaIglesias, listaIglesiasSeleccionadas, listaIglesiasFiltrada;

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
            cantones = geograpService.findByFatherId(7);
            listaIglesias = iglesiaService.listarDTOsConFlagDocumentos(Constantes.LISTA_MIEMBROS);
            esNuevoRegistro = false;
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    public void obtieneParroquias() {
        if (cantonSeleccionado != null && cantonSeleccionado.getId() != null) {
            cantonSeleccionado = geograpService.find(cantonSeleccionado.getId());
            parroquias = geograpService.findByFatherId(cantonSeleccionado.getId());
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

    public void inicializaIglesiaSeleccionado() {
        esNuevoRegistro = true;
        if (listaIglesias != null) {
            listaIglesias.clear();
        }
        iglesiaSeleccionado = new IglesiaDTO();
    }

    public void nuevaIglesia() {
        inicializaIglesiaSeleccionado();
        PrimeFaces.current().ajax().update("frmIglesias", "msgs", "frmNuevaIglesia", "dv1");
    }

    public void editarIglesia() {
        if (!existeIglesiasSeleccionadas()) {
            return;
        }
        iglesiaSeleccionado = listaIglesiasSeleccionadas.get(0);
        if (iglesiaSeleccionado.getUbicacionId() != null) {
            Geograp parroquia = geograpService.find(iglesiaSeleccionado.getUbicacionId());
            if (parroquia != null && parroquia.getGeograp() != null) {
                cantonSeleccionado = parroquia.getGeograp();
                obtieneParroquias();
            }
        }
        listaIglesiasSeleccionadas.clear();
        listaIglesias.clear();
        PrimeFaces.current().ajax().update("frmIglesias", "msgs", "frmNuevaIglesia");
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
            int eliminadas = 0;
            for (IglesiaDTO item : listaIglesiasSeleccionadas) {
                if (iglesiaService.eliminarPorId(item.getId()) != null) {
                    eliminadas++;
                }
            }
            JsfUtil.addInfoMessage(eliminadas + " Iglesias eliminadas");
        }
        listaIglesias = iglesiaService.listarDTOs();
        PrimeFaces.current().ajax().update("frmIglesias:tblIglesia", "msgs");
        this.listaIglesiasSeleccionadas = null;
    }

    public void buscaIglesiaPorDocumento() {
        if (iglesiaSeleccionado == null) {
            return;
        }
        IglesiaDTO encontrada = iglesiaService.buscarDTOPorDocumento(iglesiaSeleccionado.getDocumento());
        if (encontrada != null) {
            iglesiaSeleccionado = encontrada;
            if (encontrada.getUbicacionId() != null) {
                Geograp parroquia = geograpService.find(encontrada.getUbicacionId());
                if (parroquia != null && parroquia.getGeograp() != null) {
                    cantonSeleccionado = parroquia.getGeograp();
                    obtieneParroquias();
                }
            }
            esNuevoRegistro = false;
            JsfUtil.addInfoMessage("Iglesia con CI: " + encontrada.getDocumento() + " ya se encuentra registrado ");
        }
    }

    public void actualizarIglesia() {
        try {
            if (iglesiaSeleccionado == null || iglesiaSeleccionado.getUbicacionId() == null) {
                JsfUtil.addErrorMessage("Complete datos requeridos");
                return;
            }
            boolean esEdicion = iglesiaSeleccionado.getId() != null;
            IglesiaDTO persistida = iglesiaService.guardarDesdeDTO(iglesiaSeleccionado);
            if (persistida != null) {
                JsfUtil.addSuccessMessage(esEdicion ? "Iglesia actualido" : "Iglesia registraga");
                iglesiaSeleccionado = null;
                listaIglesias = iglesiaService.listarDTOs();
                PrimeFaces.current().ajax().update("frmNuevaIglesia", "msgs", "frmIglesias");
            }
        } catch (Exception e) {
            log.error("ERROR AL ACTUALIZAR IGLESIA", e);
        }
    }

    public void cancelarIglesia() {
        try {
            iglesiaSeleccionado = null;
            listaIglesias = iglesiaService.listarDTOs();
            JsfUtil.addWarningMessage("Registro Cancelado");
            PrimeFaces.current().ajax().update("frmNuevaIglesia", "msgs", "frmIglesias");
        } catch (Exception e) {
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
            log.error("ERROR AL OBTENER DOCUMENTOS", e);
        }
    }
}
