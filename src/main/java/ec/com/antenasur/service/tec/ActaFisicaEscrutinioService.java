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

    public Documentos obtenerVigente(Integer mesaId, Integer procesoId) {
        accesoDocumental.validar(mesaId, procesoId);
        TipoDocumento tipo = tipoDocumentoFacade.buscarActivoPorNombre(TIPO_DOCUMENTO);
        return tipo == null ? null : documentoService.buscarActivoPorMesaProcesoTipo(mesaId, procesoId, tipo.getId());
    }

    public Documentos cargar(Integer mesaId, Integer procesoId, Integer personaId,
            String usuario, String nombreOriginal, String mime, byte[] contenido) {
        validarArchivo(nombreOriginal, mime, contenido);
        Mesa mesa = validarMesaCerradaYPresidente(mesaId, procesoId, personaId);
        TipoDocumento tipo = tipoDocumentoFacade.buscarActivoPorNombre(TIPO_DOCUMENTO);
        if (tipo == null) {
            throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.actaFisica.validacion.1"));
        }
        documentoService.bloquearMesaParaVersion(mesaId);
        Documentos vigente = documentoService.buscarActivoPorMesaProcesoTipo(mesaId, procesoId, tipo.getId());
        if (vigente != null && VALIDADA.equals(vigente.getEstadoRevision())) {
            throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.actaFisica.validacion.2"));
        }

        Path archivo = null;
        try {
            documentoService.bloquearMesaParaVersion(mesaId);
            int version = documentoService.siguienteVersionMesaProcesoTipo(mesaId, procesoId, tipo.getId());
            String codigo = String.format(Locale.ROOT, "ACTA-FISICA-PE%02d-M%03d-V%02d-%s",
                    procesoId, mesaId, version, UUID.randomUUID().toString().substring(0, 8));
            archivo = RepositorioDocumentos.escribirAtomico(
                    "actas-fisicas-escrutinio/proceso-" + procesoId + "/mesa-" + mesaId,
                    codigo + ".jpg", contenido);
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
            Documentos persistido = documentoService.registrarVersionMesa(documento, mesaId,
                    procesoId, mesa.getRecinto() != null ? mesa.getRecinto().getId() : null);
            if (persistido == null || persistido.getId() == null) {
                throw new IOException("No se registr\u00f3 la evidencia del acta f\u00edsica.");
            }
            return persistido;
        } catch (Exception e) {
            sessionContext.setRollbackOnly();
            RepositorioDocumentos.eliminarSilencioso(archivo);
            throw e instanceof NegocioException ? (NegocioException) e
                    : new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.actaFisica.validacion.3"));
        }
    }

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

    public void validarActaFinal(Integer documentoId, Integer mesaId, Integer procesoId,
            ec.com.antenasur.dto.RevisionActaFinalDTO revision) {
        escrutinioService.validarResultadosFinalesDTO(documentoId, mesaId, procesoId, revision);
    }

    private Mesa validarMesaCerradaYPresidente(Integer mesaId, Integer procesoId, Integer personaId) {
        accesoDocumental.validar(mesaId, procesoId);
        Integer permitida = accesoDocumental.mesaPermitida(procesoId);
        if (permitida == null || !permitida.equals(mesaId))
            throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.error.mesa.no.autorizada"));
        if (mesaId == null || procesoId == null || personaId == null) {
            throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.actaFisica.validacion.9"));
        }
        Mesa mesa = mesaFacade.find(mesaId);
        EscrutinioCabecera cabecera = escrutinioCabeceraFacade.buscarPorMesaProceso(mesaId, procesoId);
        MiembroJRVDTO presidente = miembroJRVService.obtenerDesignacionPresidentePorPersonaProceso(personaId, procesoId);
        if (mesa == null || cabecera == null || !EstadoEscrutinio.CERRADO.equals(cabecera.getEstadoEscrutinio())
                || presidente == null || presidente.getMesa() == null || !mesaId.equals(presidente.getMesa().getId())) {
            throw new NegocioException(ec.com.antenasur.util.Constantes.getMensaje("reportesMesa.actaFisica.validacion.10"));
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
