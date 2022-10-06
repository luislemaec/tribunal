package ec.com.antenasur.domain.tec;

import ec.com.antenasur.domain.tec.TipoDocumento;
import ec.com.antenasur.domain.tec.Mesa;
import java.io.Serializable;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import ec.com.antenasur.domain.generic.EntidadAuditable;
import ec.com.antenasur.domain.generic.EntidadBase;
import java.io.File;
import java.util.Date;
import javax.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.envers.Audited;

/**
 * The persistent class for the tec_recintos database table.
 *
 */
@Entity
@Table(name = "documentos", schema = "tec")

@AttributeOverrides({
    @AttributeOverride(name = "estado", column = @Column(name = "estado")),
    @AttributeOverride(name = "fechaCrea", column = @Column(name = "f_crea")),
    @AttributeOverride(name = "fechaActualiza", column = @Column(name = "f_actualiza")),
    @AttributeOverride(name = "usuarioCrea", column = @Column(name = "u_crea")),
    @AttributeOverride(name = "usuarioActualiza", column = @Column(name = "u_actualiza"))})
@Filter(name = EntidadBase.FILTER_ACTIVE, condition = "estado = 'TRUE'")
@Audited
public class Documentos extends EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doc_id")
    @Setter
    @Getter
    private Integer id;

    @Setter
    @Getter
    @Column(name = "doc_nombre")
    private String nombre;

    @Setter
    @Getter
    @Column(name = "doc_path")
    private String path;

    // bi-directional many-to-one association to Usuario
    @Setter
    @Getter
    @ManyToOne
    @JoinColumn(name = "tipdoc_id")
    private TipoDocumento tipoDocumento;

    // bi-directional many-to-one association to Usuario
    @Setter
    @Getter
    @Column(name = "entidad_id")
    private Integer entidadId;

    @Setter
    @Getter
    @Column(name = "doc_extension")
    private String extension;

    @Setter
    @Getter
    @Column(name = "doc_mime")
    private String mime;

    @Setter
    @Getter
    @Column(name = "doc_codigo")
    private String codigo;

    @Setter
    @Getter
    @Transient
    private byte[] contenidoDocumento;

    @Setter
    @Getter
    @Transient
    private File contenidoDocumentoFile;

    @Setter
    @Getter
    @Transient
    private String tipoContenido;

    @Setter
    @Getter
    @Transient
    private Date fechaModificacion;

    public Documentos() {
    }

    public Documentos(String nombre, String path, TipoDocumento tipoDocumento, Integer entidadId, String extension, String mime, String codigo) {
        this.nombre = nombre;
        this.path = path;
        this.tipoDocumento = tipoDocumento;
        this.entidadId = entidadId;
        this.extension = extension;
        this.mime = mime;
        this.codigo = codigo;
    }
}
