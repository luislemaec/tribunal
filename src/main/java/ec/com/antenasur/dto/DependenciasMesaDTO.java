package ec.com.antenasur.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

/** Datos de trabajo obtenidos por lote, nunca almacenados en sesion. */
@Data
public class DependenciasMesaDTO {
    private long empadronados;
    private long habilitados;
    private long personasCertificado;
    private boolean cerrada;
    private boolean juntaCompleta;
    private List<String> cargosFaltantes = new ArrayList<>();
    private Map<String, Integer> documentos = new HashMap<>();
}
