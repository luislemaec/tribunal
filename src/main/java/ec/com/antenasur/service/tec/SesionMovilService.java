package ec.com.antenasur.service.tec;

import java.time.Duration;
import java.time.Instant;

import jakarta.annotation.security.PermitAll;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.facade.tec.SesionMovilFacade;
import ec.com.antenasur.model.Usuario;
import ec.com.antenasur.model.tec.SesionMovil;
import ec.com.antenasur.security.qr.TokenActaQr;

/**
 * Sesiones de la App móvil (docs/api-movil.md). Tokens opacos de 32 bytes con el mismo
 * formato y hash que el QR ({@link TokenActaQr}); en BD solo se guarda su SHA-256.
 *
 * <p>Los métodos son públicos para el filtro de la API y el inicio de sesión, que se ejecutan
 * antes de existir una identidad: exigen un token o un usuario ya verificado por Elytron y no
 * devuelven datos de otros usuarios.
 */
@Stateless
@PermitAll
public class SesionMovilService {

    /** Vida del token de acceso desde su emisión. */
    public static final Duration VIGENCIA_ACCESO = Duration.ofMinutes(5);
    /** Inactividad máxima, igual que la sesión web (session-timeout de 15 minutos). */
    public static final Duration INACTIVIDAD = Duration.ofMinutes(15);
    /** Vida máxima de la sesión aunque haya actividad (jornada electoral). */
    public static final Duration VIDA_MAXIMA = Duration.ofHours(12);
    /** Evita escribir en BD en cada petición: el último uso se actualiza cada 30 segundos. */
    private static final Duration PRECISION_ULTIMO_USO = Duration.ofSeconds(30);

    public static final String MOTIVO_LOGOUT = "LOGOUT";
    public static final String MOTIVO_CAMBIO_CLAVE = "CAMBIO_CLAVE";
    public static final String MOTIVO_REFRESH_REUTILIZADO = "REFRESH_REUTILIZADO";
    public static final String MOTIVO_USUARIO_INACTIVO = "USUARIO_INACTIVO";
    public static final String MOTIVO_SIN_ROL = "SIN_ROL";

    @Inject
    private SesionMovilFacade facade;

    /** Tokens entregados a la App: el valor original solo existe en esta respuesta. */
    public record TokensEmitidos(Long sesionId, Integer usuarioId, String usuario, String accessToken,
            String refreshToken, long expiraEnSegundos, boolean soloCambioClave) {
    }

    /** Sesión válida de una petición, sin tokens. */
    public record SesionValida(Long sesionId, Integer usuarioId, String usuario, boolean soloCambioClave) {
    }

    /** Crea una sesión para un usuario que Elytron acaba de autenticar. */
    public TokensEmitidos emitir(Integer usuarioId, boolean soloCambioClave, String ip, String dispositivo) {
        Usuario usuario = facade.usuario(usuarioId);
        if (usuario == null || !Boolean.TRUE.equals(usuario.getEstado())) {
            throw new IllegalArgumentException("Usuario no disponible para emitir sesión.");
        }
        Instant ahora = Instant.now();
        String acceso = TokenActaQr.generar();
        String refresh = TokenActaQr.generar();
        SesionMovil sesion = new SesionMovil();
        sesion.setUsuarioId(usuarioId);
        sesion.setAccesoHash(TokenActaQr.hash(acceso));
        sesion.setRefreshHash(TokenActaQr.hash(refresh));
        sesion.setSoloCambioClave(soloCambioClave);
        sesion.setCreadaEn(ahora);
        sesion.setAccesoEmitidoEn(ahora);
        sesion.setUltimoUso(ahora);
        sesion.setExpiraAbsoluta(ahora.plus(VIDA_MAXIMA));
        sesion.setIp(recortar(ip, 45));
        sesion.setDispositivo(recortar(dispositivo, 120));
        facade.registrar(sesion);
        return new TokensEmitidos(sesion.getId(), usuarioId, usuario.getUsername(), acceso, refresh,
                VIGENCIA_ACCESO.toSeconds(), soloCambioClave);
    }

