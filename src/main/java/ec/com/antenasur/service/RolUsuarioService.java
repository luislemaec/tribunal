package ec.com.antenasur.service;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import ec.com.antenasur.dto.RolUsuarioDTO;
import ec.com.antenasur.facade.RolUsuarioFacade;
import ec.com.antenasur.model.Rol;
import ec.com.antenasur.model.RolUsuario;
import ec.com.antenasur.model.Usuario;

@Stateless
public class RolUsuarioService extends AbstractService<RolUsuario, Integer, RolUsuarioFacade> {

    @Inject
    private RolUsuarioFacade rolUsuarioFacade;

    @Override
    protected RolUsuarioFacade getFacade() {
        return rolUsuarioFacade;
    }

    public List<RolUsuario> findByUserNameAndRoleName2(String userName, String roleName) {
        return rolUsuarioFacade.findByUserNameAndRoleName2(userName, roleName);
    }

    public List<RolUsuario> findByUserNameAndRoleName_(String userName) {
        return rolUsuarioFacade.findByUserNameAndRoleName_(userName);
    }

    public List<RolUsuario> findByUserName(String userName) {
        return rolUsuarioFacade.findByUserName(userName);
    }

    public List<RolUsuario> findByUserNameAndRoleName(String userName, String roleName) {
        return rolUsuarioFacade.findByUserNameAndRoleName(userName, roleName);
    }

    public List<RolUsuario> findByRoleName(String roleName) {
        return rolUsuarioFacade.findByRoleName(roleName);
    }

    public List<RolUsuario> getAllActiveRolesUsers() {
        return rolUsuarioFacade.getAllActiveRolesUsers();
    }

    public List<RolUsuario> getRolesUsuariosActivos(List<Rol> listaRoles) {
        return rolUsuarioFacade.getRolesUsuariosActivos(listaRoles);
    }

    /**
     * Devuelve los usuarios distintos vinculados a cualquiera de los roles
     * dados (rol-usuario activo). Si dos roles comparten un usuario, aparece
     * una sola vez.
     */
    public RolUsuarioDTO obtenerDTOPorId(Integer id) {
        if (id == null) return null;
        return RolUsuarioDTO.fromEntity(rolUsuarioFacade.find(id));
    }

    public List<RolUsuarioDTO> listarDTOsPorUsername(String userName) {
        return mapearLista(rolUsuarioFacade.findByUserName(userName));
    }

    public List<RolUsuarioDTO> listarDTOsActivosPorRoles(List<Rol> listaRoles) {
        return mapearLista(rolUsuarioFacade.getRolesUsuariosActivos(listaRoles));
    }

    private List<RolUsuarioDTO> mapearLista(List<RolUsuario> entidades) {
        List<RolUsuarioDTO> resultado = new ArrayList<>();
        if (entidades == null) return resultado;
        for (RolUsuario ru : entidades) resultado.add(RolUsuarioDTO.fromEntity(ru));
        return resultado;
    }

    public List<Usuario> obtenerUsuariosPorRoles(List<Rol> roles) {
        List<Usuario> resultado = new ArrayList<>();
        if (roles == null || roles.isEmpty()) {
            return resultado;
        }
        List<RolUsuario> rolesUsuarios = rolUsuarioFacade.getRolesUsuariosActivos(roles);
        if (rolesUsuarios == null) {
            return resultado;
        }
        for (RolUsuario ru : rolesUsuarios) {
            Usuario usuario = ru.getUsuario();
            if (usuario != null && !resultado.contains(usuario)) {
                resultado.add(usuario);
            }
        }
        return resultado;
    }
}
