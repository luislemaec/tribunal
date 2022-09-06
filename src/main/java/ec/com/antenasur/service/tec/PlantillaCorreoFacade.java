/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.service.tec;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import ec.com.antenasur.domain.tec.PlantillaCorreo;
import ec.com.antenasur.domain.generic.AbstractFacade;

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
            String sql = "FROM Rol r WHERE r.asunto=:asunto";
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

    public List<PlantillaCorreo> getRolesAplicativoSeleccion() {
        try {
            String sql = "FROM PlantillaCorreo r WHERE r.nombre LIKE :rolSeleccion and r.estado=true";
            Query query = super.getEntityManager().createQuery(sql);
            query.setParameter("rolSeleccion", "SITEC-%");
            List<PlantillaCorreo> resultList = query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList;
            }

        } catch (NoResultException e) {
            return null;
        }
        return null;
    }

}
