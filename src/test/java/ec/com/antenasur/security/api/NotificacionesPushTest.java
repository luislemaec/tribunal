package ec.com.antenasur.security.api;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ec.com.antenasur.dto.CronogramaFaseDTO;
import ec.com.antenasur.dto.IglesiaPersonaDTO;
import ec.com.antenasur.dto.MiembroJRVDTO;
import ec.com.antenasur.dto.PersonaDTO;
import ec.com.antenasur.enums.FaseElectoral;
import ec.com.antenasur.facade.tec.NotificacionMovilFacade;
import ec.com.antenasur.model.tec.NotificacionMovil;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.push.ClienteFcm;
import ec.com.antenasur.push.ConfiguracionPush;
import ec.com.antenasur.service.tec.CronogramaService;
import ec.com.antenasur.service.tec.DispositivoMovilService;
import ec.com.antenasur.service.tec.MiembroJRVService;
import ec.com.antenasur.service.tec.NotificacionMovilService;
import ec.com.antenasur.service.tec.ProcesoElectoralService;

import static org.junit.jupiter.api.Assertions.*;

/** Notificaciones push: firma, errores de Firebase, eventos, destinatarios y desactivación. */
class NotificacionesPushTest {

    private final List<NotificacionMovil> encoladas = new ArrayList<>();
    private final Set<String> eventos = new HashSet<>();
    private final List<CronogramaFaseDTO> fases = new ArrayList<>();
    private NotificacionMovilService servicio;
    private String activaAnterior;

    @BeforeEach
    void preparar() throws Exception {
        activaAnterior = System.getProperty(ConfiguracionPush.PROPIEDAD_ACTIVA);
        System.setProperty(ConfiguracionPush.PROPIEDAD_ACTIVA, "true");
        servicio = new NotificacionMovilService();
        inyectar("facade", new NotificacionMovilFacade() {
            @Override
            public void encolar(NotificacionMovil n) {
                encoladas.add(n);
            }

            @Override
            public boolean registrarEvento(String clave) {
                return eventos.add(clave);
            }

            @Override
            public List<Integer> usuariosDePersona(Integer personaId) {
                return personaId == 77 ? List.of(10) : List.of();
            }
        });
        inyectar("procesoElectoralService", new ProcesoElectoralService() {
            @Override
            public ProcesoElectoral getActivo() {
                ProcesoElectoral p = new ProcesoElectoral();
                p.setId(5);
                return p;
            }
        });
        inyectar("cronogramaService", new CronogramaService() {
            @Override
            public List<CronogramaFaseDTO> listarDTOsPorProceso(Integer procesoId) {
                return fases;
            }
        });
        inyectar("miembroJRVService", new MiembroJRVService() {
            @Override
            public List<MiembroJRVDTO> listarDTOsPorMesaProceso(Integer mesaId, Integer procesoId) {
                return List.of(miembro("SECRETARIO", 88), miembro("PRESIDENTE", 77));
            }
        });
    }

    @AfterEach
    void restaurar() {
        if (activaAnterior == null) {
            System.clearProperty(ConfiguracionPush.PROPIEDAD_ACTIVA);
        } else {
            System.setProperty(ConfiguracionPush.PROPIEDAD_ACTIVA, activaAnterior);
        }
    }

    // ------------------------------------------------------------ Firebase

    @Test
    void elJwtDeLaCuentaDeServicioSeFirmaConRs256() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        KeyPair par = generador.generateKeyPair();
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder().encodeToString(par.getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----\n";
        String json = "{\"project_id\":\"conpociiech\",\"client_email\":\"push@conpociiech.iam.gserviceaccount.com\","
                + "\"token_uri\":\"https://oauth2.googleapis.com/token\",\"private_key\":" + jsonTexto(pem) + "}";
        var constructor = ClienteFcm.class.getDeclaredConstructor(java.net.http.HttpClient.class, String.class);
        constructor.setAccessible(true);
        ClienteFcm cliente = constructor.newInstance(null, json);
        assertEquals("conpociiech", cliente.proyecto());

        Method jwt = ClienteFcm.class.getDeclaredMethod("jwt", Instant.class);
        jwt.setAccessible(true);
        String token = (String) jwt.invoke(cliente, Instant.ofEpochSecond(1_000_000));
        String[] partes = token.split("\\.");
        assertEquals(3, partes.length);
        String reclamos = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
        assertTrue(reclamos.contains("\"scope\":\"https://www.googleapis.com/auth/firebase.messaging\""));
        assertTrue(reclamos.contains("\"exp\":1003600"));
        Signature verificacion = Signature.getInstance("SHA256withRSA");
        verificacion.initVerify(par.getPublic());
        verificacion.update((partes[0] + "." + partes[1]).getBytes(StandardCharsets.US_ASCII));
        assertTrue(verificacion.verify(Base64.getUrlDecoder().decode(partes[2])));
    }

    @Test
    void soloSeDesactivaElTokenCuandoFirebaseLoDeclaraInvalido() throws Exception {
        Method clasificar = ClienteFcm.class.getDeclaredMethod("clasificar", int.class, String.class);
        clasificar.setAccessible(true);
        assertEquals(ClienteFcm.Resultado.ENVIADO, clasificar.invoke(null, 200, "{}"));
        assertEquals(ClienteFcm.Resultado.TOKEN_INVALIDO, clasificar.invoke(null, 404,
                "{\"error\":{\"status\":\"NOT_FOUND\",\"details\":[{\"errorCode\":\"UNREGISTERED\"}]}}"));
        assertEquals(ClienteFcm.Resultado.TOKEN_INVALIDO, clasificar.invoke(null, 400,
                "{\"error\":{\"message\":\"The registration token is not a valid FCM registration token\","
                        + "\"details\":[{\"errorCode\":\"INVALID_ARGUMENT\"}]}}"));
        assertEquals(ClienteFcm.Resultado.REINTENTAR, clasificar.invoke(null, 400,
                "{\"error\":{\"message\":\"Invalid JSON payload\",\"details\":[{\"errorCode\":\"INVALID_ARGUMENT\"}]}}"),
                "un mensaje mal formado no desactiva todos los dispositivos");
        assertEquals(ClienteFcm.Resultado.REINTENTAR, clasificar.invoke(null, 401, "{}"));
        assertEquals(ClienteFcm.Resultado.REINTENTAR, clasificar.invoke(null, 503, null));
    }

