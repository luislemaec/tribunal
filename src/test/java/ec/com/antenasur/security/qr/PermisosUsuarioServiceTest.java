package ec.com.antenasur.security.qr;

import ec.com.antenasur.service.UsuarioService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import java.lang.reflect.Proxy;
import java.security.Principal;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PermisosUsuarioServiceTest {
    @Test void crudGeneralYPermisosSonExclusivosDeAdministrador() throws Exception {
        for (var tipo : java.util.List.of(UsuarioService.class, ec.com.antenasur.service.RolService.class,
                ec.com.antenasur.service.RolUsuarioService.class, ec.com.antenasur.service.MenuRolService.class)) {
            for (var metodo : tipo.getDeclaredMethods()) {
                if (!Set.of("create", "edit", "delete", "remove", "find", "findAll", "findRange", "count").contains(metodo.getName())) continue;
                assertEquals(Set.of("SITEC-Administrador"), Set.of(metodo.getAnnotation(RolesAllowed.class).value()));
            }
        }
        var generales = Set.of("crearUsuarioDesdeDTO", "crearUsuarioConRol", "actualizarUsuarioDesdeDTO",
                "actualizarUsuarioConRol", "eliminarPorId", "reactivarPorId", "restablecerContraseniaACedula",
                "listarDTOPorRoles", "findAllActiveUsuario");
        for (var metodo : UsuarioService.class.getDeclaredMethods()) {
            if (generales.contains(metodo.getName()))
                assertEquals(Set.of("SITEC-Administrador"), Set.of(metodo.getAnnotation(RolesAllowed.class).value()));
        }
        for (var tipo : java.util.List.of(ec.com.antenasur.service.RolService.class,
                ec.com.antenasur.service.RolUsuarioService.class, ec.com.antenasur.service.MenuRolService.class)) {
            assertEquals(Set.of("SITEC-Administrador"), Set.of(tipo.getAnnotation(RolesAllowed.class).value()));
        }
    }

    @Test void flujosDerivadosNoAceptanIdDeAdministradorAunqueNombreSeaManipulado() throws Exception {
        var servicio = new UsuarioService();
        var campo = UsuarioService.class.getDeclaredField("rolFacade");
        campo.setAccessible(true);
        campo.set(servicio, new ec.com.antenasur.facade.RolFacade() {
            @Override public ec.com.antenasur.model.Rol find(Integer id) {
                var rol = new ec.com.antenasur.model.Rol();
                rol.setId(id); rol.setNombre("SITEC-Administrador"); rol.setEstado(true);
                return rol;
            }
        });
        var falso = new ec.com.antenasur.model.Rol();
        falso.setId(1); falso.setNombre("SITEC-Tribunal"); falso.setEstado(true);
        assertThrows(ec.com.antenasur.exception.NegocioException.class,
                () -> servicio.provisionarUsuarioExistenteConRol(null, "correo", falso));
        assertThrows(ec.com.antenasur.exception.NegocioException.class,
                () -> servicio.provisionarUsuarioExistenteConRol(null, "correo", falso, 2));
        assertThrows(ec.com.antenasur.exception.NegocioException.class,
                () -> servicio.provisionarAdministradorIglesiaExclusivo(null, "correo", falso, 2));
        assertThrows(ec.com.antenasur.exception.NegocioException.class,
                () -> servicio.asegurarUsuarioConRol(null, falso));
        assertThrows(ec.com.antenasur.exception.NegocioException.class,
                () -> servicio.retirarRolDePersonaSiNoTieneOtrosRoles(null, falso));
    }

    @Test void dependenciasDeAsignacionConservanRolesAdministrativos() {
        var nombres = Set.of("obtenerAdminDeIglesia", "removerAdminDeIglesia",
                "obtenerUsuarioPorPersonaIncluyendoInactivos", "findUsuarioPorPersonaIncluyendoInactivos",
                "retirarRolDePersonaSiNoTieneOtrosRoles", "provisionarUsuarioExistenteConRol");
        int verificados = 0;
        for (var metodo : UsuarioService.class.getDeclaredMethods()) {
            if (!java.lang.reflect.Modifier.isPublic(metodo.getModifiers()) || !nombres.contains(metodo.getName())) continue;
            var permiso = metodo.getAnnotation(RolesAllowed.class);
            assertNotNull(permiso, metodo.getName());
            assertEquals(Set.of("SITEC-Administrador", "SITEC-Tribunal"), Set.of(permiso.value()));
            verificados++;
        }
        assertEquals(7, verificados);
    }

    @Test void cambioClaveNoAceptaQrNiIdentidadAjena() throws Exception {
        for (String principal : new String[]{"TECQR:presidente", "otroUsuario"}) {
            var servicio = new UsuarioService();
            var contexto = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{SessionContext.class},
                    (p,m,a) -> switch (m.getName()) {
                        case "getCallerPrincipal" -> (Principal) () -> principal;
                        case "isCallerInRole" -> principal.startsWith("TECQR:");
                        default -> throw new AssertionError(m.getName());
                    });
            var campo = UsuarioService.class.getDeclaredField("sessionContext");
            campo.setAccessible(true);
            campo.set(servicio, contexto);
            assertNull(servicio.cambiarContraseniaAutenticada(8, "presidente", "anterior", "nueva"));
        }
    }
}
