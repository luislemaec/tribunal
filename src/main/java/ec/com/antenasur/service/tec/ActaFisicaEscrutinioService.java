package ec.com.antenasur.service.tec;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

import jakarta.ejb.Stateless;
import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;

import ec.com.antenasur.dto.MiembroJRVDTO;
import ec.com.antenasur.enums.EstadoEscrutinio;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.facade.tec.EscrutinioCabeceraFacade;
import ec.com.antenasur.facade.tec.MesaFacade;
import ec.com.antenasur.facade.tec.ProcesoElectoralFacade;
import ec.com.antenasur.facade.tec.TipoDocumentoFacade;
import ec.com.antenasur.model.tec.Documentos;
import ec.com.antenasur.model.tec.EscrutinioCabecera;
import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.model.tec.TipoDocumento;
import ec.com.antenasur.util.RepositorioDocumentos;

/** Gestiona la evidencia fotogr\u00e1fica del acta llenada manualmente. */
@Stateless
public class ActaFisicaEscrutinioService {
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(ActaFisicaEscrutinioService.class.getName());

    public static final String TIPO_DOCUMENTO = "ACTA FISICA DE ESCRUTINIO";
    public static final String PENDIENTE_REVISION = "PENDIENTE_REVISION";
    public static final String VALIDADA = "VALIDADA";
    public static final String OBSERVADA = "OBSERVADA";
    public static final String RECHAZADA = "RECHAZADA";
    private static final int TAMANIO_MAXIMO = 10 * 1024 * 1024;

    @Inject private DocumentoService documentoService;
    @Inject private TipoDocumentoFacade tipoDocumentoFacade;
    @Inject private MesaFacade mesaFacade;
    @Inject private ProcesoElectoralFacade procesoFacade;
    @Inject private EscrutinioCabeceraFacade escrutinioCabeceraFacade;
    @Inject private MiembroJRVService miembroJRVService;
    @Inject private EscrutinioService escrutinioService;
    @Inject private AccesoDocumentoMesaService accesoDocumental;
    @Inject private DisponibilidadDocumentoMesaService disponibilidadDocumental;
    @Resource(lookup = "java:comp/TransactionSynchronizationRegistry")
    private TransactionSynchronizationRegistry transacciones;
    @Resource private SessionContext sessionContext;

    @jakarta.annotation.security.RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal", "SITEC-Presidente-mesa"})
    public Documentos obtenerVigente(Integer mesaId, Integer procesoId) {
        accesoDocumental.validar(mesaId, procesoId);
        TipoDocumento tipo = tipoDocumentoFacade.buscarActivoPorNombre(TIPO_DOCUMENTO);
        return tipo == null ? null : documentoService.buscarActivoPorMesaProcesoTipo(mesaId, procesoId, tipo.getId());
    }

    /**
     * Acta física vigente de varias mesas del proceso, para el listado de escrutinios:
     * una sola consulta en lugar de una por mesa. Solo para revisores, que son quienes
     * ven el listado completo. Por mesa devuelve la versión activa más reciente del
     * proceso indicado.
     */
    @jakarta.annotation.security.RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal"})
    public java.util.Map<Integer, Documentos> obtenerVigentesPorMesas(java.util.List<Integer> mesaIds, Integer procesoId) {
        java.util.Map<Integer, Documentos> resultado = new java.util.HashMap<>();
        if (!accesoDocumental.esRevisor() || mesaIds == null || mesaIds.isEmpty() || procesoId == null) {
            return resultado;
        }
        TipoDocumento tipo = tipoDocumentoFacade.buscarActivoPorNombre(TIPO_DOCUMENTO);
        if (tipo == null) {
            return resultado;
        }
        // Cada lista llega ordenada por id descendente: la primera del proceso es la vigente.
        documentoService.getDocumentosPorEntidadesYTipoDoc(mesaIds, tipo.getId()).forEach((mesaId, documentos) -> {
            for (Documentos documento : documentos) {
                if (documento.getProceso() != null && procesoId.equals(documento.getProceso().getId())) {
                    resultado.put(mesaId, documento);
                    break;
                }
            }
        });
        return resultado;
    }

    @jakarta.annotation.security.RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal", "SITEC-Presidente-mesa"})
    public Documentos cargar(Integer mesaId, Integer procesoId, Integer personaId,
            String usuario, String nombreOriginal, String mime, byte[] contenido) {
        // Compatibilidad con llamados existentes: la identidad no procede de estos parametros.
        return cargar(mesaId, procesoId, nombreOriginal, mime, contenido);
    }

