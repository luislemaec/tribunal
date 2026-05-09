package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.primefaces.PrimeFaces;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.dto.CatalogoGeneralDTO;
import ec.com.antenasur.dto.IglesiaPersonaDTO;
import ec.com.antenasur.dto.MiembroJRVDTO;
import ec.com.antenasur.dto.PeriodoDTO;
import ec.com.antenasur.model.tec.MiembroJRV;
import ec.com.antenasur.service.tec.CatalogoGeneralService;
import ec.com.antenasur.service.tec.MiembroJRVService;
import ec.com.antenasur.service.tec.PeriodoService;
import ec.com.antenasur.util.JsfUtil;
import ec.com.antenasur.util.ModeloColumna;
import ec.com.antenasur.util.ReflectionColumnModelBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Named
@ViewScoped
@Slf4j
public class JrvController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String FORMULARIO = "frmMJRV";
    private static final String TABLA = "tblMJRV";
    private static final String MENSAJE_ELIMINA_OK = "Miembro de JRV eliminado";

    @Inject
    private LoginBean loginBean;

    @Inject
    private MiembroJRVService mjrvService;

    @Inject
    private CatalogoGeneralService catalogoService;

    @Inject
    private PeriodoService periodoService;

    @Setter
    @Getter
    private MiembroJRVDTO mjrvSeleccionado;

    @Setter
    @Getter
    private List<MiembroJRVDTO> listaMJRV, listaMJRVSeleccionados;

    @Setter
    @Getter
    private CatalogoGeneralDTO cargo;

    @Setter
    @Getter
    private PeriodoDTO periodo;

    @Setter
    @Getter
    private IglesiaPersonaDTO iglesiaPersona;

    @Setter
    @Getter
    private List<PeriodoDTO> periodos;

    @Setter
    @Getter
    private List<ModeloColumna> columnas = new ArrayList<ModeloColumna>(0);

    public JrvController() {
        this.columnas = new ReflectionColumnModelBuilder(MiembroJRV.class).setExcludedProperties(
                "id", "fechaCrea", "fechaActualiza", "usuarioCrea", "usuarioActualiza",
                "estado", "seleccionado", "persisted").build();
    }

    @PostConstruct
    private void init() {
        periodo = new PeriodoDTO();
        cargo = new CatalogoGeneralDTO();
        iglesiaPersona = new IglesiaPersonaDTO();
        periodos = periodoService.listarDTOs();
        if (periodos != null && !periodos.isEmpty()) {
            periodo = periodos.get(0);
        }
    }

    public void nuevo() {
        inicializaSeleccionado();
    }

    public void inicializaSeleccionado() {
        if (listaMJRV != null) {
            listaMJRV.clear();
        }
        this.mjrvSeleccionado = new MiembroJRVDTO();
    }

    public boolean existeSeleccionados() {
        return this.listaMJRVSeleccionados != null && !this.listaMJRVSeleccionados.isEmpty();
    }

    public void eliminarSeleccionados() {
        int eliminados = 0;
        if (listaMJRVSeleccionados != null) {
            for (MiembroJRVDTO item : listaMJRVSeleccionados) {
                if (item.getId() != null && mjrvService.eliminarPorId(item.getId()) != null) {
                    eliminados++;
                }
            }
        }
        JsfUtil.addInfoMessage(eliminados + " " + MENSAJE_ELIMINA_OK);
        PrimeFaces.current().ajax().update(FORMULARIO + ":" + TABLA, "msgs");
    }

    public String getMensajeBotonEliminar() {
        if (existeSeleccionados()) {
            int size = this.listaMJRVSeleccionados.size();
            return size > 1 ? size + " MJRM seleccionadas" : "1 mrjv seleccionada";
        }
        return "Eliminar";
    }
}
