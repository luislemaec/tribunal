package ec.com.antenasur.security.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Date;

import org.junit.jupiter.api.Test;

import ec.com.antenasur.api.ConsultaPublicaRecurso;
import ec.com.antenasur.api.ConsultaPublicaRecurso.Salida;
import ec.com.antenasur.api.ContratosApi.ErrorApi;
import ec.com.antenasur.api.ContratosApi.RespuestaLugarVotacion;
import ec.com.antenasur.dto.LugarVotacionPublicoDTO;
import ec.com.antenasur.service.tec.ConsultaLugarVotacionService.Estado;
import ec.com.antenasur.service.tec.ConsultaLugarVotacionService.Resultado;

/** Traducción a HTTP de la consulta pública de lugar de votación de la API móvil. */
class ConsultaPublicaRecursoTest {

    private static Salida respuesta(Resultado resultado) {
        return ConsultaPublicaRecurso.traducir(resultado);
    }

    @Test
    void encontradoDevuelveElLugarSinCedulaNiIglesia() throws Exception {
        LugarVotacionPublicoDTO lugar = new LugarVotacionPublicoDTO("J*** P****", "Elecciones 2026",
                new Date(1_794_000_000_000L), "ESCUELA A", "MESA 1", "CEBADAS", "GUAMOTE");
        Salida r = respuesta(new Resultado(Estado.ENCONTRADO, lugar));
        assertEquals(200, r.http());
        RespuestaLugarVotacion cuerpo = (RespuestaLugarVotacion) r.cuerpo();
        assertEquals("ENCONTRADO", cuerpo.getEstado());
        assertEquals("J*** P****", cuerpo.getLugar().getElector());
        assertEquals("MESA 1", cuerpo.getLugar().getMesa());
        assertEquals(10, cuerpo.getLugar().getFechaSufragio().length());
    }

    @Test
    void noEncontradoYSinProcesoSonRespuestasNormales() throws Exception {
        Salida r = respuesta(new Resultado(Estado.NO_ENCONTRADO, null));
        assertEquals(200, r.http());
        assertEquals("NO_ENCONTRADO", ((RespuestaLugarVotacion) r.cuerpo()).getEstado());
        assertNull(((RespuestaLugarVotacion) r.cuerpo()).getLugar());
        assertEquals("SIN_PROCESO",
                ((RespuestaLugarVotacion) respuesta(new Resultado(Estado.SIN_PROCESO, null)).cuerpo()).getEstado());
    }

    @Test
    void erroresConCodigoHttpYCodigoEstable() throws Exception {
        verificar(Estado.CEDULA_INVALIDA, 400, "CEDULA_INVALIDA");
        verificar(Estado.VERIFICACION, 400, "VERIFICACION_FALLIDA");
        verificar(Estado.LIMITE, 429, "DEMASIADAS_CONSULTAS");
        verificar(Estado.OCUPADO, 503, "SERVICIO_OCUPADO");
    }

    private static void verificar(Estado estado, int http, String codigo) throws Exception {
        Salida r = respuesta(new Resultado(estado, null));
        assertEquals(http, r.http());
        assertEquals(codigo, ((ErrorApi) r.cuerpo()).getCodigo());
    }
}
