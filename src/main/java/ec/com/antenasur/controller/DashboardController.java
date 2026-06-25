package ec.com.antenasur.controller;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import ec.com.antenasur.bean.LoginBean;
import ec.com.antenasur.bean.MesaBean;
import ec.com.antenasur.bean.RecintoBean;
import ec.com.antenasur.dto.DocumentoDTO;
import ec.com.antenasur.dto.EscrutinioCabeceraDTO;
import ec.com.antenasur.dto.IglesiaDTO;
import ec.com.antenasur.dto.IglesiaPersonaDTO;
import ec.com.antenasur.dto.MesaDTO;
import ec.com.antenasur.dto.MiembroJRVDTO;
import ec.com.antenasur.dto.PadronDTO;
import ec.com.antenasur.dto.PersonaDTO;
import ec.com.antenasur.dto.UsuarioDTO;
import ec.com.antenasur.enums.EstadoEscrutinio;
import ec.com.antenasur.service.IglesiaService;
import ec.com.antenasur.service.IglesiaPersonaService;
import ec.com.antenasur.service.UsuarioService;
import ec.com.antenasur.service.tec.DocumentoService;
import ec.com.antenasur.service.tec.EscrutinioService;
import ec.com.antenasur.service.tec.MiembroJRVService;
import ec.com.antenasur.service.tec.PadronService;
import ec.com.antenasur.service.tec.ProcesoElectoralService;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.JsfUtil;
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

    private static final long serialVersionUID = 1L;

    @Inject
    private LoginBean loginBean;

    @Inject
    private MesaBean mesaBean;

    @Inject
    private RecintoBean recintoBean;

    @Inject
    private PadronService padronService;

    @Inject
    private IglesiaService iglesiaService;

    @Inject
    private IglesiaPersonaService iglesiaPersonaService;

    @Inject
    private ProcesoElectoralService procesoElectoralService;

    @Inject
    private UsuarioService usuarioService;

    @Inject
    private MiembroJRVService miembroJRVService;

    @Inject
    private EscrutinioService escrutinioService;

    @Inject
    private DocumentoService documentoService;

    @Setter
    @Getter
    private float porcentajeMesasEscrutadas;

    @Setter
    @Getter
    private int totalPersonas, totalIglesias, totalRecintos, totalMesas;

    @Getter
    private boolean restringidoAIglesia;

    @Getter
    private boolean restringidoAPresidenteMesa;

    @Getter
    private boolean tieneIglesiaAsignada;

    @Getter
    private boolean tieneMesaAsignada;

    @Getter
    private IglesiaDTO iglesiaAsignada;

    @Getter
    private MesaDTO mesaAsignadaPresidente;

    @Getter
    private String procesoActivoNombre;

    @Getter
    private String estadoMesa;

    @Getter
    private String estadoApertura;

    @Getter
    private String estadoConteo;

    @Getter
    private String estadoCierre;

    @Getter
    private String estadoActaPdf;

    @Getter
    private String diferenciaMesaTexto;

    @Getter
    private int sufragantesAsignados;

    @Getter
    private int personasHabilitadasMesa;

    @Getter
    private int totalVotosRegistrados;

    @Getter
    private int diferenciaMesa;

    @Getter
    private boolean actaPdfGenerada;

    @Getter
    private EstadoEscrutinio estadoEscrutinioMesa;

    @Getter
    private int totalPersonasIglesia;

    @Getter
    private int personasInformacionCompleta;

    @Getter
    private int personasInformacionIncompleta;

    @Getter
    private int personasPendientesRevision;

    @Getter
    private String mesasAsignadas;

    @Getter
    private String recintosAsignados;

    @Getter
    private final List<String> alertasIglesia = new ArrayList<>();

    @Getter
    private final List<String> alertasMesa = new ArrayList<>();

    @Getter
    private final List<MiembroJRVDTO> integrantesMesa = new ArrayList<>();

    @PostConstruct
    private void init() {
        try {
            restringidoAPresidenteMesa = esUsuarioPresidenteMesa();
            if (restringidoAPresidenteMesa) {
                cargarDashboardPresidenteMesa();
                return;
            }
            restringidoAIglesia = esUsuarioIglesiaAdmin();
            if (restringidoAIglesia) {
                cargarDashboardIglesia();
                return;
            }
            cargarIndicadoresGlobales();
        } catch (Exception e) {
            log.error("ERROR AL INICIALIZAR OBJETOS", e);
        }
    }

    private void cargarDashboardPresidenteMesa() {
        reiniciarIndicadoresGlobales();
        alertasMesa.clear();
        ProcesoElectoral procesoActivo = procesoElectoralService.getActivo();
        if (procesoActivo == null || procesoActivo.getId() == null) {
            tieneMesaAsignada = false;
            alertasMesa.add(JsfUtil.getMessage("dashboard.presidente.alerta.sinProceso"));
            return;
        }
        procesoActivoNombre = procesoActivo.getNombre();

        UsuarioDTO usuarioActual = obtenerUsuarioActual();
        Integer personaId = usuarioActual != null ? usuarioActual.getPersonaId() : null;
        if (personaId == null) {
            tieneMesaAsignada = false;
            alertasMesa.add(JsfUtil.getMessage("dashboard.presidente.alerta.sinPersona"));
            return;
        }

        MiembroJRVDTO designacion = miembroJRVService.obtenerDesignacionPresidentePorPersonaProceso(personaId, procesoActivo.getId());
        if (designacion == null || designacion.getMesa() == null || !esCargoPresidenteMesa(designacion.getCargoNombre())) {
            tieneMesaAsignada = false;
            alertasMesa.add(JsfUtil.getMessage("dashboard.presidente.alerta.sinMesa"));
            return;
        }

        tieneMesaAsignada = true;
        mesaAsignadaPresidente = designacion.getMesa();
        cargarIndicadoresMesaAsignada(mesaAsignadaPresidente.getId(), procesoActivo.getId());
    }

    private void cargarIndicadoresMesaAsignada(Integer mesaId, Integer procesoId) {
        sufragantesAsignados = padronService.contarSufragantesPorMesaYProceso(mesaId, procesoId);
        personasHabilitadasMesa = sufragantesAsignados;
        EscrutinioCabeceraDTO cabecera = escrutinioService.buscarCabeceraDTO(mesaId, procesoId);
        estadoEscrutinioMesa = cabecera != null && cabecera.getEstadoEscrutinio() != null
                ? cabecera.getEstadoEscrutinio() : EstadoEscrutinio.PENDIENTE;
        totalVotosRegistrados = cabecera != null && cabecera.getTotalVotosRegistrados() != null
                ? cabecera.getTotalVotosRegistrados() : 0;
        diferenciaMesa = sufragantesAsignados - totalVotosRegistrados;
        diferenciaMesaTexto = construirTextoDiferencia(diferenciaMesa);
        estadoMesa = estadoEscrutinioMesa.name();
        estadoApertura = cabecera != null && cabecera.getFechaApertura() != null
                ? JsfUtil.getMessage("dashboard.presidente.estado.registrada")
                : JsfUtil.getMessage("dashboard.presidente.estado.pendiente");
        estadoConteo = resolverEstadoConteo();
        estadoCierre = EstadoEscrutinio.CERRADO.equals(estadoEscrutinioMesa)
                ? JsfUtil.getMessage("dashboard.presidente.estado.cerrada")
                : JsfUtil.getMessage("dashboard.presidente.estado.pendiente");
        actaPdfGenerada = existeActaPdf(mesaId);
        estadoActaPdf = actaPdfGenerada
                ? JsfUtil.getMessage("dashboard.presidente.estado.generada")
                : JsfUtil.getMessage("dashboard.presidente.estado.pendiente");
        integrantesMesa.clear();
        integrantesMesa.addAll(miembroJRVService.listarDTOsPorMesaProceso(mesaId, procesoId));
        cargarAlertasMesa();
    }

    private void cargarAlertasMesa() {
        if (sufragantesAsignados <= 0) {
            alertasMesa.add(JsfUtil.getMessage("dashboard.presidente.alerta.sinPadron"));
        }
        if (EstadoEscrutinio.PENDIENTE.equals(estadoEscrutinioMesa)) {
            alertasMesa.add(JsfUtil.getMessage("dashboard.presidente.alerta.aperturaPendiente"));
            return;
        }
        if (EstadoEscrutinio.ABIERTO.equals(estadoEscrutinioMesa)
                || EstadoEscrutinio.EN_CONTEO.equals(estadoEscrutinioMesa)) {
            alertasMesa.add(JsfUtil.getMessage("dashboard.presidente.alerta.conteoPendiente"));
        }
        if ((EstadoEscrutinio.CONTEO_REGISTRADO.equals(estadoEscrutinioMesa)
                || EstadoEscrutinio.REABIERTO.equals(estadoEscrutinioMesa)) && diferenciaMesa == 0) {
            alertasMesa.add(JsfUtil.getMessage("dashboard.presidente.alerta.listaCierre"));
        }
        if (EstadoEscrutinio.CERRADO.equals(estadoEscrutinioMesa) && !actaPdfGenerada) {
            alertasMesa.add(JsfUtil.getMessage("dashboard.presidente.alerta.actaPendiente"));
        }
        if (EstadoEscrutinio.OBSERVADO.equals(estadoEscrutinioMesa)) {
            alertasMesa.add(JsfUtil.getMessage("dashboard.presidente.alerta.observada"));
        }
        if (EstadoEscrutinio.ANULADO.equals(estadoEscrutinioMesa)) {
            alertasMesa.add(JsfUtil.getMessage("dashboard.presidente.alerta.anulada"));
        }
    }

    private void cargarIndicadoresGlobales() {
        totalPersonas = mesaBean.totalVotantes();
        totalIglesias = iglesiaService.count();
        totalMesas = mesaBean.totalMesas();
        totalRecintos = recintoBean.totalRecintos();
    }

    private void cargarDashboardIglesia() {
        reiniciarIndicadoresGlobales();
        Integer iglesiaId = obtenerIglesiaAsignadaIdActual();
        if (iglesiaId == null) {
            tieneIglesiaAsignada = false;
            alertasIglesia.add(JsfUtil.getMessage("dashboard.iglesia.alerta.sinAsignacion"));
            return;
        }

        iglesiaAsignada = iglesiaService.obtenerDTOPorId(iglesiaId);
        if (iglesiaAsignada == null) {
            tieneIglesiaAsignada = false;
            alertasIglesia.add(JsfUtil.getMessage("dashboard.iglesia.alerta.noDisponible"));
            return;
        }

        tieneIglesiaAsignada = true;
        cargarIndicadoresIglesia(iglesiaId);
        cargarAsignacionElectoral(iglesiaId);
        cargarAlertasIglesia();
    }

    private void cargarIndicadoresIglesia(Integer iglesiaId) {
        List<IglesiaPersonaDTO> miembros = iglesiaPersonaService.listarDTOsPorIglesia(iglesiaId);
        totalPersonasIglesia = miembros.size();
        personasInformacionCompleta = 0;
        personasPendientesRevision = 0;
        for (IglesiaPersonaDTO miembro : miembros) {
            if (tieneInformacionCompleta(miembro != null ? miembro.getPersona() : null)) {
                personasInformacionCompleta++;
            }
            if (!Boolean.TRUE.equals(miembro != null ? miembro.getActualizada() : null)) {
                personasPendientesRevision++;
            }
        }
        personasInformacionIncompleta = Math.max(0, totalPersonasIglesia - personasInformacionCompleta);
    }

    private void cargarAsignacionElectoral(Integer iglesiaId) {
        ProcesoElectoral procesoActivo = procesoElectoralService.getActivo();
        if (procesoActivo == null || procesoActivo.getId() == null) {
            mesasAsignadas = "";
            recintosAsignados = "";
            return;
        }
        List<PadronDTO> padrones = padronService.listarDTOsPorIglesiaYProceso(iglesiaId, procesoActivo.getId());
        Set<String> mesas = new LinkedHashSet<>();
        Set<String> recintos = new LinkedHashSet<>();
        for (PadronDTO padron : padrones) {
            if (padron.getMesa() == null) {
                continue;
            }
            if (padron.getMesa().getNombre() != null) {
                mesas.add(padron.getMesa().getNombre());
            }
            if (padron.getMesa().getRecinto() != null
                    && padron.getMesa().getRecinto().getNombre() != null) {
                recintos.add(padron.getMesa().getRecinto().getNombre());
            }
        }
        mesasAsignadas = String.join(", ", mesas);
        recintosAsignados = String.join(", ", recintos);
    }

    private void cargarAlertasIglesia() {
        if (personasInformacionIncompleta > 0) {
            alertasIglesia.add(JsfUtil.getMessage("dashboard.iglesia.alerta.personasIncompletas"));
        }
        if (personasPendientesRevision > 0) {
            alertasIglesia.add(JsfUtil.getMessage("dashboard.iglesia.alerta.revisionPendiente"));
        }
        if (mesasAsignadas == null || mesasAsignadas.isBlank()) {
            alertasIglesia.add(JsfUtil.getMessage("dashboard.iglesia.alerta.sinMesa"));
        }
    }

    private boolean tieneInformacionCompleta(PersonaDTO persona) {
        return persona != null
                && noVacio(persona.getDocumento())
                && noVacio(persona.getNombres())
                && noVacio(persona.getApellidos())
                && noVacio(persona.getSexo());
    }

    private boolean noVacio(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }

    private Integer obtenerIglesiaAsignadaIdActual() {
        UsuarioDTO usuarioActual = obtenerUsuarioActual();
        return usuarioActual != null ? usuarioActual.getIglesiaId() : null;
    }

    private UsuarioDTO obtenerUsuarioActual() {
        if (loginBean == null || loginBean.getUsuario() == null
                || loginBean.getUsuario().getId() == null) {
            return null;
        }
        return usuarioService.obtenerDTOPorId(loginBean.getUsuario().getId());
    }

    private boolean esUsuarioIglesiaAdmin() {
        if (loginBean == null || loginBean.getRoles() == null) {
            return false;
        }
        String prefijo = (String) JsfUtil.getProperty("roles.sitec", true);
        String rolIglesia = (prefijo == null ? "" : prefijo) + Constantes.getRolIglesiaAdmin();
        return loginBean.getRoles().contains(rolIglesia);
    }

    private boolean esUsuarioPresidenteMesa() {
        if (loginBean == null || loginBean.getRoles() == null) {
            return false;
        }
        String prefijo = (String) JsfUtil.getProperty("roles.sitec", true);
        String rolPresidenteMesa = (prefijo == null ? "" : prefijo) + Constantes.getRolPresidenteMesa();
        return loginBean.getRoles().contains(rolPresidenteMesa);
    }

    public boolean isDashboardRestringido() {
        return restringidoAIglesia || restringidoAPresidenteMesa;
    }

    public boolean isPuedeAbrirMesa() {
        return tieneMesaAsignada && EstadoEscrutinio.PENDIENTE.equals(estadoEscrutinioMesa);
    }

    public boolean isPuedeRegistrarConteo() {
        return tieneMesaAsignada
                && (EstadoEscrutinio.ABIERTO.equals(estadoEscrutinioMesa)
                || EstadoEscrutinio.EN_CONTEO.equals(estadoEscrutinioMesa)
                || EstadoEscrutinio.REABIERTO.equals(estadoEscrutinioMesa));
    }

    public boolean isPuedeValidarResultados() {
        return tieneMesaAsignada
                && (EstadoEscrutinio.EN_CONTEO.equals(estadoEscrutinioMesa)
                || EstadoEscrutinio.CONTEO_REGISTRADO.equals(estadoEscrutinioMesa)
                || EstadoEscrutinio.REABIERTO.equals(estadoEscrutinioMesa));
    }

    public boolean isPuedeCerrarMesa() {
        return tieneMesaAsignada
                && (EstadoEscrutinio.CONTEO_REGISTRADO.equals(estadoEscrutinioMesa)
                || EstadoEscrutinio.REABIERTO.equals(estadoEscrutinioMesa))
                && diferenciaMesa == 0
                && sufragantesAsignados > 0;
    }

    public boolean isPuedeDescargarActa() {
        return tieneMesaAsignada && EstadoEscrutinio.CERRADO.equals(estadoEscrutinioMesa) && actaPdfGenerada;
    }

    private boolean existeActaPdf(Integer mesaId) {
        List<DocumentoDTO> documentos = documentoService.listarDTOsPorEntidadYTipo(mesaId, Constantes.ACTA_ESCRUTINIO);
        for (DocumentoDTO documento : documentos) {
            try {
                if (documento != null && documento.getPath() != null && !documento.getPath().isBlank()
                        && Files.isRegularFile(Paths.get(documento.getPath()).toAbsolutePath().normalize())) {
                    return true;
                }
            } catch (Exception e) {
                log.warn("NO SE PUDO VALIDAR ACTA PDF DE MESA {} EN DASHBOARD", mesaId, e);
            }
        }
        return false;
    }

    private String resolverEstadoConteo() {
        if (EstadoEscrutinio.EN_CONTEO.equals(estadoEscrutinioMesa)) {
            return JsfUtil.getMessage("dashboard.presidente.estado.enConteo");
        }
        if (EstadoEscrutinio.CONTEO_REGISTRADO.equals(estadoEscrutinioMesa)
                || EstadoEscrutinio.CERRADO.equals(estadoEscrutinioMesa)) {
            return JsfUtil.getMessage("dashboard.presidente.estado.registrado");
        }
        return JsfUtil.getMessage("dashboard.presidente.estado.pendiente");
    }

    private String construirTextoDiferencia(int diferencia) {
        if (diferencia == 0) {
            return JsfUtil.getMessage("dashboard.presidente.diferencia.ok");
        }
        String clave = diferencia > 0
                ? "dashboard.presidente.diferencia.faltantes"
                : "dashboard.presidente.diferencia.excedentes";
        return Math.abs(diferencia) + " " + JsfUtil.getMessage(clave);
    }

    private boolean esCargoPresidenteMesa(String cargoNombre) {
        return cargoNombre != null && cargoNombre.trim().toUpperCase().contains("PRESIDENTE");
    }

    private void reiniciarIndicadoresGlobales() {
        totalPersonas = 0;
        totalIglesias = 0;
        totalMesas = 0;
        totalRecintos = 0;
    }

}
