package ec.com.antenasur.dto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Fila de un reporte agrupado por cantón o parroquia. */
public interface CategoriaGeograficaDTO {

    Integer getId();

    String getNombre();

    String getProvincia();

    String getEtiqueta();

    void setEtiqueta(String etiqueta);

    /**
     * Un nombre de cantón puede repetirse en otra provincia: solo entonces se
     * añade la provincia a la etiqueta.
     */
    static void desambiguar(List<? extends CategoriaGeograficaDTO> filas) {
        Map<String, Long> repetidos = filas.stream()
                .collect(Collectors.groupingBy(CategoriaGeograficaDTO::getNombre, Collectors.counting()));
        for (CategoriaGeograficaDTO fila : filas) {
            if (repetidos.get(fila.getNombre()) > 1 && fila.getProvincia() != null) {
                fila.setEtiqueta(fila.getNombre() + " (" + fila.getProvincia() + ")");
            }
        }
    }
}
