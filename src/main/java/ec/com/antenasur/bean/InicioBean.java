package ec.com.antenasur.bean;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.bean.ManagedProperty;
import javax.inject.Named;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Luis Lema
 */
@Named(value = "inicioBean")
@RequestScoped
public class InicioBean {

    @Setter
    @Getter
    @ManagedProperty(value = "#{loginBean}")
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
