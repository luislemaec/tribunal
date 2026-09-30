package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

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
    /** Fecha y hora en que el Tribunal validó el acta física (dato oficial). */
    private Date fechaValidacion;
    /** Votos de cada lista en la mesa: id de categoría de lista → votos. */
    private Map<Integer, Long> votosPorLista = new LinkedHashMap<>();
}
