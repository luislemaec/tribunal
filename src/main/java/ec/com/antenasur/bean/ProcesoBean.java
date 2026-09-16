package ec.com.antenasur.bean;

import java.sql.Date;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import ec.com.antenasur.model.tec.Proceso;
import ec.com.antenasur.service.tec.ProcesoService;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Luis Lema
 */
@Named(value = "procesoBean")
@RequestScoped
public class ProcesoBean {

    private static final Logger LOG = LoggerFactory.getLogger(ProcesoBean.class);

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

    /** Registra únicamente un rechazo previo a request.login(), sin secreto alguno. */
    public void registraLoginFallido(String usuarioIntentado) {
        try {
            procesoService.registrarLoginFallidoPreautenticacion(usuarioIntentado, JsfUtil.getIPAddress());
        } catch (Exception e) {
            LOG.error("No se pudo registrar el intento de inicio de sesión fallido", e);
        }
    }

    /** Registra una solicitud pública de recuperación sin datos sensibles. */
    public void registraSolicitudRecuperacionClave(String usuarioValidado) {
        try {
            procesoService.registrarSolicitudRecuperacionClavePreautenticacion(
                    usuarioValidado, JsfUtil.getIPAddress());
        } catch (Exception e) {
            LOG.error("No se pudo registrar la solicitud de recuperación de clave", e);
        }
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
            proceso.setActividad(datos != null && !datos.isBlank() ? activity + " | " + datos : activity);
            procesoService.create(proceso);
        } catch (Exception e) {
            LOG.error("No se pudo registrar la actividad: {}", activity, e);
        }
    }
}
