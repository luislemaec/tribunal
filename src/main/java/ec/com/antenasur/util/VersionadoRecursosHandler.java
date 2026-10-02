package ec.com.antenasur.util;

import java.util.Set;

import jakarta.faces.application.Resource;
import jakarta.faces.application.ResourceHandler;
import jakarta.faces.application.ResourceHandlerWrapper;
import jakarta.faces.application.ResourceWrapper;

/**
 * Agrega a la URL de los recursos propios de la aplicación (CSS, JS, fuentes e imágenes servidos por
 * jakarta.faces.resource) el parámetro {@code tv} con la marca del despliegue.
 *
 * Los recursos se cachean 7 días (com.sun.faces.defaultResourceMaxAge) y sus URL no cambiaban entre
 * versiones: tras un despliegue el navegador seguía usando los estilos anteriores. Cada despliegue
 * reinicia la aplicación y genera una marca nueva, por lo que la URL cambia y el navegador descarga
 * la versión actual. Las bibliotecas de PrimeFaces y Jakarta Faces ya traen su propio versionado.
 */
public class VersionadoRecursosHandler extends ResourceHandlerWrapper {

    private static final String MARCA_DESPLIEGUE = Long.toString(System.currentTimeMillis(), 36);
    private static final Set<String> BIBLIOTECAS_EXTERNAS = Set.of("primefaces", "primefaces-extensions", "jakarta.faces");

    public VersionadoRecursosHandler(ResourceHandler wrapped) {
        super(wrapped);
    }

    @Override
    public Resource createResource(String resourceName) {
        return versionar(super.createResource(resourceName));
    }

    @Override
    public Resource createResource(String resourceName, String libraryName) {
        return versionar(super.createResource(resourceName, libraryName));
    }

    @Override
    public Resource createResource(String resourceName, String libraryName, String contentType) {
        return versionar(super.createResource(resourceName, libraryName, contentType));
    }

    private static Resource versionar(Resource recurso) {
        String biblioteca = recurso == null ? null : recurso.getLibraryName();
        if (recurso == null || biblioteca != null && BIBLIOTECAS_EXTERNAS.contains(biblioteca)) {
            return recurso;
        }
        return new RecursoVersionado(recurso);
    }

    private static final class RecursoVersionado extends ResourceWrapper {

        private RecursoVersionado(Resource wrapped) {
            super(wrapped);
        }

        @Override
        public String getRequestPath() {
            String ruta = super.getRequestPath();
            return ruta + (ruta.contains("?") ? "&" : "?") + "tv=" + MARCA_DESPLIEGUE;
        }
    }
}
