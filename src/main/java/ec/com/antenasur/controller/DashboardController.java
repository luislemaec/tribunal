package ec.com.antenasur.controller;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.bean.MesaBean;
import ec.com.antenasur.bean.RecintoBean;
import ec.com.antenasur.service.IglesiaFacade;
import ec.com.antenasur.service.PersonaFacade;
import ec.com.antenasur.service.tec.PadronFacade;
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
public class DashboardController implements Serializable {

    private static final String DESTINATION = System.getProperty("java.io.tmpdir");

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
    private MesaBean mesaBean;

    @Inject
    private RecintoBean recintoBean;

    @Inject
    private PadronFacade padronFacade;

    @Inject
    private IglesiaFacade iglesiaFacade;

    @Setter
    @Getter
    private float porcentajeMesasEscrutadas;

    @Setter
    @Getter
    private int totalPersonas, totalIglesias, totalRecintos, totalMesas;

    @PostConstruct
    private void init() {
        try {
            totalPersonas = mesaBean.totalVotantes();
            totalIglesias = iglesiaFacade.count();
            totalMesas = mesaBean.totalMesas();
            totalRecintos = recintoBean.totalRecintos();
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

}
