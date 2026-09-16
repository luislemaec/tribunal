package ec.com.antenasur.service.tec;

import java.net.URI;
import java.time.Instant;
import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import ec.com.antenasur.enums.FaseElectoral;
import ec.com.antenasur.facade.tec.AccesoQrActaFacade;
import ec.com.antenasur.facade.tec.CronogramaFaseFacade;
import ec.com.antenasur.facade.tec.DocumentoFacade;
import ec.com.antenasur.model.tec.AccesoQrActa;
import ec.com.antenasur.security.qr.*;

/**
 * Debe participar en la misma transaccion que el nuevo documento y su archivo.
 */
@Stateless
@RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal" })
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class EmisionAccesoQrService {
    @Inject
    private AccesoQrActaFacade facade;
    @Inject
    private DocumentoFacade documentos;
    @Inject
    private CronogramaFaseFacade cronograma;
    @Inject
    private ValidacionAccesoQrService validacion;
    @Resource
    private SessionContext contexto;

    public EmisionAccesoQr registrarParaDocumento(Integer documentoId) {
        return registrarParaDocumento(documentoId, TokenActaQr.generar());
    }

    public EmisionAccesoQr registrarParaDocumento(Integer documentoId, String token) {
        if (!ConfiguracionQr.habilitado() || !TokenActaQr.formatoValido(token))
            throw new AccesoQrException();
        URI origen;
        try {
            origen = URI.create(System.getProperty("tec.qr.public.base-url", ""));
            TokenActaQr.url(origen, TokenActaQr.generar());
        } catch (IllegalArgumentException e) {
            throw new AccesoQrException();
        }
        var documento = facade.documento(documentoId);
        if (documento == null)
            throw new AccesoQrException();
        documentos.bloquearMesaParaVersion(documento.getMesa().getId());
        documentos.refrescar(documento);
        var presidentes = facade.presidentes(documento.getMesa().getId(), documento.getProceso().getId());
        if (presidentes.size() != 1)
            throw new AccesoQrException();
        var presidente = presidentes.get(0);
        var usuarios = facade.usuariosPresidente(presidente.getIglesiaPersona().getPersona().getId());
        if (usuarios.size() != 1)
            throw new AccesoQrException();
        var fase = cronograma.getFasePorTipo(documento.getProceso().getId(), FaseElectoral.SUFRAGIO);
        if (fase == null || fase.getFechaInicio() == null || fase.getFechaFin() == null)
            throw new AccesoQrException();
        Instant ahora = Instant.now();
        // El acta se canjea DESPUÉS del cierre: la ventana abre al fin del
        // SUFRAGIO y dura VentanaAccesoQr.VIGENCIA_POSTERIOR. El PDF puede
        // generarse antes; el token no se canjea antes del cierre.
        Instant vigenteDesde = VentanaAccesoQr.desde(fase.getFechaFin());
        Instant vigenteHasta = VentanaAccesoQr.hasta(fase.getFechaFin());
        AccesoQrActa qr = new AccesoQrActa();
        qr.setTokenHash(TokenActaQr.hash(token));
        qr.setDocumentoId(documento.getId());
        qr.setDocumentoVersion(documento.getVersion());
        qr.setProcesoId(documento.getProceso().getId());
        qr.setRecintoId(documento.getRecinto().getId());
        qr.setMesaId(documento.getMesa().getId());
        qr.setTipoDocumentoId(documento.getTipoDocumento().getId());
        qr.setPresidenteId(presidente.getId());
        qr.setUsuarioId(usuarios.get(0).getId());
        qr.setVigenteDesde(vigenteDesde);
        qr.setVigenteHasta(vigenteHasta);
        qr.setEmitidoEn(ahora);
        qr.setEmitidoPor(contexto.getCallerPrincipal().getName());
        qr.setEstado(EstadoAccesoQr.EMITIDO);
        // Se permite imprimir antes del cierre, pero nunca canjear antes de él:
        // la comprobación previa se hace en el instante de apertura de la ventana.
        Instant comprobacion = ahora.isBefore(qr.getVigenteDesde()) ? qr.getVigenteDesde() : ahora;
        if (validacion.validar(qr, false, comprobacion) != ResultadoAccesoQr.VALIDO)
            throw new AccesoQrException();
        facade.revocarContexto(qr.getProcesoId(), qr.getMesaId(), qr.getTipoDocumentoId(), ahora,
                "NUEVA_VERSION_DOCUMENTAL");
        facade.registrar(qr);
        facade.auditar(qr, null, null, "EMITIDO");
        return new EmisionAccesoQr(qr.getId(), TokenActaQr.url(origen, token));
    }
}
