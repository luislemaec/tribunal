package ec.com.antenasur.domain.generic;

import java.util.Date;

import javax.ejb.SessionContext;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

/**
 * <b> AGREGAR DESCRIPCION. </b>
 *
 * @author Luis Lema
 * @version Revision: 1.0
 * <p>
 * [Autor: Luis Lema, Fecha: 22/11/2020]
 * </p>
 */
public class EntityAuditable {

    public String getAuthenticateUserName() {
        String userName = null;
        try {
            SessionContext context = BeanLocator.getSessionContext();
            userName = context.getCallerPrincipal().getName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (userName == null || userName.isEmpty()) {
            userName = "<desconocido>";
        }
        return userName;
    }

    @PrePersist
    public void prePersist(Object object) {
        if (object instanceof EntidadAuditable) {
            EntidadAuditable entidadAuditable = (EntidadAuditable) object;
            entidadAuditable.setFechaCrea(new Date());
            entidadAuditable.setUsuarioCrea(getAuthenticateUserName());
        }
    }

    @PreUpdate
    public void preUpdate(Object object) {
        if (object instanceof EntidadAuditable) {
            EntidadAuditable entidadAuditable = (EntidadAuditable) object;
            entidadAuditable.setFechaActualiza(new Date());
            entidadAuditable.setUsuarioActualiza(getAuthenticateUserName());
        }
    }
}