    /**
     * Valida el token de acceso de una petición: sesión no revocada, acceso dentro de sus 5
     * minutos, sin superar la inactividad ni la vida máxima, y usuario activo. Devuelve
     * {@code null} si no es válido. Renueva el último uso (inactividad deslizante).
     */
    public SesionValida validarAcceso(String accessToken) {
        if (!TokenActaQr.formatoValido(accessToken)) {
            return null;
        }
        SesionMovil sesion = facade.buscarPorAcceso(TokenActaQr.hash(accessToken));
        Instant ahora = Instant.now();
        if (sesion == null || ahora.isAfter(sesion.getAccesoEmitidoEn().plus(VIGENCIA_ACCESO))) {
            return null;
        }
        Usuario usuario = usuarioVigente(sesion, ahora);
        if (usuario == null) {
            return null;
        }
        if (Duration.between(sesion.getUltimoUso(), ahora).compareTo(PRECISION_ULTIMO_USO) > 0) {
            sesion.setUltimoUso(ahora);
            facade.actualizar(sesion);
        }
        return new SesionValida(sesion.getId(), usuario.getId(), usuario.getUsername(), sesion.isSoloCambioClave());
    }

    /**
     * Rota el par de tokens. Un refresh ya rotado que se presenta de nuevo indica una copia:
     * se revoca la sesión. Devuelve {@code null} si el refresh no es válido.
     */
    public TokensEmitidos renovar(String refreshToken) {
        if (!TokenActaQr.formatoValido(refreshToken)) {
            return null;
        }
        String hash = TokenActaQr.hash(refreshToken);
        Instant ahora = Instant.now();
        SesionMovil sesion = facade.bloquearPorRefresh(hash);
        if (sesion == null) {
            SesionMovil reutilizada = facade.bloquearPorRefreshAnterior(hash);
            if (reutilizada != null && reutilizada.getRevocadaEn() == null) {
                revocar(reutilizada, ahora, MOTIVO_REFRESH_REUTILIZADO);
            }
            return null;
        }
        Usuario usuario = usuarioVigente(sesion, ahora);
        if (usuario == null) {
            return null;
        }
        String acceso = TokenActaQr.generar();
        String refresh = TokenActaQr.generar();
        sesion.setRefreshAnteriorHash(sesion.getRefreshHash());
        sesion.setAccesoHash(TokenActaQr.hash(acceso));
        sesion.setRefreshHash(TokenActaQr.hash(refresh));
        sesion.setAccesoEmitidoEn(ahora);
        sesion.setUltimoUso(ahora);
        facade.actualizar(sesion);
        return new TokensEmitidos(sesion.getId(), usuario.getId(), usuario.getUsername(), acceso, refresh,
                VIGENCIA_ACCESO.toSeconds(), sesion.isSoloCambioClave());
    }

    /** Revoca una sesión (logout o usuario sin roles). */
    public void revocar(Long sesionId, String motivo) {
        SesionMovil sesion = facade.buscar(sesionId);
        if (sesion != null && sesion.getRevocadaEn() == null) {
            revocar(sesion, Instant.now(), motivo);
        }
    }

    /** Revoca todas las sesiones del usuario (cambio o restablecimiento de clave). */
    public int revocarPorUsuario(Integer usuarioId, String motivo) {
        return facade.revocarPorUsuario(usuarioId, Instant.now(), motivo);
    }

    /**
     * Usuario de una sesión vigente: no revocada, dentro de la inactividad y la vida máxima, y
     * con el usuario activo. Una sesión de un usuario desactivado se revoca.
     */
    private Usuario usuarioVigente(SesionMovil sesion, Instant ahora) {
        if (sesion.getRevocadaEn() != null || !ahora.isBefore(sesion.getExpiraAbsoluta())
                || !ahora.isBefore(sesion.getUltimoUso().plus(INACTIVIDAD))) {
            return null;
        }
        Usuario usuario = facade.usuario(sesion.getUsuarioId());
        if (usuario == null || !Boolean.TRUE.equals(usuario.getEstado())) {
            revocar(sesion, ahora, MOTIVO_USUARIO_INACTIVO);
            return null;
        }
        return usuario;
    }

    private void revocar(SesionMovil sesion, Instant ahora, String motivo) {
        sesion.setRevocadaEn(ahora);
        sesion.setMotivoRevocacion(motivo);
        facade.actualizar(sesion);
    }

    private static String recortar(String valor, int maximo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.strip().replaceAll("[\\p{Cntrl}]", " ");
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo);
    }
}
