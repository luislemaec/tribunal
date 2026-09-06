package ec.com.antenasur.dto;

import java.io.Serializable;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FiltroPadronDTO implements Serializable {
    private Integer procesoId, provinciaId, cantonId, parroquiaId, recintoId, mesaId, iglesiaId;
    private String busqueda, iglesiaNombre;
    private Boolean conPadron;

    public FiltroPadronDTO(FiltroPadronDTO f) {
        procesoId = f.procesoId; provinciaId = f.provinciaId; cantonId = f.cantonId;
        parroquiaId = f.parroquiaId; recintoId = f.recintoId; mesaId = f.mesaId;
        iglesiaId = f.iglesiaId; busqueda = f.busqueda; conPadron = f.conPadron; iglesiaNombre = f.iglesiaNombre;
    }
}
