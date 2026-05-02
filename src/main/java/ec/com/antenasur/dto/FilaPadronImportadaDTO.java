package ec.com.antenasur.dto;

import java.io.Serializable;

import ec.com.antenasur.model.Iglesia;
import ec.com.antenasur.model.Persona;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representación cruda de una fila de Excel del padrón electoral, antes de
 * resolver mesa/ubicación contra la BD. Generado por
 * {@code ExcelPadronParser.parsearFila(...)} y consumido por el controller
 * para llamar a {@code PadronService.importarFilaPadron(...)}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilaPadronImportadaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Persona persona;
    private Iglesia iglesia;
    private Integer ubicacionId;
    private String nombreMesa;
}
