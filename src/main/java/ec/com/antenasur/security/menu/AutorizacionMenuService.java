package ec.com.antenasur.security.menu;

import ec.com.antenasur.facade.MenuFacade;
import ec.com.antenasur.facade.RolUsuarioFacade;
import ec.com.antenasur.util.Constantes;
import ec.com.antenasur.util.MenuVO;
import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJBAccessException;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.List;

@Stateless
@RolesAllowed({"SITEC-Administrador", "SITEC-Tribunal", "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa"})
public class AutorizacionMenuService {
    @Resource private SessionContext contexto;
    @Inject private RolUsuarioFacade roles;
    @Inject private MenuFacade menus;
    @Inject private PermisosPeticion peticion;

    public List<MenuVO> menusActuales() {
        var principal = contexto.getCallerPrincipal();
        if (principal == null || contexto.isCallerInRole("TEC-QR")
                || principal.getName().startsWith("TECQR:")) throw new EJBAccessException("seguridad.menu.denegado");
        try {
            var almacenados = peticion.obtener(principal.getName());
            if (almacenados != null) return almacenados;
        } catch (jakarta.enterprise.context.ContextNotActiveException e) {
            // Un EJB invocado sin peticion HTTP tambien debe validar contra BD.
        }
        var asignados = roles.findByUserNameAndRoleName2(principal.getName(), "SITEC-%");
        if (asignados == null) return List.of();
        var efectivos = asignados.stream().filter(ru -> ru.getRol() != null
                && Boolean.TRUE.equals(ru.getRol().getEstado())
                && contexto.isCallerInRole(ru.getRol().getNombre())).toList();
        String mnemonic = Constantes.getMensaje("roles.mnemonic");
        var resultado = menus.getMenusByrols(efectivos, mnemonic);
        if (resultado == null) throw new EJBAccessException("seguridad.menu.no.disponible");
        resultado = PaginasMenu.conectados(resultado, menus.findByMenuName(mnemonic).getId());
        try {
            peticion.guardar(principal.getName(), resultado);
        } catch (jakarta.enterprise.context.ContextNotActiveException e) {
            // Sin cache fuera de una peticion.
        }
        return resultado;
    }

    public List<String> paginasActuales() {
        return PaginasMenu.extraer(menusActuales());
    }

    public void exigirAlguna(String... paginas) {
        var permitidas = paginasActuales();
        for (String pagina : paginas) if (PaginasMenu.permite(permitidas, pagina)) return;
        throw new EJBAccessException("seguridad.menu.denegado");
    }
}
