package ec.com.antenasur.bean;

import java.io.Serializable;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import ec.com.antenasur.dto.LugarVotacionPublicoDTO;
import ec.com.antenasur.service.tec.ConsultaLugarVotacionService;
import ec.com.antenasur.service.tec.ConsultaLugarVotacionService.Resultado;
import ec.com.antenasur.service.tec.ConsultaLugarVotacionService.Verificacion;
import ec.com.antenasur.service.tec.DesafioAntiRobot;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Consulta pública del lugar de votación (consultar.xhtml). De petición y con vista sin estado
 * ({@code f:view transient}): no crea sesión ni guarda estado por visitante, para soportar muchas
 * consultas. Las reglas de privacidad, el anti-robot y los límites los aplica
 * {@link ConsultaLugarVotacionService}.
 */
@Slf4j
@Named("lugarVotacion")
@RequestScoped
public class LugarVotacionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ConsultaLugarVotacionService consultaService;

    @Getter
    @Setter
    private String cedula;

    /** Respuesta al desafío anti-robot, completada por el navegador. */
    @Getter
    @Setter
    private String challenge;
    @Getter
    @Setter
    private String salt;
    @Getter
    @Setter
    private String firma;
    @Getter
    @Setter
    private String numero;

    /** Campo trampa: oculto para una persona; con contenido indica un robot. */
    @Getter
    @Setter
    private String sitioWeb;

    @Getter
    private LugarVotacionPublicoDTO lugar;

    private DesafioAntiRobot.Desafio desafio;

    /** Desafío nuevo para cada página que se entrega (una vez por petición). */
    public DesafioAntiRobot.Desafio getDesafio() {
        if (desafio == null) {
            desafio = consultaService.nuevoDesafio();
        }
        return desafio;
    }

    public void buscar() {
        lugar = null;
        try {
            Resultado resultado = consultaService.consultar(cedula, JsfUtil.getIPAddress(),
                    new Verificacion(challenge, salt, firma, numero, sitioWeb));
            switch (resultado.estado()) {
                case ENCONTRADO -> lugar = resultado.lugar();
                case NO_ENCONTRADO -> JsfUtil.addWarningMessageFromBundle("consultaLugar.mensaje.no.encontrado");
                case CEDULA_INVALIDA -> JsfUtil.addErrorMessageFromBundle("consultaLugar.mensaje.cedula.invalida");
                case LIMITE -> JsfUtil.addErrorMessageFromBundle("consultaLugar.mensaje.limite");
                case OCUPADO -> JsfUtil.addWarningMessageFromBundle("consultaLugar.mensaje.ocupado");
                case VERIFICACION -> JsfUtil.addErrorMessageFromBundle("consultaLugar.mensaje.verificacion");
                case SIN_PROCESO -> JsfUtil.addWarningMessageFromBundle("consultaLugar.mensaje.sin.proceso");
            }
        } catch (Exception e) {
            log.error("Error en la consulta pública de lugar de votación", e);
            JsfUtil.addErrorMessageFromBundle("consultaLugar.mensaje.error");
        }
        // La cédula y la respuesta al desafío no se conservan: cada consulta usa un desafío nuevo.
        cedula = null;
        challenge = salt = firma = numero = null;
    }

    public boolean isEncontrado() {
        return lugar != null;
    }
}