    @jakarta.annotation.security.RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal", "SITEC-Presidente-mesa"})
    public Documentos cargar(Integer mesaId, Integer procesoId, String nombreOriginal, String mime, byte[] contenido) {
        String etapa = "ALCANCE_MESA_PROCESO";
        Path archivo = null;
        try {
            accesoDocumental.validar(mesaId, procesoId);
            etapa = "IDENTIDAD_AUTENTICADA";
            Integer personaId = accesoDocumental.personaAutenticadaId();
            etapa = "ARCHIVO";
            validarArchivo(nombreOriginal, mime, contenido);
            etapa = "AUTORIA_MESA_CERRADA";
            Mesa mesa = validarMesaCerradaYAutoria(mesaId, procesoId, personaId);
            etapa = "TIPO_DOCUMENTO";
            TipoDocumento tipo = tipoDocumentoFacade.buscarActivoPorNombre(TIPO_DOCUMENTO);
            if (tipo == null) {
                throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.actaFisica.validacion.1"));
            }
            etapa = "VERSION_ACTIVA";
            documentoService.bloquearMesaParaVersion(mesaId);
            Documentos vigente = documentoService.buscarActivoPorMesaProcesoTipo(mesaId, procesoId, tipo.getId());
            if (vigente != null && VALIDADA.equals(vigente.getEstadoRevision())) {
                throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.actaFisica.validacion.2"));
            }

            etapa = "BLOQUEO_VERSION";
            documentoService.bloquearMesaParaVersion(mesaId);
            int version = documentoService.siguienteVersionMesaProcesoTipo(mesaId, procesoId, tipo.getId());
            String codigo = String.format(Locale.ROOT, "ACTA-FISICA-PE%02d-M%03d-V%02d-%s",
                    procesoId, mesaId, version, UUID.randomUUID().toString().substring(0, 8));
            etapa = "ESCRIBIR_ARCHIVO";
            archivo = RepositorioDocumentos.escribirAtomico(
                    "actas-fisicas-escrutinio/proceso-" + procesoId + "/mesa-" + mesaId,
                    codigo + ".jpg", contenido);
            etapa = "REGISTRAR_ROLLBACK";
            registrarLimpiezaAnteRollback(archivo);

            Documentos documento = new Documentos(codigo,
                    RepositorioDocumentos.rutaRelativaParaPersistir(archivo), tipo, mesaId,
                    ".jpg", "image/jpeg", codigo);
            documento.setProceso(procesoFacade.find(procesoId));
            documento.setMesa(mesa);
            documento.setRecinto(mesa.getRecinto());
            documento.setVersion(version);
            documento.setFolio(codigo);
            documento.setHashSha256(RepositorioDocumentos.sha256(contenido));
            documento.setEstadoRevision(PENDIENTE_REVISION);
            documento.setUsuarioCrea(accesoDocumental.usuarioActual());
            etapa = "PERSISTIR_DOCUMENTO";
            Documentos persistido = documentoService.registrarVersionMesa(documento, mesaId,
                    procesoId, mesa.getRecinto() != null ? mesa.getRecinto().getId() : null);
            if (persistido == null || persistido.getId() == null) {
                throw new IOException("No se registr\u00f3 la evidencia del acta f\u00edsica.");
            }
            return persistido;
        } catch (Exception e) {
            LOG.warning("ACTA_FISICA causa=" + etapa + "; mesa=" + mesaId + "; proceso=" + procesoId
                    + "; excepcion=" + ec.com.antenasur.security.qr.DiagnosticoQr.tipoExcepcion(e));
            sessionContext.setRollbackOnly();
            RepositorioDocumentos.eliminarSilencioso(archivo);
            throw e instanceof NegocioException ? (NegocioException) e
                    : new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.actaFisica.validacion.3"));
        }
    }

    @jakarta.annotation.security.RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal"})
    public Documentos revisar(Integer documentoId, String estadoRevision, String observacion, String usuario) {
        if (!accesoDocumental.esRevisor()) {
            throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.actaFisica.validacion.4"));
        }
        // VALIDADA solo puede alcanzarse junto con resultados completos y cuadrados.
        if (!OBSERVADA.equals(estadoRevision) && !RECHAZADA.equals(estadoRevision)) {
            throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.actaFisica.validacion.5"));
        }
        if ((OBSERVADA.equals(estadoRevision) || RECHAZADA.equals(estadoRevision))
                && (observacion == null || observacion.isBlank())) {
            throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.actaFisica.validacion.6"));
        }
        Documentos documento = documentoService.obtenerEntidad(documentoId);
        if (documento == null || documento.getTipoDocumento() == null
                || !TIPO_DOCUMENTO.equalsIgnoreCase(documento.getTipoDocumento().getNombre())
                || !Boolean.TRUE.equals(documento.getEstado())) {
            throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.actaFisica.validacion.7"));
        }
        if (documento.getMesa() == null || documento.getProceso() == null)
            throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.regla.evidencia"));
        documentoService.bloquearMesaParaVersion(documento.getMesa().getId());
        documentoService.refrescar(documento);
        disponibilidadDocumental.validarFinal(documento.getProceso().getId(), documento.getMesa().getId());
        var vigenteRevision = obtenerVigente(documento.getMesa().getId(), documento.getProceso().getId());
        if (!Boolean.TRUE.equals(documento.getEstado()) || VALIDADA.equals(documento.getEstadoRevision())
                || vigenteRevision == null || !documento.getId().equals(vigenteRevision.getId()))
            throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.regla.evidencia"));
        documentoService.validarContextoMesa(documento, documento.getMesa().getId(), documento.getProceso().getId(),
                documento.getMesa().getRecinto().getId());
        usuario = accesoDocumental.usuarioActual();
        documento.setEstadoRevision(estadoRevision);
        documento.setObservacionRevision(observacion == null ? null : observacion.trim());
        documento.setUsuarioRevision(usuario);
        documento.setFechaRevision(new java.util.Date());
        documento.setUsuarioActualiza(usuario);
        return documentoService.actualizar(documento);
    }

