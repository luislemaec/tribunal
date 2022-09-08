/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.service.tec;


import javax.ejb.Stateless;

import ec.com.antenasur.domain.tec.Lista;
import ec.com.antenasur.domain.generic.AbstractFacade;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class ListaFacade extends AbstractFacade<Lista, Integer> {

    private static final String HQL = " FROM Lista l";
    private static final String ORDENADO = " ORDER BY l.id";

    public ListaFacade() {
        super(Lista.class, Integer.class);
    }

}
