package ec.com.antenasur.service.tec;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import ec.com.antenasur.dto.CronogramaFaseDTO;
import ec.com.antenasur.dto.MiembroJRVDTO;
import ec.com.antenasur.enums.FaseElectoral;
import ec.com.antenasur.facade.tec.NotificacionMovilFacade;
import ec.com.antenasur.model.tec.NotificacionMovil;
import ec.com.antenasur.model.tec.ProcesoElectoral;
import ec.com.antenasur.push.ConfiguracionPush;

/**
 * Encola las notificaciones push de la App móvil (docs/api-movil.md, «Notificaciones»). Se
 * llama dentro de la transacción del evento: si el evento se revierte, el aviso también.
 * No envía nada (lo hace {@link EnvioNotificacionesProgramado}) y no hace nada si las
 * notificaciones están desactivadas. Contenido mínimo, sin nombres ni datos personales: el
 * detalle se ve al abrir la App con sesión.
 *
 * <p>Sin {@code @RolesAllowed}: no se expone a la App; lo invocan los servicios electorales,
 * que ya autorizaron la operación, y el proceso programado del cronograma.
 */
@Stateless
public class NotificacionMovilService {

    static final String ROL_ADMINISTRADOR = "SITEC-Administrador";
    static final String ROL_TRIBUNAL = "SITEC-Tribunal";
    static final String ROL_IGLESIA_ADMIN = "SITEC-IglesiaAdmin";
    static final String ROL_PRESIDENTE = "SITEC-Presidente-mesa";

    static final String RUTA_ESCRUTINIO = "/tribunal/mesa/escrutinio";
    static final String RUTA_MESAS = "/tribunal/proceso/mesas";
    static final String RUTA_IGLESIA = "/tribunal/iglesia";

    /** Un evento del cronograma solo se avisa si ocurrió hace menos de una hora (no tras un apagado largo). */
    static final Duration VENTANA_EVENTO = Duration.ofHours(1);

    @Inject
    private NotificacionMovilFacade facade;

    @Inject
    private MiembroJRVService miembroJRVService;

    @Inject
    private ProcesoElectoralService procesoElectoralService;

    @Inject
    private CronogramaService cronogramaService;

    /** Al Presidente de la mesa, cuando el Tribunal valida, observa o rechaza el acta física. */
    public void actaFisicaRevisada(Integer mesaId, Integer procesoId, String estadoRevision) {
        if (!ConfiguracionPush.activa() || mesaId == null || procesoId == null) {
            return;
        }
        String cuerpo = switch (estadoRevision == null ? "" : estadoRevision) {
            case ActaFisicaEscrutinioService.VALIDADA -> "El Tribunal validó el acta física de su mesa.";
            case ActaFisicaEscrutinioService.OBSERVADA -> "El Tribunal observó el acta física de su mesa. Revise el detalle.";
            case ActaFisicaEscrutinioService.RECHAZADA -> "El Tribunal rechazó el acta física de su mesa. Revise el detalle.";
            default -> null;
        };
        if (cuerpo == null) {
            return;
        }
        for (MiembroJRVDTO miembro : miembroJRVService.listarDTOsPorMesaProceso(mesaId, procesoId)) {
            boolean presidente = miembro.getCargoNombre() != null
                    && miembro.getCargoNombre().trim().toUpperCase(Locale.ROOT).contains("PRESIDENTE");
            if (presidente && miembro.getIglesiaPersona() != null && miembro.getIglesiaPersona().getPersona() != null) {
                for (Integer usuarioId : facade.usuariosDePersona(miembro.getIglesiaPersona().getPersona().getId())) {
                    encolar(NotificacionMovil.DESTINO_USUARIO, String.valueOf(usuarioId), "Revisión del acta", cuerpo,
                            RUTA_ESCRUTINIO);
                }
            }
        }
    }

    /** A Tribunal y Administrador, cada vez que un Presidente cierra su mesa. */
    public void mesaCerrada() {
        if (!ConfiguracionPush.activa()) {
            return;
        }
        for (String rol : List.of(ROL_TRIBUNAL, ROL_ADMINISTRADOR)) {
            encolar(NotificacionMovil.DESTINO_ROL, rol, "Mesa cerrada",
                    "Un Presidente de mesa cerró su escrutinio. Revise el avance de mesas.", RUTA_MESAS);
        }
    }

    /**
     * Revisa el cronograma del proceso activo y encola, una sola vez por evento, el inicio de
     * cada fase (a los IglesiaAdmin) y el inicio y fin del sufragio (a los Presidentes de
     * mesa). Lo ejecuta el proceso programado; devuelve los eventos encolados.
     */
    public List<String> revisarCronograma(Instant ahora) {
        List<String> eventos = new ArrayList<>();
        if (!ConfiguracionPush.activa()) {
            return eventos;
        }
        ProcesoElectoral proceso = procesoElectoralService.getActivo();
        if (proceso == null || proceso.getId() == null) {
            return eventos;
        }
        for (CronogramaFaseDTO fase : cronogramaService.listarDTOsPorProceso(proceso.getId())) {
            Instant inicio = instante(fase.getFechaInicio());
            Instant fin = instante(fase.getFechaFin());
            boolean sufragio = fase.getFase() == FaseElectoral.SUFRAGIO;
            if (reciente(inicio, ahora) && (fin == null || ahora.isBefore(fin))
                    && facade.registrarEvento("FASE_INICIO:" + fase.getId())) {
                String titulo = fase.getTitulo() != null ? fase.getTitulo() : "una nueva fase";
                encolar(NotificacionMovil.DESTINO_ROL, ROL_IGLESIA_ADMIN, "Cronograma electoral",
                        recortar("Inicia la fase: " + titulo + ".", 200), RUTA_IGLESIA);
                eventos.add("FASE_INICIO:" + fase.getId());
                if (sufragio) {
                    encolar(NotificacionMovil.DESTINO_ROL, ROL_PRESIDENTE, "Inicio del sufragio",
                            "Comenzó la jornada de sufragio. Registre la apertura de su mesa.", RUTA_ESCRUTINIO);
                }
            }
            if (sufragio && reciente(fin, ahora) && facade.registrarEvento("SUFRAGIO_FIN:" + fase.getId())) {
                encolar(NotificacionMovil.DESTINO_ROL, ROL_PRESIDENTE, "Fin del sufragio",
                        "Terminó la jornada de sufragio. Registre el conteo y cierre su mesa.", RUTA_ESCRUTINIO);
                eventos.add("SUFRAGIO_FIN:" + fase.getId());
            }
        }
        return eventos;
    }

    private void encolar(String tipo, String destino, String titulo, String cuerpo, String ruta) {
        NotificacionMovil n = new NotificacionMovil();
        n.setDestinoTipo(tipo);
        n.setDestino(destino);
        n.setTitulo(titulo);
        n.setCuerpo(cuerpo);
        n.setRuta(ruta);
        n.setCreadaEn(Instant.now());
        facade.encolar(n);
    }

    /** El evento ya ocurrió y hace menos de {@link #VENTANA_EVENTO}. */
    static boolean reciente(Instant momento, Instant ahora) {
        return momento != null && !momento.isAfter(ahora) && momento.isAfter(ahora.minus(VENTANA_EVENTO));
    }

    private static Instant instante(Date fecha) {
        return fecha == null ? null : fecha.toInstant();
    }

    private static String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}
