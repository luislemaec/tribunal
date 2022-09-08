package ec.com.antenasur.service.tec;

import javax.ejb.Stateless;

import ec.com.antenasur.domain.tec.CategoriaVoto;
import ec.com.antenasur.domain.generic.AbstractFacade;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class CategoriaVotoFacade extends AbstractFacade<CategoriaVoto, Integer> {

    public CategoriaVotoFacade() {
        super(CategoriaVoto.class, Integer.class);
    }

}
