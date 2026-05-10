package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.primefaces.PrimeFaces;

import ec.com.antenasur.bean.DocumentoBean;
import ec.com.antenasur.bean.GeograpBean;
import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.dto.MesaDTO;
import ec.com.antenasur.dto.RecintoDTO;
import ec.com.antenasur.enums.EstadoTarea;
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
    private static final String MENSAJE_REGISTRA_OK = "Mesa registrado";
    private static final String MENSAJE_ACTUALIZA_OK = "Mesa actualizado";
    private static final String MENSAJE_ELIMINA_OK = "Mesa eliminado";
    public static final String MENSAJE_CONFORMACION_ELIMINAR = "¿Esta seguro de eliminar?";

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

    @PostConstruct
    private void init() {
        try {
            this.columnas = new ReflectionColumnModelBuilder(Mesa.class).setExcludedProperties("id", "fechaCrea", "fechaActualiza", "usuarioCrea", "usuarioActualiza",
                    "estado", "seleccionado", "persisted").build();

            this.cantonSeleccionado = parroquiaSeleccionado = new Geograp();
            this.recintoSeleccionado = new RecintoDTO();
            this.listaMesasSeleccionados = new ArrayList<>();
            this.cantones = geograpBean.getByFatherId(7);
            this.listaMesas = mesaService.listarDTOsConFlagDocumentos(Constantes.ACTA_ESCRUTINIO);
            this.mesasEscrutadas = mesaService.listarDTOsEscrutadas(EstadoTarea.COMPLETADO);
            this.porcentajeMesasEscrutado = mesaService.calcularPorcentajeEscrutado();
            this.listaRecintos = recintoService.listarDTOs();
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    public void inicializaMesaSeleccionado() {
        if (listaMesas != null) {
            listaMesas.clear();
        }
        this.mesaSeleccionado = new MesaDTO();
        this.mesaSeleccionado.setRecinto(new RecintoDTO());
    }

    public void nuevaMesa() {
        inicializaMesaSeleccionado();
    }

    public boolean existeMesasSeleccionados() {
        return this.listaMesasSeleccionados != null && !this.listaMesasSeleccionados.isEmpty();
    }

    public String getMensajeBotonEliminar() {
        if (existeMesasSeleccionados()) {
            int size = this.listaMesasSeleccionados.size();
            return size > 1 ? size + " Mesas seleccionadas" : "1 mesa seleccionada";
        }
        return "Eliminar";
    }

    public void eliminarMesaSeleccionado() {
        if (mesaSeleccionado != null && mesaSeleccionado.getId() != null) {
            mesaService.eliminarPorId(mesaSeleccionado.getId());
        }
        JsfUtil.addInfoMessage(MENSAJE_ELIMINA_OK);
        PrimeFaces.current().ajax().update(FORMULARIO + ":" + TABLA, "msgs");
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
            parroquiasTmp.add(cantonSeleccionado);
            listaRecintos = recintoService.listarDTOsPorParroquias(parroquiasTmp);
            listaMesas = mesaService.listarDTOsPorRecintos(toRecintoEntities(listaRecintos));
            if (listaMesas == null || listaMesas.isEmpty()) {
                JsfUtil.addWarningMessage("No existe registro de mesas en " + parroquiaSeleccionado.getName());
            } else {
                JsfUtil.addInfoMessage(listaMesas.size() + " mesas registradas");
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
                JsfUtil.addSuccessMessage(esEdicion ? MENSAJE_ACTUALIZA_OK : MENSAJE_REGISTRA_OK);
                mesaSeleccionado = null;
                listaMesas = mesaService.listarDTOs();
                PrimeFaces.current().ajax().update("msgs", FORMULARIO);
            }
        } catch (Exception e) {
            log.error("ERROR AL GUARDAR MESA", e);
        }
        PrimeFaces.current().executeScript("PF('dlgMesa').hide()");
        PrimeFaces.current().ajax().update(FORMULARIO + ":" + TABLA);
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
        JsfUtil.addInfoMessage(eliminados + " " + MENSAJE_ELIMINA_OK);
        PrimeFaces.current().ajax().update(FORMULARIO + ":" + TABLA, "msgs");
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
                JsfUtil.addWarningMessage("No existe registro de mesas en " + recintoSeleccionado.getNombre());
            } else {
                JsfUtil.addInfoMessage(listaMesas.size() + " mesas registradas");
            }
        } else {
            listaMesas.clear();
        }
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
     * Convierte DTOs a entidades stub (solo con id) para pasarlos al método
     * {@code listarDTOsPorRecintos} del service, que necesita entidades como
     * parámetro de query JPQL ({@code WHERE recinto IN :recintos}).
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
}
