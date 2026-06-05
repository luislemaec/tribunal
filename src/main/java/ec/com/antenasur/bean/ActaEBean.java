package ec.com.antenasur.bean;

import ec.com.antenasur.model.Persona;
import java.io.Serializable;

import jakarta.enterprise.context.RequestScoped;

import ec.com.antenasur.model.tec.Mesa;
import ec.com.antenasur.model.tec.MiembroJRV;
import ec.com.antenasur.model.tec.PlantillaCorreo;
import ec.com.antenasur.model.tec.Recinto;
import ec.com.antenasur.service.tec.MiembroJRVService;
import java.util.Collections;
import java.util.Set;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@RequestScoped
@NoArgsConstructor
public class ActaEBean implements Serializable {

    private static final long serialVersionUID = 1L;
    @Getter
    @Setter
    private PlantillaCorreo plantillaDocumento;

    @Setter
    @Getter
    private Mesa mesa;

    @Setter
    @Getter
    private Recinto recinto;

    @Setter
    @Getter
    private Set<MiembroJRV> miembrosJRV;

    @Setter
    @Getter
    private Persona presidente;

    @Setter
    @Getter
    private Persona secretario;
    
    @Setter
    @Getter
    private Persona tesoreo;
    
    @Setter
    @Getter
    private Persona vocal;

    @Inject
    private MiembroJRVService miembroJRVService;

    public ActaEBean(Mesa mesa, PlantillaCorreo plantillaDocumento) {
        this.mesa = mesa;
        this.plantillaDocumento = plantillaDocumento;
    }

    @PostConstruct
    private void init() {
        cargarMiembrosJRV();
    }

    private void cargarMiembrosJRV() {
        try {
            if (mesa != null) {
                this.recinto = mesa.getRecinto();
                miembrosJRV = miembroJRVService.getJRVPorMesa(mesa);
                cargaAutoridadesMesa();
            }
        } catch (Exception e) {
        }
    }

    private void cargaAutoridadesMesa() {
        try {
            if (miembrosJRV == null) {
                miembrosJRV = Collections.emptySet();
            }
            for (MiembroJRV mjrv : miembrosJRV) {
                if (mjrv == null || mjrv.getCargo() == null || mjrv.getCargo().getNombre() == null
                        || mjrv.getIglesiaPersona() == null || mjrv.getIglesiaPersona().getPersona() == null) {
                    continue;
                }
                String cargo = mjrv.getCargo().getNombre().trim().toUpperCase();
                Persona persona = mjrv.getIglesiaPersona().getPersona();
                if (cargo.contains("PRESIDENTE")) {
                    presidente = persona;
                } else if (cargo.contains("SECRETARIO")) {
                    secretario = persona;
                } else if (cargo.contains("TESORERO")) {
                    tesoreo = persona;
                } else if (cargo.contains("VOCAL")) {
                    vocal = persona;
                }
            }
        } catch (Exception e) {
        }
    }
}
