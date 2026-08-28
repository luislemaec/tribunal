package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.primefaces.PrimeFaces;

import ec.com.antenasur.bean.GeograpBean;
import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.dto.MesaDTO;
import ec.com.antenasur.dto.RecintoDTO;
import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.service.tec.MesaService;
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
    private static final Integer OPCION_TODOS = 0;

    @Inject
    private LoginBean loginBean;

    @Inject
    private RecintoService recintoService;

    @Inject
    private MesaService mesaService;

    @Inject
    private MesaController mesaController;

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

    private Integer indiceFilaEdicion;

    private RecintoDTO recintoFilaEdicion;

    @PostConstruct
    private void init() {
        try {
            cantonSeleccionado = parroquiaSeleccionado = new Geograp();
            this.listaRecintosSeleccionados = new ArrayList<>();
            this.cantones = geograpBean.getByFatherId(7);
            this.parroquias = new ArrayList<>();
            this.listaRecintos = new ArrayList<>();
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    public void inicializaRecintoSeleccionado() {
        this.recintoSeleccionado = new RecintoDTO();
        if (parroquiaSeleccionado != null && parroquiaSeleccionado.getId() != null) {
            this.recintoSeleccionado.setUbicacionId(parroquiaSeleccionado.getId());
        }
    }

    public void nuevaRecinto() {
        indiceFilaEdicion = null;
        recintoFilaEdicion = null;
        inicializaRecintoSeleccionado();
    }

    public void cambiarCanton() {
        liberarContextoRecintoMesa();
        parroquiaSeleccionado = new Geograp();
        obtieneParroquias();
    }

    public void cambiarParroquia() {
        liberarContextoRecintoMesa();
        obtieneRecintosPorParroquia();
    }

    public boolean existeRecintosSeleccionados() {
        return this.listaRecintosSeleccionados != null && !this.listaRecintosSeleccionados.isEmpty();
    }

    public String getMensajeBotonEliminar() {
        if (existeRecintosSeleccionados()) {
            int size = this.listaRecintosSeleccionados.size();
            return size > 1
                    ? JsfUtil.getMessage("recintos.boton.eliminar.seleccionados", size)
                    : JsfUtil.getMessage("recintos.boton.eliminar.uno");
        }
        return JsfUtil.getMessage("recintos.boton.eliminar");
    }

    public void eliminarRecintoSeleccionado() {
        if (recintoSeleccionado != null && recintoSeleccionado.getId() != null) {
            if (tieneMesasAsociadas(recintoSeleccionado)) {
                JsfUtil.addWarningMessageFromBundle("recintos.mensaje.eliminar.con.mesas");
                PrimeFaces.current().ajax().update(JsfUtil.GROWL_MESSAGES);
                return;
            }
            Integer recintoEliminadoId = recintoSeleccionado.getId();
            recintoService.eliminarPorId(recintoEliminadoId);
            if (mesaController.getRecintoSeleccionado() != null
                    && recintoEliminadoId.equals(mesaController.getRecintoSeleccionado().getId())) {
                mesaController.liberarRecintoSeleccionado();
            }
        }
        JsfUtil.addInfoMessageFromBundle("recintos.mensaje.eliminar.exito");
        recargarListaRecintosActual();
        PrimeFaces.current().ajax().update(JsfUtil.GROWL_MESSAGES);
    }

    public void obtieneParroquias() {
        if (cantonSeleccionado.getId() != null && OPCION_TODOS.equals(cantonSeleccionado.getId())) {
            parroquias = obtenerTodasParroquias();
            listaRecintos = recintoService.listarDTOs();
            JsfUtil.addInfoMessageFromBundle("recintos.mensaje.registros.encontrados", listaRecintos.size());
            return;
        }
        if (cantonSeleccionado.getId() != null) {
            cantonSeleccionado = geograpBean.getById(cantonSeleccionado.getId());
            parroquias = geograpBean.getByFatherId(cantonSeleccionado.getId());
            listaRecintos = recintoService.listarDTOsPorParroquias(parroquias);
            if (listaRecintos == null || listaRecintos.isEmpty()) {
                JsfUtil.addWarningMessageFromBundle("recintos.mensaje.sin.registros.filtro", cantonSeleccionado.getName());
            } else {
                JsfUtil.addInfoMessageFromBundle("recintos.mensaje.registros.encontrados", listaRecintos.size());
            }
        } else {
            if (parroquias != null) {
                parroquias.clear();
            }
            recintoSeleccionado = null;
            listaRecintos.clear();
        }
    }

    public void obtieneRecintosPorParroquia() {
        if (parroquiaSeleccionado.getId() != null && OPCION_TODOS.equals(parroquiaSeleccionado.getId())) {
            List<Geograp> parroquiasFiltro = obtenerParroquiasFiltroActual();
            listaRecintos = parroquiasFiltro.isEmpty()
                    ? recintoService.listarDTOs()
                    : recintoService.listarDTOsPorParroquias(parroquiasFiltro);
            if (listaRecintos == null || listaRecintos.isEmpty()) {
                JsfUtil.addWarningMessageFromBundle("recintos.mensaje.sin.registros");
            } else {
                JsfUtil.addInfoMessageFromBundle("recintos.mensaje.registros.encontrados", listaRecintos.size());
            }
            return;
        }
        if (parroquiaSeleccionado.getId() != null) {
            parroquiaSeleccionado = geograpBean.getById(parroquiaSeleccionado.getId());
            List<Geograp> parroquiasTmp = new ArrayList<>();
            parroquiasTmp.add(parroquiaSeleccionado);
            listaRecintos = recintoService.listarDTOsPorParroquias(parroquiasTmp);
            if (listaRecintos == null || listaRecintos.isEmpty()) {
                JsfUtil.addWarningMessageFromBundle("recintos.mensaje.sin.registros.filtro", parroquiaSeleccionado.getName());
            } else {
                JsfUtil.addInfoMessageFromBundle("recintos.mensaje.registros.encontrados", listaRecintos.size());
            }
        } else {
            recintoSeleccionado = new RecintoDTO();
            listaRecintos.clear();
        }
    }

    public void guardarRecintoSeleccionado() {
        try {
            if (recintoSeleccionado == null) {
                FacesContext.getCurrentInstance().validationFailed();
                return;
            }
            boolean esEdicion = recintoSeleccionado.getId() != null;
            RecintoDTO persistido = recintoService.guardarDesdeDTO(recintoSeleccionado);
            if (persistido != null) {
                JsfUtil.addSuccessMessageFromBundle(esEdicion
                        ? "recintos.mensaje.actualizar.exito"
                        : "recintos.mensaje.guardar.exito");
                boolean recintoPermaneceSeleccionado = mesaController.getRecintoSeleccionado() != null
                        && persistido.getId().equals(mesaController.getRecintoSeleccionado().getId());
                if (esEdicion) {
                    RecintoDTO filaActualizada = actualizarObjetoFila(persistido);
                    recintoSeleccionado = filaActualizada;
                    if (recintoPermaneceSeleccionado) {
                        mesaController.setRecintoSeleccionado(filaActualizada);
                    }
                    actualizarFilaEditada();
                    if (recintoPermaneceSeleccionado) {
                        PrimeFaces.current().ajax().update("frmRecintos:infoRecintoSeleccionado");
                    }
                } else {
                    recintoSeleccionado = persistido;
                    recargarListaRecintosActual();
                    PrimeFaces.current().ajax().update(
                            "frmRecintos:tblRecintos",
                            "frmRecintos:resumenRecintos");
                }
            } else {
                JsfUtil.addErrorMessageFromBundle("recintos.mensaje.parroquia.requerida");
                FacesContext.getCurrentInstance().validationFailed();
                PrimeFaces.current().ajax().update(JsfUtil.GROWL_MESSAGES);
                return;
            }
        } catch (Exception e) {
            log.error("ERROR AL GUARDAR RECINTO", e);
            JsfUtil.addErrorMessageFromBundle("recintos.mensaje.error");
            FacesContext.getCurrentInstance().validationFailed();
            PrimeFaces.current().ajax().update(JsfUtil.GROWL_MESSAGES);
            return;
        }
        PrimeFaces.current().ajax().update(JsfUtil.GROWL_MESSAGES);
    }

    public void eliminarRecintosSeleccionados() {
        int eliminados = 0;
        int omitidos = 0;
        if (listaRecintosSeleccionados != null) {
            for (RecintoDTO item : listaRecintosSeleccionados) {
                if (tieneMesasAsociadas(item)) {
                    omitidos++;
                    continue;
                }
                if (item.getId() != null && recintoService.eliminarPorId(item.getId()) != null) {
                    eliminados++;
                }
            }
        }
        listaRecintosSeleccionados = new ArrayList<>();
        JsfUtil.addInfoMessageFromBundle("recintos.mensaje.eliminados", eliminados);
        if (omitidos > 0) {
            JsfUtil.addWarningMessageFromBundle("recintos.mensaje.eliminar.con.mesas.multiple", omitidos);
        }
        recargarListaRecintosActual();
        PrimeFaces.current().ajax().update(JsfUtil.GROWL_MESSAGES);
    }

    public void cagraDatosRecintoSeleccionado() {
        try {
            if (recintoSeleccionado != null && recintoSeleccionado.getId() != null
                    && recintoSeleccionado.getUbicacionId() != null) {
                Geograp parroquia = geograpBean.getById(recintoSeleccionado.getUbicacionId());
                if (parroquia != null && parroquia.getGeograp() != null) {
                    this.parroquiaSeleccionado = parroquia;
                    this.cantonSeleccionado = parroquia.getGeograp();
                    this.parroquias = geograpBean.getByFatherId(cantonSeleccionado.getId());
                }
            }
        } catch (Exception e) {
        }
    }

    public void prepararEdicion(RecintoDTO recinto, Integer indiceFila) {
        this.recintoFilaEdicion = recinto;
        this.recintoSeleccionado = copiarRecinto(recinto);
        this.indiceFilaEdicion = indiceFila;
        cagraDatosRecintoSeleccionado();
    }

    private void liberarContextoRecintoMesa() {
        this.recintoSeleccionado = null;
        this.listaRecintosSeleccionados = new ArrayList<>();
        mesaController.liberarRecintoSeleccionado();
    }

    private void recargarListaRecintosActual() {
        if (parroquiaSeleccionado != null && parroquiaSeleccionado.getId() != null
                && OPCION_TODOS.equals(parroquiaSeleccionado.getId())) {
            List<Geograp> parroquiasFiltro = obtenerParroquiasFiltroActual();
            listaRecintos = parroquiasFiltro.isEmpty()
                    ? recintoService.listarDTOs()
                    : recintoService.listarDTOsPorParroquias(parroquiasFiltro);
            return;
        }
        if (parroquiaSeleccionado != null && parroquiaSeleccionado.getId() != null) {
            Geograp parroquia = geograpBean.getById(parroquiaSeleccionado.getId());
            if (parroquia != null) {
                List<Geograp> parroquiasTmp = new ArrayList<>();
                parroquiasTmp.add(parroquia);
                listaRecintos = recintoService.listarDTOsPorParroquias(parroquiasTmp);
                return;
            }
        }
        if (parroquias != null && !parroquias.isEmpty()) {
            listaRecintos = recintoService.listarDTOsPorParroquias(parroquias);
            return;
        }
        if (cantonSeleccionado != null && cantonSeleccionado.getId() != null
                && OPCION_TODOS.equals(cantonSeleccionado.getId())) {
            listaRecintos = recintoService.listarDTOs();
            return;
        }
        listaRecintos = new ArrayList<>();
    }

    private RecintoDTO actualizarObjetoFila(RecintoDTO persistido) {
        RecintoDTO destino = recintoFilaEdicion != null ? recintoFilaEdicion : persistido;
        copiarDatosRecinto(persistido, destino);
        return destino;
    }

    private void actualizarFilaEditada() {
        if (indiceFilaEdicion != null && indiceFilaEdicion >= 0) {
            String prefijoFila = "frmRecintos:tblRecintos:" + indiceFilaEdicion + ":";
            FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add(
                    prefijoFila + "nombreRecintoFila");
            FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add(
                    prefijoFila + "cantonRecintoFila");
            FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add(
                    prefijoFila + "parroquiaRecintoFila");
        }
    }

    private RecintoDTO copiarRecinto(RecintoDTO origen) {
        if (origen == null) {
            return null;
        }
        RecintoDTO copia = new RecintoDTO();
        copiarDatosRecinto(origen, copia);
        return copia;
    }

    private void copiarDatosRecinto(RecintoDTO origen, RecintoDTO destino) {
        destino.setId(origen.getId());
        destino.setNombre(origen.getNombre());
        destino.setUbicacionId(origen.getUbicacionId());
        destino.setUbicacionNombre(origen.getUbicacionNombre());
        destino.setCantonId(origen.getCantonId());
        destino.setCantonNombre(origen.getCantonNombre());
        destino.setProvinciaId(origen.getProvinciaId());
        destino.setProvinciaNombre(origen.getProvinciaNombre());
        destino.setEstadoTarea(origen.getEstadoTarea());
    }

    public int getTotalRecintos() {
        return listaRecintos != null ? listaRecintos.size() : 0;
    }

    public int getTotalRecintosActivos() {
        return getTotalRecintos();
    }

    private boolean tieneMesasAsociadas(RecintoDTO recinto) {
        if (recinto == null || recinto.getId() == null) {
            return false;
        }
        List<RecintoDTO> recintos = new ArrayList<>();
        recintos.add(recinto);
        List<MesaDTO> mesas = mesaService.listarDTOsPorRecintos(toRecintoEntities(recintos));
        return mesas != null && !mesas.isEmpty();
    }

    private List<ec.com.antenasur.model.tec.Recinto> toRecintoEntities(List<RecintoDTO> dtos) {
        List<ec.com.antenasur.model.tec.Recinto> entidades = new ArrayList<>();
        if (dtos == null) {
            return entidades;
        }
        for (RecintoDTO dto : dtos) {
            ec.com.antenasur.model.tec.Recinto r = new ec.com.antenasur.model.tec.Recinto();
            r.setId(dto.getId());
            entidades.add(r);
        }
        return entidades;
    }

    private List<Geograp> obtenerParroquiasFiltroActual() {
        if (cantonSeleccionado != null && cantonSeleccionado.getId() != null
                && !OPCION_TODOS.equals(cantonSeleccionado.getId())) {
            return geograpBean.getByFatherId(cantonSeleccionado.getId());
        }
        return obtenerTodasParroquias();
    }

    private List<Geograp> obtenerTodasParroquias() {
        List<Geograp> resultado = new ArrayList<>();
        if (cantones == null) {
            return resultado;
        }
        for (Geograp canton : cantones) {
            if (canton != null && canton.getId() != null) {
                List<Geograp> parroquiasCanton = geograpBean.getByFatherId(canton.getId());
                if (parroquiasCanton != null) {
                    resultado.addAll(parroquiasCanton);
                }
            }
        }
        return resultado;
    }
}
