package ec.com.antenasur.model.tec;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ec.com.antenasur.security.qr.EstadoAccesoQr;

/** No contiene el token original ni credenciales del usuario. */
@Entity
@Table(name = "acceso_qr_acta", schema = "tec")
@Getter
@Setter
public class AccesoQrActa {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "qr_id")
	private Long id;
	
	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;
	
	@Column(name = "doc_id", nullable = false, unique = true)
	private Integer documentoId;
	
	@Column(name = "proce_id", nullable = false)
	private Integer procesoId;
	
	@Column(name = "rec_id", nullable = false)
	private Integer recintoId;
	
	@Column(name = "mesa_id", nullable = false)
	private Integer mesaId;
	
	@Column(name = "miem_id", nullable = false)
	private Integer presidenteId;
	
	@Column(name = "usu_id", nullable = false)
	private Integer usuarioId;
	
	@Column(name = "tipdoc_id", nullable = false)
	private Integer tipoDocumentoId;
	
	@Column(name = "doc_version", nullable = false)
	private Integer documentoVersion;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 16)
	private EstadoAccesoQr estado;
	
	@Column(name = "vigente_desde", nullable = false)
	private Instant vigenteDesde;
	
	@Column(name = "vigente_hasta", nullable = false)
	private Instant vigenteHasta;
	
	@Column(name = "emitido_en", nullable = false)
	private Instant emitidoEn;
	
	@Column(name = "emitido_por", nullable = false, length = 255)
	private String emitidoPor;
	
	@Column(name = "canjeado_en")
	private Instant canjeadoEn;
	
	@Column(name = "revocado_en")
	private Instant revocadoEn;
	
	@Column(name = "motivo_revocacion", length = 255)
	private String motivoRevocacion;
	
	@Version
	@Column(name = "revision", nullable = false)
	private long revision;
}
