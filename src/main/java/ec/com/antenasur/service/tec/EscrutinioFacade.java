package ec.com.antenasur.service.tec;

import javax.ejb.Stateless;

import ec.com.antenasur.domain.tec.Escrutinio;
import ec.com.antenasur.domain.generic.AbstractFacade;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class EscrutinioFacade extends AbstractFacade<Escrutinio, Integer> {

    public EscrutinioFacade() {
        super(Escrutinio.class, Integer.class);
    }

}
