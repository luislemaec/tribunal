package ec.com.antenasur.service.tec;

import java.time.Instant;
import java.util.Objects;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import ec.com.antenasur.enums.FaseElectoral;
import ec.com.antenasur.facade.tec.AccesoQrActaFacade;
import ec.com.antenasur.facade.tec.CronogramaFaseFacade;
import ec.com.antenasur.model.tec.AccesoQrActa;
import ec.com.antenasur.security.qr.ReglasAccesoQr;
import ec.com.antenasur.security.qr.ResultadoAccesoQr;
import ec.com.antenasur.security.qr.EstadoAccesoQr;
import ec.com.antenasur.util.Constantes;

@Stateless
public class ValidacionAccesoQrService {
    @Inject private AccesoQrActaFacade facade;
    @Inject private CronogramaFaseFacade cronograma;
    @Inject private MiembroJRVService juntas;
    @Inject private ProcesoElectoralService procesos;

    public ResultadoAccesoQr validar(AccesoQrActa qr, boolean sesion, Instant ahora) {
        if (qr == null) return ResultadoAccesoQr.TOKEN_INVALIDO;
        if (qr.getEstado() == EstadoAccesoQr.REVOCADO) return ResultadoAccesoQr.REVOCADO;
        if (!sesion && qr.getEstado() == EstadoAccesoQr.CANJEADO) return ResultadoAccesoQr.YA_UTILIZADO;
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
        boolean documentoValido = documento != null && Boolean.TRUE.equals(documento.getEstado())
                && ec.com.antenasur.util.RepositorioDocumentos.estaDisponible(documento.getPath())
                && qr.getDocumentoVersion() != null && qr.getDocumentoVersion() > 0
                && Objects.equals(documento.getVersion(), qr.getDocumentoVersion())
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
        ResultadoAccesoQr resultado = ReglasAccesoQr.evaluar(qr.getEstado(), sesion, ahora, qr.getVigenteDesde(), qr.getVigenteHasta(),
                faseValida ? fase.getFechaInicio().toInstant() : null,
                faseValida ? fase.getFechaFin().toInstant() : null,
                documentoValido, procesoValido, mesaValida, presidenteValido, usuarioValido,
                juntaCompleta);
        if (resultado != ResultadoAccesoQr.VALIDO)
            java.util.logging.Logger.getLogger(ValidacionAccesoQrService.class.getName()).warning(
                    "QR causa=" + resultado.name() + "; qr_id=" + qr.getId()
                    + "; documento=" + documentoValido + "; proceso=" + procesoValido
                    + "; mesa=" + mesaValida + "; presidente=" + presidenteValido
                    + "; usuario=" + usuarioValido + "; fase=" + faseValida + "; juntaCompleta=" + juntaCompleta);
        return resultado;
    }
}
