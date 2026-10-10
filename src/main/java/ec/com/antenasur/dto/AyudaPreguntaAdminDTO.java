package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/** Pregunta del chatbot en la pantalla de mantenimiento (incluye las inactivas). */
@Getter
@Setter
public class AyudaPreguntaAdminDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String pregunta;
    private String respuesta;
    private String palabrasClave;
    private String pagina;
    /** Nombre de FaseElectoral o null. */
    private String fase;
    private String enlacePagina;
    private Integer orden = 100;
    private boolean activo;
    private List<Integer> rolIds = new ArrayList<>();
    /** Nombres de los roles, para la tabla; vacío = todos. */
    private String rolesTexto;
    private int utilSi;
    private int utilNo;
}
