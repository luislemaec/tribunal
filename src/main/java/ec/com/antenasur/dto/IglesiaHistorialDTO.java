package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Una revisión de Envers de una iglesia, ya interpretada para la UI: cuándo,
 * quién, qué acción, el valor que cada campo auditado tenía en esa revisión y
 * cuáles de ellos cambiaron respecto de la revisión anterior.
 *
 * <p>La vista muestra una columna por campo; {@link #cambiados} permite
 * resaltar las celdas que cambiaron, y {@code *Anterior} alimenta el tooltip
 * con el valor previo.
 */
@Data
@NoArgsConstructor
public class IglesiaHistorialDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ACCION_CREA = "CREA";
    public static final String ACCION_ACTUALIZA = "ACTUALIZA";
    public static final String ACCION_DESACTIVA = "DESACTIVA";
    public static final String ACCION_REACTIVA = "REACTIVA";
    public static final String ACCION_ELIMINA = "ELIMINA";
    /** Primera revisión de un registro cargado por migración: no hubo alta auditada. */
    public static final String ACCION_BASE = "BASE";

    /** Claves de campo usadas en {@link #cambiados}. */
    public static final String CAMPO_NOMBRE = "nombre";
    public static final String CAMPO_COMUNIDAD = "comunidad";
    public static final String CAMPO_DOCUMENTO = "documento";
    public static final String CAMPO_PARROQUIA = "parroquia";

    private Integer revision;
    private Date fecha;
    private String usuario;
    /** Código de acción; la vista lo traduce con {@code form.iglesias.hist.accion.<código>}. */
    private String accion;

    /** Valores auditados en esta revisión. */
    private String nombre;
    private String comunidad;
    private String documento;
    private String parroquia;
    /** Derivados de la parroquia de la revisión (no se auditan por separado). */
    private String canton;
    private String provincia;

    /** Valores de la revisión anterior, solo para los campos que cambiaron. */
    private String nombreAnterior;
    private String comunidadAnterior;
    private String documentoAnterior;
    private String parroquiaAnterior;

    /** Campos que cambiaron respecto de la revisión anterior. */
    private Set<String> cambiados = new HashSet<>();

    /** ¿Cambió este campo en la revisión? La vista lo usa para resaltar la celda. */
    public boolean cambio(String campo) {
        return cambiados.contains(campo);
    }

    /** ¿La revisión modificó algún dato auditado? */
    public boolean isConCambios() {
        return !cambiados.isEmpty();
    }
}
