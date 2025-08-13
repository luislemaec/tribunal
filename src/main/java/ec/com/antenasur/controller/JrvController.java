package ec.com.antenasur.controller;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.domain.Geograp;
import ec.com.antenasur.domain.Iglesia;
import ec.com.antenasur.domain.IglesiaPersona;
import ec.com.antenasur.domain.Persona;
import ec.com.antenasur.domain.tec.CatalogoGeneral;
import ec.com.antenasur.domain.tec.Documentos;
import ec.com.antenasur.domain.tec.Mesa;
import ec.com.antenasur.domain.tec.MiembroJRV;
import ec.com.antenasur.domain.tec.Periodo;
import ec.com.antenasur.domain.tec.Recinto;
import ec.com.antenasur.service.tec.CatalogoGeneralFacade;
import ec.com.antenasur.service.tec.MesaFacade;
import ec.com.antenasur.service.tec.MiembroJRVFacade;
import ec.com.antenasur.service.tec.PeriodoFacade;
import ec.com.antenasur.util.JsfUtil;
import ec.com.antenasur.util.ModeloColumna;
import ec.com.antenasur.util.ReflectionColumnModelBuilder;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.datatable.DataTableBase;
import org.primefaces.component.dialog.Dialog;
import org.primefaces.component.dialog.DialogBase;
import org.primefaces.component.outputpanel.OutputPanel;
import org.primefaces.component.outputpanel.OutputPanelBase;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Named
@ViewScoped
@Slf4j
public class JrvController implements Serializable {

    private static final String DESTINATION = System.getProperty("java.io.tmpdir");

    private static final long serialVersionUID = 1L;

    private static final String FORMULARIO = "frmMJRV";
    private static final String TABLA = "tblMJRV";
    private static final String MENSAJE_REGISTRA_OK = "Miembro de JRV registrado";
    private static final String MENSAJE_ACTUALIZA_OK = "Miembro de JRV actualizado";
    private static final String MENSAJE_ELIMINA_OK = "Miembro de JRV eliminado";
    public static final String MENSAJE_CONFORMACION_ELIMINAR = "¿Esta seguro de eliminar?";

    @Inject
    private LoginBean loginBean;

    @Inject
    private MiembroJRVFacade mjrvFacade;

    @Inject
    private CatalogoGeneralFacade catalogoFacade;

    @Inject
    private PeriodoFacade periodoFacade;

    @Setter
    @Getter
    private MiembroJRV mjrvSeleccionado;

    @Setter
    @Getter
    private List<MiembroJRV> listaMJRV, listaMJRVSeleccionados;

    @Setter
    @Getter
    private List<Documentos> documentos;


    @Setter
    @Getter
    private CatalogoGeneral cargo;

    @Setter
    @Getter
    private Periodo periodo;

    @Setter
    @Getter
    private IglesiaPersona iglesiaPersona;

    @Setter
    @Getter
    private List<Periodo> periodos;

    @Setter
    @Getter
    private List<ModeloColumna> columnas = new ArrayList<ModeloColumna>(0);

    public JrvController() {
        this.columnas = new ReflectionColumnModelBuilder(MiembroJRV.class).setExcludedProperties("id", "fechaCrea", "fechaActualiza", "usuarioCrea", "usuarioActualiza",
                "estado", "seleccionado", "persisted").build();
    }

    @PostConstruct
    private void init() {
        periodo = new Periodo();
        cargo = new CatalogoGeneral();
        iglesiaPersona = new IglesiaPersona(new Iglesia(), new Persona());

        columnas.size();

        periodos = periodoFacade.findAll();
        periodo = periodos.get(0);

        DialogBase dlgMjrv = new Dialog();
        dlgMjrv.setWidgetVar("dlgMjrv2");
        dlgMjrv.setHeader("Registrar MIRV");
        dlgMjrv.setShowEffect("fade");
        dlgMjrv.setModal(true);
        dlgMjrv.setResponsive(true);
        dlgMjrv.setClosable(false);

        OutputPanelBase outPl1 = new OutputPanel();
        outPl1.setId("outPnlMesas2");
        outPl1.setStyleClass("ui-fluid");

        OutputPanelBase outPl2 = new OutputPanel();

        outPl1.getChildren().add(outPl2);
        dlgMjrv.getChildren().add(outPl1);

        PrimeFaces.current().executeScript("PF(dlgMjrv2).show()");
        PrimeFaces.current().ajax().update("dlgMjrv2");

    }

    public void nuevo() {
        inicializaSeleccionado();
    }

    public void inicializaSeleccionado() {
        if (listaMJRV != null) {
            listaMJRV.clear();
        }
        this.mjrvSeleccionado = new MiembroJRV();

    }

    public boolean existeSeleccionados() {
        return this.listaMJRVSeleccionados != null && !this.listaMJRVSeleccionados.isEmpty();
    }

    public void eliminarSeleccionados() {
        if (listaMJRVSeleccionados != null) {
            for (MiembroJRV item : listaMJRVSeleccionados) {
                mjrvFacade.delete(item);
            }
        }
        JsfUtil.addInfoMessage(+listaMJRVSeleccionados.size() + MENSAJE_ELIMINA_OK);
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
