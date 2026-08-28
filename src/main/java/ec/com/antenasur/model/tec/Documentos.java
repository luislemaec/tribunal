package ec.com.antenasur.model.tec;

import java.io.Serializable;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;

import ec.com.antenasur.model.generic.EntidadAuditable;
import ec.com.antenasur.model.generic.EntidadBase;
import java.io.File;
import java.util.Date;
import jakarta.persistence.Transient;
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
    @JoinColumn(name = "tipdoc_id", foreignKey = @ForeignKey(name = "fk_documentos_tipo_documento"))
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
    @Column(name = "doc_hash_sha256", length = 64)
    private String hashSha256;

    @Setter
    @Getter
    @Column(name = "doc_contexto_hash", length = 64)
    private String contextoHash;

    /** Acta generada de la cual proviene un documento firmado. */
    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doc_origen_id", foreignKey = @ForeignKey(name = "fk_documentos_documento_origen"))
    private Documentos documentoOrigen;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proce_id", foreignKey = @ForeignKey(name = "fk_documentos_proceso_electoral"))
    private ProcesoElectoral proceso;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rec_id", foreignKey = @ForeignKey(name = "fk_documentos_recinto"))
    private Recinto recinto;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesa_id", foreignKey = @ForeignKey(name = "fk_documentos_mesa"))
    private Mesa mesa;

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