    // ------------------------------------------------------------ Eventos

    @Test
    void desactivadasNoSeEncolaNada() {
        System.setProperty(ConfiguracionPush.PROPIEDAD_ACTIVA, "false");
        servicio.mesaCerrada();
        servicio.actaFisicaRevisada(7, 5, "VALIDADA");
        fases.add(fase(1, FaseElectoral.SUFRAGIO, Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600)));
        assertTrue(servicio.revisarCronograma(Instant.now()).isEmpty());
        assertTrue(encoladas.isEmpty());
    }

    @Test
    void mesaCerradaAvisaATribunalYAdministradorSinDatosDeLaMesa() {
        servicio.mesaCerrada();
        assertEquals(List.of("SITEC-Tribunal", "SITEC-Administrador"), encoladas.stream().map(NotificacionMovil::getDestino).toList());
        assertTrue(encoladas.stream().allMatch(n -> NotificacionMovil.DESTINO_ROL.equals(n.getDestinoTipo())));
        assertFalse(encoladas.get(0).getCuerpo().contains("MESA"));
    }

    @Test
    void actaRevisadaAvisaSoloAlPresidenteDeEsaMesa() {
        servicio.actaFisicaRevisada(7, 5, "OBSERVADA");
        assertEquals(1, encoladas.size());
        assertEquals(NotificacionMovil.DESTINO_USUARIO, encoladas.get(0).getDestinoTipo());
        assertEquals("10", encoladas.get(0).getDestino());
        assertTrue(encoladas.get(0).getCuerpo().contains("observó"));
        servicio.actaFisicaRevisada(7, 5, "PENDIENTE_REVISION");
        assertEquals(1, encoladas.size(), "solo validada, observada o rechazada");
    }

    @Test
    void inicioYFinDelSufragioSeAvisanUnaSolaVez() {
        Instant ahora = Instant.parse("2026-10-18T13:30:00Z");
        fases.add(fase(9, FaseElectoral.SUFRAGIO, ahora.minus(Duration.ofMinutes(20)), ahora.plus(Duration.ofHours(8))));
        assertEquals(List.of("FASE_INICIO:9"), servicio.revisarCronograma(ahora));
        assertEquals(Set.of("SITEC-IglesiaAdmin", "SITEC-Presidente-mesa"),
                Set.copyOf(encoladas.stream().map(NotificacionMovil::getDestino).toList()));
        assertTrue(servicio.revisarCronograma(ahora.plusSeconds(60)).isEmpty(), "sin repetir");

        Instant fin = ahora.plus(Duration.ofHours(8)).plusSeconds(30);
        assertEquals(List.of("SUFRAGIO_FIN:9"), servicio.revisarCronograma(fin));
        assertEquals("Fin del sufragio", encoladas.get(encoladas.size() - 1).getTitulo());
    }

    @Test
    void eventosAntiguosNoSeAvisanTrasUnReinicio() {
        Instant ahora = Instant.parse("2026-10-18T13:30:00Z");
        fases.add(fase(3, FaseElectoral.ACTUALIZACION_MIEMBROS, ahora.minus(Duration.ofDays(3)), ahora.plus(Duration.ofDays(3))));
        assertTrue(servicio.revisarCronograma(ahora).isEmpty());
        assertTrue(encoladas.isEmpty());
    }

    // ------------------------------------------------------------ Registro de dispositivos

    @Test
    void elRegistroDeDispositivosExigeUnRolDelModulo() {
        Set<String> roles = Set.of("SITEC-Administrador", "SITEC-Tribunal", "SITEC-IglesiaAdmin", "SITEC-Presidente-mesa");
        for (Method m : DispositivoMovilService.class.getDeclaredMethods()) {
            if (java.lang.reflect.Modifier.isPublic(m.getModifiers())) {
                assertEquals(roles, Set.of(m.getAnnotation(RolesAllowed.class).value()), m.getName());
                assertNull(m.getAnnotation(PermitAll.class));
            }
        }
    }

    // ------------------------------------------------------------

    private static CronogramaFaseDTO fase(int id, FaseElectoral tipo, Instant inicio, Instant fin) {
        CronogramaFaseDTO f = new CronogramaFaseDTO();
        f.setId(id);
        f.setFase(tipo);
        f.setTitulo(tipo.name());
        f.setFechaInicio(Date.from(inicio));
        f.setFechaFin(Date.from(fin));
        return f;
    }

    private static MiembroJRVDTO miembro(String cargo, int personaId) {
        PersonaDTO p = new PersonaDTO();
        p.setId(personaId);
        IglesiaPersonaDTO ip = new IglesiaPersonaDTO();
        ip.setPersona(p);
        MiembroJRVDTO m = new MiembroJRVDTO();
        m.setCargoNombre(cargo);
        m.setIglesiaPersona(ip);
        return m;
    }

    private static String jsonTexto(String texto) {
        return "\"" + texto.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\"";
    }

    private void inyectar(String nombre, Object valor) throws Exception {
        var campo = NotificacionMovilService.class.getDeclaredField(nombre);
        campo.setAccessible(true);
        campo.set(servicio, valor);
    }
}
