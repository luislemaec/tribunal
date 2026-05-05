package ec.com.antenasur.bean;

import java.sql.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import ec.com.antenasur.model.tec.Proceso;
import ec.com.antenasur.service.tec.ProcesoService;
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
    ProcesoService procesoService;

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
        procesoService.create(proceso);
    }

    public List<Proceso> getTodoProceso() {
        return procesoService.findAll();
    }

    public void registraActividad(String actividad, String valorAnterior, String valorNuevo) {
        proceso = new Proceso();
        proceso.setActividad(actividad);
        proceso.setIp(JsfUtil.getIPAddress());

        procesoService.create(proceso);

    }

    public void getLista() {
        listaProceso = procesoService.findAll();
    }

    public void getListaProcesoPorUsuario(String usuario) {
        listaProceso = procesoService.getProcesoPorUsuario(usuario);
    }

    public List<Proceso> getListaProcesoFechas(Date fechaInicio, Date fechaFin, String usuario) {
        return procesoService.getProcesoPorUsuario(fechaInicio, fechaFin, usuario);
    }

    public void okActivityRegister(String activity, String datos) {
        try {
            proceso = new Proceso(JsfUtil.getIPAddress());
            proceso.setActividad(activity);
            procesoService.create(proceso);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
