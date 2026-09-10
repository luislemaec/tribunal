package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** Totales transcritos de la evidencia, independientes de la suma calculada. */
@Data
public class RevisionActaFinalDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<EscrutinioDTO> resultados = new ArrayList<>();
    private Integer validosDeclarados;
    private Integer totalDeclarado;
    private boolean revisada;

    public long getValidos() { return sumar("LISTA"); }
    public long getBlancos() { return sumar("BLANCOS"); }
    public long getNulos() { return sumar("NULOS"); }
    public long getTotal() { return getValidos() + getBlancos() + getNulos(); }
    public Long getDiferenciaValidos() { return validosDeclarados == null ? null : validosDeclarados.longValue() - getValidos(); }
    public Long getDiferenciaTotal() { return totalDeclarado == null ? null : totalDeclarado.longValue() - getTotal(); }
    public boolean isCuadrada() {
        if (resultados == null || resultados.isEmpty() || validosDeclarados == null || totalDeclarado == null
                || validosDeclarados < 0 || totalDeclarado < 0) return false;
        java.util.Set<Integer> categorias = new java.util.HashSet<>();
        boolean lista = false, blanco = false, nulo = false;
        for (var r : resultados) {
            if (r == null || r.getCategoriaId() == null || !categorias.add(r.getCategoriaId())
                    || r.getTotalVotos() == null || r.getTotalVotos() < 0 || clasificar(r) == null) return false;
            lista |= "LISTA".equals(clasificar(r));
            blanco |= "BLANCOS".equals(clasificar(r));
            nulo |= "NULOS".equals(clasificar(r));
        }
        return lista && blanco && nulo && getDiferenciaValidos() == 0 && getDiferenciaTotal() == 0;
    }
    private long sumar(String clase) {
        if (resultados == null) return 0L;
        return resultados.stream().filter(r -> r != null && clase.equals(clasificar(r)))
                .mapToLong(r -> r.getTotalVotos() == null ? 0L : r.getTotalVotos().longValue()).sum();
    }
    public static String clasificar(EscrutinioDTO r) {
        if ("LISTA".equals(r.getCategoriaTipo())) return "LISTA";
        String nombre = r.getCategoriaNombre() == null ? "" : r.getCategoriaNombre().trim().toUpperCase(java.util.Locale.ROOT);
        if ("BLANCOS".equals(nombre) || "NULOS".equals(nombre)) return nombre;
        if ("PAPELETAS RESTANTES".equals(nombre) || "PAPELTAS RESTANTES".equals(nombre)) return "PAPELETAS";
        if ("LEGACY".equals(r.getCategoriaTipo()) && nombre.matches("LISTA \\d+")) return "LISTA";
        return null;
    }
}
