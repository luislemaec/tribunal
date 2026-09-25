package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.EstadoJuntaDTO;
import ec.com.antenasur.dto.MesaDocumentosDTO;
import ec.com.antenasur.dto.ResumenDashboardDTO;
import ec.com.antenasur.enums.EstadoEscrutinio;
import ec.com.antenasur.facade.IglesiaFacade;
import ec.com.antenasur.facade.tec.EscrutinioCabeceraFacade;
import ec.com.antenasur.facade.tec.MesaFacade;
import ec.com.antenasur.facade.tec.PadronFacade;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.util.Constantes;
import lombok.extern.slf4j.Slf4j;

/**
 * Resumen gerencial del proceso electoral activo para el panel de control.
 *
 * <p>Reúne los indicadores con consultas agregadas —una por bloque— en lugar de
 * repetir COUNT sueltos o recorrer entidades: el panel se carga una sola vez al
 * abrir la vista y la página no consulta nada desde los getters.
 *
 * <p>Solo agrega información que el modelo ya registra. No existe un flujo de
 * validación o rechazo de actas, de modo que el avance documental se mide por
 * los documentos realmente emitidos y el estado del escrutinio por los estados
 * de {@link EstadoEscrutinio}.
 */
@Stateless
@Slf4j
@DeclareRoles({ "SITEC-Administrador", "SITEC-Tribunal" })
@RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal" })
public class DashboardResumenService {

    @Inject
    private ProcesoElectoralService procesoElectoralService;

    @Inject
    private CronogramaService cronogramaService;

    @Inject
    private MiembroJRVService miembroJRVService;

    @Inject
    private IglesiaFacade iglesiaFacade;

    @Inject
    private MesaFacade mesaFacade;

    @Inject
    private PadronFacade padronFacade;

    @Inject
    private EscrutinioCabeceraFacade escrutinioCabeceraFacade;

    /** Resumen del proceso activo; vacío si todavía no hay proceso configurado. */
    public ResumenDashboardDTO consultar() {
        ResumenDashboardDTO resumen = new ResumenDashboardDTO();
        ProcesoElectoral activo = procesoElectoralService.getActivo();
        if (activo == null || activo.getId() == null) {
            return resumen;
        }
        resumen.setProcesoNombre(activo.getNombre());
        var fase = cronogramaService.getFaseVigenteDelProcesoActivo();
        if (fase != null) {
            resumen.setFaseVigente(fase.getTitulo());
        }
        resumen.setTotalIglesias(iglesiaFacade.count());

        // Una sola consulta con mesa, recinto, documentos emitidos y miembros JRV.
        List<MesaDocumentosDTO> mesas = mesaFacade.listarResumenDocumentos(activo.getId(), null, null, null);
        cargarTotalesDeMesas(resumen, mesas);
        cargarElectores(resumen, activo.getId());
        cargarEscrutinio(resumen, activo.getId());
        cargarJuntas(resumen, activo.getId(), mesas);
        cargarAlertas(resumen);
        return resumen;
    }

    private void cargarTotalesDeMesas(ResumenDashboardDTO resumen, List<MesaDocumentosDTO> mesas) {
        Set<Integer> recintos = new LinkedHashSet<>();
        long padrones = 0;
        long actas = 0;
        long certificados = 0;
        long actasFisicas = 0;
        for (MesaDocumentosDTO mesa : mesas) {
            if (mesa.getRecintoId() != null) {
                recintos.add(mesa.getRecintoId());
            }
            if (mesa.isPadronGenerado()) {
                padrones++;
            }
            if (mesa.isActaGenerada()) {
                actas++;
            }
            if (mesa.isCertificadosGenerados()) {
                certificados++;
            }
            if (mesa.isActaFisicaCargada()) {
                actasFisicas++;
            }
        }
        resumen.setTotalMesas(mesas.size());
        resumen.setTotalRecintos(recintos.size());
        resumen.setPadronesGenerados(padrones);
        resumen.setActasParcialesGeneradas(actas);
        resumen.setCertificadosGenerados(certificados);
        resumen.setActasFisicasCargadas(actasFisicas);
    }