    @jakarta.annotation.security.RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal"})
    public void validarActaFinal(Integer documentoId, Integer mesaId, Integer procesoId,
            ec.com.antenasur.dto.RevisionActaFinalDTO revision) {
        escrutinioService.validarResultadosFinalesDTO(documentoId, mesaId, procesoId, revision);
    }

    /** Marca como datos oficiales lo registrado, cuando el acta física coincide. */
    @jakarta.annotation.security.RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal"})
    public void validarDatosRegistrados(Integer documentoId, Integer mesaId, Integer procesoId) {
        try {
            escrutinioService.validarDatosRegistradosDTO(documentoId, mesaId, procesoId);
        } catch (RuntimeException e) {
            sessionContext.setRollbackOnly();
            throw e;
        }
    }

    /**
     * Devuelve la mesa al presidente cuando el acta física no concuerda: marca el acta
     * vigente OBSERVADA o RECHAZADA con el motivo (mismas reglas que {@link #revisar}) y
     * reabre el escrutinio, todo en una transacción. La mesa debe estar CERRADA.
     */
    @jakarta.annotation.security.RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal"})
    public void devolverActaFisica(Integer documentoId, String estadoRevision, String motivo) {
        try {
            Documentos documento = documentoService.obtenerEntidad(documentoId);
            if (documento == null || documento.getMesa() == null || documento.getProceso() == null) {
                throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.regla.evidencia"));
            }
            Integer mesaId = documento.getMesa().getId();
            Integer procesoId = documento.getProceso().getId();
            exigirMesaCerrada(mesaId, procesoId);
            revisar(documentoId, estadoRevision, motivo, null);
            escrutinioService.cambiarEstadoCabeceraDTO(mesaId, procesoId, EstadoEscrutinio.REABIERTO, motivo.trim());
        } catch (RuntimeException e) {
            // NegocioException no revierte por sí sola (rollback=false): sin esto el acta
            // podría quedar observada con la mesa aún cerrada.
            sessionContext.setRollbackOnly();
            throw e;
        }
    }

