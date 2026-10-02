package ec.com.antenasur.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * Resumen de escrutinio del proceso electoral activo para un filtro geográfico, calculado por
 * {@code ResumenEscrutinioService}. Solo cuenta mesas cerradas con acta física VALIDADA (mismo
 * criterio que el portal público). Blancos y nulos son informativos: no pertenecen a ninguna lista.
 */
@Getter
@Setter
public class ResumenEscrutinioDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Nombre del proceso activo; null si no hay proceso activo. */
    private String procesoNombre;

    private long recintos;
    /** Mesas del filtro con padrón en el proceso. */
    private long mesasProceso;
    /** Mesas del filtro con acta validada. */
    private long mesasEscrutadas;
    /** Empadronados de todas las mesas del filtro. */
    private long empadronados;
    /** Empadronados de las mesas con acta validada: base de la participación. */
    private long empadronadosEscrutados;
    /** Votos emitidos en actas validadas: listas + blancos + nulos (sin papeletas no utilizadas). */
    private long sufragantes;
    private long votosValidos;
    private long votosBlancos;
    private long votosNulos;
    /** Listas ordenadas, con su porcentaje sobre los votos válidos. */
    private List<ResultadoCategoriaPublicaDTO> listas = new ArrayList<>();

    public boolean isProcesoActivo() {
        return procesoNombre != null;
    }

    public BigDecimal getPorcentajeMesasEscrutadas() {
        return porcentaje(mesasEscrutadas, mesasProceso);
    }

    public BigDecimal getParticipacion() {
        return porcentaje(sufragantes, empadronadosEscrutados);
    }

    public BigDecimal getPorcentajeBlancos() {
        return porcentaje(votosBlancos, sufragantes);
    }

    public BigDecimal getPorcentajeNulos() {
        return porcentaje(votosNulos, sufragantes);
    }

    private static BigDecimal porcentaje(long parte, long total) {
        if (total <= 0L) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(parte).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
}
