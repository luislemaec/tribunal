package ec.com.antenasur.dto;

import java.io.Serializable;
import ec.com.antenasur.enums.EstadoEscrutinio;

/**
 * Proyeccion de lectura del flujo vigente. No autoriza operaciones ni persiste
 * estados.
 */
public final class ProgresoEscrutinioDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Etapa {
        APERTURA, CONTEO, CIERRE, ACTA_FISICA, DATO_OFICIAL;

        public String getClave() {
            return "actaE.etapa." + name();
        }

        public int getIndice() {
            return ordinal();
        }
    }

    private final Etapa etapa;
    private final String detalle;
    private final String incidencia;
    private final boolean aperturaCumplida;
    private final boolean oficial;

    private ProgresoEscrutinioDTO(Etapa etapa, String detalle, String incidencia,
            boolean aperturaCumplida, boolean oficial) {
        this.etapa = etapa;
        this.detalle = detalle;
        this.incidencia = incidencia;
        this.aperturaCumplida = aperturaCumplida;
        this.oficial = oficial;
    }

    public static ProgresoEscrutinioDTO determinar(EstadoEscrutinio estado,
            boolean tieneFechaApertura, String revisionActaVigente) {
        if (estado == EstadoEscrutinio.ANULADO || estado == EstadoEscrutinio.OBSERVADO) {
            // No inferir conteo completo ni cierre a partir de fechas historicas.
            return new ProgresoEscrutinioDTO(tieneFechaApertura ? Etapa.CONTEO : Etapa.APERTURA,
                    "bloqueado", estado.name(), tieneFechaApertura, false);
        }
        if (estado == null || estado == EstadoEscrutinio.PENDIENTE) {
            return new ProgresoEscrutinioDTO(Etapa.APERTURA, "pendiente", null, false, false);
        }
        if (estado == EstadoEscrutinio.CERRADO) {
            if ("VALIDADA".equals(revisionActaVigente)) {
                return new ProgresoEscrutinioDTO(Etapa.DATO_OFICIAL, "validado", null, true, true);
            }
            if (revisionActaVigente == null || "OBSERVADA".equals(revisionActaVigente)
                    || "RECHAZADA".equals(revisionActaVigente)) {
                return new ProgresoEscrutinioDTO(Etapa.ACTA_FISICA,
                        revisionActaVigente == null ? "carga" : "correccion",
                        revisionActaVigente, true, false);
            }
            return new ProgresoEscrutinioDTO(Etapa.DATO_OFICIAL, "validacion", null, true, false);
        }
        if (estado == EstadoEscrutinio.CONTEO_REGISTRADO) {
            return new ProgresoEscrutinioDTO(Etapa.CIERRE, "pendiente", null, true, false);
        }
        return new ProgresoEscrutinioDTO(Etapa.CONTEO,
                estado == EstadoEscrutinio.ABIERTO ? "pendiente" : "curso",
                estado == EstadoEscrutinio.REABIERTO ? "REABIERTO" : null, true, false);
    }

    public Etapa getEtapa() {
        return etapa;
    }

    public int getIndice() {
        return etapa.ordinal();
    }

    public String getClave() {
        return etapa.getClave();
    }

    public String getDetalleClave() {
        return "actaE.avance." + detalle;
    }

    public boolean isOficial() {
        return oficial;
    }

    public boolean isTieneIncidencia() {
        return incidencia != null;
    }

    public String getIncidenciaClave() {
        return "actaE.avance.incidencia." + incidencia;
    }

    public String getSeverity() {
        if ("ANULADO".equals(incidencia) || "RECHAZADA".equals(incidencia))
            return "danger";
        if (incidencia != null)
            return "warning";
        return oficial ? "success" : "info";
    }

    public boolean completado(int indice) {
        if (indice < 0 || indice > 4)
            return false;
        if (indice == 0)
            return aperturaCumplida;
        return oficial || indice < getIndice();
    }

    public String estadoPaso(int indice) {
        if (completado(indice))
            return "completado";
        if (indice != getIndice())
            return "pendiente";
        return "ANULADO".equals(incidencia) || "OBSERVADO".equals(incidencia)
                ? "bloqueado"
                : "actual";
    }

    public boolean coincideFiltro(String filtro) {
        return filtro == null || filtro.isBlank() || etapa.name().equals(filtro.trim());
    }
}
