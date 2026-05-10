package ec.com.antenasur.bean;

import java.io.Serializable;
import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.model.tec.Recinto;
import ec.com.antenasur.service.tec.MesaService;
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
    private MesaService mesaService;

    public List<Mesa> mesasPorParroquias(List<Geograp> parroquias) {
        try {
            return mesaService.getMesasPorParroquias(parroquias);
        } catch (Exception e) {
            return null;
        }
    }

    public List<Mesa> mesasEscrutadasPorRecintos(List<Recinto> recintos) {
        try {
            return mesaService.getMesasEscrutadasPorRecintos(recintos);
        } catch (Exception e) {
            return null;
        }
    }

    public List<Mesa> mesasPorRecintos(List<Recinto> recintos) {
        try {
            return mesaService.getMesasPorRecintos(recintos);
        } catch (Exception e) {
            return null;
        }
    }

    public List<Mesa> mesasPorRecinto(Recinto recinto) {
        try {
            return mesaService.getMesasPorRecinto(recinto);
        } catch (Exception e) {
            return null;
        }
    }

    public Mesa mesaPorId(Integer idMesa) {
        try {
            return mesaService.find(idMesa);
        } catch (Exception e) {
            return null;
        }
    }

    public int totalMesas() {
        try {
            return mesaService.count();
        } catch (Exception e) {
            return 0;
        }
    }
    public int totalVotantes() {
        try {
            // Una sola query agregada en BD en lugar de cargar todas las mesas
            // y sumar en Java (patrón anterior que en el dashboard generaba
            // lentitud de varios segundos al disparar findAll + N+1).
            return (int) mesaService.sumTotalVotos();
        } catch (Exception e) {
            return 0;
        }
    }
}
