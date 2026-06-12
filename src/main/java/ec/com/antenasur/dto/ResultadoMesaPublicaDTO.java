package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResultadoMesaPublicaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer mesaId;
    private String provincia;
    private String canton;
    private String parroquia;
    private String recinto;
    private String mesa;
    private Integer sufragantesAsignados;
    private Integer votosRegistrados;
    private Integer votosValidos;
    private Integer votosBlancos;
    private Integer votosNulos;
    private Date fechaCierre;
}
