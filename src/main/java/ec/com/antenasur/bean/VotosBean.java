package ec.com.antenasur.bean;

import java.io.Serializable;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import ec.com.antenasur.domain.Geograp;
import ec.com.antenasur.domain.tec.Mesa;
import ec.com.antenasur.domain.tec.Recinto;
import ec.com.antenasur.service.tec.VwTotalVotosFacade;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@RequestScoped
@NoArgsConstructor
@Named
public class VotosBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    VwTotalVotosFacade vwTotalVotosFacade;

    @Setter
    @Getter
    private Recinto recinto;

    @Setter
    @Getter
    private List<Object[]> votos;

    public List<Object[]> votosPorParroquias(List<Geograp> parroquias) {
        try {
            return vwTotalVotosFacade.votosPorParroquias(parroquias);
        } catch (Exception e) {
            return null;
        }
    }

    public List<Object[]> votosPorRecintos(List<Recinto> recintos) {
        try {
            return vwTotalVotosFacade.votosPorRecintos(recintos);
        } catch (Exception e) {
            return null;
        }
    }

    public List<Object[]> votosPorMesas(List<Mesa> mesa) {
        try {
            return vwTotalVotosFacade.votosPorMesas(mesa);
        } catch (Exception e) {
            return null;
        }
    }

}
