package ec.com.antenasur.model.tec;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Sesión de la App móvil (API REST /api/v1). Guarda solo el SHA-256 de los tokens opacos;
 * nunca el token ni la clave. Ver docs/api-movil.md y V7__sesion_movil_api.sql.
 */
@Entity
@Table(name = "sesion_movil", schema = "tec")
@Getter
@Setter
public class SesionMovil {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "sesion_id")
	private Long id;

	@Column(name = "usu_id", nullable = false)
	private Integer usuarioId;

	@Column(name = "acceso_hash", nullable = false, unique = true, length = 64)
	private String accesoHash;

	@Column(name = "refresh_hash", nullable = false, unique = true, length = 64)
	private String refreshHash;

	/** Refresh ya rotado: si se presenta de nuevo, el token fue copiado y se revoca la sesión. */
	@Column(name = "refresh_anterior_hash", length = 64)
	private String refreshAnteriorHash;

	/** Sesión restringida al cambio de clave obligatorio. */
	@Column(name = "solo_cambio_clave", nullable = false)
	private boolean soloCambioClave;

	@Column(name = "creada_en", nullable = false)
	private Instant creadaEn;

	@Column(name = "acceso_emitido_en", nullable = false)
	private Instant accesoEmitidoEn;

	@Column(name = "ultimo_uso", nullable = false)
	private Instant ultimoUso;

	@Column(name = "expira_absoluta", nullable = false)
	private Instant expiraAbsoluta;

	@Column(name = "revocada_en")
	private Instant revocadaEn;

	@Column(name = "motivo_revocacion", length = 40)
	private String motivoRevocacion;

	@Column(name = "ip", length = 45)
	private String ip;

	@Column(name = "dispositivo", length = 120)
	private String dispositivo;
}
