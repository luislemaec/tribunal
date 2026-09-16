package ec.com.antenasur.security.qr;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VentanaAccesoQrTest {

    /**
     * Simula lo que hace el driver JDBC con una columna TIMESTAMP sin zona:
     * construye la fecha usando la zona por defecto de la JVM.
     */
    private static java.util.Date leerComoDriver(String horaDePared, String zonaJvm) {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(zonaJvm));
            return Timestamp.valueOf(LocalDateTime.parse(horaDePared));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test void laHoraDeParedSeInterpretaEnGuayaquilAunqueLaJvmEsteEnOtraZona() {
        Instant esperado = LocalDateTime.parse("2026-11-30T17:00:00")
                .atZone(VentanaAccesoQr.ZONA).toInstant();
        for (String zonaJvm : new String[]{"America/Guayaquil", "UTC", "Europe/Madrid"}) {
            TimeZone original = TimeZone.getDefault();
            try {
                TimeZone.setDefault(TimeZone.getTimeZone(zonaJvm));
                java.util.Date leida = leerComoDriver("2026-11-30T17:00:00", zonaJvm);
                assertEquals(esperado, VentanaAccesoQr.instante(leida),
                        "La ventana no debe depender de user.timezone; zona JVM=" + zonaJvm);
            } finally {
                TimeZone.setDefault(original);
            }
        }
    }

    @Test void lasDiecisieteHorasDeGuayaquilSonLasVeintidosUtc() {
        java.util.Date cierre = leerComoDriver("2026-11-30T17:00:00", "America/Guayaquil");
        assertEquals(Instant.parse("2026-11-30T22:00:00Z"), VentanaAccesoQr.instante(cierre));
    }

    @Test void laVentanaAbreEnElCierreYDuraSeisHoras() {
        java.util.Date cierre = leerComoDriver("2026-11-30T17:00:00", "America/Guayaquil");
        Instant desde = VentanaAccesoQr.desde(cierre);
        Instant hasta = VentanaAccesoQr.hasta(cierre);
        assertEquals(Instant.parse("2026-11-30T22:00:00Z"), desde);
        assertEquals(Instant.parse("2026-12-01T04:00:00Z"), hasta);
        assertEquals(6, java.time.Duration.between(desde, hasta).toHours());
        // 17:00 del cierre + 6 h = 23:00 del mismo día, hora de Guayaquil.
        assertEquals("2026-11-30T23:00-05:00[America/Guayaquil]",
                hasta.atZone(ZoneId.of("America/Guayaquil")).toString());
    }

    @Test void fechaNulaNoProduceVentana() {
        assertNull(VentanaAccesoQr.instante(null));
        assertNull(VentanaAccesoQr.desde(null));
        assertNull(VentanaAccesoQr.hasta(null));
    }
}
