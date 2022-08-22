package ec.com.antenasur.bean;

import java.sql.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import ec.com.antenasur.domain.Proceso;
import ec.com.antenasur.service.ProcesoFacade;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Luis Lema
 */
@Named(value = "procesoBean")
@RequestScoped
public class ProcesoBean {

    @Inject
    ProcesoFacade procesoFacade;

    @Setter
    @Getter
    private Proceso proceso;

    @Setter
    @Getter
    private List<Proceso> listaProceso;

    public ProcesoBean() {
    }

    @PostConstruct
    public void init() {
    }

    public void registraActividad(String actividad) {
        proceso = new Proceso();
        proceso.setActividad(actividad);
        proceso.setIp(JsfUtil.getIPAddress());
        procesoFacade.create(proceso);
    }

    public List<Proceso> getTodoProceso() {
        return procesoFacade.findAll();
    }

    public void registraActividad(String actividad, String valorAnterior, String valorNuevo) {
        proceso = new Proceso();
        proceso.setActividad(actividad);
        proceso.setIp(JsfUtil.getIPAddress());

        procesoFacade.create(proceso);

    }

    public void getLista() {
        listaProceso = procesoFacade.findAll();
    }

    public void getListaProcesoPorUsuario(String usuario) {
        listaProceso = procesoFacade.getProcesoPorUsuario(usuario);
    }

    public void getListaProcesoFechas(Date fechaInicio, Date fechaFin) {
        listaProceso = procesoFacade.getProcesoPorUsuario(fechaInicio, fechaFin);
    }
}
