package ec.com.antenasur.bean;

import java.io.Serializable;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import ec.com.antenasur.domain.Geograp;
import ec.com.antenasur.domain.tec.Mesa;
import ec.com.antenasur.domain.tec.Recinto;
import ec.com.antenasur.service.tec.MesaFacade;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@RequestScoped
@NoArgsConstructor
@Named
public class MesaBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Setter
    @Getter
    private Mesa mesa;

    @Setter
    @Getter
    private Recinto recinto;

    @Inject
    private MesaFacade mesaFacade;

    public List<Mesa> mesasPorParroquias(List<Geograp> parroquias) {
        try {
            return mesaFacade.getMesasPorParroquias(parroquias);
        } catch (Exception e) {
            return null;
        }
    }

    public List<Mesa> mesasEscrutadasPorRecintos(List<Recinto> recintos) {
        try {
            return mesaFacade.getMesasEscrutadasPorRecintos(recintos);
        } catch (Exception e) {
            return null;
        }
    }

    public List<Mesa> mesasPorRecintos(List<Recinto> recintos) {
        try {
            return mesaFacade.getMesasPorRecintos(recintos);
        } catch (Exception e) {
            return null;
        }
    }

    public List<Mesa> mesasPorRecinto(Recinto recinto) {
        try {
            return mesaFacade.getMesasPorRecinto(recinto);
        } catch (Exception e) {
            return null;
        }
    }

    public Mesa mesaPorId(Integer idMesa) {
        try {
            return mesaFacade.find(idMesa);
        } catch (Exception e) {
            return null;
        }
    }

    public int totalMesas() {
        try {
            return mesaFacade.count();
        } catch (Exception e) {
            return 0;
        }
    }
    public int totalVotantes() {
        try {
            List<Mesa> mesaTm= mesaFacade.findAll();
            int totalVotantes=0;
            if(mesaTm!=null) {
            	for(Mesa mesa:mesaTm) {
            		totalVotantes=totalVotantes+mesa.getTotalVotos();
            	}            
            }
            return totalVotantes;
        } catch (Exception e) {
            return 0;
        }
    }
}
