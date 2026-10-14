package ec.com.antenasur.service;

import ec.com.antenasur.exception.IglesiaPersonaException;
import ec.com.antenasur.util.JsfUtil;

/**
 * Validaciones de los datos propios de una persona, compartidas por el formulario de
 * Personas (TEC WEB) y la API móvil para no duplicar reglas. Lanzan
 * {@link IglesiaPersonaException} con las mismas claves de {@code messages_es.properties}
 * que muestra la web.
 */
public final class ValidadorPersona {

    /** Mismo límite que el campo «Apellidos y Nombres» de personas.xhtml. */
    public static final int LARGO_MAXIMO_NOMBRES = 100;

    private ValidadorPersona() {
    }

    /** Cédula ecuatoriana: obligatoria, 10 dígitos y dígito verificador válido. */
    public static void validarCedula(String cedula) {
        String valor = cedula == null ? "" : cedula.trim();
        if (valor.isEmpty()) {
            throw new IglesiaPersonaException("form.personas.error.documento.requerido");
        }
        if (!valor.matches("\\d{10}") || !JsfUtil.validarCedulaORUC(valor)) {
            throw new IglesiaPersonaException("form.personas.error.documento.invalido");
        }
    }

    /** «Apellidos y Nombres»: obligatorio y hasta {@value #LARGO_MAXIMO_NOMBRES} caracteres. */
    public static void validarNombres(String nombres) {
        String valor = nombres == null ? "" : nombres.trim();
        if (valor.isEmpty()) {
            throw new IglesiaPersonaException("form.personas.error.nombres.requerido");
        }
        if (valor.length() > LARGO_MAXIMO_NOMBRES) {
            throw new IglesiaPersonaException("form.personas.error.nombres.largo", LARGO_MAXIMO_NOMBRES);
        }
    }

    /** Sexo opcional; si se indica, solo M o F (las opciones del formulario web). */
    public static void validarSexo(String sexo) {
        if (sexo != null && !sexo.isBlank() && !"M".equals(sexo) && !"F".equals(sexo)) {
            throw new IglesiaPersonaException("form.personas.error.sexo.invalido");
        }
    }
}
