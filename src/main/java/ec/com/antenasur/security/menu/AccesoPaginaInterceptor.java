package ec.com.antenasur.security.menu;

import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/** Verifica el principal real y permisos vigentes, nunca IDs enviados por JSF. */
public class AccesoPaginaInterceptor {
    @Inject private AutorizacionMenuService autorizacion;

    @AroundInvoke
    public Object verificar(InvocationContext invocacion) throws Exception {
        var acceso = invocacion.getMethod().getAnnotation(AccesoPagina.class);
        if (acceso == null) acceso = invocacion.getTarget().getClass().getAnnotation(AccesoPagina.class);
        if (acceso != null) autorizacion.exigirAlguna(acceso.value());
        return invocacion.proceed();
    }
}
