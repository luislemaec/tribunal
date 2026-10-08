package ec.com.antenasur.security.api;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ec.com.antenasur.facade.tec.SesionMovilFacade;
import ec.com.antenasur.model.Usuario;
import ec.com.antenasur.model.tec.SesionMovil;
import ec.com.antenasur.security.qr.TokenActaQr;
import ec.com.antenasur.service.tec.SesionMovilService;
import ec.com.antenasur.service.tec.SesionMovilService.SesionValida;
import ec.com.antenasur.service.tec.SesionMovilService.TokensEmitidos;

import static org.junit.jupiter.api.Assertions.*;

/** Reglas de las sesiones de la App móvil (docs/api-movil.md) sin base de datos. */
class SesionMovilServiceTest {

    private FacadeEnMemoria facade;
    private SesionMovilService servicio;
    private Usuario usuario;

    @BeforeEach
    void preparar() throws Exception {
        usuario = new Usuario();
        usuario.setId(10);
        usuario.setUsername("presidente");
        usuario.setEstado(true);
        facade = new FacadeEnMemoria(usuario);
        servicio = new SesionMovilService();
        var campo = SesionMovilService.class.getDeclaredField("facade");
        campo.setAccessible(true);
        campo.set(servicio, facade);
    }

    @Test
    void emiteTokensOpacosYGuardaSoloSuHash() {
        TokensEmitidos t = servicio.emitir(10, false, "10.0.0.1", "App\u0000 prueba");
        SesionMovil s = facade.sesiones.get(0);
        assertTrue(TokenActaQr.formatoValido(t.accessToken()));
        assertTrue(TokenActaQr.formatoValido(t.refreshToken()));
        assertNotEquals(t.accessToken(), t.refreshToken());
        assertEquals(TokenActaQr.hash(t.accessToken()), s.getAccesoHash());
        assertEquals(TokenActaQr.hash(t.refreshToken()), s.getRefreshHash());
        assertNotEquals(t.accessToken(), s.getAccesoHash());
        assertEquals(300, t.expiraEnSegundos());
        assertEquals("App  prueba", s.getDispositivo(), "sin caracteres de control");
        assertEquals(s.getCreadaEn().plus(SesionMovilService.VIDA_MAXIMA), s.getExpiraAbsoluta());
    }

    @Test
    void noEmiteParaUnUsuarioInactivo() {
        usuario.setEstado(false);
        assertThrows(IllegalArgumentException.class, () -> servicio.emitir(10, false, null, null));
    }

    @Test
    void validaElAccesoVigente() {
        TokensEmitidos t = servicio.emitir(10, true, null, null);
        SesionValida v = servicio.validarAcceso(t.accessToken());
        assertNotNull(v);
        assertEquals(10, v.usuarioId());
        assertEquals("presidente", v.usuario());
        assertTrue(v.soloCambioClave());
    }

    @Test
    void rechazaTokensMalFormadosDesconocidosYElRefreshComoAcceso() {
        TokensEmitidos t = servicio.emitir(10, false, null, null);
        assertNull(servicio.validarAcceso(null));
        assertNull(servicio.validarAcceso("corto"));
        assertNull(servicio.validarAcceso(TokenActaQr.generar()));
        assertNull(servicio.validarAcceso(t.refreshToken()));
    }

    @Test
    void elAccesoVenceALosCincoMinutos() {
        TokensEmitidos t = servicio.emitir(10, false, null, null);
        retroceder(facade.sesiones.get(0), Duration.ofMinutes(5).plusSeconds(1));
        assertNull(servicio.validarAcceso(t.accessToken()));
    }

    @Test
    void quinceMinutosDeInactividadInvalidanAccesoYRefresh() {
        TokensEmitidos t = servicio.emitir(10, false, null, null);
        SesionMovil s = facade.sesiones.get(0);
        s.setUltimoUso(Instant.now().minus(SesionMovilService.INACTIVIDAD).minusSeconds(1));
        assertNull(servicio.validarAcceso(t.accessToken()));
        assertNull(servicio.renovar(t.refreshToken()));
    }

    @Test
    void laVidaMaximaNoSeExtiendeConActividad() {
        TokensEmitidos t = servicio.emitir(10, false, null, null);
        facade.sesiones.get(0).setExpiraAbsoluta(Instant.now().minusSeconds(1));
        assertNull(servicio.validarAcceso(t.accessToken()));
        assertNull(servicio.renovar(t.refreshToken()));
    }

