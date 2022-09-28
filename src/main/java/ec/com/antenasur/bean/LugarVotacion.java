/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ec.com.antenasur.bean;

import ec.com.antenasur.domain.IglesiaPersona;
import ec.com.antenasur.domain.Persona;
import ec.com.antenasur.domain.tec.Mesa;
import ec.com.antenasur.domain.tec.Padron;
import ec.com.antenasur.domain.tec.Recinto;
import java.io.Serializable;
import javax.enterprise.context.RequestScoped;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author Admindba
 */
@RequestScoped
@NoArgsConstructor
public class LugarVotacion implements Serializable {

    private static final long serialVersionUID = 1L;

    @Setter
    @Getter
    private Mesa mesa;

    @Setter
    @Getter
    private Recinto recinto;

    @Setter
    @Getter
    private Padron padron;

    @Setter
    @Getter
    private Persona persona;

    @Setter
    @Getter
    private IglesiaPersona iglesiaPersona;

}
