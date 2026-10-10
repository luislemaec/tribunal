package ec.com.antenasur.model.tec;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.Filter;
import org.hibernate.envers.Audited;

import ec.com.antenasur.enums.FaseElectoral;
import ec.com.antenasur.model.Rol;
import ec.com.antenasur.model.generic.EntidadAuditable;
import ec.com.antenasur.model.generic.EntidadBase;
import lombok.Getter;
import lombok.Setter;

/**
 * Pregunta frecuente del chatbot de ayuda. La respuesta es texto plano. La columna
 * {@code ayup_busqueda} (tsvector) la genera PostgreSQL a partir de
 * {@code textoBusqueda}; no se mapea.
 */
@Entity
@Table(name = "ayuda_pregunta", schema = "tec")
@AttributeOverrides({
    @AttributeOverride(name = "estado", column = @Column(name = "estado")),
    @AttributeOverride(name = "fechaCrea", column = @Column(name = "f_crea")),
    @AttributeOverride(name = "fechaActualiza", column = @Column(name = "f_actualiza")),
    @AttributeOverride(name = "usuarioCrea", column = @Column(name = "u_crea")),
    @AttributeOverride(name = "usuarioActualiza", column = @Column(name = "u_actualiza"))})
@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "estado = 'TRUE'")
@Audited
@Getter
@Setter
public class AyudaPregunta extends EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ayup_id")
    private Integer id;

    @Column(name = "ayup_pregunta", nullable = false, length = 300)
    private String pregunta;

    @Column(name = "ayup_respuesta", nullable = false, length = 4000)
    private String respuesta;

    @Column(name = "ayup_palabras_clave", length = 500)
    private String palabrasClave;

    @Column(name = "ayup_pagina", length = 80)
    private String pagina;

    @Enumerated(EnumType.STRING)
    @Column(name = "ayup_fase", length = 40)
    private FaseElectoral fase;

    @Column(name = "ayup_enlace_pagina", length = 80)
    private String enlacePagina;

    @Column(name = "ayup_orden", nullable = false)
    private Integer orden = 100;

    /** Valoraciones: se actualizan con UPDATE directo y no generan revisión de auditoría. */
    @org.hibernate.envers.NotAudited
    @Column(name = "ayup_util_si", nullable = false, insertable = false, updatable = false)
    private Integer utilSi;

    @org.hibernate.envers.NotAudited
    @Column(name = "ayup_util_no", nullable = false, insertable = false, updatable = false)
    private Integer utilNo;

    @Column(name = "ayup_texto_busqueda", nullable = false, length = 5000)
    private String textoBusqueda = "";

    /** Roles que ven la pregunta; vacío = todos. */
    @ManyToMany
    @JoinTable(name = "ayuda_pregunta_rol", schema = "tec",
            joinColumns = @JoinColumn(name = "ayup_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id"))
    private Set<Rol> roles = new HashSet<>();
}
