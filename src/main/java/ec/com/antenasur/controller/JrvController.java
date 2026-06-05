package ec.com.antenasur.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.dto.CatalogoGeneralDTO;
import ec.com.antenasur.dto.IglesiaDTO;
import ec.com.antenasur.dto.MiembroJRVDTO;
import ec.com.antenasur.dto.PadronDTO;
import ec.com.antenasur.dto.ProcesoElectoralDTO;
import ec.com.antenasur.dto.RecintoDTO;
import ec.com.antenasur.dto.MesaDTO;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.service.tec.CatalogoGeneralService;
import ec.com.antenasur.service.tec.MesaService;
import ec.com.antenasur.service.tec.MiembroJRVService;
import ec.com.antenasur.service.tec.PadronService;
import ec.com.antenasur.service.tec.ProcesoElectoralService;
import ec.com.antenasur.service.tec.RecintoService;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Named
@ViewScoped
@Slf4j
public class JrvController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String CATALOGO_AUTORIDADES_MESA = "CARGO AUTORIDADES MESA";
    private static final String[] DIGNIDADES_OBLIGATORIAS = {"PRESIDENTE", "SECRETARIO", "TESORERO", "VOCAL"};

    @Inject
    private LoginBean loginBean;

    @Inject
    private MiembroJRVService mjrvService;

    @Inject
    private CatalogoGeneralService catalogoService;

    @Inject
    private ProcesoElectoralService procesoElectoralService;

    @Inject
    private RecintoService recintoService;

    @Inject
    private MesaService mesaService;

    @Inject
    private PadronService padronService;

    @Getter
    @Setter
    private ProcesoElectoralDTO procesoSeleccionado;

    @Getter
    @Setter
    private RecintoDTO recintoSeleccionado;

    @Getter
    @Setter
    private MesaDTO mesaSeleccionada;

    @Getter
    @Setter
    private Integer iglesiaPersonaSeleccionadaId;

    @Getter
    @Setter
    private Integer cargoSeleccionadoId;

    @Getter
    @Setter
    private MiembroJRVDTO mjrvSeleccionado;

    @Getter
    @Setter
    private List<ProcesoElectoralDTO> procesos;

    @Getter
    @Setter
    private List<RecintoDTO> recintos;

    @Getter
    @Setter
    private List<MesaDTO> mesas;

    @Getter
    @Setter
    private List<PadronDTO> personasDisponibles;

    @Getter
    @Setter
    private List<IglesiaDTO> iglesiasAsignadas;

    @Getter
    @Setter
    private List<MiembroJRVDTO> listaMJRV, listaMJRVSeleccionados;

    @Getter
    @Setter
    private List<CatalogoGeneralDTO> cargos;

    @Getter
    private boolean restringidoAMesaAsignada;

    @Getter
    private boolean juntaRegistradaComoCompletada;

    @PostConstruct
    private void init() {
        procesoSeleccionado = procesoElectoralService.getActivoDTO();
        if (procesoSeleccionado == null) {
            procesoSeleccionado = new ProcesoElectoralDTO();
            procesos = new ArrayList<>();
            JsfUtil.addWarningMessageFromBundle("mjrv.mensaje.sin.proceso.activo");
        } else {
            procesos = new ArrayList<>(Collections.singletonList(procesoSeleccionado));
        }
        recintos = recintoService.listarDTOs();
        mesas = new ArrayList<>();
        personasDisponibles = new ArrayList<>();
        iglesiasAsignadas = new ArrayList<>();
        listaMJRV = new ArrayList<>();
        cargos = catalogoService.listarDTOsPorNombrePadre(CATALOGO_AUTORIDADES_MESA);
        recintoSeleccionado = new RecintoDTO();
        mesaSeleccionada = new MesaDTO();
        aplicarRestriccionMesaAsignada();
    }

    public void onProcesoChange() {
        limpiarSeleccionMesa();
        aplicarRestriccionMesaAsignada();
    }

    public void onRecintoChange() {
        limpiarMesaYJunta();
        if (recintoSeleccionado == null || recintoSeleccionado.getId() == null) {
            return;
        }
        List<Integer> recintoIds = new ArrayList<>();
        recintoIds.add(recintoSeleccionado.getId());
        mesas = filtrarMesasPorRecintoIds(mesaService.listarDTOs(), recintoIds);
    }

    public void onMesaChange() {
        limpiarJuntaActual();
        if (mesaSeleccionada == null || mesaSeleccionada.getId() == null) {
            return;
        }
        mesaSeleccionada = mesaService.obtenerDTOPorId(mesaSeleccionada.getId());
        cargarDatosMesaSeleccionada();
    }

    public void designarMiembro() {
        try {
            Integer procesoId = procesoSeleccionado != null ? procesoSeleccionado.getId() : null;
            Integer mesaId = mesaSeleccionada != null ? mesaSeleccionada.getId() : null;
            mjrvService.designarMiembro(iglesiaPersonaSeleccionadaId, mesaId, procesoId, cargoSeleccionadoId);
            JsfUtil.addSuccessMessageFromBundle("mjrv.mensaje.asignado");
            cargarDatosMesaSeleccionada();
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Error al designar miembro JRV", e);
            JsfUtil.addErrorMessageFromBundle("mjrv.mensaje.error");
        }
    }

    public void quitarMiembro(MiembroJRVDTO miembro) {
        if (miembro == null || miembro.getId() == null) {
            return;
        }
        try {
            if (mjrvService.eliminarPorId(miembro.getId()) != null) {
                JsfUtil.addSuccessMessageFromBundle("mjrv.mensaje.retirado");
            }
            cargarDatosMesaSeleccionada();
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Error al retirar miembro JRV", e);
            JsfUtil.addErrorMessageFromBundle("mjrv.mensaje.error");
        }
    }

    public void seleccionarPersona(PadronDTO padron) {
        if (padron != null && padron.getIglesiaPersona() != null) {
            iglesiaPersonaSeleccionadaId = padron.getIglesiaPersona().getId();
        }
    }

    public void completarJunta() {
        try {
            Integer procesoId = procesoSeleccionado != null ? procesoSeleccionado.getId() : null;
            Integer mesaId = mesaSeleccionada != null ? mesaSeleccionada.getId() : null;
            MiembroJRVDTO presidente = mjrvService.completarJunta(mesaId, procesoId);
            if (presidente != null) {
                JsfUtil.addSuccessMessageFromBundle("mjrv.mensaje.completada");
            }
            mesaSeleccionada = mesaService.obtenerDTOPorId(mesaId);
            cargarDatosMesaSeleccionada();
        } catch (NegocioException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Error al completar junta JRV", e);
            JsfUtil.addErrorMessageFromBundle("mjrv.mensaje.error");
        }
    }

    public boolean isEdicionBloqueada() {
        return juntaRegistradaComoCompletada;
    }

    public boolean isPuedeCompletarJunta() {
        return isMesaSeleccionadaValida() && isJuntaCompleta() && !juntaRegistradaComoCompletada;
    }

    public int getTotalIglesiasAsignadas() {
        return iglesiasAsignadas != null ? iglesiasAsignadas.size() : 0;
    }

    public int getTotalDignidadesObligatoriasAsignadas() {
        int total = 0;
        for (String obligatoria : DIGNIDADES_OBLIGATORIAS) {
            if (contieneDignidadAsignada(obligatoria)) {
                total++;
            }
        }
        return total;
    }

    public String getIndicadorDignidadesObligatorias() {
        return getTotalDignidadesObligatoriasAsignadas() + " de " + DIGNIDADES_OBLIGATORIAS.length;
    }

    public String getEstadoJuntaTexto() {
        if (juntaRegistradaComoCompletada) {
            return JsfUtil.getProperty("mjrv.estado.completa", true);
        }
        if (isJuntaCompleta()) {
            return JsfUtil.getProperty("mjrv.estado.lista.completar", true);
        }
        if (getTotalMiembrosDesignados() > 0) {
            return JsfUtil.getProperty("mjrv.estado.incompleta", true);
        }
        return JsfUtil.getProperty("mjrv.estado.pendiente", true);
    }

    public String getEstadoJuntaSeverity() {
        if (juntaRegistradaComoCompletada) {
            return "success";
        }
        if (isJuntaCompleta()) {
            return "info";
        }
        return getTotalMiembrosDesignados() > 0 ? "warning" : "secondary";
    }

    public String getEstadoJuntaIcon() {
        if (juntaRegistradaComoCompletada) {
            return "pi pi-check";
        }
        return isJuntaCompleta() ? "pi pi-flag" : "pi pi-clock";
    }

    public boolean isMesaSeleccionadaValida() {
        return mesaSeleccionada != null && mesaSeleccionada.getId() != null;
    }

    public int getTotalPersonasDisponibles() {
        return personasDisponibles != null ? personasDisponibles.size() : 0;
    }

    public int getTotalMiembrosDesignados() {
        return listaMJRV != null ? listaMJRV.size() : 0;
    }

    public int getTotalDignidadesPendientes() {
        return getDignidadesPendientes().size();
    }

    public List<CatalogoGeneralDTO> getDignidadesPendientes() {
        List<CatalogoGeneralDTO> resultado = new ArrayList<>();
        if (cargos == null) {
            return resultado;
        }
        Set<Integer> ocupadas = obtenerCargoIdsOcupados();
        for (CatalogoGeneralDTO cargo : cargos) {
            if (cargo != null && cargo.getId() != null && !ocupadas.contains(cargo.getId())) {
                resultado.add(cargo);
            }
        }
        return resultado;
    }

    public boolean isJuntaCompleta() {
        Set<String> cargosDesignados = new HashSet<>();
        if (listaMJRV != null) {
            for (MiembroJRVDTO miembro : listaMJRV) {
                if (miembro.getCargoNombre() != null) {
                    cargosDesignados.add(miembro.getCargoNombre().trim().toUpperCase());
                }
            }
        }
        for (String obligatoria : DIGNIDADES_OBLIGATORIAS) {
            boolean asignada = false;
            for (String cargo : cargosDesignados) {
                if (cargo.contains(obligatoria)) {
                    asignada = true;
                    break;
                }
            }
            if (!asignada) {
                return false;
            }
        }
        return true;
    }

    private void cargarDatosMesaSeleccionada() {
        limpiarJuntaActual();
        if (!isMesaSeleccionadaValida() || procesoSeleccionado == null || procesoSeleccionado.getId() == null) {
            return;
        }
        List<Integer> mesaIds = new ArrayList<>();
        mesaIds.add(mesaSeleccionada.getId());
        List<PadronDTO> padronMesa = padronService.listarDTOsPorMesaIdsYProceso(
                mesaIds, procesoSeleccionado.getId());
        listaMJRV = mjrvService.listarDTOsPorMesaProceso(mesaSeleccionada.getId(), procesoSeleccionado.getId());
        iglesiasAsignadas = obtenerIglesiasAsignadas(padronMesa);
        Set<Integer> designadas = mjrvService.obtenerIglesiaPersonaIdsDesignadas(procesoSeleccionado.getId());
        personasDisponibles = new ArrayList<>();
        for (PadronDTO padron : padronMesa) {
            if (padron.getIglesiaPersona() != null
                    && padron.getIglesiaPersona().getId() != null
                    && Boolean.TRUE.equals(padron.getIglesiaPersona().getHabilitadoPadron())
                    && !designadas.contains(padron.getIglesiaPersona().getId())) {
                personasDisponibles.add(padron);
            }
        }
        juntaRegistradaComoCompletada = mjrvService.juntaCompletadaRegistrada(
                mesaSeleccionada.getId(), procesoSeleccionado.getId());
        iglesiaPersonaSeleccionadaId = null;
        cargoSeleccionadoId = null;
    }

    private void aplicarRestriccionMesaAsignada() {
        Integer personaId = loginBean != null && loginBean.getUsuario() != null ? loginBean.getUsuario().getPersonaId() : null;
        Integer procesoId = procesoSeleccionado != null ? procesoSeleccionado.getId() : null;
        MiembroJRVDTO designacion = mjrvService.obtenerDesignacionPorPersonaProceso(personaId, procesoId);
        if (designacion == null || designacion.getMesa() == null || designacion.getMesa().getId() == null) {
            return;
        }
        restringidoAMesaAsignada = true;
        mesaSeleccionada = designacion.getMesa();
        recintoSeleccionado = mesaSeleccionada.getRecinto();
        mesas = new ArrayList<>();
        mesas.add(mesaSeleccionada);
        cargarDatosMesaSeleccionada();
    }

    private Set<Integer> obtenerCargoIdsOcupados() {
        Set<Integer> ids = new HashSet<>();
        if (listaMJRV == null) {
            return ids;
        }
        for (MiembroJRVDTO miembro : listaMJRV) {
            if (miembro.getCargoId() != null) {
                ids.add(miembro.getCargoId());
            }
        }
        return ids;
    }

    private void limpiarSeleccionMesa() {
        recintoSeleccionado = new RecintoDTO();
        mesas = new ArrayList<>();
        limpiarMesaYJunta();
    }

    private void limpiarMesaYJunta() {
        mesaSeleccionada = new MesaDTO();
        limpiarJuntaActual();
    }

    private void limpiarJuntaActual() {
        personasDisponibles = new ArrayList<>();
        listaMJRV = new ArrayList<>();
        listaMJRVSeleccionados = new ArrayList<>();
        iglesiasAsignadas = new ArrayList<>();
        juntaRegistradaComoCompletada = false;
        iglesiaPersonaSeleccionadaId = null;
        cargoSeleccionadoId = null;
    }

    private List<IglesiaDTO> obtenerIglesiasAsignadas(List<PadronDTO> padrones) {
        List<IglesiaDTO> resultado = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        if (padrones == null) {
            return resultado;
        }
        for (PadronDTO padron : padrones) {
            if (padron.getIglesiaPersona() != null
                    && padron.getIglesiaPersona().getIglesia() != null
                    && padron.getIglesiaPersona().getIglesia().getId() != null
                    && ids.add(padron.getIglesiaPersona().getIglesia().getId())) {
                resultado.add(padron.getIglesiaPersona().getIglesia());
            }
        }
        return resultado;
    }

    private boolean contieneDignidadAsignada(String dignidad) {
        if (listaMJRV == null) {
            return false;
        }
        for (MiembroJRVDTO miembro : listaMJRV) {
            if (miembro.getCargoNombre() != null
                    && miembro.getCargoNombre().trim().toUpperCase().contains(dignidad)) {
                return true;
            }
        }
        return false;
    }

    private static List<MesaDTO> filtrarMesasPorRecintoIds(List<MesaDTO> mesas, List<Integer> recintoIds) {
        List<MesaDTO> resultado = new ArrayList<>();
        if (mesas == null || recintoIds == null) {
            return resultado;
        }
        for (MesaDTO mesa : mesas) {
            if (mesa.getRecinto() != null && recintoIds.contains(mesa.getRecinto().getId())) {
                resultado.add(mesa);
            }
        }
        return resultado;
    }
}
