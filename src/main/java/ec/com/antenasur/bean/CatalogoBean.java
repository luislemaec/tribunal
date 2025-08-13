package ec.com.antenasur.bean;

import ec.com.antenasur.domain.tec.CatalogoGeneral;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import ec.com.antenasur.service.tec.CatalogoGeneralFacade;

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
    CatalogoGeneralFacade catalogoFacade;

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
        return catalogoFacade.findByFatherName(AUTORIDADES_MESA);
    }

    public List<CatalogoGeneral> getAutoridadesTribunal() {
        return catalogoFacade.findByFatherName(AUTORIDADES_TRIBUNAL);
    }

    public List<CatalogoGeneral> getAutoridadesConpociiech() {
        return catalogoFacade.findByFatherName(AUTORIDADES_CONPOCIIECH);
    }

}
