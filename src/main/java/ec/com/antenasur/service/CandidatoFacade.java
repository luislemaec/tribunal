/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.service;

import javax.ejb.Stateless;

import ec.com.antenasur.domain.Candidato;
import ec.com.antenasur.domain.CatalogoGeneral;
import ec.com.antenasur.domain.Lista;
import ec.com.antenasur.domain.generic.AbstractFacade;
import java.util.List;
import javax.persistence.TypedQuery;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class CandidatoFacade extends AbstractFacade<Candidato, Integer> {

    private static final String HQL = " FROM Candidato c";
    private static final String ORDENADO = " ORDER BY c.id";

    public CandidatoFacade() {
        super(Candidato.class, Integer.class);
    }

    public Candidato getPorCargoYLista(CatalogoGeneral cargo, Lista listaSeleccionado) {
        try {
            String sql = HQL + " WHERE c.cargo=:cargo AND c.lista =:lista ";
            TypedQuery<Candidato> query = super.getEntityManager().createQuery(sql, Candidato.class);
            query.setParameter("cargo", cargo);
            query.setParameter("lista", listaSeleccionado);
            List<Candidato> resultList = query.getResultList();

            if (resultList != null && !resultList.isEmpty()) {
                return resultList.get(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

}