    /** El total de electores es la suma de la distribución por cantón: una consulta. */
    private void cargarElectores(ResumenDashboardDTO resumen, Integer procesoId) {
        Map<String, Long> porCanton = padronFacade.contarElectoresPorCanton(procesoId);
        resumen.getElectoresPorCanton().putAll(porCanton);
        resumen.setTotalElectores(porCanton.values().stream().mapToLong(Long::longValue).sum());
    }

    private void cargarEscrutinio(ResumenDashboardDTO resumen, Integer procesoId) {
        Map<EstadoEscrutinio, Long> porEstado = escrutinioCabeceraFacade.contarPorEstadoProceso(procesoId);
        porEstado.forEach((estado, cantidad) -> resumen.getMesasPorEstado().put(estado.name(), cantidad));
        resumen.setMesasCerradas(porEstado.getOrDefault(EstadoEscrutinio.CERRADO, 0L));
    }

    /** Juntas con las dignidades obligatorias designadas, en una sola consulta. */
    private void cargarJuntas(ResumenDashboardDTO resumen, Integer procesoId, List<MesaDocumentosDTO> mesas) {
        List<Integer> ids = mesas.stream().map(MesaDocumentosDTO::getMesaId).filter(java.util.Objects::nonNull)
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        Map<Integer, EstadoJuntaDTO> estados = miembroJRVService.consultarEstadosJuntas(procesoId, ids);
        resumen.setMesasConJuntaCompleta(estados.values().stream().filter(EstadoJuntaDTO::isCompleta).count());
    }

    /**
     * Pendientes operativos derivados de los indicadores ya cargados: no añade
     * consultas ni interpreta estados que el modelo no registre.
     */
    private void cargarAlertas(ResumenDashboardDTO resumen) {
        List<ResumenDashboardDTO.AlertaDashboard> alertas = new ArrayList<>();
        if (resumen.getTotalMesas() == 0) {
            alertas.add(alerta("dashboard.alerta.sinMesas", 0, "danger", "mesas"));
        }
        if (resumen.getTotalElectores() == 0 && resumen.getTotalMesas() > 0) {
            alertas.add(alerta("dashboard.alerta.sinPadron", 0, "danger", "padron"));
        }
        agregarSiHay(alertas, resumen.getMesasSinJunta(), "dashboard.alerta.juntasIncompletas", "warn", "mjrv");
        agregarSiHay(alertas, resumen.getTotalMesas() - resumen.getPadronesGenerados(),
                "dashboard.alerta.padronesPendientes", "warn", "reportesMesa");
        agregarSiHay(alertas, resumen.getTotalMesas() - resumen.getActasParcialesGeneradas(),
                "dashboard.alerta.actasPendientes", "warn", "reportesMesa");
        agregarSiHay(alertas, resumen.getActasFisicasPendientes(),
                "dashboard.alerta.actasFisicasPendientes", "warn", "reportesMesa");
        agregarSiHay(alertas, resumen.getMesasPorEstado().getOrDefault(EstadoEscrutinio.OBSERVADO.name(), 0L),
                "dashboard.alerta.mesasObservadas", "danger", "escrutinios");
        agregarSiHay(alertas, resumen.getMesasPorEstado().getOrDefault(EstadoEscrutinio.ANULADO.name(), 0L),
                "dashboard.alerta.mesasAnuladas", "danger", "escrutinios");
        resumen.setAlertas(alertas);
    }

    private void agregarSiHay(List<ResumenDashboardDTO.AlertaDashboard> alertas, long cantidad, String clave,
            String severidad, String pagina) {
        if (cantidad > 0) {
            alertas.add(alerta(clave, cantidad, severidad, pagina));
        }
    }

    private ResumenDashboardDTO.AlertaDashboard alerta(String clave, long cantidad, String severidad, String pagina) {
        return new ResumenDashboardDTO.AlertaDashboard(Constantes.getMensaje(clave), cantidad, severidad, pagina);
    }
}
