package ec.com.antenasur.enums;

import ec.com.antenasur.util.Constantes;

/** Tipos que se gestionan desde el consolidado documental de una mesa. */
public enum TipoDocumentoMesa {
    PADRON_MESA(Constantes.TIPO_PADRON_ELECTORAL_MESA),
    ACTA_PARCIAL(Constantes.TIPO_ACTA_PARCIAL_ESCRUTINIO),
    CERTIFICADOS_VOTACION("CERTIFICADOS DE VOTACION DE MESA"),
    ACTA_FISICA_ESCRUTINIO("ACTA FISICA DE ESCRUTINIO"),
    DESIGNACION_MJRV(null);

    private final String nombreTipo;

    TipoDocumentoMesa(String nombreTipo) {
        this.nombreTipo = nombreTipo;
    }

    public String getNombreTipo() {
        return nombreTipo;
    }

    public boolean isDocumentoGenerable() {
        return this == PADRON_MESA || this == ACTA_PARCIAL || this == CERTIFICADOS_VOTACION;
    }

    public boolean isDocumentoConsultable() {
        return nombreTipo != null;
    }
}
