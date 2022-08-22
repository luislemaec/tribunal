/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.service;

import javax.ejb.Stateless;

import ec.com.antenasur.domain.MenuRol;
import ec.com.antenasur.domain.generic.AbstractFacade;



/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 */
@Stateless
public class MenuRolFacade extends AbstractFacade<MenuRol, Integer> {

	public MenuRolFacade() {
		super(MenuRol.class, Integer.class);
	}

}
