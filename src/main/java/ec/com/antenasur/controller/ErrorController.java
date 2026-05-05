/**
 *
 */
package ec.com.antenasur.controller;

import ec.com.antenasur.util.JsfUtil;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Luis Lema <lemaedu@gmail.com>
 * @Fecha 2022/08/08 10:19
 */
@Named
@RequestScoped
@Slf4j
public class ErrorController implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    public ErrorController() {
        // TODO Auto-generated constructor stub
    }
    
    public void redirigeInicio() {
        try {
            JsfUtil.redirect("/dashboard.jsf");
        } catch (RuntimeException ex) {
            Logger.getLogger(ErrorController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ErrorController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
