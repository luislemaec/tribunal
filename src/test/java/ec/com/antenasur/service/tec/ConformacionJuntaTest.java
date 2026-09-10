package ec.com.antenasur.service.tec;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import ec.com.antenasur.dto.DependenciasMesaDTO;
import ec.com.antenasur.enums.TipoDocumentoMesa;
import ec.com.antenasur.facade.tec.MiembroJRVFacade;
import ec.com.antenasur.facade.tec.MesaFacade;
import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.model.tec.MiembroJRV;
import ec.com.antenasur.model.tec.CatalogoGeneral;

class ConformacionJuntaTest {
    private List<Object[]> junta(Integer mesa) {
        List<Object[]> miembros = new ArrayList<>();
        int persona = 1;
        for (String cargo : MiembroJRVService.DIGNIDADES_OBLIGATORIAS)
            miembros.add(new Object[]{mesa, cargo + " DE MESA", persona++});
        return miembros;
    }

    @Test void cuatroDesignacionesSinDocumentoMjrvHabilitanGenerarYRegenerarActa() {
        var junta = MiembroJRVService.evaluarConformacion(junta(7));
        assertTrue(junta.isCompleta());
        assertEquals(4, junta.getDignidadesAsignadas());
        var datos = new DependenciasMesaDTO();
        datos.setJuntaCompleta(junta.isCompleta());
        datos.setEmpadronados(50);
        assertTrue(datos.getDocumentos().isEmpty());
        assertNull(DisponibilidadDocumentoMesaService.motivo(TipoDocumentoMesa.DESIGNACION_MJRV, datos, true, true, true));
        String motivo = DisponibilidadDocumentoMesaService.motivo(TipoDocumentoMesa.ACTA_PARCIAL, datos, true, true, true);
        assertNull(motivo);
        assertTrue(DisponibilidadDocumentoMesaService.estado(true, false, motivo, false).isPuedeGenerar());
        assertTrue(DisponibilidadDocumentoMesaService.estado(true, true, motivo, false).isPuedeRegenerar());
    }

    @Test void quitarUnaDignidadBloqueaAunqueExistaActa() {
        var miembros = junta(7); miembros.remove(1);
        var junta = MiembroJRVService.evaluarConformacion(miembros);
        assertFalse(junta.isCompleta()); assertEquals(List.of("SECRETARIO"), junta.getCargosFaltantes());
        var datos = new DependenciasMesaDTO(); datos.setEmpadronados(50);
        datos.setJuntaCompleta(junta.isCompleta()); datos.getCargosFaltantes().addAll(junta.getCargosFaltantes());
        var estado = DisponibilidadDocumentoMesaService.estado(true, true,
                DisponibilidadDocumentoMesaService.motivo(TipoDocumentoMesa.ACTA_PARCIAL, datos, true, true, true), false);
        assertTrue(estado.isBloqueado()); assertFalse(estado.isPuedeRegenerar());
    }

    @Test void vicepresidenteNoSustituyePresidente() {
        var miembros = junta(7); miembros.get(0)[1] = "VICEPRESIDENTE DE MESA";
        var estado = MiembroJRVService.evaluarConformacion(miembros);
        assertFalse(estado.isCompleta()); assertTrue(estado.getCargosFaltantes().contains("PRESIDENTE"));
    }

    @Test void cargoRepetidoConPersonasDistintasBloquea() {
        var miembros = junta(7); miembros.add(new Object[]{7, "PRESIDENTE", 55});
        assertFalse(MiembroJRVService.evaluarConformacion(miembros).isCompleta());
    }

    @Test void unaPersonaEnDosDignidadesBloquea() {
        var miembros = junta(7); miembros.get(1)[2] = miembros.get(0)[2];
        assertFalse(MiembroJRVService.evaluarConformacion(miembros).isCompleta());
    }

    @Test void designacionSinPersonaNoCompletaLaJunta() {
        var miembros = junta(7); miembros.get(0)[2] = null;
        assertFalse(MiembroJRVService.evaluarConformacion(miembros).isCompleta());
    }

    @Test void consultaPorLoteConservaProcesoYSeparaMesas() throws Exception {
        var llamadas = new AtomicInteger();
        var servicio = new MiembroJRVService();
        var campo = MiembroJRVService.class.getDeclaredField("miembroJRVFacade");
        campo.setAccessible(true);
        campo.set(servicio, new MiembroJRVFacade() {
            @Override public List<Object[]> consultarConformacion(Integer proceso, List<Integer> mesas) {
                llamadas.incrementAndGet(); assertEquals(9, proceso); assertEquals(List.of(7, 8, 10), mesas);
                var filas = junta(7); filas.add(new Object[]{8, "PRESIDENTE DE MESA", 12}); return filas;
            }
        });
        var estados = servicio.consultarEstadosJuntas(9, List.of(7, 8, 10));
        assertEquals(1, llamadas.get());
        assertTrue(estados.get(7).isCompleta()); assertFalse(estados.get(8).isCompleta());
        assertEquals(0, estados.get(10).getDignidadesAsignadas());
    }

    @Test void conformacionNoExigeResponsableYJuntaRegistradaConservaBloqueo() throws Exception {
        var servicio = new MiembroJRVService();
        var mesa = new Mesa();
        var campoMesa = MiembroJRVService.class.getDeclaredField("mesaFacade"); campoMesa.setAccessible(true);
        campoMesa.set(servicio, new MesaFacade() { @Override public Mesa find(Integer id) { return mesa; } });
        var campoJrv = MiembroJRVService.class.getDeclaredField("miembroJRVFacade"); campoJrv.setAccessible(true);
        campoJrv.set(servicio, new MiembroJRVFacade() {
            @Override public List<MiembroJRV> listarPorMesaProceso(Integer mesaId, Integer procesoId) {
                assertEquals(7, mesaId); assertEquals(9, procesoId);
                return MiembroJRVService.DIGNIDADES_OBLIGATORIAS.stream().map(nombre -> {
                    var cargo = new CatalogoGeneral(); cargo.setNombre(nombre + " DE MESA");
                    var miembro = new MiembroJRV(); miembro.setCargo(cargo); return miembro;
                }).toList();
            }
            @Override public List<Object[]> consultarConformacion(Integer proceso, List<Integer> mesas) {
                return junta(7);
            }
        });
        assertTrue(servicio.consultarEstadoJunta(7, 9).isCompleta());
        assertFalse(servicio.juntaCompletadaRegistrada(7, 9));
        mesa.setResponsable("presidente");
        assertTrue(servicio.juntaCompletadaRegistrada(7, 9));
    }
}
