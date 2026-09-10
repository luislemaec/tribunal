package ec.com.antenasur.service.tec;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import ec.com.antenasur.dto.DependenciasMesaDTO;
import ec.com.antenasur.dto.EstadoDocumentoMesaDTO;
import ec.com.antenasur.dto.MesaDocumentosDTO;
import ec.com.antenasur.enums.EstadoEscrutinio;
import ec.com.antenasur.enums.FaseElectoral;
import ec.com.antenasur.enums.TipoDocumentoMesa;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.facade.tec.*;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.RepositorioDocumentos;

/** Una politica compartida por el menu, las descargas y los comandos. */
@Stateless
public class DisponibilidadDocumentoMesaService {
    @Inject private DependenciasDocumentoMesaFacade facade;
    @Inject private MesaFacade mesaFacade;
    @Inject private MiembroJRVService miembroJRVService;
    @Inject private CronogramaFaseFacade cronogramaFacade;
    @Inject private CategoriaVotoFacade categoriaFacade;
    @Inject private AccesoDocumentoMesaService acceso;

    public void completar(Integer procesoId, List<MesaDocumentosDTO> filas) {
        Integer permitida = acceso.mesaPermitida(procesoId);
        if (permitida != null && filas.stream().anyMatch(f -> !permitida.equals(f.getMesaId())))
            throw new NegocioException(mensaje("reportesMesa.error.mesa.no.autorizada"));
        if (filas.isEmpty()) return;
        List<Integer> ids = filas.stream().map(MesaDocumentosDTO::getMesaId).toList();
        Map<Integer, DependenciasMesaDTO> datos = cargar(procesoId, ids);
        var fase = cronogramaFacade.getFasePorTipo(procesoId, FaseElectoral.SUFRAGIO);
        boolean fecha = fase != null && Boolean.TRUE.equals(fase.getEstado()) && fase.getFechaInicio() != null;
        var categorias = categoriaFacade.getCategoriasOrdenados(procesoId);
        boolean listas = categorias.stream().anyMatch(c -> "LISTA".equals(c.getTipo()));
        boolean revisor = acceso.esRevisor();
        var tiposActivos = new HashSet<>(facade.tiposActivos());
        for (MesaDocumentosDTO fila : filas) {
            var d = datos.get(fila.getMesaId());
            fila.setPadronDocumentoId(d.getDocumentos().get(TipoDocumentoMesa.PADRON_MESA.getNombreTipo()));
            fila.setActaDocumentoId(d.getDocumentos().get(TipoDocumentoMesa.ACTA_PARCIAL.getNombreTipo()));
            fila.setCertificadosDocumentoId(d.getDocumentos().get(TipoDocumentoMesa.CERTIFICADOS_VOTACION.getNombreTipo()));
            fila.setActaFisicaDocumentoId(d.getDocumentos().get(TipoDocumentoMesa.ACTA_FISICA_ESCRUTINIO.getNombreTipo()));
            for (TipoDocumentoMesa tipo : TipoDocumentoMesa.values()) {
                String motivo = motivo(tipo, d, revisor, fecha, listas);
                if (motivo == null && tipo.getNombreTipo() != null && !tiposActivos.contains(tipo.getNombreTipo()))
                    motivo = mensaje("reportesMesa.error.tipo.documento");
                if (tipo == TipoDocumentoMesa.ACTA_PARCIAL && motivo == null
                        && (vacio(fila.getMesa()) || vacio(fila.getRecinto()) || vacio(fila.getCanton()) || vacio(fila.getParroquia()))) {
                    motivo = mensaje("reportesMesa.regla.datos");
                }
                boolean existe = tipo == TipoDocumentoMesa.DESIGNACION_MJRV
                        ? d.isJuntaCompleta() : d.getDocumentos().containsKey(tipo.getNombreTipo());
                boolean pendiente = tipo == TipoDocumentoMesa.ACTA_FISICA_ESCRUTINIO && d.isCerrada() && !existe;
                fila.getEstados().put(tipo.name(), estado(tipo.isDocumentoGenerable(), existe, motivo, pendiente));
            }
        }
    }

    public MesaDocumentosDTO validar(TipoDocumentoMesa tipo, Integer proceso, Integer recinto, Integer mesa,
            boolean generar) {
        acceso.validar(mesa, proceso);
        List<MesaDocumentosDTO> filas = mesaFacade.listarResumenDocumentos(proceso, null, null, mesa);
        if (filas.isEmpty() || recinto == null || !recinto.equals(filas.get(0).getRecintoId())) {
            throw new NegocioException(mensaje("reportesMesa.error.seleccion"));
        }
        completar(proceso, filas);
        var fila = filas.get(0);
        var estado = fila.getEstados().get(tipo.name());
        boolean permitido = generar ? estado.isPuedeGenerar() || estado.isPuedeRegenerar() : estado.isPuedeVisualizar();
        if (!permitido) throw new NegocioException(estado.getMotivoBloqueo() != null ? estado.getMotivoBloqueo()
                : mensaje("reportesMesa.error.documento.no.disponible"));
        return fila;
    }

