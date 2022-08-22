package ec.com.antenasur.tec.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor

@NoArgsConstructor
@Data
public class PersonaDTO {

    private String fechaNacimiento;
    private String cedula;
    private String nombre;

}
