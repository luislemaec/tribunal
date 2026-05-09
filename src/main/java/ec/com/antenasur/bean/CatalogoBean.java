package ec.com.antenasur.bean;

import ec.com.antenasur.model.tec.CatalogoGeneral;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import ec.com.antenasur.service.tec.CatalogoGeneralService;

import java.io.Serializable;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Luis Lema
 */
@Named(value = "catalogoBean")
@RequestScoped
public class CatalogoBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String PATH_ACTAS_ESCRUTINIO = "C:\\ARCHIVOS\\ACTASE\\";

    private static final String AUTORIDADES_MESA = "CARGO AUTORIDADES MESA";
    private static final String AUTORIDADES_TRIBUNAL = "CARGOS AUTORIDADES TRIBUNAL";
    private static final String AUTORIDADES_CONPOCIIECH = "CARGO AUTORIDADES CONPOCIIECH";

    @Inject
    CatalogoGeneralService catalogoService;

    @Setter
    @Getter
    private List<CatalogoGeneral> catalogos;

    @Setter
    @Getter
    private CatalogoGeneral catalogo;

    @PostConstruct
    private void init() {
    }

    public List<CatalogoGeneral> getAutoridadesMesa() {
        return catalogoService.findByFatherName(AUTORIDADES_MESA);
    }

    public List<CatalogoGeneral> getAutoridadesTribunal() {
        return catalogoService.findByFatherName(AUTORIDADES_TRIBUNAL);
    }

    public List<CatalogoGeneral> getAutoridadesConpociiech() {
        return catalogoService.findByFatherName(AUTORIDADES_CONPOCIIECH);
    }

}