    public void validarFinal(Integer proceso, Integer mesa) {
        acceso.exigirRevisor(mesa, proceso);
        var entidad = mesaFacade.buscarDetallePorId(mesa);
        if (entidad == null || entidad.getRecinto() == null) throw new NegocioException(mensaje("reportesMesa.error.seleccion"));
        validar(TipoDocumentoMesa.ACTA_FISICA_ESCRUTINIO, proceso, entidad.getRecinto().getId(), mesa, false);
        var d = cargar(proceso, List.of(mesa)).get(mesa);
        if (!d.isJuntaCompleta()) throw new NegocioException(motivoJunta(d, true));
        if (d.getEmpadronados() == 0) throw new NegocioException(mensaje("reportesMesa.error.sin.padron"));
    }

    private Map<Integer, DependenciasMesaDTO> cargar(Integer proceso, List<Integer> mesas) {
        Map<Integer, DependenciasMesaDTO> datos = new HashMap<>();
        mesas.forEach(id -> datos.put(id, new DependenciasMesaDTO()));
        for (Object[] r : facade.padrones(proceso, mesas)) {
            var d = datos.get((Integer) r[0]);
            d.setEmpadronados(((Number) r[1]).longValue());
            d.setHabilitados(((Number) r[2]).longValue());
            d.setPersonasCertificado(((Number) r[3]).longValue());
        }
        var juntas = miembroJRVService.consultarEstadosJuntas(proceso, mesas);
        for (Integer mesa : mesas) {
            var d = datos.get(mesa);
            d.getCargosFaltantes().addAll(juntas.get(mesa).getCargosFaltantes());
            d.setJuntaCompleta(juntas.get(mesa).isCompleta());
        }
        for (Object[] r : facade.cabeceras(proceso, mesas)) datos.get((Integer) r[0]).setCerrada(EstadoEscrutinio.CERRADO.equals(r[1]));
        for (Object[] r : facade.documentos(proceso, mesas)) {
            if (RepositorioDocumentos.estaDisponible((String) r[3])
                    && (!TipoDocumentoMesa.ACTA_FISICA_ESCRUTINIO.getNombreTipo().equals(r[1]) || "image/jpeg".equalsIgnoreCase((String) r[4])))
                datos.get((Integer) r[0]).getDocumentos().put((String) r[1], (Integer) r[2]);
        }
        return datos;
    }

    public static EstadoDocumentoMesaDTO estado(boolean generable, boolean existe, String motivo, boolean pendiente) {
        boolean permitido = motivo == null;
        return new EstadoDocumentoMesaDTO(permitido && generable && !existe, permitido && generable && existe,
                permitido && existe, !permitido && !pendiente, motivo,
                pendiente ? "PENDIENTE" : !permitido ? "BLOQUEADO" : existe ? "VISUALIZAR" : "GENERAR");
    }

    public static String motivo(TipoDocumentoMesa tipo, DependenciasMesaDTO d, boolean revisor, boolean fecha, boolean listas) {
        if (tipo == TipoDocumentoMesa.ACTA_FISICA_ESCRUTINIO) {
            if (!d.isCerrada()) return mensaje("reportesMesa.regla.actaFisica.cierre");
            return d.getDocumentos().containsKey(tipo.getNombreTipo()) ? null : mensaje("reportesMesa.regla.actaFisica.pendiente");
        }
        if (tipo == TipoDocumentoMesa.DESIGNACION_MJRV || tipo == TipoDocumentoMesa.ACTA_PARCIAL) {
            if (!d.isJuntaCompleta()) return motivoJunta(d, tipo == TipoDocumentoMesa.ACTA_PARCIAL);
        }
        if (tipo != TipoDocumentoMesa.DESIGNACION_MJRV && d.getEmpadronados() == 0) return mensaje("reportesMesa.error.sin.padron");
        if (tipo == TipoDocumentoMesa.CERTIFICADOS_VOTACION) {
            if (!revisor) return mensaje("reportesMesa.certificados.no.autorizado");
            if (d.getHabilitados() == 0) return mensaje("reportesMesa.certificados.vacio");
            if (d.getHabilitados() != d.getPersonasCertificado()) return mensaje("reportesMesa.certificados.duplicados");
        }
        if ((tipo == TipoDocumentoMesa.ACTA_PARCIAL || tipo == TipoDocumentoMesa.CERTIFICADOS_VOTACION) && !fecha)
            return mensaje("reportesMesa.certificados.sin.fecha");
        if (tipo == TipoDocumentoMesa.ACTA_PARCIAL && !listas) return mensaje("reportesMesa.regla.categorias");
        return null;
    }

    private static String motivoJunta(DependenciasMesaDTO d, boolean acta) {
        return mensaje(acta ? "reportesMesa.regla.acta.jrv" : "reportesMesa.regla.jrv")
                + (d.getCargosFaltantes().isEmpty() ? "" : " " + Constantes.getMensaje("reportesMesa.regla.cargos", String.join(", ", d.getCargosFaltantes())));
    }
    private static String mensaje(String clave) { return Constantes.getMensaje(clave); }
    private static boolean vacio(String valor) { return valor == null || valor.isBlank(); }
}
