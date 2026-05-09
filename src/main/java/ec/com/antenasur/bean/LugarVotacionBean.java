package ec.com.antenasur.bean;

import java.io.Serializable;
import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import ec.com.antenasur.dto.VwLugarVotacionDTO;
import ec.com.antenasur.service.tec.VwLugarVotacionService;
import ec.com.antenasur.util.JsfUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Named(value = "lugarVotacion")
@RequestScoped
@NoArgsConstructor
public class LugarVotacionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    VwLugarVotacionService lugarVotacionService;

    @Inject
    ProcesoBean procesoBean;

    @Setter
    @Getter
    private List<VwLugarVotacionDTO> lugares;

    @Setter
    @Getter
    private String nombreCedula = "";

    public void buscarLugar() {
        try {
            lugares = lugarVotacionService.buscarDTOsPorNombreOCedula(nombreCedula);
            if (lugares != null && !lugares.isEmpty()) {
                procesoBean.registraActividad("BUSCA LUGAR VOTACION " + nombreCedula);
                JsfUtil.addSuccessMessage(lugares.size() + " LUGAR ENCONTRADO");
            } else {
                JsfUtil.addWarningMessage("NO SE ENCONTRO LUGAR DE VOTACIÃ“N");
            }
            nombreCedula = "";
        } catch (Exception e) {
        }
    }
}
