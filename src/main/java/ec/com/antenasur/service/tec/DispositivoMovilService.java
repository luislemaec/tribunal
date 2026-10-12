package ec.com.antenasur.service.tec;

import java.time.Instant;
import java.util.regex.Pattern;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.facade.UsuarioFacade;
import ec.com.antenasur.facade.tec.NotificacionMovilFacade;
import ec.com.antenasur.model.Usuario;
import ec.com.antenasur.model.tec.DispositivoMovil;

/**
 * Registro de los dispositivos de la App para las notificaciones push. El usuario sale del
 * principal Elytron, nunca de la petición; un token ya registrado por otro usuario (mismo
 * teléfono, otra cuenta) se reasigna al usuario actual.
 */
@Stateless
@DeclareRoles({ "SITEC-Administrador", "SITEC-Tribunal", "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa" })
public class DispositivoMovilService {

    /** Formato de los tokens de FCM (base64 URL con «:»); acota lo que se guarda. */
    static final Pattern TOKEN = Pattern.compile("^[A-Za-z0-9:_\\-]{20,512}$");

    @Resource
    private SessionContext sessionContext;

    @Inject
    private UsuarioFacade usuarioFacade;

    @Inject
    private NotificacionMovilFacade facade;

    @RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal", "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa" })
    public void registrar(String token, String plataforma) {
        if (token == null || !TOKEN.matcher(token).matches()
                || !("android".equals(plataforma) || "ios".equals(plataforma))) {
            throw new NegocioException("El dispositivo no es válido.");
        }
        Usuario usuario = usuarioFacade.findByUsuarioName(sessionContext.getCallerPrincipal().getName());
        if (usuario == null) {
            throw new NegocioException("El dispositivo no es válido.");
        }
        Instant ahora = Instant.now();
        DispositivoMovil d = facade.buscarPorToken(token);
        if (d == null) {
            d = new DispositivoMovil();
            d.setToken(token);
            d.setRegistradoEn(ahora);
        }
        d.setUsuarioId(usuario.getId());
        d.setPlataforma(plataforma);
        d.setActualizadoEn(ahora);
        d.setActivo(true);
        facade.guardar(d);
    }

    /** Baja al cerrar sesión: solo afecta al token si pertenece al usuario actual. */
    @RolesAllowed({ "SITEC-Administrador", "SITEC-Tribunal", "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa" })
    public void darDeBaja(String token) {
        if (token == null || !TOKEN.matcher(token).matches()) {
            return;
        }
        DispositivoMovil d = facade.buscarPorToken(token);
        Usuario usuario = usuarioFacade.findByUsuarioName(sessionContext.getCallerPrincipal().getName());
        if (d != null && usuario != null && usuario.getId().equals(d.getUsuarioId())) {
            facade.desactivar(d.getId());
        }
    }
}
