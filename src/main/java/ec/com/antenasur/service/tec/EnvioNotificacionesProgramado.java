package ec.com.antenasur.service.tec;

import java.time.Instant;
import java.util.List;

import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

import ec.com.antenasur.facade.tec.NotificacionMovilFacade;
import ec.com.antenasur.model.tec.DispositivoMovil;
import ec.com.antenasur.model.tec.NotificacionMovil;
import ec.com.antenasur.push.ClienteFcm;
import ec.com.antenasur.push.ConfiguracionPush;
import lombok.extern.slf4j.Slf4j;

/**
 * Cada minuto revisa el cronograma y envía la bandeja de salida de notificaciones push por
 * Firebase Cloud Messaging. Sin {@code tec.push.enabled=true} no hace nada. Corre fuera de
 * transacción: cada lectura y actualización usa la suya, para no retener filas mientras
 * espera a Firebase. Un fallo de Firebase nunca afecta a las operaciones electorales.
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Slf4j
public class EnvioNotificacionesProgramado {

    static final int LOTE = 100;
    static final int INTENTOS_MAXIMOS = 5;

    @Inject
    private NotificacionMovilFacade facade;

    @Inject
    private NotificacionMovilService notificaciones;

    private volatile boolean enCurso;
    private ClienteFcm cliente;
    private boolean avisoSinCredenciales;

    @Schedule(second = "0", minute = "*", hour = "*", persistent = false, info = "Notificaciones push App móvil")
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void ejecutar() {
        if (!ConfiguracionPush.activa() || enCurso) {
            return;
        }
        enCurso = true;
        try {
            try {
                List<String> eventos = notificaciones.revisarCronograma(Instant.now());
                if (!eventos.isEmpty()) {
                    log.info("PUSH eventos de cronograma encolados: {}", eventos);
                }
            } catch (Exception e) {
                log.error("PUSH no se pudo revisar el cronograma", e);
            }
            ClienteFcm fcm = cliente();
            if (fcm != null) {
                enviarPendientes(fcm);
            }
        } finally {
            enCurso = false;
        }
    }

    private void enviarPendientes(ClienteFcm fcm) {
        for (NotificacionMovil n : facade.pendientes(LOTE, INTENTOS_MAXIMOS)) {
            List<DispositivoMovil> dispositivos = NotificacionMovil.DESTINO_ROL.equals(n.getDestinoTipo())
                    ? facade.dispositivosDeRol(n.getDestino())
                    : facade.dispositivosDeUsuarios(List.of(Integer.valueOf(n.getDestino())));
            String error = null;
            for (DispositivoMovil d : dispositivos) {
                try {
                    ClienteFcm.Resultado r = fcm.enviar(d.getToken(), n.getTitulo(), n.getCuerpo(), n.getRuta());
                    if (r == ClienteFcm.Resultado.TOKEN_INVALIDO) {
                        facade.desactivar(d.getId());
                    } else if (r == ClienteFcm.Resultado.REINTENTAR) {
                        error = "Firebase no aceptó el envío";
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    error = e.getClass().getSimpleName();
                }
            }
            // Sin destinatarios con la App registrada, el aviso se da por atendido.
            n.setIntentos(n.getIntentos() + 1);
            if (error == null) {
                n.setEnviadaEn(Instant.now());
                n.setUltimoError(null);
            } else {
                n.setUltimoError(error.length() > 200 ? error.substring(0, 200) : error);
                log.warn("PUSH notificacion={} intento={} error={}", n.getId(), n.getIntentos(), n.getUltimoError());
            }
            facade.actualizar(n);
        }
    }

    /** Cliente de Firebase; {@code null} (y un aviso en el log) si falta la credencial. */
    private ClienteFcm cliente() {
        if (cliente != null) {
            return cliente;
        }
        String ruta = ConfiguracionPush.rutaCredenciales();
        try {
            if (ruta == null) {
                throw new IllegalStateException("falta -D" + ConfiguracionPush.PROPIEDAD_CREDENCIALES);
            }
            cliente = ClienteFcm.desdeArchivo(ruta);
            log.info("PUSH Firebase configurado para el proyecto {}", cliente.proyecto());
            return cliente;
        } catch (Exception e) {
            if (!avisoSinCredenciales) {
                log.warn("PUSH notificaciones activas pero sin credencial de Firebase utilizable ({}): "
                        + "se encolan y se enviarán cuando se configure.", e.getClass().getSimpleName());
                avisoSinCredenciales = true;
            }
            return null;
        }
    }
}
