/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.service.tec;

import javax.ejb.Stateless;

import ec.com.antenasur.domain.tec.Correo;
import ec.com.antenasur.domain.generic.AbstractFacade;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class CorreoFacade extends AbstractFacade<Correo, Integer> {

    static final String HQL = " FROM Correos c";
    static final String ACTIVOS = "  c.estado =TRUE";
    static final String ORDENADO = "  ORDER BY c.id";

    public CorreoFacade() {
        super(Correo.class, Integer.class);
    }

}
