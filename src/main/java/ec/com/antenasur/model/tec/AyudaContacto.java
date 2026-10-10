package ec.com.antenasur.model.tec;

import java.io.Serializable;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Filter;
import org.hibernate.envers.Audited;

import ec.com.antenasur.model.generic.EntidadAuditable;
import ec.com.antenasur.model.generic.EntidadBase;
import lombok.Getter;
import lombok.Setter;

/** Canal oficial de contacto que muestra el chatbot cuando no encuentra respuesta. */
@Entity
@Table(name = "ayuda_contacto", schema = "tec")
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
public class AyudaContacto extends EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TELEFONO = "TELEFONO";
    public static final String WHATSAPP = "WHATSAPP";
    public static final String CORREO = "CORREO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ayuc_id")
    private Integer id;

    @Column(name = "ayuc_tipo", nullable = false, length = 20)
    private String tipo;

    @Column(name = "ayuc_valor", nullable = false, length = 150)
    private String valor;

    @Column(name = "ayuc_etiqueta", length = 100)
    private String etiqueta;

    @Column(name = "ayuc_horario", length = 100)
    private String horario;

    @Column(name = "ayuc_orden", nullable = false)
    private Integer orden = 100;
}
