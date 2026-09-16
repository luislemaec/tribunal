package ec.com.antenasur.service.tec;

import java.time.Instant;
import java.util.Objects;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import ec.com.antenasur.enums.FaseElectoral;
import ec.com.antenasur.facade.tec.AccesoQrActaFacade;
import ec.com.antenasur.facade.tec.CronogramaFaseFacade;
import ec.com.antenasur.model.tec.AccesoQrActa;
import ec.com.antenasur.security.qr.CausaRechazoQr;
import ec.com.antenasur.security.qr.ReglasAccesoQr;
import ec.com.antenasur.security.qr.ResultadoAccesoQr;
import ec.com.antenasur.security.qr.EstadoAccesoQr;
import ec.com.antenasur.security.qr.VentanaAccesoQr;
import ec.com.antenasur.util.Constantes;

@Stateless
public class ValidacionAccesoQrService {
    private static final java.util.logging.Logger LOG =
            java.util.logging.Logger.getLogger(ValidacionAccesoQrService.class.getName());
    @Inject private AccesoQrActaFacade facade;
    @Inject private CronogramaFaseFacade cronograma;
    @Inject private MiembroJRVService juntas;
    @Inject private ProcesoElectoralService procesos;

    public ResultadoAccesoQr validar(AccesoQrActa qr, boolean sesion, Instant ahora) {
        return diagnosticar(qr, sesion, ahora).resultado();
    }

