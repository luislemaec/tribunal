package ec.com.antenasur.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ec.com.antenasur.facade.RolUsuarioFacade;
import ec.com.antenasur.facade.UsuarioFacade;
import ec.com.antenasur.model.Persona;
import ec.com.antenasur.model.Rol;
import ec.com.antenasur.model.RolUsuario;
import ec.com.antenasur.model.Usuario;
import ec.com.antenasur.model.generic.EntidadBase;
import ec.com.antenasur.service.UsuarioService.AccesoRetirado;

/** Presidente retirado de su junta: pierde el rol y, si no tiene otro, la cuenta. */
class RetiroPresidenteMesaTest {

    private final Rol presidente = rol(4);
    private final Usuario usuario = new Usuario();
    private final List<RolUsuario> relaciones = new ArrayList<>();
    private final UsuarioService service = new UsuarioService();

    RetiroPresidenteMesaTest() throws Exception {
        usuario.setId(10);
        usuario.setUsername("0601234567");
        usuario.setEstado(true);
        inyectar("usuarioFacade", new UsuarioFacade() {
            @Override public Usuario findByUsuarioNameIncluyendoInactivos(String username) {
                return usuario.getUsername().equals(username) ? usuario : null;
            }
            @Override public <T extends EntidadBase> T delete(T entidad) { entidad.setEstado(false); return entidad; }
        });
        inyectar("rolUsuarioFacade", new RolUsuarioFacade() {
            @Override public List<RolUsuario> findByUsuarioIdIncluyendoInactivos(Integer id) { return relaciones; }
            @Override public <T extends EntidadBase> T delete(T entidad) { entidad.setEstado(false); return entidad; }
        });
    }

    private void inyectar(String campo, Object valor) throws Exception {
        var f = UsuarioService.class.getDeclaredField(campo);
        f.setAccessible(true);
        f.set(service, valor);
    }

    private static Rol rol(int id) {
        Rol r = new Rol();
        r.setId(id);
        return r;
    }

    private RolUsuario relacion(Rol rol) {
        RolUsuario r = new RolUsuario();
        r.setUsuario(usuario);
        r.setRol(rol);
        r.setEstado(true);
        relaciones.add(r);
        return r;
    }

    private Persona persona() {
        Persona p = new Persona();
        p.setDocumento("0601234567");
        return p;
    }

    @Test
    void soloPresidenteDesactivaLaCuenta() {
        RolUsuario rolPresidente = relacion(presidente);
        assertEquals(AccesoRetirado.USUARIO, service.retirarRolPresidenteMesa(persona(), presidente));
        assertFalse(rolPresidente.getEstado());
        assertFalse(usuario.getEstado());
    }

    @Test
    void conOtroRolConservaLaCuenta() {
        RolUsuario rolPresidente = relacion(presidente);
        RolUsuario iglesiaAdmin = relacion(rol(3));
        assertEquals(AccesoRetirado.ROL, service.retirarRolPresidenteMesa(persona(), presidente));
        assertFalse(rolPresidente.getEstado());
        assertTrue(iglesiaAdmin.getEstado());
        assertTrue(usuario.getEstado());
    }

    @Test
    void sinCuentaNoHaceNada() {
        Persona otra = new Persona();
        otra.setDocumento("0999999999");
        assertEquals(AccesoRetirado.NINGUNO, service.retirarRolPresidenteMesa(otra, presidente));
    }
}
