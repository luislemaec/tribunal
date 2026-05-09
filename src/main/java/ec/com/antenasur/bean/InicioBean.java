package ec.com.antenasur.bean;

import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Luis Lema
 */
@Named(value = "inicioBean")
@RequestScoped
public class InicioBean {

    @Inject
    private LoginBean loginBean;

    @Setter
    @Getter
    private List<Object[]> listaMedioPorTipo;

    @Setter
    @Getter
    private int tipoMedio;

    public InicioBean() {
    }

    @PostConstruct
    public void init() {

    }
}
