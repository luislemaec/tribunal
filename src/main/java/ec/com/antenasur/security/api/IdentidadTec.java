package ec.com.antenasur.security.api;

import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.auth.server.ServerAuthenticationContext;
import org.wildfly.security.evidence.PasswordGuessEvidence;

/**
 * Identidad Elytron para la API móvil, construida con el mismo dominio de la web
 * ({@code TribunalSecurityDomain}, que WildFly asocia al despliegue): mismo realm, misma
 * verificación BCrypt y mismos roles. Ejecutar una petición con {@link SecurityIdentity#runAs}
 * hace que los EJB ({@code @RolesAllowed}, {@code SessionContext}) vean al usuario igual que
 * tras un {@code request.login} de la web. No requiere cambios en standalone.xml.
 *
 * <p>Nunca registra ni conserva la clave: la evidencia se destruye tras verificarla.
 */
public final class IdentidadTec {

    private IdentidadTec() {
    }

    /**
     * Verifica usuario y clave contra el realm de TEC. Devuelve la identidad autorizada o
     * {@code null} si el usuario no existe, la clave no coincide o no tiene permiso de login.
     */
    public static SecurityIdentity autenticar(String usuario, char[] clave) throws RealmUnavailableException {
        try (ServerAuthenticationContext contexto = dominio().createNewAuthenticationContext()) {
            contexto.setAuthenticationName(usuario);
            if (!contexto.exists()) {
                contexto.fail();
                return null;
            }
            PasswordGuessEvidence evidencia = new PasswordGuessEvidence(clave);
            try {
                if (!contexto.verifyEvidence(evidencia) || !contexto.authorize()) {
                    contexto.fail();
                    return null;
                }
            } finally {
                evidencia.destroy();
            }
            contexto.succeed();
            return contexto.getAuthorizedIdentity();
        }
    }

    /**
     * Identidad de un usuario cuya sesión ya fue validada por token: el realm vuelve a cargar
     * sus roles vigentes en cada petición, así que un cambio de roles aplica de inmediato.
     * Devuelve {@code null} si el usuario ya no existe en el realm.
     */
    public static SecurityIdentity identidadDe(String usuario) throws RealmUnavailableException {
        try (ServerAuthenticationContext contexto = dominio().createNewAuthenticationContext()) {
            contexto.setAuthenticationName(usuario);
            if (!contexto.exists() || !contexto.authorize()) {
                contexto.fail();
                return null;
            }
            contexto.succeed();
            return contexto.getAuthorizedIdentity();
        }
    }

    private static SecurityDomain dominio() {
        SecurityDomain dominio = SecurityDomain.getCurrent();
        if (dominio == null) {
            throw new IllegalStateException("El despliegue no tiene un dominio de seguridad Elytron asociado.");
        }
        return dominio;
    }
}
