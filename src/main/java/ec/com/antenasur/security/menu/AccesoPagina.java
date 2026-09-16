package ec.com.antenasur.security.menu;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Una operacion compartida requiere al menos una de sus paginas de origen. */
@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AccesoPagina {
    String[] value();
}
