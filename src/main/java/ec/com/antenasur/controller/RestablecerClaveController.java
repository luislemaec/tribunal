package ec.com.antenasur.controller;

import java.io.IOException;
import java.io.Serializable;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import ec.com.antenasur.service.UsuarioService;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;

/** Consume un enlace de recuperación sin exponer ni conservar claves temporales. */
@Named
@RequestScoped
public class RestablecerClaveController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private UsuarioService usuarioService;

    @Getter
    @Setter
    private String token;

    @Getter
    @Setter
    private String clave1;

    @Getter
    @Setter
    private String clave2;

    public void restablecer() throws IOException {
        if (token == null || token.isBlank() || clave1 == null || clave2 == null) return;
        if (!clave1.equals(clave2)) {
            JsfUtil.addErrorMessage("Las contraseñas no coinciden");
            return;
        }
        if (!JsfUtil.validarContrasenia(clave1)) return;
        if (!usuarioService.restablecerConToken(token, clave1)) {
            JsfUtil.addErrorMessage("El enlace de recuperación no es válido o ha expirado");
            return;
        }
        JsfUtil.redirect("/claveActualizada.jsf");
    }
}
