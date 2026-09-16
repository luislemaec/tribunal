package ec.com.antenasur.security.qr;

import ec.com.antenasur.security.menu.*;
import ec.com.antenasur.facade.*;
import ec.com.antenasur.model.*;
import jakarta.ejb.*;
import jakarta.interceptor.InvocationContext;
import java.lang.reflect.Proxy;
import java.security.Principal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AutorizacionMenuTest {
    @Test void cuatroRolesExigenMenuYRevocacionSeAplicaEnSiguientePeticion() throws Exception {
        for (String rol : List.of("SITEC-Administrador", "SITEC-Tribunal", "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa")) {
            var servicio = preparar(rol, "usuario");
            servicio.exigirAlguna("usuarios");
            assertThrows(EJBAccessException.class, () -> servicio.exigirAlguna("permisos"));
            inyectar(servicio, "roles", new RolUsuarioFacade() {
                @Override public List<RolUsuario> findByUserNameAndRoleName2(String usuario, String prefijo) { return List.of(); }
            });
            inyectar(servicio, "peticion", new PermisosPeticion());
            assertThrows(EJBAccessException.class, () -> servicio.exigirAlguna("usuarios"));
        }
    }

    @Test void identidadQrNoPuedeConvertirElMenuNormalEnPermisos() throws Exception {
        var servicio = preparar("SITEC-Presidente-mesa", "TECQR:presidente");
        assertThrows(EJBAccessException.class, servicio::paginasActuales);
    }

    @Test void interceptorNoEjecutaCrudSinMenu() throws Exception {
        var interceptor = new AccesoPaginaInterceptor();
        inyectar(interceptor, "autorizacion", preparar("SITEC-Tribunal", "usuario"));
        var ejecutado = new boolean[1];
        var llamada = (InvocationContext) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{InvocationContext.class},
                (p,m,a) -> switch(m.getName()) {
                    case "getMethod" -> Recurso.class.getMethod("guardar");
                    case "getTarget" -> new Recurso();
                    case "proceed" -> { ejecutado[0] = true; yield null; }
                    default -> throw new AssertionError(m.getName());
                });
        assertThrows(EJBAccessException.class, () -> interceptor.verificar(llamada));
        assertFalse(ejecutado[0]);
    }

    public static class Recurso {
        @AccesoPagina("permisos") public void guardar() { }
    }

    private AutorizacionMenuService preparar(String nombreRol, String principal) throws Exception {
        var servicio = new AutorizacionMenuService();
        var contexto = (SessionContext) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{SessionContext.class},
                (p,m,a) -> switch(m.getName()) {
                    case "getCallerPrincipal" -> (Principal) () -> principal;
                    case "isCallerInRole" -> nombreRol.equals(a[0]);
                    default -> throw new AssertionError(m.getName());
                });
        inyectar(servicio, "contexto", contexto);
        inyectar(servicio, "peticion", new PermisosPeticion());
        inyectar(servicio, "roles", new RolUsuarioFacade() {
            @Override public List<RolUsuario> findByUserNameAndRoleName2(String usuario, String prefijo) {
                var rol = new Rol(); rol.setNombre(nombreRol); rol.setEstado(true); rol.setId(1);
                var relacion = new RolUsuario(); relacion.setRol(rol);
                return List.of(relacion);
            }
        });
        inyectar(servicio, "menus", new MenuFacade() {
            @Override public List<ec.com.antenasur.util.MenuVO> getMenusByrols(List<RolUsuario> roles, String mnemonic) {
                return roles.isEmpty() ? List.of() : List.of(PaginasMenuTest.menu(2, 1, "usuarios.jsf", null, true));
            }
            @Override public Menu findByMenuName(String nombre) { var raiz = new Menu(); raiz.setId(1); return raiz; }
        });
        return servicio;
    }

    private static void inyectar(Object objeto, String nombre, Object valor) throws Exception {
        var campo = objeto.getClass().getDeclaredField(nombre); campo.setAccessible(true); campo.set(objeto, valor);
    }
}
