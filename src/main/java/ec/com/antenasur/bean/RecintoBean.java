package ec.com.antenasur.bean;

import ec.com.antenasur.model.Geograp;
import java.io.Serializable;

import javax.enterprise.context.RequestScoped;

import ec.com.antenasur.model.tec.Recinto;
import ec.com.antenasur.service.tec.RecintoService;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@RequestScoped
@NoArgsConstructor
@Named
public class RecintoBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Setter
    @Getter
    private Recinto recinto;

    @Inject
    private RecintoService recintoService;

    public List<Recinto> recintosPorParroquias(List<Geograp> parroquias) {
        try {
            return recintoService.getRecintosPorParroquias(parroquias);
        } catch (Exception e) {
            return null;
        }
    }

    public Recinto recintosPorId(Integer idRecinto) {
        try {
            return recintoService.find(idRecinto);
        } catch (Exception e) {
            return null;
        }
    }

    public int totalRecintos() {
        try {
            return recintoService.count();
        } catch (Exception e) {
            return 0;
        }
    }
}
