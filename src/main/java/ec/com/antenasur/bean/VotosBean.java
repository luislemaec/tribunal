package ec.com.antenasur.bean;

import java.io.Serializable;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.model.tec.Recinto;
import ec.com.antenasur.service.tec.VwTotalVotosService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@RequestScoped
@NoArgsConstructor
@Named
public class VotosBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    VwTotalVotosService vwTotalVotosService;

    @Setter
    @Getter
    private Recinto recinto;

    @Setter
    @Getter
    private List<Object[]> votos;

    public List<Object[]> votosPorParroquias(List<Geograp> parroquias) {
        try {
            return vwTotalVotosService.votosPorParroquias(parroquias);
        } catch (Exception e) {
            return null;
        }
    }

    public List<Object[]> votosPorRecintos(List<Recinto> recintos) {
        try {
            return vwTotalVotosService.votosPorRecintos(recintos);
        } catch (Exception e) {
            return null;
        }
    }

    public List<Object[]> votosPorMesas(List<Mesa> mesa) {
        try {
            return vwTotalVotosService.votosPorMesas(mesa);
        } catch (Exception e) {
            return null;
        }
    }

}
