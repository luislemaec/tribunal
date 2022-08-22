package ec.com.antenasur.domain.generic;

import java.util.Set;

import javax.ejb.SessionContext;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class BeanLocator {

    @SuppressWarnings("unchecked")
    public static <T> T getInstance(final Class<T> type) {
        T result = null;
        try {
            InitialContext ctx = new InitialContext();
            BeanManager manager = (BeanManager) ctx.lookup("java:comp/BeanManager");
            Set<Bean<?>> beans = manager.getBeans(type);
            Bean<T> bean = (Bean<T>) manager.resolve(beans);
            if (bean != null) {
                CreationalContext<T> context = manager.createCreationalContext(bean);
                if (context != null) {
                    result = (T) manager.getReference(bean, type, context);
                }
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public static SessionContext getSessionContext() {
        SessionContext sessionContext = null;
        try {
            InitialContext ctx = new InitialContext();
            sessionContext = (SessionContext) ctx.lookup("java:comp/EJBContext");
        } catch (NamingException ex) {
            throw new IllegalStateException(ex);
        }
        return sessionContext;
    }

}
