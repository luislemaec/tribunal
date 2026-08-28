package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.bean.ProcesoBean;
import ec.com.antenasur.dto.DocumentoDTO;
import ec.com.antenasur.dto.MesaDTO;
import ec.com.antenasur.dto.ProcesoElectoralDTO;
import ec.com.antenasur.dto.RecintoDTO;
import ec.com.antenasur.dto.ReporteMesaDTO;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.service.tec.ReporteMesaService;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Named("reporteMesaController")
@ViewScoped
@Slf4j
public class ReporteMesaController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject private ReporteMesaService reporteMesaService;
    @Inject private LoginBean loginBean;
    @Inject private ProcesoBean procesoBean;

    @Getter private List<ProcesoElectoralDTO> procesos = new ArrayList<>();
    @Getter private List<RecintoDTO> recintos = new ArrayList<>();
    @Getter private List<MesaDTO> mesas = new ArrayList<>();
    @Getter private ReporteMesaDTO reporte;

    @Getter @Setter private Integer procesoId;
    @Getter @Setter private Integer recintoId;
    @Getter @Setter private Integer mesaId;
    @Getter private boolean presidenteRestringido;

    @PostConstruct
    public void init() {
        presidenteRestringido = tieneRol("SITEC-Presidente-mesa");
        List<ProcesoElectoralDTO> disponibles = reporteMesaService.listarProcesos();
        ProcesoElectoralDTO activo = null;
        for (ProcesoElectoralDTO proceso : disponibles) {
            if (Boolean.TRUE.equals(proceso.getActivo())) {
                activo = proceso;
                break;
            }
        }
        if (presidenteRestringido) {
            if (activo != null) {
                procesos = List.of(activo);
            }
        } else {
            procesos = disponibles;
        }
        if (activo != null) {
            procesoId = activo.getId();
            cargarRecintos();
        }
    }

    public void cargarRecintos() {
        recintoId = null;
        mesaId = null;
        reporte = null;
        recintos = new ArrayList<>(reporteMesaService.listarRecintos(
                procesoId, personaId(), presidenteRestringido));
        mesas = new ArrayList<>();
        if (presidenteRestringido && recintos.size() == 1) {
            recintoId = recintos.get(0).getId();
            cargarMesas();
        }
    }

    public void cargarMesas() {
        mesaId = null;
        reporte = null;
        mesas = new ArrayList<>(reporteMesaService.listarMesas(
                procesoId, recintoId, personaId(), presidenteRestringido));
        if (presidenteRestringido && mesas.size() == 1) {
            mesaId = mesas.get(0).getId();
            consultar();
        }
    }

    public void consultar() {
        reporte = null;
        if (procesoId == null || recintoId == null || mesaId == null) {
            JsfUtil.addWarningMessage(Constantes.getMensaje("reportesMesa.error.seleccion"));
            return;
        }
        try {
            reporte = reporteMesaService.consultar(
                    procesoId, recintoId, mesaId, personaId(), presidenteRestringido);
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("ERROR CONSULTAR REPORTE DE MESA", e);
            JsfUtil.addErrorMessage(Constantes.getMensaje("reportesMesa.error.consulta"));
        }
    }

    public void generarActaParcial() {
        try {
            DocumentoDTO documento = reporteMesaService.generarActaParcial(
                    procesoId, recintoId, mesaId, personaId(), presidenteRestringido);
            consultar();
            procesoBean.okActivityRegister("GENERA ACTA PARCIAL DE ESCRUTINIO", documento.getCodigo());
            JsfUtil.addSuccessMessage(Constantes.getMensaje("reportesMesa.exito.acta", documento.getNombre()));
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("ERROR GENERAR ACTA PARCIAL", e);
            JsfUtil.addErrorMessage(Constantes.getMensaje("reportesMesa.error.generar.acta"));
        }
    }

    public void generarPadron() {
        try {
            DocumentoDTO documento = reporteMesaService.generarPadron(
                    procesoId, recintoId, mesaId, personaId(), presidenteRestringido);
            consultar();
            procesoBean.okActivityRegister("GENERA PADRON ELECTORAL DE MESA", documento.getCodigo());
            JsfUtil.addSuccessMessage(Constantes.getMensaje("reportesMesa.exito.padron", documento.getNombre()));
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("ERROR GENERAR PADRON DE MESA", e);
            JsfUtil.addErrorMessage(Constantes.getMensaje("reportesMesa.error.generar.padron"));
        }
    }

    public boolean isPuedeGenerarActaParcial() {
        return reporte != null && reporte.getCabecera() != null;
    }

    public boolean isPuedeGenerarPadron() {
        return reporte != null && reporte.getProceso() != null
                && Boolean.TRUE.equals(reporte.getProceso().getActivo())
                && reporte.getPadron() != null && !reporte.getPadron().isEmpty();
    }

    private Integer personaId() {
        return loginBean != null && loginBean.getUsuario() != null
                ? loginBean.getUsuario().getPersonaId() : null;
    }

    private boolean tieneRol(String rol) {
        return loginBean != null && loginBean.getRoles() != null && loginBean.getRoles().contains(rol);
    }
}
