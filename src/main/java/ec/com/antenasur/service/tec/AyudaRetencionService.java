package ec.com.antenasur.service.tec;

import java.time.LocalDateTime;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.inject.Inject;

import ec.com.antenasur.facade.tec.AyudaFacade;
import lombok.extern.slf4j.Slf4j;

/**
 * Retención de las consultas del chatbot sin respuesta: borra a diario las que no se
 * repiten desde hace más de {@value AyudaService#DIAS_RETENCION_SIN_RESPUESTA} días.
 * El temporizador corre sin usuario, por eso usa el facade directamente.
 */
@Singleton
@Slf4j
public class AyudaRetencionService {

    @Inject
    private AyudaFacade ayudaFacade;

    @Schedule(hour = "3", minute = "15", persistent = false)
    public void purgarSinRespuesta() {
        try {
            int borradas = ayudaFacade.purgarSinRespuesta(
                    LocalDateTime.now().minusDays(AyudaService.DIAS_RETENCION_SIN_RESPUESTA));
            if (borradas > 0) {
                log.info("Ayuda: {} consultas sin respuesta eliminadas por retencion", borradas);
            }
        } catch (RuntimeException e) {
            log.warn("Ayuda: no se pudo aplicar la retencion de consultas sin respuesta", e);
        }
    }
}
