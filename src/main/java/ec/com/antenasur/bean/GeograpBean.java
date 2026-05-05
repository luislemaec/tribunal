package ec.com.antenasur.bean;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.service.GeograpService;
import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;

@Named(value = "geograpBean")
@RequestScoped
public class GeograpBean {

    @Inject
    private GeograpService geograpService;

    @Setter
    @Getter
    private Geograp geograp;

    @PostConstruct
    public void init() {
        geograp = new Geograp();
    }

    public GeograpBean() {
    }

    public Geograp getById(Integer gelo_id) {
        try {
            return geograpService.find(gelo_id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Geograp findByFatherId(Integer gelo_id) {
        try {
            return geograpService.findByFather_Id(gelo_id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Geograp> getByFatherGeograp(Geograp geograp) {
        try {
            return geograpService.findByFatherGeograp(geograp);
        } catch (Exception e) {
            return null;
        }
    }

    public List<Geograp> getByFatherId(Integer gelo_id) {
        try {
            return geograpService.findByFatherId(gelo_id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Integer> getListaIdSGeograp(List<Geograp> listGeograp) {
        try {
            List<Integer> listaIdParroquias = new ArrayList<>();
            if (listGeograp != null) {
                for (Geograp item : listGeograp) {
                    listaIdParroquias.add(item.getId());
                }
            }
            if (!listaIdParroquias.isEmpty()) {
                return listaIdParroquias;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

}
