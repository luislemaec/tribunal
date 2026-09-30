package ec.com.antenasur.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ResultadoCategoriaPublicaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer categoriaId;
    private String categoria;
    private Long totalVotos;
    private Integer orden;
    private BigDecimal porcentaje = BigDecimal.ZERO;
    /** Tipo de la categoría (LISTA, ESPECIAL, LEGACY): decide si es una lista o un voto blanco/nulo. */
    private String tipo;
    private String listaNumero;
    private String listaNombre;
    private String listaSlogan;

    public ResultadoCategoriaPublicaDTO(Integer categoriaId, String categoria, Long totalVotos, Integer orden) {
        this.categoriaId = categoriaId;
        this.categoria = categoria;
        this.totalVotos = totalVotos != null ? totalVotos : 0L;
        this.orden = orden;
    }

    public ResultadoCategoriaPublicaDTO(Integer categoriaId, String categoria, Long totalVotos, Integer orden,
            String tipo, String listaNumero, String listaNombre, String listaSlogan) {
        this(categoriaId, categoria, totalVotos, orden);
        this.tipo = tipo;
        this.listaNumero = listaNumero;
        this.listaNombre = listaNombre;
        this.listaSlogan = listaSlogan;
    }

    public void calcularPorcentaje(long totalGeneral) {
        if (totalGeneral <= 0L) {
            this.porcentaje = BigDecimal.ZERO;
            return;
        }
        this.porcentaje = BigDecimal.valueOf(totalVotos != null ? totalVotos : 0L)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalGeneral), 2, RoundingMode.HALF_UP);
    }

    public int getPorcentajeEntero() {
        return porcentaje != null ? porcentaje.setScale(0, RoundingMode.HALF_UP).intValue() : 0;
    }
}