    /**
     * Revierte la validación (dato oficial) del acta física vigente: el acta pasa a
     * OBSERVADA con el motivo, registrando quién y cuándo, y el escrutinio se reabre
     * para que el presidente corrija, en una sola transacción. Los cambios quedan en el
     * historial Envers de documentos y escrutinio_cabecera.
     */
    @jakarta.annotation.security.RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal"})
    public void revertirValidacion(Integer documentoId, String motivo) {
        try {
            if (motivo == null || motivo.isBlank()) {
                throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.actaFisica.validacion.6"));
            }
            Documentos documento = documentoService.obtenerEntidad(documentoId);
            if (documento == null || documento.getTipoDocumento() == null
                    || !TIPO_DOCUMENTO.equalsIgnoreCase(documento.getTipoDocumento().getNombre())
                    || !Boolean.TRUE.equals(documento.getEstado())
                    || documento.getMesa() == null || documento.getProceso() == null) {
                throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.regla.evidencia"));
            }
            Integer mesaId = documento.getMesa().getId();
            Integer procesoId = documento.getProceso().getId();
            accesoDocumental.exigirRevisor(mesaId, procesoId);
            documentoService.bloquearMesaParaVersion(mesaId);
            documentoService.refrescar(documento);
            Documentos vigente = obtenerVigente(mesaId, procesoId);
            if (!Boolean.TRUE.equals(documento.getEstado()) || !VALIDADA.equals(documento.getEstadoRevision())
                    || vigente == null || !documento.getId().equals(vigente.getId())) {
                throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("actaE.oficial.revertir.no.validada"));
            }
            exigirMesaCerrada(mesaId, procesoId);
            String usuario = accesoDocumental.usuarioActual();
            documento.setEstadoRevision(OBSERVADA);
            documento.setObservacionRevision(motivo.trim());
            documento.setUsuarioRevision(usuario);
            documento.setFechaRevision(new java.util.Date());
            documento.setUsuarioActualiza(usuario);
            documentoService.actualizar(documento);
            escrutinioService.cambiarEstadoCabeceraDTO(mesaId, procesoId, EstadoEscrutinio.REABIERTO, motivo.trim());
            // La validación la marcó COMPLETADO; al reabrirse vuelve a estar en curso y deja
            // de contar como mesa escrutada en los reportes que usan estadoTarea.
            Mesa mesa = mesaFacade.find(mesaId);
            if (mesa != null) {
                mesa.setEstadoTarea(ec.com.antenasur.enums.EstadoTarea.INICIADA);
                mesa.setUsuarioActualiza(usuario);
                mesaFacade.edit(mesa);
            }
        } catch (RuntimeException e) {
            sessionContext.setRollbackOnly();
            throw e;
        }
    }

    private void exigirMesaCerrada(Integer mesaId, Integer procesoId) {
        EscrutinioCabecera cabecera = escrutinioCabeceraFacade.buscarPorMesaProceso(mesaId, procesoId);
        if (cabecera == null || !EstadoEscrutinio.CERRADO.equals(cabecera.getEstadoEscrutinio())) {
            throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.regla.actaFisica.cierre"));
        }
    }

    /**
     * Quién puede subir el acta física y cuándo.
     *
     * <ul>
     *   <li><b>Presidente de mesa</b>: solo su mesa designada, como hasta ahora.</li>
     *   <li><b>Revisores</b> (Administrador y Tribunal): cualquier mesa. Para ellos
     *       {@link AccesoDocumentoMesaService#mesaPermitida} devuelve {@code null}, lo que
     *       solo ocurre si el usuario no tiene además el rol de presidente; quien lo
     *       tenga conserva la restricción a su mesa aunque sume otros roles.</li>
     * </ul>
     *
     * <p>En ambos casos la mesa debe estar CERRADA: el acta física documenta un
     * escrutinio ya concluido. El reemplazo de un acta VALIDADA sigue bloqueado en
     * {@link #cargar}, y cada carga registra la versión y el usuario real que la subió.</p>
     */
    private Mesa validarMesaCerradaYAutoria(Integer mesaId, Integer procesoId, Integer personaId) {
        accesoDocumental.validar(mesaId, procesoId);
        Integer permitida = accesoDocumental.mesaPermitida(procesoId);
        boolean revisor = permitida == null && accesoDocumental.esRevisor();
        if (!revisor && (permitida == null || !permitida.equals(mesaId)))
            throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.error.mesa.no.autorizada"));
        if (mesaId == null || procesoId == null || personaId == null) {
            throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.actaFisica.validacion.9"));
        }
        Mesa mesa = mesaFacade.find(mesaId);
        EscrutinioCabecera cabecera = escrutinioCabeceraFacade.buscarPorMesaProceso(mesaId, procesoId);
        if (mesa == null || cabecera == null || !EstadoEscrutinio.CERRADO.equals(cabecera.getEstadoEscrutinio())) {
            throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.actaFisica.validacion.10"));
        }
        if (!revisor) {
            MiembroJRVDTO presidente = miembroJRVService.obtenerDesignacionPresidentePorPersonaProceso(personaId, procesoId);
            if (presidente == null || presidente.getMesa() == null || !mesaId.equals(presidente.getMesa().getId())) {
                throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.actaFisica.validacion.10"));
            }
        }
        return mesa;
    }

    private void validarArchivo(String nombreOriginal, String mime, byte[] contenido) {
        String nombre = nombreOriginal == null ? "" : nombreOriginal.toLowerCase(Locale.ROOT);
        if (contenido == null || contenido.length == 0 || contenido.length > TAMANIO_MAXIMO
                || (!nombre.endsWith(".jpg") && !nombre.endsWith(".jpeg"))
                || (mime != null && !mime.isBlank() && !"image/jpeg".equalsIgnoreCase(mime))
                || contenido.length < 3 || (contenido[0] & 0xFF) != 0xFF
                || (contenido[1] & 0xFF) != 0xD8 || (contenido[2] & 0xFF) != 0xFF) {
            throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.actaFisica.validacion.11"));
        }
    }

    private void registrarLimpiezaAnteRollback(Path archivo) {
        transacciones.registerInterposedSynchronization(new Synchronization() {
            @Override public void beforeCompletion() { }
            @Override public void afterCompletion(int estado) {
                if (estado == Status.STATUS_ROLLEDBACK) {
                    RepositorioDocumentos.eliminarSilencioso(archivo);
                }
            }
        });
    }
}
