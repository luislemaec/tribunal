import java.util.Map;
import org.wildfly.security.auth.realm.SimpleMapBackedSecurityRealm;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.NameRewriter;
import org.wildfly.security.authz.Roles;
import org.wildfly.security.evidence.PasswordGuessEvidence;
import org.wildfly.security.password.interfaces.ClearPassword;

/** Prueba aislada: identidades sinteticas, sin conectar a BD ni al servidor. */
public class VerificarIdentidadQr {
    public static void main(String[] args) throws Exception {
        java.security.Security.addProvider(org.wildfly.security.password.WildFlyElytronPasswordProvider.getInstance());
        var normal = new SimpleMapBackedSecurityRealm();
        var qr = new SimpleMapBackedSecurityRealm();
        char[] secretoPrueba = "solo-prueba-en-memoria".toCharArray();
        normal.setPasswordMap("prueba", ClearPassword.createRaw("clear", secretoPrueba));
        qr.setPasswordMap("prueba", ClearPassword.createRaw("clear", secretoPrueba));
        var builder = SecurityDomain.builder();
        builder.addRealm("normal", normal).setRoleMapper(r -> Roles.of("SITEC-Administrador")).build();
        builder.addRealm("qr", qr)
                .setRoleMapper(r -> Roles.fromSet(java.util.Set.of("SITEC-Presidente-mesa", "TEC-QR"))).build();
        builder.setDefaultRealmName("normal");
        builder.setRealmMapper((p, e) -> p.getName().startsWith("TECQR:") ? "qr" : "normal");
        builder.setPostRealmRewriter((NameRewriter) name -> name.replaceFirst("^TECQR:", ""));
        builder.setPermissionMapper((p, r) -> permission -> true);
        var domain = builder.build();
        var identidad = domain.authenticate("TECQR:prueba", new PasswordGuessEvidence(secretoPrueba));
        if (!"TECQR:prueba".equals(identidad.getPrincipal().getName()) || !identidad.getRoles().contains("TEC-QR")
                || identidad.getRoles().contains("SITEC-Administrador"))
            throw new AssertionError("Identidad QR incorrecta: " + identidad.getPrincipal());
        var habitual = domain.authenticate("prueba", new PasswordGuessEvidence(secretoPrueba));
        if (!habitual.getRoles().contains("SITEC-Administrador") || habitual.getRoles().contains("TEC-QR"))
            throw new AssertionError("El realm normal fue alterado");
        System.out.println(
                "OK: principal QR autenticado, usuario de realm real, roles restringidos y realm normal independiente");
    }
}