    @Test
    void renovarRotaAmbosTokensEInvalidaLosAnteriores() {
        TokensEmitidos t1 = servicio.emitir(10, false, null, null);
        TokensEmitidos t2 = servicio.renovar(t1.refreshToken());
        assertNotNull(t2);
        assertEquals(t1.sesionId(), t2.sesionId());
        assertNotEquals(t1.accessToken(), t2.accessToken());
        assertNotEquals(t1.refreshToken(), t2.refreshToken());
        assertNull(servicio.validarAcceso(t1.accessToken()));
        assertNotNull(servicio.validarAcceso(t2.accessToken()));
    }

    @Test
    void reutilizarUnRefreshRotadoRevocaLaSesion() {
        TokensEmitidos t1 = servicio.emitir(10, false, null, null);
        TokensEmitidos t2 = servicio.renovar(t1.refreshToken());
        assertNull(servicio.renovar(t1.refreshToken()), "copia del refresh anterior");
        SesionMovil s = facade.sesiones.get(0);
        assertNotNull(s.getRevocadaEn());
        assertEquals(SesionMovilService.MOTIVO_REFRESH_REUTILIZADO, s.getMotivoRevocacion());
        assertNull(servicio.validarAcceso(t2.accessToken()), "el poseedor legítimo también pierde la sesión");
        assertNull(servicio.renovar(t2.refreshToken()));
    }

    @Test
    void unUsuarioDesactivadoPierdeLaSesion() {
        TokensEmitidos t = servicio.emitir(10, false, null, null);
        usuario.setEstado(false);
        assertNull(servicio.validarAcceso(t.accessToken()));
        assertEquals(SesionMovilService.MOTIVO_USUARIO_INACTIVO, facade.sesiones.get(0).getMotivoRevocacion());
    }

    @Test
    void logoutYCambioDeClaveRevocan() {
        TokensEmitidos a = servicio.emitir(10, false, null, null);
        TokensEmitidos b = servicio.emitir(10, false, null, null);
        servicio.revocar(a.sesionId(), SesionMovilService.MOTIVO_LOGOUT);
        assertNull(servicio.validarAcceso(a.accessToken()));
        assertNotNull(servicio.validarAcceso(b.accessToken()));
        assertEquals(1, servicio.revocarPorUsuario(10, SesionMovilService.MOTIVO_CAMBIO_CLAVE));
        assertNull(servicio.validarAcceso(b.accessToken()));
        assertNull(servicio.renovar(b.refreshToken()));
    }

    @Test
    void laSesionRestringidaSeConservaAlRenovar() {
        TokensEmitidos t = servicio.emitir(10, true, null, null);
        assertTrue(servicio.renovar(t.refreshToken()).soloCambioClave());
    }

    private static void retroceder(SesionMovil s, Duration d) {
        s.setCreadaEn(s.getCreadaEn().minus(d));
        s.setAccesoEmitidoEn(s.getAccesoEmitidoEn().minus(d));
        s.setExpiraAbsoluta(s.getExpiraAbsoluta().minus(d));
    }

    /** Fachada en memoria con la misma semántica de búsqueda que las consultas JPA. */
    static final class FacadeEnMemoria extends SesionMovilFacade {
        final List<SesionMovil> sesiones = new ArrayList<>();
        final Map<Integer, Usuario> usuarios = new HashMap<>();
        private long secuencia;

        FacadeEnMemoria(Usuario usuario) {
            usuarios.put(usuario.getId(), usuario);
        }

        @Override
        public void registrar(SesionMovil sesion) {
            sesion.setId(++secuencia);
            sesiones.add(sesion);
        }

        @Override
        public SesionMovil buscarPorAcceso(String hash) {
            return sesiones.stream().filter(s -> hash.equals(s.getAccesoHash())).findFirst().orElse(null);
        }

        @Override
        public SesionMovil bloquearPorRefresh(String hash) {
            return sesiones.stream().filter(s -> hash.equals(s.getRefreshHash())).findFirst().orElse(null);
        }

        @Override
        public SesionMovil bloquearPorRefreshAnterior(String hash) {
            return sesiones.stream().filter(s -> hash.equals(s.getRefreshAnteriorHash())).findFirst().orElse(null);
        }

        @Override
        public SesionMovil buscar(Long id) {
            return sesiones.stream().filter(s -> id.equals(s.getId())).findFirst().orElse(null);
        }

        @Override
        public Usuario usuario(Integer usuarioId) {
            return usuarios.get(usuarioId);
        }

        @Override
        public void actualizar(SesionMovil sesion) {
            // Las entidades en memoria ya están actualizadas.
        }

        @Override
        public int revocarPorUsuario(Integer usuarioId, Instant ahora, String motivo) {
            int total = 0;
            for (SesionMovil s : sesiones) {
                if (usuarioId.equals(s.getUsuarioId()) && s.getRevocadaEn() == null) {
                    s.setRevocadaEn(ahora);
                    s.setMotivoRevocacion(motivo);
                    total++;
                }
            }
            return total;
        }
    }
}
