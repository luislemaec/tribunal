package ec.com.antenasur.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ec.com.antenasur.exception.IglesiaPersonaException;
import ec.com.antenasur.facade.IglesiaPersonaFacade;
import ec.com.antenasur.model.Iglesia;

/**
 * Regla: una iglesia ya agregada al padrón del proceso activo no admite cambios en sus
 * miembros, para ningún rol. Se prueba la validación única que usan todas las operaciones.
 */
class IglesiaEnPadronReglaTest {

    /** Facade sin base de datos: la iglesia 10 está en el padrón; registra cada consulta. */
    static class FacadePadron extends IglesiaPersonaFacade {
        final List<Collection<Integer>> consultas = new ArrayList<>();

        @Override
        public Set<Integer> iglesiasEnPadronActivo(Collection<Integer> ids) {
            consultas.add(new ArrayList<>(ids));
            return ids.contains(10) ? Set.of(10) : Set.of();
        }
    }

    private IglesiaPersonaService servicio;
    private FacadePadron facade;
    private Method validar;

    @BeforeEach
    void preparar() throws Exception {
        servicio = new IglesiaPersonaService();
        facade = new FacadePadron();
        Field campo = IglesiaPersonaService.class.getDeclaredField("iglesiaPersonaFacade");
        campo.setAccessible(true);
        campo.set(servicio, facade);
        validar = IglesiaPersonaService.class.getDeclaredMethod("validarIglesiasFueraDelPadron", Iglesia[].class);
        validar.setAccessible(true);
    }

    private void validar(Iglesia... iglesias) throws Throwable {
        try {
            validar.invoke(servicio, (Object) iglesias);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private static Iglesia iglesia(int id, String nombre) {
        Iglesia iglesia = new Iglesia();
        iglesia.setId(id);
        iglesia.setNombre(nombre);
        return iglesia;
    }

    @Test
    void iglesiaFueraDelPadronPermiteCambios() {
        assertDoesNotThrow(() -> validar(iglesia(20, "BETEL")));
    }

    @Test
    void iglesiaEnPadronRechazaConSuNombre() {
        IglesiaPersonaException e = assertThrows(IglesiaPersonaException.class,
                () -> validar(iglesia(20, "BETEL"), iglesia(10, "MUSHUJ ÑAN")));
        assertEquals("form.personas.error.iglesia.en.padron", e.getMessageKey());
        assertArrayEquals(new Object[] { "MUSHUJ ÑAN" }, e.getArguments());
    }

    @Test
    void origenODestinoEnPadronRechaza() {
        // Cambio de iglesia: basta con que el origen esté en el padrón.
        assertThrows(IglesiaPersonaException.class, () -> validar(iglesia(20, "DESTINO"), iglesia(10, "ORIGEN")));
    }

    @Test
    void ignoraNulosYConsultaUnaSolaVezSinRepetidos() throws Throwable {
        validar(null, iglesia(20, "A"), iglesia(20, "A"), new Iglesia());
        assertEquals(1, facade.consultas.size());
        assertEquals(List.of(20), facade.consultas.get(0));
    }

    @Test
    void consultaPublicaParaLaPantalla() {
        assertTrue(servicio.iglesiaEnPadronActivo(10));
        assertFalse(servicio.iglesiaEnPadronActivo(20));
        assertFalse(servicio.iglesiaEnPadronActivo(null));
    }
}
