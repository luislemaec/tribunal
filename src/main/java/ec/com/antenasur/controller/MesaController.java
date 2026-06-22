package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.primefaces.PrimeFaces;

import ec.com.antenasur.bean.DocumentoBean;
import ec.com.antenasur.bean.GeograpBean;
import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.dto.MesaDTO;
import ec.com.antenasur.dto.RecintoDTO;
import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.tec.Documentos;
import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.service.tec.MesaService;
import ec.com.antenasur.service.tec.RecintoService;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.JsfUtil;
import ec.com.antenasur.util.ModeloColumna;
import ec.com.antenasur.util.ReflectionColumnModelBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Named
@ViewScoped
@Slf4j
public class MesaController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String FORMULARIO = "frmMesas";
    private static final String TABLA = "tblMesas";
    private static final String MENSAJE_REGISTRA_OK = "mesas.mensaje.guardar.exito";
    private static final String MENSAJE_ACTUALIZA_OK = "mesas.mensaje.actualizar.exito";
    private static final String MENSAJE_ELIMINA_OK = "mesas.mensaje.eliminar.exito";
    public static final String MENSAJE_CONFORMACION_ELIMINAR = "mesas.confirmacion.eliminar";

    @Inject
    private LoginBean loginBean;

    @Inject
    private MesaService mesaService;

    @Inject
    private RecintoService recintoService;

    @Inject
    private GeograpBean geograpBean;

    @Inject
    private DocumentoBean documentoBean;

    @Setter
    @Getter
    private MesaDTO mesaSeleccionado;

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
    private List<MesaDTO> listaMesas, listaMesasSeleccionados, mesasEscrutadas;

    @Setter
    @Getter
    private List<RecintoDTO> listaRecintos;

    @Setter
    @Getter
    private List<ModeloColumna> columnas = new ArrayList<ModeloColumna>(0);

    @Setter
    @Getter
    private List<Documentos> documentos;

    @Setter
    @Getter
    private float porcentajeMesasEscrutado;

    @Getter
    private Map<Integer, Long> totalMesasPorRecinto = new HashMap<>();

    @PostConstruct
    private void init() {
        try {
            this.columnas = new ReflectionColumnModelBuilder(Mesa.class).setExcludedProperties("id", "fechaCrea", "fechaActualiza", "usuarioCrea", "usuarioActualiza",
                    "estado", "seleccionado", "persisted").build();

            this.cantonSeleccionado = parroquiaSeleccionado = new Geograp();
            this.recintoSeleccionado = new RecintoDTO();
            this.listaMesasSeleccionados = new ArrayList<>();
            this.cantones = geograpBean.getByFatherId(7);
            this.listaMesas = mesaService.listarDTOs();
            this.listaRecintos = recintoService.listarDTOs();
            actualizarTotalesPorRecinto();
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    public void inicializaMesaSeleccionado() {
        this.mesaSeleccionado = new MesaDTO();
        this.mesaSeleccionado.setRecinto(new RecintoDTO());
    }

    public void nuevaMesa() {
        inicializaMesaSeleccionado();
    }

    public void nuevaMesaParaRecinto() {
        if (recintoSeleccionado == null || recintoSeleccionado.getId() == null) {
            JsfUtil.addWarningMessageFromBundle("recintos.mesas.mensaje.seleccionar.recinto");
            FacesContext.getCurrentInstance().validationFailed();
            PrimeFaces.current().ajax().update(JsfUtil.GROWL_MESSAGES);
            return;
        }
        this.mesaSeleccionado = new MesaDTO();
        this.mesaSeleccionado.setRecinto(recintoSeleccionado);
        this.mesaSeleccionado.setUbicacionId(recintoSeleccionado.getUbicacionId());
        this.cantonSeleccionado = new Geograp();
        this.cantonSeleccionado.setId(recintoSeleccionado.getCantonId());
        this.parroquiaSeleccionado = new Geograp();
        this.parroquiaSeleccionado.setId(recintoSeleccionado.getUbicacionId());
    }

    public void seleccionarRecinto(RecintoDTO recinto) {
        this.recintoSeleccionado = recinto;
        recargarListaMesasActual();
        if (listaMesas == null || listaMesas.isEmpty()) {
            JsfUtil.addInfoMessageFromBundle("recintos.mesas.mensaje.sin.registros");
            PrimeFaces.current().ajax().update(JsfUtil.GROWL_MESSAGES);
        }
    }

    public void liberarRecintoSeleccionado() {
        this.recintoSeleccionado = new RecintoDTO();
        this.mesaSeleccionado = null;
        this.listaMesas = new ArrayList<>();
        this.listaMesasSeleccionados = new ArrayList<>();
    }

    public boolean existeMesasSeleccionados() {
        return this.listaMesasSeleccionados != null && !this.listaMesasSeleccionados.isEmpty();
    }

    public String getMensajeBotonEliminar() {
        if (existeMesasSeleccionados()) {
            int size = this.listaMesasSeleccionados.size();
            return size > 1
                    ? JsfUtil.getMessage("mesas.boton.eliminar.seleccionadas", size)
                    : JsfUtil.getMessage("mesas.boton.eliminar.una");
        }
        return JsfUtil.getMessage("mesas.boton.eliminar");
    }

    public void eliminarMesa(MesaDTO mesa) {
        if (mesa == null || mesa.getId() == null) {
            JsfUtil.addErrorMessageFromBundle("mesas.mensaje.error");
            FacesContext.getCurrentInstance().validationFailed();
            PrimeFaces.current().ajax().update(JsfUtil.GROWL_MESSAGES);
            return;
        }

        try {
            MesaDTO mesaEliminada = mesaService.eliminarPorId(mesa.getId());
            if (mesaEliminada == null) {
                JsfUtil.addErrorMessageFromBundle("mesas.mensaje.error");
                FacesContext.getCurrentInstance().validationFailed();
                PrimeFaces.current().ajax().update(JsfUtil.GROWL_MESSAGES);
                return;
            }

            mesaSeleccionado = null;
            recargarListaMesasActual();
            actualizarTotalesPorRecinto();
            JsfUtil.addInfoMessageFromBundle(MENSAJE_ELIMINA_OK);
        } catch (Exception e) {
            log.error("ERROR AL ELIMINAR MESA id={}", mesa.getId(), e);
            JsfUtil.addErrorMessageFromBundle("mesas.mensaje.error");
            FacesContext.getCurrentInstance().validationFailed();
        }
        PrimeFaces.current().ajax().update(JsfUtil.GROWL_MESSAGES);
    }

    public void eliminarMesaSeleccionado() {
        eliminarMesa(mesaSeleccionado);
    }

    public void obtieneParroquias() {
        if (cantonSeleccionado.getId() != null) {
            cantonSeleccionado = geograpBean.getById(cantonSeleccionado.getId());
            parroquias = geograpBean.getByFatherId(cantonSeleccionado.getId());
            listaRecintos = recintoService.listarDTOsPorParroquias(parroquias);
            listaMesas = mesaService.listarDTOsPorRecintos(toRecintoEntities(listaRecintos));
        } else {
            if (parroquias != null) {
                parroquias.clear();
            }
            mesaSeleccionado = null;
            listaMesas.clear();
        }
    }

    public void obtieneMesasPorParroquia() {
        if (parroquiaSeleccionado.getId() != null) {
            parroquiaSeleccionado = geograpBean.getById(parroquiaSeleccionado.getId());
            List<Geograp> parroquiasTmp = new ArrayList<>();
            parroquiasTmp.add(parroquiaSeleccionado);
            listaRecintos = recintoService.listarDTOsPorParroquias(parroquiasTmp);
            listaMesas = mesaService.listarDTOsPorRecintos(toRecintoEntities(listaRecintos));
            if (listaMesas == null || listaMesas.isEmpty()) {
                JsfUtil.addWarningMessageFromBundle("mesas.mensaje.sin.registros.filtro", parroquiaSeleccionado.getName());
            } else {
                JsfUtil.addInfoMessageFromBundle("mesas.mensaje.registros.encontrados", listaMesas.size());
            }
        } else {
            mesaSeleccionado = new MesaDTO();
            listaMesas.clear();
        }
    }

    public void guardarMesaSeleccionado() {
        try {
            if (mesaSeleccionado == null) {
                return;
            }
            boolean esEdicion = mesaSeleccionado.getId() != null;
            MesaDTO persistido = mesaService.guardarDesdeDTO(mesaSeleccionado);
            if (persistido != null) {
                JsfUtil.addSuccessMessageFromBundle(esEdicion ? MENSAJE_ACTUALIZA_OK : MENSAJE_REGISTRA_OK);
                mesaSeleccionado = null;
                recargarListaMesasActual();
                actualizarTotalesPorRecinto();
                PrimeFaces.current().ajax().update(JsfUtil.GROWL_MESSAGES);
            } else {
                JsfUtil.addErrorMessageFromBundle("mesas.mensaje.error");
                FacesContext.getCurrentInstance().validationFailed();
                PrimeFaces.current().ajax().update(JsfUtil.GROWL_MESSAGES);
                return;
            }
        } catch (Exception e) {
            log.error("ERROR AL GUARDAR MESA", e);
            JsfUtil.addErrorMessageFromBundle("mesas.mensaje.error");
            FacesContext.getCurrentInstance().validationFailed();
            PrimeFaces.current().ajax().update(JsfUtil.GROWL_MESSAGES);
        }
        PrimeFaces.current().executeScript("PF('dlgMesa').hide()");
        PrimeFaces.current().ajax().update(JsfUtil.GROWL_MESSAGES);
    }

    public void eliminarMesasSeleccionados() {
        int eliminados = 0;
        if (listaMesasSeleccionados != null) {
            for (MesaDTO item : listaMesasSeleccionados) {
                if (item.getId() != null && mesaService.eliminarPorId(item.getId()) != null) {
                    eliminados++;
                }
            }
        }
        listaMesasSeleccionados = new ArrayList<>();
        recargarListaMesasActual();
        actualizarTotalesPorRecinto();
        JsfUtil.addInfoMessageFromBundle("mesas.mensaje.eliminadas", eliminados);
        PrimeFaces.current().ajax().update(JsfUtil.GROWL_MESSAGES);
    }

    public void cargaDatosMesaSeleccionado() {
        try {
            if (mesaSeleccionado != null && mesaSeleccionado.getId() != null
                    && mesaSeleccionado.getUbicacionId() != null) {
                Geograp parroquia = geograpBean.getById(mesaSeleccionado.getUbicacionId());
                if (parroquia != null && parroquia.getGeograp() != null) {
                    this.cantonSeleccionado = parroquia.getGeograp();
                    this.parroquias = geograpBean.getByFatherId(cantonSeleccionado.getId());
                }
            }
        } catch (Exception e) {
        }
    }

    public void obtieneMesasPorRecinto() {
        if (recintoSeleccionado != null && recintoSeleccionado.getId() != null) {
            recintoSeleccionado = recintoService.obtenerDTOPorId(recintoSeleccionado.getId());
            List<RecintoDTO> listaRecintosTmp = new ArrayList<>();
            listaRecintosTmp.add(recintoSeleccionado);
            listaMesas = mesaService.listarDTOsPorRecintos(toRecintoEntities(listaRecintosTmp));
            if (listaMesas == null || listaMesas.isEmpty()) {
                JsfUtil.addWarningMessageFromBundle("mesas.mensaje.sin.registros.filtro", recintoSeleccionado.getNombre());
            } else {
                JsfUtil.addInfoMessageFromBundle("mesas.mensaje.registros.encontrados", listaMesas.size());
            }
        } else {
            listaMesas.clear();
        }
    }

    public int getTotalMesasRegistradas() {
        return totalMesasPorRecinto.values().stream()
                .mapToInt(Long::intValue)
                .sum();
    }

    public int getTotalMesasRecintoSeleccionado() {
        if (recintoSeleccionado == null || recintoSeleccionado.getId() == null) {
            return 0;
        }
        if (listaMesas == null) {
            return 0;
        }
        return listaMesas.size();
    }

    public long getTotalMesasPorRecinto(Integer recintoId) {
        if (recintoId == null) {
            return 0L;
        }
        return totalMesasPorRecinto.getOrDefault(recintoId, 0L);
    }

    public void cargaActasE() {
        try {
            if (mesaSeleccionado != null && mesaSeleccionado.getId() != null) {
                documentos = documentoBean.getDocumentosPorEntidadYTipoDoc(mesaSeleccionado.getId(), Constantes.ACTA_ESCRUTINIO);
            }
        } catch (Exception e) {
            log.error("ERROR AL OBTENER DOCUMENTOS", e);
        }
    }

    /**
     * Convierte DTOs a entidades stub (solo con id) para pasarlos al metodo
     * {@code listarDTOsPorRecintos} del service, que necesita entidades como
     * parametro de query JPQL ({@code WHERE recinto IN :recintos}).
     */
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

    private void recargarListaMesasActual() {
        if (recintoSeleccionado != null && recintoSeleccionado.getId() != null) {
            List<RecintoDTO> listaRecintosTmp = new ArrayList<>();
            listaRecintosTmp.add(recintoSeleccionado);
            listaMesas = mesaService.listarDTOsPorRecintos(toRecintoEntities(listaRecintosTmp));
            return;
        }
        listaMesas = mesaService.listarDTOs();
    }

    private void actualizarTotalesPorRecinto() {
        totalMesasPorRecinto = new HashMap<>();
        List<MesaDTO> mesas = mesaService.listarDTOs();
        if (mesas == null) {
            return;
        }
        for (MesaDTO mesa : mesas) {
            if (mesa.getRecinto() != null && mesa.getRecinto().getId() != null) {
                totalMesasPorRecinto.merge(mesa.getRecinto().getId(), 1L, Long::sum);
            }
        }
    }
}
