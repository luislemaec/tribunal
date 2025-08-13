package ec.com.antenasur.bean;

import java.io.Serializable;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import ec.com.antenasur.domain.tec.VwLugarVotacion;
import ec.com.antenasur.service.tec.VwLugarVotacionFacade;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author Admindba
 */
@Named(value = "lugarVotacion")
@RequestScoped
@NoArgsConstructor
public class LugarVotacionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    VwLugarVotacionFacade lugarVotacionFacade;

    @Inject
    ProcesoBean procesoBean;

    @Setter
    @Getter
    private List<VwLugarVotacion> lugares;

    @Setter
    @Getter
    private String nombreCedula="";

    public void buscarLugar() {
        try {
            lugares = lugarVotacionFacade.buscaLugarVotacion(nombreCedula);
            if (lugares != null && !lugares.isEmpty()) {                
                procesoBean.registraActividad("BUSCA LUGAR VOTACION " + nombreCedula);
                JsfUtil.addSuccessMessage(lugares.size() + " LUGAR ENCONTRADO");
            } else {
                JsfUtil.addWarningMessage("NO SE ENCONTRO LUGAR DE VOTACIÓN");
            }
            nombreCedula="";
        } catch (Exception e) {
        }

    }

}
