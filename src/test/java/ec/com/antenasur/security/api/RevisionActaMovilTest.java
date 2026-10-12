package ec.com.antenasur.security.api;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ec.com.antenasur.dto.EscrutinioCabeceraDTO;
import ec.com.antenasur.enums.EstadoEscrutinio;
import ec.com.antenasur.exception.NegocioException;
import ec.com.antenasur.model.tec.Documentos;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.service.tec.ActaFisicaEscrutinioService;
import ec.com.antenasur.service.tec.EscrutinioService;
import ec.com.antenasur.service.tec.GestionMovilService;
import ec.com.antenasur.service.tec.ProcesoElectoralService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Revisión del acta física desde la App (Administrador y Tribunal): solo mesas cerradas con
 * acta vigente sin validar, devolución con estado y motivo, y siempre por los servicios de la web.
 */
class RevisionActaMovilTest {

    private static final int PROCESO = 5;
    private static final int MESA = 7;
    private static final int DOCUMENTO = 40;

    private GestionMovilService servicio;
    private EstadoEscrutinio estado = EstadoEscrutinio.CERRADO;
    private Documentos acta;
    private final List<String> llamadas = new ArrayList<>();

    @BeforeEach
    void preparar() throws Exception {
        acta = new Documentos();
        acta.setId(DOCUMENTO);
        acta.setEstadoRevision(ActaFisicaEscrutinioService.PENDIENTE_REVISION);
        servicio = new GestionMovilService();
        inyectar("procesoElectoralService", new ProcesoElectoralService() {
            @Override
            public ProcesoElectoral getActivo() {
                ProcesoElectoral p = new ProcesoElectoral();
                p.setId(PROCESO);
                return p;
            }
        });
        inyectar("escrutinioService", new EscrutinioService() {
            @Override
            public EscrutinioCabeceraDTO buscarCabeceraDTO(Integer mesaId, Integer procesoId) {
                EscrutinioCabeceraDTO c = new EscrutinioCabeceraDTO();
                c.setEstadoEscrutinio(estado);
                return c;
            }
        });
        inyectar("actaFisicaService", new ActaFisicaEscrutinioService() {
            @Override
            public Documentos obtenerVigente(Integer mesaId, Integer procesoId) {
                assertEquals(MESA, mesaId);
                assertEquals(PROCESO, procesoId);
                return acta;
            }

            @Override
            public void validarDatosRegistrados(Integer documentoId, Integer mesaId, Integer procesoId) {
                llamadas.add("validar:" + documentoId + ":" + mesaId + ":" + procesoId);
            }

            @Override
            public void devolverActaFisica(Integer documentoId, String estadoRevision, String motivo) {
                llamadas.add("devolver:" + documentoId + ":" + estadoRevision + ":" + motivo);
            }
        });
    }

    @Test
    void validaElActaVigenteConElServicioDeLaWeb() {
        assertEquals(DOCUMENTO, servicio.validarActaFisica(MESA));
        assertEquals(List.of("validar:" + DOCUMENTO + ":" + MESA + ":" + PROCESO), llamadas);
    }

    @Test
    void devuelveConEstadoYMotivo() {
        assertEquals(DOCUMENTO, servicio.devolverActaFisica(MESA, "OBSERVADA", "  Firma ilegible  "));
        assertEquals(List.of("devolver:" + DOCUMENTO + ":OBSERVADA:Firma ilegible"), llamadas);
    }

    @Test
    void laDevolucionExigeEstadoValidoYMotivo() {
        assertThrows(NegocioException.class, () -> servicio.devolverActaFisica(MESA, "VALIDADA", "x"),
                "validar solo se hace con el cotejo");
        assertThrows(NegocioException.class, () -> servicio.devolverActaFisica(MESA, "RECHAZADA", " "));
        assertTrue(llamadas.isEmpty());
    }

    @Test
    void soloSeRevisaUnaMesaCerradaConActaSinValidar() {
        estado = EstadoEscrutinio.EN_CONTEO;
        assertThrows(NegocioException.class, () -> servicio.validarActaFisica(MESA));
        estado = EstadoEscrutinio.CERRADO;
        acta.setEstadoRevision(ActaFisicaEscrutinioService.VALIDADA);
        assertThrows(NegocioException.class, () -> servicio.validarActaFisica(MESA));
        acta = null;
        assertThrows(NegocioException.class, () -> servicio.devolverActaFisica(MESA, "RECHAZADA", "Ilegible"));
        assertTrue(llamadas.isEmpty());
    }

    @Test
    void decidirLaRevisionEsSoloDelTribunalYElAdministradorConsulta() throws Exception {
        for (String metodo : List.of("revisar", "validarActaFinal", "validarDatosRegistrados", "devolverActaFisica",
                "revertirValidacion")) {
            for (var m : ActaFisicaEscrutinioService.class.getDeclaredMethods()) {
                if (m.getName().equals(metodo)) {
                    assertEquals(java.util.Set.of("SITEC-Tribunal"),
                            java.util.Set.of(m.getAnnotation(jakarta.annotation.security.RolesAllowed.class).value()),
                            metodo + " es solo del Tribunal (web y App)");
                }
            }
        }
        var consulta = ActaFisicaEscrutinioService.class.getMethod("obtenerVigente", Integer.class, Integer.class);
        assertTrue(java.util.Set.of(consulta.getAnnotation(jakarta.annotation.security.RolesAllowed.class).value())
                .contains("SITEC-Administrador"), "el Administrador sigue consultando el acta");
    }

    private void inyectar(String nombre, Object valor) throws Exception {
        var campo = GestionMovilService.class.getDeclaredField(nombre);
        campo.setAccessible(true);
        campo.set(servicio, valor);
    }
}
