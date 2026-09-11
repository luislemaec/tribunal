package ec.com.antenasur.service.tec;

import java.time.Instant;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import ec.com.antenasur.facade.tec.AccesoQrActaFacade;
import ec.com.antenasur.model.tec.SesionQrActa;
import ec.com.antenasur.security.qr.*;

/** Servicio local, sin endpoint publico. No autentica por si solo ante Elytron. */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class CanjeAccesoQrService {
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(CanjeAccesoQrService.class.getName());
    @jakarta.annotation.Resource private jakarta.ejb.SessionContext ejb;
    @Inject private AccesoQrActaFacade facade;
    @Inject private ValidacionAccesoQrService validacion;
    @Inject private AuditoriaAccesoQrService auditoria;
    @Inject private ec.com.antenasur.service.PasswordService passwords;

    // Entrada local preautenticacion: exige token y reglas, no crea identidad Elytron.
    @PermitAll
    public CanjeAccesoQr canjear(String token, String ip, String agente) {
        String etapa = "CONFIGURACION";
        Long qrId = null;
        try {
        if (!ConfiguracionQr.habilitado()) throw new AccesoQrException();
        etapa = "BUSCAR_Y_BLOQUEAR_TOKEN";
        var qr = TokenActaQr.formatoValido(token) ? facade.bloquearPorHash(TokenActaQr.hash(token)) : null;
        qrId = qr == null ? null : qr.getId();
        Instant ahora = Instant.now();
        etapa = "VALIDACION_ELECTORAL";
        ResultadoAccesoQr resultado = validacion.validar(qr, false, ahora);
        if (resultado != ResultadoAccesoQr.VALIDO) {
            LOG.warning("QR causa=" + resultado.name() + "; qr_id=" + qrId);
            etapa = "AUDITAR_RECHAZO";
            auditoria.rechazo(qr, ip, agente, resultado);
            return new CanjeAccesoQr(resultado, null);
        }
        String prueba = TokenActaQr.generar();
        SesionQrActa sesion = new SesionQrActa();
        sesion.setQrId(qr.getId());
        sesion.setPruebaHash(TokenActaQr.hash(prueba));
        sesion.setCreadaEn(ahora);
        sesion.setExpiraEn(qr.getVigenteHasta());
        String clave = TokenActaQr.generar();
        etapa = "HASH_PUENTE";
        sesion.setLoginHash(passwords.hashBcrypt(clave));
        sesion.setLoginHasta(ahora.plusSeconds(30));
        etapa = "INVALIDAR_PUENTES";
        facade.invalidarPuentesUsuario(qr.getUsuarioId());
        etapa = "CONSUMIR_QR";
        facade.consumir(qr, ahora);
        etapa = "INSERTAR_SESION";
        facade.registrarSesion(sesion);
        // Exito y consumo se confirman juntos; un rollback no registra un falso canje exitoso.
        etapa = "AUDITAR_CANJE";
        facade.auditar(qr, ip, agente, "TOKEN_CANJEADO");
        etapa = "CONSTRUIR_CONTEXTO";
        var usuario = facade.usuario(qr.getUsuarioId());
        var contexto = new ContextoSesionQr(prueba, usuario.getId(), usuario.getUsername(),
                qr.getProcesoId(), qr.getRecintoId(), qr.getMesaId());
        return new CanjeAccesoQr(ResultadoAccesoQr.VALIDO, prueba, clave, contexto);
        } catch (RuntimeException e) {
            LOG.warning("QR causa=" + etapa + "; qr_id=" + qrId + "; excepcion="
                    + DiagnosticoQr.tipoExcepcion(e));
            throw e;
        }
    }

    @jakarta.annotation.security.RolesAllowed(ConfiguracionQr.MARCADOR)
    public void confirmar(String prueba, String username, String ip, String agente) {
        if (!(ConfiguracionQr.PREFIJO + username).equals(ejb.getCallerPrincipal().getName())) throw new AccesoQrException();
        var sesion = TokenActaQr.formatoValido(prueba) ? facade.buscarSesion(TokenActaQr.hash(prueba)) : null;
        if (sesion == null || sesion.getRevocadaEn() != null || sesion.isLoginConfirmado()
                || sesion.getLoginHasta() == null || !Instant.now().isBefore(sesion.getLoginHasta()))
            throw new AccesoQrException();
        var qr = facade.buscar(sesion.getQrId());
        if (!facade.usuario(qr.getUsuarioId()).getUsername().equals(username)
                || validacion.validar(qr, true, Instant.now()) != ResultadoAccesoQr.VALIDO)
            throw new AccesoQrException();
        facade.confirmarSesion(sesion);
        facade.auditar(qr, ip, agente, "AUTENTICADO");
    }

    // HttpSessionListener puede ejecutarse sin principal; solo revoca con prueba interna.
    @PermitAll
    public void cerrar(String prueba, String ip, String agente, String resultado) {
        if (!TokenActaQr.formatoValido(prueba)) return;
        var sesion = facade.buscarSesion(TokenActaQr.hash(prueba));
        if (sesion == null || sesion.getRevocadaEn() != null) return;
        facade.revocarSesion(sesion);
        facade.auditar(facade.buscar(sesion.getQrId()), ip, agente,
                "ERROR_AUTENTICACION".equals(resultado) ? resultado : "SESION_CERRADA");
    }

    @RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal", ConfiguracionQr.ROL })
    public ResultadoAccesoQr validarSesion(String prueba, Integer proceso, Integer mesa) {
        String principal = ejb.getCallerPrincipal().getName();
        if (!principal.startsWith(ConfiguracionQr.PREFIJO)) return ResultadoAccesoQr.USUARIO_NO_AUTORIZADO;
        return validarSesion(prueba, proceso, mesa, principal.substring(ConfiguracionQr.PREFIJO.length()));
    }

    @RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal", ConfiguracionQr.ROL })
    public ResultadoAccesoQr validarSesion(String prueba, Integer proceso, Integer mesa, String username) {
        if (!TokenActaQr.formatoValido(prueba)) return ResultadoAccesoQr.TOKEN_INVALIDO;
        var sesion = facade.buscarSesion(TokenActaQr.hash(prueba));
        Instant ahora = Instant.now();
        if (!ConfiguracionQr.habilitado() || sesion == null || !sesion.isLoginConfirmado() || sesion.getRevocadaEn() != null
                || !ReglasAccesoQr.enVentana(ahora, sesion.getCreadaEn(), sesion.getExpiraEn()))
            return ResultadoAccesoQr.FUERA_DE_VIGENCIA;
        var qr = facade.buscar(sesion.getQrId());
        if (qr == null || !qr.getProcesoId().equals(proceso) || !qr.getMesaId().equals(mesa)
                || !facade.usuario(qr.getUsuarioId()).getUsername().equals(username))
            return ResultadoAccesoQr.TOKEN_INVALIDO;
        return validacion.validar(qr, true, ahora);
    }
}
