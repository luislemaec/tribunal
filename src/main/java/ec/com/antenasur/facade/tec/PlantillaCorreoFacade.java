/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.facade.tec;

import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;

import ec.com.antenasur.model.tec.PlantillaCorreo;
import ec.com.antenasur.model.generic.AbstractFacade;

;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class PlantillaCorreoFacade extends AbstractFacade<PlantillaCorreo, Integer> {

    public PlantillaCorreoFacade() {
        super(PlantillaCorreo.class, Integer.class);
    }

    public PlantillaCorreo buscarPorAsunto(String asunto) {
        try {
            String sql = "SELECT r FROM PlantillaCorreo r WHERE r.asunto=:asunto";
            Query query = super.getEntityManager().createQuery(sql);
            query.setParameter("asunto", asunto);
            List<PlantillaCorreo> resultList = query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList.get(0);
            }

        } catch (NoResultException e) {
            return null;
        }
        return null;
    }

}
