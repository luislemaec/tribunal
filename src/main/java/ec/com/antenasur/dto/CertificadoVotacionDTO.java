package ec.com.antenasur.dto;

import java.io.Serializable;

/** Datos planos del padron para imprimir sin navegar relaciones JPA. */
public record CertificadoVotacionDTO(Integer personaId, String nombres, String apellidos,
        String documento, String iglesia, String comunidad, String parroquia,
        String canton, String provincia) implements Serializable {
}
