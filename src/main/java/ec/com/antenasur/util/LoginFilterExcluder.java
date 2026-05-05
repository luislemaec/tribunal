package ec.com.antenasur.util;

import java.util.ArrayList;
import java.util.List;

public class LoginFilterExcluder {

    private static LoginFilterExcluder instance;

    private List<String> excludeUrls;
    private String contextPath;

    private LoginFilterExcluder(String contextPath) {
        this.contextPath = contextPath;
        excludeUrls = new ArrayList<String>();

        /**
         * *******************************
         */
        /**
         * ** DESHABILITA LA SEGURIDAD ***
         */
        //excludeUrls.add("/");
        /**
         * *******************************
         */
        excludeUrls.add("/resources/");
        excludeUrls.add("/javax.faces.resource/");
        excludeUrls.add("/errors/");
        excludeUrls.add("/index.html");
        excludeUrls.add("/login.jsf");
        excludeUrls.add("/olvidoClave.jsf");
        excludeUrls.add("/recuperaClaveCorrecto.jsf");
        excludeUrls.add("/consultar.jsf");
    }

    public boolean isExcludeUrl(String url) {
        for (String string : excludeUrls) {
            if (url.startsWith(this.contextPath + string)) {
                return true;
            }
        }
        return false;
    }

    public static LoginFilterExcluder getInstance(String contextPath) {
        return instance == null ? instance = new LoginFilterExcluder(contextPath) : instance;
    }

}
