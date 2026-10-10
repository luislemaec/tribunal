package ec.com.antenasur.facade.tec;

import jakarta.ejb.Stateless;

import ec.com.antenasur.model.generic.AbstractFacade;
import ec.com.antenasur.model.tec.AyudaContacto;

/** Persistencia de los canales de contacto del chatbot (create/edit con bitácora). */
@Stateless
public class AyudaContactoFacade extends AbstractFacade<AyudaContacto, Integer> {

    public AyudaContactoFacade() {
        super(AyudaContacto.class, Integer.class);
    }
}
