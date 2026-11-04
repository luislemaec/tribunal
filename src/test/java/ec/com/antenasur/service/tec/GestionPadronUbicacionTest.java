package ec.com.antenasur.service.tec;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Set;

import jakarta.annotation.security.RolesAllowed;

import org.junit.jupiter.api.Test;

import ec.com.antenasur.model.Geograp;
import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.model.tec.Recinto;

/** Regla de ubicación al asignar una iglesia a una mesa: misma parroquia u otra del mismo cantón. */
class GestionPadronUbicacionTest {

    private static Geograp zona(int id, Geograp padre) {
        Geograp g = new Geograp();
        g.setId(id);
        g.setGeograp(padre);
        return g;
    }

    private static Iglesia iglesia(Geograp parroquia) {
        Iglesia i = new Iglesia();
        i.setUbicacion(parroquia);
        return i;
    }

    private static Recinto recinto(Geograp parroquia) {
        Recinto r = new Recinto();
        r.setUbicacion(parroquia);
        return r;
    }

    private final Geograp riobamba = zona(10, null);
    private final Geograp guamote = zona(20, null);
    private final Geograp lizarzaburu = zona(101, riobamba);
    private final Geograp lican = zona(102, riobamba);
    private final Geograp palmira = zona(201, guamote);

    @Test
    void mismaParroquiaPermitida() {
        assertTrue(GestionPadronService.ubicacionPermitida(iglesia(lizarzaburu), recinto(lizarzaburu)));
    }

    @Test
    void otraParroquiaDelMismoCantonPermitida() {
        assertTrue(GestionPadronService.ubicacionPermitida(iglesia(lican), recinto(lizarzaburu)));
    }

    @Test
    void parroquiaDeOtroCantonRechazada() {
        assertFalse(GestionPadronService.ubicacionPermitida(iglesia(palmira), recinto(lizarzaburu)));
    }

    @Test
    void sinUbicacionOSinCantonRechazada() {
        assertFalse(GestionPadronService.ubicacionPermitida(iglesia(null), recinto(lizarzaburu)));
        assertFalse(GestionPadronService.ubicacionPermitida(iglesia(lizarzaburu), recinto(null)));
        // Parroquias distintas sin cantón registrado: no se puede comprobar el cantón.
        assertFalse(GestionPadronService.ubicacionPermitida(iglesia(zona(301, null)), recinto(zona(302, null))));
    }

    @Test
    void servicioExigeRolAutenticado() throws Exception {
        RolesAllowed roles = GestionPadronService.class.getAnnotation(RolesAllowed.class);
        assertTrue(Set.of(roles.value()).containsAll(Set.of("SITEC-Administrador", "SITEC-Tribunal")));
        Method asignar = GestionPadronService.class.getMethod("asignarIglesia",
                Integer.class, Integer.class, Integer.class, Integer.class);
        assertFalse(asignar.isAnnotationPresent(jakarta.annotation.security.PermitAll.class));
    }
}
