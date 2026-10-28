package ec.com.antenasur.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import jakarta.annotation.security.RolesAllowed;

import org.junit.jupiter.api.Test;

/** Regla de iglesia apta para la habilitación masiva y roles del servicio. */
class HabilitacionMasivaServiceTest {

    @Test
    void aptaHastaElPorcentajeExacto() {
        assertTrue(HabilitacionMasivaService.esApta(100, 0, 40));
        assertTrue(HabilitacionMasivaService.esApta(100, 40, 40));
        assertFalse(HabilitacionMasivaService.esApta(100, 41, 40));
        // 40/98 = 40,8 %: la pantalla mostraría 41 % y la comparación exacta la rechaza.
        assertFalse(HabilitacionMasivaService.esApta(98, 40, 40));
        assertTrue(HabilitacionMasivaService.esApta(98, 39, 40));
    }

    @Test
    void sinMiembrosOSinPendientesNoEsApta() {
        assertFalse(HabilitacionMasivaService.esApta(0, 0, 40));
        assertFalse(HabilitacionMasivaService.esApta(10, 10, 100));
        assertTrue(HabilitacionMasivaService.esApta(10, 9, 100));
        assertFalse(HabilitacionMasivaService.esApta(10, 1, 0));
        assertTrue(HabilitacionMasivaService.esApta(10, 0, 0));
    }

    @Test
    void iglesiaConNombreYComunidadReales() {
        assertTrue(HabilitacionMasivaService.esIglesiaIdentificada("CAMINO A LA VICTORIA", "SHUMID"));
        for (String noReal : new String[] { null, "", "   ", "N/D", "n/d", " N / D ", "ND", "N.D.", "n.d" }) {
            assertFalse(HabilitacionMasivaService.esIglesiaIdentificada(noReal, "SHUMID"), "nombre " + noReal);
            assertFalse(HabilitacionMasivaService.esIglesiaIdentificada("CAMINO A LA VICTORIA", noReal),
                    "comunidad " + noReal);
        }
        // Solo el valor completo es N/D: un nombre que lo contiene sigue siendo real.
        assertTrue(HabilitacionMasivaService.esIglesiaIdentificada("ANDES", "N/D NORTE"));
    }

    @Test
    void soloAdministradorYTribunal() throws Exception {
        RolesAllowed clase = HabilitacionMasivaService.class.getAnnotation(RolesAllowed.class);
        assertTrue(java.util.Set.of(clase.value()).equals(java.util.Set.of("SITEC-Administrador", "SITEC-Tribunal")));
        Method porMiembro = IglesiaPersonaService.class.getMethod("habilitarPendiente", Integer.class, Integer.class);
        assertTrue(java.util.Set.of(porMiembro.getAnnotation(RolesAllowed.class).value())
                .equals(java.util.Set.of("SITEC-Administrador", "SITEC-Tribunal")));
    }
}
