package ec.com.antenasur.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Una revisión de Envers de una persona, ya interpretada para la UI: cuándo,
 * quién, qué acción, el valor que cada campo auditado tenía en esa revisión y
 * cuáles de ellos cambiaron respecto de la revisión anterior.
 *
 * <p>Mismo modelo que {@link IglesiaHistorialDTO}: la vista muestra una columna
 * por campo, {@link #cambiados} permite resaltar las celdas que cambiaron y
 * {@code *Anterior} alimenta el tooltip con el valor previo.
 */
@Data
@NoArgsConstructor
public class PersonaHistorialDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ACCION_CREA = "CREA";
    public static final String ACCION_ACTUALIZA = "ACTUALIZA";
    public static final String ACCION_DESACTIVA = "DESACTIVA";
    public static final String ACCION_REACTIVA = "REACTIVA";
    public static final String ACCION_ELIMINA = "ELIMINA";
    /** Primera revisión de un registro cargado por migración: no hubo alta auditada. */
    public static final String ACCION_BASE = "BASE";

    /** Claves de campo usadas en {@link #cambiados}. */
    public static final String CAMPO_NOMBRES = "nombres";
    public static final String CAMPO_APELLIDOS = "apellidos";
    public static final String CAMPO_DOCUMENTO = "documento";
    public static final String CAMPO_TRATAMIENTO = "tratamiento";
    public static final String CAMPO_SEXO = "sexo";
    public static final String CAMPO_ESTADO = "estado";

    private Integer revision;
    private Date fecha;
    private String usuario;
    /** Código de acción; la vista lo traduce con {@code form.personas.hist.accion.<código>}. */
    private String accion;

    /** Valores auditados en esta revisión. */
    private String nombres;
    private String apellidos;
    private String documento;
    private String tratamiento;
    private String sexo;
    /** Estado ya traducido a etiqueta («Activo»/«Inactivo»), no el booleano crudo. */
    private String estado;

    /** Valores de la revisión anterior, solo para los campos que cambiaron. */
    private String nombresAnterior;
    private String apellidosAnterior;
    private String documentoAnterior;
    private String tratamientoAnterior;
    private String sexoAnterior;
    private String estadoAnterior;

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
