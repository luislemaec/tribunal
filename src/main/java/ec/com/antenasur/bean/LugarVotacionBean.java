package ec.com.antenasur.bean;

import java.io.Serializable;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import ec.com.antenasur.dto.LugarVotacionPublicoDTO;
import ec.com.antenasur.service.tec.ConsultaLugarVotacionService;
import ec.com.antenasur.service.tec.ConsultaLugarVotacionService.Resultado;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Consulta pública del lugar de votación (consultar.xhtml, sin sesión). Solo por cédula; las
 * reglas de privacidad y el límite de consultas los aplica {@link ConsultaLugarVotacionService}.
 */
@Slf4j
@Named("lugarVotacion")
@ViewScoped
public class LugarVotacionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ConsultaLugarVotacionService consultaService;

    @Getter
    @Setter
    private String cedula;

    @Getter
    private LugarVotacionPublicoDTO lugar;

    public void buscar() {
        lugar = null;
        try {
            Resultado resultado = consultaService.consultar(cedula, JsfUtil.getIPAddress());
            switch (resultado.estado()) {
                case ENCONTRADO -> lugar = resultado.lugar();
                case NO_ENCONTRADO -> JsfUtil.addWarningMessageFromBundle("consultaLugar.mensaje.no.encontrado");
                case CEDULA_INVALIDA -> JsfUtil.addErrorMessageFromBundle("consultaLugar.mensaje.cedula.invalida");
                case LIMITE -> JsfUtil.addErrorMessageFromBundle("consultaLugar.mensaje.limite");
                case SIN_PROCESO -> JsfUtil.addWarningMessageFromBundle("consultaLugar.mensaje.sin.proceso");
            }
        } catch (Exception e) {
            log.error("Error en la consulta pública de lugar de votación", e);
            JsfUtil.addErrorMessageFromBundle("consultaLugar.mensaje.error");
        }
        // La cédula no se conserva en la vista tras consultar.
        cedula = null;
    }

    public boolean isEncontrado() {
        return lugar != null;
    }
}
