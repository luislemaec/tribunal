package ec.com.antenasur.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de integración: estructura del JSON que devuelve el servicio REST del
 * Registro Civil. NO es un DTO de la entidad {@code Persona} — para eso
 * existe {@link PersonaDTO}. Los nombres de campo coinciden con el contrato
 * del servicio externo y no deben renombrarse.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroCivilDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fechaNacimiento;
    private String cedula;
    private String nombre;
}