    /** Igual que {@link #validar}, devolviendo además la causa técnica para bitácora. */
    public CausaRechazoQr diagnosticar(AccesoQrActa qr, boolean sesion, Instant ahora) {
        if (qr == null) return CausaRechazoQr.TOKEN_NO_ENCONTRADO;
        if (qr.getEstado() == EstadoAccesoQr.REVOCADO)
            return registrar(CausaRechazoQr.REVOCADO, qr, ahora, null,
                    "; revocado_en=" + texto(qr.getRevocadoEn()) + "; motivo=" + qr.getMotivoRevocacion());
        if (!sesion && qr.getEstado() == EstadoAccesoQr.CANJEADO)
            return registrar(CausaRechazoQr.YA_CANJEADO, qr, ahora, null,
                    "; canjeado_en=" + texto(qr.getCanjeadoEn()));
        var documento = facade.documento(qr.getDocumentoId());
        var activo = procesos.getActivo();
        var fase = cronograma.getFasePorTipo(qr.getProcesoId(), FaseElectoral.SUFRAGIO);
        var presidentes = facade.presidentes(qr.getMesaId(), qr.getProcesoId());
        var presidente = presidentes.size() == 1 ? presidentes.get(0) : null;
        boolean presidenteValido = presidente != null && Objects.equals(presidente.getId(), qr.getPresidenteId())
                && Boolean.TRUE.equals(presidente.getIglesiaPersona().getEstado())
                && Boolean.TRUE.equals(presidente.getIglesiaPersona().getPersona().getEstado());
        var usuarios = presidenteValido
                ? facade.usuariosPresidente(presidente.getIglesiaPersona().getPersona().getId()) : java.util.List.<ec.com.antenasur.model.Usuario>of();
        var usuario = usuarios.size() == 1 ? usuarios.get(0) : null;
        boolean usuarioValido = usuario != null && Objects.equals(usuario.getId(), qr.getUsuarioId())
                && usuario.getUsername() != null && !usuario.getUsername().isBlank()
                && facade.usernameUnico(usuario.getUsername())
                && (usuario.getUsuarioFechaExpira() == null || ahora.isBefore(usuario.getUsuarioFechaExpira().toInstant()));
        // La versión se evalúa aparte para poder distinguir en bitácora un acta
        // regenerada (QR de una versión anterior) de otros rechazos documentales.
        boolean documentoVersionValida = documento != null && qr.getDocumentoVersion() != null
                && qr.getDocumentoVersion() > 0
                && Objects.equals(documento.getVersion(), qr.getDocumentoVersion());
        boolean documentoValido = documento != null && Boolean.TRUE.equals(documento.getEstado())
                && ec.com.antenasur.util.RepositorioDocumentos.estaDisponible(documento.getPath())
                && Objects.equals(documento.getProceso().getId(), qr.getProcesoId())
                && Objects.equals(documento.getMesa().getId(), qr.getMesaId())
                && Objects.equals(documento.getRecinto().getId(), qr.getRecintoId())
                && Objects.equals(documento.getTipoDocumento().getId(), qr.getTipoDocumentoId())
                && Boolean.TRUE.equals(documento.getTipoDocumento().getEstado())
                && Constantes.TIPO_ACTA_PARCIAL_ESCRUTINIO.equals(documento.getTipoDocumento().getNombre())
                && facade.esUnicoDocumentoActivo(qr);
        boolean mesaValida = documento != null && Boolean.TRUE.equals(documento.getMesa().getEstado())
                && Boolean.TRUE.equals(documento.getMesa().getRecinto().getEstado())
                && Objects.equals(documento.getMesa().getRecinto().getId(), qr.getRecintoId());
        boolean procesoValido = activo != null && Boolean.TRUE.equals(activo.getEstado())
                && Objects.equals(activo.getId(), qr.getProcesoId());
        boolean faseValida = fase != null && Boolean.TRUE.equals(fase.getEstado())
                && fase.getFechaInicio() != null && fase.getFechaFin() != null;
        boolean juntaCompleta = juntas.consultarEstadoJunta(qr.getMesaId(), qr.getProcesoId()).isCompleta();
        // El canje se habilita al CIERRE del sufragio, no durante el sufragio.
        Instant finSufragio = faseValida ? VentanaAccesoQr.instante(fase.getFechaFin()) : null;
        CausaRechazoQr causa = ReglasAccesoQr.diagnosticar(qr.getEstado(), sesion, ahora, qr.getVigenteDesde(),
                qr.getVigenteHasta(), finSufragio, documentoVersionValida, documentoValido, procesoValido,
                mesaValida, presidenteValido, usuarioValido, juntaCompleta);
        return registrar(causa, qr, ahora, finSufragio,
                "; doc_id=" + qr.getDocumentoId()
                + "; doc_version_qr=" + qr.getDocumentoVersion()
                + "; doc_version_actual=" + (documento == null ? null : documento.getVersion())
                + "; documento=" + documentoValido + "; proceso_vigente=" + procesoValido
                + "; mesa=" + mesaValida + "; presidente=" + presidenteValido
                + "; usuario=" + usuarioValido + "; fase=" + faseValida + "; juntaCompleta=" + juntaCompleta);
    }

    /** Bitácora de la ventana evaluada. Nunca registra el token ni su hash. */
    private CausaRechazoQr registrar(CausaRechazoQr causa, AccesoQrActa qr, Instant ahora, Instant finSufragio,
            String detalle) {
        if (causa != CausaRechazoQr.VALIDO)
            LOG.warning("QR causa=" + causa.name() + "; resultado=" + causa.resultado().name()
                    + "; qr_id=" + qr.getId() + "; estado=" + qr.getEstado()
                    + "; proceso=" + qr.getProcesoId() + "; mesa=" + qr.getMesaId()
                    + "; ahora=" + texto(ahora) + "; vigente_desde=" + texto(qr.getVigenteDesde())
                    + "; vigente_hasta=" + texto(qr.getVigenteHasta())
                    + "; fin_sufragio=" + texto(finSufragio)
                    + "; vigencia_posterior_horas=" + VentanaAccesoQr.VIGENCIA_POSTERIOR.toHours()
                    + detalle);
        return causa;
    }

    private static String texto(Instant instante) {
        return instante == null ? "null" : instante.atZone(VentanaAccesoQr.ZONA).toString();
    }
}
